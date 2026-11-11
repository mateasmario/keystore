package com.bwxor.command.impl;

import com.bwxor.command.Command;
import com.bwxor.entity.Secret;
import com.bwxor.exception.FileServiceException;
import com.bwxor.service.FileService;
import com.bwxor.type.Mode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Consumer;

public class ListCommand implements Command {
    private static final String ASSIGN_OPERATOR = "=";
    private static final String EMPTY_STRING = "";
    private final Map<String, Command> subcommands = new HashMap<>();

    private final FileService fileService;

    public ListCommand(FileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public void execute(List<String> args, Consumer<String> displayFunction) {
        boolean silent = !args.isEmpty() && (args.getFirst().equalsIgnoreCase("-s") || args.getFirst().equalsIgnoreCase("--silent"));

        Mode mode;
        try {
            mode = fileService.readModeFromFile();
        } catch (FileServiceException e) {
            displayFunction.accept("Could not read mode from the file. Delete the secrets file and re-create it.");
            return;
        }

        List<Secret> secrets = null;

        try {
            if (mode.equals(Mode.KEYBOARD)) {
                char[] password = System.console().readPassword("Password: ");
                secrets = fileService.getSecrets(new String(password));
            } else if (mode.equals(Mode.FILE)) {
                Path keyPath = Paths.get(System.console().readLine("File path: "));

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

        assert secrets != null;
        for (Secret s : secrets) {
            displayFunction.accept(s.key() + (!silent ? (ASSIGN_OPERATOR + s.value()) : EMPTY_STRING));
        }
    }
}