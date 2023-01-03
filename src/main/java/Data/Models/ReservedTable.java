package Data.Models;

import Data.Enums.TableLocation;

/**
 * ReservedTable class is a data container for data related to reservations (after one is made)
 */
public class ReservedTable {
    private int tableId;
    private int seatsCount;
    private double hourlyRate;

    public int getTableId() {
        return tableId;
    }

    public int getSeatsCount() {
        return seatsCount;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public String getReservedHours() {
        return reservedHours;
    }

    public TableLocation getTableLocation() {
        return tableLocation;
    }

    private String reservedHours;

    public ReservedTable(int tableId, int seatsCount, double hourlyRate, String reservedHours, TableLocation tableLocation) {
        this.tableId = tableId;
        this.seatsCount = seatsCount;
        this.hourlyRate = hourlyRate;
        this.reservedHours = reservedHours;
        this.tableLocation = tableLocation;
    }

    private TableLocation tableLocation;
}
