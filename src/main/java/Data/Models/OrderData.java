package Data.Models;

import java.util.Arrays;

/**
 * OrderData class is a data container for data related to orders
*/
public class OrderData {
    private double orderTotal;
    private TakeoutOrderItem[] items;
    private int freeCoffees = 0;

    @Override
    public String toString() {
        return "OrderData{" +
                "orderTotal=" + orderTotal +
                ", items=" + Arrays.toString(items) +
                ", freeCoffees=" + freeCoffees +
                '}';
    }

    public OrderData(TakeoutOrderItem[] items, double orderTotal) {
        this.items = items;
        this.orderTotal = orderTotal;
    }

    public void setItems(TakeoutOrderItem[] items) {
        this.items = items;
    }
    public void setOrderTotal(double orderTotal) {
        this.orderTotal = orderTotal;
    }

    public double getOrderTotal() {
        return orderTotal;
    }
    public TakeoutOrderItem[] getItems() { return items; }
}
