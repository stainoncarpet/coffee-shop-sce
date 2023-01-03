package Data.Models;

import Data.Enums.ReservationStatus;

/**
 * ReservationOrder class is a data container for data related to reservations
 */
public class ReservationOrder {
    private String id;
    private String creationDate;
    private String reservationDate;

    public String getCreationDate() {
        return creationDate;
    }

    public String getReservationDate() {
        return reservationDate;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    private ReservationStatus status;
    private String orderTotal;

    public ReservationOrder(String id, String creationDate, String reservationDate, ReservationStatus status, String orderTotal) {
        this.id = id;
        this.creationDate = creationDate;
        this.reservationDate = reservationDate;
        this.status = status;
        this.orderTotal = orderTotal;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderTotal() {
        return orderTotal;
    }

    public void setOrderTotal(String orderTotal) {
        this.orderTotal = orderTotal;
    }
}
