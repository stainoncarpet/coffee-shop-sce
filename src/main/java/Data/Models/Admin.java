package Data.Models;

import Data.Enums.UserRole;

/**
 * Admin class is model for users of type admin
 *  */
public class Admin extends User {
    public Admin(String email, String password) {
        super(0,"placeholder", "placeholder", email, password, UserRole.ADMIN);
    }
}
