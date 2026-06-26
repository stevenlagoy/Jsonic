package com.stevenlagoy.jsonic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Internal utility class responsible for parsing JSON strings into
 * {@link JSONObject} trees.
 * <p>
 * This class implements a recursive descent parser following the JSON grammar:
 * 
 * <pre>
 * &lt;json&gt;     ::= &lt;value&gt;
 * &lt;value&gt;    ::= &lt;string&gt; | &lt;number&gt; | &lt;object&gt; | &lt;array&gt; | true | false | null
 * &lt;object&gt;   ::= { } | { &lt;members&gt; }
 * &lt;members&gt;  ::= &lt;pair&gt; | &lt;pair&gt; , &lt;members&gt;
 * &lt;pair&gt;     ::= &lt;string&gt; : &lt;value&gt;
 * &lt;array&gt;    ::= [ ] | [ &lt;elements&gt; ]
 * &lt;elements&gt; ::= &lt;value&gt; | &lt;value&gt; , &lt;elements&gt;
 * &lt;string&gt;   ::= " &lt;characters&gt; "
 * &lt;number&gt;   ::= &lt;int&gt; &lt;frac&gt;? &lt;exp&gt;?
 * </pre>
 * <p>
 * All public-facing JSON parsing entry points are exposed through
 * {@link JSONObject} constructors. This class is not part of the public API and
 * should not be used directly by library consumers.
 * <p>
 * <b>Null sentinel:</b> Internally, JSON {@code null} values are represented by
 * {@link #NULL_SENTINEL} to distinguish them from parse failures, which are
 * represented by Java {@code null}. The sentinel is converted to Java
 * {@code null} before any value is stored in a {@link JSONObject}.
 */
class JSONParser {

    private JSONParser() {
    }

    /**
     * Internal sentinel object representing a successfully parsed JSON {@code null}
     * value. Distinct from Java {@code null}, which signals a parse failure
     * throughout this class.
     */
    private static final Object NULL_SENTINEL = new Object();

    /**
     * Parses the given JSON string and returns a {@link JSONObject} with the given
     * key.
     * <p>
     * Newlines outside of string literals are normalized to spaces before parsing.
     *
     * @param key      the key to assign to the root {@code JSONObject}
     * @param jsonLine the raw JSON string to parse; may contain newlines
     * @return a {@code JSONObject} representing the parsed value, or a
     *         {@code JSONObject} with a {@code null} value if parsing fails
     */
    static @NotNull JSONObject parse(@NotNull String key, @Nullable String jsonLine) {
        if (jsonLine == null) {
            return new JSONObject(key);
        }
        String normalized = StringOperations.replaceAllNotInString(jsonLine, "\n", " ").trim();
        return new JSONObject(key, denull(parseValue(normalized)));
    }

    /**
     * Parses the lines of a JSON file into a {@link JSONObject} with the given key.
     * Lines are concatenated before parsing.
     *
     * @param key      the key to assign to the root {@code JSONObject}
     * @param contents the lines of the JSON file; must not be {@code null}
     * @return a {@code JSONObject} representing the parsed content, or a
     *         {@code JSONObject} with a {@code null} value if parsing fails
     */
    static @NotNull JSONObject parse(@NotNull String key, @Nullable Iterable<String> contents) {
        if (contents == null) {
            return new JSONObject(key);
        }
        StringBuilder sb = new StringBuilder();
        for (String line : contents) {
            if (line != null) {
                sb.append(line);
            }
        }
        return parse(key, sb.toString());
    }

    /**
     * Converts the internal null sentinel to Java {@code null}, and passes all
     * other values through unchanged. Used when storing a parsed value into a
     * {@link JSONObject}.
     *
     * @param value a parsed value, possibly {@link #NULL_SENTINEL}
     * @return {@code null} if {@code value} is {@link #NULL_SENTINEL}, otherwise
     *         {@code value} unchanged
     */
    private static @Nullable Object denull(@Nullable Object value) {
        return value == NULL_SENTINEL ? null : value;
    }

    /**
     * Parses a JSON value.
     *
     * <pre>
     * &lt;value&gt; ::= &lt;string&gt; | &lt;number&gt; | &lt;object&gt; | &lt;array&gt; | true | false | null
     * </pre>
     *
     * @param valueLine the string to parse; leading and trailing whitespace is
     *                  ignored
     * @return the parsed value ({@code String}, {@code Number},
     *         {@code List<JSONObject>}, {@code List<Object>}, {@code Boolean},
     *         or {@link #NULL_SENTINEL}), or Java {@code null} if the input is not
     *         a valid JSON value
     */
    private static @Nullable Object parseValue(@Nullable String valueLine) {
        if (!isValidLine(valueLine)) {
            return null;
        }
        valueLine = valueLine.trim();

        switch (valueLine) {
            case "true" -> {
                return Boolean.TRUE;
            }
            case "false" -> {
                return Boolean.FALSE;
            }
            case "null" -> {
                return NULL_SENTINEL;
            }
        }

        String parsedString = parseString(valueLine);
        if (parsedString != null) {
            return parsedString;
        }

        Number parsedNumber = parseNumber(valueLine);
        if (parsedNumber != null) {
            return parsedNumber;
        }

        List<JSONObject> parsedObject = parseObject(valueLine);
        if (parsedObject != null) {
            return parsedObject;
        }

        return parseArray(valueLine);
    }

    /**
     * Parses a JSON object.
     *
     * <pre>
     * &lt;object&gt; ::= { } | { &lt;members&gt; }
     * </pre>
     *
     * @param objectLine the string to parse; must start with {@code {} and end with
     *                   {@code }}
     * @return a {@code List<JSONObject>} representing the object's members
     *         (possibly empty), or {@code null} if the input is not a valid JSON
     *         object
     */
    private static @Nullable List<JSONObject> parseObject(@Nullable String objectLine) {
        if (!isValidLine(objectLine)) {
            return null;
        }
        objectLine = objectLine.trim();

        if (!objectLine.startsWith("{") || !objectLine.endsWith("}")) {
            return null;
        }
        String members = objectLine.substring(1, objectLine.length() - 1).trim();
        if (members.isEmpty()) {
            return new ArrayList<>();
        }
        return parseMembers(members);
    }

    /**
     * Parses JSON object members.
     *
     * <pre>
     * &lt;members&gt; ::= &lt;pair&gt; | &lt;pair&gt; , &lt;members&gt;
     * </pre>
     *
     * <p>
     * Duplicate keys within the same object are rejected, as required by the JSON
     * semantics rule.
     *
     * @param membersLine the string containing comma-separated key-value pairs
     * @return a {@code List<JSONObject>} of parsed pairs, or {@code null} if any
     *         pair is invalid or a duplicate key is found
     */
    private static @Nullable List<JSONObject> parseMembers(@Nullable String membersLine) {
        if (!isValidLine(membersLine)) {
            return null;
        }
        membersLine = membersLine.trim();

        String[] members = StringOperations.splitByStringNotNested(membersLine, ",");
        Set<String> seenKeys = new HashSet<>();
        List<JSONObject> result = new ArrayList<>();

        for (String pairString : members) {
            JSONObject parsed = parsePair(pairString);
            if (parsed == null) {
                return null;
            }
            if (!seenKeys.add(parsed.getKey())) {
                return null; // Duplicate key
            }
            result.add(parsed);
        }
        return result;
    }

    /**
     * Parses a JSON key-value pair.
     *
     * <pre>
     * &lt;pair&gt; ::= &lt;string&gt; : &lt;value&gt;
     * </pre>
     *
     * <p>
     * A {@code null} value (i.e., JSON {@code null}) is valid and will be stored as
     * Java {@code null} in the returned {@link JSONObject}.
     *
     * @param pairLine the string containing a single key-value pair
     * @return a {@code JSONObject} representing the pair, or {@code null} if the
     *         input is not a valid pair
     */
    private static @Nullable JSONObject parsePair(@Nullable String pairLine) {
        if (!isValidLine(pairLine)) {
            return null;
        }
        pairLine = pairLine.trim();

        String[] parts = StringOperations.splitByStringNotNested(pairLine, ":");
        if (parts.length != 2) {
            return null;
        }
        String rawKey = parts[0].trim();
        // Rejoin remaining parts in case the value itself contains a colon
        String rawValue = String.join(":", java.util.Arrays.copyOfRange(parts, 1, parts.length)).trim();

        String parsedKey = parseString(rawKey);
        if (!isValidLine(parsedKey)) {
            return null;
        }

        Object parsedValue = parseValue(rawValue);
        // parsedValue == null means parse failure; NULL_SENTINEL means JSON null
        if (parsedValue == null && !rawValue.equals("null")) {
            return null;
        }

        return new JSONObject(parsedKey, denull(parsedValue));
    }

    /**
     * Parses a JSON array.
     *
     * <pre>
     * &lt;array&gt; ::= [ ] | [ &lt;elements&gt; ]
     * </pre>
     *
     * @param arrayLine the string to parse; must start with {@code [} and end with
     *                  {@code ]}
     * @return a {@code List<Object>} representing the array elements (possibly
     *         empty), or {@code null} if the input is not a valid JSON array
     */
    private static @Nullable List<Object> parseArray(@Nullable String arrayLine) {
        if (!isValidLine(arrayLine)) {
            return null;
        }
        arrayLine = arrayLine.trim();
        if (!arrayLine.startsWith("[") || !arrayLine.endsWith("]")) {
            return null;
        }
        String elements = arrayLine.substring(1, arrayLine.length() - 1).trim();
        if (elements.isBlank()) {
            return new ArrayList<>();
        }
        return parseElements(elements);
    }

    /**
     * Parses JSON array elements.
     *
     * <pre>
     * &lt;elements&gt; ::= &lt;value&gt; | &lt;value&gt; , &lt;elements&gt;
     * </pre>
     *
     * @param elementsLine the string containing comma-separated values
     * @return a {@code List<Object>} of parsed values, or {@code null} if any
     *         element is invalid
     */
    private static @Nullable List<Object> parseElements(@Nullable String elementsLine) {
        if (!isValidLine(elementsLine)) {
            return null;
        }
        elementsLine = elementsLine.trim();

        String[] values = StringOperations.splitByStringNotNested(elementsLine, ",");
        List<Object> result = new ArrayList<>();

        for (String valueString : values) {
            Object parsed = parseValue(valueString.trim());
            // parsedValue == null means parse failure; NULL_SENTINEL means JSON null
            if (parsed == null && !valueString.trim().equals("null")) {
                return null;
            }
            result.add(denull(parsed));
        }
        return result;
    }

    /**
     * Parses a JSON string literal.
     *
     * <pre>
     * &lt;string&gt; ::= " " | " &lt;characters&gt; "
     * </pre>
     *
     * @param stringLine the string to parse; must start and end with {@code "}
     * @return the parsed string value (possibly empty), or {@code null} if the
     *         input is not a valid JSON string
     */
    private static @Nullable String parseString(@Nullable String stringLine) {
        if (!isValidLine(stringLine)) {
            return null;
        }
        stringLine = stringLine.trim();
        if (!stringLine.startsWith("\"") || !stringLine.endsWith("\"")) {
            return null;
        }
        stringLine = stringLine.substring(1, stringLine.length() - 1);
        if (stringLine.isEmpty()) {
            return ""; // Empty string
        }
        return parseCharacters(stringLine);
    }

    /**
     * Parses a sequence of JSON characters.
     *
     * <pre>
     * &lt;characters&gt; ::= &lt;character&gt; | &lt;character&gt; &lt;characters&gt;
     * </pre>
     *
     * @param charactersLine the string of characters to parse (without surrounding
     *                       quotes)
     * @return the parsed string, or {@code null} if any character is invalid
     */
    private static @Nullable String parseCharacters(@Nullable String charactersLine) {
        if (charactersLine == null || charactersLine.isEmpty()) {
            return null;
        }
        // Don't trim, because whitespace is part of the value

        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < charactersLine.length()) {
            if (charactersLine.charAt(i) == '\\') {
                // Determine escape sequence length
                if (i + 1 >= charactersLine.length()) {
                    return null; // Orphan escape character
                }
                int escapeEnd;
                if (charactersLine.charAt(i + 1) == 'u') {
                    escapeEnd = i + 6; // \u0000 hex
                } else {
                    escapeEnd = i + 2; // \0 char
                }
                if (escapeEnd > charactersLine.length()) {
                    return null; // Truncated sequence
                }
                String parsed = parseEscape(charactersLine.substring(i, escapeEnd));
                if (parsed == null) {
                    return null; // Malformed sequence
                }
                sb.append(parsed);
                i = escapeEnd;
            } else {
                int charLength = Character.charCount(charactersLine.codePointAt(i));
                String parsed = parseCharacter(charactersLine.substring(i, i + charLength));
                if (parsed == null) {
                    return null; // Malformed character
                }
                sb.append(parsed);
                i += charLength;
            }
        }
        return sb.toString();
    }

    /**
     * Parses a single JSON character.
     *
     * <pre>
     * &lt;character&gt; ::= any Unicode character except " or \ or control characters
     * </pre>
     *
     * @param characterLine a string containing exactly one Unicode code point
     * @return the character as a string, or {@code null} if the character is
     *         invalid (a control character, {@code "}, or {@code \})
     */
    private static @Nullable String parseCharacter(@Nullable String characterLine) {
        if (characterLine == null) {
            return null;
        }
        if (characterLine.startsWith("\\")) {
            return parseEscape(characterLine);
        }

        int charCount = characterLine.codePointCount(0, characterLine.length());
        if (charCount != 1) {
            return null; // Too many characters
        }
        int codePoint = characterLine.codePointAt(0);
        if (Character.isISOControl(codePoint)) {
            return null; // Invalid JSON character
        }
        if (codePoint == '"' || codePoint == '\\') {
            return null; // Invalid state - these should be parsed as escape characters
        }
        return characterLine;
    }

    /**
     * Parses a JSON escape sequence.
     *
     * <pre>
     * &lt;escape&gt; ::= \ (" | \ | / | b | f | n | r | t | u &lt;hex&gt;&lt;hex&gt;&lt;hex&gt;&lt;hex&gt;)
     * </pre>
     *
     * @param escapeLine the escape sequence string, starting with {@code \};
     *                   must not be trimmed (trimming could corrupt {@code \t}
     *                   etc.)
     * @return the resolved character as a string, or {@code null} if the escape
     *         sequence is invalid
     */
    private static @Nullable String parseEscape(@Nullable String escapeLine) {
        if (escapeLine == null || escapeLine.length() < 2) {
            return null;
        }
        if (escapeLine.charAt(0) != '\\') {
            return null; // Sequences must begin with the escape flag
        }
        if (escapeLine.charAt(1) == 'u') {
            if (escapeLine.length() < 6) {
                return null; // Truncated hex sequence
            }
            try {
                int codePoint = Integer.parseInt(escapeLine.substring(2, 6), 16);
                return new String(Character.toChars(codePoint));
            } catch (NumberFormatException e) {
                return null; // Malformed hex
            }
        }
        return switch (escapeLine.charAt(1)) {
            case '"' -> "\"";
            case '\\' -> "\\";
            case '/' -> "/";
            case 'b' -> "\b";
            case 'f' -> "\f";
            case 'n' -> "\n";
            case 'r' -> "\r";
            case 't' -> "\t";
            default -> null; // Invalid escape character
        };
    }

    /**
     * Parses a JSON number.
     *
     * <pre>
     * &lt;number&gt; ::= &lt;int&gt; &lt;frac&gt;? &lt;exp&gt;?
     * </pre>
     *
     * <p>
     * Structural validation is performed first to confirm the input matches the
     * JSON number grammar. If valid, the number is parsed by Java's standard
     * numeric parsers in order of precision: {@link Integer}, {@link Long},
     * {@link Double}.
     *
     * @param numberLine the string to parse as a number
     * @return an {@code Integer}, {@code Long}, or {@code Double} depending on the
     *         value's magnitude and form, or {@code null} if the input is not a
     *         valid JSON number
     */
    private static @Nullable Number parseNumber(@Nullable String numberLine) {
        if (!isValidLine(numberLine)) {
            return null;
        }
        numberLine = numberLine.trim();

        String upper = numberLine.toUpperCase();

        // Locate structural boundaries
        int fracIndex = upper.indexOf('.');
        int expIndex = upper.indexOf('E');

        int endExp = upper.length();
        int endFrac = expIndex != -1 ? expIndex : endExp;
        int endInt = fracIndex != -1 ? fracIndex : endFrac;

        // Validate integer part
        if (!isValidInt(upper.substring(0, endInt))) {
            return null;
        }
        // Validate fraction part
        if (fracIndex != -1 && !isValidFrac(upper.substring(fracIndex, endFrac))) {
            return null;
        }
        // Validate exponent part
        if (expIndex != -1 && !isValidExp(upper.substring(expIndex, endExp))) {
            return null;
        }

        // Delegate parsing to Java's standard parsers
        try {
            return Integer.valueOf(numberLine);
        } catch (NumberFormatException e1) {
            try {
                return Long.valueOf(numberLine);
            } catch (NumberFormatException e2) {
                try {
                    return Double.valueOf(numberLine);
                } catch (NumberFormatException e3) {
                    return null; // Malformed number
                }
            }
        }
    }

    /**
     * Validates that a line is not {@code null} and is not blank.
     * 
     * @param line the string to validate
     * @return {@code true} if the string is not null and is not blank,
     *         {@code false} otherwise
     */
    private static boolean isValidLine(@Nullable String line) {
        if (line == null) {
            return false;
        }
        return !line.isBlank();
    }

    /**
     * Validates the integer part of a JSON number.
     *
     * <pre>
     * &lt;int&gt; ::= -? &lt;digits&gt;
     * </pre>
     *
     * @param intString the substring representing the integer portion
     * @return {@code true} if the substring is a valid JSON integer part
     */
    private static boolean isValidInt(@Nullable String intString) {
        if (!isValidLine(intString)) {
            return false;
        }
        int start = 0;
        if (intString.charAt(0) == '-') {
            start = 1;
            if (intString.length() == 1) {
                return false;
            }
        }
        return isValidDigits(intString.substring(start));
    }

    /**
     * Validates the fractional part of a JSON number.
     *
     * <pre>
     * &lt;frac&gt; ::= . &lt;digits&gt;
     * </pre>
     *
     * @param fracStr the substring representing the fractional portion, including
     *                the leading {@code .}
     * @return {@code true} if the substring is a valid JSON fraction part
     */
    private static boolean isValidFrac(String fracStr) {
        if (fracStr == null || fracStr.length() < 2)
            return false;
        if (fracStr.charAt(0) != '.')
            return false;
        return isValidDigits(fracStr.substring(1));
    }

    /**
     * Validates the exponent part of a JSON number.
     *
     * <pre>
     * &lt;exp&gt; ::= (e | E) (+ | -)? &lt;digits&gt;
     * </pre>
     *
     * @param expStr the substring representing the exponent portion, including the
     *               leading {@code e} or {@code E}
     * @return {@code true} if the substring is a valid JSON exponent part
     */
    private static boolean isValidExp(String expStr) {
        if (expStr == null || expStr.length() < 2)
            return false;
        char first = expStr.charAt(0);
        if (first != 'e' && first != 'E')
            return false;
        int start = 1;
        if (expStr.charAt(1) == '+' || expStr.charAt(1) == '-') {
            start = 2;
            if (expStr.length() < 3)
                return false;
        }
        return isValidDigits(expStr.substring(start));
    }

    /**
     * Validates that a string consists entirely of decimal digits.
     *
     * <pre>
     * &lt;digits&gt; ::= &lt;digit&gt; | &lt;digit&gt; &lt;digits&gt;
     * </pre>
     *
     * @param digitsString the string to validate
     * @return {@code true} if the string is non-empty and contains only digits
     *         {@code 0}–{@code 9}
     */
    private static boolean isValidDigits(@Nullable String digitsString) {
        if (!isValidLine(digitsString)) {
            return false;
        }
        for (char c : digitsString.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

}
