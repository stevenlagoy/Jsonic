package com.stevenlagoy.jsonic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

/**
 * Tests for {@link JSONParser} exercised through the {@link JSONObject}
 * constructors, which are the only public entry points to the parser.
 *
 * <p>
 * Coverage targets:
 * <ul>
 * <li>parse(String, String) — all JSON value types</li>
 * <li>parse(String, Iterable&lt;String&gt;) — line concatenation</li>
 * <li>parseValue — all branches including null sentinel</li>
 * <li>parseObject — empty, single pair, multi-pair, nested, duplicate keys</li>
 * <li>parseMembers — valid and invalid, duplicate key rejection</li>
 * <li>parsePair — valid, null value, missing colon, extra colons</li>
 * <li>parseArray — empty, primitives, mixed, nested, null elements</li>
 * <li>parseElements — valid, invalid element, null element</li>
 * <li>parseString — empty, valid, escapes, missing quotes</li>
 * <li>parseCharacters — normal chars, all escape types, unicode, invalid</li>
 * <li>parseEscape — all valid codes, unicode, invalid codes</li>
 * <li>parseNumber — integers, longs, doubles, scientific, negative,
 * invalid</li>
 * <li>isValidLine, isValidInt, isValidFrac, isValidExp, isValidDigits (via
 * number parsing)</li>
 * </ul>
 */
public class JSONParserTest {

    // Basic parsing ---------------------------------------------------------------

    @Test
    public void parse_nullContents_returnsNullValue() {
        JSONObject result = JSONParser.parse("key", (Iterable<String>) null);
        assertEquals("key", result.getKey());
        assertNull(result.getValue());
    }

    @Test
    public void parse_multiLineJson_concatenatesAndParses() {
        List<String> lines = List.of(
                "{",
                "\"name\": \"Alice\",",
                "\"age\": 30",
                "}");
        JSONObject result = JSONParser.parse("root", lines);
        assertNotNull(result.getValue());
        assertTrue(result.isObject());
        assertEquals("Alice", result.findString("name").orElse(null));
        assertEquals(30, result.findInt("age").orElse(-1).intValue());
    }

    @Test
    public void parse_linesWithNullEntries_skipsNulls() {
        List<String> lines = new java.util.ArrayList<>();
        lines.add("{\"x\": 1}");
        lines.add(null);
        lines.add("");
        JSONObject result = JSONParser.parse("root", lines);
        assertEquals(1, result.findInt("x").orElse(-1).intValue());
    }

    // parse(String, String): null and empty inputs --------------------------------

    @Test
    public void parse_nullString_returnsNullValue() {
        JSONObject result = JSONParser.parse("key", (String) null);
        assertNull(result.getValue());
    }

    @Test
    public void parse_emptyString_returnsNullValue() {
        JSONObject result = JSONParser.parse("key", "");
        assertNull(result.getValue());
    }

    @Test
    public void parse_blankString_returnsNullValue() {
        JSONObject result = JSONParser.parse("key", "   ");
        assertNull(result.getValue());
    }

    @Test
    public void parse_newlinesOutsideStrings_normalized() {
        JSONObject result = JSONParser.parse("root", "{\"a\":\n1}");
        assertEquals(1, result.findInt("a").orElse(-1).intValue());
    }

    @Test
    public void parse_newlinesInsideStrings_preserved() {
        // A literal \n escape inside the JSON string value should be kept
        JSONObject result = JSONParser.parse("root", "{\"a\":\"line1\\nline2\"}");
        assertEquals("line1\nline2", result.findString("a").orElse(null));
    }

    // JSON null -------------------------------------------------------------------

    @Test
    public void parseValue_null_storesJavaNullInNode() {
        JSONObject result = JSONParser.parse("key", "null");
        assertTrue(result.isNull());
        assertNull(result.getValue());
    }

    @Test
    public void parseValue_nullInObject_storesJavaNullForKey() {
        JSONObject result = JSONParser.parse("root", "{\"x\": null}");
        assertTrue(result.hasKey("x"));
        // find returns empty for null values, but hasKey confirms presence
        assertFalse(result.findString("x").isPresent());
    }

    @Test
    public void parseValue_nullInArray_storesJavaNullElement() {
        JSONObject result = JSONParser.parse("root", "{\"arr\": [1, null, 3]}");
        List<?> arr = result.findArray("arr").orElse(null);
        assertNotNull(arr);
        assertEquals(3, arr.size());
        assertNull(arr.get(1));
    }

    // Boolean ---------------------------------------------------------------------

    @Test
    public void parseValue_true_returnsTrue() {
        JSONObject result = JSONParser.parse("key", "true");
        assertEquals(Boolean.TRUE, result.getValue());
    }

    @Test
    public void parseValue_false_returnsFalse() {
        JSONObject result = JSONParser.parse("key", "false");
        assertEquals(Boolean.FALSE, result.getValue());
    }

    @Test
    public void parseValue_booleanInObject() {
        JSONObject result = JSONParser.parse("root", "{\"flag\": true}");
        assertEquals(Boolean.TRUE, result.findBoolean("flag").orElse(null));
    }

    // String parsing

    @Test
    public void parseString_empty_returnsEmptyString() {
        JSONObject result = JSONParser.parse("key", "\"\"");
        assertEquals("", result.getString());
    }

    @Test
    public void parseString_simple_returnsValue() {
        JSONObject result = JSONParser.parse("key", "\"hello\"");
        assertEquals("hello", result.getString());
    }

    @Test
    public void parseString_withSpaces_preservesSpaces() {
        JSONObject result = JSONParser.parse("key", "\"hello world\"");
        assertEquals("hello world", result.getString());
    }

    @Test
    public void parseString_leadingTrailingSpacesInValue_preserved() {
        JSONObject result = JSONParser.parse("key", "\"  hello  \"");
        assertEquals("  hello  ", result.getString());
    }

    @Test
    public void parseString_unicodeCharacters_preserved() {
        JSONObject result = JSONParser.parse("key", "\"こんにちは\"");
        assertEquals("こんにちは", result.getString());
    }

    @Test
    public void parseString_surrogatePair_preserved() {
        // U+1F600 GRINNING FACE
        JSONObject result = JSONParser.parse("key", "\"\\uD83D\\uDE00\"");
        assertNotNull(result.getString());
        assertEquals(2, result.getString().length()); // surrogate pair
    }

    @Test
    public void parseString_missingOpenQuote_returnsNull() {
        JSONObject result = JSONParser.parse("key", "hello\"");
        assertNull(result.getValue());
    }

    @Test
    public void parseString_missingCloseQuote_returnsNull() {
        JSONObject result = JSONParser.parse("key", "\"hello");
        assertNull(result.getValue());
    }

    @Test
    public void parseString_noQuotes_returnsNull() {
        JSONObject result = JSONParser.parse("key", "hello");
        // "hello" is not a valid JSON value (not a string, not a number, etc.)
        assertNull(result.getValue());
    }

    // Escape sequences ------------------------------------------------------------

    @Test
    public void parseEscape_quote() {
        JSONObject result = JSONParser.parse("key", "\"\\\"\"");
        assertEquals("\"", result.getString());
    }

    @Test
    public void parseEscape_backslash() {
        JSONObject result = JSONParser.parse("key", "\"\\\\\"");
        assertEquals("\\", result.getString());
    }

    @Test
    public void parseEscape_forwardSlash() {
        JSONObject result = JSONParser.parse("key", "\"\\/\"");
        assertEquals("/", result.getString());
    }

    @Test
    public void parseEscape_backspace() {
        JSONObject result = JSONParser.parse("key", "\"\\b\"");
        assertEquals("\b", result.getString());
    }

    @Test
    public void parseEscape_formFeed() {
        JSONObject result = JSONParser.parse("key", "\"\\f\"");
        assertEquals("\f", result.getString());
    }

    @Test
    public void parseEscape_newline() {
        JSONObject result = JSONParser.parse("key", "\"\\n\"");
        assertEquals("\n", result.getString());
    }

    @Test
    public void parseEscape_carriageReturn() {
        JSONObject result = JSONParser.parse("key", "\"\\r\"");
        assertEquals("\r", result.getString());
    }

    @Test
    public void parseEscape_tab() {
        JSONObject result = JSONParser.parse("key", "\"\\t\"");
        assertEquals("\t", result.getString());
    }

    @Test
    public void parseEscape_unicode_heart() {
        JSONObject result = JSONParser.parse("key", "\"\\u2665\"");
        assertEquals("♥", result.getString());
    }

    @Test
    public void parseEscape_unicode_zero() {
        JSONObject result = JSONParser.parse("key", "\"\\u0000\"");
        assertEquals("\u0000", result.getString());
    }

    @Test
    public void parseEscape_invalidCode_returnsNull() {
        JSONObject result = JSONParser.parse("key", "\"\\z\"");
        assertNull(result.getValue());
    }

    @Test
    public void parseEscape_orphanBackslash_returnsNull() {
        JSONObject result = JSONParser.parse("key", "\"\\\"");
        assertNull(result.getValue());
    }

    @Test
    public void parseEscape_truncatedUnicode_returnsNull() {
        JSONObject result = JSONParser.parse("key", "\"\\u26\"");
        assertNull(result.getValue());
    }

    @Test
    public void parseEscape_invalidUnicodeDigits_returnsNull() {
        JSONObject result = JSONParser.parse("key", "\"\\uZZZZ\"");
        assertNull(result.getValue());
    }

    @Test
    public void parseEscape_multipleEscapesInSequence() {
        JSONObject result = JSONParser.parse("key", "\"\\n\\t\\r\"");
        assertEquals("\n\t\r", result.getString());
    }

    // Integer parsing -------------------------------------------------------------

    @Test
    public void parseNumber_zero() {
        JSONObject result = JSONParser.parse("key", "0");
        assertEquals(0, ((Number) result.getValue()).intValue());
    }

    @Test
    public void parseNumber_positiveInt() {
        JSONObject result = JSONParser.parse("key", "42");
        assertEquals(42, ((Number) result.getValue()).intValue());
    }

    @Test
    public void parseNumber_negativeInt() {
        JSONObject result = JSONParser.parse("key", "-42");
        assertEquals(-42, ((Number) result.getValue()).intValue());
    }

    @Test
    public void parseNumber_intMaxValue() {
        JSONObject result = JSONParser.parse("key", String.valueOf(Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, ((Number) result.getValue()).intValue());
    }

    @Test
    public void parseNumber_intMinValue() {
        JSONObject result = JSONParser.parse("key", String.valueOf(Integer.MIN_VALUE));
        assertEquals(Integer.MIN_VALUE, ((Number) result.getValue()).intValue());
    }

    // Long parsing ----------------------------------------------------------------

    @Test
    public void parseNumber_longBeyondIntRange_parsesAsLong() {
        long val = (long) Integer.MAX_VALUE + 1;
        JSONObject result = JSONParser.parse("key", String.valueOf(val));
        assertEquals(val, ((Number) result.getValue()).longValue());
    }

    @Test
    public void parseNumber_longMaxValue() {
        JSONObject result = JSONParser.parse("key", String.valueOf(Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, ((Number) result.getValue()).longValue());
    }

    // Double parsing --------------------------------------------------------------

    @Test
    public void parseNumber_decimal() {
        JSONObject result = JSONParser.parse("key", "3.14");
        assertEquals(3.14, ((Number) result.getValue()).doubleValue(), 0.001);
    }

    @Test
    public void parseNumber_negativeDecimal() {
        JSONObject result = JSONParser.parse("key", "-3.14");
        assertEquals(-3.14, ((Number) result.getValue()).doubleValue(), 0.001);
    }

    @Test
    public void parseNumber_scientificLowerE() {
        JSONObject result = JSONParser.parse("key", "1.23e4");
        assertEquals(12300.0, ((Number) result.getValue()).doubleValue(), 0.001);
    }

    @Test
    public void parseNumber_scientificUpperE() {
        JSONObject result = JSONParser.parse("key", "1.23E4");
        assertEquals(12300.0, ((Number) result.getValue()).doubleValue(), 0.001);
    }

    @Test
    public void parseNumber_scientificNegativeExp() {
        JSONObject result = JSONParser.parse("key", "1.23e-4");
        assertEquals(1.23e-4, ((Number) result.getValue()).doubleValue(), 1e-7);
    }

    @Test
    public void parseNumber_scientificPositiveExpSign() {
        JSONObject result = JSONParser.parse("key", "1.23e+4");
        assertEquals(12300.0, ((Number) result.getValue()).doubleValue(), 0.001);
    }

    @Test
    public void parseNumber_noFractionWithExp() {
        JSONObject result = JSONParser.parse("key", "2e3");
        assertEquals(2000.0, ((Number) result.getValue()).doubleValue(), 0.001);
    }

    // Invalid number parsing ------------------------------------------------------

    @Test
    public void parseNumber_alphabetic_returnsNull() {
        JSONObject result = JSONParser.parse("key", "abc");
        assertNull(result.getValue());
    }

    @Test
    public void parseNumber_leadingPlus_returnsNull() {
        // JSON does not allow leading +
        JSONObject result = JSONParser.parse("key", "+42");
        assertNull(result.getValue());
    }

    @Test
    public void parseNumber_justMinus_returnsNull() {
        JSONObject result = JSONParser.parse("key", "-");
        assertNull(result.getValue());
    }

    @Test
    public void parseNumber_doubleDot_returnsNull() {
        JSONObject result = JSONParser.parse("key", "1..2");
        assertNull(result.getValue());
    }

    @Test
    public void parseNumber_trailingDot_returnsNull() {
        JSONObject result = JSONParser.parse("key", "1.");
        assertNull(result.getValue());
    }

    @Test
    public void parseNumber_leadingDot_returnsNull() {
        JSONObject result = JSONParser.parse("key", ".5");
        assertNull(result.getValue());
    }

    @Test
    public void parseNumber_emptyExp_returnsNull() {
        JSONObject result = JSONParser.parse("key", "1e");
        assertNull(result.getValue());
    }

    // Object parsing --------------------------------------------------------------

    @Test
    public void parseObject_empty_returnsEmptyList() {
        JSONObject result = JSONParser.parse("root", "{}");
        assertNotNull(result.getValue());
        assertTrue(result.getValue() instanceof List);
        assertTrue(((List<?>) result.getValue()).isEmpty());
    }

    @Test
    public void parseObject_singleStringPair() {
        JSONObject result = JSONParser.parse("root", "{\"name\": \"Alice\"}");
        assertEquals("Alice", result.findString("name").orElse(null));
    }

    @Test
    public void parseObject_singleNumberPair() {
        JSONObject result = JSONParser.parse("root", "{\"age\": 30}");
        assertEquals(30, result.findInt("age").orElse(-1).intValue());
    }

    @Test
    public void parseObject_singleBooleanPair() {
        JSONObject result = JSONParser.parse("root", "{\"active\": true}");
        assertEquals(Boolean.TRUE, result.findBoolean("active").orElse(null));
    }

    @Test
    public void parseObject_multiplePairs() {
        JSONObject result = JSONParser.parse("root",
                "{\"a\": 1, \"b\": \"two\", \"c\": false}");
        assertEquals(1, result.findInt("a").orElse(-1).intValue());
        assertEquals("two", result.findString("b").orElse(null));
        assertEquals(Boolean.FALSE, result.findBoolean("c").orElse(null));
    }

    @Test
    public void parseObject_nestedObject() {
        JSONObject result = JSONParser.parse("root",
                "{\"outer\": {\"inner\": 42}}");
        assertEquals(42, result.findInt("inner").orElse(-1).intValue());
    }

    @Test
    public void parseObject_deeplyNested() {
        JSONObject result = JSONParser.parse("root",
                "{\"a\": {\"b\": {\"c\": \"deep\"}}}");
        assertEquals("deep", result.findString("c").orElse(null));
    }

    @Test
    public void parseObject_duplicateKey_returnsNull() {
        JSONObject result = JSONParser.parse("root",
                "{\"x\": 1, \"x\": 2}");
        // Duplicate key should cause parse failure — value should be null
        assertNull(result.getValue());
    }

    @Test
    public void parseObject_missingOpenBrace_returnsNull() {
        JSONObject result = JSONParser.parse("root", "\"x\": 1}");
        assertNull(result.getValue());
    }

    @Test
    public void parseObject_missingCloseBrace_returnsNull() {
        JSONObject result = JSONParser.parse("root", "{\"x\": 1");
        assertNull(result.getValue());
    }

    @Test
    public void parseObject_missingColon_returnsNull() {
        JSONObject result = JSONParser.parse("root", "{\"x\" 1}");
        assertNull(result.getValue());
    }

    @Test
    public void parseObject_missingValue_returnsNull() {
        JSONObject result = JSONParser.parse("root", "{\"x\":}");
        assertNull(result.getValue());
    }

    @Test
    public void parseObject_missingKey_returnsNull() {
        JSONObject result = JSONParser.parse("root", "{: 1}");
        assertNull(result.getValue());
    }

    @Test
    public void parseObject_trailingComma_returnsNull() {
        JSONObject result = JSONParser.parse("root", "{\"x\": 1,}");
        assertNull(result.getValue());
    }

    @Test
    public void parseObject_valueWithColonInString() {
        // Colon inside a value string should not be confused with pair separator
        JSONObject result = JSONParser.parse("root", "{\"url\": \"http://example.com\"}");
        assertEquals("http://example.com", result.findString("url").orElse(null));
    }

    @Test
    public void parseObject_nullValue_keyPresent() {
        JSONObject result = JSONParser.parse("root", "{\"x\": null}");
        assertTrue(result.hasKey("x"));
    }

    @Test
    public void parseObject_whitespaceAroundPairs_parsesCorrectly() {
        JSONObject result = JSONParser.parse("root",
                "{ \"a\" : 1 , \"b\" : 2 }");
        assertEquals(1, result.findInt("a").orElse(-1).intValue());
        assertEquals(2, result.findInt("b").orElse(-1).intValue());
    }

    // Array parsing ---------------------------------------------------------------

    @Test
    public void parseArray_empty_returnsEmptyList() {
        JSONObject result = JSONParser.parse("root", "{\"arr\": []}");
        List<?> arr = result.findArray("arr").orElse(null);
        assertNotNull(arr);
        assertTrue(arr.isEmpty());
    }

    @Test
    public void parseArray_integers() {
        JSONObject result = JSONParser.parse("root", "{\"arr\": [1, 2, 3]}");
        List<?> arr = result.findArray("arr").orElse(null);
        assertNotNull(arr);
        assertEquals(3, arr.size());
        assertEquals(1, ((Number) arr.get(0)).intValue());
        assertEquals(2, ((Number) arr.get(1)).intValue());
        assertEquals(3, ((Number) arr.get(2)).intValue());
    }

    @Test
    public void parseArray_strings() {
        JSONObject result = JSONParser.parse("root", "{\"arr\": [\"a\", \"b\"]}");
        List<?> arr = result.findArray("arr").orElse(null);
        assertNotNull(arr);
        assertEquals("a", arr.get(0));
        assertEquals("b", arr.get(1));
    }

    @Test
    public void parseArray_mixedTypes() {
        JSONObject result = JSONParser.parse("root",
                "{\"arr\": [1, \"two\", true, null]}");
        List<?> arr = result.findArray("arr").orElse(null);
        assertNotNull(arr);
        assertEquals(4, arr.size());
        assertEquals(1, ((Number) arr.get(0)).intValue());
        assertEquals("two", arr.get(1));
        assertEquals(Boolean.TRUE, arr.get(2));
        assertNull(arr.get(3));
    }

    @Test
    public void parseArray_nested() {
        JSONObject result = JSONParser.parse("root",
                "{\"arr\": [[1, 2], [3, 4]]}");
        List<?> arr = result.findArray("arr").orElse(null);
        assertNotNull(arr);
        assertEquals(2, arr.size());
        assertTrue(arr.get(0) instanceof List);
        List<?> inner = (List<?>) arr.get(0);
        assertEquals(1, ((Number) inner.get(0)).intValue());
    }

    @Test
    public void parseArray_singleElement() {
        JSONObject result = JSONParser.parse("root", "{\"arr\": [42]}");
        List<?> arr = result.findArray("arr").orElse(null);
        assertNotNull(arr);
        assertEquals(1, arr.size());
    }

    @Test
    public void parseArray_missingOpenBracket_returnsNull() {
        JSONObject result = JSONParser.parse("root", "{\"arr\": 1, 2]}");
        assertNull(result.findArray("arr").orElse(null));
    }

    @Test
    public void parseArray_missingCloseBracket_returnsNull() {
        JSONObject result = JSONParser.parse("root", "{\"arr\": [1, 2}");
        assertNull(result.getValue());
    }

    @Test
    public void parseArray_trailingComma_returnsNull() {
        JSONObject result = JSONParser.parse("root", "{\"arr\": [1, 2,]}");
        assertNull(result.findArray("arr").orElse(null));
    }

    @Test
    public void parseArray_arrayOfObjects() {
        JSONObject result = JSONParser.parse("root",
                "{\"items\": [{\"id\": 1}, {\"id\": 2}]}");
        // Array of objects is stored as List<JSONObject>
        assertTrue(result.isObject());
        assertTrue(result.hasKey("items"));
    }

    @Test
    public void parseArray_commaInsideString_notSplit() {
        JSONObject result = JSONParser.parse("root",
                "{\"arr\": [\"a,b\", \"c,d\"]}");
        List<?> arr = result.findArray("arr").orElse(null);
        assertNotNull(arr);
        assertEquals(2, arr.size());
        assertEquals("a,b", arr.get(0));
        assertEquals("c,d", arr.get(1));
    }

    // Integration and complex structures ------------------------------------------

    @Test
    public void parse_personObject_allFieldsAccessible() {
        String json = "{"
                + "\"name\": \"Bob\","
                + "\"age\": 25,"
                + "\"active\": true,"
                + "\"score\": 9.5,"
                + "\"nickname\": null"
                + "}";
        JSONObject result = JSONParser.parse("person", json);
        assertEquals("Bob", result.findString("name").orElse(null));
        assertEquals(25, result.findInt("age").orElse(-1).intValue());
        assertEquals(Boolean.TRUE, result.findBoolean("active").orElse(null));
        assertEquals(9.5, result.findDouble("score").orElse(0.0), 0.001);
        assertTrue(result.hasKey("nickname"));
        assertFalse(result.findString("nickname").isPresent());
    }

    @Test
    public void parse_arrayAtRoot() {
        JSONObject result = JSONParser.parse("root", "[1, 2, 3]");
        assertNotNull(result.getValue());
        assertTrue(result.getValue() instanceof List);
        assertEquals(3, ((List<?>) result.getValue()).size());
    }

    @Test
    public void parse_stringAtRoot() {
        JSONObject result = JSONParser.parse("root", "\"hello\"");
        assertEquals("hello", result.getString());
    }

    @Test
    public void parse_numberAtRoot() {
        JSONObject result = JSONParser.parse("root", "99");
        assertEquals(99, result.getInt().intValue());
    }

    @Test
    public void parse_booleanAtRoot() {
        JSONObject result = JSONParser.parse("root", "true");
        assertEquals(Boolean.TRUE, result.getBoolean());
    }

    @Test
    public void parse_emptyObject_isObject() {
        JSONObject result = JSONParser.parse("root", "{}");
        assertTrue(result.isObject() || ((List<?>) result.getValue()).isEmpty());
    }

    @Test
    public void parse_objectWithEscapedKeyName() {
        JSONObject result = JSONParser.parse("root", "{\"ke\\\"y\": \"value\"}");
        // The key contains an escaped quote: ke"y
        assertTrue(result.hasKey("ke\"y"));
    }

    @Test
    public void parse_largeNumberOfFields_allPresent() {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < 20; i++) {
            if (i > 0)
                sb.append(",");
            sb.append("\"field").append(i).append("\": ").append(i);
        }
        sb.append("}");
        JSONObject result = JSONParser.parse("root", sb.toString());
        for (int i = 0; i < 20; i++) {
            assertEquals(i, result.findInt("field" + i).orElse(-1).intValue());
        }
    }

    @Test
    public void parse_allEscapesInValues_roundTrip() {
        String json = "{\"v\": \"\\\" \\\\ \\/ \\b \\f \\n \\r \\t\"}";
        JSONObject result = JSONParser.parse("root", json);
        String v = result.findString("v").orElse(null);
        assertNotNull(v);
        assertTrue(v.contains("\""));
        assertTrue(v.contains("\\"));
        assertTrue(v.contains("/"));
        assertTrue(v.contains("\b"));
        assertTrue(v.contains("\f"));
        assertTrue(v.contains("\n"));
        assertTrue(v.contains("\r"));
        assertTrue(v.contains("\t"));
    }

    // Invalid lines ---------------------------------------------------------------

    @Test
    public void parse_nullInput_handledGracefully() {
        // All parse paths that accept null should return null result gracefully
        JSONObject result = JSONParser.parse("key", (String) null);
        assertNull(result.getValue());
    }

    @Test
    public void parse_blankInput_handledGracefully() {
        JSONObject result = JSONParser.parse("key", "   \t  ");
        assertNull(result.getValue());
    }
}
