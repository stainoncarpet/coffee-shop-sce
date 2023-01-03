package Data.Services;

import Data.Enums.ReservationStatus;
import Data.Enums.TableLocation;
import Data.Models.*;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * ReservationService class contains functionality tailored for table reservation-related db interaction
 *  */
public class ReservationService {
    // db connection object
    private java.sql.Connection connection;

    /**
     * constructor
     * @param connection db connection object
     */
    public ReservationService(Connection connection) {
        this.connection = connection;
    }

    /**
     * This method fetches available table data for specified date
     * used in displaying data to client
     * @return List of reservation item objects
     */
    public LinkedList<ReservationItem> getAvailableTablesByDate(String dateString) {

        try {
            String tablesQuery = "SELECT * FROM TABLES WHERE tables.LOCATION != 'NONE'";

            String reservedTableHoursQuery = String.format(
                    "SELECT tables.TABLE_ID, contents.START_HOUR, contents.END_HOUR, metadata.RESERVATION_DATE\n" +
                            "FROM reservations_contents as contents\n" +
                            "JOIN tables as tables ON tables.TABLE_ID=contents.TABLE_ID\n" +
                            "JOIN reservations_metadata as metadata on metadata.RESERVATION_ID=contents.RESERVATION_ID\n" +
                            "LEFT JOIN TABLES_OFFDATES offdates on offdates.TABLE_ID=tables.TABLE_ID \n" +
                            "WHERE metadata.RESERVATION_DATE='%s' AND tables.LOCATION != 'NONE'",
                            dateString
            );

            var resultSet = this.connection.prepareStatement(tablesQuery).executeQuery();

            var resultSet2 = this.connection.prepareStatement(reservedTableHoursQuery).executeQuery();
            var alreadyReservedHoursByTable = new HashMap<String, ArrayList<String>>();
            while (resultSet2.next()) {
                var tableId = resultSet2.getInt("TABLE_ID") + "";
                var startHour = resultSet2.getInt("START_HOUR") + "";

                if (alreadyReservedHoursByTable.get(tableId) == null) {
                    alreadyReservedHoursByTable.put(tableId, new ArrayList<String>());
                }
                alreadyReservedHoursByTable.get(tableId).add(startHour);
            }

            LinkedList tables = new LinkedList();

            while (resultSet.next()){
                var tableId = resultSet.getInt("TABLE_ID");

                // determine which hours are no longer available if reservation is for today
                var isForToday = LocalDate.parse(dateString).isEqual(LocalDate.now());
                var workingHoursLeft = isForToday ? (22 - (LocalTime.now().getHour() + 1)) : 10;
                var availableHours = new String[workingHoursLeft];
                var hourOffset = 10 - workingHoursLeft;

                // check if table is unavailable on that date
                var tableUnavailabeQuery = String.format("SELECT * \n" +
                                "FROM tables_offdates\n" +
                                "WHERE OFF_DATE='%s' AND TABLE_ID=%d",
                        dateString, tableId
                );
                var tableUnavailabeDataSet = this.connection.prepareStatement(tableUnavailabeQuery).executeQuery();
                var isUnavailable = tableUnavailabeDataSet.next();

                // don't show working hours if unavailable
                if(!isUnavailable) {
                    // coffeshop open between 12 and 22
                    for (int i = 0 + hourOffset; i < availableHours.length + hourOffset; i++) {
                        if(!alreadyReservedHoursByTable.containsKey(tableId + "") || !alreadyReservedHoursByTable.get(tableId + "").contains((i+12) + "")) {
                            availableHours[i - hourOffset] = (i + 12) + "—" + (i + 13);
                        }
                    }
                }

                // don't add/display tables with no available hours
                var tableAvailableHours = Arrays.stream(availableHours).filter(Objects::nonNull).toArray(String[]::new);
                if(tableAvailableHours.length > 0) {
                    tables.add(new ReservationItem(
                            tableId,
                            resultSet.getInt("SEATS_COUNT"),
                            resultSet.getInt("HOURLY_RATE"),
                            tableAvailableHours,
                            TableLocation.valueOf(resultSet.getString("LOCATION"))
                    ));
                }
            }

            return tables;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * This method adds reservation data records to the db
     * @param reservationData reservation data object
     * @param userId client who makes the reservation
     * @return boolean indicating if tables were reserved successfully
     */
    public boolean reserveTables(ReservationData reservationData, String userId) {
        try {
            String query0 = "SELECT MAX(RESERVATION_ID) FROM reservations_metadata";
            var res = this.connection.prepareStatement(query0).executeQuery();
            res.next();
            int currentMaxId = res.getInt("MAX(RESERVATION_ID)");

            var sb = new StringBuilder();
            sb.append("INSERT INTO RESERVATIONS_CONTENTS (RESERVATION_ID, TABLE_ID, START_HOUR, END_HOUR) VALUES");

            var tableIds = new String[reservationData.getTableHoursMap().size()];
            var k = 0;
            for (var r: reservationData.getTableHoursMap().entrySet()) {
                tableIds[k] = r.getKey();
                System.out.println("tableIds[k] " + tableIds[k]);
                k++;
            }

            var idsAsStingedArray = Arrays.toString(tableIds);
            var tablesMap = getTablesById(idsAsStingedArray);

            double reservationTotal = 0;

            for (var tableHours: reservationData.getTableHoursMap().entrySet()) {
                var tableId = tableHours.getKey();
                var reservedHours = tableHours.getValue();

                reservationTotal += tablesMap.get(tableId).getRate() * reservedHours.size();

                for (int i = 0; i < reservedHours.size(); i++) {
                    var hours = reservedHours.get(i).split("—");

                    sb.append(
                            String.format(
                                    " (%d, %d, %d, %d),",
                                    currentMaxId + 1, Integer.parseInt(tableId), Integer.parseInt(hours[0]), Integer.parseInt(hours[1])
                            ));
                }
            }

            String query = String.format(
                    "INSERT INTO RESERVATIONS_METADATA (RESERVATION_ID, CLIENT_ID, CREATION_DATE, RESERVATION_DATE, STATUS, RESERVATION_TOTAL) VALUES (%d, %s, CURDATE(), '%s', 'PENDING', %f)",
                    currentMaxId + 1, userId, reservationData.getReservationDateString(),reservationTotal);
            this.connection.prepareStatement(query).executeUpdate();

            sb.deleteCharAt(sb.length() - 1);
            this.connection.prepareStatement(sb.toString()).executeUpdate();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * This method fetches data for tables with specified ids
     * @param tableIds ids of tables to fetch
     * @return hashmap where key is table id and value is other table information
     */
    public HashMap<String, ReservationItem> getTablesById(String tableIds) {
        try {
            String query = String.format(
                    "SELECT * FROM TABLES WHERE TABLE_ID IN (%s) AND tables.LOCATION != 'NONE'",
                    tableIds.substring(1, tableIds.length() - 1)
            );

            var resultSet = this.connection.prepareStatement(query).executeQuery();

            var tables = new HashMap<String, ReservationItem>();

            while (resultSet.next()){
                tables.put(
                        String.valueOf(resultSet.getInt("TABLE_ID")),
                        new ReservationItem(
                            resultSet.getInt("TABLE_ID"),
                            resultSet.getInt("SEATS_COUNT"),
                            resultSet.getInt("HOURLY_RATE"),
                            new String[]{},
                                TableLocation.valueOf(resultSet.getString("LOCATION"))
                        )
                );
            }

            return tables;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * This method fetches reservations data tied to specific user
     * @param userId user id
     * @return list of reservation orders
     */
    public ArrayList<ReservationOrder> getReservationsByUserId(int userId) {
        try {
            String query = "SELECT M.RESERVATION_ID, M.CREATION_DATE, M.RESERVATION_DATE, M.STATUS, M.RESERVATION_TOTAL\n" +
                    "FROM reservations_metadata as M\n" +
                    "WHERE M.CLIENT_ID=" + userId;

            var resultSet = this.connection.prepareStatement(query).executeQuery();

            var reservations = new ArrayList<ReservationOrder>();

            while (resultSet.next()){
                reservations.add(
                       new ReservationOrder( resultSet.getInt("RESERVATION_ID") + "",
                               resultSet.getDate("CREATION_DATE").toString(),
                               resultSet.getDate("RESERVATION_DATE").toString(),
                               ReservationStatus.valueOf(resultSet.getString("STATUS")),
                               resultSet.getDouble("RESERVATION_TOTAL") + "")
                );
            }

            return reservations;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * This method add new table record to the db
     * @param seats number of seats at the table
     * @param rate hourly cost to rent table
     * @param location table location
     * @param unavailableDates dates the new table will be unavailable on
     * @return boolean indicating if table was added successfully
     */
    public boolean addTable(int seats, double rate, TableLocation location, String[] unavailableDates) {
        try {
            String query = String.format(
                    "INSERT INTO TABLES (SEATS_COUNT, HOURLY_RATE, LOCATION) VALUES (%d, %f, '%s')",
                    seats, rate, location.toString()
            );

            this.connection.prepareStatement(query).executeUpdate();

            var sb = new StringBuilder();
            sb.append("INSERT INTO TABLES_OFFDATES (OFF_DATE, TABLE_ID) VALUES");
            for (int i = 0; i < unavailableDates.length; i++) {
                sb.append(String.format(
                        " ('%s', %s),",
                        unavailableDates[i], "(SELECT MAX(TABLE_ID) FROM tables)"
                ));
            }
            sb.deleteCharAt(sb.length() - 1);

            this.connection.prepareStatement(sb.toString()).executeUpdate();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * This method fetches table by its id
     * @param tableId table id
     * @return table data object
     */
    public ReservationItem getTableById(int tableId) {
        try {
            String query = String.format("SELECT * FROM TABLES WHERE TABLE_ID=%d AND LOCATION != 'NONE'", tableId);

            var resultSet = this.connection.prepareStatement(query).executeQuery();

            ReservationItem table = null;

            while (resultSet.next()){
                table = new ReservationItem(
                        resultSet.getInt("TABLE_ID"),
                        resultSet.getInt("SEATS_COUNT"),
                        resultSet.getDouble("HOURLY_RATE"),
                        new String[]{},
                        TableLocation.valueOf(resultSet.getString("LOCATION"))
                );
            }

            return table;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * This method fetches tables for admin
     * @return list of table objects
     */
    public ArrayList<ReservationItem> getTables() {
        try {
            String query = String.format("SELECT * FROM TABLES WHERE LOCATION != 'NONE'");

            var resultSet = this.connection.prepareStatement(query).executeQuery();

            ArrayList<ReservationItem> tables = new ArrayList<ReservationItem>();

            while (resultSet.next()){
                tables.add(new ReservationItem(
                        resultSet.getInt("TABLE_ID"),
                        resultSet.getInt("SEATS_COUNT"),
                        resultSet.getDouble("HOURLY_RATE"),
                        new String[]{},
                        TableLocation.valueOf(resultSet.getString("LOCATION"))
                ));
            }

            return tables;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<ReservationItem>();
        }
    }

    /**
     * This method updates data related to specific table
     * @param seats new updated number of seats
     * @param rate new updated hourly rate
     * @param location new updated location
     * @param tableId table id
     * @param unavailableDates updated list of dates on which table is unavailable
     * @return boolean indicating if table was updated successfully
     */
    public boolean updateTable(int seats, double rate, TableLocation location, int tableId, String[] unavailableDates) {
        try {
            String query = String.format(
                    "UPDATE TABLES SET SEATS_COUNT=%d, HOURLY_RATE=%f, LOCATION='%s' WHERE TABLE_ID=%d",
                    seats, rate, location, tableId
            );

            this.connection.prepareStatement(query).executeUpdate();

            String query2 = String.format(
                    "DELETE FROM TABLES_OFFDATES WHERE TABLE_ID=%d",
                    tableId
            );

            this.connection.prepareStatement(query2).executeUpdate();

            if(unavailableDates[0].compareToIgnoreCase("Unavailable dates...") != 0) {
                var sb = new StringBuilder();
                sb.append("INSERT INTO TABLES_OFFDATES (OFF_DATE, TABLE_ID) VALUES");
                for (int i = 0; i < unavailableDates.length; i++) {
                    sb.append(String.format(
                            " ('%s', %d),",
                            unavailableDates[i], tableId
                    ));
                }
                sb.deleteCharAt(sb.length() - 1);
                this.connection.prepareStatement(sb.toString()).executeUpdate();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * This method erases some data tied to inactive table while preserving some and making table inaccessible
     * @param tableId id of table which is to be inactivated
     * @return boolean indicating if table was inactivated successfully
     */
    public boolean inactivateTableById(int tableId) {
        try {
            String deleteUnavailableDatesQuery = String.format("DELETE FROM TABLES_OFFDATES WHERE TABLE_ID=%d", tableId);
            var deleteUnavailableDatesQueryResult = this.connection.prepareStatement(deleteUnavailableDatesQuery).executeUpdate();

            String inactivateTableQuery = String.format("UPDATE TABLES SET LOCATION='NONE' WHERE TABLE_ID=%d", tableId);
            var inactivateTableQueryResult = this.connection.prepareStatement(inactivateTableQuery).executeUpdate();

            return inactivateTableQueryResult == 1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * This method fetches reservations accessible to barista
     * @param baristaId barista id
     * @return List of table reservation objects
     */
    public ArrayList<ReservedOrder> getPendingReservations(int baristaId) {
        try {
            var reservations = new ArrayList<ReservedOrder>();
            String query = String.format(
                    "SELECT meta.RESERVATION_ID, meta.CLIENT_ID, cli.IS_VIP, meta.CREATION_DATE, meta.RESERVATION_DATE, meta.RESERVATION_TOTAL, meta.BARISTA_ID \n" +
                            "FROM reservations_metadata meta\n" +
                            "LEFT JOIN clients cli ON cli.USER_ID = meta.CLIENT_ID\n" +
                            "WHERE meta.STATUS='PENDING' AND (meta.BARISTA_ID=%d OR meta.BARISTA_ID IS NULL)",
                            baristaId
            );

            var result = this.connection.prepareStatement(query).executeQuery();

            while (result.next()) {
                var resId = result.getInt("RESERVATION_ID");
                var query2 = String.format(
                        "SELECT * \n" +
                                "FROM reservations_contents cont\n" +
                                "JOIN tables tab on tab.TABLE_ID=cont.TABLE_ID \n" +
                                "WHERE RESERVATION_ID=%d AND tab.LOCATION != 'NONE'",
                        resId
                );

                var result2 = this.connection.prepareStatement(query2).executeQuery();
                var reservedTables = new ArrayList<ReservedTable>();

                while (result2.next()) {
                    var tableId = result2.getInt("TABLE_ID");

                    var reservedHours = new ArrayList<String>();

                    reservedHours.add(result2.getInt("START_HOUR") + "");
                    reservedTables.add(
                            new ReservedTable(
                                    tableId,
                                    result2.getInt("SEATS_COUNT"),
                                    result2.getDouble("HOURLY_RATE"),
                                    String.join(", ", reservedHours),
                                    TableLocation.valueOf(result2.getString("LOCATION"))
                            )
                    );
                }

                reservations.add(
                        new ReservedOrder(
                                resId,
                                result.getInt("CLIENT_ID"),
                                result.getBoolean("IS_VIP") ? "YES" : "NO",
                                result.getDate("CREATION_DATE"),
                                reservedTables.toArray(new ReservedTable[reservedTables.size()]),
                                result.getDate("RESERVATION_DATE"),
                                result.getDouble("RESERVATION_TOTAL"),
                                result.getInt("BARISTA_ID")
                        )
                );
            }

            return reservations;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<ReservedOrder>();
        }
    }

    /**
     * This method records the table chosen by assigning barista to reservation order
     * @param reservationId reservation id
     * @param baristaId barista id
     * @return boolean indicating if reservation was successfully assigned to barista
     */
    public boolean recordReservation(int reservationId, int baristaId) {
        try {
            String query = String.format(
                    "UPDATE RESERVATIONS_METADATA SET BARISTA_ID=%d WHERE RESERVATION_ID=%d",
                    baristaId, reservationId
            );

            var result = this.connection.prepareStatement(query).executeUpdate();

            return result == 1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * This method packages reservation data for display during reservation checkout ("reserveout")
     * @param reservationData table selection data
     * @return reservation data consisting of table data and reservation total value
     */
    public ReservationCart getReservationCartItems(ReservationData reservationData) {
        var tableSet = reservationData.getTableHoursMap().entrySet();
        var tablesWithRates = this.getTablesById(String.valueOf(reservationData.getTableHoursMap().keySet()));
        var cartItems = new ReservationCartItem[tableSet.size()];

        var i = 0;
        var orderTotal = 0;
        for (var r: tableSet) {
            var hours = r.getValue().size();
            var rate = tablesWithRates.get(r.getKey()).getRate();
            var itemTotal = hours * rate;
            cartItems[i] = new ReservationCartItem(
                    r.getKey(),
                    hours,
                    String.join(",", reservationData.getTableHoursMap().get(r.getKey())),
                    rate,
                    itemTotal
            );
            orderTotal += itemTotal;
            i++;
        }

        return new ReservationCart(cartItems, orderTotal);
    }

    /**
     * This method fetches dates (within specified range) on which table is unavailable
     * @param tableId table id
     * @param rangeStart start of range
     * @param rangeEnd end of range
     * @return list of dates
     */
    public ArrayList<String> getUnavailableDatesByTableId(int tableId, String rangeStart, String rangeEnd) {
        var dates = new ArrayList<String>();

        try {
            String query = String.format(
                    "SELECT * \n" +
                    "FROM tables_offdates\n" +
                    "WHERE TABLE_ID=%d AND (OFF_DATE >= '%s' AND OFF_DATE <= '%s')",
                    tableId, rangeStart, rangeEnd
            );

            var resultSet = this.connection.prepareStatement(query).executeQuery();
            while (resultSet.next()){ dates.add(resultSet.getDate("OFF_DATE").toString()); }

            return dates;
        } catch (Exception e) {
            e.printStackTrace();
            return dates;
        }
    }

    /**
     * This method fetches dates (within specified range) on which table was reserved by a client
     * @param tableId table id
     * @param rangeStart start of range
     * @param rangeEnd end of range
     * @return List of menu item objects
     */
    public ArrayList<String> getClientTouchedDates(int tableId, String rangeStart, String rangeEnd) {
        var dates = new ArrayList<String>();

        try {
            String query = String.format(
                    "SELECT meta.RESERVATION_DATE\n" +
                            "FROM reservations_metadata meta\n" +
                            "JOIN reservations_contents cont ON meta.RESERVATION_ID=cont.RESERVATION_ID\n" +
                            "WHERE cont.TABLE_ID=%d AND meta.RESERVATION_DATE >= '%s' AND meta.RESERVATION_DATE <= '%s'\n" +
                            "GROUP BY meta.RESERVATION_DATE",
                    tableId, rangeStart, rangeEnd
            );

            var resultSet = this.connection.prepareStatement(query).executeQuery();
            while (resultSet.next()){ dates.add(resultSet.getDate("RESERVATION_DATE").toString()); }

            return dates;
        } catch (Exception e) {
            e.printStackTrace();
            return dates;
        }
    }

    /**
     * This method completes reservation and credits funds to barista
     * @param reservationId reservation id
     * @param baristaId barista id
     * @return boolean indicating if reservation was successfully assigned to barista
     */
    public boolean completeReservation(int reservationId, int baristaId) {
        try {
            var amountEarnedByBaristaQuery = String.format(
                    "SELECT AMOUNT_EARNED FROM BARISTAS WHERE USER_ID=%d",
                    baristaId
            );

            var amountEarnedByBaristaQueryRes = this.connection.prepareStatement(amountEarnedByBaristaQuery).executeQuery();
            double earnedByNow = 0;
            while (amountEarnedByBaristaQueryRes.next()) {
                earnedByNow = amountEarnedByBaristaQueryRes.getDouble("AMOUNT_EARNED");
            }

            // make sure that order is assigned to barista
            var query2 = "SELECT RESERVATION_TOTAL \n" +
                    "FROM reservations_metadata\n" +
                    "WHERE BARISTA_ID=" + baristaId + " AND RESERVATION_ID=" + reservationId;
            var res2 = this.connection.prepareStatement(query2).executeQuery();
            var reservationTotalValue = 0.0;
            while (res2.next()) {
                reservationTotalValue = res2.getDouble("RESERVATION_TOTAL");
            }

            var query3=
                    String.format(
                            "REPLACE INTO BARISTAS (USER_ID, AMOUNT_EARNED) VALUES(%d, %f)",
                            baristaId, reservationTotalValue + earnedByNow
                    );

            this.connection.prepareStatement(query3).executeUpdate();

            var query4=
                    String.format(
                            "UPDATE reservations_metadata SET STATUS='COMPLETED' WHERE RESERVATION_ID=%d",
                            reservationId
                    );

            this.connection.prepareStatement(query4).executeUpdate();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
