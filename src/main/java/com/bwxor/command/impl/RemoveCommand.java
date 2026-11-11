package com.bwxor.command.impl;

import com.bwxor.command.Command;
import com.bwxor.entity.Secret;
import com.bwxor.exception.FileServiceException;
import com.bwxor.service.FileService;
import com.bwxor.type.Mode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RemoveCommand implements Command {
    private static final String USAGE_MESSAGE = """
            Usage: keystore remove <key>""";

    private final FileService fileService;

    public RemoveCommand(FileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public void execute(List<String> args, Consumer<String> displayFunction) {
        if (args.size() != 1) {
            displayFunction.accept(USAGE_MESSAGE);
            return;
        }

        Mode mode;
        try {
            mode = fileService.readModeFromFile();
        } catch (FileServiceException e) {
            displayFunction.accept("Could not read mode from the file. Delete the secrets file and re-create it.");
            return;
        }

        List<Secret> secrets = List.of();
        List<Secret> newSecrets = List.of();

        try {
            if (mode.equals(Mode.KEYBOARD)) {
                char[] password = System.console().readPassword("Password: ");
                secrets = fileService.getSecrets(new String(password));
                fileService.createSecretsFile(new String(password), createSecretsListWithoutKeyToBeRemoved(secrets, args.get(0), displayFunction));
            } else if (mode.equals(Mode.FILE)) {
                Path keyPath = Paths.get(System.console().readLine("File path: "));

                if (!Files.exists(keyPath)) {
                    displayFunction.accept("File path does not exist.");
                    return;
                }

                secrets = fileService.getSecrets(keyPath);
                fileService.createSecretsFile(null, keyPath, createSecretsListWithoutKeyToBeRemoved(secrets, args.get(0), displayFunction));
            }
        } catch (FileServiceException e) {
            displayFunction.accept("Could not read secrets from the file. Invalid password, maybe?");
            return;
        }

        displayFunction.accept("Key " + args.get(0) + " has been removed from the keystore.");
    }

    private List<Secret> createSecretsListWithoutKeyToBeRemoved(List<Secret> secrets, String key, Consumer<String> displayFunction) {
        List<Secret> newSecrets = new ArrayList<>();
        newSecrets.addAll(secrets);

        var keyOptional = secrets.stream().filter(e -> e.key().equals(key)).findAny();

        if (keyOptional.isPresent()) {
            displayFunction.accept("Key " + keyOptional.get().key() + " has been removed from the keystore.");
            newSecrets.remove(keyOptional.get());
        } else {
            displayFunction.accept("Key does not exist in the keystore.");
        }

        return newSecrets;
    }
}
