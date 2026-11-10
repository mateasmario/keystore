package com.bwxor.type;

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
            if (m.value.equals(value)) {
                return m;
            }
        }

        for (Mode m : values()) {
            if (m.value.startsWith(value)) {
                return m;
            }
        }

        return UNKNOWN;
    }
}
