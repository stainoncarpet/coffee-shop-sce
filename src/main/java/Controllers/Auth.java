package Controllers;

import Data.Enums.ConsumableType;
import Data.Services.MenuItemService;
import Data.Services.UserService;
import Data.Services.OrderService;
import Data.Services.ReservationService;
import Utilities.Filling;
import io.github.cdimascio.dotenv.Dotenv;
import org.eclipse.jetty.util.UrlEncoded;
import spark.ModelAndView;
import spark.template.handlebars.HandlebarsTemplateEngine;

import java.util.HashMap;
import java.util.Map;

/**
 * Auth class contains functionality related to rendering signin/signup/profile pages
 *  */
public class Auth {
    /**
     * called on route GET /signin and whenever authentication is required
     * @return String HTML
     */
    public static String renderSigninPage() {
        Map<String, Object> model = new HashMap<>();
        try {
            model.put("isSecondAttempt", "");
            return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/signin.hbs"));
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    /**
     * called on route GET /signout
     * @param req incoming request object
     * @param res object to be returned to the client
     * @return String HTML
     */
    public static String renderSignoutPage(spark.Request req, spark.Response res) {
        Map<String, Object> model = new HashMap<>();
        try {
            res.removeCookie("jwt");
            res.redirect("/signin");
            return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/signin.hbs"));
        } catch (Exception e) {
            e.printStackTrace();
            return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/signup.hbs"));
        }
    }

    /**
     * called on route GET /signup
     * @return String HTML
     */
    public static String renderSignupPage() {
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("isSecondAttempt", "");
            return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/signup.hbs"));
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    /**
     * called on route GET /profile
     * @param req incoming request object
     * @param res object to be returned to the client
     * @param dbConnection instance of db connection that is passed further
     * @return String HTML
     */
    public static String renderProfilePage(spark.Request req, spark.Response res, java.sql.Connection dbConnection) {
        Map<String, Object> model = new HashMap<>();
        try {
            var userService = new UserService(dbConnection);
            var user = userService.getUserByEmail(req.attribute("userEmail"));

            Filling.fillModelWithUserData(model, user);

            switch (user.getRole()) {
                case CLIENT:
                    var orderService = new OrderService(dbConnection);
                    var orders = orderService.getOrdersByUserId(Integer.parseInt(req.attribute("userId")));
                    var reservationService = new ReservationService(dbConnection);
                    var reservations = reservationService.getReservationsByUserId(user.getId());

                    Filling.fillModelWithPlainData(model, "orders", orders, "reservations", reservations);
                    return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/profile-client.hbs"));
                case ADMIN:
                    var menuItemService = new MenuItemService(dbConnection);
                    var items = menuItemService.getMenuItemsByType(ConsumableType.COFFEE, ConsumableType.SNACK, ConsumableType.TEA, ConsumableType.DRINK);

                    var tableService = new ReservationService(dbConnection);
                    var tables = tableService.getTables();

                    Filling.fillModelWithPlainData(model, "items", items, "tables", tables);
                    return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/profile-admin.hbs"));
                case BARISTA:
                    var orderService2 = new OrderService(dbConnection);
                    var pendingOrders = orderService2.getPendingOrders(user.getId());

                    var reservationService2 = new ReservationService(dbConnection);
                    var pendingReservations = reservationService2.getPendingReservations(Integer.parseInt(req.attribute("userId")));

                    Filling.fillModelWithPlainData(model, "orders", pendingOrders, "reservations", pendingReservations);
                    return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/profile-barista.hbs"));
                default: return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/signin.hbs"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/signin.hbs"));
        }
    }

    /**
     * called on route POST /signup
     * @param req incoming request object
     * @param res object to be returned to the client
     * @param dbConnection instance of db connection that is passed further
     * @return String HTML
     */
    public static String renderSignupResultPage(spark.Request req, spark.Response res, java.sql.Connection dbConnection) {
        var clientService = new UserService(dbConnection);

        try {
            var decoded = UrlEncoded.decodeString(req.body());
            String[] dataPairs = decoded.split("&");
            String firstName = dataPairs[0].split("=")[1];
            String lastName = dataPairs[1].split("=")[1];
            String email = dataPairs[2].split("=")[1];
            String password = dataPairs[3].split("=")[1];

            var user = clientService.createClient(email, password, firstName, lastName);
            var jwt = Utilities.Auth.generateJWT(user.getId(), email);

            if(user != null) {
                Map<String, Object> model = new HashMap<>();
                model.put("jwt", jwt);
                res.cookie("jwt", jwt, Integer.parseInt(Dotenv.configure().load().get("COOKIE_LIFESPAN")), true);
                res.redirect("/");
                return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/home.hbs"));
            } else {
                Map<String, Object> model = new HashMap<>();
                model.put("isSecondAttempt", "true");
                return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/signup.hbs"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    /**
     * called on route GET /signin
     * @param req incoming request object
     * @param res object to be returned to the client
     * @param dbConnection instance of db connection that is passed further
     * @return String HTML
     */
    public static String renderSigninResultPage(spark.Request req, spark.Response res, java.sql.Connection dbConnection) {
        try {
            var clientService = new UserService(dbConnection);
            var decoded = UrlEncoded.decodeString(req.body());
            String[] dataPairs = decoded.split("&");
            String email = dataPairs[0].split("=")[1];
            String password = dataPairs[1].split("=")[1];

            var user = clientService.verifyUserCreds(email, password);

            if(user != null) {
                var jwt = Utilities.Auth.generateJWT(user.getId(), email);
                Map<String, Object> model = new HashMap<>();
                Filling.fillModelWithUserData(model, user);
                res.cookie("jwt", jwt, Integer.parseInt(Dotenv.configure().load().get("COOKIE_LIFESPAN")), true);
                res.redirect("/profile");
                return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/profile-client.hbs"));
            } else {
                Map<String, Object> model = new HashMap<>();
                model.put("isSecondAttempt", "true");
                return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/signin.hbs"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }
}