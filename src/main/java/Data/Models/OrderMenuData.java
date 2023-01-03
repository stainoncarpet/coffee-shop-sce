package Data.Models;

import Data.Enums.OrderStatus;

import java.util.ArrayList;

/**
 * OrderMenuData class is a data container for data related to menu items
 */
public class OrderMenuData {
    private ArrayList<MenuItem> menuItems;

    public ArrayList<MenuItem> getMenuItems() {
        return menuItems;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    private OrderStatus orderStatus;

    public OrderMenuData(ArrayList<MenuItem> menuItems, OrderStatus orderStatus) {
        this.menuItems = menuItems;
        this.orderStatus = orderStatus;
    }
}
