package Controllers;

import spark.ModelAndView;
import spark.template.handlebars.HandlebarsTemplateEngine;

import java.util.HashMap;
import java.util.Map;

public class Home {
    public static String renderHomePage(spark.Request req, spark.Response res) {
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("jwt", (req.cookie("jwt") != null) ? req.cookie("jwt") : "");
            return new HandlebarsTemplateEngine().render(new ModelAndView(model, "/pages/home.hbs"));
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }
}
