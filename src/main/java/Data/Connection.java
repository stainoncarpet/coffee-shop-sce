package Data;

import io.github.cdimascio.dotenv.Dotenv;
import java.sql.DriverManager;

public class Connection {
    private static java.sql.Connection conn = null;
    public static java.sql.Connection connect() {
        var dotenv = Dotenv.configure().load();

        if(conn != null) {
            return conn;
        }

        try {
            conn = DriverManager.getConnection(dotenv.get("DB_URL"), dotenv.get("DB_USERNAME"), dotenv.get("DB_PASSWORD"));

            if(conn != null){
                System.out.println("Successfully connected to the database.");
            } else{
                System.out.println("Failed to connect to the database.");
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        return conn;
    }
}
