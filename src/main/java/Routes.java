import Controllers.*;

import static spark.Spark.*;
/**
 * Routes class is responsible for http routing
 */
public class Routes {
    /**
     * This method registers all routes
     * @param dbConnection instance of db connection that is passed further
     * @return void
     */
    public static void registerRoutes(java.sql.Connection dbConnection) {
        notFound("<html><body><h1>Page not found</h1></body></html>");

        before("/profile", Routes::protectRoute);
        before("/takeout/*", Routes::protectRoute);
        before("/reservation/*", Routes::protectRoute);
        before("/admin/*", Routes::protectRoute);
        before("/barista/*", Routes::protectRoute);

        get("/", Home::renderHomePage);
        get("/signup", (req, res) -> Auth.renderSignupPage());
        get("/signin", (req, res) -> Auth.renderSigninPage());
        get("/signout", Auth::renderSignoutPage);
        get("/profile", (req, res) -> Auth.renderProfilePage(req, res, dbConnection));
        get("/menu/new", (req, res) -> Menu.renderMenuPage(req, res, dbConnection));
        get("/menu/:takeoutId", (req, res) -> Menu.renderReMenuPage(req, res, dbConnection));
        get("/reservation/new", (req, res) -> Tables.renderReservationPage(req, res, dbConnection));
        get("/reservation/confirm/new", (req, res) -> Tables.renderReservationCheckoutPage(req, res, dbConnection));

        get("/admin/item/add", (req, res) -> Admin.renderAddMenuItemPage(req, res, dbConnection));
        get("/admin/item/edit/:consumableId", (req, res) -> Admin.renderEditMenuItemPage(req, res, dbConnection));
        get("/admin/table/add", (req, res) -> Admin.renderAddTablePage(req, res, dbConnection));
        get("/admin/table/edit/:tableId", (req, res) -> Admin.renderEditTablePage(req, res, dbConnection));

        get("/barista/order/change/:orderId", (req, res) -> Barista.renderChangeOrderPage(req, res, dbConnection));

        post("/signup", (req, res) -> Auth.renderSignupResultPage(req, res, dbConnection));
        post("/signin", (req, res) -> Auth.renderSigninResultPage(req, res, dbConnection));
        post("/takeout/new", (req, res) -> Menu.renderConfirmTakeoutPage(req, res, dbConnection));
        post("/takeout/:takeoutId", (req, res) -> Menu.renderConfirmEditTakeoutPage(req, res, dbConnection));
        post("/takeout/confirm/new", (req, res) -> Menu.renderFinishTakeoutOrderPage(req, res, dbConnection));
        post("/takeout/confirm/:takeoutId", (req, res) -> Menu.renderFinishEditTakeoutPage(req, res, dbConnection));
        post("/reservation/new", (req, res) -> Tables.renderReservationPage(req, res, dbConnection));
        post("/reservation/confirm/new", (req, res) -> Tables.renderStartReservationPage(req, res, dbConnection));
        post("/reservation/finish/new", (req, res) -> Tables.renderReservationCheckoutPage(req, res, dbConnection));

        post("/admin/item/add", (req, res) -> Admin.renderAddCoffeeResultPage(req, res, dbConnection));
        post("/admin/item/edit/:consumableId", (req, res) -> Admin.renderFinishEditMenuItemPage(req, res, dbConnection));
        post("/admin/item/upload/:consumableId", (req, res) -> Admin.uploadConsumableImage(req, res, dbConnection));
        post("/admin/item/remove/:consumableId", (req, res) -> Admin.removeMenuItem(req, res, dbConnection));
        post("/admin/table/add", (req, res) -> Admin.renderAddTableResultPage(req, res, dbConnection));
        post("/admin/table/edit/:tableId", (req, res) -> Admin.renderFinishEditTablePage(req, res, dbConnection));
        post("/admin/table/upload/:tableId", (req, res) -> Admin.uploadTableImage(req, res, dbConnection));
        post("/admin/table/remove/:tableId", (req, res) -> Admin.removeTable(req, res, dbConnection));

        post("/barista/order/take/:orderId", (req, res) -> Barista.renderTakeOrderResultPage(req, res, dbConnection));
        post("/barista/order/charge/:orderId", (req, res) -> Barista.renderTakePaymentResultPage(req, res, dbConnection));
        post("/barista/order/change/:orderId", (req, res) -> Barista.renderChangeOrderResultPage(req, res, dbConnection));
        post("/barista/reservation/record/:reservationId", (req, res) -> Barista.renderRecordReservationResultPage(req, res, dbConnection));
        post("/barista/reservation/charge/:reservationId", (req, res) -> Barista.renderChargeReservationResultPage(req, res, dbConnection));
    }

    /**
     * This method restricts routes to registered users
     * @param req incoming request object
     * @param res object to be returned to the client
     * @return void
     */
    private static void protectRoute(spark.Request req, spark.Response res) {
        var userData = Utilities.Auth.unwrapJWT(req.cookie("jwt"));
        if (userData == null) {
            res.redirect("/signin");
            halt(401, Auth.renderSigninPage());
        } else {
            req.attribute("userEmail", userData.split(" ")[0]);
            req.attribute("userId", userData.split(" ")[1]);
        }
    }
}
