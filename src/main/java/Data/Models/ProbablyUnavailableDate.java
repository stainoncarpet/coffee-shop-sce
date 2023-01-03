package Data.Models;

/**
 * ProbablyUnavailableDate class is used in construction of 'unavailable dates' dropdown
 */
public class ProbablyUnavailableDate {
    public String getDate() {
        return date;
    }

    public String getIsUnavailable() {
        return isUnavailable;
    }

    public ProbablyUnavailableDate(String isUnavailable, String date) {
        this.isUnavailable = isUnavailable;
        this.date = date;
    }

    private String isUnavailable;
    private String date;
}
