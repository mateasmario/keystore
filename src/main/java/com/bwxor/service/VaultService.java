package com.bwxor.service;

import com.bwxor.exception.VaultServiceException;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

public class VaultService {
    private static final byte[] MAGIC = "MYKS".getBytes(StandardCharsets.US_ASCII);
    private static final int SALT_LEN = 16;
    private static final int NONCE_LEN = 12;
    private static final int ITERATIONS = 600_000;
    private static final SecureRandom RNG = new SecureRandom();

    public byte[] encrypt(String plaintext, char[] password) throws VaultServiceException {
        byte[] salt = new byte[SALT_LEN];
        byte[] nonce = new byte[NONCE_LEN];
        RNG.nextBytes(salt);
        RNG.nextBytes(nonce);

        SecretKey key;

        try {
            key = deriveKey(password, salt);
        } catch (Exception e) {
            throw new VaultServiceException(e);
        }

        Cipher cipher;

        try {
            cipher = Cipher.getInstance("AES/GCM/NoPadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new VaultServiceException(e);
        }

        try {
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new VaultServiceException(e);
        }

        byte[] ciphertext;

        try {
            ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new VaultServiceException(e);
        }

        return ByteBuffer.allocate(MAGIC.length + SALT_LEN + NONCE_LEN + ciphertext.length)
                .put(MAGIC)
                .put(salt)
                .put(nonce)
                .put(ciphertext)
                .array();
    }

    /**
     * Creates a key hash to be used for encryption
     */
    private SecretKey deriveKey(char[] password, byte[] salt) throws VaultServiceException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, 256);
        try {
            byte[] keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
            SecretKey key = new SecretKeySpec(keyBytes, "AES");

            // Wipe key from memory. No longer needed
            Arrays.fill(keyBytes, (byte) 0);
            return key;
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new VaultServiceException(e);
        } finally {
            spec.clearPassword();
        }
    }

    public String decrypt(byte[] data, char[] password) throws VaultServiceException {
        int headerLen = MAGIC.length + SALT_LEN + NONCE_LEN;
        if (data.length < headerLen + 16) {                  // 16 = GCM tag size
            throw new IllegalArgumentException("File is too short to be a keystore");
        }

        ByteBuffer buf = ByteBuffer.wrap(data);

        byte[] magic = new byte[MAGIC.length];
        buf.get(magic);
        if (!Arrays.equals(magic, MAGIC)) {
            throw new IllegalArgumentException("Not a keystore file");
        }

        byte[] salt = new byte[SALT_LEN];
        buf.get(salt);

        byte[] nonce = new byte[NONCE_LEN];
        buf.get(nonce);

        byte[] ciphertext = new byte[buf.remaining()];
        buf.get(ciphertext);

        SecretKey key = deriveKey(password, salt);

        Cipher cipher;
        try {
            cipher = Cipher.getInstance("AES/GCM/NoPadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new VaultServiceException(e);
        }

        try {
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, nonce));
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new VaultServiceException(e);
        }

        byte[] plaintext;       // throws AEADBadTagException on wrong password
        try {
            plaintext = cipher.doFinal(ciphertext);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new VaultServiceException(e);
        }

        return new String(plaintext, StandardCharsets.UTF_8);
    }
}