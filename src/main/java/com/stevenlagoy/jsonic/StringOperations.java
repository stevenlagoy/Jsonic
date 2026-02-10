package com.stevenlagoy.jsonic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Util class for operations using Strings. */
public class StringOperations {

    private StringOperations() {
    }

    /**
     * Determines whether a given position in a line of text is currently inside a
     * string literal, accounting for
     * escaped quotes.
     *
     * <p>
     * When called, this method checks if the "in-string" state for the provided
     * line has already been computed and cached.
     * If so, it returns the cached result for the given position in O(1) time. If
     * not, it computes the in-string state for
     * every character in the line, caches the result, and then returns the value
     * for the requested position.
     * </p>
     *
     * @param line
     *                 The input string to analyze.
     * @param position
     *                 The index position in the string to check.
     *
     * @return {@code true} if the position is within a string literal,
     *         {@code false} otherwise.
     * 
     * @see #clearInStringCache()
     */
    public static boolean isInString(String line, int position) {
        if (inStringCache == null)
            inStringCache = new HashMap<String, Boolean[]>();

        // If the String is already in the cache, return the inString value for the
        // given position.
        Boolean[] inStringArr = inStringCache.get(line);
        if (inStringArr != null) {
            return inStringArr[position];
        }

        // Otherwise, precompute the cache entry for this String and call this method
        // again.
        inStringArr = new Boolean[line.length()];
        boolean inString = false;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == '"' && (i == 0 || line.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            inStringArr[i] = inString;
        }
        inStringCache.put(line, inStringArr);
        return isInString(line, position);
    }

    private static Map<String, Boolean[]> inStringCache;

    /** Clear inStringCache */
    public static void clearInStringCache() {
        inStringCache = null;
    }

    /**
     * Determines whether the specified position in a line of text is currently
     * inside a JSON array (which is marked by square brackets {@code []}).
     * <p>
     * This method uses caching to improve performance for repeated queries on the
     * same string. When called, it checks if the
     * "in-array" state for the provided line has already been computed and cached.
     * If so, it returns the cached result for
     * the given position in O(1) time. If not, it computes the in-array state for
     * every character in the line, caches the
     * result, and then returns the value for the requested position.
     * </p>
     *
     * @param line
     *                 The input string to analyze.
     * @param position
     *                 The index position in the string to check.
     *
     * @return {@code true} if the position is within a JSON array, {@code false}
     *         otherwise.
     *
     * @see #isInString(String, int)
     * @see #clearInArrayCache()
     */
    public static boolean isInArray(String line, int position) {
        if (inArrayCache == null)
            inArrayCache = new HashMap<>();

        // If the String is already in the cache, return the inArray value for the given
        // position.
        Boolean[] inArrayArr = inArrayCache.get(line);
        if (inArrayArr != null) {
            return inArrayArr[position];
        }

        // Otherwise, precompute the cache entry for this String and call this method
        // again.
        inArrayArr = new Boolean[line.length()];
        int depth = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == '[' && (i == 0 || !isInString(line, i - 1)))
                depth++;
            else if (line.charAt(i) == ']' && (i == 0 || !isInString(line, i - 1)))
                depth--;
            inArrayArr[i] = depth > 0;
        }
        inArrayCache.put(line, inArrayArr);
        return isInArray(line, position);
    }

    private static Map<String, Boolean[]> inArrayCache;

    /**
     * Clear inArrayCache
     */
    public static void clearInArrayCache() {
        inStringCache = null;
    }

    /**
     * Determines whether the specified position in a line of text is currently
     * inside a JSON object (which is marked by curly braces {@code &#123;&#125;}).
     * <p>
     * This method uses caching to improve performance for repeated queries on the
     * same string. When called, it checks if the
     * "in-object" state for the provided line has already been computed and cached.
     * If so, it returns the cached result for
     * the given position in O(1) time. If not, it computes the in-object state for
     * every character in the line, caches the
     * result, and then returns the value for the requested position.
     * </p>
     * <p>
     * This caching approach greatly improves performance when the same string is
     * analyzed multiple times, especially for
     * large strings or when called in a loop.
     * </p>
     *
     * @param line
     *                 The input string to analyze.
     * @param position
     *                 The index position in the string to check.
     *
     * @return {@code true} if the position is within a JSON object, {@code false}
     *         otherwise.
     *
     * @see #isInString(String, int)
     * @see #clearInObjectCache()
     */
    public static boolean isInObject(String line, int position) {
        if (inObjectCache == null)
            inObjectCache = new HashMap<>();

        // If the String is already in the cache, return the inObject value for the
        // given position.
        Boolean[] inObjectArr = inObjectCache.get(line);
        if (inObjectArr != null) {
            return inObjectArr[position];
        }

        // Otherwise, precompute the cache entry for this String and call this method
        // again.
        inObjectArr = new Boolean[line.length()];
        int depth = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == '{' && (i == 0 || !isInString(line, i)))
                depth++;
            else if (line.charAt(i) == '}' && (i == 0 || !isInString(line, i)))
                depth--;
            inObjectArr[i] = depth > 0;
        }
        inObjectCache.put(line, inObjectArr);
        return isInObject(line, position);
    }

    private static Map<String, Boolean[]> inObjectCache;

    /** Clear inObjectCache */
    public static void clearInObjectCache() {
        inObjectCache = null;
    }

    /**
     * Checks if the given line contains at least one unquoted occurrence of the
     * target character.
     *
     * <p>
     * This method ignores characters that appear inside string literals (delimited
     * by double quotes) and only considers
     * unescaped characters outside of strings.
     * </p>
     *
     * @param line
     *               The string to search.
     * @param target
     *               The character to look for.
     *
     * @return {@code true} if the character appears outside of a string,
     *         {@code false} otherwise.
     *
     * @see #countUnquotedChar(String, char)
     * @see #isInString(String, int)
     */
    public static boolean containsUnquotedChar(String line, char target) {
        return findFirstUnquotedChar(line, target) != -1;
    }

    /**
     * Counts how many times a target character appears outside of string literals
     * in the given line.
     *
     * <p>
     * This method scans the string and increments a counter for each instance of
     * the target character that is not
     * within a quoted string. Escaped quotes are correctly ignored.
     * </p>
     *
     * @param line
     *               The {@code string} to analyze.
     * @param target
     *               The {@code char} to count.
     *
     * @return The number of unquoted occurrences of the target character.
     *
     * @see #isInString(String, int)
     */
    public static int countUnquotedChar(String line, char target) {
        int count = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == target && !isInString(line, i))
                count++;
        }
        return count;
    }

    /**
     * Finds the first instance of the target character outside of a string literal.
     *
     * @param line
     *               The {@code string} through which to search
     * @param target
     *               The {@code char} to search for
     *
     * @return The index of the first unquoted instance of the target character, or
     *         {@code -1} if not found
     *
     * @see #isInString(String, int)
     */
    public static int findFirstUnquotedChar(String line, char target) {
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == target && !isInString(line, i))
                return i;
        }
        return -1;
    }

    /**
     * Splits the input string around occurrences of the given separator string,
     * ignoring separators that appear inside
     * quoted strings.
     *
     * <p>
     * This method respects string literals delimited by double quotes, treating
     * escaped quotes correctly. Trims
     * whitespace around each resulting substring.
     * </p>
     *
     * @param string
     *                  The input string to split.
     * @param separator
     *                  The substring to split on, when not within quotes.
     *
     * @return An array of trimmed substrings resulting from the split.
     *
     * @see #isInString(String, int)
     */
    public static String[] splitByUnquotedString(String string, String separator) {
        if (splitByUnquotedCache == null)
            splitByUnquotedCache = new HashMap<>();

        // Check if the string and separator are already in the cache.
        String[] splitByUnquotedArr = splitByUnquotedCache.get(Map.of(string, separator));
        if (splitByUnquotedArr != null) {
            return splitByUnquotedArr;
        }

        // Otherwise, precompute and add result to the cache.
        List<String> parts = new ArrayList<>();
        int lastSplitIndex = 0;

        for (int i = 0; i <= string.length() - separator.length(); i++) {
            // Check if this position contains the separator
            if (string.startsWith(separator, i)) {
                // Verify the separator is not in quotes
                if (!isInString(string, i)) {
                    // Add the part before this separator
                    parts.add(string.substring(lastSplitIndex, i).trim());
                    lastSplitIndex = i + separator.length();
                    i += separator.length() - 1; // Skip the rest of the separator
                }
            }
        }

        // Add the remaining part after the last separator
        if (lastSplitIndex <= string.length()) {
            parts.add(string.substring(lastSplitIndex).trim());
        }

        splitByUnquotedCache.put(Map.of(string, separator), parts.toArray(new String[0]));
        return parts.toArray(new String[0]);
    }

    private static Map<Map<String, String>, String[]> splitByUnquotedCache;

    /** Clear splitByUnquotedCache */
    public static void clearSplitByUnquotedCache() {
        splitByUnquotedCache = null;
    }

    /**
     * Splits the input string around occurrences of the given separator string,
     * ignoring separators that appear inside
     * quoted strings, and limits the number of resulting substrings.
     *
     * <p>
     * Trims whitespace around each substring. Stops splitting once {@code limit}
     * parts have been collected, placing the
     * remainder into the last entry.
     * </p>
     *
     * @param string
     *                  The input string to split.
     * @param separator
     *                  The substring to split on, when not within quotes.
     * @param limit
     *                  The maximum number of substrings to return. Must be at least
     *                  1.
     *
     * @return An array of trimmed substrings resulting from the split, with at most
     *         {@code limit} entries.
     *
     * @see #isInString(String, int)
     */
    public static String[] splitByUnquotedString(String string, String separator, int limit) {
        List<String> parts = new ArrayList<>();
        int lastSplitIndex = 0;
        int count = 1; // always returns at least one string

        for (int i = 0; i <= string.length() - separator.length() && count < limit; i++) {
            // Check if this position contains the separator
            if (string.startsWith(separator, i)) {
                // Verify the separator is not in quotes
                if (!isInString(string, i)) {
                    // Add the part before this separator
                    parts.add(string.substring(lastSplitIndex, i).trim());
                    count++;
                    lastSplitIndex = i + separator.length();
                    i += separator.length() - 1; // Skip the rest of the separator
                }
            }
        }

        // Add the remaining part after the last separator
        if (lastSplitIndex <= string.length()) {
            parts.add(string.substring(lastSplitIndex).trim());
        }

        return parts.toArray(new String[0]);
    }

    /**
     * Splits the input string around occurrences of the given separator string,
     * ignoring separators that appear within
     * array brackets <code>[...]</code>.
     *
     * <p>
     * This method treats brackets as array delimiters and avoids splitting inside
     * them, even if the separator appears.
     * Trims whitespace around each resulting substring.
     * </p>
     *
     * @param string
     *                  The input string to split.
     * @param separator
     *                  The substring to split on, when not within array brackets.
     *
     * @return An array of trimmed substrings resulting from the split.
     *
     * @see #isInArray(String, int)
     */
    public static String[] splitByStringNotInArray(String string, String separator) {
        if (splitByNotInArrayCache == null)
            splitByNotInArrayCache = new HashMap<>();

        // Check if the string and separator is already in the cache.
        String[] splitByNotInArrayArr = splitByNotInArrayCache.get(Map.of(string, separator));
        if (splitByNotInArrayArr != null) {
            return splitByNotInArrayArr;
        }

        // Otherwise, precompute and add result to the cache.
        List<String> parts = new ArrayList<>();
        int lastSplitIndex = 0;

        for (int i = 0; i <= string.length() - separator.length(); i++) {
            // Check if this position contains the separator
            if (string.startsWith(separator, i)) {
                // Verify the separator is not in brackets
                if (!isInArray(string, i)) {
                    // Add the part before this separator
                    parts.add(string.substring(lastSplitIndex, i).trim());
                    lastSplitIndex = i + separator.length();
                    i += separator.length() - 1; // Skip the rest of the separator
                }
            }
        }

        // Add the remaining part after the last separator
        if (lastSplitIndex <= string.length()) {
            parts.add(string.substring(lastSplitIndex).trim());
        }

        splitByNotInArrayCache.put(Map.of(string, separator), parts.toArray(new String[0]));
        return parts.toArray(new String[0]);
    }

    private static Map<Map<String, String>, String[]> splitByNotInArrayCache;

    /** Clear splitByNotInArrayCache */
    public static void clearSplitByNotInArrayCache() {
        splitByNotInArrayCache = null;
    }

    /**
     * Split by the passed string where it appears outside of any object (not
     * surrounded by curly braces {@code {}}.
     * 
     * @param string    String to split
     * 
     * @param separator Separator used to split
     * @return Array of String of of the original string split by the separator. The
     *         separator will not be present unless inside an object {@code {}}.
     */
    public static String[] splitByStringNotInObject(String string, String separator) {
        if (splitByNotInObjectCache == null)
            splitByNotInObjectCache = new HashMap<>();

        // Check if the string and separator are already in the cache.
        String[] splitByNotInObjectArr = splitByNotInObjectCache.get(Map.of(string, separator));
        if (splitByNotInObjectArr != null) {
            return splitByNotInObjectArr;
        }

        // Otherwise, precompute and add result to the cache.
        List<String> parts = new ArrayList<>();
        int lastSplitIndex = 0;

        for (int i = 0; i <= string.length() - separator.length(); i++) {
            // Check if this position contains the separator
            if (string.startsWith(separator, i)) {
                // Verify the separator is not in brackets
                if (!isInObject(string, i)) {
                    // Add the part before this separator
                    parts.add(string.substring(lastSplitIndex, i).trim());
                    lastSplitIndex = i + separator.length();
                    i += separator.length() - 1; // Skip the rest of the separator
                }
            }
        }

        // Add the remaining part after the last separator
        if (lastSplitIndex <= string.length()) {
            parts.add(string.substring(lastSplitIndex).trim());
        }

        splitByNotInObjectCache.put(Map.of(string, separator), parts.toArray(new String[0]));
        return parts.toArray(new String[0]);
    }

    private static Map<Map<String, String>, String[]> splitByNotInObjectCache;

    /** Clear splitByNotInObjectCache */
    public static void clearSplitByNotInObjectCache() {
        splitByNotInObjectCache = null;
    }

    /**
     * Splits a string by a separator only when it is not inside an object or an
     * array.
     *
     * @param string    Input string to split
     * @param separator String to use as separator
     *
     * @return List of strings split from the input string based on the separator.
     */
    public static String[] splitByStringNotNested(String string, String separator) {
        if (splitByNotNestedCache == null)
            splitByNotNestedCache = new HashMap<>();

        // Check if the string and separator are already in the cache.
        String[] splitByNotNestedArr = splitByNotNestedCache.get(Map.of(string, separator));
        if (splitByNotNestedArr != null) {
            return splitByNotNestedArr;
        }

        // Otherwise, precompute and add result to the cache.
        List<String> parts = new ArrayList<>();
        int lastSplitIndex = 0;

        for (int i = 0; i <= string.length() - separator.length(); i++) {
            // Check if this position contains the separator
            if (string.startsWith(separator, i)) {
                // Verify the separator is not in brackets
                if (!isInObject(string, i) && !isInArray(string, i) && !isInString(string, i)) {
                    // Add the part before this separator
                    parts.add(string.substring(lastSplitIndex, i).trim());
                    lastSplitIndex = i + separator.length();
                    i += separator.length() - 1; // Skip the rest of the separator
                }
            }
        }

        // Add the remaining part after the last separator
        if (lastSplitIndex <= string.length()) {
            parts.add(string.substring(lastSplitIndex).trim());
        }

        splitByNotNestedCache.put(Map.of(string, separator), parts.toArray(new String[0]));
        return parts.toArray(new String[0]);
    }

    private static Map<Map<String, String>, String[]> splitByNotNestedCache;

    /** Clear splitByNotNestedCache */
    public static void clearSplitByNotNestedCache() {
        splitByNotNestedCache = null;
    }

    /**
     * Replaces the last occurrence of a specified regular expression with a
     * replacement string in the input text.
     * This method uses the {@link #replaceFirst(String, String, String)} method by
     * reversing the string and performing
     * the replacement on the first occurrence in the reversed string.
     *
     * @param text
     *                    The input string in which the replacement will be made.
     * @param regex
     *                    The regular expression to match for the replacement.
     * @param replacement
     *                    The string to replace the matched regex.
     *
     * @return A new string with the last occurrence of the regex replaced by the
     *         replacement string.
     *
     * @see String#replaceFirst(String, String)
     */
    public static String replaceLast(String text, String regex, String replacement) {
        String reversedText = new StringBuilder(text).reverse().toString();
        String reversedResult = reversedText.replaceFirst(regex, replacement);
        String result = new StringBuilder(reversedResult).reverse().toString();
        return result;
    }

    /**
     * Replaces the first occurrence of a specified regular expression with a
     * replacement string in the input text.
     * This method reverses the input string, performs the replacement on the first
     * occurrence of the regex in the reversed string,
     * and then reverses the resulting string again to produce the final output.
     *
     * @param text
     *                    The input string in which the replacement will be made.
     * @param regex
     *                    The regular expression to match for the replacement
     * @param replacement
     *                    The string to replace the matched regex
     *
     * @return A new string with the first occurrence of the regex replaced by the
     *         replacement string.
     *
     * @see String#replaceFirst(String, String)
     */
    public static String replaceFirst(String text, String regex, String replacement) {
        return text.replaceFirst(regex, replacement);
    }

    /**
     * Replaces all occurrences of a specified regular expression in the input text
     * with a replacement string.
     *
     * @param text
     *                    The input string in which the replacements will be made.
     * @param regex
     *                    The regular expression to match for the replacement.
     * @param replacement
     *                    The string to replace the matched regex
     *
     * @return A new string with all occurrences of the regex replaced by the
     *         replacement string.
     *
     * @see String#replaceAll(String, String)
     */
    public static String replaceAll(String text, String regex, String replacement) {
        return text.replaceAll(regex, replacement);
    }

    /**
     * Replaces all occurrences of a specified regular expression where it appears
     * outside of a quotated string in the
     * input text with a replacement string.
     *
     * @param text
     *                    The input string in which the replacements will be made.
     * @param regex
     *                    The regular expression to match for the replacement.
     * @param replacement
     *                    The string to replace the matched regex
     *
     * @return A new string with all occurrences of the regex outside of a string
     *         replaced by the replacement string
     *
     * @see #isInString(String, int)
     */
    public static String replaceAllNotInString(String text, String regex, String replacement) {
        String[] split = splitByUnquotedString(text, regex);
        String result = String.join(replacement, split);
        return result;
    }

    /**
     * Replaces the first {@code count} occurrences of the specified regular
     * expression with the replacement string in
     * the input text.
     *
     * @param text
     *                    The input string in which the replacements will be made.
     * @param regex
     *                    The regular expression to match for the replacement.
     * @param replacement
     *                    The string to replace the matched regex.
     * @param count
     *                    The number of occurrences to replace.
     *
     * @return A new string with the first {@code count} occurrences of the regex
     *         replaced by the replacement string.
     *
     * @see #replaceFirst(String, String, String)
     */
    public static String replaceFirstCount(String text, String regex, String replacement, int count) {
        StringBuilder result = new StringBuilder(text);
        int startPos = 0; // Start position for the next search

        for (int i = 0; i < count; i++) {
            // Find the next occurrence of the regex starting from the current position
            int index = result.indexOf(regex, startPos);
            if (index == -1) {
                break; // No more matches found, stop the loop
            }

            // Replace the match with the replacement string
            result.replace(index, index + regex.length(), replacement);

            // Move the start position just after the current replacement
            startPos = index + replacement.length();
        }

        return result.toString();
    }

    /**
     * Replaces the last {@code count} occurrences of the specified regular
     * expression with the replacement string in
     * the input text.
     *
     * @param text
     *                    The input string in which the replacements will be made.
     * @param regex
     *                    The regular expression to match for the replacement.
     * @param replacement
     *                    The string to replace the matched regex.
     * @param count
     *                    The number of occurrences to replace.
     *
     * @return A new string with the last {@code count} occurrences of the regex
     *         replaced by the replacement string.
     *
     * @see #replaceLast(String, String, String)
     */
    public static String replaceLastCount(String text, String regex, String replacement, int count) {
        StringBuilder result = new StringBuilder(text);
        int startPos = text.length(); // Start from the end of the string

        for (int i = 0; i < count; i++) {
            // Find the last occurrence of the regex before the current position
            int index = result.lastIndexOf(regex, startPos - 1);
            if (index == -1) {
                break; // No more matches found, stop the loop
            }

            // Replace the match with the replacement string
            result.replace(index, index + regex.length(), replacement);

            // Move the start position just before the current replacement
            startPos = index;
        }

        return result.toString();
    }
}