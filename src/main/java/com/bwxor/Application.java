package com.bwxor;

import com.bwxor.command.impl.*;
import com.bwxor.service.FileService;
import com.bwxor.service.VaultService;

import java.util.List;

public class Application {
    static void main(String[] args) {
        VaultService vaultService = new VaultService();
        FileService fileService = new FileService(vaultService);

        CommandDispatcher commandDispatcher = new CommandDispatcher();

        commandDispatcher.register("help", new HelpCommand());
        commandDispatcher.register("list", new ListCommand(fileService));
        commandDispatcher.register("add", new AddCommand(fileService));
        commandDispatcher.register("get", new GetCommand(fileService));
        commandDispatcher.register("remove", new RemoveCommand(fileService));
        commandDispatcher.register("delete-file", new DeleteFileCommand(fileService));

        commandDispatcher.execute(List.of(args), System.out::println);
    }
}
