package Utilities;

import Data.Models.ReservationData;
import Data.Services.MenuItemService;
import Data.Models.OrderData;
import Data.Models.MenuItem;
import Data.Models.TakeoutOrderItem;
import org.eclipse.jetty.util.UrlEncoded;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Transformation class is responsible for transforming raw data originating from http requests into objects
 */
public class Transformation {
    /**
     * This method extracts client-selected items from an order and matches them with their actual prices
     * @param dataPairs array of values pertaining to an order
     * @param dbConnection instance of db connection that is passed into service objects
     * @return OrderData (ordered items and order total value)
     */
    public static OrderData extractOrderData(String[] dataPairs, java.sql.Connection dbConnection) {
        try {
            // consumable id + quantity
            HashMap<String, Integer> consumablesTakeoutList = new HashMap<String, Integer>();

            for (int i = 0; i < dataPairs.length; i = i + 3) {
                String consumableTitle = dataPairs[i].split("=")[1]; // deprecated
                int consumableId = Integer.parseInt(dataPairs[i+1].split("=")[1].replaceAll(" ", ""));
                int consumableQuantity = Integer.parseInt(dataPairs[i+2].split("=")[1]);

                if(consumableQuantity > 0) {
                    consumablesTakeoutList.put(consumableId+"", consumableQuantity);
                }
            }

            var menuItemService = new MenuItemService(dbConnection);
            LinkedList<MenuItem> consumablesWithPrices = null;

            if(consumablesTakeoutList.size() > 0) {
                consumablesWithPrices = menuItemService.
                        getSpecificMenuItemsById(
                                consumablesTakeoutList.keySet().toArray(new String[0])
                        );
            }

            var takeoutOrderItems = new TakeoutOrderItem[consumablesWithPrices.size()];

            double orderTotal = 0;
            for (int i = 0; i < consumablesWithPrices.size(); i++) {
                var item = consumablesWithPrices.get(i);
                var itemTotal = item.getPrice() * consumablesTakeoutList.get(item.getConsumableId()+"");
                orderTotal += itemTotal;

                var orderItem = new TakeoutOrderItem(
                        item.getConsumableId(),
                        item.getTitle(),
                        item.getPrice(),
                        consumablesTakeoutList.get(item.getConsumableId()+""),
                        itemTotal
                );
                takeoutOrderItems[i] = orderItem;
            }
            return new OrderData(takeoutOrderItems, orderTotal);
        } catch (Exception e) {
            e.printStackTrace();
            return new OrderData(new TakeoutOrderItem[0], 0);
        }
    }

    /**
     * This method extracts data related to table selection
     * @param formBody is raw String received from the client
     * @return ReservationData (reservation date and tables + reserved hours)
     * */
    public static ReservationData parseReservationData(String formBody) {
        var map = new HashMap<String, ArrayList<String>>();

        try {
            if(formBody.length() == 0) {
                return new ReservationData(new Date(), map);
            }

            var decoded = UrlEncoded.decodeString(formBody);
            String[] reservationDataPairs = decoded.split("&");
            var reservationDate = reservationDataPairs[0].split("=")[1];

            for (int i = 1; i < reservationDataPairs.length; i++) {
                var pair = reservationDataPairs[i].split("=");

                var currArr = map.get(pair[0]);

                if(currArr == null) {
                    map.put(pair[0], new ArrayList<>());
                }

                map.get(pair[0]).add(pair[1]);
            }
            var format = new SimpleDateFormat("yyyy-MM-dd");
            return new ReservationData(format.parse(reservationDate), map);
        } catch (Exception e) {
            e.printStackTrace();
            return new ReservationData(new Date(), map);
        }
    }

    /**
     * This method extracts data related to table selection (from checkout page)
     * @param formDataPairs is raw String received from the client
     * @return ReservationData (reservation date and tables + reserved hours)
     * */
    public static ReservationData parseReservationData(String[] formDataPairs) {
        var map = new HashMap<String, ArrayList<String>>();

        try {
            if(formDataPairs.length == 0) { return new ReservationData(new Date(), map); }

            var reservationDate = formDataPairs[0].split("=")[1];

            for (int i = 1; i < formDataPairs.length; i = i + 2) {
                var tableId = formDataPairs[i].split("=")[1];
                var hoursList = formDataPairs[i+1].split("=")[1]; // e.g. "20—21,21—22"

                var currArr = map.get(tableId);

                if(currArr == null) {
                    map.put(tableId, new ArrayList<>());
                }

                var hoursList2 = hoursList.split(",");

                for (int j = 0; j < hoursList2.length; j++) {
                    map.get(tableId).add(hoursList2[j]);
                }
            }
            var format = new SimpleDateFormat("yyyy-MM-dd");
            return new ReservationData(format.parse(reservationDate), map);
        } catch (Exception e) {
            e.printStackTrace();
            return new ReservationData(new Date(), map);
        }
    }
}
