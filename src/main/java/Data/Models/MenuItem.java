package Data.Models;

import Data.Enums.ConsumableType;

/**
 * MenuItem class is a data container for data related to menu items
 */
public class MenuItem {
    private int consumableId;
    private String title;
    private double price;
    private String description;
    private int quantity = 0;
    private double spentAmount = 0;
    private ConsumableType type = ConsumableType.NONE;

    public int getInStock() {
        return inStock;
    }

    public void setInStock(int inStock) {
        this.inStock = inStock;
    }

    private int inStock;

    public MenuItem(int consumableId, String title, double price, String description, int quantity, double spentAmount, int inStock) {
        this.consumableId = consumableId;
        this.title = title;
        this.price = price;
        this.description = description;
        this.quantity = quantity;
        this.spentAmount = spentAmount;
        this.inStock = inStock;
    }

    public ConsumableType getType() {
        return type;
    }

    public void setType(ConsumableType type) {
        this.type = type;
    }

    public MenuItem(int consumableId, String title, double price, String description, String type, int inStock) {
        this.consumableId = consumableId;
        this.title = title;
        this.price = price;
        this.description = description;
        this.type = ConsumableType.valueOf(type);
        this.inStock = inStock;
    }

    public String getTitle() { return title; }
    public double getPrice() { return price; }
    public String getDescription() { return description; }
    public int getQuantity() { return quantity; }
    public double getSpentAmount() { return spentAmount; }
    public int getConsumableId() { return consumableId; }

    public void setTitle(String title) { this.title = title;  }
    public void setPrice(int price) { this.price = price; }

    @Override
    public String toString() {
        return "MenuItem{" +
                "consumableId=" + consumableId +
                ", title='" + title + '\'' +
                ", price=" + price +
                ", description='" + description + '\'' +
                ", quantity=" + quantity +
                ", spentAmount=" + spentAmount +
                ", type=" + type +
                ", inStock=" + inStock +
                '}';
    }

    public void setDescription(String description) { this.description = description; }
    public void setPrice(double price) { this.price = price; }
    public void setConsumableId(int consumableId) {
        this.consumableId = consumableId;
    }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setSpentAmount(double spentAmount) { this.spentAmount = spentAmount; }
}
