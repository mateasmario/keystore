package com.bwxor.command.impl;

import com.bwxor.command.Command;

import java.util.List;
import java.util.function.Consumer;

public class HelpCommand implements Command {
    private static final String HELP_MESSAGE = """
            help            Displays all available commands
            list            Lists all keys from the keystore
            get             Gets a specific key from the keystore
            add             Adds a new key-value item to the keystore
            remove          Removes a key from the keystore
            delete-file     Deletes the entire keystore file""";

    @Override
    public void execute(List<String> args, Consumer<String> displayFunction) {
        displayFunction.accept(HELP_MESSAGE);
    }
}
