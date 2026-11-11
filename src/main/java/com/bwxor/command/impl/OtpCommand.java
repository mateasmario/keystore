package com.bwxor.command.impl;

import com.bwxor.command.Command;
import com.bwxor.entity.Secret;
import com.bwxor.exception.FileServiceException;
import com.bwxor.generator.TotpGenerator;
import com.bwxor.service.FileService;
import com.bwxor.type.Mode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

public class OtpCommand implements Command {
    private static final String USAGE_MESSAGE = """
            Usage: keystore otp <key>""";

    private final FileService fileService;
    private final TotpGenerator totpGenerator = new TotpGenerator();

    public OtpCommand(FileService fileService) {
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

        if (keyOptional.isEmpty()) {
            displayFunction.accept("Key does not exist in the keystore.");
            return;
        }

        byte[] secretKey = toKeyBytes(keyOptional.get().value());
        long now = Instant.now().getEpochSecond();

        try {
            String code = totpGenerator.generate(secretKey, now);
            displayFunction.accept(code + " (valid for " + totpGenerator.secondsRemaining(now) + "s)");
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            displayFunction.accept("Could not generate an OTP code from this key.");
        }
    }

    /**
     * Keys are usually base32 secrets (as shown by authenticator setup pages). If the value
     * is not valid base32, its UTF-8 bytes are used instead.
     */
    private byte[] toKeyBytes(String value) {
        try {
            return TotpGenerator.decodeBase32(value);
        } catch (IllegalArgumentException e) {
            return value.getBytes(StandardCharsets.UTF_8);
        }
    }
}
