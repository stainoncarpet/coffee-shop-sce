package Data.Models;

import Data.Enums.UserRole;

/**
 * Client class is model for users of type client
 *  */
public class Client extends User {
    private boolean isVip = false;

    public int getMaxFreeCoffees() {
        return maxFreeCoffees;
    }

    public void setMaxFreeCoffees(int maxFreeCoffees) {
        this.maxFreeCoffees = maxFreeCoffees;
    }

    private int maxFreeCoffees = 0;

    public boolean isVip() {
        return isVip;
    }

    public Client(int id, String fName, String lName, String email, String password, UserRole role, boolean isVip) {
        super(id, fName, lName, email, password, role);
        this.isVip = isVip;
    }
}
