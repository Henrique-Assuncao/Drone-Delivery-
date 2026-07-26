package com.example.drone.service;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

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
        Map<Drone, DroneEntity> droneEntitiesByDomain = new IdentityHashMap<>();
        Map<Order, OrderEntity> orderEntitiesByDomain = new IdentityHashMap<>();

        List<DroneEntity> availableDroneEntities = droneStorage.findByStatus(DroneStatus.AVAILABLE);
        List<OrderEntity> requestedOrderEntities = orderStorage.findDeliveryQueue();
        List<Obstacle> obstacles = toDomainObstacles(obstacleStorage.findActive());

        List<Order> orders = toDomainOrders(
                requestedOrderEntities,
                orderEntitiesByDomain
        );
        enqueueDronesWithoutEnoughBatteryForRequestedOrders(availableDroneEntities, orders, obstacles);

        List<Drone> drones = toDomainDrones(availableDroneEntities, droneEntitiesByDomain);

        TripPlan plan = tripPlanner.plan(drones, orders, optimizeRoute, obstacles);

        List<TripEntity> savedTrips = saveTrips(plan.trips(), droneEntitiesByDomain, orderEntitiesByDomain);
        List<PersistedUnallocatedOrder> unallocatedOrders = markUnallocatedOrders(
                plan.unallocatedOrders(),
                orderEntitiesByDomain
        );

        return new PersistedTripPlan(savedTrips, unallocatedOrders);
    }

    private List<Obstacle> toDomainObstacles(List<ObstacleEntity> obstacleEntities) {
        return obstacleEntities.stream()
                .map(ObstacleEntity::toDomain)
                .toList();
    }

    private List<Drone> toDomainDrones(
            List<DroneEntity> droneEntities,
            Map<Drone, DroneEntity> droneEntitiesByDomain
    ) {
        List<Drone> drones = new ArrayList<>();

        for (DroneEntity entity : droneEntities) {
            Drone drone = toDomainDrone(entity);
            drones.add(drone);
            droneEntitiesByDomain.put(drone, entity);
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
                    entity.getPriority()
            );
            orders.add(order);
            orderEntitiesByDomain.put(order, entity);
        }

        return orders;
    }

    private void enqueueDronesWithoutEnoughBatteryForRequestedOrders(
            List<DroneEntity> droneEntities,
            List<Order> orders,
            List<Obstacle> obstacles
    ) {
        for (DroneEntity entity : droneEntities) {
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
            Map<Order, OrderEntity> orderEntitiesByDomain
    ) {
        List<TripEntity> savedTrips = new ArrayList<>();

        for (Trip trip : trips) {
            TripEntity entity = new TripEntity(
                    null,
                    droneEntitiesByDomain.get(trip.drone()),
                    TripStatus.PLANNED,
                    trip.totalWeight(),
                    trip.totalDistance()
            );

            List<Order> route = trip.route();
            List<Double> estimatedDeliveryTimes = trip.estimatedDeliveryTimes();
            for (int index = 0; index < route.size(); index++) {
                OrderEntity orderEntity = orderEntitiesByDomain.get(route.get(index));
                orderEntity.changeStatus(OrderStatus.ALLOCATED);
                entity.addOrder(orderEntity, index, null, estimatedDeliveryTimes.get(index));
            }

            savedTrips.add(tripStorage.save(entity));
        }

        return savedTrips;
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
            case "order cannot be served by any drone" ->
                    "Pedido não pode ser atendido por nenhum drone no planejamento atual.";
            default -> reason;
        };
    }
}
