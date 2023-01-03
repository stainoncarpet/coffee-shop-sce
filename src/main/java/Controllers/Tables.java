package Controllers;

import Data.Services.UserService;
import Data.Services.ReservationService;
import Utilities.Filling;
import Utilities.Payment;
import Utilities.Transformation;
import org.eclipse.jetty.util.UrlEncoded;
import spark.ModelAndView;
import spark.template.handlebars.HandlebarsTemplateEngine;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Tables class contains functionality related to rendering table reservation-related pages
 *  */
public class Tables {
    /**
     * called on routes GET & POST /reservation/new
     * @param req incoming request object
     * @param res object to be returned to the client
     * @param dbConnection db connection object
     * */
    public static String renderReservationPage(spark.Request req, spark.Response res, java.sql.Connection dbConnection) {
        try {
            Map<String, Object> model = new HashMap<>();
            var clientService = new UserService(dbConnection);
            var reservationItemServiceService = new ReservationService(dbConnection);
            var user = clientService.getUserByToken(req.cookie("jwt"));

            var formMap = Transformation.parseReservationData(req.body());
            var reservationDate = formMap.getReservationDateString();

            var reservationItems = reservationItemServiceService.getAvailableTablesByDate(reservationDate);

            if (reservationItems != null && reservationItems.size() > 0) {
                model.put("reservationItems", reservationItems);
            }

            Filling.fillModelWithUserData(model, user);
            Filling.fillModelWithPlainData(model, "minDate", LocalDate.now().toString(), "reservationDate", reservationDate == null ? LocalDate.now().toString() : reservationDate);

            return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/reservation.hbs"));
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    /**
     * called on route POST /reservation/confirm/new
     * @param req incoming request object
     * @param res object to be returned to the client
     * @param dbConnection db connection object
     * */
    public static String renderStartReservationPage(spark.Request req, spark.Response res, java.sql.Connection dbConnection) {
        try {
            Map<String, Object> model = new HashMap<>();
            var reservationData = Transformation.parseReservationData(req.body());
            var reservationService = new ReservationService(dbConnection);

            var clientService = new UserService(dbConnection);
            var user = clientService.getUserByToken(req.cookie("jwt"));

            var cart = reservationService.getReservationCartItems(reservationData);

            Filling.fillModelWithUserData(model, user);
            Filling.fillModelWithPlainData(model, "reservationDate", reservationData.getReservationDateStringMonth(), "reservationDateRaw", reservationData.getReservationDateString(), "orderTotal", cart.getOrderTotal(), "cartItems", cart.getCartItems());

            return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/reserveout.hbs"));
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }
    /**
     * called on route GET /reservation/confirm/new
     * @param req incoming request object
     * @param res object to be returned to the client
     * @param dbConnection db connection object
     * */
    public static String renderReservationCheckoutPage(spark.Request req, spark.Response res, java.sql.Connection dbConnection) {
        try {
            var clientService = new UserService(dbConnection);
            var user = clientService.getUserByToken(req.cookie("jwt"));
            Map<String, Object> model = new HashMap<>();

            var decoded = UrlEncoded.decodeString(req.body());
            String[] allDataPairs = decoded.split("&");
            String[] onlyPaymentDetails = Arrays.copyOfRange(allDataPairs, 0,4);
            boolean areFieldsValid = Payment.verifyPaymentDetails(onlyPaymentDetails[0], onlyPaymentDetails[1], onlyPaymentDetails[2], onlyPaymentDetails[3]);

            var reservationData = Transformation.parseReservationData(
                    Arrays.copyOfRange(allDataPairs, 4, allDataPairs.length)
            );
            var reservationService = new ReservationService(dbConnection);
            var cart = reservationService.getReservationCartItems(reservationData);

            Filling.fillModelWithUserData(model, user);
            Filling.fillModelWithPlainData(model, "reservationDate", reservationData.getReservationDateStringMonth(), "reservationDateRaw", reservationData.getReservationDateString(), "orderTotal", cart.getOrderTotal(), "cartItems", cart.getCartItems());

            if(!areFieldsValid) {
                Filling.fillModelWithPlainData(model,"toastTitle", "Payment Failed", "toastSubtitle", "Error", "toastBody", "Some of the entered info is incorrect", "isSecondAttempt", "true");
                return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/reserveout.hbs"));
            }

            var reservationItemService = new ReservationService(dbConnection);
            reservationItemService.reserveTables(reservationData, req.attribute("userId"));
            Filling.fillModelWithPlainData(model, "toastTitle", "Payment Successful", "toastSubtitle", "Reservation In Done", "toastBody", "You will be redirected to the home page", "isSuccess", "true");
            return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/reserveout.hbs"));
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }
}
