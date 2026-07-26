# API Contract

Base URL local:

```text
http://localhost:8080
```

All responses are JSON. Error responses use:

```json
{
  "message": "error description"
}
```

Common error rules:

- Invalid request body syntax returns HTTP `400` with `request body is invalid`.
- Invalid enum query parameters return HTTP `400` with `status must be one of ...`.
- Invalid domain input returns HTTP `400`.
- Duplicated resource identifiers return HTTP `409`.
- Unknown resources return HTTP `404`.

## Enums

### DroneStatus

- `AVAILABLE`
- `IN_ROUTE`
- `UNAVAILABLE`
- `CHARGING`

### OrderStatus

- `REQUESTED`
- `ALLOCATED`
- `IN_ROUTE`
- `PENDING_REASSIGNMENT`
- `DELIVERED`
- `CANCELLED`
- `UNALLOCATED`

### TripStatus

- `PLANNED`
- `IN_ROUTE`
- `RETURNED_EARLY`
- `COMPLETED`
- `CANCELLED`

### Priority

- `HIGH`
- `MEDIUM`
- `LOW`

## Drones

### Create Drone

```text
POST /api/drones
```

Request:

```json
{
  "identifier": "DRONE-1",
  "maxWeightCapacity": 10.0,
  "maxRange": 20.0,
  "batteryLevel": 100.0,
  "batteryConsumptionPerDistanceUnit": 1.0,
  "minimumReturnBattery": 20.0,
  "speed": 1.0,
  "chargingRate": 10.0
}
```

Battery fields are optional. When omitted, the defaults are `batteryLevel: 100.0`,
`batteryConsumptionPerDistanceUnit: 1.0`, `minimumReturnBattery: 20.0`,
`speed: 1.0` and `chargingRate: 10.0`.

Success:

- HTTP `201`

```json
{
  "id": 1,
  "identifier": "DRONE-1",
  "maxWeightCapacity": 10.0,
  "maxRange": 20.0,
  "status": "AVAILABLE",
  "batteryLevel": 100.0,
  "batteryConsumptionPerDistanceUnit": 1.0,
  "minimumReturnBattery": 20.0,
  "speed": 1.0,
  "chargingRate": 10.0
}
```

Errors:

- HTTP `400`: `identifier must not be blank`
- HTTP `400`: `maxWeightCapacity must be greater than zero`
- HTTP `400`: `maxRange must be greater than zero`
- HTTP `400`: `batteryLevel must be between 0 and 100`
- HTTP `400`: `batteryConsumptionPerDistanceUnit must be greater than zero`
- HTTP `400`: `minimumReturnBattery must be between 0 and 100`
- HTTP `400`: `speed must be greater than zero`
- HTTP `400`: `chargingRate must be greater than zero`
- HTTP `409`: `drone identifier already exists`

### List Drones

```text
GET /api/drones
GET /api/drones?status=AVAILABLE
```

Query parameters:

| Name | Required | Values |
| --- | --- | --- |
| `status` | No | `AVAILABLE`, `IN_ROUTE`, `UNAVAILABLE`, `CHARGING` |

Success:

- HTTP `200`

```json
[
  {
    "id": 1,
    "identifier": "DRONE-1",
    "maxWeightCapacity": 10.0,
    "maxRange": 20.0,
    "status": "AVAILABLE",
    "batteryLevel": 100.0,
    "batteryConsumptionPerDistanceUnit": 1.0,
    "minimumReturnBattery": 20.0,
    "speed": 1.0,
    "chargingRate": 10.0
  }
]
```

Errors:

- HTTP `400`: `status must be one of AVAILABLE, IN_ROUTE, UNAVAILABLE, CHARGING`

### List Available Drones

```text
GET /api/drones/available
```

Success:

- HTTP `200`

```json
[
  {
    "id": 1,
    "identifier": "DRONE-1",
    "maxWeightCapacity": 10.0,
    "maxRange": 20.0,
    "status": "AVAILABLE",
    "batteryLevel": 100.0,
    "batteryConsumptionPerDistanceUnit": 1.0,
    "minimumReturnBattery": 20.0,
    "speed": 1.0,
    "chargingRate": 10.0
  }
]
```

### Get Drone By ID

```text
GET /api/drones/{id}
```

Success:

- HTTP `200`

```json
{
  "id": 1,
  "identifier": "DRONE-1",
  "maxWeightCapacity": 10.0,
  "maxRange": 20.0,
  "status": "AVAILABLE",
  "batteryLevel": 100.0,
  "batteryConsumptionPerDistanceUnit": 1.0,
  "minimumReturnBattery": 20.0,
  "speed": 1.0,
  "chargingRate": 10.0
}
```

Errors:

- HTTP `404`: `drone not found`

### Mark Drone Unavailable

```text
POST /api/drones/{id}/unavailable
```

Success:

- HTTP `200`

```json
{
  "id": 1,
  "identifier": "DRONE-1",
  "maxWeightCapacity": 10.0,
  "maxRange": 20.0,
  "status": "UNAVAILABLE",
  "batteryLevel": 100.0,
  "batteryConsumptionPerDistanceUnit": 1.0,
  "minimumReturnBattery": 20.0,
  "speed": 1.0,
  "chargingRate": 10.0
}
```

Errors:

- HTTP `404`: `drone not found`
- HTTP `400`: `drone must be AVAILABLE to mark unavailable`

### Mark Drone Available

```text
POST /api/drones/{id}/available
```

Success:

- HTTP `200`

```json
{
  "id": 1,
  "identifier": "DRONE-1",
  "maxWeightCapacity": 10.0,
  "maxRange": 20.0,
  "status": "AVAILABLE",
  "batteryLevel": 100.0,
  "batteryConsumptionPerDistanceUnit": 1.0,
  "minimumReturnBattery": 20.0,
  "speed": 1.0,
  "chargingRate": 10.0
}
```

Errors:

- HTTP `404`: `drone not found`
- HTTP `400`: `drone must be UNAVAILABLE to mark available`

### Enqueue Drone For Recharge

```text
POST /api/drones/{id}/recharge
```

Success:

- HTTP `200`

```json
{
  "id": 1,
  "identifier": "DRONE-1",
  "maxWeightCapacity": 10.0,
  "maxRange": 20.0,
  "status": "CHARGING",
  "batteryLevel": 75.0,
  "batteryConsumptionPerDistanceUnit": 1.0,
  "minimumReturnBattery": 20.0,
  "speed": 1.0,
  "chargingRate": 10.0,
  "rechargeQueuedAt": "2026-07-25T20:00:00Z",
  "rechargeReason": "manual recharge requested"
}
```

Errors:

- HTTP `404`: `drone not found`
- HTTP `400`: `drone must be AVAILABLE to enter recharge queue`
- HTTP `400`: `drone battery must be below 100 to enter recharge queue`

### Complete Drone Recharge

```text
POST /api/drones/{id}/recharge/complete
```

Success:

- HTTP `200`

```json
{
  "id": 1,
  "identifier": "DRONE-1",
  "maxWeightCapacity": 10.0,
  "maxRange": 20.0,
  "status": "AVAILABLE",
  "batteryLevel": 100.0,
  "batteryConsumptionPerDistanceUnit": 1.0,
  "minimumReturnBattery": 20.0,
  "speed": 1.0,
  "chargingRate": 10.0
}
```

Errors:

- HTTP `404`: `drone not found`
- HTTP `400`: `drone must be CHARGING to complete recharge`

## Recharge Queue

### List Recharge Queue

```text
GET /api/recharge-queue
```

Success:

- HTTP `200`

```json
[
  {
    "droneId": 1,
    "droneIdentifier": "DRONE-1",
    "status": "CHARGING",
    "batteryLevel": 75.0,
    "queuedAt": "2026-07-25T20:00:00Z",
    "reason": "manual recharge requested"
  }
]
```

## Internal Drones

### Get Drone Battery

```text
GET /internal/drones/{id}/battery
```

Success:

- HTTP `200`

```json
{
  "id": 1,
  "identifier": "DRONE-1",
  "status": "AVAILABLE",
  "batteryLevel": 100.0,
  "batteryConsumptionPerDistanceUnit": 1.0,
  "minimumReturnBattery": 20.0,
  "chargingRate": 10.0
}
```

Errors:

- HTTP `404`: `drone not found`

## Internal Demo

### Reset And Seed Demo Scenario

```text
POST /internal/demo/reset-and-seed?confirmation=RESET_DEMO_DATA
```

This endpoint clears current operational data and recreates a deterministic demo scenario with drones, orders, one obstacle, one review and optimized trip planning.

Success:

- HTTP `200`

```json
{
  "drones": 3,
  "orders": 5,
  "obstacles": 1,
  "reviews": 1,
  "trips": 2,
  "unallocatedOrders": 0
}
```

Errors:

- HTTP `400`: `confirmation must be RESET_DEMO_DATA`

## Orders

### Create Order

```text
POST /api/orders
```

Request:

```json
{
  "identifier": "ORDER-1",
  "location": {
    "x": 3.0,
    "y": 4.0
  },
  "weight": 4.0,
  "priority": "HIGH"
}
```

Success:

- HTTP `201`

```json
{
  "id": 1,
  "identifier": "ORDER-1",
  "location": {
    "x": 3.0,
    "y": 4.0
  },
  "weight": 4.0,
  "priority": "HIGH",
  "status": "REQUESTED",
  "queuedAt": "2026-07-25T20:00:00Z"
}
```

Errors:

- HTTP `400`: `identifier must not be blank`
- HTTP `400`: `location must not be null`
- HTTP `400`: `weight must be greater than zero`
- HTTP `400`: `priority must not be null`
- HTTP `409`: `order identifier already exists`

### List Orders

```text
GET /api/orders
GET /api/orders?status=REQUESTED
```

Query parameters:

| Name | Required | Values |
| --- | --- | --- |
| `status` | No | `REQUESTED`, `ALLOCATED`, `IN_ROUTE`, `PENDING_REASSIGNMENT`, `DELIVERED`, `CANCELLED`, `UNALLOCATED` |

Success:

- HTTP `200`

```json
[
  {
    "id": 1,
    "identifier": "ORDER-1",
    "location": {
      "x": 3.0,
      "y": 4.0
    },
    "weight": 4.0,
    "priority": "HIGH",
    "status": "REQUESTED",
    "queuedAt": "2026-07-25T20:00:00Z"
  }
]
```

Errors:

- HTTP `400`: `status must be one of REQUESTED, ALLOCATED, IN_ROUTE, PENDING_REASSIGNMENT, DELIVERED, CANCELLED, UNALLOCATED`

### Get Order By ID

```text
GET /api/orders/{id}
```

Success:

- HTTP `200`

```json
{
  "id": 1,
  "identifier": "ORDER-1",
  "location": {
    "x": 3.0,
    "y": 4.0
  },
  "weight": 4.0,
  "priority": "HIGH",
  "status": "REQUESTED",
  "queuedAt": "2026-07-25T20:00:00Z"
}
```

Errors:

- HTTP `404`: `order not found`

### List Delivery Queue

```text
GET /api/delivery-queue
```

Behavior:

- Returns only orders with status `REQUESTED` or `PENDING_REASSIGNMENT`.
- Orders are sorted by `queuedAt` and then by `id`.

Success:

- HTTP `200`

```json
[
  {
    "orderId": 1,
    "orderIdentifier": "ORDER-1",
    "location": {
      "x": 3.0,
      "y": 4.0
    },
    "weight": 4.0,
    "priority": "HIGH",
    "status": "REQUESTED",
    "queuedAt": "2026-07-25T20:00:00Z"
  }
]
```

## Reviews

### Create Review

```text
POST /api/reviews
```

Request:

```json
{
  "stars": 5,
  "title": "Entrega excelente",
  "feedback": "O pedido chegou antes do previsto."
}
```

Success:

- HTTP `201`

```json
{
  "id": 1,
  "stars": 5,
  "title": "Entrega excelente",
  "feedback": "O pedido chegou antes do previsto.",
  "reviewedAt": "2026-07-25T20:00:00Z"
}
```

Errors:

- HTTP `400`: `stars must be between 1 and 5`
- HTTP `400`: `title must not be blank`
- HTTP `400`: `feedback must not be blank`

### List Reviews

```text
GET /api/reviews
```

Success:

- HTTP `200`

```json
[
  {
    "id": 1,
    "stars": 5,
    "title": "Entrega excelente",
    "feedback": "O pedido chegou antes do previsto.",
    "reviewedAt": "2026-07-25T20:00:00Z"
  }
]
```

### Get Review By ID

```text
GET /api/reviews/{id}
```

Success:

- HTTP `200`

```json
{
  "id": 1,
  "stars": 5,
  "title": "Entrega excelente",
  "feedback": "O pedido chegou antes do previsto.",
  "reviewedAt": "2026-07-25T20:00:00Z"
}
```

Errors:

- HTTP `404`: `review not found`

## Obstacles

### Create Obstacle

```text
POST /api/obstacles
```

Request:

```json
{
  "center": {
    "x": 5.0,
    "y": 0.0
  },
  "radius": 1.0
}
```

Success:

- HTTP `201`

```json
{
  "id": 1,
  "center": {
    "x": 5.0,
    "y": 0.0
  },
  "radius": 1.0,
  "active": true
}
```

Errors:

- HTTP `400`: `center must not be null`
- HTTP `400`: `radius must be greater than zero`

### List Obstacles

```text
GET /api/obstacles
```

Success:

- HTTP `200`

```json
[
  {
    "id": 1,
    "center": {
      "x": 5.0,
      "y": 0.0
    },
    "radius": 1.0,
    "active": true
  }
]
```

### Deactivate Obstacle

```text
DELETE /api/obstacles/{id}
```

Success:

- HTTP `200`

```json
{
  "id": 1,
  "center": {
    "x": 5.0,
    "y": 0.0
  },
  "radius": 1.0,
  "active": false
}
```

Errors:

- HTTP `404`: `obstacle not found`

## Trip Plans

### Create Trip Plan

```text
POST /api/trip-plans
POST /api/trip-plans?optimizeRoute=false
```

Query parameters:

| Name | Required | Values | Default |
| --- | --- | --- | --- |
| `optimizeRoute` | No | `true`, `false` | `true` |

Request body:

```text
No body is required.
```

Behavior:

- Uses persisted drones with status `AVAILABLE`.
- Uses persisted orders with status `REQUESTED` or `PENDING_REASSIGNMENT`.
- Uses active circular obstacles to adjust route segment distances when a segment would cross an obstacle area.
- Automatically moves available drones to the recharge queue when they have weight/range for requested orders but insufficient current battery for all of them.
- With `optimizeRoute=true`, automatically orders deliveries by priority (`HIGH`, `MEDIUM`, `LOW`), higher weight, shorter distance from base, and then identifier.
- With `optimizeRoute=false`, preserves the delivery queue order inside planned trips.
- Requires each planned trip to leave enough battery for the full route and the drone's safe-return reserve.
- Calculates `estimatedDeliveryTime` for each route position and `averageDeliveryTime` for the trip.
- Creates persisted trips with status `PLANNED`.
- Changes allocated orders to `ALLOCATED`.
- Changes impossible orders to `UNALLOCATED`.

Success:

- HTTP `200`

```json
{
  "trips": [
    {
      "id": 1,
      "droneId": 1,
	    "status": "PLANNED",
	    "orders": [1],
	    "route": [1],
	    "routeProgress": [
		      {
		        "orderId": 1,
		        "routePosition": 0,
		        "delivered": false,
		        "deliveredAt": null,
		        "estimatedDeliveryTime": 5.0
		      }
		    ],
		    "totalWeight": 4.0,
		    "totalDistance": 10.0,
		    "estimatedDuration": 10.0,
		    "averageDeliveryTime": 5.0
	    }
  ],
  "unallocatedOrders": []
}
```

`estimatedDuration` is calculated as `totalDistance / speed` from the assigned drone.
`estimatedDeliveryTime` is cumulative from trip start to each route position, and `averageDeliveryTime` is the mean of those route-position times.
When active obstacles affect the route, `totalDistance`, `estimatedDuration`, range and battery checks use the adjusted distance.

Response with unallocated orders:

```json
{
  "trips": [],
  "unallocatedOrders": [
    {
      "orderId": 1,
      "orderIdentifier": "ORDER-1",
      "reason": "order exceeds max drone weight capacity"
    }
  ]
}
```

Known unallocated reasons:

- `order exceeds max drone weight capacity`
- `order exceeds max drone range`
- `order exceeds max drone weight capacity and max drone range`
- `order exceeds drone battery for complete trip and safe return`
- `order cannot be served by any drone`

Errors:

- HTTP `400`: `optimizeRoute is invalid`

## Trips

### List Trips

```text
GET /api/trips
GET /api/trips?status=PLANNED
```

Query parameters:

| Name | Required | Values |
| --- | --- | --- |
| `status` | No | `PLANNED`, `IN_ROUTE`, `RETURNED_EARLY`, `COMPLETED`, `CANCELLED` |

Success:

- HTTP `200`

```json
[
  {
    "id": 1,
    "droneId": 1,
	  "status": "PLANNED",
	  "orders": [1],
	  "route": [1],
	  "routeProgress": [
	    {
	      "orderId": 1,
	      "routePosition": 0,
	      "delivered": false,
	      "deliveredAt": null,
	      "estimatedDeliveryTime": 5.0
	    }
	  ],
	  "totalWeight": 4.0,
	  "totalDistance": 10.0,
	  "estimatedDuration": 10.0,
	  "averageDeliveryTime": 5.0,
	  "simulation": {
	    "tripId": 1,
	    "droneId": 1,
	    "status": "PLANNED",
	    "currentLocation": {
	      "x": 0.0,
	      "y": 0.0
	    },
	    "travelledDistance": 0.0,
	    "totalDistance": 10.0,
	    "progress": 0.0,
	    "nextOrderId": 1,
	    "nextRoutePosition": 0,
	    "moving": false,
	    "updatedAt": null
	  }
  }
]
```

Errors:

- HTTP `400`: `status must be one of PLANNED, IN_ROUTE, RETURNED_EARLY, COMPLETED, CANCELLED`

### Get Trip By ID

```text
GET /api/trips/{id}
```

Success:

- HTTP `200`

```json
{
  "id": 1,
  "droneId": 1,
  "status": "PLANNED",
  "orders": [1],
  "route": [1],
  "totalWeight": 4.0,
  "totalDistance": 10.0,
  "estimatedDuration": 10.0,
  "averageDeliveryTime": 5.0,
  "simulation": {
    "tripId": 1,
    "droneId": 1,
    "status": "PLANNED",
    "currentLocation": {
      "x": 0.0,
      "y": 0.0
    },
    "travelledDistance": 0.0,
    "totalDistance": 10.0,
    "progress": 0.0,
    "nextOrderId": 1,
    "nextRoutePosition": 0,
    "moving": false,
    "updatedAt": null
  }
}
```

Errors:

- HTTP `404`: `trip not found`

### Start Trip

```text
POST /api/trips/{id}/start
```

Behavior:

- Validates that current drone battery can cover the saved route and safe-return reserve.
- Trip changes from `PLANNED` to `IN_ROUTE`.
- Associated drone changes from `AVAILABLE` to `IN_ROUTE`.
- Associated orders change to `IN_ROUTE`.

Success:

- HTTP `200`

```json
{
  "id": 1,
  "droneId": 1,
	  "status": "IN_ROUTE",
	  "orders": [1],
	  "route": [1],
	  "routeProgress": [
	    {
	      "orderId": 1,
	      "routePosition": 0,
	      "delivered": false,
	      "deliveredAt": null
	    }
	  ],
	  "totalWeight": 4.0,
	  "totalDistance": 10.0,
	  "estimatedDuration": 10.0
}
```

Errors:

- HTTP `404`: `trip not found`
- HTTP `400`: `trip must be PLANNED to start`
- HTTP `400`: `drone must be AVAILABLE to start trip`
- HTTP `400`: `drone battery is insufficient for complete trip and safe return`

### Trip Simulation

```text
GET /api/trips/{id}/simulation
POST /api/trips/{id}/simulation/tick
```

Tick request:

```json
{
  "elapsedMinutes": 1.0
}
```

Behavior:

- `GET` returns the current persisted simulation state for the trip.
- `POST /tick` advances a planned or in-route trip by the given simulated minutes.
- When the first tick reaches a `PLANNED` trip, the trip starts automatically if the drone is `AVAILABLE` and battery is sufficient for the saved route plus safe-return reserve.
- The simulation consumes battery by travelled distance, updates current drone position, marks reached route positions as delivered, and completes the trip when the full route is finished.
- If the remaining route is no longer safe, the trip changes to `RETURNED_EARLY`, undelivered orders become `PENDING_REASSIGNMENT`, and the drone enters `CHARGING`.

Success:

- HTTP `200`

```json
{
  "tripId": 1,
  "droneId": 1,
  "status": "IN_ROUTE",
  "currentLocation": {
    "x": 5.0,
    "y": 0.0
  },
  "travelledDistance": 5.0,
  "totalDistance": 10.0,
  "progress": 0.5,
  "nextOrderId": 1,
  "nextRoutePosition": 0,
  "moving": true,
  "updatedAt": "2026-07-26T15:00:00Z"
}
```

Errors:

- HTTP `404`: `trip not found`
- HTTP `400`: `elapsedMinutes must be greater than zero`
- HTTP `400`: `drone must be AVAILABLE to start trip`
- HTTP `400`: `drone battery is insufficient for complete trip and safe return`

### Report Route Delivery

```text
POST /api/trips/{id}/route/{routePosition}/deliver
```

Behavior:

- Accepts delivery progress only for trips with status `IN_ROUTE`.
- Marks the route item at `routePosition` as delivered and stores `deliveredAt`.
- Changes the associated order status to `DELIVERED`.
- Requires previous route positions to be delivered first.

Success:

- HTTP `200`

```json
{
  "id": 1,
  "droneId": 1,
  "status": "IN_ROUTE",
  "orders": [1, 2],
  "route": [1, 2],
  "routeProgress": [
    {
      "orderId": 1,
      "routePosition": 0,
      "delivered": true,
      "deliveredAt": "2026-07-25T20:00:00Z"
    },
    {
      "orderId": 2,
      "routePosition": 1,
      "delivered": false,
      "deliveredAt": null
    }
  ],
  "totalWeight": 8.0,
  "totalDistance": 20.0,
  "estimatedDuration": 20.0
}
```

Errors:

- HTTP `404`: `trip not found`
- HTTP `404`: `trip route position not found`
- HTTP `400`: `trip must be IN_ROUTE to report delivery`
- HTTP `400`: `routePosition must not be negative`
- HTTP `400`: `previous route positions must be delivered first`
- HTTP `400`: `route position already delivered`

### Report Trip Telemetry

```text
POST /api/trips/{id}/telemetry
```

Request:

```json
{
  "batteryLevel": 35.0
}
```

Behavior:

- Accepts telemetry only for trips with status `IN_ROUTE`.
- Updates the associated drone's current `batteryLevel`.
- If the updated battery still covers the saved route and safe-return reserve, the trip remains `IN_ROUTE`.
- If the updated battery no longer covers the saved route and safe-return reserve, the early-return path is applied immediately.
- In the early-return path, only route positions already reported as delivered remain `DELIVERED`; unreported route positions become `PENDING_REASSIGNMENT`.

Success:

- HTTP `200`

```json
{
  "id": 1,
  "droneId": 1,
	  "status": "IN_ROUTE",
	  "orders": [1],
	  "route": [1],
	  "routeProgress": [
	    {
	      "orderId": 1,
	      "routePosition": 0,
	      "delivered": false,
	      "deliveredAt": null
	    }
	  ],
	  "totalWeight": 4.0,
	  "totalDistance": 10.0,
	  "estimatedDuration": 10.0
}
```

Early-return success:

- HTTP `200`

```json
{
  "id": 1,
  "droneId": 1,
	  "status": "RETURNED_EARLY",
	  "orders": [1, 2],
	  "route": [1, 2],
	  "routeProgress": [
	    {
	      "orderId": 1,
	      "routePosition": 0,
	      "delivered": true,
	      "deliveredAt": "2026-07-25T20:00:00Z"
	    },
	    {
	      "orderId": 2,
	      "routePosition": 1,
	      "delivered": false,
	      "deliveredAt": null
	    }
	  ],
	  "totalWeight": 8.0,
	  "totalDistance": 20.0,
	  "estimatedDuration": 20.0
}
```

Errors:

- HTTP `404`: `trip not found`
- HTTP `400`: `trip must be IN_ROUTE to report telemetry`
- HTTP `400`: `batteryLevel must not be null`
- HTTP `400`: `batteryLevel must be between 0 and 100`

### List Trip Telemetry

```text
GET /api/trips/{id}/telemetry
```

Behavior:

- Returns the persisted telemetry history for the trip.
- Telemetry entries are sorted by `reportedAt` and then by `id`.

Success:

- HTTP `200`

```json
[
  {
    "id": 1,
    "tripId": 1,
    "batteryLevel": 90.0,
    "reportedAt": "2026-07-25T20:00:00Z"
  }
]
```

Errors:

- HTTP `404`: `trip not found`

### Complete Trip

```text
POST /api/trips/{id}/complete
```

Behavior:

- If the current battery can still cover the saved route and safe-return reserve, the trip changes from `IN_ROUTE` to `COMPLETED`.
- In that complete path, the associated drone changes to `AVAILABLE` and all associated orders change to `DELIVERED`.
- If the current battery cannot cover the saved route, the early-return path uses the persisted route progress.
- In the early-return path, route positions already reported as delivered remain `DELIVERED`, unreported route positions change to `PENDING_REASSIGNMENT`, the trip changes to `RETURNED_EARLY`, and the drone enters `CHARGING`.

Success:

- HTTP `200`

```json
{
  "id": 1,
  "droneId": 1,
	  "status": "COMPLETED",
	  "orders": [1],
	  "route": [1],
	  "routeProgress": [
	    {
	      "orderId": 1,
	      "routePosition": 0,
	      "delivered": true,
	      "deliveredAt": "2026-07-25T20:00:00Z"
	    }
	  ],
	  "totalWeight": 4.0,
	  "totalDistance": 10.0,
	  "estimatedDuration": 10.0
}
```

Early-return success:

- HTTP `200`

```json
{
  "id": 1,
  "droneId": 1,
	  "status": "RETURNED_EARLY",
	  "orders": [1, 2],
	  "route": [1, 2],
	  "routeProgress": [
	    {
	      "orderId": 1,
	      "routePosition": 0,
	      "delivered": true,
	      "deliveredAt": "2026-07-25T20:00:00Z"
	    },
	    {
	      "orderId": 2,
	      "routePosition": 1,
	      "delivered": false,
	      "deliveredAt": null
	    }
	  ],
	  "totalWeight": 8.0,
	  "totalDistance": 20.0,
	  "estimatedDuration": 20.0
}
```

Errors:

- HTTP `404`: `trip not found`
- HTTP `400`: `trip must be IN_ROUTE to complete`

### Cancel Trip

```text
POST /api/trips/{id}/cancel
```

Behavior:

- Trip changes from `PLANNED` or `IN_ROUTE` to `CANCELLED`.
- Associated drone changes to `AVAILABLE`.
- Associated orders that are not `DELIVERED` change to `REQUESTED`.

Success:

- HTTP `200`

```json
{
  "id": 1,
  "droneId": 1,
  "status": "CANCELLED",
  "orders": [1],
  "route": [1],
  "totalWeight": 4.0,
  "totalDistance": 10.0,
  "estimatedDuration": 10.0
}
```

Errors:

- HTTP `404`: `trip not found`
- HTTP `400`: `trip must be PLANNED or IN_ROUTE to cancel`
