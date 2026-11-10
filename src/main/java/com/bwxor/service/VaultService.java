package com.bwxor.service;

import com.bwxor.exception.VaultServiceException;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

    record ProcessedByteBuffer(byte[] salt, byte[] nonce, byte[] ciphertext){}

    /**
     * Encrypts the plaintext, deriving a key from the given password
     * @param plaintext Plaintext to encrypt
     * @param password Password to be used for deriving the key
     * @return Encrypted text
     */
    public byte[] encrypt(String plaintext, char[] password) throws VaultServiceException {
        return encrypt(plaintext, password, null);
    }

    /**
     * Encrypts the plaintext using directly the key
     * @param plaintext Plaintext to encrypt
     * @param key Key to be used for encryption
     * @return Encrypted text
     */
    public byte[] encrypt(String plaintext, SecretKey key) throws VaultServiceException {
        byte[] salt = new byte[SALT_LEN];
        byte[] nonce = new byte[NONCE_LEN];
        RNG.nextBytes(salt);
        RNG.nextBytes(nonce);

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
     * Encrypts the plaintext and creates a file containing the key
     * @param plaintext Plaintext to encrypt
     * @param password Password to be used for deriving the key
     * @param keyPath Path where the key file should be created, if not null
     * @return Encrypted text
     */
    public byte[] encrypt(String plaintext, char[] password, Path keyPath) throws VaultServiceException {
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

        // Write to file, if needed
        if (keyPath != null) {
            try {
                Files.deleteIfExists(keyPath);
                Files.createFile(keyPath);
                Files.write(keyPath, key.getEncoded());
            } catch (IOException e) {
                throw new VaultServiceException(e);
            }
        }

        return encrypt(plaintext, key);
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

    public String decrypt(byte[] data, Path keyPath) throws VaultServiceException {
        checkIfValidKeystoreContent(data);

        ProcessedByteBuffer buf = getProcessedByteBuffer(data);

        SecretKey key;

        try {
            byte[] bytes = Files.readAllBytes(keyPath);
            key = new SecretKeySpec(bytes, "AES");
        } catch (IOException e) {
            throw new VaultServiceException(e);
        }

        return doDecrypt(key, buf);
    }

    public String decrypt(byte[] data, char[] password) throws VaultServiceException {
        checkIfValidKeystoreContent(data);

        ProcessedByteBuffer buf = getProcessedByteBuffer(data);

        SecretKey key = deriveKey(password, buf.salt);

        return doDecrypt(key, buf);
    }

    private void checkIfValidKeystoreContent(byte[] data) {
        int headerLen = MAGIC.length + SALT_LEN + NONCE_LEN;
        if (data.length < headerLen + 16) {                  // 16 = GCM tag size
            throw new IllegalArgumentException("File is too short to be a keystore");
        }
    }

    private ProcessedByteBuffer getProcessedByteBuffer(byte[] data) {
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

        return new ProcessedByteBuffer(salt, nonce, ciphertext);
    }

    private String doDecrypt(SecretKey key, ProcessedByteBuffer buf) throws VaultServiceException {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance("AES/GCM/NoPadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new VaultServiceException(e);
        }

        try {
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, buf.nonce));
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new VaultServiceException(e);
        }

        byte[] plaintext;
        try {
            plaintext = cipher.doFinal(buf.ciphertext);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new VaultServiceException(e);
        }

        return new String(plaintext, StandardCharsets.UTF_8);
    }
}