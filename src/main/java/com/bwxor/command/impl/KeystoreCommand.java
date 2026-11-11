package com.bwxor.command.impl;

import com.bwxor.command.CommandDispatcher;

import java.util.function.Consumer;

public class KeystoreCommand extends CommandDispatcher {
    private static final String USAGE_MESSAGE = """
        Usage: keystore <command> [options]
        Type 'keystore help' for more information.""";


    @Override
    public void processDefaultMessage(Consumer<String> displayFunction) {
        displayFunction.accept(USAGE_MESSAGE);
    }
}