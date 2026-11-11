package com.bwxor.command.impl;

import com.bwxor.command.Command;
import com.bwxor.entity.Secret;
import com.bwxor.exception.FileServiceException;
import com.bwxor.exception.GenerateModifiersProcessingException;
import com.bwxor.generator.PasswordGenerator;
import com.bwxor.service.FileService;
import com.bwxor.type.GenerateModifier;
import com.bwxor.type.Mode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GenerateCommand implements Command {
    record Modifiers(int length, boolean uppercase, boolean lowercase, boolean digits, boolean symbols) { }

    private static final int DEFAULT_LENGTH = 16;

    private static final String USAGE_MESSAGE = """
            Usage: keystore generate <key>""";

    private final FileService fileService;

    public GenerateCommand(FileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public void execute(List<String> args, Consumer<String> displayFunction) {
        if (args.isEmpty()) {
            displayFunction.accept(USAGE_MESSAGE);
            return;
        }

        Modifiers modifiers = null;
        try {
            modifiers = processModifiers(args);
        } catch (GenerateModifiersProcessingException e) {
            displayFunction.accept(e.getMessage());
            return;
        }

        Mode mode = Mode.UNKNOWN;
        try {
            mode = fileService.readModeFromFile();
        } catch (FileServiceException e) {
            displayFunction.accept("Mode file does not exist yet. It will be created for you.");
        }

        while (mode.equals(Mode.UNKNOWN)) {
            String modeInput = System.console().readLine("Mode (KEYBOARD/FILE): ");
            mode = Mode.parse(modeInput.trim());
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
            displayFunction.accept("Could not read secrets from the file. Invalid password, maybe?");
            return;
        }

        if (secrets.stream().anyMatch(e -> e.key().equals(args.get(0)))) {
            displayFunction.accept("A key with the same name already exists.");
            return;
        }

        List<Secret> newSecrets = new ArrayList<>(secrets);

        String generatedValue = new PasswordGenerator().generate(modifiers.length, modifiers.digits, modifiers.uppercase, modifiers.lowercase, modifiers.symbols);

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

    private Modifiers processModifiers(List<String> args) throws GenerateModifiersProcessingException {
        int length = DEFAULT_LENGTH;
        boolean digits = true;
        boolean uppercase = true;
        boolean lowercase = true;
        boolean symbols = true;

        // do a first pass to disable all modifiers, if any is explicitly required
        for (int i = 0; i < args.size(); i++) {
            GenerateModifier m = GenerateModifier.parse(args.get(i));
            if (m != null && !m.equals(GenerateModifier.LENGTH)) {
                digits = false;
                uppercase = false;
                lowercase = false;
                symbols = false;
            }
        }

        for (int i = 0; i < args.size(); i++) {
            GenerateModifier m = GenerateModifier.parse(args.get(i));

            if (m != null) {
                switch (m) {
                    case LENGTH -> {
                        if (i + 1 < args.size()) {
                            try {
                                length = Integer.parseInt(args.get(i + 1));
                                if (length < 4) {
                                    throw new GenerateModifiersProcessingException("Minimum length should be 4.");
                                }
                            } catch (NumberFormatException e) {
                                throw new GenerateModifiersProcessingException("Number expected after -len (--length) modifier.");
                            }
                        }
                    }
                    case DIGITS -> digits = true;
                    case UPPERCASE -> uppercase = true;
                    case LOWERCASE -> lowercase = true;
                    case SYMBOLS -> symbols = true;
                }
            }

        }

        return new Modifiers(length, digits, uppercase, lowercase, symbols);
    }
}
