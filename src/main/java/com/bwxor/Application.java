package com.bwxor;

import com.bwxor.command.impl.*;
import com.bwxor.command.impl.help.GenerateHelpCommand;
import com.bwxor.command.impl.help.ListHelpCommand;
import com.bwxor.service.FileService;
import com.bwxor.service.VaultService;
import net.harawata.appdirs.AppDirsFactory;

import java.nio.file.Paths;
import java.util.List;

public class Application {
    private static final  String APP_NAME = "keystore";
    private static final  String APP_AUTHOR = "bwxor";
    private static final  String USER_DATA_DIR = AppDirsFactory.getInstance().getUserDataDir(APP_NAME, null, APP_AUTHOR);

    static void main(String[] args) {
        VaultService vaultService = new VaultService();
        FileService fileService = new FileService(vaultService, Paths.get(USER_DATA_DIR));

        KeystoreCommand keystoreCommand = new KeystoreCommand();

        HelpCommand helpCommand = new HelpCommand();
        helpCommand.register("list", new ListHelpCommand());
        helpCommand.register("generate", new GenerateHelpCommand());

        keystoreCommand.register("help", helpCommand);
        keystoreCommand.register("list", new ListCommand(fileService));
        keystoreCommand.register("add", new AddCommand(fileService));
        keystoreCommand.register("generate", new GenerateCommand(fileService));
        keystoreCommand.register("get", new GetCommand(fileService));
        keystoreCommand.register("copy", new CopyCommand(fileService));
        keystoreCommand.register("remove", new RemoveCommand(fileService));
        keystoreCommand.register("delete-keystore", new DeleteFileCommand(fileService));
        keystoreCommand.register("mode", new ModeCommand(fileService));

        keystoreCommand.execute(List.of(args), System.out::println);
    }
}
