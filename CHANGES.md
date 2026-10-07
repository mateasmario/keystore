# Changes

- **src/main/java/com/bwxor/command/impl/AddCommand.java**: Replaced separate `displayFunction.accept("Please enter your password:")` and `System.console().readPassword()` calls with direct argument `System.console().readPassword("Password: ")`
- **src/main/java/com/bwxor/command/impl/GetCommand.java**: Replaced separate `displayFunction.accept("Please enter your password:")` and `System.console().readPassword()` calls with direct argument `System.console().readPassword("Password: ")`
- **src/main/java/com/bwxor/command/impl/RemoveCommand.java**: Replaced separate `displayFunction.accept("Please enter your password:")` and `System.console().readPassword()` calls with direct argument `System.console().readPassword("Password: ")`
