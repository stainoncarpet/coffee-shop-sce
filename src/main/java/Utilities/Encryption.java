package Utilities;

import io.github.cdimascio.dotenv.Dotenv;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Encryption class contains functionality related to user password hashing for its subsequent storage in the db
 *  */
public class Encryption {
    /* Declaration of variables */
    private static final int iterations = 10000;
    private static final int keylength = 256;

    /**
     * This method generates a salt value that is later used during hashing
     * @param length how many times a value is compounded by itself
     * @return String final salt value
     * */
    public static String getSaltvalue(int length) {
        StringBuilder finalval = new StringBuilder(length);
        for (int i = 0; i < length; i++) { finalval.append(Dotenv.configure().load().get("SALT_APPENDIX")); }
        return new String(finalval);
    }

    /**
     * This method generates a hash value
     * @param password original user password as char codes
     * @param salt previously generated additional value involved in hashing
     * @return byte[] password hash
     * */
    public static byte[] hash(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keylength);
        Arrays.fill(password, Character.MIN_VALUE);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            return null;
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * This method encrypts the password using the original password and salt value.
     * @param password original user password
     * @param salt previously generated additional value involved in hashing
     * @return byte[] password hash
     * */
    public static String encryptPassword(String password, String salt) {
        try {
            String finalval = null;
            byte[] encryptedPassword = hash(password.toCharArray(), salt.getBytes());
            finalval = Base64.getEncoder().encodeToString(encryptedPassword);
            return finalval;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}