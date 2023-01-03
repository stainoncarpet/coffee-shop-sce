package Utilities;

/**
 * Payment class contains functionality related to validating user-submitted payment details
 *  */
public class Payment {
    /**
     * This method checks if submitted payment details are valid
     * @param fullName user's full name
     * @param cardNumber user's card number
     * @param expirationMonth user's card number expiration month
     * @param cvv user's card cvv number
     * @return boolean indicating if the details are valid
     */
    public static boolean verifyPaymentDetails(String fullName, String cardNumber, String expirationMonth, String cvv) {
        boolean isValidFullName = verifyFullName(fullName.split("=")[1]);
        boolean isValidCardNumber = verifyCardNumber(cardNumber.split("=")[1]);
        boolean isValidExpirationMonth = verifyExpirationMonth(expirationMonth.split("=")[1]);
        boolean isValidCVV = verifyCVV(cvv.split("=")[1]);

        if(!isValidCVV || !isValidExpirationMonth || !isValidCardNumber || !isValidFullName) {
            return false;
        }

        return true;
    }

    /**
     * This method checks if the submitted full name is valid
     * @param fullName user's full name
     * @return boolean indicating if the full name is valid
     */
    private static boolean verifyFullName(String fullName) {
        try {
            var names = fullName.split(" ");

            if (names.length != 2) { return false; }
            if (names[0].length() < 2 || names[1].length() < 2) { return false; }
            if (!names[0].matches("[a-zA-Z]+") || !names[1].matches("[a-zA-Z]+")) { return false; }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * This method checks if the submitted card number is valid
     * @param cardNumber user's card number
     * @return boolean indicating if the card number is valid
     */
    private static boolean verifyCardNumber(String cardNumber) {
        try {
            var pureCardNumber = cardNumber.replaceAll(" ", "");
            if(pureCardNumber.length() != 16) { return false; }
            if(!pureCardNumber.matches("[0-9]+")) { return false; }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * This method checks if the submitted card is expired
     * @param expirationMonth user's card expiration month
     * @return boolean indicating if the card is still valid
     */
    private static boolean verifyExpirationMonth(String expirationMonth) {
        try {
            var monthYear = expirationMonth.split("/");

            if (expirationMonth.charAt(2)  != '/') { return false; }

            if(!monthYear[0].matches("[0-9]{2}")) { return false; }
            if(!monthYear[1].matches("[0-9]{2}")) { return false; }

            int cardMonth = Integer.parseInt(monthYear[0]);
            int cardYear = Integer.parseInt(monthYear[1]);

            java.sql.Date date = new java.sql.Date(System.currentTimeMillis());
            int currentMonth = Integer.parseInt(date.toString().substring(5, 7));
            int currentYear = Integer.parseInt(date.toString().substring(2, 4));

            if(cardYear < currentYear) { return false; }
            if (cardMonth < currentMonth) { return false; }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * This method checks if the submitted card cvv is valid
     * @param cvv user's card cvv
     * @return boolean indicating if the cvv is valid
     */
    private static boolean verifyCVV(String cvv) {
        try {
            if (cvv.length() < 3 || cvv.length() > 4) { return false; }
            if (!cvv.matches("[0-9]+")) { return false; }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
