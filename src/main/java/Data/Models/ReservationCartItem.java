package Data.Models;

/**
 * ReservationCartItem class is a data container for data related to table reservations
 */
public class ReservationCartItem {
    private String  id;
    private int hoursTotal;
    private String hoursListString;
    private double rate;

    public String getId() {
        return id;
    }

    public int getHoursTotal() {
        return hoursTotal;
    }

    public String getHoursListString() {
        return hoursListString;
    }

    public double getRate() {
        return rate;
    }

    public double getTotalSpentAmount() {
        return totalSpentAmount;
    }

    public void setId(String id) {
        this.id = id;
    }

    private double totalSpentAmount;

    public ReservationCartItem(String id, int hoursTotal, String hoursListString, double rate, double totalSpentAmount) {
        this.id = id;
        this.hoursTotal = hoursTotal;
        this.hoursListString = hoursListString;
        this.rate = rate;
        this.totalSpentAmount = totalSpentAmount;
    }
}
