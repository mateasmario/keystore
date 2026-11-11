package com.bwxor.command.impl;

import com.bwxor.command.Command;
import com.bwxor.entity.Secret;
import com.bwxor.exception.FileServiceException;
import com.bwxor.service.FileService;
import com.bwxor.type.Mode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;

public class UpdateCommand implements Command {
    private static final String USAGE_MESSAGE = """
            Usage: keystore update <key> <newvalue>""";

    private final FileService fileService;

    public UpdateCommand(FileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public void execute(List<String> args, Consumer<String> displayFunction) {
        if (args.size() != 2) {
            displayFunction.accept(USAGE_MESSAGE);
            return;
        }

        String key = args.get(0);
        String newValue = args.get(1);

        Mode mode;
        try {
            mode = fileService.readModeFromFile();
        } catch (FileServiceException e) {
            displayFunction.accept("Could not read mode from the file. Delete the secrets file and re-create it.");
            return;
        }

        if (mode.equals(Mode.UNKNOWN)) {
            displayFunction.accept("Could not read mode from the file. Delete the secrets file and re-create it.");
            return;
        }

        List<Secret> secrets = List.of();
        char[] password = null;
        Path keyPath = null;

        try {
            if (mode.equals(Mode.KEYBOARD)) {
                password = System.console().readPassword("Password: ");
                secrets = fileService.getSecrets(new String(password));
            } else if (mode.equals(Mode.FILE)) {
                keyPath = Paths.get(System.console().readLine("File path: "));

                if (!Files.exists(keyPath)) {
                    displayFunction.accept("File path does not exist.");
                    return;
                }

                secrets = fileService.getSecrets(keyPath);
            }
        } catch (FileServiceException e) {
            displayFunction.accept("Could not read secrets from the file. Invalid password, maybe?");
            return;
        }

        if (secrets.stream().noneMatch(e -> e.key().equals(key))) {
            displayFunction.accept("Key does not exist in the keystore.");
            return;
        }

        List<Secret> newSecrets = secrets.stream()
                .map(e -> e.key().equals(key) ? new Secret(key, newValue) : e)
                .toList();

        try {
            if (mode.equals(Mode.KEYBOARD)) {
                fileService.createSecretsFile(new String(password), newSecrets);
            } else if (mode.equals(Mode.FILE)) {
                fileService.createSecretsFile(null, keyPath, newSecrets);
            }
        } catch (FileServiceException e) {
            displayFunction.accept("Error while (re-)creating the keystore file.");
            return;
        }

        displayFunction.accept("Key " + key + " has been updated in the keystore.");
    }
}
