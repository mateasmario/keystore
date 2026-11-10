package com.bwxor.command.impl;

import com.bwxor.command.Command;
import com.bwxor.exception.FileServiceException;
import com.bwxor.service.FileService;
import com.bwxor.type.Mode;

import java.util.List;
import java.util.function.Consumer;

public class ModeCommand implements Command {
    private final FileService fileService;

    public ModeCommand(FileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public void execute(List<String> args, Consumer<String> displayFunction) {
        try {
            Mode mode = fileService.readModeFromFile();
            displayFunction.accept(mode.getValue());
        } catch (FileServiceException e) {
            displayFunction.accept("Could not read mode from the file. Delete the secrets file and re-create it.");
        }
    }
}
