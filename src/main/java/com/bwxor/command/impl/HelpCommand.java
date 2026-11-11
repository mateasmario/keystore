package com.bwxor.command.impl;

import com.bwxor.command.CommandDispatcher;

import java.util.function.Consumer;

public class HelpCommand extends CommandDispatcher {
    private static final String HELP_MESSAGE = """
            help                Displays all available commands
            mode                Displays the current mode (KEYBOARD/FILE)
            list                Lists all keys from the keystore
            get                 Gets a specific key from the keystore
            copy                Copies a specific value to the clipboard
            add                 Adds a new key-value item to the keystore
            generate            Generates a secure value for a given key name
            remove              Removes a key from the keystore
            update              Updates the value of an existing key in the keystore
            otp                 Generates a one-time password (changes every 30 seconds) from a key
            delete-keystore     Deletes the entire keystore file""";


    @Override
    public void processDefaultMessage(Consumer<String> displayFunction) {
        displayFunction.accept(HELP_MESSAGE);
    }
}
