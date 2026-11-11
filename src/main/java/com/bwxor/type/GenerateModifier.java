package com.bwxor.type;

public enum GenerateModifier {
    LENGTH("-len", "--length"),
    DIGITS("-d", "--digits"),
    UPPERCASE("-u", "--uppercase"),
    LOWERCASE("-l", "--lowercase"),
    SYMBOLS("-s", "--symbols");
    final String shortened;
    final String complete;

    GenerateModifier(String shortened, String complete) {
        this.shortened = shortened;
        this.complete = complete;
    }

    public static GenerateModifier parse(String value) {
        for(GenerateModifier m : values()) {
            if (m.shortened.equalsIgnoreCase(value) || m.complete.equalsIgnoreCase(value)) {
                return m;
            }
        }

        return null;
    }
}
