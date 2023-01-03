package Data.Services;

import Data.Models.TakeoutOrder;
import Data.Models.TakeoutOrderItem;
import Data.Enums.OrderStatus;
import java.sql.Connection;
import java.util.ArrayList;

/**
 * OrderService class contains functionality tailored for takeout order-related db interaction
 *  */
public class OrderService {
    // db connection object
    private java.sql.Connection connection;

    /**
     * constructor
     * @param connection db connection object
     */
    public OrderService(Connection connection) {
        this.connection = connection;
    }

    /**
     * This method creates db records related to order
     * @param userId id of client the order originated from
     * @param orderItems items that nake up the order
     * @param orderTotal total order value
     * @param freeCoffees number of free coffees ordered
     * @return boolean indicating if order was added successfully
     */
    public boolean createOrder(int userId, TakeoutOrderItem[] orderItems, double orderTotal, int freeCoffees) {
        try {
            var query=
                    String.format(
                            "INSERT INTO ORDERS_METADATA (CLIENT_ID, CREATION_DATE, STATUS, ORDER_TOTAL) VALUES (%d, %s, '%s', %f)",
                            userId, "CURDATE()", OrderStatus.PENDING, orderTotal
                    );
            this.connection.createStatement().executeUpdate(query);

            var sb = new StringBuilder();
            sb.append("INSERT INTO ORDERS_CONTENTS (ORDER_ID, ITEM_ID, QUANTITY) VALUES");
            for (int i = 0; i < orderItems.length; i++) {
                sb.append(
                        String.format(
                                " (LAST_INSERT_ID(), %d, %d),",
                                orderItems[i].getConsumableId(), orderItems[i].getQuantity()
                        ));

                var resSet = this.connection.prepareStatement("SELECT IN_STOCK FROM CONSUMABLES WHERE CONSUMABLE_ID=" + orderItems[i].getConsumableId()).executeQuery();
                resSet.next();
                var currentlyInStock = resSet.getInt("IN_STOCK");
                this.connection.prepareStatement("UPDATE CONSUMABLES SET IN_STOCK=" + (currentlyInStock - orderItems[i].getQuantity()) + " WHERE CONSUMABLE_ID=" + orderItems[i].getConsumableId()).executeUpdate();
            }
            sb.deleteCharAt(sb.length() - 1);
            this.connection.createStatement().executeUpdate(sb.toString());

            // check if client is vip
            var clientService = new UserService(this.connection);
            var client = clientService.getClientById(userId + "");
            if (client.isVip() && freeCoffees > 0) {
                this.connection.createStatement().executeUpdate(String.format(
                        "INSERT INTO VIP_COFFEES (ORDER_ID, QUANTITY) VALUES (LAST_INSERT_ID(), %d)", freeCoffees
                ));
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * This method fetches records related to orders originated from client with specified id
     * @param userId id of client
     * @return list of orders
     */
    public ArrayList<TakeoutOrder> getOrdersByUserId(int userId) {
        try {
            var query=
                    String.format(
                            "SELECT * FROM ORDERS_METADATA WHERE CLIENT_ID=%d",
                            userId
                    );

            var takeoutOrdersDataSet = this.connection.prepareStatement(query).executeQuery();

            var orders = new ArrayList<TakeoutOrder>();

            while(takeoutOrdersDataSet.next()) {
                orders.add(
                        new TakeoutOrder(
                                takeoutOrdersDataSet.getInt("ORDER_ID"),
                                userId,
                                new TakeoutOrderItem[0], // No need to have them now
                                takeoutOrdersDataSet.getDate("CREATION_DATE"),
                                OrderStatus.valueOf(takeoutOrdersDataSet.getString("STATUS")),
                                takeoutOrdersDataSet.getDouble("ORDER_TOTAL")
                        )
                );
            }

            return orders;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * This method fetches one order record by its id
     * @param orderId id of the order to fetch
     * @return order object or null
     */
    public TakeoutOrder getOrderById(int orderId) {
        TakeoutOrder order = null;

        try {
            var query=
                    String.format(
                            "SELECT M.ORDER_ID, M.CLIENT_ID, M.STATUS, M.CREATION_DATE, M.ORDER_TOTAL \n" +
                                    "FROM ORDERS_METADATA M\n" +
                                    "JOIN ORDERS_CONTENTS C on C.ORDER_ID = M.ORDER_ID\n" +
                                    "WHERE C.ORDER_ID=%d", orderId
                    );

            var orderContents = this.connection.prepareStatement(query).executeQuery();

            while (orderContents.next()) {
                    order = new TakeoutOrder(
                            orderContents.getInt("ORDER_ID"),
                            orderContents.getInt("CLIENT_ID"),
                            new TakeoutOrderItem[]{},
                            orderContents.getDate("CREATION_DATE"),
                            OrderStatus.valueOf(orderContents.getString("STATUS")),
                            orderContents.getDouble("ORDER_TOTAL")
                    );
            }

            return order;
        } catch (Exception e) {
            e.printStackTrace();
            return order;
        }
    }

    /**
     * This method updates order record with new data
     * @param callerId user id
     * @param orderId order id
     * @param items items that are present in the updated version of the order
     * @param updatedFreeCoffees number of free coffees client wants
     * @return boolean indicating if order was updated successfully
     */
    public boolean editOrderById(int callerId, int orderId, TakeoutOrderItem[] items, int updatedFreeCoffees) {
        try {
            var order = getOrderById(orderId);
            // check if user is owner and if order is editable
            if(callerId != order.getClientId() || order.getStatus() != OrderStatus.PENDING) { return false;}

            double newOrderTotal = 0;
            for (TakeoutOrderItem it: items ) {
                newOrderTotal += it.getQuantity() * it.getPricePerPiece();
            }

            changeOrderTotal(orderId, newOrderTotal);
            redoOrderItems(orderId, items);
            redoVipCoffees(orderId, updatedFreeCoffees);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * This method updates order total value
     * @param orderId order id
     * @param newValue new order total value
     * @return boolean indicating if order total was updated successfully
     */
    private boolean changeOrderTotal(int orderId, double newValue) {
        try {
            var query=
                    String.format(
                            "UPDATE ORDERS_METADATA SET ORDER_TOTAL=%f WHERE ORDER_ID=%d",
                            newValue,orderId
                    );

            var result = this.connection.prepareStatement(query).executeUpdate();

            return result > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * This method recreates order contents
     * @param orderId order id
     * @param orderItems items that are present in the updated version of the order
     * @return boolean indicating if order items were updated successfully
     */
    private boolean redoOrderItems(int orderId, TakeoutOrderItem[] orderItems) {
        try {
            var selectOrderContentsData = String.format("SELECT ITEM_ID, QUANTITY FROM ORDERS_CONTENTS WHERE ORDER_ID=%d", orderId);
            var selectOrderContentsDataResult = this.connection.prepareStatement(selectOrderContentsData).executeQuery();

            while (selectOrderContentsDataResult.next()) {
                var itemId = selectOrderContentsDataResult.getInt("ITEM_ID");
                var alreadyOrderedQty = selectOrderContentsDataResult.getInt("QUANTITY");

                var inStockData = this.connection.prepareStatement("SELECT IN_STOCK FROM CONSUMABLES WHERE CONSUMABLE_ID=" + itemId).executeQuery();
                inStockData.next();
                var currentlyInStock = inStockData.getInt("IN_STOCK");

                this.connection.prepareStatement(String.format("UPDATE CONSUMABLES SET IN_STOCK=%d WHERE CONSUMABLE_ID=%d", alreadyOrderedQty + currentlyInStock, itemId)).executeUpdate();
            }

            var query=
                    String.format(
                            "DELETE FROM ORDERS_CONTENTS WHERE ORDER_ID=%d",
                            orderId
                    );
            var result = this.connection.prepareStatement(query).executeUpdate();

            var sb = new StringBuilder();
            sb.append("INSERT INTO ORDERS_CONTENTS (ORDER_ID, ITEM_ID, QUANTITY) VALUES");
            for (int i = 0; i < orderItems.length; i++) {
                sb.append(
                        String.format(
                                " (%d, %d, %d),",
                                orderId, orderItems[i].getConsumableId(), orderItems[i].getQuantity()
                        ));

                var query5 = this.connection.prepareStatement(String.format("SELECT IN_STOCK FROM CONSUMABLES WHERE CONSUMABLE_ID=" + orderItems[i].getConsumableId())).executeQuery();
                query5.next();
                var currentQty = query5.getInt("IN_STOCK");
                this.connection.prepareStatement(String.format("UPDATE CONSUMABLES SET IN_STOCK=%d WHERE CONSUMABLE_ID=%d", (currentQty - orderItems[i].getQuantity()), orderItems[i].getConsumableId())).executeUpdate();
            }
            sb.deleteCharAt(sb.length() - 1);
            var result2 = this.connection.createStatement().executeUpdate(sb.toString());

            return (result > 0) && (result2 > 0);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * This method updates order vip coffee value
     * @param orderId order id
     * @param updatedFreeCoffees number of free coffees client wants
     * @return boolean indicating if order vip coffee value was updated successfully
     */
    private boolean redoVipCoffees(int orderId, int updatedFreeCoffees) {
        try {
            var query= "";

            if(updatedFreeCoffees == 0) {
                query = String.format(
                                "DELETE FROM VIP_COFFEES WHERE ORDER_ID=%d",
                                orderId
                        );
            } else {
                this.connection.prepareStatement(String.format(
                        "DELETE FROM VIP_COFFEES WHERE ORDER_ID=%d",
                        orderId
                )).executeUpdate();

                query = String.format(
                                "INSERT INTO VIP_COFFEES (ORDER_ID, QUANTITY) VALUES (%d, %d)",
                                orderId, updatedFreeCoffees
                                );
            }

            var result = this.connection.prepareStatement(query).executeUpdate();

            return (result > 0);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * This method fetches free coffee data pertaining to a certain order
     * @param orderId order id
     * @return int number of free coffees taken within order
     */
    public int getOrderedFreeCoffeesByOrderId(int orderId) {
        try {
            var query=
                    String.format(
                            "SELECT QUANTITY FROM VIP_COFFEES WHERE ORDER_ID=%d",
                            orderId
                    );

            var resultSet = this.connection.prepareStatement(query).executeQuery();
            var qty = 0;
            while (resultSet.next()) {
                qty = resultSet.getInt("QUANTITY");
            }
            return qty;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * This method fetches orders that are taken/ can be taken by barista with specified id
     * @param baristaId barista id
     * @return list of orders that meet the requirements
     */
    public ArrayList<TakeoutOrder> getPendingOrders(int baristaId) {
        try {
            var query=  String.format("SELECT * FROM ORDERS_METADATA WHERE STATUS='PENDING' AND (BARISTA_ID=%d OR BARISTA_ID IS NULL)", baristaId);

            var takeoutOrdersDataSet = this.connection.prepareStatement(query).executeQuery();

            var orders = new ArrayList<TakeoutOrder>();

            while(takeoutOrdersDataSet.next()) {
                var orderId = takeoutOrdersDataSet.getInt("ORDER_ID");
                var query2 = String.format(
                        "SELECT cons.CONSUMABLE_ID, cons.TITLE, cons.PRICE, cont.QUANTITY, meta.ORDER_TOTAL, meta.CLIENT_ID, clients.IS_VIP, meta.BARISTA_ID \n" +
                        "FROM orders_contents cont\n" +
                        "JOIN consumables cons ON cont.ITEM_ID = cons.CONSUMABLE_ID\n" +
                        "JOIN orders_metadata meta on meta.ORDER_ID = cont.ORDER_ID \n" +
                        "LEFT JOIN clients ON clients.USER_ID = meta.CLIENT_ID \n" +
                        "WHERE cont.ORDER_ID=%d AND (meta.BARISTA_ID=%d OR meta.BARISTA_ID IS NULL)\n",
                        orderId, baristaId
                );
                var orderContentsDataSet = this.connection.prepareStatement(query2).executeQuery();
                ArrayList<TakeoutOrderItem> orderItems = new ArrayList<TakeoutOrderItem>();
                boolean isVipClient = false;

                while(orderContentsDataSet.next()) {
                    isVipClient = orderContentsDataSet.getBoolean("IS_VIP");

                    orderItems.add(new TakeoutOrderItem(
                            orderContentsDataSet.getInt("CONSUMABLE_ID"),
                            orderContentsDataSet.getString("TITLE"),
                            orderContentsDataSet.getDouble("PRICE"),
                            orderContentsDataSet.getInt("QUANTITY"),
                            orderContentsDataSet.getDouble("ORDER_TOTAL")
                    ));
                }

                // add free coffees to orderItems if applicable
                var freeCoffeesWithinOrderQuery = String.format(
                        "SELECT QUANTITY \n" +
                        "FROM vip_coffees\n" +
                        "WHERE ORDER_ID=%d", orderId
                );
                var freeCoffeesWithinOrderQueryResult = this.connection.prepareStatement(freeCoffeesWithinOrderQuery).executeQuery();
                while (freeCoffeesWithinOrderQueryResult.next()) {
                    var qty = freeCoffeesWithinOrderQueryResult.getInt("QUANTITY");
                    var msg = String.format("FREE %s", qty > 1 ? "Coffees" : "Coffee");
                    // need just enough fields to display "N FREE COFFEES"
                    orderItems.add(new TakeoutOrderItem(0, msg,0, qty,0));
                }

                orders.add(
                        new TakeoutOrder(
                                orderId,
                                takeoutOrdersDataSet.getInt("CLIENT_ID"),
                                isVipClient ? "YES" : "NO",
                                orderItems.toArray(TakeoutOrderItem[]::new),
                                takeoutOrdersDataSet.getDate("CREATION_DATE"),
                                OrderStatus.valueOf(takeoutOrdersDataSet.getString("STATUS")),
                                takeoutOrdersDataSet.getDouble("ORDER_TOTAL"),
                                takeoutOrdersDataSet.getInt("BARISTA_ID")
                        )
                );
            }

            return orders;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * This method assigns order to barista
     * @param orderId order id
     * @param baristaId barista id
     * @return boolean indicating if order was successfully assigned to barista
     */
    public boolean takeOrder(int orderId, int baristaId) {
        try {
            var query=
                    String.format(
                            "UPDATE ORDERS_METADATA SET BARISTA_ID=%d WHERE ORDER_ID=%d",
                            baristaId, orderId
                    );

            var res = this.connection.prepareStatement(query).executeUpdate();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * This method completes order and credits order total value to barista balance
     * @param orderId order id
     * @param baristaId barista id
     * @return boolean indicating if order was successfully completed and barista received earnings
     */
    public boolean completeOrderAndTakePayment(int orderId, int baristaId) {
        try {
            var query0 = String.format(
                    "SELECT ORDER_TOTAL, STATUS FROM ORDERS_METADATA WHERE ORDER_ID=%d",
                    orderId
            );

            var res = this.connection.prepareStatement(query0).executeQuery();
            res.next();
            double orderTotalValue = res.getDouble("ORDER_TOTAL");
            OrderStatus status = OrderStatus.valueOf(res.getString("STATUS"));

            if(status == OrderStatus.COMPLETED) {
                return false;
            }

            var query1 = String.format(
                    "SELECT AMOUNT_EARNED FROM BARISTAS WHERE USER_ID=%d",
                    baristaId
            );

            var res1 = this.connection.prepareStatement(query1).executeQuery();
            double earnedByNow = 0;
            while (res1.next()) {
                earnedByNow = res1.getDouble("AMOUNT_EARNED");
            }

            var query2=
                    String.format(
                            "REPLACE INTO BARISTAS (USER_ID, AMOUNT_EARNED) VALUES(%d, %f)",
                            baristaId, orderTotalValue + earnedByNow
                    );

            this.connection.prepareStatement(query2).executeUpdate();

            this.connection.prepareStatement(String.format(
                    "UPDATE ORDERS_METADATA SET STATUS='COMPLETED' WHERE ORDER_ID=%d",
                    orderId
            )).executeUpdate();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * This method facilitates barista changing order
     * @param orderId order id
     * @param items updated version items list
     * @param baristaId barista id
     * @param updatedFreeCoffees updated number of coffees taken by client
     * @return boolean indicating if order was successfully changed by barista
     */
    public boolean changeOrder(int orderId, TakeoutOrderItem[] items, int baristaId, int updatedFreeCoffees) {
        double newOrderTotal = 0;
        for (TakeoutOrderItem it: items ) {
            newOrderTotal += it.getQuantity() * it.getPricePerPiece();
        }

        changeOrderTotal(orderId, newOrderTotal);
        redoOrderItems(orderId, items);
        redoVipCoffees(orderId, updatedFreeCoffees);

        return true;
    }
}
