package com.bwxor.command;

import java.util.List;
import java.util.function.Consumer;

public interface Command {
    void execute(List<String> args, Consumer<String> displayFunction);
}