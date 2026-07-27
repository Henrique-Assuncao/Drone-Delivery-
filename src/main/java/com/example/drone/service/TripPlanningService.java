package com.example.drone.service;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TripPlanningService {

    private final DroneStorage droneStorage;
    private final OrderStorage orderStorage;
    private final TripStorage tripStorage;
    private final ObstacleStorage obstacleStorage;
    private final TripPlanner tripPlanner = new TripPlanner();
    private final RouteDistanceCalculator distanceCalculator = new RouteDistanceCalculator();

    public TripPlanningService(
            DroneStorage droneStorage,
            OrderStorage orderStorage,
            TripStorage tripStorage,
            ObstacleStorage obstacleStorage
    ) {
        this.droneStorage = droneStorage;
        this.orderStorage = orderStorage;
        this.tripStorage = tripStorage;
        this.obstacleStorage = obstacleStorage;
    }

    @Transactional
    public PersistedTripPlan planSaved() {
        return planSaved(true);
    }

    @Transactional
    public PersistedTripPlan planSaved(boolean optimizeRoute) {
        Instant now = Instant.now();
        Map<Drone, DroneEntity> droneEntitiesByDomain = new IdentityHashMap<>();
        Map<Order, OrderEntity> orderEntitiesByDomain = new IdentityHashMap<>();
        Map<Long, Drone> dronesByEntityId = new HashMap<>();
        Map<Drone, TripEntity> plannedTripEntitiesByDomainDrone = new IdentityHashMap<>();

        List<TripEntity> plannedTripEntities = tripStorage.findByStatus(TripStatus.PLANNED);
        List<DroneEntity> availableDroneEntities = dronesAvailableForNewPlanning(
                droneStorage.findByStatus(DroneStatus.AVAILABLE),
                plannedTripEntities,
                now
        );
        List<OrderEntity> requestedOrderEntities = orderStorage.findDeliveryQueue();
        List<Obstacle> obstacles = toDomainObstacles(obstacleStorage.findActive());

        List<Order> orders = toDomainOrders(
                requestedOrderEntities,
                orderEntitiesByDomain
        );
        enqueueDronesWithoutEnoughBatteryForRequestedOrders(
                availableDroneEntities,
                orders,
                obstacles,
                plannedDroneIds(plannedTripEntities)
        );

        List<Drone> drones = toDomainDrones(availableDroneEntities, droneEntitiesByDomain, dronesByEntityId);
        List<Trip> existingTrips = toAppendableDomainTrips(
                plannedTripEntities,
                dronesByEntityId,
                orderEntitiesByDomain,
                plannedTripEntitiesByDomainDrone,
                optimizeRoute,
                obstacles,
                now
        );

        TripPlan plan = tripPlanner.plan(drones, existingTrips, orders, optimizeRoute, obstacles);

        List<TripEntity> savedTrips = saveTrips(
                plan.trips(),
                droneEntitiesByDomain,
                orderEntitiesByDomain,
                plannedTripEntitiesByDomainDrone
        );
        List<PersistedUnallocatedOrder> unallocatedOrders = markUnallocatedOrders(
                plan.unallocatedOrders(),
                orderEntitiesByDomain
        );

        return new PersistedTripPlan(savedTrips, unallocatedOrders);
    }

    private List<DroneEntity> dronesAvailableForNewPlanning(
            List<DroneEntity> availableDroneEntities,
            List<TripEntity> plannedTripEntities,
            Instant now
    ) {
        Set<Long> reservedDroneIds = reservedDroneIds(plannedTripEntities, now);
        return availableDroneEntities.stream()
                .filter(drone -> drone.getId() == null || !reservedDroneIds.contains(drone.getId()))
                .toList();
    }

    private Set<Long> reservedDroneIds(List<TripEntity> plannedTripEntities, Instant now) {
        Set<Long> reservedDroneIds = new HashSet<>();
        collectReservedDroneIds(reservedDroneIds, tripStorage.findByStatus(TripStatus.IN_ROUTE));
        for (TripEntity trip : plannedTripEntities) {
            if (TripDispatchPolicy.isDispatchWindowOpen(trip, now)) {
                collectReservedDroneId(reservedDroneIds, trip);
            }
        }
        return reservedDroneIds;
    }

    private void collectReservedDroneIds(Set<Long> reservedDroneIds, List<TripEntity> trips) {
        for (TripEntity trip : trips) {
            collectReservedDroneId(reservedDroneIds, trip);
        }
    }

    private void collectReservedDroneId(Set<Long> reservedDroneIds, TripEntity trip) {
        Long droneId = trip.getDrone().getId();
        if (droneId != null) {
            reservedDroneIds.add(droneId);
        }
    }

    private List<Obstacle> toDomainObstacles(List<ObstacleEntity> obstacleEntities) {
        return obstacleEntities.stream()
                .map(ObstacleEntity::toDomain)
                .toList();
    }

    private List<Drone> toDomainDrones(
            List<DroneEntity> droneEntities,
            Map<Drone, DroneEntity> droneEntitiesByDomain,
            Map<Long, Drone> dronesByEntityId
    ) {
        List<Drone> drones = new ArrayList<>();

        for (DroneEntity entity : droneEntities) {
            Drone drone = toDomainDrone(entity);
            drones.add(drone);
            droneEntitiesByDomain.put(drone, entity);
            if (entity.getId() != null) {
                dronesByEntityId.put(entity.getId(), drone);
            }
        }

        return drones;
    }

    private Drone toDomainDrone(DroneEntity entity) {
        return new Drone(
                entity.getIdentifier(),
                entity.getMaxWeightCapacity(),
                entity.getMaxRange(),
                entity.getBatteryLevel(),
                entity.getBatteryConsumptionPerDistanceUnit(),
                entity.getMinimumReturnBattery(),
                entity.getSpeed(),
                entity.getChargingRate()
        );
    }

    private List<Order> toDomainOrders(
            List<OrderEntity> orderEntities,
            Map<Order, OrderEntity> orderEntitiesByDomain
    ) {
        List<Order> orders = new ArrayList<>();

        for (OrderEntity entity : orderEntities) {
            Order order = new Order(
                    entity.getIdentifier(),
                    new Coordinate(entity.getLocationX(), entity.getLocationY()),
                    entity.getWeight(),
                    entity.getPriority(),
                    entity.getConfirmedDeliveryTime()
            );
            orders.add(order);
            orderEntitiesByDomain.put(order, entity);
        }

        return orders;
    }

    private List<Trip> toAppendableDomainTrips(
            List<TripEntity> plannedTripEntities,
            Map<Long, Drone> dronesByEntityId,
            Map<Order, OrderEntity> orderEntitiesByDomain,
            Map<Drone, TripEntity> plannedTripEntitiesByDomainDrone,
            boolean optimizeRoute,
            List<Obstacle> obstacles,
            Instant now
    ) {
        List<Trip> trips = new ArrayList<>();

        for (TripEntity tripEntity : plannedTripEntities) {
            Long droneId = tripEntity.getDrone().getId();
            Drone drone = droneId == null ? null : dronesByEntityId.get(droneId);
            if (drone == null || TripDispatchPolicy.isDispatchWindowOpen(tripEntity, now)) {
                continue;
            }

            List<Order> orders = toDomainOrders(
                    tripEntity.getTripOrders().stream()
                            .map(TripOrderEntity::getOrder)
                            .toList(),
                    orderEntitiesByDomain
            );
            Trip trip = new Trip(drone, orders, optimizeRoute, obstacles);
            trips.add(trip);
            plannedTripEntitiesByDomainDrone.put(drone, tripEntity);
        }

        return trips;
    }

    private void enqueueDronesWithoutEnoughBatteryForRequestedOrders(
            List<DroneEntity> droneEntities,
            List<Order> orders,
            List<Obstacle> obstacles,
            Set<Long> plannedDroneIds
    ) {
        for (DroneEntity entity : droneEntities) {
            if (entity.getId() != null && plannedDroneIds.contains(entity.getId())) {
                continue;
            }

            Drone drone = toDomainDrone(entity);
            if (shouldEnterRechargeQueue(drone, orders, obstacles)) {
                entity.enqueueForRecharge(DroneRechargeService.INSUFFICIENT_BATTERY_REASON);
            }
        }
    }

    private boolean shouldEnterRechargeQueue(Drone drone, List<Order> orders, List<Obstacle> obstacles) {
        if (drone.batteryLevel() >= Drone.DEFAULT_BATTERY_LEVEL) {
            return false;
        }

        boolean hasOrderWithinWeightAndRange = false;

        for (Order order : orders) {
            double requiredDistance = roundTripDistanceFromBase(order, obstacles);
            if (drone.supportsWeightOf(order) && requiredDistance <= drone.maxRange()) {
                hasOrderWithinWeightAndRange = true;
                if (drone.canCompleteTripWithSafeReturn(requiredDistance)) {
                    return false;
                }
            }
        }

        return hasOrderWithinWeightAndRange;
    }

    private double roundTripDistanceFromBase(Order order, List<Obstacle> obstacles) {
        return distanceCalculator.roundTripDistanceFromBase(order, obstacles);
    }

    private List<TripEntity> saveTrips(
            List<Trip> trips,
            Map<Drone, DroneEntity> droneEntitiesByDomain,
            Map<Order, OrderEntity> orderEntitiesByDomain,
            Map<Drone, TripEntity> plannedTripEntitiesByDomainDrone
    ) {
        List<TripEntity> savedTrips = new ArrayList<>();

        for (Trip trip : trips) {
            List<Order> route = trip.route();
            List<Double> estimatedDeliveryTimes = trip.estimatedDeliveryTimes();
            List<OrderEntity> routeEntities = route.stream()
                    .map(orderEntitiesByDomain::get)
                    .toList();

            TripEntity entity = plannedTripEntitiesByDomainDrone.get(trip.drone());
            boolean existingPlannedTrip = entity != null;
            if (!existingPlannedTrip) {
                entity = new TripEntity(
                        null,
                        droneEntitiesByDomain.get(trip.drone()),
                        TripStatus.PLANNED,
                        trip.totalWeight(),
                        trip.totalDistance()
                );
            } else {
                entity.replacePlannedRoute(trip.totalWeight(), trip.totalDistance(), routeEntities, estimatedDeliveryTimes);
            }

            for (int index = 0; index < route.size(); index++) {
                OrderEntity orderEntity = routeEntities.get(index);
                orderEntity.changeStatus(OrderStatus.ALLOCATED);
                if (!existingPlannedTrip) {
                    entity.addOrder(orderEntity, index, null, estimatedDeliveryTimes.get(index));
                }
            }

            savedTrips.add(tripStorage.save(entity));
        }

        return savedTrips;
    }

    private Set<Long> plannedDroneIds(List<TripEntity> plannedTripEntities) {
        Set<Long> droneIds = new HashSet<>();
        for (TripEntity trip : plannedTripEntities) {
            Long droneId = trip.getDrone().getId();
            if (droneId != null) {
                droneIds.add(droneId);
            }
        }

        return droneIds;
    }

    private List<PersistedUnallocatedOrder> markUnallocatedOrders(
            List<UnallocatedOrder> unallocatedOrders,
            Map<Order, OrderEntity> orderEntitiesByDomain
    ) {
        List<PersistedUnallocatedOrder> persistedUnallocatedOrders = new ArrayList<>();

        for (UnallocatedOrder unallocatedOrder : unallocatedOrders) {
            OrderEntity orderEntity = orderEntitiesByDomain.get(unallocatedOrder.order());
            String localizedReason = localizedUnallocatedReason(unallocatedOrder.reason());
            orderEntity.changeStatus(OrderStatus.UNALLOCATED, localizedReason);
            persistedUnallocatedOrders.add(new PersistedUnallocatedOrder(orderEntity, localizedReason));
        }

        return persistedUnallocatedOrders;
    }

    private String localizedUnallocatedReason(String reason) {
        return switch (reason) {
            case "order exceeds max drone weight capacity" ->
                    "Pedido excede a capacidade máxima de peso dos drones disponíveis.";
            case "order exceeds max drone range" ->
                    "Pedido excede o alcance máximo dos drones disponíveis.";
            case "order exceeds max drone weight capacity and max drone range" ->
                    "Pedido excede a capacidade máxima de peso e o alcance máximo dos drones disponíveis.";
            case "order exceeds drone battery for complete trip and safe return" ->
                    "Pedido exige mais bateria do que a frota disponível possui para concluir a rota e retornar em segurança.";
            case "order requires another drone but no immediate drone is available" ->
                    "Pedido exige outro drone imediato, mas não há drone disponível nesta rodada de planejamento.";
            case "order cannot be served by any drone" ->
                    "Pedido não pode ser atendido por nenhum drone no planejamento atual.";
            default -> reason;
        };
    }
}
