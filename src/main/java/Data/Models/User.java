package Data.Models;

import Data.Enums.UserRole;

/**
 * User class is model for generic users
 *  */
public class User {
    private int id;
    private String firstName;

    public int getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPassword() {
        return password;
    }

    private String lastName;
    private String email;

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }

    private String password;
    private UserRole role;

    public User(int id, String fName, String lName, String email, String password, UserRole role) {
        this.id = id;
        this.firstName = fName;
        this.lastName = lName;
        this.email = email;
        this.password = password;
        this.role = role;
    }
}
