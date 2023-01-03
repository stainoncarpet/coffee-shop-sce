package Data.Models;

import java.util.Date;

/**
 * ReservedOrder class is a data container for data related to reservations (after one is made)
 */
public class ReservedOrder {
    private int reservationId;
    private int clientId;
    private int baristaId;

    public int getReservationId() {
        return reservationId;
    }

    public int getClientId() {
        return clientId;
    }

    public int getBaristaId() {
        return baristaId;
    }

    public String getIsClientVip() {
        return isClientVip;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public ReservedTable[] getReservedTables() {
        return reservedTables;
    }

    public Date getReservationDate() {
        return reservationDate;
    }

    public double getReservationTotal() {
        return reservationTotal;
    }

    public ReservedOrder(int reservationId, int clientId, String isClientVip, Date creationDate, ReservedTable[] reservedTables, Date reservationDate, double reservationTotal, int baristaId) {
        this.reservationId = reservationId;
        this.clientId = clientId;
        this.isClientVip = isClientVip;
        this.creationDate = creationDate;
        this.reservationDate = reservationDate;
        this.reservedTables = reservedTables;
        this.reservationTotal = reservationTotal;
        this.baristaId = baristaId;
    }

    private String isClientVip = "NO";
    private Date creationDate;

    private ReservedTable[] reservedTables;
    private Date reservationDate;
    private double reservationTotal;
}
