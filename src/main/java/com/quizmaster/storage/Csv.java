package com.quizmaster.storage;

import java.util.ArrayList;
import java.util.List;

/** Small RFC-4180-style helpers for one-line UTF-8 CSV records. */
public final class Csv {
    private Csv() { }

    public static String encode(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    public static String encodeRow(List<String> values) {
        List<String> encoded = new ArrayList<>();
        for (String value : values) encoded.add(encode(value));
        return String.join(",", encoded);
    }

    public static List<String> parse(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean closedQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                        closedQuote = true;
                    }
                } else field.append(ch);
            } else if (ch == ',' ) {
                fields.add(field.toString());
                field.setLength(0);
                closedQuote = false;
            } else if (ch == '"' && field.length() == 0 && !closedQuote) {
                quoted = true;
            } else if (closedQuote || ch == '"') {
                throw new IllegalArgumentException("Malformed CSV quoting.");
            } else field.append(ch);
        }
        if (quoted) throw new IllegalArgumentException("Unclosed CSV quote.");
        fields.add(field.toString());
        return fields;
    }
}
