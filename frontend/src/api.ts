import type {
  CreateDronePayload,
  ClientAuthPayload,
  ClientAuthResponse,
  ClientRegisterPayload,
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
  clients: number;
  trips: number;
  unallocatedOrders: number;
}

export const API_UNAVAILABLE_MESSAGE =
  "API Spring indisponível. Inicie o backend em http://localhost:8080 antes de operar o dashboard.";

const INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
const internalApiKey = import.meta.env.VITE_INTERNAL_API_KEY ?? "dev-internal-key";

async function fetchJson<T>(path: string, authToken?: string): Promise<T> {
  let response: Response;

  try {
    response = await fetch(path, {
      headers: requestHeaders(path, false, authToken)
    });
  } catch {
    throw new Error(API_UNAVAILABLE_MESSAGE);
  }

  if (!response.ok) {
    throw new Error(await errorMessageFor(response, path));
  }

  return response.json() as Promise<T>;
}

async function postJson<T>(path: string, body?: unknown, authToken?: string): Promise<T> {
  let response: Response;

  try {
    response = await fetch(path, {
      method: "POST",
      headers: requestHeaders(path, body !== undefined, authToken),
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
      headers: requestHeaders(path)
    });
  } catch {
    throw new Error(API_UNAVAILABLE_MESSAGE);
  }

  if (!response.ok) {
    throw new Error(await errorMessageFor(response, path));
  }

  return response.json() as Promise<T>;
}

function requestHeaders(path: string, hasJsonBody = false, authToken?: string) {
  return {
    Accept: "application/json",
    ...(hasJsonBody ? { "Content-Type": "application/json" } : {}),
    ...(path.startsWith("/internal/") ? { [INTERNAL_API_KEY_HEADER]: internalApiKey } : {}),
    ...(authToken ? { Authorization: `Bearer ${authToken}` } : {})
  };
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

export async function loadDashboardSnapshot(reportMonth?: string): Promise<DashboardSnapshot> {
  const [drones, orders, trips, deliveryQueue, obstacles, rechargeQueue, reviews, productivityReport] = await Promise.all([
    fetchJson<Drone[]>("/api/drones"),
    fetchJson<Order[]>("/api/orders"),
    fetchJson<Trip[]>("/api/trips"),
    fetchJson<DeliveryQueueEntry[]>("/api/delivery-queue"),
    fetchJson<Obstacle[]>("/api/obstacles"),
    fetchJson<RechargeQueueEntry[]>("/api/recharge-queue"),
    fetchJson<Review[]>("/api/reviews"),
    loadMonthlyProductivityReport(reportMonth)
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

export async function loadMonthlyProductivityReport(month?: string): Promise<ProductivityReport> {
  const query = month ? `?month=${encodeURIComponent(month)}` : "";
  return fetchJson<ProductivityReport>(`/api/reports/productivity/monthly${query}`);
}

export async function performDroneAction(id: number, action: DroneAction): Promise<Drone> {
  if (action === "delete") {
    return deleteJson<Drone>(`/api/drones/${id}`);
  }

  const pathByAction: Record<DroneAction, string> = {
    markUnavailable: `/api/drones/${id}/unavailable`,
    markAvailable: `/api/drones/${id}/available`,
    enqueueRecharge: `/api/drones/${id}/recharge`,
    completeRecharge: `/api/drones/${id}/recharge/complete`,
    delete: `/api/drones/${id}`
  };

  return postJson<Drone>(pathByAction[action]);
}

export async function createDrone(payload: CreateDronePayload): Promise<Drone> {
  return postJson<Drone>("/api/drones", payload);
}

export async function createOrder(payload: CreateOrderPayload): Promise<Order> {
  return postJson<Order>("/api/orders", payload);
}

export async function registerClient(payload: ClientRegisterPayload): Promise<ClientAuthResponse> {
  return postJson<ClientAuthResponse>("/api/auth/register", payload);
}

export async function loginClient(payload: ClientAuthPayload): Promise<ClientAuthResponse> {
  return postJson<ClientAuthResponse>("/api/auth/login", payload);
}

export async function loadCurrentClient(authToken: string): Promise<ClientAuthResponse["user"]> {
  return fetchJson<ClientAuthResponse["user"]>("/api/auth/me", authToken);
}

export async function loadClientOrders(authToken: string): Promise<Order[]> {
  return fetchJson<Order[]>("/api/client/orders", authToken);
}

export async function createClientOrder(payload: CreateOrderPayload, authToken: string): Promise<Order> {
  return postJson<Order>("/api/client/orders", payload, authToken);
}

export async function confirmTripRouteDelivery(
  tripId: number,
  routePosition: number,
  confirmationCode: string
): Promise<Trip> {
  return postJson<Trip>(`/api/trips/${tripId}/route/${routePosition}/deliver`, { confirmationCode });
}

export async function confirmTripRouteAvailability(
  tripId: number,
  routePosition: number,
  available: boolean
): Promise<Trip> {
  return postJson<Trip>(`/api/trips/${tripId}/route/${routePosition}/availability`, { available });
}

export async function cancelOrder(id: number, reason: string): Promise<Order> {
  return postJson<Order>(`/api/orders/${id}/cancel`, { reason });
}

export async function requeueOrder(id: number): Promise<Order> {
  return postJson<Order>(`/api/orders/${id}/requeue`);
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
  confirmationCode?: string;
}

export async function performTripAction(id: number, action: TripAction, options: TripActionOptions = {}): Promise<Trip> {
  if (action === "deliverNext") {
    if (options.routePosition === undefined) {
      throw new Error("Posição da rota não informada");
    }

    if (options.confirmationCode === undefined) {
      throw new Error("Código de confirmação não informado");
    }

    return confirmTripRouteDelivery(id, options.routePosition, options.confirmationCode);
  }

  if (action === "sendTelemetry") {
    if (options.batteryLevel === undefined) {
      throw new Error("Bateria não informada");
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
