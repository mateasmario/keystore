package com.bwxor.command.impl;

import com.bwxor.command.Command;
import com.bwxor.entity.Secret;
import com.bwxor.exception.FileServiceException;
import com.bwxor.service.FileService;

import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;

public class RemoveCommand implements Command {
    private static final String ASSIGN_OPERATOR = "=";
    private static final String NEWLINE = "\n";
    private static final String USAGE_MESSAGE = """
            Usage: 'keystore remove <key>'
            """;

    private final FileService fileService;

    public RemoveCommand(FileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public void execute(List<String> args, Consumer<String> displayFunction) {
        if (args.size() != 1) {
            displayFunction.accept(USAGE_MESSAGE);
        }

        Scanner scanner = new Scanner(System.in);
        displayFunction.accept("Please enter your password:");
        String password = scanner.nextLine();

        List<Secret> secrets;

        try {
            secrets = fileService.getSecrets(password);
        } catch (FileServiceException e) {
            displayFunction.accept("Could not read secrets from the file.");
            return;
        }


        var keyOptional = secrets.stream().filter(e -> e.key().equals(args.get(0))).findAny();

        if (keyOptional.isPresent()) {
            secrets.remove(keyOptional.get());
            displayFunction.accept("Key " + keyOptional.get().key() + " has been removed from the keystore.");
        } else {
            displayFunction.accept("Key does not exist in the keystore.");
        }

        try {
            fileService.createSecretsFile(password, secrets);
        } catch (FileServiceException e) {
            displayFunction.accept("Error while (re-)creating the keystore file.");
        }

        displayFunction.accept("Key " + args.get(0) + " has been added to the keystore.");
    }
}