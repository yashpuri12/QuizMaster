package com.quizmaster.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Arrays;
import java.util.Properties;

/** Local teacher credential setup and PBKDF2 password verification. */
public final class TeacherCredentialStore {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 310_000;
    private static final int KEY_BITS = 256;
    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final Path file;

    public TeacherCredentialStore(Path dataDirectory) { this.file = dataDirectory.resolve("teacher.properties"); }

    public synchronized boolean isConfigured() { return Files.isRegularFile(file); }

    public synchronized void initialize(char[] password, char[] confirmation) throws IOException {
        if (password == null || confirmation == null || !Arrays.equals(password, confirmation)) {
            throw new IllegalArgumentException("The passwords do not match.");
        }
        validatePassword(password);
        if (isConfigured()) throw new IllegalStateException("A teacher password is already configured.");
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] hash = derive(password, salt, ITERATIONS);
        Properties properties = new Properties();
        properties.setProperty("algorithm", ALGORITHM);
        properties.setProperty("iterations", Integer.toString(ITERATIONS));
        properties.setProperty("salt", Base64.getEncoder().encodeToString(salt));
        properties.setProperty("hash", Base64.getEncoder().encodeToString(hash));
        save(properties);
    }

    public synchronized boolean verify(char[] password) throws IOException {
        if (!isConfigured() || password == null) return false;
        Properties properties = load();
        try {
            if (!ALGORITHM.equals(properties.getProperty("algorithm"))) return false;
            int iterations = Integer.parseInt(properties.getProperty("iterations"));
            if (iterations < 100_000 || iterations > 2_000_000) return false;
            byte[] salt = Base64.getDecoder().decode(properties.getProperty("salt"));
            byte[] expected = Base64.getDecoder().decode(properties.getProperty("hash"));
            byte[] actual = derive(password, salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private void validatePassword(char[] password) {
        if (password.length < MIN_PASSWORD_LENGTH) throw new IllegalArgumentException("Use at least 12 characters for the teacher password.");
        boolean letter = false, digit = false;
        for (char ch : password) { letter |= Character.isLetter(ch); digit |= Character.isDigit(ch); }
        if (!letter || !digit) throw new IllegalArgumentException("Use a password containing at least one letter and one number.");
    }

    private Properties load() throws IOException {
        Properties properties = new Properties();
        try (var input = Files.newInputStream(file)) { properties.load(input); }
        return properties;
    }

    private void save(Properties properties) throws IOException {
        Files.createDirectories(file.getParent());
        Path temporary = file.resolveSibling("teacher.properties.tmp");
        try (var output = Files.newOutputStream(temporary)) { properties.store(output, "QuizMaster local teacher credentials. Contains a salted password hash only."); }
        try { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (IOException unsupportedAtomicMove) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try { return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded(); }
        catch (GeneralSecurityException ex) { throw new IllegalStateException("PBKDF2 password hashing is unavailable.", ex); }
        finally { spec.clearPassword(); }
    }

}
