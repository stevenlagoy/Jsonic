package com.stevenlagoy.jsonic;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * JSONProcessor turns strings representing a valid JSON file into a JSONObject.
 */
public class JSONProcessor {

    private JSONProcessor() {
    }

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

    /**
     * Process the data in the file with the given path into a JSONObject.
     * 
     * @param path Path to a JSONFile.
     * @return JSONObject parsed from the file.
     */
    public static JSONObject processJson(Path path) {
        if (path == null)
            return null;
        List<String> contents = FileOperations.readFile(path);
        Path fileName = path.getFileName();
        if (fileName == null)
            return null;
        String key = fileName.toString().split("\\.")[0];
        return processJson(key, contents);
    }

    /**
     * Process the data in the String Iterable into a JSONObject, and give it the
     * passed key.
     * 
     * @param key      The key to give to the parsed JSONObject.
     * @param contents Data to parse into JSONObject
     * @return The created JSONObject with the passed key.
     */
    public static JSONObject processJson(String key, Iterable<String> contents) {
        if (contents == null)
            return null;
        StringBuilder sb = new StringBuilder();
        for (String line : contents) {
            if (line != null)
                sb.append(line);
        }
        return processJson(key, sb.toString());
    }

    /**
     * Processes a JSON string by normalizing whitespace and validating the object
     * structure.
     * This method processes a JSON string by:
     * <ol>
     * <li>Replacing all newlines outside of strings with spaces</li>
     * <li>Trimming leading and trailing whitespace</li>
     * <li>Validating the resulting string as a JSON object</li>
     * </ol>
     * <p>
     * The input must represent a valid JSON value containing at least one of the
     * following:
     * <ul>
     * <li>String: Characters surrounded by double quotes
     * <li>Number: Number with integer and possible fraction and/or exponent part
     * <li>Object: Key-Value pairs separated by commas, all surrouned by curly
     * braces { }
     * <li>Array: Values separated by commas, all surrounded by square brackets [ ]
     * <li>true
     * <li>false
     * <li>null
     * </ul>
     *
     * @param key      Key of the new JSONObject.
     * @param jsonLine
     *                 The JSON string to process (may contain newlines).
     *
     * @return {@code true} if the string represents a valid JSON object,
     *         {@code false} otherwise.
     */
    public static JSONObject processJson(String key, String jsonLine) {
        String processed = StringOperations.replaceAllNotInString(jsonLine, "\n", " ").trim(); // Put the while
                                                                                               // structure on one line
        return new JSONObject(key, processValue(processed));
    }

    /**
     * Processes an Object.
     *
     * <pre>
     * &lt;object&gt; ::= { } | { &lt;members&gt; }
     * </pre>
     *
     * @param objectLine
     *                   The line containing the full object to process (must not be
     *                   null or empty or blank)
     *
     * @return {@code true} if the object is successfully processed, {@code false}
     *         otherwise
     */
    public static List<JSONObject> processObject(String objectLine) {
        if (objectLine == null)
            return null;
        objectLine = objectLine.trim();
        if (objectLine.isEmpty())
            return null;

        if (!objectLine.startsWith("{") || !objectLine.endsWith("}"))
            return null;
        String members = objectLine.substring(1, objectLine.length() - 1).trim();
        if (members.isEmpty())
            return new ArrayList<>();
        return processMembers(members);
    }

    /**
     * Processes members
     *
     * <pre>
     * &lt;members&gt; ::= &lt;pair&gt; | &lt;pair&gt; , &lt;members&gt;
     * </pre>
     *
     * @param membersLine
     *                    The line contining the full members to process
     *
     * @return {@code true} if the members are successfully processed, {@code false}
     *         otherwise
     */
    public static List<JSONObject> processMembers(String membersLine) {
        if (membersLine == null)
            return null;
        membersLine = membersLine.trim();
        if (membersLine.isEmpty())
            return null;

        Set<String> keys = new TreeSet<>();
        String[] members = StringOperations.splitByStringNotNested(membersLine, ",");

        List<JSONObject> result = new ArrayList<>();
        for (String pair : members) {
            JSONObject processed = processPair(pair);
            if (processed == null)
                return null;

            String[] parts = StringOperations.splitByStringNotNested(pair.trim(), ":");
            String key = parts[0].trim();
            key = key.substring(1, key.length() - 1); // Remove the surrounding quotes

            // Check for duplicates
            if (!keys.add(key))
                return null;
            result.add(processed);
        }
        return result;
    }

    /**
     * Processes pair
     *
     * <pre>
     * &lt;pair&gt; ::= &lt;string&gt; : &lt;value&gt;
     * </pre>
     *
     * @param pairLine
     *                 The line contining the full pair to process (must not be null
     *                 or empty or blank)
     *
     * @return {@code true} if the pair is successfully processed, {@code false}
     *         otherwise
     */
    public static JSONObject processPair(String pairLine) {
        if (pairLine == null)
            return null;
        pairLine = pairLine.trim();
        if (pairLine.isEmpty())
            return null;

        String[] parts = StringOperations.splitByStringNotNested(pairLine, ":");
        if (parts.length != 2)
            return null;
        String key = parts[0].trim();
        String value = parts[1].trim();

        String processedKey = processString(key);
        Object processedValue = processValue(value);

        if (processedKey == null || processedValue == null)
            return null;
        return new JSONObject(processedKey, processedValue);
    }

    /**
     * Processes array
     *
     * <pre>
     * &lt;array&gt; ::= [ ] | [ &lt;elements&gt; ]
     * </pre>
     *
     * @param arrayLine
     *                  The line contining the full array to process (may not be
     *                  null or empty or blank)
     *
     * @return {@code true} if the array is successfully processed, {@code false}
     *         otherwise
     */
    public static List<Object> processArray(String arrayLine) {
        if (arrayLine == null)
            return null;
        arrayLine = arrayLine.trim();
        if (arrayLine.isEmpty())
            return null;

        if (!arrayLine.startsWith("[") || !arrayLine.endsWith("]"))
            return null;
        String elements = arrayLine.substring(1, arrayLine.length() - 1).trim();
        if (elements.isEmpty())
            return new ArrayList<Object>();
        return processElements(elements);
    }

    /**
     * Processes elements
     *
     * <pre>
     * &lt;elements&gt; ::= &lt;value&gt; | &lt;value&gt; , &lt;elements&gt;
     * </pre>
     *
     * @param elementsLine
     *                     The line contining the full elements to process (must not
     *                     be null or empty or blank)
     *
     * @return {@code true} if the elements are successfully processed,
     *         {@code false} otherwise
     */
    public static List<Object> processElements(String elementsLine) {
        if (elementsLine == null)
            return null;
        elementsLine = elementsLine.trim();
        if (elementsLine.isEmpty())
            return null;

        List<Object> result = new ArrayList<>();
        String[] values = StringOperations.splitByStringNotNested(elementsLine, ",");
        for (String value : values) {
            Object processedValue = processValue(value);
            if (processedValue == null)
                return null;
            result.add(processedValue);
        }
        return result;
    }

    /**
     * Processes value
     *
     * <pre>
     * &lt;value&gt; ::= &lt;string&gt; | &lt;number&gt; | &lt;object&gt; | &lt;array&gt; | true | false | null
     * </pre>
     *
     * @param valueLine
     *                  The line contining the full value to process (may not be
     *                  null or empty or blank)
     *
     * @return {@code true} if the value is successfully processed, {@code false}
     *         otherwise
     */
    public static Object processValue(String valueLine) {
        if (valueLine == null)
            return null;
        valueLine = valueLine.trim();
        if (valueLine.isEmpty())
            return null;

        if (valueLine.equals("true"))
            return Boolean.TRUE;
        if (valueLine.equals("false"))
            return Boolean.FALSE;
        if (valueLine.equals("null"))
            return new Object();
        String processedString = processString(valueLine);
        if (processedString != null)
            return processedString;
        Number processedNumber = processNumber(valueLine);
        if (processedNumber != null)
            return processedNumber;
        List<JSONObject> processedObject = processObject(valueLine);
        if (processedObject != null)
            return processedObject;
        List<Object> processedArray = processArray(valueLine);
        if (processedArray != null)
            return processedArray;

        return null;
    }

    /**
     * Processes string
     *
     * <pre>
     * &lt;string&gt; ::= " " | " &lt;characters&gt; "
     * </pre>
     *
     * @param stringLine
     *                   The line contining the full string to process (may not be
     *                   null or empty, but may be blank)
     *
     * @return {@code true} if the string is successfully processed, {@code false}
     *         otherwise
     */
    public static String processString(String stringLine) {
        if (stringLine == null)
            return null;
        if (stringLine.isEmpty())
            return null;

        if (stringLine.length() < 2)
            return null;
        if (!stringLine.startsWith("\"") || !stringLine.endsWith("\""))
            return null;
        if (stringLine.length() < 3)
            return "";
        return processCharacters(stringLine.substring(1, stringLine.length() - 1));
    }

    /**
     * Processes characters
     *
     * <pre>
     * &lt;characters&gt; ::= &lt;character&gt; | &lt;character&gt; &lt;characters&gt;
     * </pre>
     *
     * @param charactersLine
     *                       The line contining the full characters to process (must
     *                       not be null or empty, but may be blank)
     *
     * @return {@code true} if the characters are successfully processed,
     *         {@code false} otherwise
     */
    public static String processCharacters(String charactersLine) {
        if (charactersLine == null)
            return null;
        if (charactersLine.isEmpty())
            return null;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < charactersLine.length();) {
            if (charactersLine.charAt(i) == '\\') {
                int escapeEnd = i + 1;
                if (escapeEnd >= charactersLine.length())
                    return null;

                if (charactersLine.charAt(escapeEnd) == 'u') {
                    escapeEnd += 4;
                }
                escapeEnd++;

                if (escapeEnd > charactersLine.length())
                    return null;

                String processedEscape = processEscape(charactersLine.substring(i, escapeEnd));

                if (processedEscape == null)
                    return null;
                sb.append(processedEscape);
                i = escapeEnd;
                continue;
            }
            int charLength = Character.charCount(charactersLine.codePointAt(i));
            String processedCharacter = processCharacter(charactersLine.substring(i, i + charLength));
            if (processedCharacter == null)
                return null;
            sb.append(processedCharacter);
            i += charLength;
        }

        return sb.toString();
    }

    /**
     * Processes character
     *
     * <pre>
     * &lt;character&gt; ::= # any unicode character except "" or \ or control characters # | &lt;escape&gt;
     * </pre>
     *
     * @param characterLine
     *                      The line contining the full character to process (must
     *                      not be null)
     *
     * @return {@code true} if the character is successfully processed,
     *         {@code false} otherwise
     */
    public static String processCharacter(String characterLine) {
        if (characterLine == null)
            return null;

        if (characterLine.startsWith("\\"))
            return processEscape(characterLine);

        int charCount = characterLine.codePointCount(0, characterLine.length());
        if (charCount != 1)
            return null;

        int codePoint = characterLine.codePointAt(0);

        if (Character.isISOControl(codePoint))
            return null;
        if (codePoint == '\"' || codePoint == '\\')
            return null;

        return characterLine;
    }

    /**
     * Processes escape character
     *
     * <pre>
     * &lt;escape&gt; ::= \ (" | \ | / | b | f | n | r | t | u &lt;hex&gt;&lt;hex&gt;&lt;hex&gt;&lt;hex&gt;)
     * </pre>
     *
     * @param escapeLine
     *                   The line contining the full escape to process (must not be
     *                   null or empty or blank)
     *
     * @return {@code true} if the escape is successfully processed, {@code false}
     *         otherwise
     */
    public static String processEscape(String escapeLine) {
        if (escapeLine == null)
            return null;
        escapeLine = escapeLine.trim();
        if (escapeLine.isEmpty())
            return null;

        if (escapeLine.charAt(0) != '\\')
            return null;
        if (escapeLine.length() < 2)
            return null;
        // process special character hex
        if (escapeLine.charAt(1) == 'u') {
            int codePoint = Integer.parseInt(escapeLine.substring(2), 16);
            return new String(Character.toChars(codePoint));
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
            default -> null;
        };
    }

    /**
     * Processes number
     *
     * <pre>
     * &lt;number&gt; ::= &lt;int&gt; &lt;frac&gt;? &lt;exp&gt;?
     * </pre>
     *
     * @param numberLine
     *                   The line contining the full number to process (must not be
     *                   null or empty or blank)
     *
     * @return {@code true} if the number is successfully processed, {@code false}
     *         otherwise
     */
    public static Number processNumber(String numberLine) {
        if (numberLine == null)
            return null;
        numberLine = numberLine.trim().toUpperCase();
        if (numberLine.isEmpty())
            return null;
        // Identify portions
        int beginInt = 0;
        int beginFrac = numberLine.indexOf(".");
        int beginExp = numberLine.indexOf("E");

        int endExp = numberLine.length();
        int endFrac = beginExp != -1 ? beginExp : endExp;
        int endInt = beginFrac != -1 ? beginFrac : endFrac;

        // Parse integer part
        Integer processedInt = processInt(numberLine.substring(beginInt, endInt));
        if (processedInt == null)
            return null;
        // Parse fraction part
        if (beginFrac != -1 && beginFrac < endFrac) {
            Double processedFrac = processFrac(numberLine.substring(beginFrac, endFrac));
            if (processedFrac == null)
                return null;
        }
        // Parse exponent part
        if (beginExp != -1 && beginExp < endExp) {
            Integer processedExp = processExp(numberLine.substring(beginExp, endExp));
            if (processedExp == null)
                return null;
        }
        try {
            return Integer.valueOf(numberLine);
        } catch (NumberFormatException e1) {
            try {
                return Long.valueOf(numberLine);
            } catch (NumberFormatException e2) {
                try {
                    return Double.valueOf(numberLine);
                } catch (NumberFormatException e3) {
                    return null;
                }
            }
        }
    }

    /**
     * Processes integer
     *
     * <pre>
     * &lt;int&gt; ::= -? &lt;digits&gt;
     * </pre>
     *
     * @param intLine
     *                The line contining the full int to process (must not be null
     *                or empty or blank)
     *
     * @return {@code true} if the int is successfully processed, {@code false}
     *         otherwise
     */
    public static Integer processInt(String intLine) {
        if (intLine == null)
            return null;
        intLine = intLine.trim();
        if (intLine.isEmpty())
            return null;
        if (intLine.charAt(0) == '-') {
            return processDigits(intLine.substring(1));
        }
        return processDigits(intLine);
    }

    /**
     * Processes fraction
     *
     * <pre>
     * &lt;frac&gt; ::= . &lt;digits&gt;
     * </pre>
     *
     * @param fracLine
     *                 The line contining the full frac to process (must not be null
     *                 or empty or blank)
     *
     * @return {@code true} if the frac is successfully processed, {@code false}
     *         otherwise
     */
    public static Double processFrac(String fracLine) {
        if (fracLine == null)
            return null;
        fracLine = fracLine.trim();
        if (fracLine.isEmpty())
            return null;

        if (fracLine.length() < 2)
            return null;
        if (fracLine.charAt(0) != '.')
            return null;
        return Double.valueOf(processDigits(fracLine.substring(1)));
    }

    /**
     * Processes exponent
     *
     * <pre>
     * &lt;exp&gt; ::= (e | E) (+ | -)? &lt;digits&gt;
     * </pre>
     *
     * @param expLine
     *                The line contining the full exp to process (must not be null
     *                or empty or blank)
     *
     * @return {@code true} if the exp is successfully processed, {@code false}
     *         otherwise
     */
    public static Integer processExp(String expLine) {
        if (expLine == null)
            return null;
        expLine = expLine.trim();
        if (expLine.isEmpty())
            return null;
        if (expLine.length() < 2)
            return null;
        if (expLine.charAt(0) != 'e' && expLine.charAt(0) != 'E')
            return null;
        if (expLine.charAt(1) == '+' || expLine.charAt(1) == '-') {
            return processDigits(expLine.substring(2));
        }
        return processDigits(expLine.substring(1));
    }

    /**
     * Processes digits
     *
     * <pre>
     * &lt;digits&gt; ::= &lt;digit&gt; | &lt;digit&gt; &lt;digits&gt;
     * </pre>
     *
     * @param digitsLine
     *                   The line contining the full digits to process (must not be
     *                   null or empty or blank)
     *
     * @return The decimal value of the digits, or {@code -1} if invalid
     */
    public static int processDigits(String digitsLine) {
        if (digitsLine == null)
            return -1;
        digitsLine = digitsLine.trim();
        if (digitsLine.isEmpty())
            return -1;
        int result = 0;
        for (int i = digitsLine.length() - 1; i > 0; i--) {
            int e = 0;
            char digit = digitsLine.charAt(i);
            int processed = processDigit(digit);
            if (processed == -1)
                return -1;
            result += Math.pow(10, e) * processed;
        }
        return result;
    }

    /**
     * Processes a digit
     *
     * <pre>
     * &lt;digit&gt; ::= # digit from 0 to 9 #
     * </pre>
     *
     * @param digit
     *              The digit character to process
     *
     * @return The decimal value of the digit, or {@code -1} if invalid
     */
    public static int processDigit(char digit) {
        if (Character.isDigit(digit)) {
            return digit - '0';
        }
        return -1;
    }

    /**
     * Processes a hex
     *
     * <pre>
     * &lt;hex&gt; ::= &lt;digit&gt; | [a-f] [A-F]
     * </pre>
     *
     * @param hex
     *            The hexidecimal character to process
     *
     * @return The decimal value of the hex character, or {@code -1} if invalid
     */
    public static int processHex(char hex) {
        if (Character.isDigit(hex)) {
            return hex - '0';
        }
        if (hex >= 'a' && hex <= 'f') {
            return hex - 'a' + 10;
        }
        if (hex >= 'A' && hex <= 'F') {
            return hex - 'A' + 10;
        }
        return -1;
    }

}
