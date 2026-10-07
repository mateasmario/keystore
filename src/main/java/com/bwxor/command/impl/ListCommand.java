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

public class ListCommand implements Command {
    private static final String ASSIGN_OPERATOR = "=";
    private static final String NEWLINE = "\n";
    private final Map<String, Command> subcommands = new HashMap<>();

    private final FileService fileService;

    public ListCommand(FileService fileService) {
        this.fileService = fileService;
    }

    public ListCommand register(String name, Command command) {
        subcommands.put(name, command);
        return this;
    }

    @Override
    public void execute(List<String> args, Consumer<String> displayFunction) {
        char[] password = System.console().readPassword("Password: ");

        List<Secret> secrets;

        try {
            secrets = fileService.getSecrets(new String(password));
        } catch (FileServiceException e) {
            displayFunction.accept("Could not read secrets from the file.");
            return;
        }

        for (Secret s : secrets) {
            displayFunction.accept(s.key() + ASSIGN_OPERATOR + s.value());
        }
    }
}