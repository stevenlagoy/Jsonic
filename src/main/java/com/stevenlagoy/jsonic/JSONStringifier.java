package com.stevenlagoy.jsonic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Internal utility class responsible for converting a {@link JSONObject} tree
 * into its JSON string representation.
 * <p>
 * This class is not part of the public API. The only intended entry point is
 * {@link JSONObject#toString()}, which calls {@link #stringify(JSONObject)}.
 * <p>
 * The output of {@link #stringify(JSONObject)} is a compact single-line JSON
 * string. {@link #expand(String)} can be used to produce a human-readable,
 * indented form of that string.
 */
class JSONStringifier {

    private JSONStringifier() {
    }

    /**
     * Converts the given {@link JSONObject} into a compact JSON string.
     * <p>
     * The root node's key is not included in the output — only its value is
     * stringified. This matches the expectation that a root node wraps a complete
     * JSON value.
     *
     * @param json the root {@code JSONObject} to stringify; must not be
     *             {@code null}
     * @return a compact JSON string representation of the node's value
     */
    static String stringify(JSONObject json) {
        return stringifyValue(json.getValue());
    }

    /**
     * Expands a compact JSON string into a human-readable, indented form.
     * Returns a list of lines, one structural element per line, indented with
     * tabs to reflect nesting depth.
     * <p>
     * Characters inside string literals are passed through unchanged.
     *
     * @param json a compact JSON string, as produced by {@link #stringify}
     * @return a list of indented lines representing the formatted JSON
     */
    static List<String> expand(String json) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        StringBuilder currentLine = new StringBuilder();

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            // Inside a string: pass characters through as-is, except for the closing quote
            // which falls through to the '"' case below.
            if (StringOperations.isInString(json, i) && c != '"') {
                currentLine.append(c);
                continue;
            }

            switch (c) {
                case '"' -> currentLine.append(c);
                case '{', '[' -> {
                    currentLine.append(c);
                    result.add("\t".repeat(depth) + currentLine);
                    currentLine = new StringBuilder();
                    depth++;
                }
                case '}', ']' -> {
                    if (!currentLine.isEmpty()) {
                        result.add("\t".repeat(depth) + currentLine);
                        currentLine = new StringBuilder();
                    }
                    depth--;
                    // Consume a trailing comma on the closing bracket if present
                    if (i + 1 < json.length() && json.charAt(i + 1) == ',') {
                        result.add("\t".repeat(depth) + c + ",");
                        i++;
                    } else {
                        result.add("\t".repeat(depth) + c);
                    }
                }
                case ',' -> {
                    currentLine.append(c);
                    result.add("\t".repeat(depth) + currentLine);
                    currentLine = new StringBuilder();
                }
                case ':' -> currentLine.append(": ");
                default -> {
                    if (!Character.isWhitespace(c)) {
                        currentLine.append(c);
                    }
                }
            }
        }

        if (!currentLine.isEmpty()) {
            result.add("\t".repeat(depth) + currentLine);
        }
        return result;
    }

    /**
     * Stringifies a single JSON value of any valid type.
     * <p>
     * Dispatches to the appropriate method based on the runtime type of
     * {@code value}:
     * <ul>
     * <li>{@code null} → {@code "null"}</li>
     * <li>{@code String} → quoted and escaped string</li>
     * <li>{@code Number} or {@code Boolean} → {@link Object#toString()}</li>
     * <li>{@code List<JSONObject>} → JSON object body {@code {...}}</li>
     * <li>{@code List<?>} → JSON array {@code [...]}</li>
     * <li>{@code JSONObject} → nested object</li>
     * </ul>
     *
     * @param value the value to stringify; may be {@code null}
     * @return the JSON string representation of the value
     */
    private static String stringifyValue(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String s) {
            return "\"" + escapeString(s) + "\"";
        } else if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof List<?> list) {
            return stringifyList(list);
        } else if (value instanceof JSONObject json) {
            // Edge case from manual construction: never happens by reading a JSON file
            return stringifyObject(json);
        } else if (value instanceof JSONSerializable<?> serializable) {
            return stringifyObject(serializable.toJson());
        }
        // Fallback: stringify unknown types as quoted strings
        return "\"" + escapeString(value.toString()) + "\"";
    }

    /**
     * Stringifies a {@link List}, dispatching to either object or array form based
     * on the content of the list.
     * <p>
     * A list whose first element is a {@link JSONObject} is treated as a JSON
     * object body (a sequence of key-value pairs). Any other list is treated as a
     * JSON array. Due to type erasure, empty lists will be assumed to be a JSON
     * array rather than a JSON object, producing {@code []} rather than {@code {}}.
     * If a list should be treated as a JSON object, it is recommended to put a
     * marker object inside, like {@code {"_": ""}}.
     *
     * @param list the list to stringify; must not be {@code null}
     * @return a JSON object body {@code {...}} or array {@code [...]} string
     */
    private static String stringifyList(List<?> list) {
        if (list.isEmpty())
            return "[]";
        if (list.get(0) instanceof JSONObject)
            return stringifyObjectBody(list);
        return stringifyArray(list);
    }

    /**
     * Stringifies a list of {@link JSONObject}s as a JSON object body.
     * <p>
     * Each element is rendered as a key-value pair separated by commas, all
     * wrapped in curly braces.
     *
     * @param list a non-empty list whose elements are all {@link JSONObject}s
     * @return a JSON object string {@code {"key": value, ...}}
     */
    @SuppressWarnings("unchecked")
    private static String stringifyObjectBody(List<?> list) {
        List<JSONObject> objects = (List<JSONObject>) list;
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < objects.size(); i++) {
            sb.append(stringifyPair(objects.get(i)));
            if (i < objects.size() - 1)
                sb.append(", ");
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Stringifies a single {@link JSONObject} as a JSON key-value pair.
     * <p>
     * The key is rendered as a quoted string followed by {@code :} and the
     * stringified value.
     *
     * @param object the object to render as a pair; must not be {@code null}
     * @return a JSON key-value pair string {@code "key": value}
     */
    private static String stringifyPair(JSONObject object) {
        return "\"" + escapeString(object.getKey()) + "\": "
                + stringifyValue(object.getValue());
    }

    /**
     * Stringifies a single nested {@link JSONObject} as a JSON object. If the
     * object's value is a list of {@link JSONObject}s, it is rendered as a full
     * object body. Otherwise, it is rendered as a single key-value pair wrapped
     * in braces.
     *
     * @param object the {@code JSONObject} to stringify; must not be {@code null}
     * @return a JSON object string
     */
    private static String stringifyObject(JSONObject object) {
        Object value = object.getValue();
        if (value instanceof List<?> list && !list.isEmpty()
                && list.get(0) instanceof JSONObject) {
            return stringifyObjectBody(list);
        }
        return "{" + stringifyPair(object) + "}";
    }

    /**
     * Stringifies a list of primitive JSON values as a JSON array.
     *
     * @param array the list to render as a JSON array; must not be {@code null}
     * @return a JSON array string {@code [value, ...]}
     */
    private static String stringifyArray(List<?> array) {
        if (array.isEmpty())
            return "[]";
        StringBuilder sb = new StringBuilder("[");
        Iterator<?> it = array.iterator();
        while (it.hasNext()) {
            sb.append(stringifyValue(it.next()));
            if (it.hasNext())
                sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Escapes special characters in a string for inclusion in a JSON string
     * literal. Processes characters in an order that avoids double-escaping:
     * backslashes are escaped first so that subsequently added backslashes from
     * other replacements are not re-escaped.
     *
     * @param s the raw string to escape; must not be {@code null}
     * @return the escaped string, safe for embedding between JSON double quotes
     */
    private static String escapeString(String s) {
        return s
                .replace("\\", "\\\\") // must be first
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

}