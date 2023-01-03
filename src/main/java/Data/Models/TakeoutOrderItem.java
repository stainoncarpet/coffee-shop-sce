package Data.Models;

/**
 * TakeoutOrderItem class is a data container for data related to orders
 */
public class TakeoutOrderItem {
    private int consumableId;
    private String consumableTitle;
    private double pricePerPiece;
    private int quantity;
    private double totalSpentAmount;

    public String getConsumableTitle() {
        return consumableTitle;
    }

    public double getTotalSpentAmount() {
        return totalSpentAmount;
    }

    @Override
    public String toString() {
        return "{" +
                "consumableId=" + consumableId +
                ", consumableTitle='" + consumableTitle + '\'' +
                ", pricePerPiece='" + pricePerPiece + '\'' +
                ", quantity='" + quantity + '\'' +
                ", totalSpentAmount='" + totalSpentAmount + '\'' +
                '}';
    }

    public TakeoutOrderItem(int consumableId, String consumableTitle, double pricePerPiece, int quantity, double totalSpentAmount) {
        this.consumableId = consumableId;
        this.consumableTitle = consumableTitle;
        this.pricePerPiece = pricePerPiece;
        this.quantity = quantity;
        this.totalSpentAmount = totalSpentAmount;
    }

    public void setConsumableId(int consumableId) { this.consumableId = consumableId; }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    public int getConsumableId() { return consumableId; }
    public double getPricePerPiece() {
        return pricePerPiece;
    }
    public int getQuantity() {
        return quantity;
    }
}
