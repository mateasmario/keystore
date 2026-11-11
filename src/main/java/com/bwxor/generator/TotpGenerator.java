package com.bwxor.generator;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Locale;

/**
 * Time-based one-time passwords (RFC 6238 / RFC 4226): HMAC-SHA1, 6 digits, 30 second time step.
 */
public class TotpGenerator {
    public static final int TIME_STEP_SECONDS = 30;
    private static final int DIGITS = 6;
    private static final int MODULUS = 1_000_000;
    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    /**
     * Generates the OTP code for the given key at the given time.
     *
     * @param key          secret key bytes
     * @param epochSeconds current time in seconds since the Unix epoch
     * @return the 6-digit code, zero-padded
     * @throws GeneralSecurityException if HMAC-SHA1 is not available or the key is invalid
     */
    public String generate(byte[] key, long epochSeconds) throws GeneralSecurityException {
        long counter = epochSeconds / TIME_STEP_SECONDS;
        byte[] message = ByteBuffer.allocate(Long.BYTES).putLong(counter).array();

        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
        byte[] hash = mac.doFinal(message);

        int offset = hash[hash.length - 1] & 0x0f;
        int binary = ((hash[offset] & 0x7f) << 24)
                | ((hash[offset + 1] & 0xff) << 16)
                | ((hash[offset + 2] & 0xff) << 8)
                | (hash[offset + 3] & 0xff);

        return String.format("%0" + DIGITS + "d", binary % MODULUS);
    }

    /**
     * Returns how many seconds the code generated at the given time stays valid.
     *
     * @param epochSeconds current time in seconds since the Unix epoch
     * @return seconds left until the code changes
     */
    public int secondsRemaining(long epochSeconds) {
        return (int) (TIME_STEP_SECONDS - (epochSeconds % TIME_STEP_SECONDS));
    }

    /**
     * Decodes an RFC 4648 base32 string, as used by most authenticator secrets.
     * Spaces, hyphens and padding are ignored, and letters are case-insensitive.
     *
     * @param input base32 text
     * @return the decoded bytes
     * @throws IllegalArgumentException if the input is empty or contains characters outside the base32 alphabet
     */
    public static byte[] decodeBase32(String input) {
        String cleaned = input.replace(" ", "").replace("-", "")
                .replaceAll("=+$", "").toUpperCase(Locale.ROOT);

        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Empty base32 value");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int buffer = 0;
        int bitsLeft = 0;

        for (char c : cleaned.toCharArray()) {
            int value = BASE32_ALPHABET.indexOf(c);
            if (value < 0) {
                throw new IllegalArgumentException("Invalid base32 character: " + c);
            }

            buffer = (buffer << 5) | value;
            bitsLeft += 5;

            if (bitsLeft >= 8) {
                out.write((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }

        return out.toByteArray();
    }
}
