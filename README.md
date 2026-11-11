# 🔐Keystore

A command-line application for securely storing and managing key-value pairs with password protection. The keystore encrypts all stored secrets and stores them in a file that can only be accessed with the correct password.

## Installation

### Prerequisites

- Java 25 or higher installed on your system

### Step-by-Step Installation

1. Download the latest [binary release](https://github.com/mateasmario/keystore/releases/latest)
2. Extract the downloaded archive to a location on your computer
3. Add folder to PATH (Windows)
4. Verify installation by typing `keystore help` in the terminal

## Building from Source

### Prerequisites

- Java 25 or higher
- Maven 3.6 or higher

### Build Steps

Run `mvn package`. This creates a fat JAR in `target/` and, via jpackage (run as part of the `package` phase), an executable application under `dist/`.

## Commands

### Help
Displays all available commands. Use `help <command>` for details about a specific command:
```bash
keystore help
keystore help <command>
```

### Mode
Displays the current mode (KEYBOARD/FILE):
```bash
keystore mode
```

### List
Lists all stored key-value pairs. Prompts for password:
```bash
keystore list [options]
```

| Option | Description |
|--------|-------------|
| `-s`, `--silent` | List only the keys, without their values |

### Add
Adds a new key-value pair to the keystore. Prompts for password:
```bash
keystore add <key> <value>
```
**Example:**
```bash
keystore add api_key secret123
```

### Generate
Generates a random value and stores it under a new key. Prompts for password:
```bash
keystore generate <key> [options]
```

| Option | Description |
|--------|-------------|
| `-len <n>`, `--length <n>` | Value length (minimum 4, default 16) |
| `-d`, `--digits` | Include digits (0-9) |
| `-u`, `--uppercase` | Include uppercase letters (A-Z) |
| `-l`, `--lowercase` | Include lowercase letters (a-z) |
| `-s`, `--symbols` | Include symbols |

If no character type is given, all types are used. If one or more are given, only those are used.

**Example:**
```bash
keystore generate github --length 24 --digits --lowercase
```

### Get
Retrieves a specific key from the keystore. Prompts for password:
```bash
keystore get <key>
```
**Example:**
```bash
keystore get api_key
```

### Copy
Copies the value of a specific key to the clipboard. Prompts for password:
```bash
keystore copy <key>
```
**Example:**
```bash
keystore copy api_key
```

### Remove
Removes a key from the keystore. Prompts for password:
```bash
keystore remove <key>
```
**Example:**
```bash
keystore remove api_key
```

### Update
Updates the value of an existing key in the keystore. Prompts for password:
```bash
keystore update <key> <newValue>
```
**Example:**
```bash
keystore update api_key newsecret456
```

### Delete Keystore File
Deletes the entire keystore file:
```bash
keystore delete-keystore
```
**Note:** Use with caution - this permanently deletes all stored secrets.
