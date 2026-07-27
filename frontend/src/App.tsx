import {
  AlertTriangle,
  Ban,
  BarChart3,
  BatteryFull,
  BatteryCharging,
  CalendarDays,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Circle,
  Clock3,
  Eye,
  Flag,
  KeyRound,
  ListChecks,
  LogIn,
  LogOut,
  MapPin,
  MessageSquareText,
  PackageCheck,
  Plane,
  Play,
  Plus,
  PowerOff,
  RefreshCcw,
  Route,
  Ruler,
  Search,
  Send,
  Star,
  Trash2,
  UserPlus,
  Weight,
  XCircle
} from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import {
  advanceTripSimulation,
  cancelOrder,
  confirmTripRouteDelivery,
  confirmTripRouteAvailability,
  createDemoScenario,
  createDrone,
  createClientOrder,
  createObstacle,
  createOrder,
  createReview,
  deactivateObstacle,
  isApiUnavailableError,
  loadDashboardSnapshot,
  loadClientOrders,
  loadCurrentClient,
  loadTripTelemetryHistory,
  loginClient,
  performDroneAction,
  performTripAction,
  planTrips,
  requeueOrder,
  registerClient,
  type DemoScenarioResult
} from "./api";
import type {
  ClientAuthPayload,
  ClientRegisterPayload,
  ClientUser,
  CreateDronePayload,
  CreateObstaclePayload,
  CreateOrderPayload,
  CreateReviewPayload,
  DashboardSnapshot,
  DeliveryQueueEntry,
  Drone,
  DroneAction,
  DroneStatus,
  Obstacle,
  Order,
  OrderStatus,
  Priority,
  ProductivityReport,
  RechargeQueueEntry,
  Review,
  Trip,
  TripAction,
  TripSimulation,
  TripTelemetry,
  TripPlan,
  TripStatus
} from "./types";

const emptySnapshot: DashboardSnapshot = {
  drones: [],
  orders: [],
  trips: [],
  deliveryQueue: [],
  obstacles: [],
  rechargeQueue: [],
  reviews: [],
  productivityReport: null
};

const droneStatuses: DroneStatus[] = ["AVAILABLE", "IN_ROUTE", "CHARGING", "UNAVAILABLE"];
const orderStatuses: OrderStatus[] = [
  "REQUESTED",
  "ALLOCATED",
  "IN_ROUTE",
  "PENDING_REASSIGNMENT",
  "DELIVERED",
  "NOT_DELIVERED",
  "UNALLOCATED",
  "CANCELLED"
];
const tripStatuses: TripStatus[] = ["PLANNED", "IN_ROUTE", "RETURNED_EARLY", "COMPLETED", "CANCELLED"];
const priorities: Priority[] = ["HIGH", "MEDIUM", "LOW"];
const tableViews = ["drones", "orders", "trips"] as const;
const mapRouteModes = ["selected", "all"] as const;
const simulationTickIntervalMs = 2500;
const simulationTickMinutes = 1;
const approachNotificationWindowMinutes = 2;
const clientAuthTokenStorageKey = "droneDelivery.clientAuthToken";
const mapViewportMinimumSizeKilometers = 0.12;
const mapViewportPaddingRatio = 0.28;
const mapObstacleContextRatio = 0.7;

type Experience = "admin" | "client";
type AdminSection = "overview" | "operations" | "planning" | "feedback";
type ClientSection = "order" | "myOrders" | "tracking" | "reviews";
type ClientAuthMode = "login" | "register";
type TableView = (typeof tableViews)[number];
type MapRouteMode = (typeof mapRouteModes)[number];
type PlanningAction = "createDrone" | "createOrder" | "createObstacle" | "createReview" | "planTrips";
type OrderAction = "cancel" | "requeue";
type JourneyStatus = "done" | "ready" | "optional" | "pending";
type ApiStatus = "checking" | "online" | "offline";
type ActionMessageTone = "success" | "error" | "info";
type RouteProgressStatus = "DELIVERED" | "NOT_DELIVERED" | "PENDING";
type ObstacleDisplayStatus = "ACTIVE" | "INACTIVE";

const droneStatusLabels: Record<DroneStatus, string> = {
  AVAILABLE: "Disponível",
  IN_ROUTE: "Em rota",
  CHARGING: "Em recarga",
  UNAVAILABLE: "Indisponível"
};

const orderStatusLabels: Record<OrderStatus, string> = {
  REQUESTED: "Solicitado",
  ALLOCATED: "Alocado",
  IN_ROUTE: "Em rota",
  PENDING_REASSIGNMENT: "Aguardando reatribuição",
  DELIVERED: "Entregue",
  NOT_DELIVERED: "Não entregue",
  CANCELLED: "Cancelado",
  UNALLOCATED: "Não alocado"
};

const tripStatusLabels: Record<TripStatus, string> = {
  PLANNED: "Planejada",
  IN_ROUTE: "Em rota",
  RETURNED_EARLY: "Retorno antecipado",
  COMPLETED: "Concluída",
  CANCELLED: "Cancelada"
};

const routeProgressStatusLabels: Record<RouteProgressStatus, string> = {
  DELIVERED: "Entregue",
  NOT_DELIVERED: "Não entregue",
  PENDING: "Pendente"
};

const obstacleStatusLabels: Record<ObstacleDisplayStatus, string> = {
  ACTIVE: "Ativo",
  INACTIVE: "Inativo"
};

const mapRouteModeLabels: Record<MapRouteMode, string> = {
  selected: "Viagem selecionada",
  all: "Todas as viagens"
};

const tripRouteColors = ["#15616d", "#2b5c9e", "#a56013", "#257a4f", "#7a4f9a", "#aa2e25", "#3f6f73", "#7c5b2f"];
const measurementUnits = {
  weight: "kg",
  distance: "km",
  speed: "km/h"
} as const;

interface TripActionOptions {
  routePosition?: number;
  batteryLevel?: number;
  confirmationCode?: string;
}

interface OrderCancellationDraft {
  orderId: number;
  identifier: string;
  reason: string;
}

interface JourneyStep {
  title: string;
  detail: string;
  status: JourneyStatus;
  href: string;
  actionLabel: string;
  table?: TableView;
  icon: React.ReactNode;
}

interface DroneFormState {
  identifier: string;
  maxWeightCapacity: string;
  maxRange: string;
  batteryLevel: string;
  batteryConsumptionPerDistanceUnit: string;
  minimumReturnBattery: string;
  speed: string;
  chargingRate: string;
}

interface OrderFormState {
  identifier: string;
  x: string;
  y: string;
  weight: string;
  priority: Priority;
  confirmedDeliveryTime: string;
}

interface ObstacleFormState {
  x: string;
  y: string;
  radius: string;
}

interface ReviewFormState {
  stars: number;
  title: string;
  feedback: string;
}

interface ClientOrderFormState {
  x: string;
  y: string;
  weight: string;
  confirmedDeliveryTime: string;
}

interface ClientAuthFormState {
  name: string;
  email: string;
  password: string;
}

interface ArrivalNotificationState {
  key: string;
  tripId: number;
  routePosition: number;
  orderId: number;
  orderIdentifier: string;
  droneId: number;
  deadline: string | null;
}

interface ClientOrderListItem {
  identifier: string;
  order: Order;
  trip: Trip | null;
  routeProgress: Trip["routeProgress"][number] | null;
  selected: boolean;
}

interface MapPoint {
  x: number;
  y: number;
}

interface MapRoutePoint extends MapPoint {
  key: string;
  label: string;
  orderId?: number;
  routePosition?: number;
}

interface MapViewport {
  minX: number;
  minY: number;
  size: number;
}

interface MapBounds {
  minX: number;
  maxX: number;
  minY: number;
  maxY: number;
}

interface MapRouteSegment {
  key: string;
  left: number;
  top: number;
  length: number;
  angle: number;
  fromLabel: string;
  toLabel: string;
}

interface MapRouteLayer {
  trip: Trip;
  color: string;
  selected: boolean;
  routeSegments: MapRouteSegment[];
}

interface MapOrderHighlight {
  color: string;
  tripId: number;
  routePosition: number;
  selected: boolean;
}

const initialDroneForm: DroneFormState = {
  identifier: "",
  maxWeightCapacity: "",
  maxRange: "",
  batteryLevel: "",
  batteryConsumptionPerDistanceUnit: "",
  minimumReturnBattery: "",
  speed: "",
  chargingRate: ""
};

const initialOrderForm: OrderFormState = {
  identifier: "",
  x: "",
  y: "",
  weight: "",
  priority: "HIGH",
  confirmedDeliveryTime: ""
};

const initialObstacleForm: ObstacleFormState = {
  x: "",
  y: "",
  radius: ""
};

const initialReviewForm: ReviewFormState = {
  stars: 5,
  title: "",
  feedback: ""
};

const initialClientOrderForm: ClientOrderFormState = {
  x: "",
  y: "",
  weight: "",
  confirmedDeliveryTime: ""
};

const initialClientAuthForm: ClientAuthFormState = {
  name: "",
  email: "",
  password: ""
};

function App() {
  const [activeExperience, setActiveExperience] = useState<Experience>("admin");
  const [activeAdminSection, setActiveAdminSection] = useState<AdminSection>("overview");
  const [activeClientSection, setActiveClientSection] = useState<ClientSection>("order");
  const [snapshot, setSnapshot] = useState<DashboardSnapshot>(emptySnapshot);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [apiStatus, setApiStatus] = useState<ApiStatus>("checking");
  const [updatedAt, setUpdatedAt] = useState<Date | null>(null);
  const [reportMonth, setReportMonth] = useState(currentReportMonth());
  const [reportMonthLoading, setReportMonthLoading] = useState(false);
  const [activeTable, setActiveTable] = useState<TableView>("drones");
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [droneActionInFlight, setDroneActionInFlight] = useState<{ id: number; action: DroneAction } | null>(null);
  const [orderActionInFlight, setOrderActionInFlight] = useState<{ id: number; action: OrderAction } | null>(null);
  const [tripActionInFlight, setTripActionInFlight] = useState<{ id: number; action: TripAction } | null>(null);
  const [planningActionInFlight, setPlanningActionInFlight] = useState<PlanningAction | null>(null);
  const [obstacleActionInFlight, setObstacleActionInFlight] = useState<number | null>(null);
  const [demoActionInFlight, setDemoActionInFlight] = useState(false);
  const [demoConfirmationOpen, setDemoConfirmationOpen] = useState(false);
  const [orderCancellationDraft, setOrderCancellationDraft] = useState<OrderCancellationDraft | null>(null);
  const [arrivalNotification, setArrivalNotification] = useState<ArrivalNotificationState | null>(null);
  const [availabilityResponseInFlight, setAvailabilityResponseInFlight] = useState(false);
  const [actionMessage, setActionMessage] = useState<{ tone: ActionMessageTone; text: string } | null>(null);
  const [telemetryDrafts, setTelemetryDrafts] = useState<Record<number, string>>({});
  const [droneForm, setDroneForm] = useState<DroneFormState>(initialDroneForm);
  const [orderForm, setOrderForm] = useState<OrderFormState>(initialOrderForm);
  const [clientOrderForm, setClientOrderForm] = useState<ClientOrderFormState>(initialClientOrderForm);
  const [clientTrackingTerm, setClientTrackingTerm] = useState("");
  const [clientAuthToken, setClientAuthToken] = useState<string | null>(() => loadClientAuthToken());
  const [clientUser, setClientUser] = useState<ClientUser | null>(null);
  const [clientOrders, setClientOrders] = useState<Order[]>([]);
  const [clientOrdersLoading, setClientOrdersLoading] = useState(false);
  const [clientAuthMode, setClientAuthMode] = useState<ClientAuthMode>("login");
  const [clientAuthForm, setClientAuthForm] = useState<ClientAuthFormState>(initialClientAuthForm);
  const [clientAuthInFlight, setClientAuthInFlight] = useState(false);
  const [trackingCodeDialog, setTrackingCodeDialog] = useState<string | null>(null);
  const [deliveryConfirmationCode, setDeliveryConfirmationCode] = useState("");
  const [deliveryConfirmationInFlight, setDeliveryConfirmationInFlight] = useState<number | null>(null);
  const [obstacleForm, setObstacleForm] = useState<ObstacleFormState>(initialObstacleForm);
  const [reviewForm, setReviewForm] = useState<ReviewFormState>(initialReviewForm);
  const [optimizeRoute, setOptimizeRoute] = useState(true);
  const [selectedTripId, setSelectedTripId] = useState<number | null>(null);
  const [mapRouteMode, setMapRouteMode] = useState<MapRouteMode>("selected");
  const [telemetryHistory, setTelemetryHistory] = useState<TripTelemetry[]>([]);
  const [telemetryLoading, setTelemetryLoading] = useState(false);
  const [telemetryError, setTelemetryError] = useState<string | null>(null);
  const snapshotRef = useRef(snapshot);
  const approachNotificationKeysRef = useRef<Set<string>>(new Set());
  const availabilityNotificationKeysRef = useRef<Set<string>>(new Set());

  useEffect(() => {
    snapshotRef.current = snapshot;
  }, [snapshot]);

  async function refresh(showLoading = true, requestedReportMonth = reportMonth) {
    if (showLoading) {
      setLoading(true);
    }
    setError(null);

    try {
      const data = await loadDashboardSnapshot(requestedReportMonth);
      setSnapshot(data);
      setApiStatus("online");
      setUpdatedAt(new Date());
      if (clientAuthToken) {
        await refreshClientOrders(clientAuthToken);
      }
    } catch (exception) {
      setApiStatus(isApiUnavailableError(exception) ? "offline" : "online");
      setError(exception instanceof Error ? exception.message : "Falha ao carregar dados");
    } finally {
      if (showLoading) {
        setLoading(false);
      }
    }
  }

  function operationErrorMessageFor(exception: unknown, fallback: string) {
    if (isApiUnavailableError(exception)) {
      setApiStatus("offline");
    }

    return exception instanceof Error ? exception.message : fallback;
  }

  function handleExperienceChange(experience: Experience) {
    setActiveExperience(experience);
    setActionMessage(null);
    setError(null);
    if (experience !== "client") {
      setArrivalNotification(null);
    }
  }

  async function handleReportMonthChange(month: string) {
    if (!month) {
      return;
    }

    setReportMonth(month);
    setReportMonthLoading(true);

    try {
      await refresh(false, month);
    } finally {
      setReportMonthLoading(false);
    }
  }

  function handleReportMonthStep(offset: number) {
    void handleReportMonthChange(shiftReportMonth(reportMonth, offset));
  }

  function handleClientOrderSelect(identifier: string) {
    setClientTrackingTerm(identifier);
    setActiveClientSection("tracking");
  }

  async function refreshClientSession(authToken: string) {
    setClientOrdersLoading(true);

    try {
      const [user, orders] = await Promise.all([
        loadCurrentClient(authToken),
        loadClientOrders(authToken)
      ]);
      setClientUser(user);
      setClientOrders(orders);
      setApiStatus("online");
    } catch (exception) {
      clearClientSession();
      if (activeExperience === "client") {
        setActionMessage({
          tone: "error",
          text: operationErrorMessageFor(exception, "Sessão do cliente expirada")
        });
      }
    } finally {
      setClientOrdersLoading(false);
    }
  }

  async function refreshClientOrders(authToken: string) {
    setClientOrdersLoading(true);

    try {
      setClientOrders(await loadClientOrders(authToken));
    } catch (exception) {
      clearClientSession();
      if (activeExperience === "client") {
        setActionMessage({
          tone: "error",
          text: operationErrorMessageFor(exception, "Falha ao carregar meus pedidos")
        });
      }
    } finally {
      setClientOrdersLoading(false);
    }
  }

  function clearClientSession() {
    saveClientAuthToken(null);
    setClientAuthToken(null);
    setClientUser(null);
    setClientOrders([]);
    setClientTrackingTerm("");
    setDeliveryConfirmationCode("");
  }

  async function handleClientAuthSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setClientAuthInFlight(true);
    setActionMessage(null);
    setError(null);

    try {
      const response = clientAuthMode === "register"
        ? await registerClient(toClientRegisterPayload(clientAuthForm))
        : await loginClient(toClientAuthPayload(clientAuthForm));

      saveClientAuthToken(response.token);
      setClientAuthToken(response.token);
      setClientUser(response.user);
      setClientAuthForm(initialClientAuthForm);
      setActiveClientSection("myOrders");
      setActionMessage({
        tone: "success",
        text: `${response.user.name} conectado.`
      });
      await refreshClientOrders(response.token);
    } catch (exception) {
      setActionMessage({
        tone: "error",
        text: operationErrorMessageFor(exception, clientAuthMode === "register" ? "Falha ao criar conta" : "Falha ao entrar")
      });
    } finally {
      setClientAuthInFlight(false);
    }
  }

  function handleClientLogout() {
    clearClientSession();
    setActiveClientSection("order");
    setActionMessage({
      tone: "info",
      text: "Sessão encerrada."
    });
  }

  async function handleDroneAction(id: number, action: DroneAction) {
    setDroneActionInFlight({ id, action });
    setActionMessage(null);
    setError(null);

    try {
      const drone = await performDroneAction(id, action);
      setActionMessage({
        tone: "success",
        text: droneSuccessMessageFor(drone, action)
      });
      await refresh(false);
    } catch (exception) {
      setActionMessage({
        tone: "error",
        text: operationErrorMessageFor(exception, "Falha ao executar ação")
      });
    } finally {
      setDroneActionInFlight(null);
    }
  }

  async function handleOrderRequeue(id: number) {
    setOrderActionInFlight({ id, action: "requeue" });
    setActionMessage(null);
    setError(null);

    try {
      const order = await requeueOrder(id);
      setActionMessage({
        tone: "success",
        text: `${order.identifier} voltou para a fila de planejamento.`
      });
      await refresh(false);
    } catch (exception) {
      setActionMessage({
        tone: "error",
        text: operationErrorMessageFor(exception, "Falha ao reenviar pedido")
      });
    } finally {
      setOrderActionInFlight(null);
    }
  }

  async function handleCancelUnallocatedOrder() {
    if (!orderCancellationDraft) {
      return;
    }

    setOrderActionInFlight({ id: orderCancellationDraft.orderId, action: "cancel" });
    setActionMessage(null);
    setError(null);

    try {
      const order = await cancelOrder(orderCancellationDraft.orderId, orderCancellationDraft.reason);
      setOrderCancellationDraft(null);
      setActionMessage({
        tone: "success",
        text: `${order.identifier} cancelado com justificativa.`
      });
      await refresh(false);
    } catch (exception) {
      setActionMessage({
        tone: "error",
        text: operationErrorMessageFor(exception, "Falha ao cancelar pedido")
      });
    } finally {
      setOrderActionInFlight(null);
    }
  }

  async function handleTripAction(id: number, action: TripAction, options?: TripActionOptions) {
    setTripActionInFlight({ id, action });
    setActionMessage(null);
    setError(null);

    try {
      const trip = await performTripAction(id, action, options);
      if (action === "sendTelemetry") {
        setTelemetryDrafts((drafts) => ({ ...drafts, [id]: "" }));
      }
      setActionMessage({
        tone: "success",
        text: tripSuccessMessageFor(trip, action, options)
      });
      await refresh(false);
      if (selectedTripId === id) {
        await loadTelemetryForTrip(id);
      }
    } catch (exception) {
      setActionMessage({
        tone: "error",
        text: operationErrorMessageFor(exception, "Falha ao executar ação")
      });
    } finally {
      setTripActionInFlight(null);
    }
  }

  async function loadTelemetryForTrip(id: number) {
    setTelemetryLoading(true);
    setTelemetryError(null);

    try {
      setTelemetryHistory(await loadTripTelemetryHistory(id));
    } catch (exception) {
      setTelemetryHistory([]);
      setTelemetryError(operationErrorMessageFor(exception, "Falha ao carregar telemetria"));
    } finally {
      setTelemetryLoading(false);
    }
  }

  async function handleCreateDrone(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPlanningActionInFlight("createDrone");
    setActionMessage(null);
    setError(null);

    try {
      const drone = await createDrone(toDronePayload(droneForm));
      setDroneForm(initialDroneForm);
      setActionMessage({
        tone: "success",
        text: `${drone.identifier} cadastrado.`
      });
      await refresh(false);
      setActiveTable("drones");
    } catch (exception) {
      setActionMessage({
        tone: "error",
        text: operationErrorMessageFor(exception, "Falha ao cadastrar drone")
      });
    } finally {
      setPlanningActionInFlight(null);
    }
  }

  async function handleCreateOrder(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPlanningActionInFlight("createOrder");
    setActionMessage(null);
    setError(null);

    try {
      const order = await createOrder(toOrderPayload(orderForm));
      setOrderForm(initialOrderForm);
      setActionMessage({
        tone: "success",
        text: `${order.identifier} cadastrado.`
      });
      await refresh(false);
      setActiveTable("orders");
    } catch (exception) {
      setActionMessage({
        tone: "error",
        text: operationErrorMessageFor(exception, "Falha ao cadastrar pedido")
      });
    } finally {
      setPlanningActionInFlight(null);
    }
  }

  async function handleCreateClientOrder(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPlanningActionInFlight("createOrder");
    setActionMessage(null);
    setError(null);

    try {
      if (!clientAuthToken) {
        throw new Error("Faça login para solicitar entregas.");
      }

      const trackingCode = generateTrackingCode(snapshot.orders);
      const order = await createClientOrder(toClientOrderPayload(clientOrderForm, trackingCode), clientAuthToken);
      setClientOrderForm(initialClientOrderForm);
      setClientTrackingTerm(order.identifier);
      setTrackingCodeDialog(order.identifier);
      setActionMessage({
        tone: "success",
        text: `Pedido ${order.identifier} solicitado.`
      });
      await refresh(false);
    } catch (exception) {
      setActionMessage({
        tone: "error",
        text: operationErrorMessageFor(exception, "Falha ao solicitar entrega")
      });
    } finally {
      setPlanningActionInFlight(null);
    }
  }

  async function handleConfirmClientDelivery(tripId: number, routePosition: number) {
    setDeliveryConfirmationInFlight(tripId);
    setActionMessage(null);
    setError(null);

    try {
      const trip = await confirmTripRouteDelivery(tripId, routePosition, deliveryConfirmationCode);
      setDeliveryConfirmationCode("");
      setActionMessage({
        tone: "success",
        text: `Entrega da viagem #${trip.id} confirmada pelo cliente.`
      });
      await refresh(false);
    } catch (exception) {
      setActionMessage({
        tone: "error",
        text: operationErrorMessageFor(exception, "Falha ao confirmar entrega")
      });
    } finally {
      setDeliveryConfirmationInFlight(null);
    }
  }

  async function handleDeliveryAvailabilityResponse(available: boolean) {
    if (!arrivalNotification) {
      return;
    }

    setAvailabilityResponseInFlight(true);
    setActionMessage(null);
    setError(null);

    try {
      const trip = await confirmTripRouteAvailability(
        arrivalNotification.tripId,
        arrivalNotification.routePosition,
        available
      );
      setArrivalNotification(null);
      setActionMessage({
        tone: available ? "success" : "info",
        text: available
          ? `Disponibilidade confirmada para a viagem #${trip.id}. Informe o código quando o drone parar no endereço.`
          : `O drone da viagem #${trip.id} está retornando para a base com o pacote.`
      });
      await refresh(false);
    } catch (exception) {
      setActionMessage({
        tone: "error",
        text: operationErrorMessageFor(exception, "Falha ao responder disponibilidade")
      });
    } finally {
      setAvailabilityResponseInFlight(false);
    }
  }

  async function handleCreateObstacle(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPlanningActionInFlight("createObstacle");
    setActionMessage(null);
    setError(null);

    try {
      const obstacle = await createObstacle(toObstaclePayload(obstacleForm));
      setObstacleForm(initialObstacleForm);
      setActionMessage({
        tone: "success",
        text: `Obstáculo #${obstacle.id} cadastrado.`
      });
      await refresh(false);
    } catch (exception) {
      setActionMessage({
        tone: "error",
        text: operationErrorMessageFor(exception, "Falha ao cadastrar obstáculo")
      });
    } finally {
      setPlanningActionInFlight(null);
    }
  }

  async function handleDeactivateObstacle(id: number) {
    setObstacleActionInFlight(id);
    setActionMessage(null);
    setError(null);

    try {
      const obstacle = await deactivateObstacle(id);
      setActionMessage({
        tone: "success",
        text: `Obstáculo #${obstacle.id} desativado.`
      });
      await refresh(false);
    } catch (exception) {
      setActionMessage({
        tone: "error",
        text: operationErrorMessageFor(exception, "Falha ao desativar obstáculo")
      });
    } finally {
      setObstacleActionInFlight(null);
    }
  }

  async function handleCreateReview(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPlanningActionInFlight("createReview");
    setActionMessage(null);
    setError(null);

    try {
      const review = await createReview(toReviewPayload(reviewForm));
      setReviewForm(initialReviewForm);
      setActionMessage({
        tone: "success",
        text: `Avaliação #${review.id} registrada com ${review.stars} estrelas.`
      });
      await refresh(false);
    } catch (exception) {
      setActionMessage({
        tone: "error",
        text: operationErrorMessageFor(exception, "Falha ao cadastrar avaliação")
      });
    } finally {
      setPlanningActionInFlight(null);
    }
  }

  async function handlePlanTrips() {
    setPlanningActionInFlight("planTrips");
    setActionMessage(null);
    setError(null);

    try {
      const plan = await planTrips(optimizeRoute);
      setActionMessage({
        tone: "success",
        text: tripPlanSuccessMessageFor(plan)
      });
      await refresh(false);
      setActiveTable("trips");
      setActiveAdminSection("operations");
    } catch (exception) {
      setActionMessage({
        tone: "error",
        text: operationErrorMessageFor(exception, "Falha ao planejar viagens")
      });
    } finally {
      setPlanningActionInFlight(null);
    }
  }

  async function handleCreateDemoScenario() {
    setDemoActionInFlight(true);
    setDemoConfirmationOpen(false);
    setActionMessage(null);
    setError(null);

    try {
      const result = await createDemoScenario();
      approachNotificationKeysRef.current.clear();
      availabilityNotificationKeysRef.current.clear();
      setArrivalNotification(null);
      setActionMessage({
        tone: "success",
        text: demoScenarioSuccessMessageFor(result)
      });
      await refresh(false);
      setActiveTable("trips");
      setActiveAdminSection("operations");
    } catch (exception) {
      setActionMessage({
        tone: "error",
        text: operationErrorMessageFor(exception, "Falha ao carregar demo")
      });
    } finally {
      setDemoActionInFlight(false);
    }
  }

  function handleJourneyNavigation(step: JourneyStep) {
    if (step.table) {
      setActiveTable(step.table);
    }

    if (["#planning", "#obstacles"].includes(step.href)) {
      setActiveAdminSection("planning");
      return;
    }

    if (["#trip-detail", "#queues", "#operation"].includes(step.href)) {
      setActiveAdminSection("operations");
      return;
    }

    if (step.href === "#reviews") {
      setActiveAdminSection("feedback");
      return;
    }

    setActiveAdminSection("overview");
  }

  useEffect(() => {
    void refresh();
  }, []);

  useEffect(() => {
    if (clientAuthToken) {
      void refreshClientSession(clientAuthToken);
    }
  }, [clientAuthToken]);

  useEffect(() => {
    if (activeExperience !== "client" || apiStatus === "offline") {
      return;
    }

    const intervalId = window.setInterval(() => {
      void refresh(false);
    }, 5000);

    return () => window.clearInterval(intervalId);
  }, [activeExperience, apiStatus]);

  useEffect(() => {
    setStatusFilter("ALL");
  }, [activeTable]);

  useEffect(() => {
    if (!snapshot.trips.length) {
      setSelectedTripId(null);
      return;
    }

    if (selectedTripId === null || !snapshot.trips.some((trip) => trip.id === selectedTripId)) {
      setSelectedTripId(snapshot.trips[0].id);
    }
  }, [selectedTripId, snapshot.trips]);

  useEffect(() => {
    if (selectedTripId === null) {
      setTelemetryHistory([]);
      setTelemetryError(null);
      return;
    }

    void loadTelemetryForTrip(selectedTripId);
  }, [selectedTripId]);

  const metrics = useMemo(() => buildMetrics(snapshot), [snapshot]);
  const alerts = useMemo(() => buildAlerts(snapshot), [snapshot]);
  const tableData = useMemo(
    () => buildTableData(snapshot, activeTable, searchTerm, statusFilter),
    [snapshot, activeTable, searchTerm, statusFilter]
  );
  const statusOptions = statusOptionsFor(activeTable);
  const activeTableTotal = totalFor(snapshot, activeTable);
  const selectedTrip = selectedTripId === null ? null : snapshot.trips.find((trip) => trip.id === selectedTripId) ?? null;
  const simulatableTripIds = useMemo(
    () => snapshot.trips.filter((trip) => trip.status === "PLANNED" || trip.status === "IN_ROUTE").map((trip) => trip.id),
    [snapshot.trips]
  );
  const simulatableTripKey = simulatableTripIds.join("|");

  useEffect(() => {
    if (!simulatableTripIds.length || apiStatus === "offline") {
      return;
    }

    let active = true;
    let tickInProgress = false;
    const intervalId = window.setInterval(() => {
      if (tickInProgress || !active) {
        return;
      }

      tickInProgress = true;
      void Promise.allSettled(
        simulatableTripIds.map((tripId) => advanceTripSimulation(tripId, simulationTickMinutes))
      )
        .then(async (results) => {
          if (!active) {
            return;
          }

          const failedTick = results.find((result) => result.status === "rejected");
          if (failedTick?.status === "rejected") {
            setActionMessage({
              tone: "error",
              text: operationErrorMessageFor(failedTick.reason, "Falha ao simular viagem")
            });
          } else {
            const notification = approachingDeliveryNotificationFor(
              results,
              snapshotRef.current,
              approachNotificationKeysRef.current
            );

            if (notification && activeExperience !== "client") {
              setActionMessage({
                tone: "info",
                text: notification
              });
            }
          }

          await refresh(false);
        })
        .catch((exception) => {
          if (!active) {
            return;
          }

          setActionMessage({
            tone: "error",
            text: operationErrorMessageFor(exception, "Falha ao atualizar simulação")
          });
        })
        .finally(() => {
          tickInProgress = false;
        });
    }, simulationTickIntervalMs);

    return () => {
      active = false;
      window.clearInterval(intervalId);
    };
  }, [simulatableTripKey, apiStatus, activeExperience]);

  useEffect(() => {
    if (activeExperience !== "client") {
      return;
    }

    if (arrivalNotification && !isAvailabilityNotificationPending(snapshot, arrivalNotification)) {
      setArrivalNotification(null);
      return;
    }

    if (arrivalNotification) {
      return;
    }

    const notification = nextAvailabilityNotification(snapshot, availabilityNotificationKeysRef.current);
    if (!notification) {
      return;
    }

    availabilityNotificationKeysRef.current.add(notification.key);
    setArrivalNotification(notification);
    playNotificationSound();
  }, [activeExperience, arrivalNotification, snapshot]);

  const journeySteps = useMemo(
    () => buildJourneySteps(snapshot, selectedTrip, telemetryHistory),
    [snapshot, selectedTrip, telemetryHistory]
  );
  const completedJourneySteps = journeySteps.filter((step) => step.status === "done").length;
  const actionBusy =
    droneActionInFlight !== null ||
    orderActionInFlight !== null ||
    tripActionInFlight !== null ||
    planningActionInFlight !== null ||
    obstacleActionInFlight !== null ||
    deliveryConfirmationInFlight !== null ||
    availabilityResponseInFlight ||
    clientAuthInFlight ||
    demoActionInFlight ||
    apiStatus === "offline";

  return (
    <div className="appShell">
      <aside className="sidebar" aria-label="Navegação principal">
        <div className="brandBlock">
          <div className="brandMark">DD</div>
          <div>
            <p className="brandName">Drone Delivery</p>
            <p className="brandMeta">{activeExperience === "admin" ? "Admin" : "Cliente"}</p>
          </div>
        </div>

        <div className="experienceSwitch" role="tablist" aria-label="Selecionar experiência">
          <button
            type="button"
            className={activeExperience === "admin" ? "selected" : ""}
            onClick={() => handleExperienceChange("admin")}
            role="tab"
            aria-selected={activeExperience === "admin"}
          >
            Admin
          </button>
          <button
            type="button"
            className={activeExperience === "client" ? "selected" : ""}
            onClick={() => handleExperienceChange("client")}
            role="tab"
            aria-selected={activeExperience === "client"}
          >
            Cliente
          </button>
        </div>

        <nav className="navList" aria-label="Menu da experiência">
          {activeExperience === "admin" ? (
            <>
              <button
                className={activeAdminSection === "overview" ? "navItem active" : "navItem"}
                type="button"
                onClick={() => setActiveAdminSection("overview")}
              >
                Painel
              </button>
              <button
                className={activeAdminSection === "operations" ? "navItem active" : "navItem"}
                type="button"
                onClick={() => setActiveAdminSection("operations")}
              >
                Operação
              </button>
              <button
                className={activeAdminSection === "planning" ? "navItem active" : "navItem"}
                type="button"
                onClick={() => setActiveAdminSection("planning")}
              >
                Planejamento
              </button>
              <button
                className={activeAdminSection === "feedback" ? "navItem active" : "navItem"}
                type="button"
                onClick={() => setActiveAdminSection("feedback")}
              >
                Feedback
              </button>
            </>
          ) : (
            <>
              <button
                className={activeClientSection === "order" ? "navItem active" : "navItem"}
                type="button"
                onClick={() => setActiveClientSection("order")}
              >
                Solicitar
              </button>
              <button
                className={activeClientSection === "myOrders" ? "navItem active" : "navItem"}
                type="button"
                onClick={() => setActiveClientSection("myOrders")}
              >
                Meus pedidos
              </button>
              <button
                className={activeClientSection === "tracking" ? "navItem active" : "navItem"}
                type="button"
                onClick={() => setActiveClientSection("tracking")}
              >
                Acompanhar
              </button>
              <button
                className={activeClientSection === "reviews" ? "navItem active" : "navItem"}
                type="button"
                onClick={() => setActiveClientSection("reviews")}
              >
                Avaliações
              </button>
            </>
          )}
        </nav>
      </aside>

      <main className="dashboard">
        <header className="topbar">
          <div>
            <p className="eyebrow">{activeExperience === "admin" ? "Painel operacional" : "Área do cliente"}</p>
            <h1>
              {activeExperience === "admin"
                ? adminSectionTitle(activeAdminSection)
                : clientSectionTitle(activeClientSection)}
            </h1>
          </div>

          <div className="topbarActions">
            <span className={`connectionStatus ${apiStatus}`}>{apiStatusLabel(apiStatus)}</span>
            {updatedAt ? <span className="updatedAt">Atualizado {formatTime(updatedAt)}</span> : null}
            {activeExperience === "admin" ? (
              <button
                className="secondaryButton"
                type="button"
                onClick={() => setDemoConfirmationOpen(true)}
                disabled={actionBusy}
                title="Limpa os dados operacionais atuais e recria o cenário demo após confirmação"
              >
                {demoActionInFlight ? (
                  <RefreshCcw className="spinIcon" size={16} aria-hidden="true" />
                ) : (
                  <ListChecks size={17} aria-hidden="true" />
                )}
                <span>Recriar demo</span>
              </button>
            ) : null}
            <button
              className="iconButton"
              type="button"
              onClick={() => void refresh()}
              aria-label="Atualizar dados"
              title="Atualiza os dados do dashboard a partir da API Spring"
            >
              <RefreshCcw size={18} aria-hidden="true" />
            </button>
          </div>
        </header>

        {apiStatus === "offline" ? (
          <section className="connectionBanner" role="alert">
            <AlertTriangle size={18} aria-hidden="true" />
            <div>
              <strong>API Spring offline</strong>
              <span>Inicie o backend em http://localhost:8080 e tente atualizar.</span>
            </div>
            <button className="inlineButton" type="button" onClick={() => void refresh()}>
              <RefreshCcw size={16} aria-hidden="true" />
              <span>Atualizar</span>
            </button>
          </section>
        ) : null}

        {error && apiStatus !== "offline" ? (
          <section className="errorBanner" role="alert">
            <AlertTriangle size={18} aria-hidden="true" />
            <span>{error}</span>
          </section>
        ) : null}

        {actionMessage ? (
          <section className={`actionBanner ${actionMessage.tone}`} role="status">
            {actionMessage.tone === "success" ? (
              <CheckCircle2 size={18} aria-hidden="true" />
            ) : actionMessage.tone === "info" ? (
              <Plane size={18} aria-hidden="true" />
            ) : (
              <AlertTriangle size={18} aria-hidden="true" />
            )}
            <span>{actionMessage.text}</span>
          </section>
        ) : null}

        {demoConfirmationOpen ? (
          <ConfirmationDialog
            title="Recriar demo"
            detail="Essa ação limpa os dados operacionais atuais e recria o cenário de simulação."
            confirmLabel="Recriar demo"
            cancelLabel="Cancelar"
            busy={demoActionInFlight}
            onCancel={() => setDemoConfirmationOpen(false)}
            onConfirm={() => void handleCreateDemoScenario()}
          />
        ) : null}

        {trackingCodeDialog ? (
          <TrackingCodeDialog
            code={trackingCodeDialog}
            onClose={() => setTrackingCodeDialog(null)}
            onTrack={() => {
              setTrackingCodeDialog(null);
              setActiveClientSection("tracking");
            }}
          />
        ) : null}

        {orderCancellationDraft ? (
          <OrderCancellationDialog
            draft={orderCancellationDraft}
            busy={isBusy(orderActionInFlight, orderCancellationDraft.orderId, "cancel")}
            onReasonChange={(reason) =>
              setOrderCancellationDraft((current) => (current ? { ...current, reason } : current))
            }
            onCancel={() => setOrderCancellationDraft(null)}
            onConfirm={() => void handleCancelUnallocatedOrder()}
          />
        ) : null}

        {arrivalNotification ? (
          <ArrivalNotificationDialog
            notification={arrivalNotification}
            busy={availabilityResponseInFlight}
            onConfirm={() => void handleDeliveryAvailabilityResponse(true)}
            onDecline={() => void handleDeliveryAvailabilityResponse(false)}
          />
        ) : null}

        {activeExperience === "admin" ? (
          <>
        {activeAdminSection === "overview" ? (
          <>
        <section id="overview" className="metricGrid" aria-label="Resumo operacional">
          <MetricCard
            icon={<Plane size={21} />}
            label="Drones disponíveis"
            value={metrics.availableDrones}
            detail={`${metrics.dronesInRoute} em rota`}
          />
          <MetricCard
            icon={<PackageCheck size={21} />}
            label="Pedidos pendentes"
            value={metrics.pendingOrders}
            detail={`${metrics.deliveredOrders} entregues`}
          />
          <MetricCard
            icon={<Route size={21} />}
            label="Viagens ativas"
            value={metrics.activeTrips}
            detail={`${metrics.plannedTrips} planejadas`}
          />
          <MetricCard
            icon={<BatteryCharging size={21} />}
            label="Fila de recarga"
            value={snapshot.rechargeQueue.length}
            detail={`${metrics.lowBatteryDrones} com bateria baixa`}
          />
          <MetricCard
            icon={<Clock3 size={21} />}
            label="Tempo médio"
            value={formatDuration(metrics.averageDeliveryTime)}
            detail="até entrega"
          />
          <MetricCard
            icon={<Star size={21} />}
            label="Avaliação média"
            value={metrics.averageStars ? metrics.averageStars.toFixed(1) : "-"}
            detail={`${snapshot.reviews.length} avaliações`}
          />
        </section>

        <ProductivityReportPanel
          report={snapshot.productivityReport}
          selectedMonth={reportMonth}
          loading={reportMonthLoading}
          onMonthChange={(month) => void handleReportMonthChange(month)}
          onPreviousMonth={() => handleReportMonthStep(-1)}
          onNextMonth={() => handleReportMonthStep(1)}
          onCurrentMonth={() => void handleReportMonthChange(currentReportMonth())}
        />

        <section className="contentGrid">
          <section id="fleet" className="panel">
            <PanelHeader title="Frota" count={snapshot.drones.length} />
            <StatusRows statuses={droneStatuses} counts={countBy(snapshot.drones, "status")} labelFor={droneStatusLabel} />
          </section>

          <section id="orders" className="panel">
            <PanelHeader title="Pedidos" count={snapshot.orders.length} />
            <StatusRows statuses={orderStatuses} counts={countBy(snapshot.orders, "status")} labelFor={orderStatusLabel} />
          </section>

          <section id="trips" className="panel">
            <PanelHeader title="Viagens" count={snapshot.trips.length} />
            <StatusRows statuses={tripStatuses} counts={countBy(snapshot.trips, "status")} labelFor={tripStatusLabel} />
          </section>

          <section className="panel">
            <PanelHeader title="Atenção" count={alerts.length} />
            <div className="alertList">
              {alerts.length ? (
                alerts.map((alert) => (
                  <div className="alertItem" key={alert}>
                    <AlertTriangle size={16} aria-hidden="true" />
                    <span>{alert}</span>
                  </div>
                ))
              ) : (
                <p className="emptyState">{loading ? "Carregando dados..." : "Sem alertas operacionais."}</p>
              )}
            </div>
          </section>
        </section>

        <section id="journey" className="commandSection" aria-label="Jornada operacional">
          <div className="operationHeader">
            <div>
              <p className="eyebrow">Ciclo completo</p>
              <h2>Jornada operacional</h2>
            </div>
            <span className="recordCount">
              {completedJourneySteps} de {journeySteps.length}
            </span>
          </div>

          <JourneyGuide steps={journeySteps} onNavigate={handleJourneyNavigation} />
        </section>
          </>
        ) : null}

        {activeAdminSection === "operations" ? (
          <>
        <section id="trip-detail" className="commandSection" aria-label="Detalhe da viagem">
          <div className="operationHeader">
            <div>
              <p className="eyebrow">Acompanhamento de rota</p>
              <h2>Detalhe da viagem</h2>
            </div>
            <label className="detailSelector">
              <span className="srOnly">Selecionar viagem</span>
              <select
                value={selectedTripId ?? ""}
                onChange={(event) => setSelectedTripId(event.target.value ? Number(event.target.value) : null)}
              >
                {snapshot.trips.length ? null : <option value="">Sem viagens</option>}
                {snapshot.trips.map((trip) => (
                  <option key={trip.id} value={trip.id}>
                    Viagem #{trip.id}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <TripDetailPanel
            trip={selectedTrip}
            trips={snapshot.trips}
            orders={snapshot.orders}
            obstacles={snapshot.obstacles}
            mapRouteMode={mapRouteMode}
            onMapRouteModeChange={setMapRouteMode}
            onSelectTrip={setSelectedTripId}
            telemetryHistory={telemetryHistory}
            telemetryLoading={telemetryLoading}
            telemetryError={telemetryError}
          />
        </section>

        <section id="queues" className="commandSection" aria-label="Filas operacionais">
          <div className="operationHeader">
            <div>
              <p className="eyebrow">Fluxo operacional</p>
              <h2>Filas</h2>
            </div>
            <span className="recordCount">{metrics.queueTotal} registros</span>
          </div>

          <QueueOverview
            deliveryQueue={snapshot.deliveryQueue}
            rechargeQueue={snapshot.rechargeQueue}
          />
        </section>

        <section id="unallocated-orders" className="commandSection" aria-label="Tratamento de pedidos não alocados">
          <div className="operationHeader">
            <div>
              <p className="eyebrow">Tratamento operacional</p>
              <h2>Pedidos não alocados</h2>
            </div>
            <span className="recordCount">
              {snapshot.orders.filter((order) => order.status === "UNALLOCATED").length} pendentes
            </span>
          </div>

          <UnallocatedOrderTreatmentPanel
            orders={snapshot.orders.filter((order) => order.status === "UNALLOCATED")}
            onCancelOrder={(order) => setOrderCancellationDraft({ orderId: order.id, identifier: order.identifier, reason: "" })}
            onRequeueOrder={handleOrderRequeue}
            actionInFlight={orderActionInFlight}
            actionBusy={actionBusy}
          />
        </section>
          </>
        ) : null}

        {activeAdminSection === "planning" ? (
          <>
        <section id="planning" className="commandSection" aria-label="Planejamento operacional">
          <div className="operationHeader">
            <div>
              <p className="eyebrow">Entrada operacional</p>
              <h2>Planejamento</h2>
            </div>
          </div>

          <div className="commandGrid">
            <CreateDroneForm
              form={droneForm}
              onChange={(field, value) => setDroneForm((current) => ({ ...current, [field]: value }))}
              onSubmit={handleCreateDrone}
              busy={planningActionInFlight === "createDrone"}
              actionBusy={actionBusy}
            />
            <CreateOrderForm
              form={orderForm}
              onChange={(field, value) => setOrderForm((current) => ({ ...current, [field]: value }))}
              onSubmit={handleCreateOrder}
              busy={planningActionInFlight === "createOrder"}
              actionBusy={actionBusy}
            />
            <TripPlanningTool
              plannableOrders={metrics.plannableOrders}
              availableDrones={metrics.availableDrones}
              plannedTrips={metrics.plannedTrips}
              activeObstacles={metrics.activeObstacles}
              optimizeRoute={optimizeRoute}
              onOptimizeRouteChange={setOptimizeRoute}
              onPlanTrips={handlePlanTrips}
              busy={planningActionInFlight === "planTrips"}
              actionBusy={actionBusy}
            />
          </div>
        </section>

        <section id="obstacles" className="commandSection" aria-label="Gestão de obstáculos">
          <div className="operationHeader">
            <div>
              <p className="eyebrow">Restrições de rota</p>
              <h2>Obstáculos</h2>
            </div>
            <span className="recordCount">{metrics.activeObstacles} ativos</span>
          </div>

          <ObstacleManager
            form={obstacleForm}
            obstacles={snapshot.obstacles}
            onChange={(field, value) => setObstacleForm((current) => ({ ...current, [field]: value }))}
            onSubmit={handleCreateObstacle}
            onDeactivate={handleDeactivateObstacle}
            busy={planningActionInFlight === "createObstacle"}
            actionBusy={actionBusy}
            obstacleActionInFlight={obstacleActionInFlight}
          />
        </section>
          </>
        ) : null}

        {activeAdminSection === "feedback" ? (
          <>
        <section id="reviews" className="commandSection" aria-label="Avaliações do serviço">
          <div className="operationHeader">
            <div>
              <p className="eyebrow">Feedback do cliente</p>
              <h2>Avaliações</h2>
            </div>
            <span className="recordCount">
              {metrics.averageStars ? metrics.averageStars.toFixed(1) : "-"} média
            </span>
          </div>

          <ReviewManager
            form={reviewForm}
            reviews={snapshot.reviews}
            onChange={(field, value) => setReviewForm((current) => ({ ...current, [field]: value }))}
            onSubmit={handleCreateReview}
            busy={planningActionInFlight === "createReview"}
            actionBusy={actionBusy}
          />
        </section>
          </>
        ) : null}

        {activeAdminSection === "operations" ? (
        <section id="operation" className="operationPanel">
          <div className="operationHeader">
            <div>
              <p className="eyebrow">Consulta operacional</p>
              <h2>Tabelas</h2>
            </div>
            <span className="recordCount">
              {tableData.length} de {activeTableTotal}
            </span>
          </div>

          <div className="tableToolbar">
            <div className="segmentedControl" role="tablist" aria-label="Selecionar tabela operacional">
              <button
                type="button"
                className={activeTable === "drones" ? "selected" : ""}
                onClick={() => setActiveTable("drones")}
                role="tab"
                aria-selected={activeTable === "drones"}
              >
                Drones
              </button>
              <button
                type="button"
                className={activeTable === "orders" ? "selected" : ""}
                onClick={() => setActiveTable("orders")}
                role="tab"
                aria-selected={activeTable === "orders"}
              >
                Pedidos
              </button>
              <button
                type="button"
                className={activeTable === "trips" ? "selected" : ""}
                onClick={() => setActiveTable("trips")}
                role="tab"
                aria-selected={activeTable === "trips"}
              >
                Viagens
              </button>
            </div>

            <label className="searchBox">
              <Search size={17} aria-hidden="true" />
              <span className="srOnly">Buscar na tabela</span>
              <input
                type="search"
                value={searchTerm}
                onChange={(event) => setSearchTerm(event.target.value)}
                placeholder="Buscar"
              />
            </label>

            <label className="filterBox">
              <span>Status</span>
              <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
                <option value="ALL">Todos</option>
                {statusOptions.map((status) => (
                  <option key={status} value={status}>
                    {tableStatusLabel(activeTable, status)}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <OperationalTable
            activeTable={activeTable}
            rows={tableData}
            onDroneAction={handleDroneAction}
            onCancelOrder={(order) => setOrderCancellationDraft({ orderId: order.id, identifier: order.identifier, reason: "" })}
            onRequeueOrder={handleOrderRequeue}
            onTripAction={handleTripAction}
            onSelectTrip={setSelectedTripId}
            selectedTripId={selectedTripId}
            droneActionInFlight={droneActionInFlight}
            orderActionInFlight={orderActionInFlight}
            tripActionInFlight={tripActionInFlight}
            actionBusy={actionBusy}
            telemetryDrafts={telemetryDrafts}
            onTelemetryDraftChange={(id, value) => setTelemetryDrafts((drafts) => ({ ...drafts, [id]: value }))}
          />
        </section>
        ) : null}
          </>
        ) : (
          <ClientExperience
            activeSection={activeClientSection}
            snapshot={snapshot}
            orderForm={clientOrderForm}
            onOrderFormChange={(field, value) => setClientOrderForm((current) => ({ ...current, [field]: value }))}
            onOrderSubmit={handleCreateClientOrder}
            orderBusy={planningActionInFlight === "createOrder"}
            trackingTerm={clientTrackingTerm}
            onTrackingTermChange={setClientTrackingTerm}
            clientUser={clientUser}
            clientOrders={clientOrders}
            clientOrdersLoading={clientOrdersLoading}
            authMode={clientAuthMode}
            authForm={clientAuthForm}
            authBusy={clientAuthInFlight}
            onAuthModeChange={setClientAuthMode}
            onAuthFormChange={(field, value) => setClientAuthForm((current) => ({ ...current, [field]: value }))}
            onAuthSubmit={handleClientAuthSubmit}
            onLogout={handleClientLogout}
            onClientOrderSelect={handleClientOrderSelect}
            reviewForm={reviewForm}
            onReviewFormChange={(field, value) => setReviewForm((current) => ({ ...current, [field]: value }))}
            onReviewSubmit={handleCreateReview}
            reviewBusy={planningActionInFlight === "createReview"}
            deliveryConfirmationCode={deliveryConfirmationCode}
            onDeliveryConfirmationCodeChange={setDeliveryConfirmationCode}
            onDeliveryConfirm={handleConfirmClientDelivery}
            deliveryConfirmationInFlight={deliveryConfirmationInFlight}
            actionBusy={actionBusy}
          />
        )}
      </main>
    </div>
  );
}

function JourneyGuide({
  steps,
  onNavigate
}: {
  steps: JourneyStep[];
  onNavigate: (step: JourneyStep) => void;
}) {
  return (
    <div className="journeyGrid">
      {steps.map((step, index) => (
        <article className={`journeyStep ${step.status}`} key={step.title}>
          <div className="journeyStepHeader">
            <span className="journeyStepIndex">{index + 1}</span>
            <span className="journeyStepIcon" aria-hidden="true">
              {step.icon}
            </span>
          </div>
          <div className="journeyStepBody">
            <h3>{step.title}</h3>
            <p>{step.detail}</p>
          </div>
          <div className="journeyStepFooter">
            <span className={`journeyStatus ${step.status}`}>{journeyStatusLabel(step.status)}</span>
            <a
              className="journeyAction"
              href={step.href}
              onClick={() => {
                onNavigate(step);
              }}
            >
              {step.actionLabel}
            </a>
          </div>
        </article>
      ))}
    </div>
  );
}

function TripDetailPanel({
  trip,
  trips,
  orders,
  obstacles,
  mapRouteMode,
  onMapRouteModeChange,
  onSelectTrip,
  telemetryHistory,
  telemetryLoading,
  telemetryError
}: {
  trip: Trip | null;
  trips: Trip[];
  orders: Order[];
  obstacles: Obstacle[];
  mapRouteMode: MapRouteMode;
  onMapRouteModeChange: (mode: MapRouteMode) => void;
  onSelectTrip: (id: number) => void;
  telemetryHistory: TripTelemetry[];
  telemetryLoading: boolean;
  telemetryError: string | null;
}) {
  if (!trip) {
    return <p className="tableEmpty">Nenhuma viagem selecionada.</p>;
  }

  const routeProgress = sortedRouteProgress(trip);
  const telemetry = sortedTelemetry(telemetryHistory);

  return (
    <section className="tripDetailPanel">
      <div className="tripDetailSummary">
        <DetailStat label="Status" value={tripStatusLabel(trip.status)} />
        <DetailStat label="Drone" value={`#${trip.droneId}`} />
        <DetailStat label="Pedidos" value={trip.orders.length} />
        <DetailStat label="Progresso" value={`${deliveredCount(trip)} / ${trip.routeProgress.length}`} />
        <DetailStat label="Peso" value={formatWeight(trip.totalWeight)} />
        <DetailStat label="Distância" value={formatDistance(trip.totalDistance)} />
        <DetailStat label="Duração" value={formatDuration(trip.estimatedDuration)} />
        <DetailStat label="Tempo médio" value={formatDuration(trip.averageDeliveryTime)} />
        <DetailStat label="Saída ideal" value={formatNullableDateTime(trip.idealDispatchTime)} />
        <DetailStat
          label="Janela"
          value={trip.dispatchWindowOpen ? "Aberta" : `Em ${formatDuration(trip.minutesUntilIdealDispatch)}`}
        />
      </div>

      <OperationMap
        selectedTrip={trip}
        trips={trips}
        orders={orders}
        obstacles={obstacles}
        routeMode={mapRouteMode}
        onRouteModeChange={onMapRouteModeChange}
        onSelectTrip={onSelectTrip}
      />

      <div className="tripDetailGrid">
        <section className="detailSubpanel" aria-label="Rota da viagem">
          <ToolHeader icon={<Route size={18} />} title="Rota" />
          <div className="routeProgressList">
            {routeProgress.length ? (
              routeProgress.map((progress) => {
                const displayStatus = routeProgressDisplayStatus(progress);

                return (
                  <article className="routeStep" key={`${progress.routePosition}-${progress.orderId}`}>
                    <div className={`routeStepMarker ${routeProgressMarkerClass(displayStatus)}`}>
                      {progress.routePosition + 1}
                    </div>
                    <div className="routeStepMain">
                      <div className="routeStepHeader">
                        <strong>Pedido #{progress.orderId}</strong>
                        <span className={`statusChip ${statusChipClass(displayStatus)}`}>
                          {routeProgressStatusLabel(displayStatus)}
                        </span>
                      </div>
                      <div className="routeStepMeta">
                        <span>{formatDuration(progress.estimatedDeliveryTime)}</span>
                        <span>{formatNullableDateTime(progress.deliveredAt ?? progress.deliveryFailedAt)}</span>
                      </div>
                      {progress.deliveryFailureReason ? <p className="routeStepReason">{progress.deliveryFailureReason}</p> : null}
                    </div>
                  </article>
                );
              })
            ) : (
              <p className="emptyState">Sem progresso de rota registrado.</p>
            )}
          </div>
        </section>

        <section className="detailSubpanel" aria-label="Histórico de telemetria">
          <ToolHeader icon={<BatteryCharging size={18} />} title="Telemetria" />
          {telemetryError ? (
            <p className="detailError">{telemetryError}</p>
          ) : telemetryLoading ? (
            <p className="emptyState">Carregando telemetria...</p>
          ) : telemetry.length ? (
            <div className="telemetryHistoryList">
              {telemetry.map((entry) => (
                <article className="telemetryHistoryItem" key={entry.id}>
                  <BatteryMeter value={entry.batteryLevel} />
                  <span>{formatDateTime(entry.reportedAt)}</span>
                </article>
              ))}
            </div>
          ) : (
            <p className="emptyState">Sem telemetria registrada.</p>
          )}
        </section>
      </div>
    </section>
  );
}

function OperationMap({
  selectedTrip,
  trips,
  orders,
  obstacles,
  routeMode,
  onRouteModeChange,
  onSelectTrip,
  showRouteControls = true
}: {
  selectedTrip: Trip;
  trips: Trip[];
  orders: Order[];
  obstacles: Obstacle[];
  routeMode: MapRouteMode;
  onRouteModeChange: (mode: MapRouteMode) => void;
  onSelectTrip: (id: number) => void;
  showRouteControls?: boolean;
}) {
  const visibleTrips = (routeMode === "all" ? trips : [selectedTrip]).filter((trip) => trip.routeProgress.length > 0);
  const viewport = buildMapViewport(visibleTrips, orders, obstacles);
  const routeLayers = visibleTrips
    .map((trip) => buildMapRouteLayer(trip, orders, viewport, selectedTrip.id))
    .sort((left, right) => Number(left.selected) - Number(right.selected));
  const orderHighlights = buildMapOrderHighlights(routeLayers);
  const visibleObstacles = obstacles.filter((obstacle) => obstacleIntersectsViewport(obstacle, viewport));
  const visibleOrderMarkers = orders.filter((order) => orderHighlights.has(order.id) || mapPointIsInsideViewport(order.location, viewport));

  return (
    <section className="detailSubpanel mapPanel" aria-label="Mapa 2D da operação">
      <div className="mapHeader">
        <ToolHeader icon={<MapPin size={18} />} title="Mapa 2D" />
        <div className="mapControls">
          {showRouteControls ? (
            <div className="mapModeControl" role="tablist" aria-label="Visualização de rotas no mapa">
              {mapRouteModes.map((mode) => (
                <button
                  type="button"
                  className={routeMode === mode ? "selected" : ""}
                  key={mode}
                  onClick={() => onRouteModeChange(mode)}
                  role="tab"
                  aria-selected={routeMode === mode}
                  title={mapRouteModeDescription(mode)}
                >
                  {mapRouteModeLabel(mode)}
                </button>
              ))}
            </div>
          ) : null}
          <div className="mapLegend" aria-label="Legenda do mapa">
            <span><i className="legendDot base" />Base</span>
            <span><i className="legendDot drone" />Drone</span>
            <span><i className="legendDot order" />Pedido fora da rota</span>
            <span><i className="legendDot routeOrder" />Pedido da rota</span>
            <span><i className="legendLine selected" />Selecionada</span>
            <span><i className="legendLine planned" />Planejada</span>
            <span><i className="legendLine inRoute" />Em rota</span>
            <span><i className="legendLine returned" />Retorno</span>
            <span><i className="legendObstacle" />Obstáculo</span>
          </div>
        </div>
      </div>

      {showRouteControls ? (
        routeLayers.length ? (
          <div className="mapTripLegend" aria-label="Viagens exibidas no mapa">
            {routeLayers.map((layer) => (
              <button
                className={layer.selected ? "mapTripChip selected" : "mapTripChip"}
                key={layer.trip.id}
                type="button"
                onClick={() => onSelectTrip(layer.trip.id)}
                style={mapColorStyle(layer.color)}
                title={`Selecionar viagem #${layer.trip.id} no detalhe operacional`}
              >
                <i aria-hidden="true" />
                <strong>Viagem #{layer.trip.id}</strong>
                <span>{tripStatusLabel(layer.trip.status)}</span>
              </button>
            ))}
          </div>
        ) : (
          <p className="mapHint">Nenhuma rota disponível para exibição.</p>
        )
      ) : null}

      <div className="mapCanvas">
        <div
          className="mapScaleBadge"
          title={`Área exibida: ${formatDistance(viewport.size)} por ${formatDistance(viewport.size)}`}
        >
          <Ruler size={14} aria-hidden="true" />
          <span>Escala {formatDistance(viewport.size)}</span>
        </div>

        {routeLayers.flatMap((layer) =>
          layer.routeSegments.map((segment) => (
            <div
              className={`mapRouteSegment ${layer.trip.status.toLowerCase()} ${layer.selected ? "selected" : ""} ${
                routeMode === "all" && !layer.selected ? "faded" : ""
              }`}
              key={`${layer.trip.id}-${segment.key}`}
              style={routeSegmentStyle(segment, layer.color)}
              title={`Viagem #${layer.trip.id} (${tripStatusLabel(layer.trip.status)}): ${segment.fromLabel} para ${segment.toLabel}`}
            >
              <span className="mapRouteArrow" aria-hidden="true" />
            </div>
          ))
        )}

        {visibleObstacles.map((obstacle) => {
          const position = projectPoint(obstacle.center, viewport);
          const diameter = (obstacle.radius * 2 * 100) / viewport.size;

          return (
            <div
              className={obstacle.active ? "mapObstacle active" : "mapObstacle"}
              key={obstacle.id}
              title={`Obstáculo #${obstacle.id}`}
              style={{
                left: `${position.left}%`,
                top: `${position.top}%`,
                width: `${Math.max(2, diameter)}%`,
                height: `${Math.max(2, diameter)}%`
              }}
            />
          );
        })}

        <div className="mapBaseMarker" style={mapPointStyle(projectPoint({ x: 0, y: 0 }, viewport))} title="Base">
          B
        </div>

        {routeLayers.map((layer) => {
          const simulation = layer.trip.simulation;
          if (!simulation) {
            return null;
          }

          const progress = Math.round(simulation.progress * 100);

          return (
            <div
              className={`mapDroneMarker ${layer.selected ? "selected" : ""} ${
                routeMode === "all" && !layer.selected ? "faded" : ""
              }`}
              key={`drone-${layer.trip.id}`}
              style={mapDroneMarkerStyle(projectPoint(simulation.currentLocation, viewport), layer.color)}
              title={`Drone #${simulation.droneId} na viagem #${layer.trip.id}: ${progress}% da rota, ${tripStatusLabel(layer.trip.status)}`}
            >
              <Plane size={15} aria-hidden="true" />
            </div>
          );
        })}

        {visibleOrderMarkers.map((order) => {
          const highlight = orderHighlights.get(order.id);

          return (
            <div
              className={`mapOrderMarker ${highlight ? "onRoute" : ""} ${highlight?.selected ? "selected" : ""} ${order.status.toLowerCase()}`}
              key={order.id}
              style={mapOrderMarkerStyle(projectPoint(order.location, viewport), highlight?.color)}
              title={mapOrderTitle(order, highlight)}
            >
              {highlight ? highlight.routePosition + 1 : order.id}
            </div>
          );
        })}
      </div>
    </section>
  );
}

function DetailStat({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="detailStat">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function ClientExperience({
  activeSection,
  snapshot,
  orderForm,
  onOrderFormChange,
  onOrderSubmit,
  orderBusy,
  trackingTerm,
  onTrackingTermChange,
  clientUser,
  clientOrders,
  clientOrdersLoading,
  authMode,
  authForm,
  authBusy,
  onAuthModeChange,
  onAuthFormChange,
  onAuthSubmit,
  onLogout,
  onClientOrderSelect,
  reviewForm,
  onReviewFormChange,
  onReviewSubmit,
  reviewBusy,
  deliveryConfirmationCode,
  onDeliveryConfirmationCodeChange,
  onDeliveryConfirm,
  deliveryConfirmationInFlight,
  actionBusy
}: {
  activeSection: ClientSection;
  snapshot: DashboardSnapshot;
  orderForm: ClientOrderFormState;
  onOrderFormChange: (field: keyof ClientOrderFormState, value: string) => void;
  onOrderSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
  orderBusy: boolean;
  trackingTerm: string;
  onTrackingTermChange: (value: string) => void;
  clientUser: ClientUser | null;
  clientOrders: Order[];
  clientOrdersLoading: boolean;
  authMode: ClientAuthMode;
  authForm: ClientAuthFormState;
  authBusy: boolean;
  onAuthModeChange: (mode: ClientAuthMode) => void;
  onAuthFormChange: (field: keyof ClientAuthFormState, value: string) => void;
  onAuthSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
  onLogout: () => void;
  onClientOrderSelect: (identifier: string) => void;
  reviewForm: ReviewFormState;
  onReviewFormChange: (field: keyof ReviewFormState, value: string | number) => void;
  onReviewSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
  reviewBusy: boolean;
  deliveryConfirmationCode: string;
  onDeliveryConfirmationCodeChange: (value: string) => void;
  onDeliveryConfirm: (tripId: number, routePosition: number) => void;
  deliveryConfirmationInFlight: number | null;
  actionBusy: boolean;
}) {
  if (!clientUser) {
    return (
      <section className="clientExperience">
        <ClientAuthPanel
          mode={authMode}
          form={authForm}
          busy={authBusy}
          actionBusy={actionBusy}
          onModeChange={onAuthModeChange}
          onChange={onAuthFormChange}
          onSubmit={onAuthSubmit}
        />
      </section>
    );
  }

  const selectedOrder = findClientOrder(clientOrders, trackingTerm);
  const selectedTrip = selectedOrder ? findTripForOrder(snapshot.trips, selectedOrder.id) : null;
  const selectedRouteProgress = selectedTrip && selectedOrder ? routeProgressForOrder(selectedTrip, selectedOrder.id) : null;
  const clientMapOrders = selectedTrip ? snapshot.orders.filter((order) => selectedTrip.route.includes(order.id)) : [];
  const estimatedDeliveryTime = selectedRouteProgress ? formatDuration(selectedRouteProgress.estimatedDeliveryTime) : "-";
  const clientOrderItems = buildClientOrderList(clientOrders, snapshot.trips, trackingTerm);

  return (
    <section className="clientExperience">
      <ClientSessionBar user={clientUser} onLogout={onLogout} actionBusy={actionBusy} />

      <section className="clientMetricGrid" aria-label="Resumo do cliente">
        <MetricCard
          icon={<PackageCheck size={21} />}
          label="Pedido"
          value={selectedOrder ? selectedOrder.identifier : "-"}
          detail={selectedOrder ? orderStatusLabel(selectedOrder.status) : "sem seleção"}
        />
        <MetricCard
          icon={<Clock3 size={21} />}
          label="Tempo estimado"
          value={estimatedDeliveryTime}
          detail="até entrega"
        />
        <MetricCard
          icon={<Route size={21} />}
          label="Viagem"
          value={selectedTrip ? `#${selectedTrip.id}` : "-"}
          detail={selectedTrip ? tripStatusLabel(selectedTrip.status) : "aguardando"}
        />
        <MetricCard
          icon={<ListChecks size={21} />}
          label="Meus pedidos"
          value={clientOrders.length}
          detail={clientOrdersLoading ? "atualizando" : "na conta"}
        />
        <MetricCard
          icon={<Star size={21} />}
          label="Avaliações"
          value={snapshot.reviews.length}
          detail={`${average(snapshot.reviews.map((review) => review.stars)).toFixed(1)} média`}
        />
      </section>

      {activeSection === "order" ? (
      <section id="client-order" className="clientGrid">
        <ClientOrderForm
          form={orderForm}
          onChange={onOrderFormChange}
          onSubmit={onOrderSubmit}
          busy={orderBusy}
          actionBusy={actionBusy}
        />
        <ClientTrackingPanel
          order={selectedOrder}
          trip={selectedTrip}
          routeProgress={selectedRouteProgress}
          trackingTerm={trackingTerm}
          onTrackingTermChange={onTrackingTermChange}
          confirmationCode={deliveryConfirmationCode}
          onConfirmationCodeChange={onDeliveryConfirmationCodeChange}
          onConfirmDelivery={onDeliveryConfirm}
          confirmationBusy={selectedTrip ? deliveryConfirmationInFlight === selectedTrip.id : false}
          actionBusy={actionBusy}
        />
      </section>
      ) : null}

      {activeSection === "myOrders" ? (
      <section id="client-my-orders" className="commandSection" aria-label="Meus pedidos">
        <div className="operationHeader">
          <div>
            <p className="eyebrow">Pedidos</p>
            <h2>Meus pedidos</h2>
          </div>
          <span className="recordCount">{clientOrderItems.length} registros</span>
        </div>

        <ClientOrdersPanel
          items={clientOrderItems}
          onSelect={onClientOrderSelect}
          actionBusy={actionBusy || clientOrdersLoading}
        />
      </section>
      ) : null}

      {activeSection === "tracking" ? (
      <section id="client-tracking" className="commandSection" aria-label="Acompanhamento do cliente">
        <div className="operationHeader">
          <div>
            <p className="eyebrow">Acompanhamento</p>
            <h2>Rota do pedido</h2>
          </div>
          <span className="recordCount">{selectedTrip ? tripStatusLabel(selectedTrip.status) : "sem viagem"}</span>
        </div>

        <div className="clientTrackingLayout">
          <ClientTrackingPanel
            order={selectedOrder}
            trip={selectedTrip}
            routeProgress={selectedRouteProgress}
            trackingTerm={trackingTerm}
            onTrackingTermChange={onTrackingTermChange}
            confirmationCode={deliveryConfirmationCode}
            onConfirmationCodeChange={onDeliveryConfirmationCodeChange}
            onConfirmDelivery={onDeliveryConfirm}
            confirmationBusy={selectedTrip ? deliveryConfirmationInFlight === selectedTrip.id : false}
            actionBusy={actionBusy}
          />
          {selectedTrip ? (
            <OperationMap
              selectedTrip={selectedTrip}
              trips={[selectedTrip]}
              orders={clientMapOrders}
              obstacles={[]}
              routeMode="selected"
              onRouteModeChange={() => undefined}
              onSelectTrip={() => undefined}
              showRouteControls={false}
            />
          ) : (
            <p className="tableEmpty">Nenhuma viagem vinculada ao pedido selecionado.</p>
          )}
        </div>
      </section>
      ) : null}

      {activeSection === "reviews" ? (
      <section id="client-reviews" className="commandSection" aria-label="Avaliações públicas">
        <div className="operationHeader">
          <div>
            <p className="eyebrow">Feedback</p>
            <h2>Avaliações</h2>
          </div>
          <span className="recordCount">{snapshot.reviews.length} registros</span>
        </div>

        <ReviewManager
          form={reviewForm}
          reviews={snapshot.reviews}
          onChange={onReviewFormChange}
          onSubmit={onReviewSubmit}
          busy={reviewBusy}
          actionBusy={actionBusy}
        />
      </section>
      ) : null}
    </section>
  );
}

function ClientAuthPanel({
  mode,
  form,
  busy,
  actionBusy,
  onModeChange,
  onChange,
  onSubmit
}: {
  mode: ClientAuthMode;
  form: ClientAuthFormState;
  busy: boolean;
  actionBusy: boolean;
  onModeChange: (mode: ClientAuthMode) => void;
  onChange: (field: keyof ClientAuthFormState, value: string) => void;
  onSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
}) {
  const isRegister = mode === "register";

  return (
    <section className="clientAuthLayout" aria-label="Acesso do cliente">
      <form className="toolPanel clientAuthPanel" onSubmit={onSubmit}>
        <ToolHeader icon={isRegister ? <UserPlus size={18} /> : <LogIn size={18} />} title={isRegister ? "Criar conta" : "Entrar"} />
        <div className="segmentedControl" role="tablist" aria-label="Modo de acesso">
          <button
            type="button"
            className={mode === "login" ? "selected" : ""}
            onClick={() => onModeChange("login")}
            role="tab"
            aria-selected={mode === "login"}
          >
            Entrar
          </button>
          <button
            type="button"
            className={mode === "register" ? "selected" : ""}
            onClick={() => onModeChange("register")}
            role="tab"
            aria-selected={mode === "register"}
          >
            Criar conta
          </button>
        </div>
        {isRegister ? (
          <TextField
            label="Nome"
            value={form.name}
            onChange={(value) => onChange("name", value)}
            required
            icon={<UserPlus size={15} />}
          />
        ) : null}
        <TextField
          label="E-mail"
          value={form.email}
          onChange={(value) => onChange("email", value)}
          required
          icon={<MessageSquareText size={15} />}
        />
        <TextField
          label="Senha"
          value={form.password}
          onChange={(value) => onChange("password", value)}
          required
          type="password"
          icon={<KeyRound size={15} />}
        />
        <PrimaryButton
          icon={isRegister ? <UserPlus size={17} /> : <LogIn size={17} />}
          label={isRegister ? "Criar conta" : "Entrar"}
          busy={busy}
          disabled={actionBusy}
        />
      </form>

      <section className="toolPanel clientAuthAside">
        <ToolHeader icon={<PackageCheck size={18} />} title="Área Cliente" />
        <div className="clientAuthBenefits">
          <DetailStat label="Pedidos" value="Conta vinculada" />
          <DetailStat label="Rastreio" value="Código único" />
          <DetailStat label="Entrega" value="Confirmação segura" />
        </div>
      </section>
    </section>
  );
}

function ClientSessionBar({
  user,
  onLogout,
  actionBusy
}: {
  user: ClientUser;
  onLogout: () => void;
  actionBusy: boolean;
}) {
  return (
    <section className="clientSessionBar" aria-label="Sessão do cliente">
      <div>
        <span>Cliente conectado</span>
        <strong>{user.name}</strong>
        <small>{user.email}</small>
      </div>
      <button className="secondaryButton compact" type="button" onClick={onLogout} disabled={actionBusy}>
        <LogOut size={16} aria-hidden="true" />
        <span>Sair</span>
      </button>
    </section>
  );
}

function ClientOrdersPanel({
  items,
  onSelect,
  actionBusy
}: {
  items: ClientOrderListItem[];
  onSelect: (identifier: string) => void;
  actionBusy: boolean;
}) {
  if (!items.length) {
    return <p className="emptyState">Nenhum pedido solicitado por esta conta.</p>;
  }

  return (
    <div className="clientOrdersList">
      {items.map((item) => {
        const order = item.order;
        const trip = item.trip;
        const routeProgress = item.routeProgress;
        const statusLabel = orderStatusLabel(order.status);
        const statusClass = orderStatusClass(order.status);

        return (
          <article className={item.selected ? "clientOrderItem selected" : "clientOrderItem"} key={item.identifier}>
            <div className="clientOrderItemMain">
              <div className="clientOrderItemHeader">
                <strong>{item.identifier}</strong>
                <span className={`statusChip ${statusClass}`}>{statusLabel}</span>
              </div>
              <div className="clientOrderItemMeta">
                <span>{formatDateTime(order.confirmedDeliveryTime)}</span>
                <span>{formatWeight(order.weight)}</span>
                <span>{formatLocation(order.location.x, order.location.y)}</span>
                <span>{trip ? `Viagem #${trip.id}` : "Sem viagem"}</span>
                <span>{routeProgress ? formatDuration(routeProgress.estimatedDeliveryTime) : "-"}</span>
              </div>
              {order?.statusReason ? <p className="clientOrderItemReason">{localizedStatusReason(order.statusReason)}</p> : null}
            </div>
            <div className="clientOrderItemActions">
              <button
                className="secondaryButton compact"
                type="button"
                onClick={() => onSelect(item.identifier)}
                disabled={actionBusy}
              >
                <Eye size={16} aria-hidden="true" />
                <span>Ver</span>
              </button>
            </div>
          </article>
        );
      })}
    </div>
  );
}

function ClientOrderForm({
  form,
  onChange,
  onSubmit,
  busy,
  actionBusy
}: {
  form: ClientOrderFormState;
  onChange: (field: keyof ClientOrderFormState, value: string) => void;
  onSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
  busy: boolean;
  actionBusy: boolean;
}) {
  return (
    <form className="toolPanel clientOrderPanel" onSubmit={onSubmit}>
      <ToolHeader icon={<PackageCheck size={18} />} title="Novo pedido" />
      <div className="formGrid compact">
        <TextField
          label="Peso (kg)"
          type="number"
          min="0.1"
          step="0.1"
          value={form.weight}
          onChange={(value) => onChange("weight", value)}
          required
          icon={<Weight size={15} />}
        />
        <TextField
          label="X (km)"
          type="number"
          step="0.1"
          value={form.x}
          onChange={(value) => onChange("x", value)}
          required
          icon={<MapPin size={15} />}
        />
        <TextField
          label="Y (km)"
          type="number"
          step="0.1"
          value={form.y}
          onChange={(value) => onChange("y", value)}
          required
          icon={<MapPin size={15} />}
        />
        <TextField
          label="Horário confirmado"
          type="datetime-local"
          value={form.confirmedDeliveryTime}
          onChange={(value) => onChange("confirmedDeliveryTime", value)}
          required
          icon={<Clock3 size={15} />}
        />
      </div>
      <PrimaryButton icon={<Plus size={17} />} label="Solicitar entrega" busy={busy} disabled={actionBusy} />
    </form>
  );
}

function ClientTrackingPanel({
  order,
  trip,
  routeProgress,
  trackingTerm,
  onTrackingTermChange,
  confirmationCode,
  onConfirmationCodeChange,
  onConfirmDelivery,
  confirmationBusy,
  actionBusy
}: {
  order: Order | null;
  trip: Trip | null;
  routeProgress: Trip["routeProgress"][number] | null;
  trackingTerm: string;
  onTrackingTermChange: (value: string) => void;
  confirmationCode: string;
  onConfirmationCodeChange: (value: string) => void;
  onConfirmDelivery: (tripId: number, routePosition: number) => void;
  confirmationBusy: boolean;
  actionBusy: boolean;
}) {
  const canConfirmDelivery = isDeliveryConfirmationAvailable(order, trip, routeProgress);

  return (
    <section className="toolPanel clientTrackingPanel">
      <ToolHeader icon={<Search size={18} />} title="Acompanhar pedido" />
      <label className="field">
        <span>Código de rastreio e confirmação</span>
        <input
          type="search"
          value={trackingTerm}
          onChange={(event) => onTrackingTermChange(event.target.value)}
          placeholder="Ex.: A7K2P9"
        />
      </label>

      {order ? (
        <>
          <div className="clientTrackingSummary">
            <DetailStat label="Código" value={order.identifier} />
            <DetailStat label="Status" value={orderStatusLabel(order.status)} />
            <DetailStat label="Tempo" value={routeProgress ? formatDuration(routeProgress.estimatedDeliveryTime) : "-"} />
            <DetailStat label="Horário" value={formatDateTime(order.confirmedDeliveryTime)} />
            <DetailStat label="Drone" value={trip ? `#${trip.droneId}` : "-"} />
          </div>
          <ClientOrderNotice order={order} />
          <ClientDeliveryAvailabilityNotice order={order} trip={trip} routeProgress={routeProgress} />
          <ClientStatusTimeline order={order} />
          <form
            className="deliveryConfirmationForm"
            onSubmit={(event) => {
              event.preventDefault();
              if (trip && routeProgress && canConfirmDelivery) {
                onConfirmDelivery(trip.id, routeProgress.routePosition);
              }
            }}
          >
            <TextField
              label="Código de rastreio e confirmação"
              value={confirmationCode}
              onChange={onConfirmationCodeChange}
              required
              icon={<KeyRound size={15} />}
              disabled={!canConfirmDelivery || actionBusy}
            />
            <PrimaryButton
              icon={<PackageCheck size={17} />}
              label="Confirmar recebimento"
              busy={confirmationBusy}
              disabled={actionBusy || !canConfirmDelivery}
            />
          </form>
        </>
      ) : (
        <p className="emptyState">Nenhum pedido selecionado.</p>
      )}
    </section>
  );
}

function ClientStatusTimeline({ order }: { order: Order }) {
  const steps = clientOrderSteps(order.status);

  return (
    <div className="clientTimeline" aria-label="Status do pedido">
      {steps.map((step) => (
        <div className={`clientTimelineStep ${step.state}`} key={step.label}>
          <span aria-hidden="true" />
          <strong>{step.label}</strong>
        </div>
      ))}
    </div>
  );
}

function ClientOrderNotice({ order }: { order: Order }) {
  if (!["UNALLOCATED", "CANCELLED", "NOT_DELIVERED"].includes(order.status)) {
    return null;
  }

  const titleByStatus: Record<"UNALLOCATED" | "CANCELLED" | "NOT_DELIVERED", string> = {
    UNALLOCATED: "Pedido não alocado",
    CANCELLED: "Entrega cancelada",
    NOT_DELIVERED: "Pacote não entregue"
  };
  const reason = localizedStatusReason(order.statusReason);
  const detailByStatus: Record<"UNALLOCATED" | "CANCELLED" | "NOT_DELIVERED", string> = {
    UNALLOCATED:
      "Não foi possível alocar esse pacote no planejamento atual. O admin deve cancelar com justificativa ou reenviar para planejamento.",
    CANCELLED: "A entrega foi cancelada pelo admin.",
    NOT_DELIVERED: "O drone retornou para a base com a encomenda."
  };
  const status = order.status as "UNALLOCATED" | "CANCELLED" | "NOT_DELIVERED";
  const tone = order.status === "UNALLOCATED" ? "warning" : "error";

  return (
    <div className={`clientOrderNotice ${tone}`} role="status">
      <AlertTriangle size={17} aria-hidden="true" />
      <div>
        <strong>{titleByStatus[status]}</strong>
        <span>{reason ? `${detailByStatus[status]} Motivo: ${reason}` : detailByStatus[status]}</span>
      </div>
    </div>
  );
}

function ClientDeliveryAvailabilityNotice({
  order,
  trip,
  routeProgress
}: {
  order: Order;
  trip: Trip | null;
  routeProgress: Trip["routeProgress"][number] | null;
}) {
  if (!trip || !routeProgress || routeProgress.delivered || routeProgress.deliveryFailedAt || order.status !== "IN_ROUTE") {
    return null;
  }

  if (routeProgress.availabilityConfirmedAt) {
    const deadlineExpired =
      routeProgress.deliveryConfirmationDeadline !== null &&
      Date.now() > Date.parse(routeProgress.deliveryConfirmationDeadline);

    if (deadlineExpired) {
      return (
        <div className="clientOrderNotice error" role="status">
          <AlertTriangle size={17} aria-hidden="true" />
          <div>
            <strong>Prazo do código expirado</strong>
            <span>O drone seguirá a rota e levará este pacote novamente para a base.</span>
          </div>
        </div>
      );
    }

    const codeInstruction = routeProgress.deliveryConfirmationDeadline
      ? `Confirme o recebimento com o código do pedido até ${formatDateTime(routeProgress.deliveryConfirmationDeadline)}.`
      : "A confirmação por código ficará disponível quando o drone parar no endereço.";

    return (
      <div className="clientOrderNotice success" role="status">
        <CheckCircle2 size={17} aria-hidden="true" />
        <div>
          <strong>Disponibilidade confirmada</strong>
          <span>{codeInstruction}</span>
        </div>
      </div>
    );
  }

  if (routeProgress.availabilityNotifiedAt) {
    return (
      <div className="clientOrderNotice warning" role="status">
        <AlertTriangle size={17} aria-hidden="true" />
        <div>
          <strong>Confirme sua disponibilidade</strong>
          <span>
            Responda à notificação até {formatNullableDateTime(routeProgress.availabilityResponseDeadline)} para evitar que o drone
            retorne à base com o pacote.
          </span>
        </div>
      </div>
    );
  }

  return null;
}

function UnallocatedOrderTreatmentPanel({
  orders,
  onCancelOrder,
  onRequeueOrder,
  actionInFlight,
  actionBusy
}: {
  orders: Order[];
  onCancelOrder: (order: Order) => void;
  onRequeueOrder: (id: number) => void;
  actionInFlight: { id: number; action: OrderAction } | null;
  actionBusy: boolean;
}) {
  const sortedOrders = [...orders].sort((left, right) => Date.parse(left.queuedAt) - Date.parse(right.queuedAt) || left.id - right.id);

  if (!sortedOrders.length) {
    return <p className="tableEmpty">Nenhum pedido aguardando tratamento.</p>;
  }

  return (
    <div className="unallocatedTreatmentList">
      {sortedOrders.map((order) => (
        <article className="unallocatedTreatmentItem" key={order.id}>
          <div className="unallocatedTreatmentMain">
            <div className="unallocatedTreatmentHeader">
              <strong>{order.identifier}</strong>
              <span className={`priorityChip ${order.priority.toLowerCase()}`}>{order.priority}</span>
            </div>
            <p>{localizedStatusReason(order.statusReason) ?? unallocatedOrderMessage(order)}</p>
            <div className="queueMeta">
              <span>
                <Weight size={14} aria-hidden="true" />
                {formatWeight(order.weight)}
              </span>
              <span>
                <MapPin size={14} aria-hidden="true" />
                {formatLocation(order.location.x, order.location.y)}
              </span>
              <span>
                <CalendarDays size={14} aria-hidden="true" />
                {formatDateTime(order.confirmedDeliveryTime)}
              </span>
              <span>
                <Clock3 size={14} aria-hidden="true" />
                {formatDateTime(order.queuedAt)}
              </span>
            </div>
          </div>
          <div className="unallocatedTreatmentActions">
            <button
              className="secondaryButton compact"
              type="button"
              onClick={() => onRequeueOrder(order.id)}
              disabled={actionBusy}
            >
              {isBusy(actionInFlight, order.id, "requeue") ? (
                <RefreshCcw className="spinIcon" size={16} aria-hidden="true" />
              ) : (
                <Route size={16} aria-hidden="true" />
              )}
              <span>Reenviar</span>
            </button>
            <button
              className="dangerButton"
              type="button"
              onClick={() => onCancelOrder(order)}
              disabled={actionBusy}
            >
              {isBusy(actionInFlight, order.id, "cancel") ? (
                <RefreshCcw className="spinIcon" size={16} aria-hidden="true" />
              ) : (
                <XCircle size={16} aria-hidden="true" />
              )}
              <span>Cancelar</span>
            </button>
          </div>
        </article>
      ))}
    </div>
  );
}

function QueueOverview({
  deliveryQueue,
  rechargeQueue
}: {
  deliveryQueue: DeliveryQueueEntry[];
  rechargeQueue: RechargeQueueEntry[];
}) {
  const requestedOrders = deliveryQueue.filter((entry) => entry.status === "REQUESTED");
  const reassignmentOrders = deliveryQueue.filter((entry) => entry.status === "PENDING_REASSIGNMENT");

  return (
    <section className="queueGrid">
      <QueueColumn
        icon={<PackageCheck size={18} />}
        title="Aguardando planejamento"
        count={requestedOrders.length}
      >
        <DeliveryQueueList entries={requestedOrders} emptyText="Nenhum pedido aguardando planejamento." />
      </QueueColumn>

      <QueueColumn
        icon={<Route size={18} />}
        title="Reatribuicao"
        count={reassignmentOrders.length}
      >
        <DeliveryQueueList entries={reassignmentOrders} emptyText="Nenhum pedido aguardando reatribuição." />
      </QueueColumn>

      <QueueColumn
        icon={<BatteryCharging size={18} />}
        title="Recarga"
        count={rechargeQueue.length}
      >
        <RechargeQueueList entries={rechargeQueue} />
      </QueueColumn>
    </section>
  );
}

function QueueColumn({
  icon,
  title,
  count,
  children
}: {
  icon: React.ReactNode;
  title: string;
  count: number;
  children: React.ReactNode;
}) {
  return (
    <section className="toolPanel queueColumn">
      <div className="queueHeader">
        <ToolHeader icon={icon} title={title} />
        <span className="recordCount">{count}</span>
      </div>
      {children}
    </section>
  );
}

function DeliveryQueueList({ entries, emptyText }: { entries: DeliveryQueueEntry[]; emptyText: string }) {
  if (!entries.length) {
    return <p className="emptyState">{emptyText}</p>;
  }

  return (
    <div className="queueList">
      {entries.map((entry) => (
        <article className="queueItem" key={entry.orderId}>
          <div className="queueItemHeader">
            <strong>{entry.orderIdentifier}</strong>
            <span className={`priorityChip ${entry.priority.toLowerCase()}`}>{entry.priority}</span>
          </div>
          <div className="queueMeta">
            <span>
              <Weight size={14} aria-hidden="true" />
              {formatWeight(entry.weight)}
            </span>
            <span>
              <MapPin size={14} aria-hidden="true" />
              {formatLocation(entry.location.x, entry.location.y)}
            </span>
            <span>
              <CalendarDays size={14} aria-hidden="true" />
              {formatDateTime(entry.confirmedDeliveryTime)}
            </span>
            <span>
              <Clock3 size={14} aria-hidden="true" />
              {formatDateTime(entry.queuedAt)}
            </span>
          </div>
        </article>
      ))}
    </div>
  );
}

function RechargeQueueList({ entries }: { entries: RechargeQueueEntry[] }) {
  if (!entries.length) {
    return <p className="emptyState">Nenhum drone aguardando recarga.</p>;
  }

  return (
    <div className="queueList">
      {entries.map((entry) => (
        <article className="queueItem" key={entry.droneId}>
          <div className="queueItemHeader">
            <strong>{entry.droneIdentifier}</strong>
            <span className={`statusChip ${entry.status.toLowerCase()}`}>{droneStatusLabel(entry.status)}</span>
          </div>
          <BatteryMeter value={entry.batteryLevel} />
          <div className="queueMeta">
            <span>
              <Clock3 size={14} aria-hidden="true" />
              {formatNullableDateTime(entry.queuedAt)}
            </span>
            <span>{entry.reason ?? "-"}</span>
          </div>
        </article>
      ))}
    </div>
  );
}

function CreateDroneForm({
  form,
  onChange,
  onSubmit,
  busy,
  actionBusy
}: {
  form: DroneFormState;
  onChange: (field: keyof DroneFormState, value: string) => void;
  onSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
  busy: boolean;
  actionBusy: boolean;
}) {
  return (
    <form className="toolPanel" onSubmit={onSubmit}>
      <ToolHeader icon={<Plane size={18} />} title="Novo drone" />
      <div className="formGrid">
        <TextField label="Identificador" value={form.identifier} onChange={(value) => onChange("identifier", value)} required />
        <TextField
          label="Capacidade (kg)"
          type="number"
          min="0.1"
          step="0.1"
          value={form.maxWeightCapacity}
          onChange={(value) => onChange("maxWeightCapacity", value)}
          required
        />
        <TextField
          label="Alcance (km)"
          type="number"
          min="0.1"
          step="0.1"
          value={form.maxRange}
          onChange={(value) => onChange("maxRange", value)}
          required
        />
        <TextField
          label="Bateria (%)"
          type="number"
          min="0"
          max="100"
          step="0.1"
          value={form.batteryLevel}
          onChange={(value) => onChange("batteryLevel", value)}
        />
        <TextField
          label="Consumo (%/km)"
          type="number"
          min="0.1"
          step="0.1"
          value={form.batteryConsumptionPerDistanceUnit}
          onChange={(value) => onChange("batteryConsumptionPerDistanceUnit", value)}
        />
        <TextField
          label="Reserva (%)"
          type="number"
          min="0"
          max="100"
          step="0.1"
          value={form.minimumReturnBattery}
          onChange={(value) => onChange("minimumReturnBattery", value)}
        />
        <TextField
          label="Velocidade (km/h)"
          type="number"
          min="0.1"
          step="0.1"
          value={form.speed}
          onChange={(value) => onChange("speed", value)}
        />
        <TextField
          label="Recarga (%/min)"
          type="number"
          min="0.1"
          step="0.1"
          value={form.chargingRate}
          onChange={(value) => onChange("chargingRate", value)}
        />
      </div>
      <PrimaryButton icon={<Plus size={17} />} label="Criar drone" busy={busy} disabled={actionBusy} />
    </form>
  );
}

function CreateOrderForm({
  form,
  onChange,
  onSubmit,
  busy,
  actionBusy
}: {
  form: OrderFormState;
  onChange: (field: keyof OrderFormState, value: string | Priority) => void;
  onSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
  busy: boolean;
  actionBusy: boolean;
}) {
  return (
    <form className="toolPanel" onSubmit={onSubmit}>
      <ToolHeader icon={<PackageCheck size={18} />} title="Novo pedido" />
      <div className="formGrid compact">
        <TextField label="Identificador" value={form.identifier} onChange={(value) => onChange("identifier", value)} required />
        <TextField
          label="Peso (kg)"
          type="number"
          min="0.1"
          step="0.1"
          value={form.weight}
          onChange={(value) => onChange("weight", value)}
          required
          icon={<Weight size={15} />}
        />
        <TextField
          label="X (km)"
          type="number"
          step="0.1"
          value={form.x}
          onChange={(value) => onChange("x", value)}
          required
          icon={<MapPin size={15} />}
        />
        <TextField
          label="Y (km)"
          type="number"
          step="0.1"
          value={form.y}
          onChange={(value) => onChange("y", value)}
          required
          icon={<MapPin size={15} />}
        />
        <TextField
          label="Horário confirmado"
          type="datetime-local"
          value={form.confirmedDeliveryTime}
          onChange={(value) => onChange("confirmedDeliveryTime", value)}
          required
          icon={<Clock3 size={15} />}
        />
        <label className="field">
          <span>Prioridade</span>
          <select value={form.priority} onChange={(event) => onChange("priority", event.target.value as Priority)}>
            {priorities.map((priority) => (
              <option key={priority} value={priority}>
                {priority}
              </option>
            ))}
          </select>
        </label>
      </div>
      <PrimaryButton icon={<Plus size={17} />} label="Criar pedido" busy={busy} disabled={actionBusy} />
    </form>
  );
}

function TripPlanningTool({
  plannableOrders,
  availableDrones,
  plannedTrips,
  activeObstacles,
  optimizeRoute,
  onOptimizeRouteChange,
  onPlanTrips,
  busy,
  actionBusy
}: {
  plannableOrders: number;
  availableDrones: number;
  plannedTrips: number;
  activeObstacles: number;
  optimizeRoute: boolean;
  onOptimizeRouteChange: (value: boolean) => void;
  onPlanTrips: () => void;
  busy: boolean;
  actionBusy: boolean;
}) {
  return (
    <div className="toolPanel planningTool">
      <ToolHeader icon={<ListChecks size={18} />} title="Planejar viagens" />
      <div className="planningStats" aria-label="Resumo para planejamento">
        <div>
          <span>Pedidos</span>
          <strong>{plannableOrders}</strong>
        </div>
        <div>
          <span>Drones</span>
          <strong>{availableDrones}</strong>
        </div>
        <div>
          <span>Planejadas</span>
          <strong>{plannedTrips}</strong>
        </div>
        <div>
          <span>Obstáculos</span>
          <strong>{activeObstacles}</strong>
        </div>
      </div>
      <label className="toggleField">
        <input
          type="checkbox"
          checked={optimizeRoute}
          onChange={(event) => onOptimizeRouteChange(event.target.checked)}
          disabled={actionBusy}
        />
        <span>Rota otimizada</span>
      </label>
      <PrimaryButton icon={<Route size={17} />} label="Planejar" busy={busy} disabled={actionBusy} onClick={onPlanTrips} />
    </div>
  );
}

function ObstacleManager({
  form,
  obstacles,
  onChange,
  onSubmit,
  onDeactivate,
  busy,
  actionBusy,
  obstacleActionInFlight
}: {
  form: ObstacleFormState;
  obstacles: Obstacle[];
  onChange: (field: keyof ObstacleFormState, value: string) => void;
  onSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
  onDeactivate: (id: number) => void;
  busy: boolean;
  actionBusy: boolean;
  obstacleActionInFlight: number | null;
}) {
  return (
    <section className="toolPanel obstaclePanel">
      <form className="obstacleForm" onSubmit={onSubmit}>
        <ToolHeader icon={<Ban size={18} />} title="Novo obstáculo" />
        <div className="formGrid obstacleFormGrid">
          <TextField
            label="X (km)"
            type="number"
            step="0.1"
            value={form.x}
            onChange={(value) => onChange("x", value)}
            required
            icon={<MapPin size={15} />}
          />
          <TextField
            label="Y (km)"
            type="number"
            step="0.1"
            value={form.y}
            onChange={(value) => onChange("y", value)}
            required
            icon={<MapPin size={15} />}
          />
          <TextField
            label="Raio (km)"
            type="number"
            min="0.1"
            step="0.1"
            value={form.radius}
            onChange={(value) => onChange("radius", value)}
            required
            icon={<Circle size={15} />}
          />
          <PrimaryButton icon={<Plus size={17} />} label="Criar obstáculo" busy={busy} disabled={actionBusy} />
        </div>
      </form>

      <div className="obstacleList" aria-label="Obstáculos cadastrados">
        {obstacles.length ? (
          obstacles.map((obstacle) => (
            <article className="obstacleItem" key={obstacle.id}>
              <div>
                <strong>#{obstacle.id}</strong>
                <span>{formatLocation(obstacle.center.x, obstacle.center.y)}</span>
              </div>
              <div>
                <span>Raio</span>
                <strong>{formatDistance(obstacle.radius)}</strong>
              </div>
              <span className={`statusChip ${obstacle.active ? "available" : "unavailable"}`}>
                {obstacleStatusLabel(obstacle.active ? "ACTIVE" : "INACTIVE")}
              </span>
              <ActionIconButton
                label="Desativar obstáculo"
                description="Desativa este obstáculo para que ele deixe de afetar novos planejamentos."
                icon={<Trash2 size={16} />}
                disabled={actionBusy || !obstacle.active}
                busy={obstacleActionInFlight === obstacle.id}
                onClick={() => onDeactivate(obstacle.id)}
              />
            </article>
          ))
        ) : (
          <p className="emptyState">Nenhum obstáculo cadastrado.</p>
        )}
      </div>
    </section>
  );
}

function ReviewManager({
  form,
  reviews,
  onChange,
  onSubmit,
  busy,
  actionBusy
}: {
  form: ReviewFormState;
  reviews: Review[];
  onChange: (field: keyof ReviewFormState, value: string | number) => void;
  onSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
  busy: boolean;
  actionBusy: boolean;
}) {
  const visibleReviews = [...reviews].sort((left, right) => right.id - left.id);

  return (
    <section className="reviewGrid">
      <form className="toolPanel reviewFormPanel" onSubmit={onSubmit}>
        <ToolHeader icon={<Star size={18} />} title="Nova avaliação" />
        <label className="field">
          <span>Estrelas</span>
          <StarRating value={form.stars} onChange={(value) => onChange("stars", value)} disabled={actionBusy} />
        </label>
        <TextField label="Título" value={form.title} onChange={(value) => onChange("title", value)} required />
        <label className="field">
          <span>Feedback</span>
          <textarea value={form.feedback} onChange={(event) => onChange("feedback", event.target.value)} required />
        </label>
        <PrimaryButton icon={<Plus size={17} />} label="Criar avaliação" busy={busy} disabled={actionBusy} />
      </form>

      <section className="toolPanel reviewListPanel" aria-label="Feedbacks registrados">
        <ToolHeader icon={<MessageSquareText size={18} />} title="Feedbacks" />
        <div className="reviewList">
          {visibleReviews.length ? (
            visibleReviews.map((review) => (
              <article className="reviewItem" key={review.id}>
                <div className="reviewItemHeader">
                  <div>
                    <strong>{review.title}</strong>
                    <span>{formatDateTime(review.reviewedAt)}</span>
                  </div>
                  <RatingValue stars={review.stars} />
                </div>
                <p>{review.feedback}</p>
              </article>
            ))
          ) : (
            <p className="emptyState">Nenhuma avaliação registrada.</p>
          )}
        </div>
      </section>
    </section>
  );
}

function StarRating({ value, onChange, disabled }: { value: number; onChange: (value: number) => void; disabled: boolean }) {
  return (
    <div className="starRatingControl">
      {[1, 2, 3, 4, 5].map((star) => (
        <button
          className={star <= value ? "starRatingButton selected" : "starRatingButton"}
          type="button"
          key={star}
          disabled={disabled}
          onClick={() => onChange(star)}
          aria-label={`${star} estrelas`}
          title={`${star} estrelas`}
        >
          <Star size={17} aria-hidden="true" />
        </button>
      ))}
    </div>
  );
}

function RatingValue({ stars }: { stars: number }) {
  return (
    <div className="ratingValue" aria-label={`${stars} estrelas`}>
      {[1, 2, 3, 4, 5].map((star) => (
        <Star className={star <= stars ? "selected" : ""} size={14} key={star} aria-hidden="true" />
      ))}
    </div>
  );
}

function ToolHeader({ icon, title }: { icon: React.ReactNode; title: string }) {
  return (
    <div className="toolHeader">
      <span aria-hidden="true">{icon}</span>
      <h3>{title}</h3>
    </div>
  );
}

function TextField({
  label,
  value,
  onChange,
  type = "text",
  min,
  max,
  step,
  required = false,
  icon,
  disabled = false
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: "text" | "number" | "datetime-local" | "password";
  min?: string;
  max?: string;
  step?: string;
  required?: boolean;
  icon?: React.ReactNode;
  disabled?: boolean;
}) {
  return (
    <label className="field">
      <span>{label}</span>
      <div className={icon ? "inputWithIcon" : undefined}>
        {icon ? <span aria-hidden="true">{icon}</span> : null}
        <input
          type={type}
          min={min}
          max={max}
          step={step}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          required={required}
          disabled={disabled}
        />
      </div>
    </label>
  );
}

function PrimaryButton({
  icon,
  label,
  busy,
  disabled,
  onClick
}: {
  icon: React.ReactNode;
  label: string;
  busy: boolean;
  disabled: boolean;
  onClick?: () => void;
}) {
  return (
    <button className="primaryButton" type={onClick ? "button" : "submit"} disabled={disabled} onClick={onClick}>
      {busy ? <RefreshCcw className="spinIcon" size={16} aria-hidden="true" /> : icon}
      <span>{label}</span>
    </button>
  );
}

function ConfirmationDialog({
  title,
  detail,
  confirmLabel,
  cancelLabel,
  busy,
  onCancel,
  onConfirm
}: {
  title: string;
  detail: string;
  confirmLabel: string;
  cancelLabel: string;
  busy: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}) {
  return (
    <div className="modalBackdrop">
      <section className="confirmationDialog" role="dialog" aria-modal="true" aria-labelledby="demoConfirmTitle">
        <div className="confirmationHeader">
          <span aria-hidden="true">
            <AlertTriangle size={18} />
          </span>
          <div>
            <h2 id="demoConfirmTitle">{title}</h2>
            <p>{detail}</p>
          </div>
        </div>
        <div className="confirmationActions">
          <button className="secondaryButton compact" type="button" onClick={onCancel} disabled={busy}>
            <XCircle size={16} aria-hidden="true" />
            <span>{cancelLabel}</span>
          </button>
          <button className="dangerButton" type="button" onClick={onConfirm} disabled={busy}>
            {busy ? <RefreshCcw className="spinIcon" size={16} aria-hidden="true" /> : <Trash2 size={16} aria-hidden="true" />}
            <span>{confirmLabel}</span>
          </button>
        </div>
      </section>
    </div>
  );
}

function OrderCancellationDialog({
  draft,
  busy,
  onReasonChange,
  onCancel,
  onConfirm
}: {
  draft: OrderCancellationDraft;
  busy: boolean;
  onReasonChange: (reason: string) => void;
  onCancel: () => void;
  onConfirm: () => void;
}) {
  const reasonIsBlank = !draft.reason.trim();

  return (
    <div className="modalBackdrop">
      <section className="confirmationDialog" role="dialog" aria-modal="true" aria-labelledby="orderCancelTitle">
        <div className="confirmationHeader">
          <span aria-hidden="true">
            <AlertTriangle size={18} />
          </span>
          <div>
            <h2 id="orderCancelTitle">Cancelar pedido {draft.identifier}</h2>
            <p>Informe a justificativa que será exibida para o cliente.</p>
          </div>
        </div>
        <label className="field reasonField">
          <span>Justificativa</span>
          <textarea
            value={draft.reason}
            onChange={(event) => onReasonChange(event.target.value)}
            placeholder="Ex.: Endereço fora da área atendida pela frota disponível."
            disabled={busy}
            required
          />
        </label>
        <div className="confirmationActions">
          <button className="secondaryButton compact" type="button" onClick={onCancel} disabled={busy}>
            <XCircle size={16} aria-hidden="true" />
            <span>Voltar</span>
          </button>
          <button className="dangerButton" type="button" onClick={onConfirm} disabled={busy || reasonIsBlank}>
            {busy ? <RefreshCcw className="spinIcon" size={16} aria-hidden="true" /> : <Trash2 size={16} aria-hidden="true" />}
            <span>Cancelar pedido</span>
          </button>
        </div>
      </section>
    </div>
  );
}

function ArrivalNotificationDialog({
  notification,
  busy,
  onConfirm,
  onDecline
}: {
  notification: ArrivalNotificationState;
  busy: boolean;
  onConfirm: () => void;
  onDecline: () => void;
}) {
  return (
    <div className="modalBackdrop">
      <section className="arrivalNotificationDialog" role="alertdialog" aria-modal="true" aria-labelledby="arrivalTitle">
        <div className="arrivalNotificationIcon" aria-hidden="true">
          <Plane size={24} />
        </div>
        <p className="eyebrow">Entrega chegando</p>
        <h2 id="arrivalTitle">Você está disponível para receber?</h2>
        <p>
          Drone #{notification.droneId} está chegando ao destino do pedido {notification.orderIdentifier}. Responda até{" "}
          {formatNullableDateTime(notification.deadline)}; sem resposta, o drone retorna para a base com o pacote.
        </p>
        <div className="confirmationActions">
          <button className="secondaryButton compact" type="button" onClick={onDecline} disabled={busy}>
            <XCircle size={16} aria-hidden="true" />
            <span>Não posso receber</span>
          </button>
          <button className="primaryButton compact" type="button" onClick={onConfirm} disabled={busy}>
            {busy ? <RefreshCcw className="spinIcon" size={16} aria-hidden="true" /> : <CheckCircle2 size={16} aria-hidden="true" />}
            <span>Estou disponível</span>
          </button>
        </div>
      </section>
    </div>
  );
}

function TrackingCodeDialog({
  code,
  onClose,
  onTrack
}: {
  code: string;
  onClose: () => void;
  onTrack: () => void;
}) {
  return (
    <div className="modalBackdrop">
      <section className="trackingCodeDialog" role="dialog" aria-modal="true" aria-labelledby="trackingCodeTitle">
        <div className="trackingCodeIcon" aria-hidden="true">
          <CheckCircle2 size={22} />
        </div>
        <p className="eyebrow">Pedido solicitado</p>
        <h2 id="trackingCodeTitle">Código do pedido</h2>
        <div className="trackingCodeLine">
          <span>Rastreio / confirmação</span>
          <strong className="trackingCodeValue">{code}</strong>
        </div>
        <p className="trackingCodeHint">Use o mesmo código para acompanhar o pedido e confirmar a entrega quando o drone chegar.</p>
        <div className="confirmationActions">
          <button className="secondaryButton compact" type="button" onClick={onClose}>
            <XCircle size={16} aria-hidden="true" />
            <span>Fechar</span>
          </button>
          <button className="primaryButton compact" type="button" onClick={onTrack}>
            <Search size={16} aria-hidden="true" />
            <span>Acompanhar</span>
          </button>
        </div>
      </section>
    </div>
  );
}

function MetricCard({
  icon,
  label,
  value,
  detail
}: {
  icon: React.ReactNode;
  label: string;
  value: string | number;
  detail: string;
}) {
  return (
    <article className="metricCard">
      <div className="metricIcon" aria-hidden="true">
        {icon}
      </div>
      <div>
        <p className="metricLabel">{label}</p>
        <p className="metricValue">{value}</p>
        <p className="metricDetail">{detail}</p>
      </div>
    </article>
  );
}

function PanelHeader({ title, count }: { title: string; count: number }) {
  return (
    <div className="panelHeader">
      <h2>{title}</h2>
      <span>{count}</span>
    </div>
  );
}

function StatusRows<T extends string>({
  statuses,
  counts,
  labelFor
}: {
  statuses: T[];
  counts: Record<T, number>;
  labelFor: (status: T) => string;
}) {
  return (
    <div className="statusRows">
      {statuses.map((status) => (
        <div className="statusRow" key={status}>
          <span className={`statusChip ${status.toLowerCase()}`}>{labelFor(status)}</span>
          <strong>{counts[status] ?? 0}</strong>
        </div>
      ))}
    </div>
  );
}

function ProductivityReportPanel({
  report,
  selectedMonth,
  loading,
  onMonthChange,
  onPreviousMonth,
  onNextMonth,
  onCurrentMonth
}: {
  report: ProductivityReport | null;
  selectedMonth: string;
  loading: boolean;
  onMonthChange: (month: string) => void;
  onPreviousMonth: () => void;
  onNextMonth: () => void;
  onCurrentMonth: () => void;
}) {
  const currentMonth = currentReportMonth();
  const nextMonthDisabled = selectedMonth >= currentMonth;

  if (!report) {
    return (
      <section className="productivityGrid" aria-label="Relatório mensal de produtividade">
        <div className="productivityPanel">
          <div className="operationHeader compact reportHeader">
            <ToolHeader icon={<BarChart3 size={18} />} title="Produtividade mensal" />
            <ReportMonthControls
              selectedMonth={selectedMonth}
              currentMonth={currentMonth}
              loading={loading}
              nextMonthDisabled={nextMonthDisabled}
              onMonthChange={onMonthChange}
              onPreviousMonth={onPreviousMonth}
              onNextMonth={onNextMonth}
              onCurrentMonth={onCurrentMonth}
            />
          </div>
          <p className="emptyState">Relatório mensal indisponível.</p>
        </div>
      </section>
    );
  }

  const funnel = [
    { label: "Entradas", value: report.orderEntries, className: "entries" },
    { label: "Enviados", value: report.ordersSent, className: "sent" },
    { label: "Entregues", value: report.ordersDelivered, className: "delivered" },
    { label: "Cancelados", value: report.ordersCancelled, className: "cancelled" }
  ];
  const maxValue = Math.max(...funnel.map((item) => item.value), 1);
  const topDrones = report.drones.slice(0, 5);

  return (
    <section className="productivityGrid" aria-label="Relatório mensal de produtividade">
      <div className="productivityPanel">
        <div className="operationHeader compact reportHeader">
          <ToolHeader icon={<BarChart3 size={18} />} title="Produtividade mensal" />
          <ReportMonthControls
            selectedMonth={selectedMonth}
            currentMonth={currentMonth}
            loading={loading}
            nextMonthDisabled={nextMonthDisabled}
            onMonthChange={onMonthChange}
            onPreviousMonth={onPreviousMonth}
            onNextMonth={onNextMonth}
            onCurrentMonth={onCurrentMonth}
          />
        </div>
        <span className="reportMonthLabel">{formatReportMonth(report.month)}</span>
        <div className="funnelList">
          {funnel.map((item) => (
            <div className={`funnelRow ${item.className}`} key={item.label}>
              <div className="funnelMeta">
                <span>{item.label}</span>
                <strong>{item.value}</strong>
              </div>
              <div className="funnelTrack" aria-hidden="true">
                <div style={{ width: `${Math.max(8, (item.value / maxValue) * 100)}%` }} />
              </div>
            </div>
          ))}
        </div>
        <div className="productivitySummary">
          <DetailStat label="Taxa entrega" value={`${Math.round(report.conversionRate * 100)}%`} />
          <DetailStat label="Atualizado" value={formatDateTime(report.generatedAt)} />
        </div>
      </div>

      <div className="productivityPanel">
        <div className="operationHeader compact">
          <ToolHeader icon={<Plane size={18} />} title="Drones por desempenho" />
          <span className="recordCount">{topDrones.length} drones</span>
        </div>
        {topDrones.length ? (
          <div className="droneProductivityList">
            {topDrones.map((drone, index) => (
              <article className="droneProductivityRow" key={drone.droneId}>
                <div className="droneRank">{index + 1}</div>
                <div className="droneProductivityMain">
                  <strong>{drone.droneIdentifier}</strong>
                  <span>{drone.tripsStarted} viagens iniciadas</span>
                </div>
                <div className="droneProductivityStats">
                  <span>{drone.ordersDelivered} entregas</span>
                  <span>{drone.tripsCompleted} concluídas</span>
                  <span>{drone.tripsCancelled} canceladas</span>
                  <span>{drone.tripsReturnedEarly} retornos</span>
                </div>
              </article>
            ))}
          </div>
        ) : (
          <p className="emptyState">Nenhum drone cadastrado para a competência.</p>
        )}
      </div>
    </section>
  );
}

function ReportMonthControls({
  selectedMonth,
  currentMonth,
  loading,
  nextMonthDisabled,
  onMonthChange,
  onPreviousMonth,
  onNextMonth,
  onCurrentMonth
}: {
  selectedMonth: string;
  currentMonth: string;
  loading: boolean;
  nextMonthDisabled: boolean;
  onMonthChange: (month: string) => void;
  onPreviousMonth: () => void;
  onNextMonth: () => void;
  onCurrentMonth: () => void;
}) {
  return (
    <div className="reportMonthControls" aria-label="Selecionar mês do relatório">
      <button
        className="rowActionButton"
        type="button"
        onClick={onPreviousMonth}
        disabled={loading}
        aria-label="Mês anterior"
        title="Mês anterior"
      >
        {loading ? <RefreshCcw className="spinIcon" size={15} aria-hidden="true" /> : <ChevronLeft size={16} aria-hidden="true" />}
      </button>
      <label className="monthPicker">
        <CalendarDays size={15} aria-hidden="true" />
        <span className="srOnly">Mês do relatório</span>
        <input
          type="month"
          value={selectedMonth}
          max={currentMonth}
          onChange={(event) => onMonthChange(event.target.value)}
          disabled={loading}
        />
      </label>
      <button
        className="rowActionButton"
        type="button"
        onClick={onNextMonth}
        disabled={loading || nextMonthDisabled}
        aria-label="Próximo mês"
        title="Próximo mês"
      >
        <ChevronRight size={16} aria-hidden="true" />
      </button>
      <button
        className="secondaryButton compact"
        type="button"
        onClick={onCurrentMonth}
        disabled={loading || selectedMonth === currentMonth}
      >
        <span>Mês atual</span>
      </button>
    </div>
  );
}

function OperationalTable({
  activeTable,
  rows,
  onDroneAction,
  onCancelOrder,
  onRequeueOrder,
  onTripAction,
  onSelectTrip,
  selectedTripId,
  droneActionInFlight,
  orderActionInFlight,
  tripActionInFlight,
  actionBusy,
  telemetryDrafts,
  onTelemetryDraftChange
}: {
  activeTable: TableView;
  rows: Drone[] | Order[] | Trip[];
  onDroneAction: (id: number, action: DroneAction) => void;
  onCancelOrder: (order: Order) => void;
  onRequeueOrder: (id: number) => void;
  onTripAction: (id: number, action: TripAction, options?: TripActionOptions) => void;
  onSelectTrip: (id: number) => void;
  selectedTripId: number | null;
  droneActionInFlight: { id: number; action: DroneAction } | null;
  orderActionInFlight: { id: number; action: OrderAction } | null;
  tripActionInFlight: { id: number; action: TripAction } | null;
  actionBusy: boolean;
  telemetryDrafts: Record<number, string>;
  onTelemetryDraftChange: (id: number, value: string) => void;
}) {
  if (!rows.length) {
    return <p className="tableEmpty">Nenhum registro encontrado.</p>;
  }

  if (activeTable === "drones") {
    return (
      <DroneTable
        rows={rows as Drone[]}
        onDroneAction={onDroneAction}
        actionInFlight={droneActionInFlight}
        actionBusy={actionBusy}
      />
    );
  }

  if (activeTable === "orders") {
    return (
      <OrderTable
        rows={rows as Order[]}
        onCancelOrder={onCancelOrder}
        onRequeueOrder={onRequeueOrder}
        actionInFlight={orderActionInFlight}
        actionBusy={actionBusy}
      />
    );
  }

  return (
    <TripTable
      rows={rows as Trip[]}
      onTripAction={onTripAction}
      onSelectTrip={onSelectTrip}
      selectedTripId={selectedTripId}
      actionInFlight={tripActionInFlight}
      actionBusy={actionBusy}
      telemetryDrafts={telemetryDrafts}
      onTelemetryDraftChange={onTelemetryDraftChange}
    />
  );
}

function DroneTable({
  rows,
  onDroneAction,
  actionInFlight,
  actionBusy
}: {
  rows: Drone[];
  onDroneAction: (id: number, action: DroneAction) => void;
  actionInFlight: { id: number; action: DroneAction } | null;
  actionBusy: boolean;
}) {
  return (
    <div className="tableScroller">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Identificador</th>
            <th>Status</th>
            <th>Bateria (%)</th>
            <th>Capacidade (kg)</th>
            <th>Alcance (km)</th>
            <th>Velocidade (km/h)</th>
            <th>Recarga</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((drone) => (
            <tr key={drone.id}>
              <td>{drone.id}</td>
              <td className="strongCell">{drone.identifier}</td>
              <td>
                <span className={`statusChip ${drone.status.toLowerCase()}`}>{droneStatusLabel(drone.status)}</span>
              </td>
              <td>
                <BatteryMeter value={drone.batteryLevel} />
              </td>
              <td>{formatWeight(drone.maxWeightCapacity)}</td>
              <td>{formatDistance(drone.maxRange)}</td>
              <td>{formatSpeed(drone.speed)}</td>
              <td>{drone.rechargeReason ?? "-"}</td>
              <td>
                <DroneActions
                  drone={drone}
                  onDroneAction={onDroneAction}
                  actionInFlight={actionInFlight}
                  actionBusy={actionBusy}
                />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function DroneActions({
  drone,
  onDroneAction,
  actionInFlight,
  actionBusy
}: {
  drone: Drone;
  onDroneAction: (id: number, action: DroneAction) => void;
  actionInFlight: { id: number; action: DroneAction } | null;
  actionBusy: boolean;
}) {
  return (
    <div className="rowActions">
      <ActionIconButton
        label="Marcar indisponível"
        description="Retira este drone dos próximos planejamentos enquanto ele estiver fora de operação."
        icon={<PowerOff size={16} />}
        disabled={actionBusy || drone.status !== "AVAILABLE"}
        busy={isBusy(actionInFlight, drone.id, "markUnavailable")}
        onClick={() => onDroneAction(drone.id, "markUnavailable")}
      />
      <ActionIconButton
        label="Marcar disponível"
        description="Devolve este drone para a lista de drones disponíveis para planejamento."
        icon={<CheckCircle2 size={16} />}
        disabled={actionBusy || drone.status !== "UNAVAILABLE"}
        busy={isBusy(actionInFlight, drone.id, "markAvailable")}
        onClick={() => onDroneAction(drone.id, "markAvailable")}
      />
      <ActionIconButton
        label="Enviar para recarga"
        description="Move este drone disponível para a fila de recarga."
        icon={<BatteryCharging size={16} />}
        disabled={actionBusy || drone.status !== "AVAILABLE" || drone.batteryLevel >= 100}
        busy={isBusy(actionInFlight, drone.id, "enqueueRecharge")}
        onClick={() => onDroneAction(drone.id, "enqueueRecharge")}
      />
      <ActionIconButton
        label="Concluir recarga"
        description="Finaliza a recarga, restaura a bateria para 100% e deixa o drone disponível."
        icon={<BatteryFull size={16} />}
        disabled={actionBusy || drone.status !== "CHARGING"}
        busy={isBusy(actionInFlight, drone.id, "completeRecharge")}
        onClick={() => onDroneAction(drone.id, "completeRecharge")}
      />
      <ActionIconButton
        label="Excluir drone"
        description="Remove drones sem viagens vinculadas; drones em rota não podem ser excluídos."
        icon={<Trash2 size={16} />}
        disabled={actionBusy || drone.status === "IN_ROUTE"}
        busy={isBusy(actionInFlight, drone.id, "delete")}
        onClick={() => onDroneAction(drone.id, "delete")}
      />
    </div>
  );
}

function ActionIconButton({
  label,
  description,
  icon,
  disabled,
  busy,
  onClick
}: {
  label: string;
  description?: string;
  icon: React.ReactNode;
  disabled: boolean;
  busy: boolean;
  onClick: () => void;
}) {
  const tooltip = description ?? label;

  return (
    <span className="actionTooltip" title={tooltip}>
      <button className="rowActionButton" type="button" disabled={disabled} onClick={onClick} aria-label={tooltip}>
        {busy ? <RefreshCcw className="spinIcon" size={15} aria-hidden="true" /> : icon}
      </button>
    </span>
  );
}

function OrderTable({
  rows,
  onCancelOrder,
  onRequeueOrder,
  actionInFlight,
  actionBusy
}: {
  rows: Order[];
  onCancelOrder: (order: Order) => void;
  onRequeueOrder: (id: number) => void;
  actionInFlight: { id: number; action: OrderAction } | null;
  actionBusy: boolean;
}) {
  return (
    <div className="tableScroller">
      <table className="orderTable">
        <thead>
          <tr>
            <th>ID</th>
            <th>Identificador</th>
            <th>Status</th>
            <th>Prioridade</th>
            <th>Peso (kg)</th>
            <th>Localização (km)</th>
            <th>Horário confirmado</th>
            <th>Entrada na fila</th>
            <th>Mensagem</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((order) => (
            <tr key={order.id}>
              <td>{order.id}</td>
              <td className="strongCell">{order.identifier}</td>
              <td>
                <span className={`statusChip ${order.status.toLowerCase()}`}>{orderStatusLabel(order.status)}</span>
              </td>
              <td>
                <span className={`priorityChip ${order.priority.toLowerCase()}`}>{order.priority}</span>
              </td>
              <td>{formatWeight(order.weight)}</td>
              <td>{formatLocation(order.location.x, order.location.y)}</td>
              <td>{formatDateTime(order.confirmedDeliveryTime)}</td>
              <td>{formatDateTime(order.queuedAt)}</td>
              <td className="reasonCell">{localizedStatusReason(order.statusReason) ?? unallocatedOrderMessage(order)}</td>
              <td>
                <OrderActions
                  order={order}
                  onCancelOrder={onCancelOrder}
                  onRequeueOrder={onRequeueOrder}
                  actionInFlight={actionInFlight}
                  actionBusy={actionBusy}
                />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function OrderActions({
  order,
  onCancelOrder,
  onRequeueOrder,
  actionInFlight,
  actionBusy
}: {
  order: Order;
  onCancelOrder: (order: Order) => void;
  onRequeueOrder: (id: number) => void;
  actionInFlight: { id: number; action: OrderAction } | null;
  actionBusy: boolean;
}) {
  const canAct = order.status === "UNALLOCATED";

  return (
    <div className="rowActions">
      <ActionIconButton
        label="Cancelar pedido"
        description="Cancela o pedido não alocado e registra a justificativa para o cliente."
        icon={<XCircle size={16} />}
        disabled={actionBusy || !canAct}
        busy={isBusy(actionInFlight, order.id, "cancel")}
        onClick={() => onCancelOrder(order)}
      />
      <ActionIconButton
        label="Reenviar para planejamento"
        description="Retorna o pedido não alocado para a fila de planejamento."
        icon={<RefreshCcw size={16} />}
        disabled={actionBusy || !canAct}
        busy={isBusy(actionInFlight, order.id, "requeue")}
        onClick={() => onRequeueOrder(order.id)}
      />
    </div>
  );
}

function TripTable({
  rows,
  onTripAction,
  onSelectTrip,
  selectedTripId,
  actionInFlight,
  actionBusy,
  telemetryDrafts,
  onTelemetryDraftChange
}: {
  rows: Trip[];
  onTripAction: (id: number, action: TripAction, options?: TripActionOptions) => void;
  onSelectTrip: (id: number) => void;
  selectedTripId: number | null;
  actionInFlight: { id: number; action: TripAction } | null;
  actionBusy: boolean;
  telemetryDrafts: Record<number, string>;
  onTelemetryDraftChange: (id: number, value: string) => void;
}) {
  return (
    <div className="tableScroller">
      <table className="tripTable">
        <thead>
          <tr>
            <th>ID</th>
            <th>Drone</th>
            <th>Status</th>
            <th>Pedidos</th>
            <th>Progresso</th>
            <th>Distância (km)</th>
            <th>Duração</th>
            <th>Tempo médio</th>
            <th>Saída ideal</th>
            <th>Telemetria</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((trip) => (
            <tr className={selectedTripId === trip.id ? "selectedRow" : undefined} key={trip.id}>
              <td>{trip.id}</td>
              <td className="strongCell">#{trip.droneId}</td>
              <td>
                <span className={`statusChip ${trip.status.toLowerCase()}`}>{tripStatusLabel(trip.status)}</span>
              </td>
              <td>{trip.orders.length}</td>
              <td>
                {deliveredCount(trip)} / {trip.routeProgress.length}
              </td>
              <td>{formatDistance(trip.totalDistance)}</td>
              <td>{formatDuration(trip.estimatedDuration)}</td>
              <td>{formatDuration(trip.averageDeliveryTime)}</td>
              <td>{formatNullableDateTime(trip.idealDispatchTime)}</td>
              <td>
                <TelemetryControl
                  trip={trip}
                  value={telemetryDrafts[trip.id] ?? ""}
                  onChange={(value) => onTelemetryDraftChange(trip.id, value)}
                  onSubmit={(batteryLevel) => onTripAction(trip.id, "sendTelemetry", { batteryLevel })}
                  actionInFlight={actionInFlight}
                  actionBusy={actionBusy}
                />
              </td>
              <td>
                <TripActions
                  trip={trip}
                  onTripAction={onTripAction}
                  onSelectTrip={onSelectTrip}
                  actionInFlight={actionInFlight}
                  actionBusy={actionBusy}
                />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function TelemetryControl({
  trip,
  value,
  onChange,
  onSubmit,
  actionInFlight,
  actionBusy
}: {
  trip: Trip;
  value: string;
  onChange: (value: string) => void;
  onSubmit: (batteryLevel: number) => void;
  actionInFlight: { id: number; action: TripAction } | null;
  actionBusy: boolean;
}) {
  const batteryLevel = Number(value);
  const canSubmit = trip.status === "IN_ROUTE" && isValidBatteryInput(value);

  return (
    <div className="telemetryControl">
      <label className="telemetryInput">
        <span className="srOnly">Bateria da viagem {trip.id}</span>
        <input
          type="number"
          min="0"
          max="100"
          step="0.1"
          inputMode="decimal"
          value={value}
          onChange={(event) => onChange(event.target.value)}
          placeholder="%"
          title="Informe a bateria atual do drone em porcentagem."
          disabled={actionBusy || trip.status !== "IN_ROUTE"}
        />
      </label>
      <ActionIconButton
        label="Enviar telemetria"
        description="Registra a bateria informada; se a rota deixar de ser segura, aciona retorno antecipado."
        icon={<Send size={16} />}
        disabled={actionBusy || !canSubmit}
        busy={isBusy(actionInFlight, trip.id, "sendTelemetry")}
        onClick={() => onSubmit(batteryLevel)}
      />
    </div>
  );
}

function TripActions({
  trip,
  onTripAction,
  onSelectTrip,
  actionInFlight,
  actionBusy
}: {
  trip: Trip;
  onTripAction: (id: number, action: TripAction, options?: TripActionOptions) => void;
  onSelectTrip: (id: number) => void;
  actionInFlight: { id: number; action: TripAction } | null;
  actionBusy: boolean;
}) {
  const nextRoutePosition = nextUndeliveredRoutePosition(trip);
  const canCancel = trip.status === "PLANNED" || trip.status === "IN_ROUTE";
  const awaitingClientConfirmation = trip.status === "IN_ROUTE" && nextRoutePosition !== null;
  const canStart = trip.status === "PLANNED" && trip.dispatchWindowOpen;

  return (
    <div className="rowActions">
      <ActionIconButton
        label="Ver detalhes"
        description="Seleciona esta viagem no painel de detalhe, exibindo rota, mapa e telemetria."
        icon={<Eye size={16} />}
        disabled={actionBusy}
        busy={false}
        onClick={() => onSelectTrip(trip.id)}
      />
      <ActionIconButton
        label="Iniciar viagem"
        description={
          trip.status === "PLANNED" && !trip.dispatchWindowOpen
            ? `A viagem aguardará até ${formatNullableDateTime(trip.idealDispatchTime)} para sair no prazo.`
            : "Inicia a viagem planejada e coloca o drone e os pedidos em rota."
        }
        icon={<Play size={16} />}
        disabled={actionBusy || !canStart}
        busy={isBusy(actionInFlight, trip.id, "start")}
        onClick={() => onTripAction(trip.id, "start")}
      />
      <ActionIconButton
        label="Aguardando cliente"
        description={
          awaitingClientConfirmation
            ? `A entrega da posição ${nextRoutePosition} depende do código informado pelo cliente.`
            : "Não há entrega aguardando confirmação do cliente."
        }
        icon={<KeyRound size={16} />}
        disabled
        busy={false}
        onClick={() => undefined}
      />
      <ActionIconButton
        label="Concluir viagem"
        description="Finaliza a viagem em rota depois das confirmações; se não houver bateria segura para completar, registra retorno antecipado."
        icon={<Flag size={16} />}
        disabled={actionBusy || trip.status !== "IN_ROUTE"}
        busy={isBusy(actionInFlight, trip.id, "complete")}
        onClick={() => onTripAction(trip.id, "complete")}
      />
      <ActionIconButton
        label="Cancelar viagem"
        description="Cancela uma viagem planejada ou em rota e devolve pedidos não entregues para replanejamento."
        icon={<XCircle size={16} />}
        disabled={actionBusy || !canCancel}
        busy={isBusy(actionInFlight, trip.id, "cancel")}
        onClick={() => onTripAction(trip.id, "cancel")}
      />
    </div>
  );
}

function BatteryMeter({ value }: { value: number }) {
  return (
    <div className="batteryMeter" aria-label={`Bateria ${formatNumber(value)} por cento`}>
      <div className="batteryTrack">
        <div className={value < 30 ? "batteryFill low" : "batteryFill"} style={{ width: `${Math.max(0, Math.min(100, value))}%` }} />
      </div>
      <span>{formatNumber(value)}%</span>
    </div>
  );
}

function buildMetrics(snapshot: DashboardSnapshot) {
  const activeTrips = snapshot.trips.filter((trip) => trip.status === "IN_ROUTE").length;
  const plannedTrips = snapshot.trips.filter((trip) => trip.status === "PLANNED").length;
  const availableDrones = snapshot.drones.filter((drone) => drone.status === "AVAILABLE").length;
  const dronesInRoute = snapshot.drones.filter((drone) => drone.status === "IN_ROUTE").length;
  const lowBatteryDrones = snapshot.drones.filter((drone) => drone.batteryLevel < 30).length;
  const activeObstacles = snapshot.obstacles.filter((obstacle) => obstacle.active).length;
  const plannableOrders = snapshot.orders.filter((order) => ["REQUESTED", "PENDING_REASSIGNMENT"].includes(order.status)).length;
  const queueTotal = snapshot.deliveryQueue.length + snapshot.rechargeQueue.length;
  const pendingOrders = snapshot.orders.filter((order) =>
    ["REQUESTED", "PENDING_REASSIGNMENT", "UNALLOCATED"].includes(order.status)
  ).length;
  const deliveredOrders = snapshot.orders.filter((order) => order.status === "DELIVERED").length;
  const averageDeliveryTime = average(snapshot.trips.map((trip) => trip.averageDeliveryTime).filter(Boolean));
  const averageStars = average(snapshot.reviews.map((review) => review.stars));

  return {
    activeTrips,
    plannedTrips,
    availableDrones,
    dronesInRoute,
    lowBatteryDrones,
    activeObstacles,
    plannableOrders,
    queueTotal,
    pendingOrders,
    deliveredOrders,
    averageDeliveryTime,
    averageStars
  };
}

function buildJourneySteps(
  snapshot: DashboardSnapshot,
  selectedTrip: Trip | null,
  telemetryHistory: TripTelemetry[]
): JourneyStep[] {
  const activeObstacles = snapshot.obstacles.filter((obstacle) => obstacle.active).length;
  const availableDrones = snapshot.drones.filter((drone) => drone.status === "AVAILABLE").length;
  const plannableOrders = snapshot.orders.filter((order) => ["REQUESTED", "PENDING_REASSIGNMENT"].includes(order.status)).length;
  const plannedTrips = snapshot.trips.filter((trip) => trip.status === "PLANNED").length;
  const activeTrips = snapshot.trips.filter((trip) => trip.status === "IN_ROUTE").length;
  const completedTrips = snapshot.trips.filter((trip) => trip.status === "COMPLETED").length;
  const returnedTrips = snapshot.trips.filter((trip) => trip.status === "RETURNED_EARLY").length;
  const hasRegistration = snapshot.drones.length > 0 && snapshot.orders.length > 0;
  const hasTripPlan = snapshot.trips.length > 0;
  const hasStartedTrip = snapshot.trips.some((trip) => ["IN_ROUTE", "RETURNED_EARLY", "COMPLETED"].includes(trip.status));
  const hasDeliveredPosition = snapshot.trips.some((trip) => trip.routeProgress.some((progress) => progress.delivered));
  const hasPendingDeliveryInRoute = snapshot.trips.some(
    (trip) => trip.status === "IN_ROUTE" && nextUndeliveredRoutePosition(trip) !== null
  );
  const selectedDeliveryCount = selectedTrip ? deliveredCount(selectedTrip) : 0;
  const selectedRouteCount = selectedTrip?.routeProgress.length ?? 0;

  return [
    {
      title: "Cadastro",
      detail: `${snapshot.drones.length} drones, ${snapshot.orders.length} pedidos`,
      status: hasRegistration ? "done" : "pending",
      href: "#planning",
      actionLabel: "Abrir cadastro",
      icon: <Plus size={18} />
    },
    {
      title: "Obstáculo",
      detail: `${activeObstacles} ativos`,
      status: activeObstacles > 0 ? "done" : "optional",
      href: "#obstacles",
      actionLabel: "Abrir obstáculos",
      icon: <Ban size={18} />
    },
    {
      title: "Planejamento",
      detail: `${plannableOrders} elegíveis, ${availableDrones} disponíveis`,
      status: hasTripPlan ? "done" : availableDrones > 0 && plannableOrders > 0 ? "ready" : "pending",
      href: "#planning",
      actionLabel: "Abrir plano",
      icon: <Route size={18} />
    },
    {
      title: "Inicio",
      detail: `${plannedTrips} planejadas, ${activeTrips} em rota`,
      status: hasStartedTrip ? "done" : plannedTrips > 0 ? "ready" : "pending",
      href: "#operation",
      actionLabel: "Abrir viagens",
      table: "trips",
      icon: <Play size={18} />
    },
    {
      title: "Entregas",
      detail: selectedTrip ? `${selectedDeliveryCount} / ${selectedRouteCount} na selecionada` : "sem viagem selecionada",
      status: hasDeliveredPosition ? "done" : hasPendingDeliveryInRoute ? "ready" : "pending",
      href: "#operation",
      actionLabel: "Abrir entregas",
      table: "trips",
      icon: <PackageCheck size={18} />
    },
    {
      title: "Telemetria",
      detail: `${telemetryHistory.length} leituras na selecionada`,
      status: telemetryHistory.length > 0 ? "done" : selectedTrip?.status === "IN_ROUTE" ? "ready" : "pending",
      href: "#trip-detail",
      actionLabel: "Abrir detalhe",
      icon: <Send size={18} />
    },
    {
      title: "Encerramento",
      detail: `${completedTrips} concluídas, ${returnedTrips} retornos`,
      status: completedTrips > 0 || returnedTrips > 0 ? "done" : activeTrips > 0 ? "ready" : "pending",
      href: "#operation",
      actionLabel: "Abrir ações",
      table: "trips",
      icon: <Flag size={18} />
    }
  ];
}

function journeyStatusLabel(status: JourneyStatus) {
  const labels: Record<JourneyStatus, string> = {
    done: "Concluído",
    ready: "Pronto",
    optional: "Opcional",
    pending: "Pendente"
  };

  return labels[status];
}

function droneStatusLabel(status: DroneStatus) {
  return droneStatusLabels[status];
}

function orderStatusLabel(status: OrderStatus) {
  return orderStatusLabels[status];
}

function tripStatusLabel(status: TripStatus) {
  return tripStatusLabels[status];
}

function routeProgressStatusLabel(status: RouteProgressStatus) {
  return routeProgressStatusLabels[status];
}

function routeProgressDisplayStatus(progress: Trip["routeProgress"][number]): RouteProgressStatus {
  if (progress.delivered) {
    return "DELIVERED";
  }

  if (progress.deliveryFailedAt) {
    return "NOT_DELIVERED";
  }

  return "PENDING";
}

function routeProgressMarkerClass(status: RouteProgressStatus) {
  if (status === "DELIVERED") {
    return "delivered";
  }

  if (status === "NOT_DELIVERED") {
    return "failed";
  }

  return "";
}

function statusChipClass(status: RouteProgressStatus) {
  return status.toLowerCase();
}

function orderStatusClass(status: OrderStatus) {
  return status.toLowerCase();
}

function adminSectionTitle(section: AdminSection) {
  const labels: Record<AdminSection, string> = {
    overview: "Painel",
    operations: "Operação",
    planning: "Planejamento",
    feedback: "Feedback"
  };

  return labels[section];
}

function clientSectionTitle(section: ClientSection) {
  const labels: Record<ClientSection, string> = {
    order: "Solicitar entrega",
    myOrders: "Meus pedidos",
    tracking: "Acompanhar entrega",
    reviews: "Avaliações"
  };

  return labels[section];
}

function obstacleStatusLabel(status: ObstacleDisplayStatus) {
  return obstacleStatusLabels[status];
}

function tableStatusLabel(activeTable: TableView, status: DroneStatus | OrderStatus | TripStatus) {
  if (activeTable === "drones") {
    return droneStatusLabel(status as DroneStatus);
  }

  if (activeTable === "orders") {
    return orderStatusLabel(status as OrderStatus);
  }

  return tripStatusLabel(status as TripStatus);
}

function mapRouteModeLabel(mode: MapRouteMode) {
  return mapRouteModeLabels[mode];
}

function mapRouteModeDescription(mode: MapRouteMode) {
  const descriptions: Record<MapRouteMode, string> = {
    selected: "Mostra somente a rota da viagem selecionada no detalhe.",
    all: "Mostra as rotas de todas as viagens cadastradas, com cores diferentes."
  };

  return descriptions[mode];
}

function apiStatusLabel(status: ApiStatus) {
  const labels: Record<ApiStatus, string> = {
    checking: "API verificando",
    online: "API online",
    offline: "API offline"
  };

  return labels[status];
}

function buildAlerts(snapshot: DashboardSnapshot) {
  const alerts: string[] = [];
  const pendingReassignment = snapshot.orders.filter((order) => order.status === "PENDING_REASSIGNMENT").length;
  const unallocated = snapshot.orders.filter((order) => order.status === "UNALLOCATED").length;
  const notDelivered = snapshot.orders.filter((order) => order.status === "NOT_DELIVERED").length;
  const returnedEarly = snapshot.trips.filter((trip) => trip.status === "RETURNED_EARLY").length;
  const activeObstacles = snapshot.obstacles.filter((obstacle) => obstacle.active).length;
  const lowBatteryAvailable = snapshot.drones.filter(
    (drone) => drone.status === "AVAILABLE" && drone.batteryLevel < 30
  ).length;

  if (pendingReassignment) {
    alerts.push(`${pendingReassignment} pedidos aguardando reatribuição`);
  }

  if (unallocated) {
    alerts.push(`${unallocated} pedidos não alocados. Cancele com justificativa ou reenvie para planejamento.`);
  }

  if (notDelivered) {
    alerts.push(`${notDelivered} pacotes marcados como não entregues por ausência de disponibilidade do cliente.`);
  }

  if (returnedEarly) {
    alerts.push(`${returnedEarly} viagens com retorno antecipado`);
  }

  if (lowBatteryAvailable) {
    alerts.push(`${lowBatteryAvailable} drones disponíveis com bateria abaixo de 30%`);
  }

  if (activeObstacles) {
    alerts.push(`${activeObstacles} obstáculos ativos afetando próximos planejamentos`);
  }

  return alerts;
}

function unallocatedOrderMessage(order: Order) {
  if (order.status === "UNALLOCATED") {
    return "Pedido não alocado. Cancele com justificativa ou reenvie para planejamento.";
  }

  if (order.status === "CANCELLED") {
    return "Pedido cancelado.";
  }

  if (order.status === "NOT_DELIVERED") {
    return "Pacote não entregue. O drone retornou para a base com a encomenda.";
  }

  return "-";
}

function localizedStatusReason(reason?: string | null) {
  if (!reason) {
    return null;
  }

  const normalizedReason = reason.trim();
  const translations: Record<string, string> = {
    "order exceeds max drone weight capacity": "Pedido excede a capacidade máxima de peso dos drones disponíveis.",
    "order exceeds max drone range": "Pedido excede o alcance máximo dos drones disponíveis.",
    "order exceeds max drone weight capacity and max drone range":
      "Pedido excede a capacidade máxima de peso e o alcance máximo dos drones disponíveis.",
    "order exceeds drone battery for complete trip and safe return":
      "Pedido exige mais bateria do que a frota disponível possui para concluir a rota e retornar em segurança.",
    "order requires another drone but no immediate drone is available":
      "Pedido exige outro drone imediato, mas não há drone disponível nesta rodada de planejamento.",
    "order cannot be served by any drone": "Pedido não pode ser atendido por nenhum drone no planejamento atual."
  };

  return translations[normalizedReason] ?? normalizedReason;
}

function buildTableData(snapshot: DashboardSnapshot, activeTable: TableView, searchTerm: string, statusFilter: string) {
  const normalizedSearch = normalizeSearch(searchTerm);

  if (activeTable === "drones") {
    return snapshot.drones
      .filter((drone) => statusFilter === "ALL" || drone.status === statusFilter)
      .filter((drone) =>
        includesSearch([drone.id, drone.identifier, drone.status, drone.batteryLevel, drone.rechargeReason ?? ""], normalizedSearch)
      )
      .sort((left, right) => left.id - right.id);
  }

  if (activeTable === "orders") {
    return snapshot.orders
      .filter((order) => statusFilter === "ALL" || order.status === statusFilter)
      .filter((order) =>
        includesSearch(
          [
            order.id,
            order.identifier,
            order.status,
            order.priority,
            order.weight,
            order.location.x,
            order.location.y,
            order.confirmedDeliveryTime,
            order.statusReason ?? ""
          ],
          normalizedSearch
        )
      )
      .sort((left, right) => left.id - right.id);
  }

  return snapshot.trips
    .filter((trip) => statusFilter === "ALL" || trip.status === statusFilter)
    .filter((trip) =>
      includesSearch([trip.id, trip.droneId, trip.status, trip.orders.join(","), trip.route.join(",")], normalizedSearch)
    )
    .sort((left, right) => left.id - right.id);
}

function statusOptionsFor(activeTable: TableView) {
  if (activeTable === "drones") {
    return droneStatuses;
  }

  if (activeTable === "orders") {
    return orderStatuses;
  }

  return tripStatuses;
}

function totalFor(snapshot: DashboardSnapshot, activeTable: TableView) {
  if (activeTable === "drones") {
    return snapshot.drones.length;
  }

  if (activeTable === "orders") {
    return snapshot.orders.length;
  }

  return snapshot.trips.length;
}

function includesSearch(values: Array<string | number>, normalizedSearch: string) {
  if (!normalizedSearch) {
    return true;
  }

  return values.some((value) => normalizeSearch(String(value)).includes(normalizedSearch));
}

function normalizeSearch(value: string) {
  return value.trim().toLowerCase();
}

function sortedRouteProgress(trip: Trip) {
  return [...trip.routeProgress].sort((left, right) => left.routePosition - right.routePosition);
}

function sortedTelemetry(telemetryHistory: TripTelemetry[]) {
  return [...telemetryHistory].sort((left, right) => Date.parse(right.reportedAt) - Date.parse(left.reportedAt) || right.id - left.id);
}

function buildClientOrderList(orders: Order[], trips: Trip[], trackingTerm: string): ClientOrderListItem[] {
  const selectedIdentifier = normalizeSearch(trackingTerm);

  return [...orders].sort((left, right) => right.id - left.id).map((order) => {
    const trip = findTripForOrder(trips, order.id);
    const routeProgress = trip ? routeProgressForOrder(trip, order.id) : null;

    return {
      identifier: order.identifier,
      order,
      trip,
      routeProgress,
      selected: normalizeSearch(order.identifier) === selectedIdentifier || String(order.id) === selectedIdentifier
    };
  });
}

function findClientOrder(orders: Order[], trackingTerm: string) {
  const normalizedTerm = normalizeSearch(trackingTerm);

  if (!normalizedTerm) {
    return null;
  }

  return (
    orders.find((order) => String(order.id) === normalizedTerm || normalizeSearch(order.identifier) === normalizedTerm) ?? null
  );
}

function findTripForOrder(trips: Trip[], orderId: number) {
  return (
    [...trips]
      .filter((trip) => trip.orders.includes(orderId) || trip.route.includes(orderId))
      .sort((left, right) => tripTrackingRank(right.status) - tripTrackingRank(left.status) || right.id - left.id)[0] ?? null
  );
}

function routeProgressForOrder(trip: Trip, orderId: number) {
  return trip.routeProgress.find((progress) => progress.orderId === orderId) ?? null;
}

function loadClientAuthToken() {
  if (typeof window === "undefined") {
    return null;
  }

  return window.localStorage.getItem(clientAuthTokenStorageKey);
}

function saveClientAuthToken(authToken: string | null) {
  if (typeof window === "undefined") {
    return;
  }

  if (authToken) {
    window.localStorage.setItem(clientAuthTokenStorageKey, authToken);
    return;
  }

  window.localStorage.removeItem(clientAuthTokenStorageKey);
}

function isDeliveryConfirmationAvailable(
  order: Order | null,
  trip: Trip | null,
  routeProgress: Trip["routeProgress"][number] | null
) {
  if (!order || !trip || !routeProgress || routeProgress.delivered || routeProgress.deliveryFailedAt) {
    return false;
  }

  const simulation = trip.simulation;
  const confirmationDeadlineIsActive =
    routeProgress.deliveryConfirmationDeadline === null ||
    Date.now() <= Date.parse(routeProgress.deliveryConfirmationDeadline);

  return (
    order.status === "IN_ROUTE" &&
    trip.status === "IN_ROUTE" &&
    simulation?.status === "IN_ROUTE" &&
    simulation.nextOrderId === order.id &&
    simulation.nextRoutePosition === routeProgress.routePosition &&
    !simulation.moving &&
    Boolean(routeProgress.availabilityConfirmedAt) &&
    Boolean(routeProgress.deliveryConfirmationRequestedAt) &&
    confirmationDeadlineIsActive
  );
}

function nextAvailabilityNotification(snapshot: DashboardSnapshot, handledKeys: Set<string>) {
  for (const trip of snapshot.trips) {
    if (trip.status !== "IN_ROUTE") {
      continue;
    }

    const pendingRouteProgress = sortedRouteProgress(trip).find((progress) => {
      const key = availabilityNotificationKey(trip.id, progress.routePosition, progress.orderId);
      const order = snapshot.orders.find((candidate) => candidate.id === progress.orderId);
      return (
        progress.availabilityNotifiedAt !== null &&
        progress.availabilityConfirmedAt === null &&
        !progress.delivered &&
        !progress.deliveryFailedAt &&
        order?.status === "IN_ROUTE" &&
        !handledKeys.has(key)
      );
    });

    if (!pendingRouteProgress) {
      continue;
    }

    const order = snapshot.orders.find((candidate) => candidate.id === pendingRouteProgress.orderId);
    return {
      key: availabilityNotificationKey(trip.id, pendingRouteProgress.routePosition, pendingRouteProgress.orderId),
      tripId: trip.id,
      routePosition: pendingRouteProgress.routePosition,
      orderId: pendingRouteProgress.orderId,
      orderIdentifier: order?.identifier ?? `#${pendingRouteProgress.orderId}`,
      droneId: trip.droneId,
      deadline: pendingRouteProgress.availabilityResponseDeadline
    };
  }

  return null;
}

function isAvailabilityNotificationPending(snapshot: DashboardSnapshot, notification: ArrivalNotificationState) {
  const trip = snapshot.trips.find((candidate) => candidate.id === notification.tripId);
  const routeProgress = trip?.routeProgress.find((progress) => progress.routePosition === notification.routePosition);
  const order = snapshot.orders.find((candidate) => candidate.id === notification.orderId);

  return (
    trip?.status === "IN_ROUTE" &&
    order?.status === "IN_ROUTE" &&
    routeProgress !== undefined &&
    !routeProgress.delivered &&
    !routeProgress.deliveryFailedAt &&
    routeProgress.availabilityNotifiedAt !== null &&
    routeProgress.availabilityConfirmedAt === null
  );
}

function availabilityNotificationKey(tripId: number, routePosition: number, orderId: number) {
  return `${tripId}:${routePosition}:${orderId}`;
}

function approachingDeliveryNotificationFor(
  results: PromiseSettledResult<TripSimulation>[],
  snapshot: DashboardSnapshot,
  notificationKeys: Set<string>
) {
  for (const result of results) {
    if (result.status !== "fulfilled") {
      continue;
    }

    const notification = approachingDeliveryNotificationFromSimulation(result.value, snapshot, notificationKeys);
    if (notification) {
      return notification;
    }
  }

  return null;
}

function approachingDeliveryNotificationFromSimulation(
  simulation: TripSimulation,
  snapshot: DashboardSnapshot,
  notificationKeys: Set<string>
) {
  if (
    simulation.status !== "IN_ROUTE" ||
    !simulation.moving ||
    simulation.nextOrderId === null ||
    simulation.nextRoutePosition === null
  ) {
    return null;
  }

  const trip = snapshot.trips.find((candidate) => candidate.id === simulation.tripId);
  const routeProgress = trip?.routeProgress.find((progress) => progress.routePosition === simulation.nextRoutePosition);
  if (!trip || !routeProgress || routeProgress.delivered || routeProgress.deliveryFailedAt) {
    return null;
  }

  const speed = droneSpeedForSimulation(snapshot, trip, simulation);
  if (speed <= 0) {
    return null;
  }

  const minutesUntilDelivery = routeProgress.estimatedDeliveryTime - minutesForDistance(simulation.travelledDistance, speed);
  if (minutesUntilDelivery <= 0 || minutesUntilDelivery > approachNotificationWindowMinutes) {
    return null;
  }

  const notificationKey = `${simulation.tripId}:${routeProgress.routePosition}:${routeProgress.orderId}`;
  if (notificationKeys.has(notificationKey)) {
    return null;
  }

  notificationKeys.add(notificationKey);
  const order = snapshot.orders.find((candidate) => candidate.id === routeProgress.orderId);
  const orderLabel = order ? order.identifier : `#${routeProgress.orderId}`;

  return `Drone #${simulation.droneId} está chegando ao destino do pedido ${orderLabel}. O cliente deve confirmar disponibilidade para receber o pacote.`;
}

function droneSpeedForSimulation(snapshot: DashboardSnapshot, trip: Trip, simulation: TripSimulation) {
  const drone = snapshot.drones.find((candidate) => candidate.id === simulation.droneId);
  if (drone) {
    return drone.speed;
  }

  return trip.estimatedDuration > 0 ? (trip.totalDistance / trip.estimatedDuration) * 60 : 0;
}

function tripTrackingRank(status: TripStatus) {
  const ranks: Record<TripStatus, number> = {
    IN_ROUTE: 5,
    PLANNED: 4,
    RETURNED_EARLY: 3,
    COMPLETED: 2,
    CANCELLED: 1
  };

  return ranks[status];
}

function clientOrderSteps(status: OrderStatus) {
  if (status === "CANCELLED") {
    return [
      { label: "Solicitado", state: "done" },
      { label: "Cancelado", state: "blocked" },
      { label: "Em rota", state: "pending" },
      { label: "Entregue", state: "pending" }
    ];
  }

  if (status === "UNALLOCATED") {
    return [
      { label: "Solicitado", state: "done" },
      { label: "Não alocado", state: "blocked" },
      { label: "Em rota", state: "pending" },
      { label: "Entregue", state: "pending" }
    ];
  }

  if (status === "NOT_DELIVERED") {
    return [
      { label: "Solicitado", state: "done" },
      { label: "Planejado", state: "done" },
      { label: "Retornou", state: "blocked" },
      { label: "Não entregue", state: "blocked" }
    ];
  }

  const allocated = ["ALLOCATED", "IN_ROUTE", "DELIVERED"].includes(status);
  const inRoute = ["IN_ROUTE", "DELIVERED"].includes(status);
  const delivered = status === "DELIVERED";

  return [
    { label: "Solicitado", state: "done" },
    { label: status === "PENDING_REASSIGNMENT" ? "Reatribuição" : "Planejado", state: allocated ? "done" : "current" },
    { label: "Em rota", state: inRoute ? "done" : "pending" },
    { label: "Entregue", state: delivered ? "done" : "pending" }
  ];
}

function buildMapRouteLayer(trip: Trip, orders: Order[], viewport: MapViewport, selectedTripId: number): MapRouteLayer {
  const routePoints = buildMapRoutePoints(trip, orders);

  return {
    trip,
    color: tripRouteColorFor(trip),
    selected: trip.id === selectedTripId,
    routeSegments: buildRouteSegments(routePoints, viewport)
  };
}

function buildMapOrderHighlights(routeLayers: MapRouteLayer[]) {
  return routeLayers.reduce((highlights, layer) => {
    sortedRouteProgress(layer.trip).forEach((progress) => {
      const current = highlights.get(progress.orderId);
      if (!current || layer.selected) {
        highlights.set(progress.orderId, {
          color: layer.color,
          tripId: layer.trip.id,
          routePosition: progress.routePosition,
          selected: layer.selected
        });
      }
    });

    return highlights;
  }, new Map<number, MapOrderHighlight>());
}

function tripRouteColorFor(trip: Trip) {
  return tripRouteColors[Math.abs(trip.id - 1) % tripRouteColors.length];
}

function buildMapRoutePoints(trip: Trip, orders: Order[]) {
  const routeProgress = sortedRouteProgress(trip);
  const routeOrders = routeProgress
    .map((progress) => ({
      progress,
      order: orders.find((order) => order.id === progress.orderId)
    }))
    .filter((entry): entry is { progress: Trip["routeProgress"][number]; order: Order } => entry.order !== undefined);

  return [
    { key: `trip-${trip.id}-base-start`, x: 0, y: 0, label: "Base" },
    ...routeOrders.map(({ progress, order }) => ({
      key: `trip-${trip.id}-order-${order.id}`,
      x: order.location.x,
      y: order.location.y,
      label: `${order.identifier} (${progress.routePosition + 1})`,
      orderId: order.id,
      routePosition: progress.routePosition
    })),
    { key: `trip-${trip.id}-base-end`, x: 0, y: 0, label: "Base" }
  ] satisfies MapRoutePoint[];
}

function buildMapViewport(trips: Trip[], orders: Order[], obstacles: Obstacle[]): MapViewport {
  const routePoints = trips.flatMap((trip) => buildMapRoutePoints(trip, orders));
  const simulationPoints = trips.flatMap((trip) => (trip.simulation ? [trip.simulation.currentLocation] : []));
  const focusPoints: MapPoint[] = [...routePoints, ...simulationPoints];
  const basePoints = focusPoints.length ? focusPoints : [{ x: 0, y: 0 }, ...orders.map((order) => order.location)];
  const routeBounds = boundsForMapPoints(basePoints);
  const routeSpan = Math.max(routeBounds.maxX - routeBounds.minX, routeBounds.maxY - routeBounds.minY, mapViewportMinimumSizeKilometers);
  const obstacleContext = expandMapBounds(routeBounds, routeSpan * mapObstacleContextRatio);
  const contextualObstaclePoints = obstacles
    .filter((obstacle) => obstacleIntersectsBounds(obstacle, obstacleContext))
    .flatMap(obstacleBoundsPoints);
  const bounds = boundsForMapPoints([...basePoints, ...contextualObstaclePoints]);
  const spanX = bounds.maxX - bounds.minX;
  const spanY = bounds.maxY - bounds.minY;
  const contentSize = Math.max(spanX, spanY, mapViewportMinimumSizeKilometers);
  const size = contentSize * (1 + mapViewportPaddingRatio * 2);
  const centerX = (bounds.minX + bounds.maxX) / 2;
  const centerY = (bounds.minY + bounds.maxY) / 2;

  return {
    minX: centerX - size / 2,
    minY: centerY - size / 2,
    size
  };
}

function boundsForMapPoints(points: MapPoint[]): MapBounds {
  return points.reduce(
    (bounds, point) => ({
      minX: Math.min(bounds.minX, point.x),
      maxX: Math.max(bounds.maxX, point.x),
      minY: Math.min(bounds.minY, point.y),
      maxY: Math.max(bounds.maxY, point.y)
    }),
    {
      minX: points[0]?.x ?? 0,
      maxX: points[0]?.x ?? 0,
      minY: points[0]?.y ?? 0,
      maxY: points[0]?.y ?? 0
    }
  );
}

function expandMapBounds(bounds: MapBounds, padding: number): MapBounds {
  return {
    minX: bounds.minX - padding,
    maxX: bounds.maxX + padding,
    minY: bounds.minY - padding,
    maxY: bounds.maxY + padding
  };
}

function obstacleBoundsPoints(obstacle: Obstacle): MapPoint[] {
  return [
    { x: obstacle.center.x - obstacle.radius, y: obstacle.center.y - obstacle.radius },
    { x: obstacle.center.x + obstacle.radius, y: obstacle.center.y + obstacle.radius }
  ];
}

function obstacleIntersectsBounds(obstacle: Obstacle, bounds: MapBounds) {
  return (
    obstacle.center.x + obstacle.radius >= bounds.minX &&
    obstacle.center.x - obstacle.radius <= bounds.maxX &&
    obstacle.center.y + obstacle.radius >= bounds.minY &&
    obstacle.center.y - obstacle.radius <= bounds.maxY
  );
}

function obstacleIntersectsViewport(obstacle: Obstacle, viewport: MapViewport) {
  return obstacleIntersectsBounds(obstacle, mapViewportBounds(viewport));
}

function mapViewportBounds(viewport: MapViewport): MapBounds {
  return {
    minX: viewport.minX,
    maxX: viewport.minX + viewport.size,
    minY: viewport.minY,
    maxY: viewport.minY + viewport.size
  };
}

function mapPointIsInsideViewport(point: MapPoint, viewport: MapViewport) {
  const bounds = mapViewportBounds(viewport);

  return point.x >= bounds.minX && point.x <= bounds.maxX && point.y >= bounds.minY && point.y <= bounds.maxY;
}

function projectPoint(point: MapPoint, viewport: MapViewport) {
  return {
    left: ((point.x - viewport.minX) / viewport.size) * 100,
    top: 100 - ((point.y - viewport.minY) / viewport.size) * 100
  };
}

function mapPointStyle(point: { left: number; top: number }) {
  return {
    left: `${point.left}%`,
    top: `${point.top}%`
  };
}

function mapColorStyle(color: string) {
  return {
    "--route-color": color
  } as React.CSSProperties;
}

function routeSegmentStyle(segment: MapRouteSegment, color: string) {
  return {
    left: `${segment.left}%`,
    top: `${segment.top}%`,
    width: `${segment.length}%`,
    transform: `translateY(-50%) rotate(${segment.angle}deg)`,
    "--route-color": color
  } as React.CSSProperties;
}

function mapOrderMarkerStyle(point: { left: number; top: number }, color?: string) {
  return {
    ...mapPointStyle(point),
    ...(color ? { "--route-color": color } : {})
  } as React.CSSProperties;
}

function mapDroneMarkerStyle(point: { left: number; top: number }, color: string) {
  return {
    ...mapPointStyle(point),
    "--route-color": color
  } as React.CSSProperties;
}

function mapOrderTitle(order: Order, highlight?: MapOrderHighlight) {
  const base = `${order.identifier} (${formatLocation(order.location.x, order.location.y)}), ${orderStatusLabel(order.status)}`;

  if (!highlight) {
    return `${base}. Pedido fora das rotas exibidas.`;
  }

  return `${base}. Viagem #${highlight.tripId}, posição ${highlight.routePosition + 1} da rota.`;
}

function buildRouteSegments(points: MapRoutePoint[], viewport: MapViewport) {
  return points.slice(0, -1).flatMap((point, index) => {
    const nextPoint = points[index + 1];
    const start = projectPoint(point, viewport);
    const end = projectPoint(nextPoint, viewport);
    const deltaX = end.left - start.left;
    const deltaY = end.top - start.top;
    const length = Math.hypot(deltaX, deltaY);

    if (!length) {
      return [];
    }

    return [
      {
        key: `${point.key}-${nextPoint.key}-${index}`,
        left: start.left,
        top: start.top,
        length,
        angle: (Math.atan2(deltaY, deltaX) * 180) / Math.PI,
        fromLabel: point.label,
        toLabel: nextPoint.label
      }
    ];
  });
}

function toDronePayload(form: DroneFormState): CreateDronePayload {
  return {
    identifier: form.identifier.trim(),
    maxWeightCapacity: requiredNumber(form.maxWeightCapacity),
    maxRange: requiredNumber(form.maxRange),
    batteryLevel: optionalNumber(form.batteryLevel),
    batteryConsumptionPerDistanceUnit: optionalNumber(form.batteryConsumptionPerDistanceUnit),
    minimumReturnBattery: optionalNumber(form.minimumReturnBattery),
    speed: optionalNumber(form.speed),
    chargingRate: optionalNumber(form.chargingRate)
  };
}

function toOrderPayload(form: OrderFormState): CreateOrderPayload {
  return {
    identifier: form.identifier.trim(),
    location: {
      x: requiredNumber(form.x),
      y: requiredNumber(form.y)
    },
    weight: requiredNumber(form.weight),
    priority: form.priority,
    confirmedDeliveryTime: requiredDateTimeIso(form.confirmedDeliveryTime)
  };
}

function toClientOrderPayload(form: ClientOrderFormState, trackingCode: string): CreateOrderPayload {
  return {
    identifier: trackingCode,
    location: {
      x: requiredNumber(form.x),
      y: requiredNumber(form.y)
    },
    weight: requiredNumber(form.weight),
    priority: "MEDIUM",
    confirmedDeliveryTime: requiredDateTimeIso(form.confirmedDeliveryTime)
  };
}

function toClientAuthPayload(form: ClientAuthFormState): ClientAuthPayload {
  return {
    email: form.email.trim(),
    password: form.password
  };
}

function toClientRegisterPayload(form: ClientAuthFormState): ClientRegisterPayload {
  return {
    name: form.name.trim(),
    ...toClientAuthPayload(form)
  };
}

function generateTrackingCode(existingOrders: Order[]) {
  const existingCodes = new Set(existingOrders.map((order) => normalizeSearch(order.identifier)));

  for (let attempt = 0; attempt < 24; attempt++) {
    const code = randomTrackingCode();
    if (!existingCodes.has(normalizeSearch(code))) {
      return code;
    }
  }

  return Date.now().toString(36).slice(-6).toUpperCase();
}

function randomTrackingCode() {
  const alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  const values = new Uint32Array(6);

  if (globalThis.crypto?.getRandomValues) {
    globalThis.crypto.getRandomValues(values);
  } else {
    values.forEach((_, index) => {
      values[index] = Math.floor(Math.random() * alphabet.length);
    });
  }

  return Array.from(values, (value) => alphabet[value % alphabet.length]).join("");
}

function toObstaclePayload(form: ObstacleFormState): CreateObstaclePayload {
  return {
    center: {
      x: requiredNumber(form.x),
      y: requiredNumber(form.y)
    },
    radius: requiredNumber(form.radius)
  };
}

function toReviewPayload(form: ReviewFormState): CreateReviewPayload {
  return {
    stars: form.stars,
    title: form.title.trim(),
    feedback: form.feedback.trim()
  };
}

function requiredNumber(value: string) {
  return Number(value);
}

function requiredDateTimeIso(value: string) {
  if (!value.trim()) {
    throw new Error("Horário confirmado deve ser informado");
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    throw new Error("Horário confirmado é inválido");
  }

  return date.toISOString();
}

function optionalNumber(value: string) {
  return value.trim() ? Number(value) : undefined;
}

function deliveredCount(trip: Trip) {
  return trip.routeProgress.filter((progress) => progress.delivered).length;
}

function nextUndeliveredRoutePosition(trip: Trip) {
  const nextProgress = [...trip.routeProgress]
    .filter((progress) => !progress.delivered && !progress.deliveryFailedAt)
    .sort((left, right) => left.routePosition - right.routePosition)[0];

  return nextProgress?.routePosition ?? null;
}

function isValidBatteryInput(value: string) {
  if (!value.trim()) {
    return false;
  }

  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed >= 0 && parsed <= 100;
}

function isBusy<TAction extends string>(actionInFlight: { id: number; action: TAction } | null, id: number, action: TAction) {
  return actionInFlight?.id === id && actionInFlight.action === action;
}

function droneSuccessMessageFor(drone: Drone, action: DroneAction) {
  const actionMessages: Record<DroneAction, string> = {
    markUnavailable: `${drone.identifier} marcado como indisponível.`,
    markAvailable: `${drone.identifier} marcado como disponível.`,
    enqueueRecharge: `${drone.identifier} enviado para a fila de recarga.`,
    completeRecharge: `${drone.identifier} concluiu a recarga.`,
    delete: `${drone.identifier} excluído.`
  };

  return actionMessages[action];
}

function tripSuccessMessageFor(trip: Trip, action: TripAction, options?: TripActionOptions) {
  const tripLabel = `Viagem #${trip.id}`;

  if (action === "complete" && trip.status === "RETURNED_EARLY") {
    return `${tripLabel} retornou antecipadamente.`;
  }

  const actionMessages: Record<TripAction, string> = {
    start: `${tripLabel} iniciada.`,
    deliverNext: `${tripLabel}: entrega da posição ${options?.routePosition ?? "-"} registrada.`,
    sendTelemetry:
      trip.status === "RETURNED_EARLY"
        ? `${tripLabel}: telemetria registrada e retorno antecipado acionado.`
        : `${tripLabel}: telemetria registrada.`,
    complete: `${tripLabel} concluída.`,
    cancel: `${tripLabel} cancelada.`
  };

  return actionMessages[action];
}

function tripPlanSuccessMessageFor(plan: TripPlan) {
  const tripCount = plan.trips.length;
  const unallocatedCount = plan.unallocatedOrders.length;
  const averageDeliveryTime = average(plan.trips.map((trip) => trip.averageDeliveryTime).filter(Boolean));

  return `${tripCount} viagens planejadas, ${unallocatedCount} pedidos não alocados, tempo médio ${formatDuration(averageDeliveryTime)}.`;
}

function demoScenarioSuccessMessageFor(result: DemoScenarioResult) {
  return `Demo recriada: ${result.drones} drones, ${result.orders} pedidos, ${result.obstacles} obstáculo, ${result.reviews} avaliação e ${result.trips} viagens planejadas.`;
}

function countBy<T extends Record<K, string>, K extends keyof T>(items: T[], key: K) {
  return items.reduce(
    (counts, item) => {
      const value = item[key] as T[K] & string;
      counts[value] = (counts[value] ?? 0) + 1;
      return counts;
    },
    {} as Record<T[K] & string, number>
  );
}

function average(values: number[]) {
  if (!values.length) {
    return 0;
  }

  return values.reduce((sum, value) => sum + value, 0) / values.length;
}

function formatDuration(value: number) {
  if (!Number.isFinite(value) || value <= 0) {
    return "-";
  }

  if (value >= 60) {
    const totalMinutes = Math.round(value);
    const hours = Math.floor(totalMinutes / 60);
    const minutes = totalMinutes % 60;

    return `${hours.toString().padStart(2, "0")}:${minutes
      .toString()
      .padStart(2, "0")} h`;
  }

  return `${value.toFixed(1)} min`;
}

function formatNumber(value: number) {
  return value.toLocaleString("pt-BR", {
    maximumFractionDigits: 1
  });
}

function formatWeight(value: number) {
  return `${formatNumber(value)} ${measurementUnits.weight}`;
}

function formatDistance(value: number) {
  const absoluteValue = Math.abs(value);
  const maximumFractionDigits = absoluteValue > 0 && absoluteValue < 0.1 ? 3 : absoluteValue < 1 ? 2 : 1;

  return `${value.toLocaleString("pt-BR", { maximumFractionDigits })} ${measurementUnits.distance}`;
}

function formatSpeed(value: number) {
  return `${formatNumber(value)} ${measurementUnits.speed}`;
}

function formatLocation(x: number, y: number) {
  return `${formatDistance(x)}, ${formatDistance(y)}`;
}

function minutesForDistance(distanceKilometers: number, speedKilometersPerHour: number) {
  return speedKilometersPerHour > 0 ? (distanceKilometers / speedKilometersPerHour) * 60 : 0;
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  });
}

function formatNullableDateTime(value: string | null) {
  return value ? formatDateTime(value) : "-";
}

function currentReportMonth() {
  const now = new Date();
  return formatYearMonth(now.getFullYear(), now.getMonth() + 1);
}

function shiftReportMonth(month: string, offset: number) {
  const [year, monthNumber] = month.split("-").map(Number);
  if (!year || !monthNumber) {
    return currentReportMonth();
  }

  const shifted = new Date(year, monthNumber - 1 + offset, 1);
  const shiftedMonth = formatYearMonth(shifted.getFullYear(), shifted.getMonth() + 1);
  const currentMonth = currentReportMonth();

  return shiftedMonth > currentMonth ? currentMonth : shiftedMonth;
}

function formatYearMonth(year: number, monthNumber: number) {
  return `${year}-${String(monthNumber).padStart(2, "0")}`;
}

function formatReportMonth(month: string) {
  const [year, monthNumber] = month.split("-").map(Number);
  if (!year || !monthNumber) {
    return month;
  }

  return new Date(year, monthNumber - 1, 1).toLocaleDateString("pt-BR", {
    month: "long",
    year: "numeric"
  });
}

function formatTime(value: Date) {
  return value.toLocaleTimeString("pt-BR", {
    hour: "2-digit",
    minute: "2-digit"
  });
}

function playNotificationSound() {
  try {
    const webkitWindow = window as Window & { webkitAudioContext?: typeof AudioContext };
    const AudioContextConstructor = window.AudioContext ?? webkitWindow.webkitAudioContext;
    if (!AudioContextConstructor) {
      return;
    }

    const context = new AudioContextConstructor();
    const oscillator = context.createOscillator();
    const gain = context.createGain();
    const start = context.currentTime;

    oscillator.type = "sine";
    oscillator.frequency.setValueAtTime(880, start);
    oscillator.frequency.setValueAtTime(660, start + 0.16);
    gain.gain.setValueAtTime(0.001, start);
    gain.gain.exponentialRampToValueAtTime(0.24, start + 0.03);
    gain.gain.exponentialRampToValueAtTime(0.001, start + 0.45);
    oscillator.connect(gain);
    gain.connect(context.destination);
    oscillator.start(start);
    oscillator.stop(start + 0.45);
    oscillator.addEventListener("ended", () => {
      void context.close();
    });

    if (context.state === "suspended") {
      void context.resume().catch(() => undefined);
    }
  } catch {
    // Audio notification is best-effort; the modal remains the reliable alert.
  }
}

export default App;
