package com.bwxor.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class TotpGeneratorTest {
    private static final byte[] TEST_KEY = "0123456789".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] TEST_KEY_2 = "1234567890".getBytes(StandardCharsets.US_ASCII);

    static IntStream interval30Secs() {
        return IntStream.rangeClosed(61, 89);
    }

    private TotpGenerator totpGenerator;

    @BeforeEach
    public void setUp() {
        totpGenerator = new TotpGenerator();
    }

    @Test
    void matchesRfc6238Vector() throws GeneralSecurityException {
        assertEquals("866985", totpGenerator.generate(TEST_KEY, 59));
    }

    @Test
    public void codeIs6Digits() throws GeneralSecurityException {
        String otp = totpGenerator.generate(TEST_KEY, 60);
        assertEquals(6, otp.length());
    }

    @ParameterizedTest
    @MethodSource("interval30Secs")
    public void codeStaysTheSameInsideOneTimeStep(int t) throws GeneralSecurityException {
        assertEquals(totpGenerator.generate(TEST_KEY, 60), totpGenerator.generate(TEST_KEY, t));
    }

    @Test
    public void codeChangesWhenTimeExceeds30Secs() throws GeneralSecurityException {
        assertNotEquals(totpGenerator.generate(TEST_KEY, 30), totpGenerator.generate(TEST_KEY, 60));
    }

    @Test
    public void differentKeysGiveDifferentCodes() throws GeneralSecurityException {
        assertNotEquals(totpGenerator.generate(TEST_KEY, 30), totpGenerator.generate(TEST_KEY_2, 30));
    }

    @ParameterizedTest
    @CsvSource({"0,30", "1,29", "29,1", "30,30", "31,29", "59,1", "60,30"})
    void secondsRemainingCountsDownToTheNextStep(long time, int expected) {
        assertEquals(expected, totpGenerator.secondsRemaining(time));
    }

    @Test
    void decodesBase32() {
        assertEquals("foobar", new String(TotpGenerator.decodeBase32("MZXW6YTBOI======"), StandardCharsets.US_ASCII));
    }
}
