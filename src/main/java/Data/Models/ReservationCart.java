package Data.Models;

/**
 * ReservationCart class is a data container for data related to table reservations
 */
public class ReservationCart {
    private ReservationCartItem[] cartItems;

    public ReservationCartItem[] getCartItems() {
        return cartItems;
    }

    public ReservationCart(ReservationCartItem[] cartItems, double orderTotal) {
        this.cartItems = cartItems;
        this.orderTotal = orderTotal;
    }

    public void setCartItems(ReservationCartItem[] cartItems) {
        this.cartItems = cartItems;
    }

    public double getOrderTotal() {
        return orderTotal;
    }

    public void setOrderTotal(double orderTotal) {
        this.orderTotal = orderTotal;
    }

    private double orderTotal;
}
