package com.example.drone.service;

import com.example.drone.persistence.*;

import java.time.Instant;

public final class DeliveryAvailabilityPolicy {

    static final double NOTIFICATION_WINDOW_MINUTES = 2.0;
    static final long RESPONSE_TIMEOUT_SECONDS = 30L;
    static final long DELIVERY_CONFIRMATION_TIMEOUT_SECONDS = 60L;
    static final String UNCONFIRMED_AVAILABILITY_REASON =
            "Cliente não confirmou disponibilidade para receber o pacote.";
    static final String DECLINED_AVAILABILITY_REASON =
            "Cliente informou que não está disponível para receber o pacote.";
    static final String UNCONFIRMED_DELIVERY_CODE_REASON =
            "Cliente não informou o código de confirmação no prazo. Drone seguiu a rota e retornará o pacote à base.";
    static final String ROUTE_INTERRUPTED_REASON =
            "Viagem interrompida porque outro pacote não teve disponibilidade confirmada pelo cliente.";

    private DeliveryAvailabilityPolicy() {
    }

    public static Instant responseDeadlineFor(TripOrderEntity tripOrder) {
        Instant notifiedAt = tripOrder.getAvailabilityNotifiedAt();
        return notifiedAt == null ? null : notifiedAt.plusSeconds(RESPONSE_TIMEOUT_SECONDS);
    }

    static boolean hasResponseExpired(TripOrderEntity tripOrder, Instant now) {
        Instant deadline = responseDeadlineFor(tripOrder);
        return deadline != null
                && !tripOrder.isAvailabilityConfirmed()
                && !tripOrder.isDelivered()
                && !tripOrder.isDeliveryFailed()
                && !now.isBefore(deadline);
    }

    public static Instant deliveryConfirmationDeadlineFor(TripOrderEntity tripOrder) {
        Instant requestedAt = tripOrder.getDeliveryConfirmationRequestedAt();
        return requestedAt == null ? null : requestedAt.plusSeconds(DELIVERY_CONFIRMATION_TIMEOUT_SECONDS);
    }

    static boolean hasDeliveryConfirmationExpired(TripOrderEntity tripOrder, Instant now) {
        Instant deadline = deliveryConfirmationDeadlineFor(tripOrder);
        return deadline != null
                && tripOrder.isAvailabilityConfirmed()
                && !tripOrder.isDelivered()
                && !tripOrder.isDeliveryFailed()
                && !now.isBefore(deadline);
    }
}
