package Data.Services;

import Data.Enums.ConsumableType;
import Data.Models.MenuItem;
import Data.Enums.OrderStatus;
import Data.Models.OrderMenuData;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;

/**
 * MenuItemService class contains functionality tailored for shop menu-related db interaction
 *  */
public class MenuItemService {
    // db connection object
    private java.sql.Connection connection;

    /**
     * constructor
     * @param connection db connection object
     */
    public MenuItemService(Connection connection) {
        this.connection = connection;
    }

    /**
     * This method fetches menu items from the db
     * used in displaying data to barista
     * @return List of menu item objects
     */
    public LinkedList<MenuItem> getAllMenuItems() {
        try {
            String query = "SELECT * FROM CONSUMABLES";

            var resultSet = this.connection.prepareStatement(query).executeQuery();

            LinkedList menuItems = new LinkedList();

            while (resultSet.next()){
                if(ConsumableType.valueOf(resultSet.getString("TYPE")) != ConsumableType.NONE) {
                    menuItems.add(new MenuItem(
                            resultSet.getInt("CONSUMABLE_ID"),
                            resultSet.getString("TITLE"),
                            Double.parseDouble(resultSet.getString("PRICE")),
                            resultSet.getString("DESCRIPTION"),
                            resultSet.getString("TYPE"),
                            resultSet.getInt("IN_STOCK")
                    ));
                }
            }

            return menuItems;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * This method fetches menu items from the db
     * used in displaying data to client
     * @param sorting "sort by" request parameter
     * @param model hashmap with data for later use in the UI
     * @return List of menu item objects
     */
    public LinkedList<MenuItem> getAllMenuItems(String sorting, Map<String, Object> model) {
        try {
            String query = "SELECT * FROM CONSUMABLES \n";

            if(sorting.length() > 0) {
                switch (sorting) {
                    case "price-asc":
                        query += "ORDER BY PRICE ASC";
                        model.put("isPriceAscSelected", "true");
                        break;
                    case "price-desc":
                        query += "ORDER BY PRICE DESC";
                        model.put("isPriceDescSelected", "true");
                        break;
                    case "popularity":
                        query = "SELECT DISTINCT cons.CONSUMABLE_ID, cons.TITLE, cons.DESCRIPTION, cons.PRICE, cons.TYPE, cons.IN_STOCK, COUNT(cont.QUANTITY) as qty\n" +
                                "FROM consumables cons\n" +
                                "LEFT JOIN orders_contents cont on cont.ITEM_ID=cons.CONSUMABLE_ID OR NOT EXISTS (SELECT * FROM orders_contents)\n" +
                                "GROUP BY cons.CONSUMABLE_ID\n" +
                                "ORDER BY COUNT(cont.QUANTITY) DESC\n" +
                                "LIMIT " + Dotenv.configure().load().get("TOP_LIMIT");
                        model.put("isPopularitySelected", "true");
                        break;
                    case "oftheday":
                        model.put("isOfTheDaySelected", "true");
                        var queryConsumableOfTheDay = String.format("SELECT * \n" +
                                "FROM consumable_otd\n" +
                                "WHERE FOR_DATE=CURDATE()");

                        var set = this.connection.prepareStatement(queryConsumableOfTheDay).executeQuery();
                        var cid = -1;
                        while (set.next()) {
                            cid = set.getInt("CONSUMABLE_ID");
                        }

                        if(cid == -1) {
                            // add otd if there is no otd
                            var candidatesQuery = String.format("SELECT * \n" +
                                    "FROM consumables\n" +
                                    "WHERE IN_STOCK > 0 AND TYPE != 'NONE'"
                            );
                            var candidatesSet = this.connection.prepareStatement(candidatesQuery).executeQuery();
                            var candidatesIds = new ArrayList<Integer>();

                            while (candidatesSet.next()) {
                                var id = candidatesSet.getInt("CONSUMABLE_ID");
                                candidatesIds.add(id);
                            }

                            var minIndex = 0;
                            var maxIndex = candidatesIds.size() - 1;
                            int winnerIndex = (int)(Math.random()*(maxIndex-minIndex+1)+minIndex);
                            int winnerId = candidatesIds.get(winnerIndex);

                            var str = String.format("INSERT INTO CONSUMABLE_OTD (FOR_DATE, CONSUMABLE_ID) VALUES (CURDATE(), %d)", winnerId);
                            this.connection.prepareStatement(str).executeUpdate();

                            query += "WHERE CONSUMABLE_ID=" +  winnerId;
                        } else {
                            query += "WHERE CONSUMABLE_ID=" +  cid;
                        }

                        break;
                    default:
                        model.put("isNoneSelected", "true");
                }
            }

            var resultSet = this.connection.prepareStatement(query).executeQuery();

            LinkedList menuItems = new LinkedList();

            while (resultSet.next()){
                if(ConsumableType.valueOf(resultSet.getString("TYPE")) != ConsumableType.NONE) {
                    menuItems.add(new MenuItem(
                            resultSet.getInt("CONSUMABLE_ID"),
                            resultSet.getString("TITLE"),
                            Double.parseDouble(resultSet.getString("PRICE")),
                            resultSet.getString("DESCRIPTION"),
                            resultSet.getString("TYPE"),
                            resultSet.getInt("IN_STOCK")
                    ));
                }
            }

            return menuItems;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * This method fetches data for specific items
     * used for extracting current prices
     * @param ids ids of specific items
     * @return List of menu item objects
     */
    public LinkedList<MenuItem> getSpecificMenuItemsById(String ... ids) {
        var sb = new StringBuilder();

        try {
            for (int i = 0; i < ids.length; i++) {
                if(i == ids.length - 1) {
                    sb.append(ids[i]);
                } else {
                    sb.append(ids[i] + ", ");
                }
            }

            String query = String.format("SELECT * FROM CONSUMABLES WHERE CONSUMABLE_ID IN (%s)", sb);

            var resultSet = this.connection.prepareStatement(query).executeQuery();

            LinkedList menuItems = new LinkedList();

            while (resultSet.next()){
                menuItems.add(new MenuItem(
                        resultSet.getInt("CONSUMABLE_ID"),
                        resultSet.getString("TITLE"),
                        Double.parseDouble(resultSet.getString("PRICE")),
                        resultSet.getString("DESCRIPTION"),
                        resultSet.getString("TYPE"),
                        resultSet.getInt("IN_STOCK")
                ));
            }

            return menuItems;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * This method fetches data for specific items
     * used for extracting current prices
     * @param orderId order id
     * @return List of menu item objects
     */
    public OrderMenuData getMenuItemsByOrderId(int orderId) {
        ArrayList menuItems = new ArrayList();
        OrderStatus orderStatus = OrderStatus.NONE;

        try {
            var query=
                    String.format(
                            "SELECT cons.consumable_id, cons.title, contents.quantity, cons.price, cons.description, meta.status, cons.IN_STOCK "+
                                    "FROM CONSUMABLES CONS " +
                                    "JOIN ORDERS_CONTENTS CONTENTS ON CONTENTS.ITEM_ID = CONS.CONSUMABLE_ID " +
                                    "JOIN ORDERS_METADATA META ON META.ORDER_ID = contents.order_id " +
                                    "WHERE CONTENTS.ORDER_ID=%d "+
                                    "ORDER BY contents.order_id", orderId
                    );

            var resultSet = this.connection.prepareStatement(query).executeQuery();

            while (resultSet.next()){
                orderStatus = OrderStatus.valueOf(resultSet.getString("STATUS"));
                var consId = resultSet.getInt("CONSUMABLE_ID");
                var resultSet2 = this.connection.prepareStatement("SELECT QUANTITY FROM ORDERS_CONTENTS WHERE ORDER_ID=" + orderId + " AND ITEM_ID=" + consId).executeQuery();
                resultSet2.next();
                var combinedQty = (resultSet.getInt("IN_STOCK") + resultSet2.getInt("QUANTITY"));

                menuItems.add(new MenuItem(
                        consId,
                        resultSet.getString("title"),
                        Double.parseDouble(resultSet.getString("price")),
                        resultSet.getString("description"),
                        resultSet.getInt("QUANTITY"),
                        Double.parseDouble(resultSet.getString("price")) * resultSet.getInt("QUANTITY"),
                        resultSet.getInt("IN_STOCK")
                ));
            }
            return new OrderMenuData(menuItems, orderStatus);
        } catch (Exception e) {
            e.printStackTrace();
            return new OrderMenuData(menuItems, orderStatus);
        }
    }

    /**
     * This method fetches menu items of certain types
     * @param types types of items to fetch
     * @return List of menu item objects
     */
    public LinkedList<MenuItem> getMenuItemsByType(ConsumableType ... types) {
        var sb = new StringBuilder();
        sb.append("(");
        for (int i = 0; i < types.length; i++) {
            if(i == types.length - 1) {
                sb.append("'" + types[i] + "'");
            } else {
                sb.append("'" + types[i] + "',");
            }
        }
        sb.append(")");

        try {
            String query = "SELECT * FROM CONSUMABLES WHERE TYPE IN " + sb;

            var resultSet = this.connection.prepareStatement(query).executeQuery();

            LinkedList menuItems = new LinkedList();

            while (resultSet.next()){
                menuItems.add(new MenuItem(
                        resultSet.getInt("CONSUMABLE_ID"),
                        resultSet.getString("TITLE"),
                        Double.parseDouble(resultSet.getString("PRICE")),
                        resultSet.getString("DESCRIPTION"),
                        resultSet.getString("TYPE"),
                        resultSet.getInt("IN_STOCK")
                ));
            }

            return menuItems;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * This method adds a new menu record to the db
     * @param title item title (name)
     * @param desc item description
     * @param price item price (per piece)
     * @param type item type
     * @param inStock how many items of this kind are in stock
     * @return boolean indicating if item was added successfully
     */
    public boolean addMenuItem(String title, String desc, double price, String type, int inStock) {
        try {
            String query = String.format(
                    "INSERT INTO CONSUMABLES (TITLE, DESCRIPTION, PRICE, TYPE, IN_STOCK) VALUES ('%s', '%s', %f, '%s', %d)",
                        title, desc, price, type, inStock
                    );

            this.connection.prepareStatement(query).executeUpdate();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * This method updates a menu item record with new data
     * @param title item title (name)
     * @param desc item description
     * @param price item price (per piece)
     * @param type item type
     * @param consumableId id of the item that will be updated
     * @param inStock how many items of this kind are in stock
     * @return boolean indicating if item was updated successfully
     */
    public boolean updateMenuItem(String title, String desc, double price, String type, int consumableId, int inStock) {
        try {
            String query = String.format(
                    "UPDATE CONSUMABLES SET TITLE='%s', DESCRIPTION='%s', PRICE=%f, TYPE='%s', IN_STOCK=%d WHERE CONSUMABLE_ID=%d",
                    title, desc, price, type, inStock, consumableId
            );

            this.connection.prepareStatement(query).executeUpdate();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * This method fetches a menu item record from the db
     * @param consumableId id of the item to fetch
     * @return menu item record
     */
    public MenuItem getMenuItemById(int consumableId) {
        try {
            String query = String.format("SELECT * FROM CONSUMABLES WHERE CONSUMABLE_ID=%d", consumableId);

            var resultSet = this.connection.prepareStatement(query).executeQuery();

            MenuItem menuItem = null;

            while (resultSet.next()){
                menuItem = new MenuItem(
                        resultSet.getInt("CONSUMABLE_ID"),
                        resultSet.getString("TITLE"),
                        Double.parseDouble(resultSet.getString("PRICE")),
                        resultSet.getString("DESCRIPTION"),
                        resultSet.getString("TYPE"),
                        resultSet.getInt("IN_STOCK")
                );
            }

            return menuItem;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * This method renders menu item record inactive by setting its status to 'NONE'
     * Simply deleting row will cause loss of data
     * @param consumableId id of the item to fetch
     * @return boolean indicating if item was inactivated successfully
     */
    public boolean inactivateMenuItemById(int consumableId) {
        try {
            String query = String.format("UPDATE CONSUMABLES SET TYPE='NONE', IN_STOCK=0 WHERE CONSUMABLE_ID=%d", consumableId);

            var result = this.connection.prepareStatement(query).executeUpdate();

            return result == 1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
