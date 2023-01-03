package Utilities;

import static org.junit.jupiter.api.Assertions.*;

class PaymentTest {
    @org.junit.jupiter.api.Test
    void verifyPaymentDetails() {
        assertTrue(Payment.verifyPaymentDetails(
                "fullName=ANTON VORONOV",
                "cardNumber=1234 1234 1234 1234",
                "expiration=12/22",
                "cvv=123"
        ));

        assertFalse(Payment.verifyPaymentDetails(
                "fullName=ANTON VORONOV",
                "cardNumber=1234 1234 1234 1234a",
                "expiration=12/22",
                "cvv=123"
        ));

        assertFalse(Payment.verifyPaymentDetails(
                "fullName=ANTON VORONOV1",
                "cardNumber=1234 1234 1234 1234a",
                "expiration=12/22",
                "cvv=123"
        ));

        assertFalse(Payment.verifyPaymentDetails(
                "fullName=ANTON VORONOV",
                "cardNumber=1234 1234 1234 1234a",
                "expiration=12/22a",
                "cvv=123"
        ));

        assertFalse(Payment.verifyPaymentDetails(
                "fullName=ANTON VORONOV",
                "cardNumber=1234 1234 1234 1234a",
                "expiration=12/22",
                "cvv=123a"
        ));

        assertFalse(Payment.verifyPaymentDetails(
                "fullName=ANTON",
                "cardNumber=1234 1234 1234 1234",
                "expiration=12/22",
                "cvv=123"
        ));
    }
}