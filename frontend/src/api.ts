import type {
  CreateDronePayload,
  CreateObstaclePayload,
  CreateOrderPayload,
  CreateReviewPayload,
  DashboardSnapshot,
  DeliveryQueueEntry,
  Drone,
  DroneAction,
  Obstacle,
  Order,
  ProductivityReport,
  RechargeQueueEntry,
  Review,
  Trip,
  TripAction,
  TripTelemetry,
  TripPlan,
  TripSimulation
} from "./types";

export interface DemoScenarioResult {
  drones: number;
  orders: number;
  obstacles: number;
  reviews: number;
  trips: number;
  unallocatedOrders: number;
}

export const API_UNAVAILABLE_MESSAGE =
  "API Spring indisponivel. Inicie o backend em http://localhost:8080 antes de operar o dashboard.";

async function fetchJson<T>(path: string): Promise<T> {
  let response: Response;

  try {
    response = await fetch(path, {
      headers: {
        Accept: "application/json"
      }
    });
  } catch {
    throw new Error(API_UNAVAILABLE_MESSAGE);
  }

  if (!response.ok) {
    throw new Error(await errorMessageFor(response, path));
  }

  return response.json() as Promise<T>;
}

async function postJson<T>(path: string, body?: unknown): Promise<T> {
  let response: Response;

  try {
    response = await fetch(path, {
      method: "POST",
      headers: {
        Accept: "application/json",
        ...(body === undefined ? {} : { "Content-Type": "application/json" })
      },
      body: body === undefined ? undefined : JSON.stringify(body)
    });
  } catch {
    throw new Error(API_UNAVAILABLE_MESSAGE);
  }

  if (!response.ok) {
    throw new Error(await errorMessageFor(response, path));
  }

  return response.json() as Promise<T>;
}

async function deleteJson<T>(path: string): Promise<T> {
  let response: Response;

  try {
    response = await fetch(path, {
      method: "DELETE",
      headers: {
        Accept: "application/json"
      }
    });
  } catch {
    throw new Error(API_UNAVAILABLE_MESSAGE);
  }

  if (!response.ok) {
    throw new Error(await errorMessageFor(response, path));
  }

  return response.json() as Promise<T>;
}

async function errorMessageFor(response: Response, path: string) {
  const contentType = response.headers.get("content-type") ?? "";
  let responseBody = "";

  try {
    responseBody = await response.text();
  } catch {
    responseBody = "";
  }

  if (contentType.includes("application/json") && responseBody.trim()) {
    try {
      const body = JSON.parse(responseBody) as { message?: string };
      if (body.message) {
        return body.message;
      }
    } catch {
      // Keep the plain response fallback below.
    }
  }

  const trimmedBody = responseBody.trim();
  if (isLikelyProxyUnavailable(response, trimmedBody)) {
    return API_UNAVAILABLE_MESSAGE;
  }

  if (trimmedBody) {
    return `${path} retornou HTTP ${response.status}: ${trimmedBody}`;
  }

  return `${path} retornou HTTP ${response.status}`;
}

function isLikelyProxyUnavailable(response: Response, responseBody: string) {
  if (![500, 502, 503, 504].includes(response.status)) {
    return false;
  }

  if (!responseBody) {
    return true;
  }

  const normalizedBody = responseBody.toLowerCase();
  return (
    normalizedBody.includes("econnrefused") ||
    normalizedBody.includes("enotfound") ||
    normalizedBody.includes("etimedout") ||
    normalizedBody.includes("proxy error") ||
    normalizedBody.includes("error occurred while trying to proxy")
  );
}

export function isApiUnavailableError(exception: unknown) {
  return exception instanceof Error && exception.message === API_UNAVAILABLE_MESSAGE;
}

export async function loadDashboardSnapshot(): Promise<DashboardSnapshot> {
  const [drones, orders, trips, deliveryQueue, obstacles, rechargeQueue, reviews, productivityReport] = await Promise.all([
    fetchJson<Drone[]>("/api/drones"),
    fetchJson<Order[]>("/api/orders"),
    fetchJson<Trip[]>("/api/trips"),
    fetchJson<DeliveryQueueEntry[]>("/api/delivery-queue"),
    fetchJson<Obstacle[]>("/api/obstacles"),
    fetchJson<RechargeQueueEntry[]>("/api/recharge-queue"),
    fetchJson<Review[]>("/api/reviews"),
    fetchJson<ProductivityReport>("/api/reports/productivity/monthly")
  ]);

  return {
    drones,
    orders,
    trips,
    deliveryQueue,
    obstacles,
    rechargeQueue,
    reviews,
    productivityReport
  };
}

export async function performDroneAction(id: number, action: DroneAction): Promise<Drone> {
  const pathByAction: Record<DroneAction, string> = {
    markUnavailable: `/api/drones/${id}/unavailable`,
    markAvailable: `/api/drones/${id}/available`,
    enqueueRecharge: `/api/drones/${id}/recharge`,
    completeRecharge: `/api/drones/${id}/recharge/complete`
  };

  return postJson<Drone>(pathByAction[action]);
}

export async function createDrone(payload: CreateDronePayload): Promise<Drone> {
  return postJson<Drone>("/api/drones", payload);
}

export async function createOrder(payload: CreateOrderPayload): Promise<Order> {
  return postJson<Order>("/api/orders", payload);
}

export async function createObstacle(payload: CreateObstaclePayload): Promise<Obstacle> {
  return postJson<Obstacle>("/api/obstacles", payload);
}

export async function createReview(payload: CreateReviewPayload): Promise<Review> {
  return postJson<Review>("/api/reviews", payload);
}

export async function createDemoScenario(): Promise<DemoScenarioResult> {
  return postJson<DemoScenarioResult>("/internal/demo/reset-and-seed?confirmation=RESET_DEMO_DATA");
}

export async function deactivateObstacle(id: number): Promise<Obstacle> {
  return deleteJson<Obstacle>(`/api/obstacles/${id}`);
}

export async function planTrips(optimizeRoute: boolean): Promise<TripPlan> {
  return postJson<TripPlan>(`/api/trip-plans?optimizeRoute=${optimizeRoute}`);
}

export async function loadTripTelemetryHistory(id: number): Promise<TripTelemetry[]> {
  return fetchJson<TripTelemetry[]>(`/api/trips/${id}/telemetry`);
}

export async function loadTripSimulation(id: number): Promise<TripSimulation> {
  return fetchJson<TripSimulation>(`/api/trips/${id}/simulation`);
}

export async function advanceTripSimulation(id: number, elapsedMinutes = 1): Promise<TripSimulation> {
  return postJson<TripSimulation>(`/api/trips/${id}/simulation/tick`, { elapsedMinutes });
}

export interface TripActionOptions {
  routePosition?: number;
  batteryLevel?: number;
}

export async function performTripAction(id: number, action: TripAction, options: TripActionOptions = {}): Promise<Trip> {
  if (action === "deliverNext") {
    if (options.routePosition === undefined) {
      throw new Error("Posicao da rota nao informada");
    }

    return postJson<Trip>(`/api/trips/${id}/route/${options.routePosition}/deliver`);
  }

  if (action === "sendTelemetry") {
    if (options.batteryLevel === undefined) {
      throw new Error("Bateria nao informada");
    }

    return postJson<Trip>(`/api/trips/${id}/telemetry`, { batteryLevel: options.batteryLevel });
  }

  const pathByAction: Record<Exclude<TripAction, "deliverNext" | "sendTelemetry">, string> = {
    start: `/api/trips/${id}/start`,
    complete: `/api/trips/${id}/complete`,
    cancel: `/api/trips/${id}/cancel`
  };

  return postJson<Trip>(pathByAction[action]);
}
