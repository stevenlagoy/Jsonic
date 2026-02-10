package com.stevenlagoy.jsonic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JSONStringifier {

    /*
     * JSON BNF Grammar
     * <json> ::= <value>
     * <value> ::= <string> | <number> | <object> | <array> | true | false | null
     *
     * <object> ::= { } | { <members> }
     * <members> ::= <pair> | <pair> , <members>
     * <pair> ::= <string> : <value>
     * semantics rule: <string> must be unique within its level
     *
     * <array> ::= [ ] | [ <elements> ]
     * <elements> ::= <value> | <value> , <elements>
     *
     * <string> ::= " <characters> "
     * <characters> ::= <character> | <character> <characters>
     * <character> ::= # any unicode character except " or \ or control characters #
     * | <escape>
     *
     * <escape> ::= \ (" | \ | / | b | f | n | r | t | u <hex><hex><hex><hex>)
     *
     * <number> ::= <int> <frac>? <exp>?
     * <int> ::= -? <digits>
     * <frac> ::= . <digits>
     * <exp> ::= (e | E) (+ | -)? <digits>
     * <digits> ::= <digit> | <digit> <digits>
     * <digit> ::= # digit from 0 to 9 #
     * <hex> ::= <digit> | [a-f] | [A-F]
     */

    public static String stringifyJson(JSONObject json) {
        String result = stringifyObject(json.getAsObject());
        return result.substring(1, result.length() - 1);
    }

    public static String stringifyValue(Object value, Class<?> type) {
        if (type.equals(JSONObject.class)) {
            @SuppressWarnings("unchecked") // The type of this object is known from the type parameter
            List<JSONObject> objects = (List<JSONObject>) value;
            StringBuilder result = new StringBuilder();
            for (JSONObject object : objects)
                result.append(stringifyObject(object));
            return result.toString();
        }
        if (type.equals(ArrayList.class)) {
            return stringifyArray((List<?>) value);
        }
        return stringifyValue(value);
    }

    public static String stringifyValue(Object value) {
        if (value instanceof String)
            return "\"" + stringifyEscape((String) value) + "\"";
        else if (value instanceof Number || value instanceof Boolean)
            return value.toString();
        else if (value instanceof Jsonic)
            return "\"" + ((Jsonic) value).toJson().toString() + "\"";
        else if (value.getClass() == Object.class)
            return "null"; // Only Object, not subclass
        else
            return "\"" + value.toString() + "\"";
    }

    public static String stringifyObject(JSONObject object) {
        if (object == null)
            return "{}";

        StringBuilder sb = new StringBuilder();
        sb.append("{");

        sb.append(stringifyValue(object.getKey()));
        sb.append(" : ");

        Object value = object.getValue();
        Class<?> type = object.getType();

        if (value == null)
            sb.append("null");
        else if (type != null && type.equals(JSONObject.class) && value instanceof List) {
            @SuppressWarnings("unchecked")
            List<JSONObject> objects = (List<JSONObject>) value;
            sb.append("{");
            for (int i = 0; i < objects.size(); i++) {
                String nested = stringifyObject(objects.get(i));
                sb.append(nested.substring(1, nested.length() - 1));
                if (i < objects.size() - 1)
                    sb.append(", ");
            }
            sb.append("}");
        } else if (type != null && value instanceof JSONObject json) {
            sb.append("{");
            sb.append(json.toString());
            sb.append("}");
        } else if (type != null)
            sb.append(stringifyValue(value, type));
        else
            sb.append(stringifyValue(value));

        sb.append("}");

        return sb.toString();
    }

    public static String stringifyArray(List<?> array) {
        if (array == null || array.isEmpty())
            return "[]";

        StringBuilder sb = new StringBuilder();
        sb.append("[");
        Iterator<?> it = array.iterator();
        while (it.hasNext()) {
            sb.append(stringifyValue(it.next()));
            if (it.hasNext())
                sb.append(", ");
        }
        sb.append("]");

        return sb.toString();
    }

    public static String stringifyEscape(String escape) {
        return escape
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static List<String> expandJson(String json) {
        List<String> result = new ArrayList<>();
        int indentation = 0;
        String tab = "	";
        StringBuilder currentLine = new StringBuilder();

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (StringOperations.isInString(json, i) && c != '"') {
                currentLine.append(c);
                continue;
            }
            switch (c) {
                case '"':
                    currentLine.append(c);
                    break;
                case '{':
                case '[':
                    currentLine.append(c);
                    result.add(tab.repeat(indentation) + currentLine);
                    currentLine = new StringBuilder();
                    indentation++;
                    break;
                case '}':
                case ']':
                    if (currentLine.length() > 0) {
                        result.add(tab.repeat(indentation) + currentLine);
                        currentLine = new StringBuilder();
                    }
                    indentation--;
                    if (i + 1 < json.length() && json.charAt(i + 1) == ',') {
                        result.add(tab.repeat(indentation) + c + ",");
                        i++;
                    } else
                        result.add(tab.repeat(indentation) + c);
                    break;
                case ',':
                    currentLine.append(c);
                    result.add(tab.repeat(indentation) + currentLine);
                    currentLine = new StringBuilder();
                    break;
                case ':':
                    currentLine.append(" ").append(c).append(" ");
                    break;
                default:
                    if (!Character.isWhitespace(c)) {
                        currentLine.append(c);
                    }
            }
        }

        if (currentLine.length() > 0) {
            result.add(tab.repeat(indentation) + currentLine);
        }
        return result;
    }

}
