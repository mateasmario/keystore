# Keystore

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

1. Build the project and create a fat JAR:
   ```bash
   mvn package
   ```

2. (Optional) Create a native application image using jpackage:
   ```bash
   jpackage --type app-image --name keystore --input target --main-jar keystore.jar --main-class com.bwxor.Application --win-console --java-options "--enable-native-access=ALL-UNNAMED" --dest dist
   ```

## Commands

### Help
Displays all available commands:
```bash
keystore help
```

### List
Lists all stored key-value pairs. Prompts for password:
```bash
keystore list
```

### Add
Adds a new key-value pair to the keystore. Prompts for password:
```bash
keystore add <key> <value>
```
**Example:**
```bash
keystore add api_key "secret123"
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

### Remove
Removes a key from the keystore. Prompts for password:
```bash
keystore remove <key>
```
**Example:**
```bash
keystore remove api_key
```

### Delete Keystore File
Deletes the entire keystore file:
```bash
keystore delete-keystore
```
**Note:** Use with caution - this permanently deletes all stored secrets.
