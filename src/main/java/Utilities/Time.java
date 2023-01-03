package Utilities;

import Data.Services.ReservationService;
import Data.Models.ProbablyUnavailableDate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Time {
    private static final long MILLIS_IN_A_DAY = 1000 * 60 * 60 * 24;
    public static ArrayList<ProbablyUnavailableDate> getDatesAsStrings(int days, int tableId, ReservationService reservationService){
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        var today = new Date();
        var tomorrow = today.getTime() + MILLIS_IN_A_DAY;
        var end = today.getTime() + MILLIS_IN_A_DAY * days;
        var daysStrings = new ArrayList<ProbablyUnavailableDate>();
        var userTouchedDates = reservationService.getClientTouchedDates(tableId, format.format(tomorrow), format.format(end));
        var madeFullyUnavailableDates = reservationService.getUnavailableDatesByTableId(tableId, format.format(tomorrow), format.format(end));

        for (int i = 0; i < days; i++) {
            if(!userTouchedDates.contains(format.format(tomorrow))) {
                var isBanned = madeFullyUnavailableDates.contains(format.format(tomorrow));
                daysStrings.add(
                        new ProbablyUnavailableDate(isBanned ? "true" : "", format.format(tomorrow))
                );
            }

            tomorrow += MILLIS_IN_A_DAY;
        }

        return daysStrings;
    }

    public static ArrayList<ProbablyUnavailableDate> getDatesAsStrings(int days){
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        var today = new Date();
        var tomorrow = today.getTime() + MILLIS_IN_A_DAY;
        var daysStrings = new ArrayList<ProbablyUnavailableDate>();

        for (int i = 0; i < days; i++) {
            daysStrings.add(
                    new ProbablyUnavailableDate("", format.format(tomorrow))
            );

            tomorrow += MILLIS_IN_A_DAY;
        }

        return daysStrings;
    }
}
