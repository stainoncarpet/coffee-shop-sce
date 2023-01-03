package Utilities;

import Data.Models.User;

import java.util.Map;

/**
 * Filling class contains functionality related to injecting values into a hashmap for later use in the UI
 *  */
public class Filling {
    /**
     * This method inserts email and role into model
     * @param model hashmap into which data is injected
     * @param user user object that contains user data
     * @return void
     */
    public static void fillModelWithUserData(Map<String, Object> model, User user) {
        if (user != null) {
            model.put("email", user.getEmail());
            model.put("role", user.getRole());
        } else {
            model.put("email", "");
            model.put("role", "");
        }
    }

    /**
     * This method inserts data into a model
     * @param model hashmap into which data is injected
     * @param pairs of values as 1)key + 2)value
     * @return void
     */
    public static void fillModelWithPlainData(Map<String, Object> model, Object... pairs) {
        for (int i = 0; i < pairs.length - 1; i = i + 2) {
            model.put((String) pairs[i], pairs[i + 1]);
        }
    }
}
