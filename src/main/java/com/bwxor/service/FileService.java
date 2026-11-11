package com.bwxor.service;

import com.bwxor.entity.Secret;
import com.bwxor.exception.FileServiceException;
import com.bwxor.exception.VaultServiceException;
import com.bwxor.type.Mode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileService {
    private static final String SECRETS_FILE_NAME = "secrets";
    private static final String MODE_FILE_NAME = "mode";

    private static final String ASSIGN_OPERATOR = "=";
    private static final String NEWLINE = "\n";

    private final VaultService vaultService;
    private final Path directoryPath;
    private final Path secretsFilePath;
    private final Path modeFilePath;

    public FileService(VaultService vaultService, Path directoryPath) {
        this.vaultService = vaultService;
        this.directoryPath = directoryPath;
        secretsFilePath = Paths.get(directoryPath.toString(), SECRETS_FILE_NAME);
        modeFilePath = Paths.get(directoryPath.toString(), MODE_FILE_NAME);
    }

    /**
     * Saves the secrets, encrypted, in the secrets file, replacing its previous content.
     * The key comes from the key file at {@code keyPath}. If {@code password} is not null,
     * the key is first derived from it and saved to that file.
     *
     * @param password password to derive a new key from, or null to use the existing key file
     * @param keyPath  path of the key file
     * @param secrets  secrets to save
     * @throws FileServiceException if the key file or the secrets file cannot be read or written, or the encryption fails
     */
    public void createSecretsFile(String password, Path keyPath, List<Secret> secrets) throws FileServiceException {
        StringBuilder sb = new StringBuilder();

        for (Secret s : secrets) {
            sb.append(s.key()).append(ASSIGN_OPERATOR).append(s.value()).append(NEWLINE);
        }

        try {
            if (password == null) {
                doCreateSecretsFile(vaultService.encrypt(sb.toString(), keyPath));
            } else {
                doCreateSecretsFile(vaultService.encrypt(sb.toString(), password.toCharArray(), keyPath));
            }
        } catch (VaultServiceException e) {
            throw new FileServiceException(e);
        }
    }

    /**
     * Saves the secrets, encrypted with a key derived from the password, in the secrets file,
     * replacing its previous content.
     *
     * @param password password to derive the key from
     * @param secrets  secrets to save
     * @throws FileServiceException if the encryption or the writing of the secrets file fails
     */
    public void createSecretsFile(String password, List<Secret> secrets) throws FileServiceException {
        StringBuilder sb = new StringBuilder();

        for (Secret s : secrets) {
            sb.append(s.key()).append(ASSIGN_OPERATOR).append(s.value()).append(NEWLINE);
        }

        try {
            doCreateSecretsFile(vaultService.encrypt(sb.toString(), password.toCharArray()));
        } catch (VaultServiceException e) {
            throw new FileServiceException(e);
        }
    }

    /**
     * Writes the encrypted data to the secrets file, creating the directory if needed and
     * replacing any existing file.
     *
     * @param encrypted encrypted data to write
     * @throws FileServiceException if the directory or the file cannot be created or written
     */
    private void doCreateSecretsFile(byte[] encrypted) throws FileServiceException {
        try {
            Files.deleteIfExists(secretsFilePath);
        } catch (IOException e) {
            throw new FileServiceException(e);
        }

        try {
            Files.createDirectories(directoryPath);
        } catch (IOException e) {
            throw new FileServiceException(e);
        }

        try {
            Files.createFile(secretsFilePath);
        } catch (IOException e) {
            throw new FileServiceException(e);
        }

        try {
            Files.write(secretsFilePath, encrypted);
        } catch (IOException e) {
            throw new FileServiceException(e);
        }
    }

    /**
     * Reads the secrets from the secrets file, decrypting them with the key stored in a key file.
     *
     * @param keyPath path of the key file
     * @return the secrets, or an empty list if the secrets file does not exist
     * @throws FileServiceException if the secrets file or the key file cannot be read, or the key is wrong
     */
    public List<Secret> getSecrets(Path keyPath) throws FileServiceException {
        if (Files.notExists(secretsFilePath)) {
            return List.of();
        }

        try {
            return collectSecrets(vaultService.decrypt(readSecretsEncrypted(), keyPath));
        } catch (VaultServiceException e) {
            throw new FileServiceException(e);
        }
    }

    /**
     * Reads the secrets from the secrets file, decrypting them with a key derived from the password.
     *
     * @param password password the secrets were encrypted with
     * @return the secrets, or an empty list if the secrets file does not exist
     * @throws FileServiceException if the secrets file cannot be read, or the password is wrong
     */
    public List<Secret> getSecrets(String password) throws FileServiceException {
        if (Files.notExists(secretsFilePath)) {
            return List.of();
        }

        try {
            return collectSecrets(vaultService.decrypt(readSecretsEncrypted(), password.toCharArray()));
        } catch (VaultServiceException e) {
            throw new FileServiceException(e);
        }
    }

    /**
     * Parses the decrypted content into secrets. Each line holds one secret, as {@code key=value},
     * split at the first {@code =}. Empty lines and lines without {@code =} are skipped.
     *
     * @param content decrypted content of the secrets file
     * @return the parsed secrets
     */
    private List<Secret> collectSecrets(String content) {
        List<Secret> output = new ArrayList<>();

        String[] keys = content.split(NEWLINE);

        for (String key : keys) {
            if (key.isEmpty()) {
                continue;
            }

            String[] keySplit = key.split(ASSIGN_OPERATOR, 2);

            if (keySplit.length < 2) {
                continue;
            }

            output.add(new Secret(keySplit[0], keySplit[1]));
        }

        return output;
    }

    /**
     * Reads the raw encrypted content of the secrets file.
     *
     * @return the encrypted bytes
     * @throws FileServiceException if the file cannot be read
     */
    private byte[] readSecretsEncrypted() throws FileServiceException {
        try {
            return Files.readAllBytes(secretsFilePath);
        } catch (IOException e) {
            throw new FileServiceException(e);
        }
    }

    /**
     * Deletes the secrets file, if it exists.
     *
     * @throws FileServiceException if the file cannot be deleted
     */
    public void deleteSecretsFile() throws FileServiceException {
        try {
            Files.deleteIfExists(secretsFilePath);
        } catch (IOException e) {
            throw new FileServiceException(e);
        }
    }

    /**
     * Reads the mode (keyboard or file) saved in the mode file.
     *
     * @return the saved mode, or {@link Mode#UNKNOWN} if the content is not a valid mode
     * @throws FileServiceException if the mode file cannot be read, for example if it does not exist yet
     */
    public Mode readModeFromFile() throws FileServiceException {
        String mode;

        try {
            mode = Files.readString(modeFilePath);
        } catch (IOException e) {
            throw new FileServiceException(e);
        }

        return Mode.parse(mode);
    }

    /**
     * Saves the mode in the mode file, replacing any existing one.
     *
     * @param mode mode to save
     * @throws FileServiceException if the file cannot be written
     */
    public void createModeFile(Mode mode) throws FileServiceException {
        try {
            Files.deleteIfExists(modeFilePath);
            Files.createFile(modeFilePath);
            Files.writeString(modeFilePath, mode.getValue());
        } catch (IOException e) {
            throw new FileServiceException(e);
        }
    }

    /**
     * Deletes the mode file, if it exists.
     *
     * @throws FileServiceException if the file cannot be deleted
     */
    public void deleteModeFile() throws FileServiceException {
        try {
            Files.deleteIfExists(modeFilePath);
        } catch (IOException e) {
            throw new FileServiceException(e);
        }
    }
}
