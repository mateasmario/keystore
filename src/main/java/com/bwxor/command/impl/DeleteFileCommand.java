package com.bwxor.command.impl;

import com.bwxor.command.Command;
import com.bwxor.entity.Secret;
import com.bwxor.exception.FileServiceException;
import com.bwxor.service.FileService;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;

public class DeleteFileCommand implements Command {
    private final FileService fileService;

    public DeleteFileCommand(FileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public void execute(List<String> args, Consumer<String> displayFunction) {
        displayFunction.accept("Are you sure? (Y/N):");
        Scanner scanner = new Scanner(System.in);
        String answer = scanner.nextLine();

        if (answer.equalsIgnoreCase("Y")) {
            try {
                fileService.deleteFile();
                displayFunction.accept("Successfully deleted the keystore file.");
            } catch (FileServiceException e) {
                displayFunction.accept("Could not delete keystore file.");
            }
        } else {
            displayFunction.accept("Keystore file has not been deleted.");
        }
    }
}