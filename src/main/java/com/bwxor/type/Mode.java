package com.bwxor.type;

import java.util.Locale;

public enum Mode {
    KEYBOARD("KEYBOARD"), FILE("FILE"), UNKNOWN("UNKNOWN");
    final String value;

    Mode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Mode parse(String value) {
        for(Mode m : values()) {
            if (m.value.equalsIgnoreCase(value)) {
                return m;
            }
        }

        for (Mode m : values()) {
            if (m.value.toLowerCase(Locale.ROOT).startsWith(value.toLowerCase(Locale.ROOT))) {
                return m;
            }
        }

        return UNKNOWN;
    }
}
