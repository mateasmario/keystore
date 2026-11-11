package com.bwxor.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordGeneratorTest {

    private static final String UPPER   = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER   = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS  = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{};:,.<>?";

    private PasswordGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new PasswordGenerator();
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 8, 16, 32, 128, 1024})
    void generatesRequestedLength(int length) {
        String password = generator.generate(length, true, true, true, true);

        assertEquals(length, password.length());
    }

    @RepeatedTest(200)
    void containsAtLeastOneCharOfEachSelectedType() {
        String password = generator.generate(32, true, true, true, true);

        assertTrue(containsAny(password, DIGITS), "missing digit: " + password);
        assertTrue(containsAny(password, UPPER), "missing uppercase: " + password);
        assertTrue(containsAny(password, LOWER), "missing lowercase: " + password);
        assertTrue(containsAny(password, SYMBOLS), "missing symbol: " + password);
    }

    @RepeatedTest(100)
    void usesOnlyDigits() {
        assertOnlyFrom(generator.generate(32, true, false, false, false), DIGITS);
    }

    @RepeatedTest(100)
    void usesOnlyUppercase() {
        assertOnlyFrom(generator.generate(32, false, true, false, false), UPPER);
    }

    @RepeatedTest(100)
    void usesOnlyLowercase() {
        assertOnlyFrom(generator.generate(32, false, false, true, false), LOWER);
    }

    @RepeatedTest(100)
    void usesOnlySymbols() {
        assertOnlyFrom(generator.generate(32, false, false, false, true), SYMBOLS);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15})
    void worksForEveryCombinationOfTypes(int mask) {
        boolean digits    = (mask & 1) != 0;
        boolean uppercase = (mask & 2) != 0;
        boolean lowercase = (mask & 4) != 0;
        boolean symbols   = (mask & 8) != 0;

        String allowed = (digits ? DIGITS : "")
                + (uppercase ? UPPER : "")
                + (lowercase ? LOWER : "")
                + (symbols ? SYMBOLS : "");

        for (int i = 0; i < 100; i++) { // aleatoriu -> repetă
            String password = generator.generate(32, digits, uppercase, lowercase, symbols);

            assertEquals(32, password.length());
            assertOnlyFrom(password, allowed);
            if (digits)    assertTrue(containsAny(password, DIGITS),  "no digit: " + password);
            if (uppercase) assertTrue(containsAny(password, UPPER),   "no uppercase: " + password);
            if (lowercase) assertTrue(containsAny(password, LOWER),   "no lowercase: " + password);
            if (symbols)   assertTrue(containsAny(password, SYMBOLS), "no symbol: " + password);
        }
    }

    @Test
    void consecutiveCallsProduceDifferentPasswords() {
        Set<String> passwords = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            passwords.add(generator.generate(32, true, true, true, true));
        }

        assertEquals(100, passwords.size());
    }

    @Test
    void guaranteedCharactersAreNotAlwaysAtTheSamePositions() {
        Set<Boolean> firstCharIsDigit = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            String p = generator.generate(4, true, true, true, true);
            firstCharIsDigit.add(DIGITS.indexOf(p.charAt(0)) >= 0);
        }

        assertEquals(2, firstCharIsDigit.size(), "first char was always/never a digit");
    }

    private static boolean containsAny(String s, String chars) {
        return s.chars().anyMatch(c -> chars.indexOf(c) >= 0);
    }

    private static void assertOnlyFrom(String s, String allowed) {
        assertTrue(s.chars().allMatch(c -> allowed.indexOf(c) >= 0),
                "unexpected character in: " + s);
    }
}