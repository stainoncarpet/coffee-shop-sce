package Controllers;

import Data.Services.UserService;
import Data.Services.MenuItemService;
import Data.Services.OrderService;
import Data.Models.MenuItem;
import Data.Enums.OrderStatus;
import Utilities.Filling;
import Utilities.Payment;
import Utilities.Transformation;
import io.github.cdimascio.dotenv.Dotenv;
import org.eclipse.jetty.util.UrlEncoded;
import spark.ModelAndView;
import spark.template.handlebars.HandlebarsTemplateEngine;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Menu class contains functionality related to rendering client-facing pages
 *  */
public class Menu {
    /**
     * called on route GET /menu/new
     * @param req incoming request object
     * @param res object to be returned to the client
     * @param dbConnection instance of db connection that is passed further
     * @return String HTML
     */
    public static String renderMenuPage(spark.Request req, spark.Response res, java.sql.Connection dbConnection) {
        try {
            Map<String, Object> model = new HashMap<>();
            var clientService = new UserService(dbConnection);
            var consumableService = new MenuItemService(dbConnection);
            var user = clientService.getUserByToken(req.cookie("jwt"));
            var consumables = consumableService.getAllMenuItems(
                    req.queryParams("sort") == null
                            ? ""
                            : req.queryParams("sort"),
                    model
            );

            Filling.fillModelWithUserData(model, user);

            if (consumables != null && consumables.size() > 0) {
                model.put("menuItems", consumables);
            } else {
                model.put("menuItems", new MenuItem[0]);
            }
            model.put("topLimit", Dotenv.configure().load().get("TOP_LIMIT"));
            return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/menu.hbs"));
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }
    }

    /**
     * called on route POST /takeout/new
     * @param req incoming request object
     * @param res object to be returned to the client
     * @param dbConnection instance of db connection that is passed further
     * @return String HTML
     */
    public static String renderConfirmTakeoutPage(spark.Request req, spark.Response res, java.sql.Connection dbConnection) {
        try {
            Map<String, Object> model = new HashMap<>();

            var decoded = UrlEncoded.decodeString(req.body());
            String[] orderItems = decoded.split("&");
            var orderData = Transformation.extractOrderData(orderItems, dbConnection);

            var clientService = new UserService(dbConnection);
            var client = clientService.getClientById(req.attribute("userId"));

            Filling.fillModelWithUserData(model, client);
            System.out.println("client " + client);
            Filling.fillModelWithPlainData(model, "items", orderData.getItems(), "quantity", (orderData.getItems().length), "orderTotal", orderData.getOrderTotal(), "maxFreeCoffees", client.getMaxFreeCoffees() > 5 ? 5 : client.getMaxFreeCoffees());

            return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/takeout.hbs"));
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }
    }

    /**
     * called on route GET /menu/:takeoutId
     * @param req incoming request object
     * @param res object to be returned to the client
     * @param dbConnection instance of db connection that is passed further
     * @return String HTML
     */
    public static String renderReMenuPage(spark.Request req, spark.Response res, java.sql.Connection dbConnection) {
        try {
            Map<String, Object> model = new HashMap<>();
            var clientService = new UserService(dbConnection);
            var menuItemService = new MenuItemService(dbConnection);
            var user = clientService.getUserByToken(req.cookie("jwt"));

            var allMenuItems = menuItemService.getAllMenuItems(req.queryParams("sort") == null ? "" : req.queryParams("sort"), model);
            var orderMenuData = menuItemService.getMenuItemsByOrderId(Integer.parseInt(req.params(":takeoutId")));
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

            if(orderMenuData.getOrderStatus() == OrderStatus.COMPLETED) {
                model.put("status", "");
            } else {
                model.put("status", orderMenuData.getOrderStatus());
            }

            model.put("takeoutId", req.params(":takeoutId"));

            Filling.fillModelWithUserData(model, user);

            if (allMenuItems.size() > 0) {
                model.put("menuItems", allMenuItems);
                model.put("amountDue", amountDue);
            } else {
                model.put("menuItems", new MenuItem[0]);
            }
            model.put("topLimit", Dotenv.configure().load().get("TOP_LIMIT"));
            return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/menu2.hbs"));
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }
    }

    /**
     * called on route POST /takeout/confirm/new
     * @param req incoming request object
     * @param res object to be returned to the client
     * @param dbConnection instance of db connection that is passed further
     * @return String HTML
     */
    public static String renderFinishTakeoutOrderPage(spark.Request req, spark.Response res, java.sql.Connection dbConnection) {
        try {
            Map<String, Object> model = new HashMap<>();
            var decoded = UrlEncoded.decodeString(req.body());
            String[] takeoutData = decoded.split("&");
            String[] onlyPaymentDetails = Arrays.copyOfRange(takeoutData, 0,4);
            boolean areFieldsValid = Payment.verifyPaymentDetails(onlyPaymentDetails[0], onlyPaymentDetails[1], onlyPaymentDetails[2], onlyPaymentDetails[3]);

            // cut off payment-related fields
            String[] onlyItems = Arrays.copyOfRange(takeoutData, 4,takeoutData.length - 1);
            var orderData = Transformation.extractOrderData(onlyItems, dbConnection);

            var clientService = new UserService(dbConnection);
            var client = clientService.getClientById(req.attribute("userId"));

            Filling.fillModelWithUserData(model, client);
            Filling.fillModelWithPlainData(model, "items", orderData.getItems(), "quantity", (orderData.getItems().length), "orderTotal", orderData.getOrderTotal(), "maxFreeCoffees", client.getMaxFreeCoffees() > 5 ? 5 : client.getMaxFreeCoffees());

            if(!areFieldsValid) {
                Filling.fillModelWithPlainData(model, "toastTitle", "Payment Failed", "toastSubtitle", "Error", "toastBody", "Some of the entered info is incorrect", "isSecondAttempt", "true");
                return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/takeout.hbs"));
            } else {
                var orderService = new OrderService(dbConnection);
                var selectedFreeCoffeesCount = Integer.parseInt(takeoutData[takeoutData.length - 1].split("=")[1]);
                orderService.createOrder(Integer.parseInt(req.attribute("userId")), orderData.getItems(), orderData.getOrderTotal(), selectedFreeCoffeesCount);

                Filling.fillModelWithPlainData(model, "toastTitle", "Payment Successful", "toastSubtitle", "Order In Progress", "toastBody", "You will be redirected to the home page", "isSuccess", "true");
                return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/takeout.hbs"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }
    }

    /**
     * called on route POST /takeout/:takeoutId
     * @param req incoming request object
     * @param res object to be returned to the client
     * @param dbConnection instance of db connection that is passed further
     * @return String HTML
     */
    public static String renderConfirmEditTakeoutPage(spark.Request req, spark.Response res, java.sql.Connection dbConnection) {
        try {
            Map<String, Object> model = new HashMap<>();

            var decoded = UrlEncoded.decodeString(req.body());
            String[] orderItems = decoded.split("&");
            var orderData = Transformation.extractOrderData(orderItems, dbConnection);

            var clientService = new UserService(dbConnection);
            var client = clientService.getClientById(req.attribute("userId"));
            var orderService = new OrderService(dbConnection);

            var alreadyOrderedCoffees = orderService.getOrderedFreeCoffeesByOrderId(Integer.parseInt(req.params(":takeoutId")));

            // getMaxFreeCoffees returns the difference between the current total bought and free
            // to recreate the initial cart configuration
            Filling.fillModelWithUserData(model, client);
            Filling.fillModelWithPlainData(model, "items", orderData.getItems(), "quantity", (orderData.getItems().length), "orderTotal", orderData.getOrderTotal(), "takeoutId", req.params(":takeoutId"), "maxFreeCoffees", (client.getMaxFreeCoffees() + alreadyOrderedCoffees), "orderedFreeCoffees", alreadyOrderedCoffees);
            return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/takeout2.hbs"));
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }
    }

    /**
     * called on route POST /takeout/confirm/:takeoutId
     * @param req incoming request object
     * @param res object to be returned to the client
     * @param dbConnection instance of db connection that is passed further
     * @return String HTML
     */
    public static String renderFinishEditTakeoutPage(spark.Request req, spark.Response res, java.sql.Connection dbConnection) {
        try {
            Map<String, Object> model = new HashMap<>();
            var decoded = UrlEncoded.decodeString(req.body());

            String[] takeoutData = decoded.split("&");
            String[] onlyPaymentDetails = Arrays.copyOfRange(takeoutData, 0,4);
            boolean areFieldsValid = Payment.verifyPaymentDetails(onlyPaymentDetails[0], onlyPaymentDetails[1], onlyPaymentDetails[2], onlyPaymentDetails[3]);

            // cut off payment-related fields
            String[] onlyItems = Arrays.copyOfRange(takeoutData, 4,takeoutData.length - 1);
            var orderData = Transformation.extractOrderData(onlyItems, dbConnection);
            var clientService = new UserService(dbConnection);
            var client = clientService.getClientById(req.attribute("userId"));

            var orderService = new OrderService(dbConnection);
            var alreadyOrderedCoffees = orderService.getOrderedFreeCoffeesByOrderId(Integer.parseInt(req.params(":takeoutId")));

            Filling.fillModelWithUserData(model, client);

            if(!areFieldsValid) {
                Filling.fillModelWithPlainData(model, "toastTitle", "Payment Failed", "toastSubtitle", "Error", "toastBody", "Some of the entered info is incorrect", "isSecondAttempt", "true");
                return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/takeout2.hbs"));
            }

            var t1 = takeoutData[takeoutData.length - 1];
            var selectedFreeCoffeesCount = Integer.parseInt(t1.split("=")[1]);

            orderService.editOrderById(
                    Integer.parseInt(req.attribute("userId")),
                    Integer.parseInt(req.params(":takeoutId")),
                    orderData.getItems(),
                    selectedFreeCoffeesCount
            );

            // getMaxFreeCoffees returns the difference between the current total bought and free
            // to recreate the initial cart configuration
            Filling.fillModelWithPlainData(model, "items", orderData.getItems(), "quantity", (orderData.getItems().length), "orderTotal", orderData.getOrderTotal(), "takeoutId", req.params(":takeoutId"), "toastTitle", "Payment Successful", "toastSubtitle", "Order In Progress", "toastBody", "You will be redirected to the home page", "isSuccess", "true", "maxFreeCoffees", client.getMaxFreeCoffees() + alreadyOrderedCoffees, "orderedFreeCoffees", alreadyOrderedCoffees);
            return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/takeout2.hbs"));
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }
    }
}
