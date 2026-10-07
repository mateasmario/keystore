package com.bwxor.command.impl;

import com.bwxor.command.Command;
import com.bwxor.entity.Secret;
import com.bwxor.exception.FileServiceException;
import com.bwxor.service.FileService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Consumer;

public class AddCommand implements Command {
    private static final String USAGE_MESSAGE = """
            Usage: 'keystore add <key> <value>'
            """;

    private final FileService fileService;

    public AddCommand(FileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public void execute(List<String> args, Consumer<String> displayFunction) {
        if (args.size() != 2) {
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

        if (secrets.stream().anyMatch(e -> e.key().equals(args.get(0)))) {
            displayFunction.accept("A key with the same name already exists.");
            return;
        }

        secrets.add(new Secret(args.get(0), args.get(1)));

        try {
            fileService.createSecretsFile(password, secrets);
        } catch (FileServiceException e) {
            displayFunction.accept("Error while (re-)creating the keystore file.");
        }

        displayFunction.accept("Key " + args.get(0) + " has been added to the keystore.");
    }
}