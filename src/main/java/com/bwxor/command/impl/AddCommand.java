package com.bwxor.command.impl;

import com.bwxor.command.Command;
import com.bwxor.entity.Secret;
import com.bwxor.exception.FileServiceException;
import com.bwxor.service.FileService;

import java.util.*;
import java.util.function.Consumer;

public class AddCommand implements Command {
    private static final String USAGE_MESSAGE = """
            Usage: 'keystore add <key> <value>'""";

    private final FileService fileService;

    public AddCommand(FileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public void execute(List<String> args, Consumer<String> displayFunction) {
        if (args.size() != 2) {
            displayFunction.accept(USAGE_MESSAGE);
            return;
        }

        char[] password = System.console().readPassword("Password: ");

        List<Secret> secrets;

        try {
            secrets = fileService.getSecrets(new String(password));
        } catch (FileServiceException e) {
            displayFunction.accept("Could not read secrets from the file.");
            return;
        }

        if (secrets.stream().anyMatch(e -> e.key().equals(args.get(0)))) {
            displayFunction.accept("A key with the same name already exists.");
            return;
        }

        List<Secret> newSecrets = new ArrayList<>();
        newSecrets.addAll(secrets);
        newSecrets.add(new Secret(args.get(0), args.get(1)));

        try {
            fileService.createSecretsFile(new String(password), newSecrets);
        } catch (FileServiceException e) {
            displayFunction.accept("Error while (re-)creating the keystore file.");
        }

        displayFunction.accept("Key " + args.get(0) + " has been added to the keystore.");
    }
}
