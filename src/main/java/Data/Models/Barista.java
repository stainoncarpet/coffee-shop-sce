package Data.Models;

import Data.Enums.UserRole;

/**
 * Barista class is model for users of type barista
 *  */
public class Barista extends User {
    private double amountEarned;

    public Barista(int id, String fName, String lName, String email, String password, UserRole role, double amountEarned) {
        super(id, fName, lName, email, password, role);
        this.amountEarned = amountEarned;
    }

    public Barista(String email, String password) {
        super(0, "placeholder", "placeholder", email, password, UserRole.BARISTA);
    }
}
