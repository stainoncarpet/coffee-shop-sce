package Data.Models;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
/**
 * ReservationData class is a data container for data related to table reservations
 */
public class ReservationData {
    private Date reservationDate;
    private HashMap<String, ArrayList<String>> tableHoursMap;

    public Date getReservationDate() {
        return reservationDate;
    }

    /**
     * This method formats date as string
     * @return String formatted date
     * */
    public String getReservationDateString() {
        String str = new SimpleDateFormat("yyyy-MM-dd").format(reservationDate);
        return str;
    }

    /**
     * This method truncates year if reservation date year is current year
     * @return String formatted date
     * */
    public String getReservationDateStringMonth() {
        if(new Date().getYear() == this.reservationDate.getYear()) {
            return new SimpleDateFormat("dd MMMM").format(reservationDate);
        } else {
            return new SimpleDateFormat("dd MMMM yyyy").format(reservationDate);
        }
    }

    public ReservationData(Date reservationDate, HashMap<String, ArrayList<String>> tableHoursMap) {
        this.reservationDate = reservationDate;
        this.tableHoursMap = tableHoursMap;
    }

    public HashMap<String, ArrayList<String>> getTableHoursMap() {
        return tableHoursMap;
    }
}
