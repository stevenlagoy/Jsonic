package com.stevenlagoy.jsonic;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/** JSONValidator determines the validity of a JSON file. */
public class JSONValidator {

    private JSONValidator() {
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
     * Determines whether the JSON file with the given path is valid.
     * 
     * @param path Path to a file.
     * @return {@code true} if valid, {@code false} otherwise.
     */
    public static boolean validateJson(Path path) {
        List<String> contents = FileOperations.readFile(path);
        return validateJson(contents);
    }

    /**
     * Determines whether the JSON data represented by the passed String Iterator is
     * valid.
     * 
     * @param contents Iterable collection of Strings.
     * @return {@code true} if valid, {@code false} otherwise.
     */
    public static boolean validateJson(Iterable<String> contents) {
        if (contents == null)
            return false;
        StringBuilder sb = new StringBuilder();
        for (String line : contents) {
            if (line != null)
                sb.append(line);
        }
        return validateJson(sb.toString());
    }

    /**
     * Validates a JSON string by normalizing whitespace and validating the object
     * structure.
     * This method processes a JSON string by:
     * <ol>
     * <li>Replacing all newlines outside of strings with spaces</li>
     * <li>Trimming leading and trailing whitespace</li>
     * <li>Validating the resulting string as a JSON object</li>
     * </ol>
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
     * @param jsonLine
     *                 The JSON string to validate (may contain newlines)
     *
     * @return {@code true} if the string represents a valid JSON object,
     *         {@code false} otherwise
     *
     * @see JSONValidator#validateObject(String)
     */
    public static boolean validateJson(String jsonLine) {
        String processed = StringOperations.replaceAllNotInString(jsonLine, "\n", " ").trim(); // Put the while
                                                                                               // structure on one line
        return validateValue(processed);
    }

    /**
     * Validates an Object.
     *
     * <pre>
     * &lt;object&gt; ::= { } | { &lt;members&gt; }
     * </pre>
     *
     * @param objectLine
     *                   The line containing the full object to parse (must not be
     *                   null or empty or blank)
     *
     * @return {@code true} if the object is successfully parsed, {@code false}
     *         otherwise
     */
    public static boolean validateObject(String objectLine) {
        if (objectLine == null)
            return false;
        objectLine = objectLine.trim();
        if (objectLine.isEmpty())
            return false;

        if (!objectLine.startsWith("{") || !objectLine.endsWith("}"))
            return false;
        String members = objectLine.substring(1, objectLine.length() - 1).trim();
        if (members.isEmpty())
            return true;
        return validateMembers(members);
    }

    /**
     * Validates members
     *
     * <pre>
     * &lt;members&gt; ::= &lt;pair&gt; | &lt;pair&gt; , &lt;members&gt;
     * </pre>
     *
     * @param membersLine
     *                    The line contining the full members to parse
     *
     * @return {@code true} if the members are successfully parsed, {@code false}
     *         otherwise
     */
    public static boolean validateMembers(String membersLine) {
        if (membersLine == null)
            return false;
        membersLine = membersLine.trim();
        if (membersLine.isEmpty())
            return false;

        Set<String> keys = new HashSet<>();
        String[] members = StringOperations.splitByStringNotNested(membersLine, ",");

        for (String pair : members) {
            if (!validatePair(pair))
                return false;

            String[] parts = StringOperations.splitByStringNotNested(pair.trim(), ":");
            String key = parts[0].trim();
            key = key.substring(1, key.length() - 1); // Remove the surrounding quotes

            // Check for duplicates
            if (!keys.add(key))
                return false;
        }
        return true;
    }

    /**
     * Validates pair
     *
     * <pre>
     * &lt;pair&gt; ::= &lt;string&gt; : &lt;value&gt;
     * </pre>
     *
     * @param pairLine
     *                 The line contining the full pair to parse (must not be null
     *                 or empty or blank)
     *
     * @return {@code true} if the pair is successfully parsed, {@code false}
     *         otherwise
     */
    public static boolean validatePair(String pairLine) {
        if (pairLine == null)
            return false;
        pairLine = pairLine.trim();
        if (pairLine.isEmpty())
            return false;

        String[] parts = StringOperations.splitByStringNotNested(pairLine, ":");
        if (parts.length != 2)
            return false;
        String key = parts[0].trim();
        String value = parts[1].trim();

        if (!validateString(key))
            return false;
        if (!validateValue(value))
            return false;
        return true;
    }

    /**
     * Validates array
     *
     * <pre>
     * &lt;array&gt; ::= [ ] | [ &lt;elements&gt; ]
     * </pre>
     *
     * @param arrayLine
     *                  The line contining the full array to parse (may not be null
     *                  or empty or blank)
     *
     * @return {@code true} if the array is successfully parsed, {@code false}
     *         otherwise
     */
    public static boolean validateArray(String arrayLine) {
        if (arrayLine == null)
            return false;
        arrayLine = arrayLine.trim();
        if (arrayLine.isEmpty())
            return false;

        if (!arrayLine.startsWith("[") || !arrayLine.endsWith("]"))
            return false;
        String elements = arrayLine.substring(1, arrayLine.length() - 1).trim();
        if (elements.isEmpty())
            return true;
        return validateElements(elements);
    }

    /**
     * Validates elements
     *
     * <pre>
     * &lt;elements&gt; ::= &lt;value&gt; | &lt;value&gt; , &lt;elements&gt;
     * </pre>
     *
     * @param elementsLine
     *                     The line contining the full elements to parse (must not
     *                     be null or empty or blank)
     *
     * @return {@code true} if the elements are successfully parsed, {@code false}
     *         otherwise
     */
    public static boolean validateElements(String elementsLine) {
        if (elementsLine == null)
            return false;
        elementsLine = elementsLine.trim();
        if (elementsLine.isEmpty())
            return false;

        String[] values = StringOperations.splitByStringNotNested(elementsLine, ",");
        for (String value : values) {
            if (!validateValue(value))
                return false;
        }
        return true;
    }

    /**
     * Validates value
     *
     * <pre>
     * &lt;value&gt; ::= &lt;string&gt; | &lt;number&gt; | &lt;object&gt; | &lt;array&gt; | true | false | null
     * </pre>
     *
     * @param valueLine
     *                  The line contining the full value to parse (may not be null
     *                  or empty or blank)
     *
     * @return {@code true} if the value is successfully parsed, {@code false}
     *         otherwise
     */
    public static boolean validateValue(String valueLine) {
        if (valueLine == null)
            return false;
        valueLine = valueLine.trim();
        if (valueLine.isEmpty())
            return false;

        if (valueLine.equals("true") || valueLine.equals("false") || valueLine.equals("null"))
            return true;
        return validateString(valueLine) || validateNumber(valueLine) || validateObject(valueLine)
                || validateArray(valueLine);
    }

    /**
     * Validates string
     *
     * <pre>
     * &lt;string&gt; ::= " " | " &lt;characters&gt; "
     * </pre>
     *
     * @param stringLine
     *                   The line contining the full string to parse (may not be
     *                   null or empty, but may be blank)
     *
     * @return {@code true} if the string is successfully parsed, {@code false}
     *         otherwise
     */
    public static boolean validateString(String stringLine) {
        if (stringLine == null)
            return false;
        if (stringLine.isEmpty())
            return false;

        if (stringLine.length() < 2)
            return false;
        if (!stringLine.startsWith("\"") || !stringLine.endsWith("\""))
            return false;
        if (stringLine.length() < 3)
            return true;
        return validateCharacters(stringLine.substring(1, stringLine.length() - 1));
    }

    /**
     * Validates characters
     *
     * <pre>
     * &lt;characters&gt; ::= &lt;character&gt; | &lt;character&gt; &lt;characters&gt;
     * </pre>
     *
     * @param charactersLine
     *                       The line contining the full characters to parse (must
     *                       not be null or empty, but may be blank)
     *
     * @return {@code true} if the characters are successfully parsed, {@code false}
     *         otherwise
     */
    public static boolean validateCharacters(String charactersLine) {
        if (charactersLine == null)
            return false;
        if (charactersLine.isEmpty())
            return false;

        for (int i = 0; i < charactersLine.length();) {
            if (charactersLine.charAt(i) == '\\') {
                int escapeEnd = i + 1;
                if (escapeEnd >= charactersLine.length())
                    return false;

                if (charactersLine.charAt(escapeEnd) == 'u') {
                    escapeEnd += 4;
                }
                escapeEnd++;

                if (escapeEnd > charactersLine.length())
                    return false;

                if (!validateEscape(charactersLine.substring(i, escapeEnd)))
                    return false;
                i = escapeEnd;
                continue;
            }
            int charLength = Character.charCount(charactersLine.codePointAt(i));
            if (!validateCharacter(charactersLine.substring(i, i + charLength)))
                return false;
            i += charLength;
        }

        return true;
    }

    /**
     * Validates character
     *
     * <pre>
     * &lt;character&gt; ::= # any unicode character except "" or \ or control characters # | &lt;escape&gt;
     * </pre>
     *
     * @param characterLine
     *                      The line contining the full character to parse (must not
     *                      be null)
     *
     * @return {@code true} if the character is successfully parsed, {@code false}
     *         otherwise
     */
    public static boolean validateCharacter(String characterLine) {
        if (characterLine == null)
            return false;

        if (characterLine.startsWith("\\"))
            return validateEscape(characterLine);

        int charCount = characterLine.codePointCount(0, characterLine.length());
        if (charCount != 1)
            return false;

        int codePoint = characterLine.codePointAt(0);

        if (Character.isISOControl(codePoint))
            return false;
        if (codePoint == '\"' || codePoint == '\\')
            return false;

        return true;
    }

    /**
     * Validates escape character
     *
     * <pre>
     * &lt;escape&gt; ::= \ (" | \ | / | b | f | n | r | t | u &lt;hex&gt;&lt;hex&gt;&lt;hex&gt;&lt;hex&gt;)
     * </pre>
     *
     * @param escapeLine
     *                   The line contining the full escape to parse (must not be
     *                   null or empty or blank)
     *
     * @return {@code true} if the escape is successfully parsed, {@code false}
     *         otherwise
     */
    public static boolean validateEscape(String escapeLine) {
        if (escapeLine == null)
            return false;
        escapeLine = escapeLine.trim();
        if (escapeLine.isEmpty())
            return false;
        if (escapeLine.charAt(0) != '\\')
            return false;

        if (escapeLine.length() < 2)
            return false;
        char[] escapeChars = { '"', '\\', '/', 'b', 'f', 'n', 'r', 't', 'u' };
        boolean validEscape = false;
        for (char escape : escapeChars)
            if (escape == escapeLine.charAt(1))
                validEscape = true;
        if (!validEscape)
            return false;
        // validate special character hex
        if (escapeLine.charAt(1) == 'u') {
            if (escapeLine.length() != 6)
                return false;
            for (int i = 2; i <= 5; i++)
                if (!validateHex(escapeLine.charAt(i)))
                    return false;
        } else if (escapeLine.length() > 2)
            return false;
        return true;
    }

    /**
     * Validates number
     *
     * <pre>
     * &lt;number&gt; ::= &lt;int&gt; &lt;frac&gt;? &lt;exp&gt;?
     * </pre>
     *
     * @param numberLine
     *                   The line contining the full number to parse (must not be
     *                   null or empty or blank)
     *
     * @return {@code true} if the number is successfully parsed, {@code false}
     *         otherwise
     */
    public static boolean validateNumber(String numberLine) {
        if (numberLine == null)
            return false;
        numberLine = numberLine.trim().toUpperCase();
        if (numberLine.isEmpty())
            return false;
        // Identify portions
        int beginInt = 0;
        int beginFrac = numberLine.indexOf(".");
        int beginExp = numberLine.indexOf("E");

        int endExp = numberLine.length();
        int endFrac = beginExp != -1 ? beginExp : endExp;
        int endInt = beginFrac != -1 ? beginFrac : endFrac;

        // Parse integer part
        if (!validateInt(numberLine.substring(beginInt, endInt)))
            return false;
        // Parse fraction part
        if (beginFrac != -1) {
            if (!validateFrac(numberLine.substring(beginFrac, endFrac)))
                return false;
        }
        // Parse exponent part
        if (beginExp != -1) {
            if (!validateExp(numberLine.substring(beginExp, endExp)))
                return false;
        }
        return true;
    }

    /**
     * Validates integer
     *
     * <pre>
     * &lt;int&gt; ::= -? &lt;digits&gt;
     * </pre>
     *
     * @param intLine
     *                The line contining the full int to parse (must not be null or
     *                empty or blank)
     *
     * @return {@code true} if the int is successfully parsed, {@code false}
     *         otherwise
     */
    public static boolean validateInt(String intLine) {
        if (intLine == null)
            return false;
        intLine = intLine.trim();
        if (intLine.isEmpty())
            return false;
        if (intLine.charAt(0) == '-') {
            return validateDigits(intLine.substring(1));
        }
        return validateDigits(intLine);
    }

    /**
     * Validates fraction
     *
     * <pre>
     * &lt;frac&gt; ::= . &lt;digits&gt;
     * </pre>
     *
     * @param fracLine
     *                 The line contining the full frac to parse (must not be null
     *                 or empty or blank)
     *
     * @return {@code true} if the frac is successfully parsed, {@code false}
     *         otherwise
     */
    public static boolean validateFrac(String fracLine) {
        if (fracLine == null)
            return false;
        fracLine = fracLine.trim();
        if (fracLine.isEmpty())
            return false;

        if (fracLine.length() < 2)
            return false;
        if (fracLine.charAt(0) != '.')
            return false;
        return validateDigits(fracLine.substring(1));
    }

    /**
     * Validates exponent
     *
     * <pre>
     * &lt;exp&gt; ::= (e | E) (+ | -)? &lt;digits&gt;
     * </pre>
     *
     * @param expLine
     *                The line contining the full exp to parse (must not be null or
     *                empty or blank)
     *
     * @return {@code true} if the exp is successfully parsed, {@code false}
     *         otherwise
     */
    public static boolean validateExp(String expLine) {
        if (expLine == null)
            return false;
        expLine = expLine.trim();
        if (expLine.isEmpty())
            return false;
        if (expLine.length() < 2)
            return false;
        if (expLine.charAt(0) != 'e' && expLine.charAt(0) != 'E')
            return false;
        if (expLine.charAt(1) == '+' || expLine.charAt(1) == '-') {
            return validateDigits(expLine.substring(2));
        }
        return validateDigits(expLine.substring(1));
    }

    /**
     * Validates digits
     *
     * <pre>
     * &lt;digits&gt; ::= &lt;digit&gt; | &lt;digit&gt; &lt;digits&gt;
     * </pre>
     *
     * @param digitsLine
     *                   The line contining the full digits to parse (must not be
     *                   null or empty or blank)
     *
     * @return {@code true} if the digits are successfully parsed, {@code false}
     *         otherwise
     */
    public static boolean validateDigits(String digitsLine) {
        if (digitsLine == null)
            return false;
        digitsLine = digitsLine.trim();
        if (digitsLine.isEmpty())
            return false;
        for (char digit : digitsLine.toCharArray()) {
            if (!validateDigit(digit))
                return false;
        }
        return true;
    }

    /**
     * Validates digit
     *
     * <pre>
     * &lt;digit&gt; ::= # digit from 0 to 9 #
     * </pre>
     *
     * @param digit
     *              The the digit character to parse
     *
     * @return {@code true} if the digit is successfully parsed, {@code false}
     *         otherwise
     */
    public static boolean validateDigit(char digit) {
        return digit >= '0' && digit <= '9';
    }

    /**
     * Validates hex
     * 
     * <pre>
     * &lt;hex&gt; ::= &lt;digit&gt; | [a-f] [A-F]
     * </pre>
     * 
     * @param hex
     *            The hex to parse
     *
     * @return {@code true} if the hex is successfully parsed, {@code false}
     *         otherwise
     */
    public static boolean validateHex(char hex) {
        return validateDigit(hex) || (hex >= 'a' && hex <= 'f') || (hex >= 'A' && hex <= 'F');
    }
}
