package com.quizmaster.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Standard-library PBKDF2 password hashing for local accounts. */
public final class PasswordHash {
    public static final int ITERATIONS = 310_000;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final SecureRandom RANDOM = new SecureRandom();
    private PasswordHash() { }

    public static String[] create(char[] password) {
        validate(password);
        byte[] salt = new byte[16]; RANDOM.nextBytes(salt);
        return new String[]{Base64.getEncoder().encodeToString(salt),
                Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS)), Integer.toString(ITERATIONS)};
    }

    public static boolean verify(char[] password, String saltText, String hashText, String iterationsText) {
        if (password == null || saltText == null || hashText == null || iterationsText == null) return false;
        try {
            int iterations = Integer.parseInt(iterationsText);
            if (iterations < 100_000 || iterations > 2_000_000) return false;
            byte[] salt = Base64.getDecoder().decode(saltText);
            byte[] expected = Base64.getDecoder().decode(hashText);
            return MessageDigest.isEqual(expected, derive(password, salt, iterations));
        } catch (RuntimeException ex) { return false; }
    }

    public static void validate(char[] password) {
        if (password == null || password.length < 10) throw new IllegalArgumentException("Use a password with at least 10 characters.");
        boolean letter=false, digit=false;
        for (char c : password) { letter |= Character.isLetter(c); digit |= Character.isDigit(c); }
        if (!letter || !digit) throw new IllegalArgumentException("Password must contain at least one letter and one number.");
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, 256);
        try { return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded(); }
        catch (GeneralSecurityException ex) { throw new IllegalStateException("PBKDF2 password hashing is unavailable.", ex); }
        finally { spec.clearPassword(); }
    }
}
