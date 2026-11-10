package com.bwxor.command.impl;

import com.bwxor.command.Command;
import com.bwxor.entity.Secret;
import com.bwxor.exception.FileServiceException;
import com.bwxor.generator.PasswordGenerator;
import com.bwxor.service.FileService;
import com.bwxor.type.Mode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GenerateCommand implements Command {
    private static final String USAGE_MESSAGE = """
            Usage: 'keystore generate <key>'""";

    private final FileService fileService;

    public GenerateCommand(FileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public void execute(List<String> args, Consumer<String> displayFunction) {
        if (args.size() != 1) {
            displayFunction.accept(USAGE_MESSAGE);
            return;
        }

        Mode mode = Mode.UNKNOWN;
        try {
            mode = fileService.readModeFromFile();
        } catch (FileServiceException e) {
            displayFunction.accept("Mode file does not exist yet.");
        }

        while (mode.equals(Mode.UNKNOWN)) {
            String modeInput = System.console().readLine("Mode (KEYBOARD/FILE): ");
            mode = Mode.parse(modeInput.trim().toUpperCase());
        }

        char[] password = null;

        Path keyPath = null;


        List<Secret> secrets = List.of();

        try {
            if (mode.equals(Mode.KEYBOARD)) {
                password = System.console().readPassword("Password: ");
                secrets = fileService.getSecrets(new String(password));
            } else if (mode.equals(Mode.FILE)) {
                keyPath = Paths.get(System.console().readLine("File path: "));

                if (!Files.exists(keyPath)) {
                    displayFunction.accept("File path does not exist. One will be created for you.");
                    password = System.console().readPassword("Password to derive key from: ");
                }
                secrets = fileService.getSecrets(keyPath);
            }
        } catch(FileServiceException e) {
            displayFunction.accept("Could not read secrets from file.");
            return;
        }

        if (secrets.stream().anyMatch(e -> e.key().equals(args.get(0)))) {
            displayFunction.accept("A key with the same name already exists.");
            return;
        }

        List<Secret> newSecrets = new ArrayList<>(secrets);

        String generatedValue = PasswordGenerator.generate();

        newSecrets.add(new Secret(args.get(0), generatedValue));

        try {
            if (mode.equals(Mode.KEYBOARD)) {
                fileService.createSecretsFile(new String(password), newSecrets);
            }
            else if (mode.equals(Mode.FILE)) {
                fileService.createSecretsFile(new String(password), keyPath, newSecrets);
            }
            fileService.createModeFile(mode);
        } catch (FileServiceException e) {
            displayFunction.accept("Error while (re-)creating the keystore file.");
            return;
        }

        displayFunction.accept("Key " + args.get(0) + " has been generated and added to the keystore.");
    }
}
