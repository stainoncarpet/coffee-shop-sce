package Data.Models;

import Data.Enums.TableLocation;

/**
 * ReservationItem class is a data container for data related to reservations
 */
public class ReservationItem {
    private int id;
    private int seats;
    private double rate;
    private String[] availableHours;

    public TableLocation getLocation() {
        return location;
    }

    private TableLocation location;

    public int getId() {
        return id;
    }

    public int getSeats() {
        return seats;
    }

    public String[] getAvailableHours() {
        return availableHours;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getRate() {
        return rate;
    }

    public ReservationItem(int id, int seats, double rate, String[] availableHours, TableLocation location) {
        this.id = id;
        this.seats = seats;
        this.rate = rate;
        this.availableHours = availableHours;
        this.location = location;
    }

    @Override
    public String toString() {
        return "ReservationItem{" +
                "id=" + id +
                ", seats=" + seats +
                ", rate=" + rate +
                '}';
    }
}
