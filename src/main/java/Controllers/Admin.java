package Controllers;

import Data.Enums.ConsumableType;
import Data.Enums.TableLocation;
import Data.Services.MenuItemService;
import Data.Services.ReservationService;
import Data.Services.UserService;
import Utilities.File;
import Utilities.Filling;
import Utilities.Time;
import org.eclipse.jetty.util.UrlEncoded;
import spark.ModelAndView;
import spark.template.handlebars.HandlebarsTemplateEngine;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Admin class contains functionality related to rendering admin-facing pages
 *  */
public class Admin {
    /**
     * called on route GET /admin/item/add
     * @param req incoming request object
     * @param res object to be returned to the client
     * @param dbConnection instance of db connection that is passed further
     * @return String HTML
     */
    public static String renderAddMenuItemPage(spark.Request req, spark.Response res, java.sql.Connection dbConnection) {
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("types", Arrays.stream(ConsumableType.values()).filter(x -> x.compareTo(ConsumableType.NONE) != 0).toArray());
            return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/add-menu-item.hbs"));
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    /**
     * called on route GET /admin/item/edit/:consumableId
     * @param req incoming request object
     * @param res object to be returned to the client
     * @param dbConnection instance of db connection that is passed further
     * @return String HTML
     */
    public static String renderEditMenuItemPage(spark.Request req, spark.Response res, java.sql.Connection dbConnection) {
        try {
            Map<String, Object> model = new HashMap<>();
            var itemId = Integer.parseInt(req.params("consumableId"));

            var userService = new UserService(dbConnection);
            var admin = userService.getUserByEmail(req.attribute("userEmail"));

            var menuItemService = new MenuItemService(dbConnection);
            var item = menuItemService.getMenuItemById(itemId);

            Filling.fillModelWithUserData(model, admin);
            Filling.fillModelWithPlainData(model, "item", item, "types", Arrays.stream(ConsumableType.values()).filter(x -> x.compareTo(item.getType()) != 0 && x.compareTo(ConsumableType.NONE) != 0).toArray(), "currentType", item.getType());
            return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/edit-menu-item.hbs"));
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    /**
     * called on route POST /admin/item/edit/:consumableId
     * @param req incoming request object
     * @param res object to be returned to the client
     * @param dbConnection instance of db connection that is passed further
     * @return String HTML
     */
    public static String renderFinishEditMenuItemPage(spark.Request req, spark.Response res, java.sql.Connection dbConnection) {
        try {
            Map<String, Object> model = new HashMap<>();
            var id = Integer.parseInt(req.params("consumableId"));

            var decoded = UrlEncoded.decodeString(req.body());
            String[] orderItems = decoded.split("&");
            var title = orderItems[0].split("=")[1];
            var desc = orderItems[1].split("=")[1];
            var price = Double.parseDouble(orderItems[2].split("=")[1]);
            var type = orderItems[3].split("=")[1];
            var inStock = Integer.parseInt(orderItems[4].split("=")[1]);

            var menuService = new MenuItemService(dbConnection);
            menuService.updateMenuItem(title, desc, price, type ,id, inStock);

            var userService = new UserService(dbConnection);
            var user = userService.getUserByEmail(req.attribute("userEmail"));

            Filling.fillModelWithUserData(model, user);

            res.redirect("/profile");
            return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/profile-admin.hbs"));
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    /**
     * called on route POST /admin/item/add
     * @param req incoming request object
     * @param res object to be returned to the client
     * @param dbConnection instance of db connection that is passed further
     * @return String HTML
     */
    public static String renderAddCoffeeResultPage(spark.Request req, spark.Response res, java.sql.Connection dbConnection) {
        try {
            Map<String, Object> model = new HashMap<>();

            var clientService = new UserService(dbConnection);
            var user = clientService.getUserByEmail(req.attribute("userEmail"));
            var menuService = new MenuItemService(dbConnection);

            var decoded = UrlEncoded.decodeString(req.body());
            String[] orderItems = decoded.split("&");
            var title = orderItems[0].split("=")[1];
            var desc = orderItems[1].split("=")[1];
            var price = Double.parseDouble(orderItems[2].split("=")[1]);
            var type = orderItems[3].split("=")[1];
            var qtyInStock = Integer.parseInt(orderItems[4].split("=")[1]);
            menuService.addMenuItem(title, desc, price, type, qtyInStock);

            Filling.fillModelWithUserData(model, user);

            res.redirect("/profile");
            return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/profile-admin.hbs"));
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    /**
     * called on route POST /admin/item/upload/:consumableId
     * @param req incoming request object
     * @param res object to be returned to the client
     * @param dbConnection instance of db connection that is passed further
     * @return String HTML
     */
    public static String uploadConsumableImage(spark.Request req, spark.Response res, java.sql.Connection dbConnection) {
        Map<String, Object> model = new HashMap<>();

        try {
            var consumableId = Integer.parseInt(req.params("consumableId"));
            var menuItemService = new MenuItemService(dbConnection);
            var item = menuItemService.getMenuItemById(consumableId);

            File.uploadFile(req, System.getProperty("user.dir") + "/src/main/resources/public/" + item.getType().toString().toLowerCase() + "-" + consumableId);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            var clientService = new UserService(dbConnection);
            var user = clientService.getUserByEmail(req.attribute("userEmail"));
            Filling.fillModelWithUserData(model, user);
            res.redirect("/profile");
            return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/profile-admin.hbs"));
        }
    }

    /**
     * called on route POST /admin/item/remove/:consumableId
     * @param req incoming request object
     * @param res object to be returned to the client
     * @param dbConnection instance of db connection that is passed further
     * @return String HTML
     */
    public static String removeMenuItem(spark.Request req, spark.Response res, java.sql.Connection dbConnection) {
        try {
            Map<String, Object> model = new HashMap<>();
            var consumableId = Integer.parseInt(req.params("consumableId"));

            var userService = new UserService(dbConnection);
            var user = userService.getUserByEmail(req.attribute("userEmail"));

            Filling.fillModelWithUserData(model, user);

            var menuItemService = new MenuItemService(dbConnection);
            var item = menuItemService.getMenuItemById(consumableId);
            var isRemovedFromDb = menuItemService.inactivateMenuItemById(consumableId);

            if(isRemovedFromDb) {
                File.removeFile(System.getProperty("user.dir") + "/src/main/resources/public/" + item.getType().toString().toLowerCase() + "-" + consumableId + ".jpg");
                res.redirect("/profile");
                return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/profile-admin.hbs"));
            } else {
                var coffee = menuItemService.getMenuItemById(consumableId);
                Filling.fillModelWithPlainData(model, "coffee", coffee, "types", ConsumableType.values());
                return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/edit-menu-item.hbs"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    /**
     * called on route POST /admin/item/remove/:consumableId
     * @param req incoming request object
     * @param res object to be returned to the client
     * @param dbConnection instance of db connection that is passed further
     * @return String HTML
     */
    public static String renderAddTablePage(spark.Request req, spark.Response res, java.sql.Connection dbConnection) {
        try {
            Map<String, Object> model = new HashMap<>();
            var dates = Time.getDatesAsStrings(90);
            Filling.fillModelWithPlainData(model, "dates", dates, "types", Arrays.stream(TableLocation.values()).filter(x -> x.compareTo(TableLocation.NONE) != 0).toArray());
            return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/add-table.hbs"));
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    /**
     * called on route GET /admin/table/edit/:tableId
     * @param req incoming request object
     * @param res object to be returned to the client
     * @param dbConnection instance of db connection that is passed further
     * @return String HTML
     */
    public static String renderEditTablePage(spark.Request req, spark.Response res, java.sql.Connection dbConnection) {
        try {
            Map<String, Object> model = new HashMap<>();
            var tableId = Integer.parseInt(req.params("tableId"));

            var userService = new UserService(dbConnection);
            var user = userService.getUserByEmail(req.attribute("userEmail"));

            var tableSerice = new ReservationService(dbConnection);
            var table = tableSerice.getTableById(tableId);

            var reservationService = new ReservationService(dbConnection);
            var dates = Time.getDatesAsStrings(90, tableId, reservationService);

            Filling.fillModelWithUserData(model, user);
            Filling.fillModelWithPlainData(model, "table", table, "locations", Arrays.stream(TableLocation.values()).filter(x -> x.compareTo(TableLocation.NONE) != 0 && x.compareTo(table.getLocation()) != 0).toArray(), "currentLocation", table.getLocation(), "dates", dates);
            return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/edit-table.hbs"));
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    /**
     * called on route POST /admin/table/add
     * @param req incoming request object
     * @param res object to be returned to the client
     * @param dbConnection instance of db connection that is passed further
     * @return String HTML
     */
    public static String renderAddTableResultPage(spark.Request req, spark.Response res, java.sql.Connection dbConnection) {
        try {
            Map<String, Object> model = new HashMap<>();

            var clientService = new UserService(dbConnection);
            var user = clientService.getUserByEmail(req.attribute("userEmail"));
            var tableService = new ReservationService(dbConnection);

            var decoded = UrlEncoded.decodeString(req.body());
            String[] tableData = decoded.split("&");
            var seats = Integer.parseInt(tableData[0].split("=")[1]);
            var rate = Double.parseDouble(tableData[1].split("=")[1]);
            var location = TableLocation.valueOf(tableData[2].split("=")[1]);
            var unavailableDates = tableData[3].split("=")[1].split(", ");

            tableService.addTable(seats, rate, location, unavailableDates);

            Filling.fillModelWithUserData(model, user);

            res.redirect("/profile");
            return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/profile-admin.hbs"));
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    /**
     * called on route POST /admin/table/edit/:tableId
     * @param req incoming request object
     * @param res object to be returned to the client
     * @param dbConnection instance of db connection that is passed further
     * @return String HTML
     */
    public static String renderFinishEditTablePage(spark.Request req, spark.Response res, java.sql.Connection dbConnection) {
        try {
            Map<String, Object> model = new HashMap<>();
            var id = Integer.parseInt(req.params("tableId"));

            var decoded = UrlEncoded.decodeString(req.body());
            String[] tableData = decoded.split("&");
            var seats = Integer.parseInt(tableData[0].split("=")[1]);
            var rate = Double.parseDouble(tableData[1].split("=")[1]);
            var location = TableLocation.valueOf(tableData[2].split("=")[1]);
            var unavailableDates = tableData[3].split("=")[1].split(", ");

            var reservationService = new ReservationService(dbConnection);
            var isUpdated = reservationService.updateTable(seats, rate, location, id, unavailableDates);

            var userService = new UserService(dbConnection);
            var user = userService.getUserByEmail(req.attribute("userEmail"));

            Filling.fillModelWithUserData(model, user);

            res.redirect("/profile");
            return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/profile-admin.hbs"));
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    /**
     * called on route POST /admin/table/remove/:tableId
     * @param req incoming request object
     * @param res object to be returned to the client
     * @param dbConnection instance of db connection that is passed further
     * @return String HTML
     */
    public static String removeTable(spark.Request req, spark.Response res, java.sql.Connection dbConnection) {
        try {
            Map<String, Object> model = new HashMap<>();
            var tableId = Integer.parseInt(req.params("tableId"));

            var userService = new UserService(dbConnection);
            var user = userService.getUserByEmail(req.attribute("userEmail"));

            Filling.fillModelWithUserData(model, user);

            var tableService = new ReservationService(dbConnection);
            var isRemovedFromDb = tableService.inactivateTableById(tableId);

            if(isRemovedFromDb) {
                File.removeFile(System.getProperty("user.dir") + "/src/main/resources/public/table-" + tableId + ".jpg");

                res.redirect("/profile");
                return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/profile-admin.hbs"));
            } else {
                var table = tableService.getTableById(tableId);
                Filling.fillModelWithPlainData(model, "table", table, "locations", TableLocation.values());
                return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/edit-table.hbs"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    /**
     * called on route POST /admin/table/upload/:tableId
     * @param req incoming request object
     * @param res object to be returned to the client
     * @param dbConnection instance of db connection that is passed further
     * @return String HTML
     */
    public static String uploadTableImage(spark.Request req, spark.Response res, java.sql.Connection dbConnection) {
        Map<String, Object> model = new HashMap<>();

        try {
            var tableId = Integer.parseInt(req.params("tableId"));
            File.uploadFile(req, System.getProperty("user.dir") + "/src/main/resources/public/table-" + tableId);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            var userService = new UserService(dbConnection);
            var user = userService.getUserByEmail(req.attribute("userEmail"));
            res.redirect("/profile");
            Filling.fillModelWithUserData(model, user);
            return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/profile-admin.hbs"));
        }
    }
}