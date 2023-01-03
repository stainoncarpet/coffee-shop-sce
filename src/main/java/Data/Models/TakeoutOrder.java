package Data.Models;

import Data.Enums.OrderStatus;

import java.util.Arrays;
import java.util.Date;

/**
 * TakeoutOrder class is a data container for data related to takeout orders (after one is made)
 */
public class TakeoutOrder {
    private int id;
    private int clientId;

    private String isClientVip = "NO";

    private TakeoutOrderItem[] takeoutOrderItems;
    private Date creationDate;
    private OrderStatus status;
    private double orderTotal;

    private int baristaId = 0;

    public TakeoutOrder(int id, int clientId, TakeoutOrderItem[] takeoutOrderItems, Date creationDate, OrderStatus status, double orderTotal) {
        this.id = id;
        this.clientId = clientId;
        this.takeoutOrderItems = takeoutOrderItems;
        this.creationDate = creationDate;
        this.status = status;
        this.orderTotal = orderTotal;
    }

    @Override
    public String toString() {
        return "TakeoutOrder{" +
                "id=" + id +
                ", clientId=" + clientId +
                ", isClientVip=" + isClientVip +
                ", takeoutOrderItems=" + Arrays.toString(takeoutOrderItems) +
                ", creationDate=" + creationDate +
                ", status=" + status +
                ", orderTotal=" + orderTotal +
                '}';
    }

    public String getIsClientVip() {
        return isClientVip;
    }

    public TakeoutOrderItem[] getTakeoutOrderItems() {
        return takeoutOrderItems;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public int getBaristaId() {
        return baristaId;
    }

    public TakeoutOrder(int id, int clientId, String isClientVip, TakeoutOrderItem[] takeoutOrderItems, Date creationDate, OrderStatus status, double orderTotal, int baristaId) {
        this.id = id;
        this.clientId = clientId;
        this.isClientVip = isClientVip;
        this.takeoutOrderItems = takeoutOrderItems;
        this.creationDate = creationDate;
        this.status = status;
        this.orderTotal = orderTotal;
        this.baristaId = baristaId;
    }

    public OrderStatus getStatus() { return status; }
    public double getOrderTotal() { return orderTotal; }
    public void setOrderTotal(double orderTotal) { this.orderTotal = orderTotal; }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public int getClientId() {
        return clientId;
    }
}
