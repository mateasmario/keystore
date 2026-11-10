package com.bwxor.service;

import com.bwxor.entity.Secret;
import com.bwxor.exception.FileServiceException;
import com.bwxor.exception.VaultServiceException;
import com.bwxor.type.Mode;
import net.harawata.appdirs.AppDirsFactory;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileService {
    private static final  String APP_NAME = "keystore";
    private static final  String APP_AUTHOR = "bwxor";
    private static final  String USER_DATA_DIR = AppDirsFactory.getInstance().getUserDataDir(APP_NAME, null, APP_AUTHOR);
    private static final  String SECRETS_FILE_NAME = "secrets";
    private static final  String MODE_FILE_NAME = "mode";
    private static final Path SECRETS_FILE_PATH = Paths.get(USER_DATA_DIR, SECRETS_FILE_NAME);
    private static final Path MODE_FILE_PATH = Paths.get(USER_DATA_DIR, MODE_FILE_NAME);

    private static final String ASSIGN_OPERATOR = "=";
    private static final String NEWLINE = "\n";

    private final VaultService vaultService;

    public FileService(VaultService vaultService) {
        this.vaultService = vaultService;
    }

    public void createSecretsFile(Path keyPath, List<Secret> secrets) throws FileServiceException {
        StringBuilder sb = new StringBuilder();

        for (Secret s : secrets) {
            sb.append(s.key()).append(ASSIGN_OPERATOR).append(s.value()).append(NEWLINE);
        }

        try {
            SecretKey key = new SecretKeySpec(Files.readAllBytes(keyPath), "AES");
            doCreateSecretsFile(vaultService.encrypt(sb.toString(), key));
        } catch (VaultServiceException e) {
            throw new FileServiceException(e);
        } catch (IOException e) {
            throw new FileServiceException(e);
        }
    }

    public void createSecretsFile(String password, Path keyPath, List<Secret> secrets) throws FileServiceException {

        StringBuilder sb = new StringBuilder();

        for (Secret s : secrets) {
            sb.append(s.key()).append(ASSIGN_OPERATOR).append(s.value()).append(NEWLINE);
        }

        try {
            doCreateSecretsFile(vaultService.encrypt(sb.toString(), password.toCharArray(), keyPath));
        } catch (VaultServiceException e) {
            throw new FileServiceException(e);
        }
    }

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

    private void doCreateSecretsFile(byte[] encrypted) throws FileServiceException {
        try {
            Files.deleteIfExists(SECRETS_FILE_PATH);
        } catch (IOException e) {
            throw new FileServiceException(e);
        }

        try {
            Files.createDirectories(Paths.get(USER_DATA_DIR));
        } catch (IOException e) {
            throw new FileServiceException(e);
        }

        try {
            Files.createFile(SECRETS_FILE_PATH);
        } catch (IOException e) {
            throw new FileServiceException(e);
        }

        try {
            Files.write(SECRETS_FILE_PATH, encrypted);
        } catch (IOException e) {
            throw new FileServiceException(e);
        }
    }

    public List<Secret> getSecrets(Path keyPath) throws FileServiceException {
        try {
            return collectSecrets(vaultService.decrypt(readSecretsEncrypted(), keyPath));
        } catch (VaultServiceException e) {
            throw new FileServiceException(e);
        }
    }

    public List<Secret> getSecrets(String password) throws FileServiceException {
        try {
            return collectSecrets(vaultService.decrypt(readSecretsEncrypted(), password.toCharArray()));
        } catch (VaultServiceException e) {
            throw new FileServiceException(e);
        }
    }

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

    private byte[] readSecretsEncrypted() throws FileServiceException {
        if (!Files.exists(SECRETS_FILE_PATH)) {
            return new byte[0];
        }

        byte[] data;

        try {
            return Files.readAllBytes(SECRETS_FILE_PATH);
        } catch (IOException e) {
            throw new FileServiceException(e);
        }
    }

    public void deleteSecretsFile() throws FileServiceException {
        try {
            Files.deleteIfExists(SECRETS_FILE_PATH);
        } catch (IOException e) {
            throw new FileServiceException(e);
        }
    }

    public Mode readModeFromFile() throws FileServiceException {
        String mode;

        try {
            mode = Files.readString(MODE_FILE_PATH);
        } catch (IOException e) {
            throw new FileServiceException(e);
        }

        return Mode.parse(mode);
    }

    public void writeModeToFile(Mode mode) throws FileServiceException {
        try {
            Files.deleteIfExists(MODE_FILE_PATH);
            Files.createFile(MODE_FILE_PATH);
            Files.writeString(MODE_FILE_PATH, mode.getValue());
        } catch (IOException e) {
            throw new FileServiceException(e);
        }
    }

    public void deleteModeFile() throws FileServiceException {
        try {
            Files.deleteIfExists(MODE_FILE_PATH);
        } catch (IOException e) {
            throw new FileServiceException(e);
        }
    }
}
