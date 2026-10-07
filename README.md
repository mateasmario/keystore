# Keystore

A command-line application for securely storing and managing key-value pairs with password protection. The keystore encrypts all stored secrets and stores them in a file that can only be accessed with the correct password.

## Installation

### Prerequisites

- Java 25 or higher installed on your system

### Step-by-Step Installation

1. **Download the binaries**
   - Download the latest `keystore` application binary release

2. **Extract the folder**
   - Extract the downloaded archive to a location on your computer
   - Example: `C:\Program Files\keystore` or `~/Applications/keystore`

3. **Add to PATH (Windows)**
   - Open Environment Variables:
     - Right-click **This PC** or **My Computer** → Properties
     - Click **Advanced system settings**
     - Click **Environment Variables** button
   - Under **User variables** or **System variables**, click **New**
   - Variable name: `PATH`
   - Variable value: Path to the extracted folder (e.g., `C:\Program Files\keystore\bin`)
   - Click **OK** and restart your command prompt/terminal

4. **Add to PATH (macOS/Linux)**
   - Open a terminal and edit your shell profile:
     ```bash
     nano ~/.bashrc    # or ~/.zshrc for zsh
     ```
   - Add the following line at the end:
     ```bash
     export PATH="$PATH:/path/to/keystore/bin"
     ```
   - Save and exit, then reload:
     ```bash
     source ~/.bashrc  # or source ~/.zshrc
     ```

5. **Verify Installation**
   - Open a new terminal/command prompt and type:
     ```bash
     keystore help
     ```
   - If successful, you should see the help message

## Building from Source

### Prerequisites

- Java 25 or higher
- Maven 3.6 or higher

### Build Steps

1. Build the project and create a fat JAR:
   ```bash
   mvn package
   ```
   This generates a fat JAR file at `target/keystore.jar` with all dependencies included.

2. (Optional) Create a native application image using jpackage:
   ```bash
   jpackage --type app-image --name keystore --input target --main-jar keystore.jar --main-class com.bwxor.Application --win-console --java-options "--enable-native-access=ALL-UNNAMED" --dest dist
   ```
   This creates a native application image in the `dist` directory.

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

### Delete File
Deletes the entire keystore file:
```bash
keystore delete-file
```
**Note:** Use with caution - this permanently deletes all stored secrets.

## Usage Examples

### Create a new keystore and add secrets:
```bash
keystore add database_password "mySecurePassword123"
```
(Prompts for a master password to encrypt the keystore)

### View all secrets:
```bash
keystore list
```
(Prompts for the master password)

### Add multiple secrets:
```bash
keystore add api_key "abc123xyz"
keystore add db_host "localhost"
keystore add db_port "5432"
```

## Data Storage

The keystore stores encrypted secrets in a file managed by the VaultService. The location of the keystore file is platform-dependent and determined by the application's file service.

## Security Notes

- Always use a strong, unique password for your keystore
- The master password is not stored - it's used only to decrypt/encrypt secrets
- Secrets are encrypted when stored to the file
- Keep your password secure and don't share it with others
