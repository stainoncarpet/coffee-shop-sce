package Data.Services;

import Data.Models.Admin;
import Data.Models.Barista;
import Data.Models.Client;
import Data.Models.User;
import Data.Enums.UserRole;
import Utilities.Auth;
import Utilities.Encryption;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;

public class UserService {
    java.sql.Connection connection;

    public UserService(Connection connection) {
        this.connection = connection;
    }

    public User createClient(String email, String password, String firstName, String lastName) {
        String passwordSaltvalue = Encryption.getSaltvalue(Integer.parseInt(Dotenv.configure().load().get("SALT")));
        String encryptedPassword = Encryption.encryptPassword(password, passwordSaltvalue);

        try {
            String query = String.format(
                    "INSERT INTO USERS (FNAME, LNAME, EMAIL, PASSWORD, ROLE) " + "VALUES ('%s','%s','%s','%s','CLIENT')",
                    firstName, lastName, email, encryptedPassword
            );

            this.connection.prepareStatement(query).executeUpdate();

            return getUserByEmail(email);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public User getUserByEmail(String email) {
        try {
            String query = String.format("SELECT * FROM USERS WHERE EMAIL='%s'", email);

            var resultSet = this.connection.prepareStatement(query).executeQuery();
            User user = null;

            while(resultSet.next()) {
                var role = UserRole.valueOf(resultSet.getString("ROLE"));

                user = new User(
                        resultSet.getInt("ID"),
                        resultSet.getString("FNAME"),
                        resultSet.getString("LNAME"),
                        resultSet.getString("EMAIL"),
                        resultSet.getString("PASSWORD"),
                        role
                );
            }

            return user;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Client getClientById(String clientId) {
        try {
            String queryClientData = String.format(
                    "SELECT U.ID, U.FNAME, U.LNAME, U.EMAIL, U.PASSWORD, U.ROLE, C.IS_VIP\n" +
                            "FROM USERS as U\n" +
                            "LEFT JOIN CLIENTS as C on C.USER_ID=U.ID \n" +
                     "WHERE U.ID=%d", Integer.parseInt(clientId)
            );

            var resultSet = this.connection.prepareStatement(queryClientData).executeQuery();
            Client client = null;

            while(resultSet.next()) {
                client = new Client(
                        resultSet.getInt("ID"),
                        resultSet.getString("FNAME"),
                        resultSet.getString("LNAME"),
                        resultSet.getString("EMAIL"),
                        resultSet.getString("PASSWORD"),
                        UserRole.valueOf(resultSet.getString("ROLE")),
                        resultSet.getBoolean("IS_VIP")
                );
            }

            if(client != null && client.isVip()) {
                // count only completed orders when calculating available free coffees
                var queryTotalCoffeesBought = String.format(
                        "SELECT SUM(QUANTITY)\n" +
                                "    FROM orders_contents as c\n" +
                                "    JOIN orders_metadata as m on c.ORDER_ID = m.ORDER_ID\n" +
                                "    WHERE c.ITEM_ID IN (SELECT CONSUMABLE_ID FROM CONSUMABLES WHERE TYPE='COFFEE') AND m.CLIENT_ID=%s AND m.STATUS='COMPLETED'",
                        clientId
                );

                var coffeesBought = 0;
                var resultSet2 = this.connection.prepareStatement(queryTotalCoffeesBought).executeQuery();
                while(resultSet2.next()) {
                    coffeesBought = resultSet2.getInt("SUM(QUANTITY)");
                }

                var queryFreeCoffeesReceived = String.format(
                        "SELECT SUM(vip.QUANTITY) as qty\n" +
                                "FROM vip_coffees as vip\n" +
                                "LEFT JOIN orders_metadata as met on met.ORDER_ID = vip.ORDER_ID\n" +
                                "WHERE met.CLIENT_ID=%d", Integer.parseInt(clientId)
                );

                var freeCoffeesReceived = 0;
                var resultSet3 = this.connection.prepareStatement(queryFreeCoffeesReceived).executeQuery();
                while(resultSet3.next()) {
                    freeCoffeesReceived = resultSet3.getInt("qty");
                }

                int diff = (coffeesBought / 10) - freeCoffeesReceived;

                client.setMaxFreeCoffees(diff > 0 ? diff : 0);
            }

            return client;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public User getUserByToken(String token) {
        try {
            String userEmail = Auth.unwrapJWT(token);
            User user = getUserByEmail(userEmail.split(" ")[0]);

            return user;
        } catch (Exception e) {
            System.out.println("No valid token on user");
            return null;
        }
    }

    public Client getClientByOrderId(int orderId) {
        System.out.println("orderId " + orderId);
        try {
            String query = String.format(
                    "SELECT CLIENT_ID FROM ORDERS_METADATA WHERE ORDER_ID=%d",
                    orderId
            );
            System.out.println("query " + query);
            var resultSet = this.connection.prepareStatement(query).executeQuery();
            resultSet.next();
            var clientId = resultSet.getInt("CLIENT_ID");
            var client = getClientById(clientId + "");

            return client;
        } catch (Exception e) {
            System.out.println("No valid token on user");
            return null;
        }
    }

    public User verifyUserCreds(String email, String password) {
        try {
            String passwordSaltvalue = Encryption.getSaltvalue(Integer.parseInt(Dotenv.configure().load().get("SALT")));
            String encryptedPassword = Encryption.encryptPassword(password, passwordSaltvalue);

            String query = String.format(
                    "SELECT * FROM USERS WHERE EMAIL='%s' AND PASSWORD='%s'",
                    email, encryptedPassword
            );

            var resultSet = this.connection.prepareStatement(query).executeQuery();
            User user = null;

            while (resultSet.next()){
                user = new User(
                        resultSet.getInt("ID"),
                        resultSet.getString("FNAME"),
                        resultSet.getString("LNAME"),
                        resultSet.getString("EMAIL"),
                        resultSet.getString("PASSWORD"),
                        UserRole.valueOf(resultSet.getString("ROLE"))
                );
            }

            return user;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
