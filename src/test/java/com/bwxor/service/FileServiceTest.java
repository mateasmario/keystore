package com.bwxor.service;

import com.bwxor.entity.Secret;
import com.bwxor.exception.FileServiceException;
import com.bwxor.type.Mode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FileServiceTest {
    @TempDir
    Path tempDir;

    private FileService fileService;
    private VaultService vaultService;

    @BeforeEach
    void setUp() {
        vaultService = new VaultService();
        fileService = new FileService(vaultService, tempDir);
    }

    @Test
    void secretsRoundTrip() throws FileServiceException {
        List<Secret> secrets = List.of(new Secret("github", "p@ss"), new Secret("mail", "ünï"));

        fileService.createSecretsFile("masterPassword", secrets);

        assertEquals(secrets, fileService.getSecrets("masterPassword"));
    }

    @Test
    void wrongPasswordThrows() throws FileServiceException {
        fileService.createSecretsFile("right", List.of(new Secret("a", "b")));

        assertThrows(FileServiceException.class, () -> fileService.getSecrets("wrong"));
    }

    @Test
    void deleteSecretsFileRemovesTheSecrets() throws FileServiceException {
        fileService.createSecretsFile("pw", List.of(new Secret("a", "b")));

        fileService.deleteSecretsFile();

        assertEquals(List.of(), fileService.getSecrets("pw"));
    }

    @ParameterizedTest
    @EnumSource(value = Mode.class, names = {"KEYBOARD", "FILE"})
    void modeRoundTrip(Mode mode) throws FileServiceException {
        fileService.createModeFile(mode);

        assertEquals(mode, fileService.readModeFromFile());
    }

    @Test
    void createModeFileOverwritesPreviousMode() throws FileServiceException {
        fileService.createModeFile(Mode.KEYBOARD);
        fileService.createModeFile(Mode.FILE);

        assertEquals(Mode.FILE, fileService.readModeFromFile());
    }

    @Test
    void readModeFromFileThrowsWhenMissing() {
        assertThrows(FileServiceException.class, () -> fileService.readModeFromFile());
    }

    @Test
    void readModeFromFileReturnsUnknownForInvalidContent() throws Exception {
        Files.writeString(tempDir.resolve("mode"), "garbage");

        assertEquals(Mode.UNKNOWN, fileService.readModeFromFile());
    }

    @Test
    void deleteModeFileRemovesTheMode() throws FileServiceException {
        fileService.createModeFile(Mode.KEYBOARD);

        fileService.deleteModeFile();

        assertThrows(FileServiceException.class, () -> fileService.readModeFromFile());
    }
}
