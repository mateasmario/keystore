package com.bwxor.command.impl;

import com.bwxor.command.Command;
import com.bwxor.entity.Secret;
import com.bwxor.exception.FileServiceException;
import com.bwxor.service.FileService;
import com.bwxor.type.Mode;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;

public class CopyCommand implements Command {
    private static final String USAGE_MESSAGE = """
            Usage: keystore copy <key>""";

    private final FileService fileService;

    public CopyCommand(FileService fileService) {
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

        var keyOptional = secrets.stream().filter(e -> e.key().equals(args.get(0))).findAny();

        if (keyOptional.isPresent()) {
            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new StringSelection(keyOptional.get().value()), null);
            displayFunction.accept("Key " + keyOptional.get().key() + " has been copied to the clipboard.");
        } else {
            displayFunction.accept("Key does not exist in the keystore.");
        }
    }
}
