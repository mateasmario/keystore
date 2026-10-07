package com.bwxor.service;

import com.bwxor.entity.Secret;
import com.bwxor.exception.FileServiceException;
import com.bwxor.exception.VaultServiceException;
import net.harawata.appdirs.AppDirsFactory;

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
    private static final Path SECRETS_FILE_PATH = Paths.get(USER_DATA_DIR, SECRETS_FILE_NAME);

    private static final String ASSIGN_OPERATOR = "=";
    private static final String NEWLINE = "\n";

    private final VaultService vaultService;

    public FileService(VaultService vaultService) {
        this.vaultService = vaultService;
    }

    public void createSecretsFile(String password, List<Secret> secrets) throws FileServiceException {

        StringBuilder sb = new StringBuilder();

        for (Secret s : secrets) {
            sb.append(s.key()).append(ASSIGN_OPERATOR).append(s.value()).append(NEWLINE);
        }

        byte[] encrypted;

        try {
            encrypted = vaultService.encrypt(sb.toString(), password.toCharArray());
        } catch (VaultServiceException e) {
            throw new FileServiceException(e);
        }

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

    public List<Secret> getSecrets(String password) throws FileServiceException {
        List<Secret> output = new ArrayList<>();

        if (!Files.exists(SECRETS_FILE_PATH)) {
            return List.of();
        }

        byte[] data;

        try {
            data = Files.readAllBytes(SECRETS_FILE_PATH);
        } catch (IOException e) {
            throw new FileServiceException(e);
        }

        String content;

        try {
            content = vaultService.decrypt(data, password.toCharArray());
        } catch (VaultServiceException e) {
            throw new FileServiceException(e);
        }

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

    public void deleteFile() throws FileServiceException {
        try {
            Files.deleteIfExists(SECRETS_FILE_PATH);
        } catch (IOException e) {
            throw new FileServiceException(e);
        }
    }
}
