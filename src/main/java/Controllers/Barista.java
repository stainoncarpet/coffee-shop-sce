package Controllers;

import Data.Enums.OrderStatus;
import Data.Services.MenuItemService;
import Data.Services.OrderService;
import Data.Services.ReservationService;
import Data.Services.UserService;
import Data.Models.MenuItem;
import Utilities.Filling;
import Utilities.Transformation;
import org.eclipse.jetty.util.UrlEncoded;
import spark.ModelAndView;
import spark.template.handlebars.HandlebarsTemplateEngine;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Barista class contains functionality related to rendering barista-facing pages
 *  */
public class Barista {
    /**
     * called on route POST /barista/order/take/:orderId
     * @param req incoming request object
     * @param res object to be returned to the client
     * @param dbConnection instance of db connection that is passed further
     * @return String HTML
     */
    public static String renderTakeOrderResultPage(spark.Request req, spark.Response res, java.sql.Connection dbConnection) {
        try {
            Map<String, Object> model = new HashMap<>();

            var userService = new UserService(dbConnection);
            var user = userService.getUserByEmail(req.attribute("userEmail"));

            var orderId = Integer.parseInt(req.params("orderId"));
            var baristaId = Integer.parseInt(req.attribute("userId"));

            var orderService = new OrderService(dbConnection);
            var result = orderService.takeOrder(orderId, baristaId);
            var pendingOrders = orderService.getPendingOrders(baristaId);

            model.put("orders", pendingOrders);

            Filling.fillModelWithUserData(model, user);
            res.redirect("/profile");
            return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/profile-barista.hbs"));
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    /**
     * called on route POST /barista/order/charge/:orderId
     * @param req incoming request object
     * @param res object to be returned to the client
     * @param dbConnection instance of db connection that is passed further
     * @return String HTML
     */
    public static String renderTakePaymentResultPage(spark.Request req, spark.Response res, java.sql.Connection dbConnection) {
        try {
            Map<String, Object> model = new HashMap<>();

            var userService = new UserService(dbConnection);
            var user = userService.getUserByEmail(req.attribute("userEmail"));

            var orderId = Integer.parseInt(req.params("orderId"));
            var baristaId = Integer.parseInt(req.attribute("userId"));

            var orderService = new OrderService(dbConnection);
            var result = orderService.completeOrderAndTakePayment(orderId, baristaId);
            var pendingOrders = orderService.getPendingOrders(baristaId);

            model.put("orders", pendingOrders);

            Filling.fillModelWithUserData(model, user);
            res.redirect("/profile");
            return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/profile-barista.hbs"));
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    /**
     * called on route GET /barista/order/change/:orderId
     * @param req incoming request object
     * @param res object to be returned to the client
     * @param dbConnection instance of db connection that is passed further
     * @return String HTML
     */
    public static String renderChangeOrderPage(spark.Request req, spark.Response res, java.sql.Connection dbConnection) {
        try {
            Map<String, Object> model = new HashMap<>();
            var clientService = new UserService(dbConnection);
            var menuItemService = new MenuItemService(dbConnection);
            var barista = clientService.getUserByToken(req.cookie("jwt"));
            var orderId = req.params(":orderId");
            var allMenuItems = menuItemService.getAllMenuItems();
            var orderMenuData = menuItemService.getMenuItemsByOrderId(Integer.parseInt(orderId));
            var orderData = orderMenuData.getMenuItems();
            double amountDue = 0;

            for (MenuItem item : allMenuItems) {
                for (int i = 0; i < orderData.size(); i++) {
                    var orderItem = orderData.get(i);
                    if(item.getConsumableId() == orderItem.getConsumableId()) {
                        item.setQuantity(orderItem.getQuantity());
                        item.setSpentAmount(orderItem.getSpentAmount());
                        item.setInStock(orderItem.getInStock() + orderItem.getQuantity());
                        amountDue += orderItem.getQuantity() * orderItem.getPrice();
                    }
                }
            }

            if(orderMenuData.getOrderStatus() != OrderStatus.COMPLETED) { model.put("status", orderMenuData.getOrderStatus()); }
            if (allMenuItems.size() > 0) { model.put("amountDue", amountDue); }
            var client = clientService.getClientByOrderId(Integer.parseInt(req.params(":orderId")));
            var orderService = new OrderService(dbConnection);
            var alreadyOrderedCoffees = orderService.getOrderedFreeCoffeesByOrderId(Integer.parseInt(req.params(":orderId")));
            // getMaxFreeCoffees returns the difference between the current total bought and free
            // to recreate the initial cart configuration
            Filling.fillModelWithUserData(model, barista);
            Filling.fillModelWithPlainData(model, "orderId", orderId, "menuItems", allMenuItems, "maxFreeCoffees", client.getMaxFreeCoffees() + alreadyOrderedCoffees, "orderedFreeCoffees", alreadyOrderedCoffees);
            return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/change-order.hbs"));
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }
    }

    /**
     * called on route POST /barista/order/change/:orderId
     * @param req incoming request object
     * @param res object to be returned to the client
     * @param dbConnection instance of db connection that is passed further
     * @return String HTML
     */
    public static String renderChangeOrderResultPage(spark.Request req, spark.Response res, java.sql.Connection dbConnection) {
        try {
            Map<String, Object> model = new HashMap<>();

            var decoded = UrlEncoded.decodeString(req.body());
            String[] orderItems = decoded.split("&");
            var withoutFreeCoffees = Arrays.copyOfRange(orderItems, 0,orderItems.length - 1);
            var freeCoffeesQty = Integer.parseInt(orderItems[orderItems.length - 1].split("=")[1]);
            var orderData = Transformation.extractOrderData(withoutFreeCoffees, dbConnection);

            var orderService = new OrderService(dbConnection);

            orderService.changeOrder(
                    Integer.parseInt(req.params("orderId")),
                    orderData.getItems(),
                    Integer.parseInt(req.attribute("userId")),
                    freeCoffeesQty
            );
            var userService = new UserService(dbConnection);
            var user = userService.getUserByEmail(req.attribute("userEmail"));

            Filling.fillModelWithUserData(model, user);
            res.redirect("/profile");
            return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/profile-barista.hbs"));
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }
    }

    /**
     * called on route POST /barista/reservation/record/:reservationId
     * @param req incoming request object
     * @param res object to be returned to the client
     * @param dbConnection instance of db connection that is passed further
     * @return String HTML
     */
    public static String renderRecordReservationResultPage(spark.Request req, spark.Response res, java.sql.Connection dbConnection) {
        try {
            Map<String, Object> model = new HashMap<>();
            var resId = Integer.parseInt(req.params(":reservationId"));
            var baristaId = Integer.parseInt(req.attribute("userId"));

            var reservationService = new ReservationService(dbConnection);
            reservationService.recordReservation(resId, baristaId);

            var userService = new UserService(dbConnection);
            var user = userService.getUserByEmail(req.attribute("userEmail"));

            Filling.fillModelWithUserData(model, user);
            res.redirect("/profile");
            return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/profile-barista.hbs"));
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }
    }

    /**
     * called on route POST /barista/reservation/charge/:reservationId
     * @param req incoming request object
     * @param res object to be returned to the client
     * @param dbConnection instance of db connection that is passed further
     * @return String HTML
     */
    public static String renderChargeReservationResultPage(spark.Request req, spark.Response res, java.sql.Connection dbConnection) {
        try {
            Map<String, Object> model = new HashMap<>();
            var resId = Integer.parseInt(req.params(":reservationId"));
            var baristaId = Integer.parseInt(req.attribute("userId"));

            var reservationService = new ReservationService(dbConnection);
            reservationService.completeReservation(resId, baristaId);

            var userService = new UserService(dbConnection);
            var user = userService.getUserByEmail(req.attribute("userEmail"));

            Filling.fillModelWithUserData(model, user);
            res.redirect("/profile");
            return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/profile-barista.hbs"));
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }
    }
}
