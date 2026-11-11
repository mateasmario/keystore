package com.bwxor.service;

import com.bwxor.exception.VaultServiceException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
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
    public static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int TAG_BITS = 128;

    record ProcessedByteBuffer(byte[] salt, byte[] nonce, byte[] ciphertext){}

    /**
     * Encrypts the plaintext with a key derived from the password.
     *
     * @param plaintext text to encrypt
     * @param password  password the key is derived from
     * @return the encrypted data (header and ciphertext)
     * @throws VaultServiceException if the encryption fails
     */
    public byte[] encrypt(String plaintext, char[] password) throws VaultServiceException {
        return encrypt(plaintext, password, null);
    }

    /**
     * Encrypts the plaintext with the key stored in an existing key file.
     *
     * @param plaintext text to encrypt
     * @param keyPath   path of the key file
     * @return the encrypted data (header and ciphertext)
     * @throws VaultServiceException if the key file cannot be read or is empty, or if the encryption fails
     */
    public byte[] encrypt(String plaintext, Path keyPath) throws VaultServiceException {
        return doEncrypt(plaintext, readKey(keyPath), null);
    }

    /**
     * Encrypts the plaintext with a key derived from the password and saves that key to a file.
     * An existing file at {@code keyPath} is replaced. If {@code keyPath} is null, no file is created.
     *
     * @param plaintext text to encrypt
     * @param password  password the key is derived from
     * @param keyPath   where to save the key file, or null to skip it
     * @return the encrypted data (header and ciphertext)
     * @throws VaultServiceException if the key derivation, the key file writing or the encryption fails
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

        return doEncrypt(plaintext, key, salt);
    }

    /**
     * Encrypts the plaintext with the given key and builds the final data layout
     * (magic, salt, nonce, ciphertext). A new random nonce is used on every call.
     *
     * @param plaintext text to encrypt
     * @param key       AES key used for encryption
     * @param salt      salt to store in the header, needed to derive the key again from a password
     *                  when decrypting. If null, a random one is generated.
     * @return the encrypted data (header and ciphertext)
     * @throws VaultServiceException if the encryption fails
     */
    private byte[] doEncrypt(String plaintext, SecretKey key, byte[] salt) throws VaultServiceException {
        if (salt == null) {
            salt = new byte[SALT_LEN];
            RNG.nextBytes(salt);
        }

        byte[] nonce = new byte[NONCE_LEN];
        RNG.nextBytes(nonce);

        byte[] ciphertext = crypt(Cipher.ENCRYPT_MODE, key, nonce, plaintext.getBytes(StandardCharsets.UTF_8));

        return ByteBuffer.allocate(MAGIC.length + SALT_LEN + NONCE_LEN + ciphertext.length)
                .put(MAGIC)
                .put(salt)
                .put(nonce)
                .put(ciphertext)
                .array();
    }

    /**
     * Decrypts data with the key stored in a key file.
     *
     * @param data    complete encrypted data (header and ciphertext)
     * @param keyPath path of the key file
     * @return the decrypted text
     * @throws VaultServiceException    if the key file cannot be read or is empty, or if the key is wrong
     *                                  or the data has been modified
     * @throws IllegalArgumentException if the data is too short or is not a keystore
     */
    public String decrypt(byte[] data, Path keyPath) throws VaultServiceException {
        checkIfValidKeystoreContent(data);

        ProcessedByteBuffer buf = getProcessedByteBuffer(data);

        return doDecrypt(readKey(keyPath), buf);
    }

    /**
     * Decrypts data with a password. The key is derived again from the password and the salt
     * stored in the data header.
     *
     * @param data     complete encrypted data (header and ciphertext)
     * @param password password used when the data was encrypted
     * @return the decrypted text
     * @throws VaultServiceException    if the password is wrong or the data has been modified
     * @throws IllegalArgumentException if the data is too short or is not a keystore
     */
    public String decrypt(byte[] data, char[] password) throws VaultServiceException {
        checkIfValidKeystoreContent(data);

        ProcessedByteBuffer buf = getProcessedByteBuffer(data);
        SecretKey key = deriveKey(password, buf.salt);

        return doDecrypt(key, buf);
    }

    /**
     * Decrypts the ciphertext with the given key and converts the result to text.
     * Final step of both {@code decrypt} methods.
     *
     * @param key AES key
     * @param buf parsed data, holding the nonce and the ciphertext
     * @return the decrypted text (UTF-8)
     * @throws VaultServiceException if the key is wrong or the data has been modified
     */
    private String doDecrypt(SecretKey key, ProcessedByteBuffer buf) throws VaultServiceException {
        byte[] plaintext = crypt(Cipher.DECRYPT_MODE, key, buf.nonce(), buf.ciphertext());
        return new String(plaintext, StandardCharsets.UTF_8);
    }

    /**
     * Derives a 256-bit AES key from the password, using PBKDF2 with HMAC-SHA256.
     * The same password and salt always give the same key.
     *
     * @param password password to derive the key from. The array is not cleared, that is up to the caller.
     * @param salt     random value that makes the key different even for identical passwords
     * @return the derived AES key
     * @throws VaultServiceException if the key derivation fails
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

    /**
     * Reads an AES key from a key file. The file holds the raw key bytes.
     *
     * @param keyPath path of the key file
     * @return the key
     * @throws VaultServiceException if the file cannot be read or is empty
     */
    private SecretKey readKey(Path keyPath) throws VaultServiceException {
        try {
            byte[] bytes = Files.readAllBytes(keyPath);
            if (bytes.length == 0) {
                throw new VaultServiceException("Key is empty.");
            }
            return new SecretKeySpec(bytes, "AES");
        } catch (IOException e) {
            throw new VaultServiceException(e);
        }
    }


    /**
     * Runs AES-GCM in the given mode. Used for both encryption and decryption.
     * When decrypting, a wrong key or modified data makes the authentication check fail.
     *
     * @param mode  {@link Cipher#ENCRYPT_MODE} or {@link Cipher#DECRYPT_MODE}
     * @param key   AES key
     * @param nonce nonce (IV) used for encryption
     * @param input plaintext bytes when encrypting, ciphertext with the tag when decrypting
     * @return ciphertext when encrypting, plaintext bytes when decrypting
     * @throws VaultServiceException if the cipher fails, including a failed authentication check
     */
    private byte[] crypt(int mode, SecretKey key, byte[] nonce, byte[] input) throws VaultServiceException {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(mode, key, new GCMParameterSpec(TAG_BITS, nonce));
            return cipher.doFinal(input);
        } catch (GeneralSecurityException e) {
            throw new VaultServiceException(e);
        }
    }

    /**
     * Checks that the data is long enough to be a keystore: the header (magic, salt, nonce)
     * plus at least the GCM tag. The encrypted content itself can be empty.
     *
     * @param data data to check
     * @throws VaultServiceException if the data is too short
     */
    private void checkIfValidKeystoreContent(byte[] data) throws VaultServiceException {
        int headerLen = MAGIC.length + SALT_LEN + NONCE_LEN;
        if (data.length < headerLen + 16) {                  // 16 = GCM tag size
            throw new VaultServiceException("File is too short to be a keystore");
        }
    }

    /**
     * Splits the encrypted data into salt, nonce and ciphertext, following the data layout.
     * The data length must already be checked with {@link #checkIfValidKeystoreContent(byte[])}.
     *
     * @param data complete encrypted data
     * @return the salt, the nonce and the ciphertext (with the GCM tag)
     * @throws VaultServiceException if the data does not start with the expected magic bytes
     */
    private ProcessedByteBuffer getProcessedByteBuffer(byte[] data) throws VaultServiceException {
        ByteBuffer buf = ByteBuffer.wrap(data);

        byte[] magic = new byte[MAGIC.length];
        buf.get(magic);
        if (!Arrays.equals(magic, MAGIC)) {
            throw new VaultServiceException("Not a keystore file");
        }

        byte[] salt = new byte[SALT_LEN];
        buf.get(salt);

        byte[] nonce = new byte[NONCE_LEN];
        buf.get(nonce);

        byte[] ciphertext = new byte[buf.remaining()];
        buf.get(ciphertext);

        return new ProcessedByteBuffer(salt, nonce, ciphertext);
    }

}