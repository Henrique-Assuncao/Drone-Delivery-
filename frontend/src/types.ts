export type DroneStatus = "AVAILABLE" | "IN_ROUTE" | "UNAVAILABLE" | "CHARGING";

export type OrderStatus =
  | "REQUESTED"
  | "ALLOCATED"
  | "IN_ROUTE"
  | "PENDING_REASSIGNMENT"
  | "DELIVERED"
  | "CANCELLED"
  | "UNALLOCATED";

export type TripStatus = "PLANNED" | "IN_ROUTE" | "RETURNED_EARLY" | "COMPLETED" | "CANCELLED";

export type Priority = "HIGH" | "MEDIUM" | "LOW";

export type DroneAction = "markUnavailable" | "markAvailable" | "enqueueRecharge" | "completeRecharge";

export type TripAction = "start" | "deliverNext" | "sendTelemetry" | "complete" | "cancel";

export interface CreateDronePayload {
  identifier: string;
  maxWeightCapacity: number;
  maxRange: number;
  batteryLevel?: number;
  batteryConsumptionPerDistanceUnit?: number;
  minimumReturnBattery?: number;
  speed?: number;
  chargingRate?: number;
}

export interface CreateOrderPayload {
  identifier: string;
  location: {
    x: number;
    y: number;
  };
  weight: number;
  priority: Priority;
}

export interface CreateObstaclePayload {
  center: {
    x: number;
    y: number;
  };
  radius: number;
}

export interface CreateReviewPayload {
  stars: number;
  title: string;
  feedback: string;
}

export interface Drone {
  id: number;
  identifier: string;
  maxWeightCapacity: number;
  maxRange: number;
  status: DroneStatus;
  batteryLevel: number;
  batteryConsumptionPerDistanceUnit: number;
  minimumReturnBattery: number;
  speed: number;
  chargingRate: number;
  rechargeQueuedAt?: string | null;
  rechargeReason?: string | null;
}

export interface Order {
  id: number;
  identifier: string;
  location: {
    x: number;
    y: number;
  };
  weight: number;
  priority: Priority;
  status: OrderStatus;
  queuedAt: string;
}

export interface TripRouteProgress {
  orderId: number;
  routePosition: number;
  delivered: boolean;
  deliveredAt: string | null;
  estimatedDeliveryTime: number;
}

export interface TripSimulation {
  tripId: number;
  droneId: number;
  status: TripStatus;
  currentLocation: {
    x: number;
    y: number;
  };
  travelledDistance: number;
  totalDistance: number;
  progress: number;
  nextOrderId: number | null;
  nextRoutePosition: number | null;
  moving: boolean;
  updatedAt: string | null;
}

export interface Trip {
  id: number;
  droneId: number;
  status: TripStatus;
  orders: number[];
  route: number[];
  routeProgress: TripRouteProgress[];
  totalWeight: number;
  totalDistance: number;
  estimatedDuration: number;
  averageDeliveryTime: number;
  simulation?: TripSimulation;
}

export interface TripTelemetry {
  id: number;
  tripId: number;
  batteryLevel: number;
  reportedAt: string;
}

export interface UnallocatedOrder {
  orderId: number;
  orderIdentifier: string;
  reason: string;
}

export interface TripPlan {
  trips: Trip[];
  unallocatedOrders: UnallocatedOrder[];
}

export interface DeliveryQueueEntry {
  orderId: number;
  orderIdentifier: string;
  location: {
    x: number;
    y: number;
  };
  weight: number;
  priority: Priority;
  status: OrderStatus;
  queuedAt: string;
}

export interface Obstacle {
  id: number;
  center: {
    x: number;
    y: number;
  };
  radius: number;
  active: boolean;
}

export interface RechargeQueueEntry {
  droneId: number;
  droneIdentifier: string;
  status: DroneStatus;
  batteryLevel: number;
  queuedAt: string | null;
  reason: string | null;
}

export interface Review {
  id: number;
  stars: number;
  title: string;
  feedback: string;
  reviewedAt: string;
}

export interface DroneProductivityReport {
  droneId: number;
  droneIdentifier: string;
  ordersDelivered: number;
  tripsStarted: number;
  tripsCompleted: number;
  tripsCancelled: number;
  tripsReturnedEarly: number;
}

export interface ProductivityReport {
  month: string;
  periodStart: string;
  periodEnd: string;
  orderEntries: number;
  ordersSent: number;
  ordersDelivered: number;
  ordersCancelled: number;
  conversionRate: number;
  drones: DroneProductivityReport[];
  generatedAt: string;
}

export interface DashboardSnapshot {
  drones: Drone[];
  orders: Order[];
  trips: Trip[];
  deliveryQueue: DeliveryQueueEntry[];
  obstacles: Obstacle[];
  rechargeQueue: RechargeQueueEntry[];
  reviews: Review[];
  productivityReport: ProductivityReport | null;
}
