package com.bwxor.command.impl;

import com.bwxor.command.Command;
import com.bwxor.entity.Secret;
import com.bwxor.exception.FileServiceException;
import com.bwxor.service.FileService;

import java.util.List;
import java.util.function.Consumer;

public class GetCommand implements Command {
    private static final String ASSIGN_OPERATOR = "=";
    private static final String NEWLINE = "\n";
    private static final String USAGE_MESSAGE = """
            Usage: 'keystore get <key>'""";

    private final FileService fileService;

    public GetCommand(FileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public void execute(List<String> args, Consumer<String> displayFunction) {
        if (args.size() != 1) {
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

        var keyOptional = secrets.stream().filter(e -> e.key().equals(args.get(0))).findAny();

        if (keyOptional.isPresent()) {
            displayFunction.accept(keyOptional.get().key() + ASSIGN_OPERATOR + keyOptional.get().value() + NEWLINE);
        } else {
            displayFunction.accept("Key does not exist in the keystore.");
        }
    }
}
