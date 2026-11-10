package com.bwxor.generator;

import java.security.SecureRandom;

public class PasswordGenerator {
    private static final int LENGTH = 16;
    private static final String UPPER   = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER   = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS  = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{};:,.<>?";
    private static final String ALL = UPPER + LOWER + DIGITS + SYMBOLS;

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generate() {
        char[] password = new char[LENGTH];

        password[0] = pick(UPPER);
        password[1] = pick(LOWER);
        password[2] = pick(DIGITS);
        password[3] = pick(SYMBOLS);

        for (int i = 4; i < LENGTH; i++) {
            password[i] = pick(ALL);
        }

        for (int i = LENGTH - 1; i > 0; i--) {
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
