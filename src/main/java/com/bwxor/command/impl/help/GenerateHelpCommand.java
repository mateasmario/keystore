package com.bwxor.command.impl.help;

import com.bwxor.command.Command;

import java.util.List;
import java.util.function.Consumer;

public class GenerateHelpCommand implements Command {
    private static final String HELP_LIST_MESSAGE = """
        Usage: keystore generate <key> [options]

        Generates a random password and stores it under <key>.

        Options:
          --length <n>   Password length (minimum 4, default 16)
          --digits       Include digits (0-9)
          --uppercase    Include uppercase letters (A-Z)
          --lowercase    Include lowercase letters (a-z)
          --symbols      Include symbols (!@#$...)

        If no character type is given, all types are used.
        If one or more are given, only those are used.

        Example:
          keystore generate github --length 24 --digits --lowercase""";

    @Override
    public void execute(List<String> args, Consumer<String> displayFunction) {
        displayFunction.accept(HELP_LIST_MESSAGE);
    }
}
