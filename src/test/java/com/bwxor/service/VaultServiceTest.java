package com.bwxor.service;

import com.bwxor.exception.VaultServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class VaultServiceTest {
    private static final char[] RIGHT_PASSWORD = "correct".toCharArray();
    private static final char[] WRONG_PASSWORD = "wrong".toCharArray();

    @TempDir
    Path tempDir;

    private final VaultService vault = new VaultService();

    @Test
    void passwordRoundTrip() throws VaultServiceException {
        byte[] encrypted = vault.encrypt("héllo ünï", RIGHT_PASSWORD);

        assertEquals("héllo ünï", vault.decrypt(encrypted, RIGHT_PASSWORD));
    }

    @Test
    void wrongPasswordThrows() throws VaultServiceException {
        byte[] encrypted = vault.encrypt("secret", RIGHT_PASSWORD);

        assertThrows(VaultServiceException.class, () -> vault.decrypt(encrypted, WRONG_PASSWORD));
    }

    @Test
    void sameInputGivesDifferentOutput() throws VaultServiceException {
        byte[] a = vault.encrypt("secret", RIGHT_PASSWORD);
        byte[] b = vault.encrypt("secret", RIGHT_PASSWORD);

        assertFalse(java.util.Arrays.equals(a, b));
    }

    @Test
    void ciphertextDoesNotContainPlaintext() throws VaultServiceException {
        byte[] encrypted = vault.encrypt("very-visible-secret", RIGHT_PASSWORD);

        assertFalse(new String(encrypted, StandardCharsets.ISO_8859_1).contains("very-visible-secret"));
    }

    @Test
    void tamperedDataThrows() throws VaultServiceException {
        byte[] encrypted = vault.encrypt("secret", RIGHT_PASSWORD);
        encrypted[encrypted.length - 1] ^= 1;   // modify last byte (GCM)

        assertThrows(VaultServiceException.class, () -> vault.decrypt(encrypted, RIGHT_PASSWORD));
    }

    @Test
    void dataWithoutMagicIsRejected() throws VaultServiceException {
        byte[] encrypted = vault.encrypt("secret", RIGHT_PASSWORD);
        encrypted[0] ^= 1;

        assertThrows(VaultServiceException.class, () -> vault.decrypt(encrypted, RIGHT_PASSWORD));
    }

    @Test
    void encryptWithKeyPathCreatesKeyFileUsableForDecrypt() throws Exception {
        Path keyFile = tempDir.resolve("key");

        byte[] encrypted = vault.encrypt("secret", RIGHT_PASSWORD, keyFile);

        assertTrue(Files.exists(keyFile));
        assertEquals(32, Files.size(keyFile));
        assertEquals("secret", vault.decrypt(encrypted, keyFile));
    }

    @Test
    void existingKeyFileRoundTrip() throws Exception {
        Path keyFile = tempDir.resolve("key");

        // create the keystore file
        vault.encrypt("first", RIGHT_PASSWORD, keyFile);

        // should overwrite the keystore file
        byte[] encrypted = vault.encrypt("second", keyFile);

        assertEquals("second", vault.decrypt(encrypted, keyFile));
    }

    @Test
    void emptyKeyFileThrows() throws Exception {
        Path keyFile = tempDir.resolve("key");
        byte[] encrypted = vault.encrypt("secret", RIGHT_PASSWORD);
        Files.write(keyFile, new byte[0]);

        assertThrows(VaultServiceException.class, () -> vault.decrypt(encrypted, keyFile));
    }

    @Test
    void missingKeyFileThrows() throws VaultServiceException {
        byte[] encrypted = vault.encrypt("secret", RIGHT_PASSWORD);

        assertThrows(VaultServiceException.class, () -> vault.decrypt(encrypted, tempDir.resolve("nope")));
    }
}