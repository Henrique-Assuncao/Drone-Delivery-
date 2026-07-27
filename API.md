# API Contract

Base URL local:

```text
http://localhost:8080
```

Interactive OpenAPI documentation:

```text
http://localhost:8080/swagger-ui.html
```

Machine-readable OpenAPI specification:

```text
http://localhost:8080/v3/api-docs
http://localhost:8080/v3/api-docs.yaml
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
- Missing, invalid or expired client authorization tokens return HTTP `401`.
- Duplicated resource identifiers return HTTP `409`.
- Unknown resources return HTTP `404`.

## Measurement Units

The API uses metric units compatible with Brazilian operational conventions:

- Weight, drone capacity and payload: kilograms (`kg`).
- X/Y coordinates, distance, range and obstacle radius: kilometers (`km`).
- Drone average speed: kilometers per hour (`km/h`).
- Battery level and minimum return reserve: percent (`%`).
- Battery consumption: percent per kilometer (`%/km`).
- Charging rate: percent per minute (`%/min`).
- Trip duration and delivery estimates: minutes (`min`).

With these units, `estimatedDuration` is calculated as `(totalDistance / speed) * 60`, using `totalDistance` in km and `speed` in km/h.

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
- `NOT_DELIVERED`
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
  "speed": 60.0,
  "chargingRate": 10.0
}
```

Battery, speed and charging fields are optional. When omitted, the defaults are `batteryLevel: 100.0`,
`batteryConsumptionPerDistanceUnit: 1.0`, `minimumReturnBattery: 20.0`,
`speed: 60.0` and `chargingRate: 10.0`.

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
  "speed": 60.0,
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
    "speed": 60.0,
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
    "speed": 60.0,
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
  "speed": 60.0,
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
  "speed": 60.0,
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
  "speed": 60.0,
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
  "speed": 60.0,
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
  "speed": 60.0,
  "chargingRate": 10.0
}
```

Errors:

- HTTP `404`: `drone not found`
- HTTP `400`: `drone must be CHARGING to complete recharge`

### Delete Drone

```text
DELETE /api/drones/{id}
```

Behavior:

- Deletes a drone that is not in route and has no trips linked to it.
- Drones in route and drones with trip history are rejected.

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
  "speed": 60.0,
  "chargingRate": 10.0
}
```

Errors:

- HTTP `404`: `drone not found`
- HTTP `400`: `drone must not be IN_ROUTE to delete`
- HTTP `400`: `drone with trips cannot be deleted`

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

Internal endpoints require the header `X-Internal-Api-Key`.

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

- HTTP `401`: `internal api key is required`
- HTTP `404`: `drone not found`

## Internal Demo

Internal endpoints require the header `X-Internal-Api-Key`.

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

- HTTP `401`: `internal api key is required`
- HTTP `400`: `confirmation must be RESET_DEMO_DATA`

## Client Authentication

Client endpoints that require identity use:

```text
Authorization: Bearer <token>
```

### Register Client

```text
POST /api/auth/register
```

Request:

```json
{
  "name": "Ana Cliente",
  "email": "ana@example.com",
  "password": "senha123"
}
```

Success:

- HTTP `201`

```json
{
  "user": {
    "id": 1,
    "name": "Ana Cliente",
    "email": "ana@example.com",
    "createdAt": "2026-07-26T20:00:00Z"
  },
  "token": "signed-token"
}
```

Errors:

- HTTP `400`: `name must not be blank`
- HTTP `400`: `email is invalid`
- HTTP `400`: `password must have at least 8 characters`
- HTTP `409`: `user email already exists`

### Login Client

```text
POST /api/auth/login
```

Request:

```json
{
  "email": "ana@example.com",
  "password": "senha123"
}
```

Success:

- HTTP `200`

```json
{
  "user": {
    "id": 1,
    "name": "Ana Cliente",
    "email": "ana@example.com",
    "createdAt": "2026-07-26T20:00:00Z"
  },
  "token": "signed-token"
}
```

Errors:

- HTTP `401`: `email or password is invalid`

### Current Client

```text
GET /api/auth/me
```

Success:

- HTTP `200`

```json
{
  "id": 1,
  "name": "Ana Cliente",
  "email": "ana@example.com",
  "createdAt": "2026-07-26T20:00:00Z"
}
```

Errors:

- HTTP `401`: `authorization token is required`
- HTTP `401`: `authorization token is invalid`
- HTTP `401`: `authorization token expired`

## Client Orders

### Create Client Order

```text
POST /api/client/orders
```

Request:

```json
{
  "identifier": "DD-8F4A2B",
  "location": {
    "x": 3.0,
    "y": 4.0
  },
  "weight": 2.5,
  "confirmedDeliveryTime": "2026-07-26T18:30:00Z"
}
```

Behavior:

- Requires a valid client token.
- Creates the order with priority `MEDIUM`.
- Links the order to the authenticated client.
- Uses the order identifier as both tracking code and delivery confirmation code.

Success:

- HTTP `201`

```json
{
  "id": 1,
  "identifier": "DD-8F4A2B",
  "location": {
    "x": 3.0,
    "y": 4.0
  },
  "weight": 2.5,
  "priority": "MEDIUM",
  "status": "REQUESTED",
  "queuedAt": "2026-07-26T20:00:00Z",
  "confirmedDeliveryTime": "2026-07-26T18:30:00Z",
  "deliveryConfirmationCode": "DD-8F4A2B"
}
```

Errors:

- HTTP `401`: `authorization token is required`
- HTTP `400`: `location must not be null`
- HTTP `400`: `weight must be greater than zero`
- HTTP `400`: `confirmedDeliveryTime must not be null`
- HTTP `409`: `order identifier already exists`

### List Client Orders

```text
GET /api/client/orders
```

Behavior:

- Requires a valid client token.
- Returns only orders linked to the authenticated client.
- Includes the confirmation code because this is the customer's own order list.

Success:

- HTTP `200`

```json
[
  {
    "id": 1,
    "identifier": "DD-8F4A2B",
    "location": {
      "x": 3.0,
      "y": 4.0
    },
    "weight": 2.5,
    "priority": "MEDIUM",
    "status": "REQUESTED",
    "queuedAt": "2026-07-26T20:00:00Z",
    "confirmedDeliveryTime": "2026-07-26T18:30:00Z",
    "deliveryConfirmationCode": "DD-8F4A2B"
  }
]
```

Errors:

- HTTP `401`: `authorization token is required`
- HTTP `401`: `authorization token is invalid`
- HTTP `401`: `authorization token expired`

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
  "priority": "HIGH",
  "confirmedDeliveryTime": "2026-07-26T18:30:00Z"
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
  "queuedAt": "2026-07-25T20:00:00Z",
  "confirmedDeliveryTime": "2026-07-26T18:30:00Z",
  "deliveryConfirmationCode": "ORDER-1"
}
```

The create response includes `deliveryConfirmationCode` with the same value as `identifier`, so the customer can track the order and confirm receipt later with one code. List and detail order responses omit this field.
Order responses include `confirmedDeliveryTime` with the customer-confirmed delivery time and `statusReason` when a non-allocated, not-delivered or cancelled order has a message to show to the customer.

Errors:

- HTTP `400`: `identifier must not be blank`
- HTTP `400`: `location must not be null`
- HTTP `400`: `weight must be greater than zero`
- HTTP `400`: `priority must not be null`
- HTTP `400`: `confirmedDeliveryTime must not be null`
- HTTP `409`: `order identifier already exists`

### List Orders

```text
GET /api/orders
GET /api/orders?status=REQUESTED
```

Query parameters:

| Name | Required | Values |
| --- | --- | --- |
| `status` | No | `REQUESTED`, `ALLOCATED`, `IN_ROUTE`, `PENDING_REASSIGNMENT`, `DELIVERED`, `NOT_DELIVERED`, `CANCELLED`, `UNALLOCATED` |

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
    "queuedAt": "2026-07-25T20:00:00Z",
    "confirmedDeliveryTime": "2026-07-26T18:30:00Z"
  }
]
```

Errors:

- HTTP `400`: `status must be one of REQUESTED, ALLOCATED, IN_ROUTE, PENDING_REASSIGNMENT, DELIVERED, NOT_DELIVERED, CANCELLED, UNALLOCATED`

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
  "queuedAt": "2026-07-25T20:00:00Z",
  "confirmedDeliveryTime": "2026-07-26T18:30:00Z"
}
```

Errors:

- HTTP `404`: `order not found`

### Cancel Unallocated Order

```text
POST /api/orders/{id}/cancel
```

Request:

```json
{
  "reason": "Endereço fora da área atendida pela frota disponível."
}
```

Behavior:

- Only orders with status `UNALLOCATED` can be cancelled through this endpoint.
- The cancellation reason is stored as `statusReason` and is returned to the admin and customer views.

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
  "status": "CANCELLED",
  "queuedAt": "2026-07-25T20:00:00Z",
  "confirmedDeliveryTime": "2026-07-26T18:30:00Z",
  "statusReason": "Endereço fora da área atendida pela frota disponível."
}
```

Errors:

- HTTP `404`: `order not found`
- HTTP `400`: `request body must not be null`
- HTTP `400`: `cancel reason must not be blank`
- HTTP `400`: `order must be UNALLOCATED to cancel`

### Requeue Unallocated Order

```text
POST /api/orders/{id}/requeue
```

Behavior:

- Only orders with status `UNALLOCATED` can be returned to planning.
- The order status changes back to `REQUESTED` and the previous status message is cleared.

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
  "queuedAt": "2026-07-25T20:00:00Z",
  "confirmedDeliveryTime": "2026-07-26T18:30:00Z"
}
```

Errors:

- HTTP `404`: `order not found`
- HTTP `400`: `order must be UNALLOCATED to requeue`

### List Delivery Queue

```text
GET /api/delivery-queue
```

Behavior:

- Returns only orders with status `REQUESTED` or `PENDING_REASSIGNMENT`.
- Orders are sorted by `confirmedDeliveryTime`, priority, `queuedAt` and then by `id`.

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
    "queuedAt": "2026-07-25T20:00:00Z",
    "confirmedDeliveryTime": "2026-07-26T18:30:00Z"
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
- Reuses existing `PLANNED` trips before their ideal dispatch time when the recalculated route still respects weight, range, battery and active obstacles.
- Excludes drones that already have `IN_ROUTE` trips or `PLANNED` trips whose ideal dispatch window is already open, even when the drone status is still `AVAILABLE`.
- Uses persisted orders with status `REQUESTED` or `PENDING_REASSIGNMENT`.
- Uses active circular obstacles to adjust route segment distances when a segment would cross an obstacle area.
- Automatically moves available drones to the recharge queue when they have weight/range for requested orders but insufficient current battery for all of them.
- With `optimizeRoute=true`, automatically orders deliveries by confirmed delivery time, priority (`HIGH`, `MEDIUM`, `LOW`), higher weight, shorter distance from base, and then identifier.
- With `optimizeRoute=false`, preserves the delivery queue order inside planned trips; the queue is ordered by confirmed delivery time, priority, queue entry time and then persisted ID.
- Reserves each available drone for at most one planned trip in the current planning run.
- Calculates `idealDispatchTime` as the earliest value of `confirmedDeliveryTime - estimatedDeliveryTime` across unresolved route orders.
- Creates new trips with the smallest capable unused drone, preserving larger drones for heavier orders.
- When an order no longer fits in an already planned trip, immediately tries another still-unused capable drone.
- Marks the order as unallocated when it is individually serviceable but no immediate unused capable drone exists in the current planning run.
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

`estimatedDuration` is calculated as `(totalDistance / speed) * 60` from the assigned drone.
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
      "reason": "Pedido excede a capacidade máxima de peso dos drones disponíveis."
    }
  ]
}
```

Known unallocated reasons:

- `Pedido excede a capacidade máxima de peso dos drones disponíveis.`
- `Pedido excede o alcance máximo dos drones disponíveis.`
- `Pedido excede a capacidade máxima de peso e o alcance máximo dos drones disponíveis.`
- `Pedido exige mais bateria do que a frota disponível possui para concluir a rota e retornar em segurança.`
- `Pedido exige outro drone imediato, mas não há drone disponível nesta rodada de planejamento.`
- `Pedido não pode ser atendido por nenhum drone no planejamento atual.`

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
- Rejects the start while the planned trip is before `idealDispatchTime`.
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
- HTTP `400`: `trip must wait until ideal dispatch time`
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
- When the first tick reaches a `PLANNED` trip before `idealDispatchTime`, the trip remains stopped at the base.
- When a tick reaches a `PLANNED` trip at or after `idealDispatchTime`, the trip starts automatically if the drone is `AVAILABLE` and battery is sufficient for the saved route plus safe-return reserve.
- The simulation consumes battery by travelled distance, updates current drone position, requests customer availability when the drone enters the approach window, stops at reached route positions while waiting for customer confirmation, and completes the trip when the full route is finished after all route positions are resolved.
- If the customer does not respond to the availability request before `availabilityResponseDeadline`, the trip changes to `RETURNED_EARLY`, the current package becomes `NOT_DELIVERED` with a Portuguese `statusReason`, remaining undelivered packages become `PENDING_REASSIGNMENT`, and the drone returns to base.
- After availability is confirmed and the drone reaches the address, `deliveryConfirmationRequestedAt` starts a 1-minute window. If the customer does not enter the code before `deliveryConfirmationDeadline`, that package becomes `NOT_DELIVERED`, the route position is resolved as failed, and the drone continues the route carrying the package back to base.
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

### Confirm Route Availability

```text
POST /api/trips/{id}/route/{routePosition}/availability
```

Request:

```json
{
  "available": true
}
```

Behavior:

- Accepts customer availability only for trips with status `IN_ROUTE`.
- Requires the availability notification to have been sent for that route position.
- With `available: true`, stores `availabilityConfirmedAt` and allows delivery confirmation by code when the drone reaches the route position.
- With `available: false`, the drone returns to base, the trip changes to `RETURNED_EARLY`, and the current package changes to `NOT_DELIVERED` with `statusReason`.
- If the response arrives after `availabilityResponseDeadline`, it is treated as no response and the package becomes `NOT_DELIVERED`.

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
      "deliveredAt": null,
      "estimatedDeliveryTime": 10.0,
      "availabilityNotifiedAt": "2026-07-26T18:28:00Z",
      "availabilityConfirmedAt": "2026-07-26T18:28:10Z",
      "availabilityResponseDeadline": "2026-07-26T18:28:30Z",
      "deliveryConfirmationRequestedAt": null,
      "deliveryConfirmationDeadline": null,
      "deliveryFailedAt": null,
      "deliveryFailureReason": null
    }
  ],
  "totalWeight": 4.0,
  "totalDistance": 20.0,
  "estimatedDuration": 20.0,
  "averageDeliveryTime": 10.0
}
```

Errors:

- HTTP `404`: `trip not found`
- HTTP `404`: `trip route position not found`
- HTTP `400`: `request body must not be null`
- HTTP `400`: `available must not be null`
- HTTP `400`: `trip must be IN_ROUTE to confirm delivery availability`
- HTTP `400`: `routePosition must not be negative`
- HTTP `400`: `previous route positions must be delivered first`
- HTTP `400`: `route position already delivered`
- HTTP `400`: `delivery availability has not been requested yet`

### Report Route Delivery

```text
POST /api/trips/{id}/route/{routePosition}/deliver
```

Request:

```json
{
  "confirmationCode": "ORDER-1"
}
```

Behavior:

- Accepts delivery progress only for trips with status `IN_ROUTE`.
- Requires the customer delivery confirmation code, which is the same as the order tracking identifier.
- Requires customer availability to be confirmed for that route position.
- Requires the drone to have reached the route position before confirmation.
- The code can be entered only until `deliveryConfirmationDeadline`, 1 minute after `deliveryConfirmationRequestedAt`.
- Marks the route item at `routePosition` as delivered and stores `deliveredAt`.
- Changes the associated order status to `DELIVERED`.
- Requires previous route positions to be resolved first, either delivered or marked `NOT_DELIVERED` by the delivery-confirmation timeout.

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
      "deliveredAt": "2026-07-25T20:00:00Z",
      "estimatedDeliveryTime": 10.0,
      "availabilityNotifiedAt": "2026-07-26T18:28:00Z",
      "availabilityConfirmedAt": "2026-07-26T18:28:10Z",
      "availabilityResponseDeadline": "2026-07-26T18:28:30Z",
      "deliveryConfirmationRequestedAt": "2026-07-26T18:30:00Z",
      "deliveryConfirmationDeadline": "2026-07-26T18:31:00Z",
      "deliveryFailedAt": null,
      "deliveryFailureReason": null
    },
    {
      "orderId": 2,
      "routePosition": 1,
      "delivered": false,
      "deliveredAt": null,
      "estimatedDeliveryTime": 16.0,
      "availabilityNotifiedAt": null,
      "availabilityConfirmedAt": null,
      "availabilityResponseDeadline": null,
      "deliveryConfirmationRequestedAt": null,
      "deliveryConfirmationDeadline": null,
      "deliveryFailedAt": null,
      "deliveryFailureReason": null
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
- HTTP `400`: `request body must not be null`
- HTTP `400`: `trip must be IN_ROUTE to report delivery`
- HTTP `400`: `routePosition must not be negative`
- HTTP `400`: `previous route positions must be delivered first`
- HTTP `400`: `route position already delivered`
- HTTP `400`: `route position already marked not delivered`
- HTTP `400`: `delivery confirmation code must not be blank`
- HTTP `400`: `delivery availability must be confirmed before delivery`
- HTTP `400`: `delivery confirmation window expired`
- HTTP `400`: `delivery confirmation code is invalid`
- HTTP `400`: `drone has not reached route position yet`

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

- If the current battery can still cover the saved route and safe-return reserve and every route position has been resolved, the trip changes from `IN_ROUTE` to `COMPLETED`.
- In that complete path, the associated drone changes to `AVAILABLE`; associated orders are already `DELIVERED` from customer confirmations.
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
- HTTP `400`: `all route positions must be resolved before completing trip`

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

## Productivity Reports

### Monthly Productivity Report

```text
GET /api/reports/productivity/monthly
GET /api/reports/productivity/monthly?month=2026-07
```

Query parameters:

| Name | Required | Format | Default |
| --- | --- | --- | --- |
| `month` | No | `YYYY-MM` | Current month |

Behavior:

- Calculates and saves the requested month before returning it.
- Counts order entries, sent orders, delivered orders, cancelled orders and drone performance inside the selected monthly period.

Success:

- HTTP `200`

```json
{
  "month": "2026-07",
  "periodStart": "2026-07-01T03:00:00Z",
  "periodEnd": "2026-08-01T03:00:00Z",
  "orderEntries": 5,
  "ordersSent": 4,
  "ordersDelivered": 3,
  "ordersCancelled": 1,
  "conversionRate": 0.6,
  "drones": [
    {
      "droneId": 1,
      "droneIdentifier": "DRONE-1",
      "ordersDelivered": 3,
      "tripsStarted": 2,
      "tripsCompleted": 1,
      "tripsCancelled": 0,
      "tripsReturnedEarly": 0
    }
  ],
  "generatedAt": "2026-07-26T18:00:00Z"
}
```

Errors:

- HTTP `400`: `month must use YYYY-MM format`

### Monthly Productivity Report History

```text
GET /api/reports/productivity/monthly/history
```

Success:

- HTTP `200`

```json
[
  {
    "month": "2026-07",
    "periodStart": "2026-07-01T03:00:00Z",
    "periodEnd": "2026-08-01T03:00:00Z",
    "orderEntries": 5,
    "ordersSent": 4,
    "ordersDelivered": 3,
    "ordersCancelled": 1,
    "conversionRate": 0.6,
    "drones": [],
    "generatedAt": "2026-07-26T18:00:00Z"
  }
]
```
