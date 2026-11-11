# Changes

- `src/main/java/com/bwxor/command/impl/UpdateCommand.java` (new): adds `update <key> <newvalue>`. It prompts for the password (KEYBOARD mode) or key file path (FILE mode) like the other commands, replaces the value of an existing key, and rewrites the keystore. It reports an error if the key does not exist.
- `src/main/java/com/bwxor/Application.java`: registers the `update` command with the keystore dispatcher.
- `src/main/java/com/bwxor/command/impl/HelpCommand.java`: adds an `update` line to the help message.
- `README.md`: adds an Update section to the Commands list, after Remove.
- `CHANGES.md` (new): this change log.
