package edu.bulsu.lms.security;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Salted password hashing with PBKDF2-HMAC-SHA256 from the JDK (no third-party library needed).
 *
 * <p>The stored form is {@code pbkdf2-sha256$iterations$salt$hash}, with salt and hash in Base64.
 * Because the iteration count is stored with every hash, it can be raised later without breaking
 * existing accounts.
 */
public final class PasswordHasher {

    public static final int DEFAULT_ITERATIONS = 600_000;

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "pbkdf2-sha256";
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final int MAX_ACCEPTED_ITERATIONS = 5_000_000;

    private final SecureRandom random = new SecureRandom();
    private final int iterations;

    public PasswordHasher() {
        this(DEFAULT_ITERATIONS);
    }

    /** Lower counts are only meant for fast tests. */
    public PasswordHasher(int iterations) {
        if (iterations < 1) {
            throw new IllegalArgumentException("iterations must be positive");
        }
        this.iterations = iterations;
    }

    public String hash(char[] password) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] key = derive(password, salt, iterations);
        Base64.Encoder encoder = Base64.getEncoder();
        return String.join("$", PREFIX, String.valueOf(iterations), encoder.encodeToString(salt), encoder.encodeToString(key));
    }

    /** Constant-time check of {@code password} against a value produced by {@link #hash}. */
    public boolean verify(char[] password, String stored) {
        if (stored == null) {
            return false;
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            int cost = Integer.parseInt(parts[1]);
            if (cost < 1 || cost > MAX_ACCEPTED_ITERATIONS) {
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            return MessageDigest.isEqual(expected, derive(password, salt, cost));
        } catch (IllegalArgumentException malformed) {
            return false;
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int cost) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, cost, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 is not available on this JVM", e);
        } finally {
            spec.clearPassword();
        }
    }
}
