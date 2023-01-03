import Data.Connection;

import static spark.Spark.*;

/**
 * Coffee Shop Simulator Program
 * @author Anton Voronov & Lev Kovler
 * @version 1.0
 * @since 2022-12-25
 */
public class Main {
    /**
     * This method spins up the program's core modules
     * @param args default parameter, unused
     * @return void
     */
    public static void main(String[] args) {
        // root is 'src/main/resources', so put files in 'src/main/resources/public'
        // home.hbs file is in resources/templates directory
        String projectDir = System.getProperty("user.dir");
        String staticDir = "/src/main/resources/public";
        staticFiles.externalLocation(projectDir + staticDir);

        var dbConnection = Connection.connect();

        Routes.registerRoutes(dbConnection);
    }
}