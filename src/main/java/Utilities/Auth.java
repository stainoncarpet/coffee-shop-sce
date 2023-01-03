package Utilities;

import io.fusionauth.jwt.Signer;
import io.fusionauth.jwt.Verifier;
import io.fusionauth.jwt.domain.JWT;
import io.fusionauth.jwt.hmac.HMACSigner;
import io.fusionauth.jwt.hmac.HMACVerifier;
import io.github.cdimascio.dotenv.Dotenv;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
* Auth class contains functionality related to the generation and processing of JSON web tokens
*  */
public class Auth {
    /**
     * This method generates a new token based on user id and email
     * @param userId user id
     * @param email user email
     * @return String token
     */
    public static String generateJWT(int userId, String email) {
        try {
            // Build an HMAC signer using a SHA-256 hash
            Signer signer = HMACSigner.newSHA256Signer(Dotenv.configure().load().get("SECRET"));

            // Build a new JWT with an issuer(iss), issued at(iat), subject(sub) and expiration(exp)
            JWT jwt = new JWT().setIssuer(Dotenv.configure().load().get("ISSUER"))
                    .setIssuedAt(ZonedDateTime.now(ZoneOffset.UTC))
                    .setSubject(email + " " + userId)
                    .setExpiration(ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(Integer.parseInt(Dotenv.configure().load().get("COOKIE_LIFESPAN"))));

            // Sign and encode the JWT to a JSON string representation
            String encodedJWT = JWT.getEncoder().encode(jwt, signer);
            return encodedJWT;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * This method decodes a token and extracts original data
     * @param encodedJWT token string for decoding
     * @return String original token payload
     * */
    public static String unwrapJWT(String encodedJWT) {
        try {
            // Build an HMC verifier using the same secret that was used to sign the JWT
            Verifier verifier = HMACVerifier.newVerifier(Dotenv.configure().load().get("SECRET"));

            if(encodedJWT != null) {
                // Verify and decode the encoded string JWT to a rich object
                JWT jwt = JWT.getDecoder().decode(encodedJWT, verifier);
                return jwt.subject;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
