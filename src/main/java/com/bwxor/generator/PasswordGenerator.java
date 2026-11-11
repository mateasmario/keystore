package com.bwxor.generator;

import java.security.SecureRandom;

public class PasswordGenerator {
    private static final String EMPTY_STRING = "";
    private static final String UPPER   = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER   = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS  = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{};:,.<>?";

    private static final SecureRandom RANDOM = new SecureRandom();

    public String generate(int length, boolean digits, boolean uppercase, boolean lowercase, boolean symbols) {
        String ALL = EMPTY_STRING;

        int curr = 0;

        char[] password = new char[length];

        if (digits) {
            ALL += DIGITS;
            password[curr++] = pick(DIGITS);
        }
        if (uppercase) {
            ALL += UPPER;
            password[curr++] = pick(UPPER);
        }
        if (lowercase) {
            ALL += LOWER;
            password[curr++] = pick(LOWER);
        }
        if (symbols) {
            ALL += SYMBOLS;
            password[curr++] = pick(SYMBOLS);
        }

        for (int i = curr; i < length; i++) {
            password[i] = pick(ALL);
        }

        for (int i = length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char tmp = password[i];
            password[i] = password[j];
            password[j] = tmp;
        }

        return new String(password);
    }

    private static char pick(String chars) {
        return chars.charAt(RANDOM.nextInt(chars.length()));
    }
}
