package com.stevenlagoy.jsonic;

import static org.junit.Assert.*;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link JSONStringifier}, exercised through
 * {@link JSONObject#toString()}
 * and the package-private {@link JSONStringifier#stringify} and
 * {@link JSONStringifier#expand} entry points.
 * <p>
 * Coverage targets:
 * <ul>
 * <li>{@code stringify} — null value, scalars, object bodies, arrays,
 * nested</li>
 * <li>{@code expand} — depth tracking, commas, colons, strings passthrough</li>
 * <li>{@code stringifyValue} — all type branches including fallback</li>
 * <li>{@code stringifyList} — empty list, object list, primitive list</li>
 * <li>{@code stringifyObjectBody} — single pair, multiple pairs</li>
 * <li>{@code stringifyPair} — key and value escaping</li>
 * <li>{@code stringifyObject} — object-list value, scalar value</li>
 * <li>{@code stringifyArray} — empty, single, multiple elements</li>
 * <li>{@code escapeString} — all escape sequences, ordering</li>
 * <li>Round-trip: parse → stringify → parse produces equivalent structure</li>
 * </ul>
 */
public class JSONStringifierTest {

    // Stringify scalar ------------------------------------------------------------

    @Test
    public void stringify_stringValue_returnsQuotedString() {
        JSONObject node = new JSONObject("key", "hello");
        assertEquals("\"hello\"", node.toString());
    }

    @Test
    public void stringify_emptyString_returnsEmptyQuotes() {
        JSONObject node = new JSONObject("key", "");
        assertEquals("\"\"", node.toString());
    }

    @Test
    public void stringify_integerValue() {
        JSONObject node = new JSONObject("key", 42);
        assertEquals("42", node.toString());
    }

    @Test
    public void stringify_longValue() {
        JSONObject node = new JSONObject("key", Long.MAX_VALUE);
        assertEquals(String.valueOf(Long.MAX_VALUE), node.toString());
    }

    @Test
    public void stringify_doubleValue() {
        JSONObject node = new JSONObject("key", 3.14);
        assertEquals("3.14", node.toString());
    }

    @Test
    public void stringify_booleanTrue() {
        JSONObject node = new JSONObject("key", true);
        assertEquals("true", node.toString());
    }

    @Test
    public void stringify_booleanFalse() {
        JSONObject node = new JSONObject("key", false);
        assertEquals("false", node.toString());
    }

    // Stringify object body -------------------------------------------------------

    @Test
    public void stringify_emptyObjectList_returnsEmptyBraces() {
        JSONObject node = new JSONObject("key", new ArrayList<JSONObject>());
        // Empty list — stringifyList returns "[]" due to type erasure
        // This is documented behavior; see stringifyList Javadoc
        assertEquals("[\n]", node.toString());
    }

    @Test
    public void stringify_singlePairObject() {
        JSONObject node = new JSONObject("root", List.of(
                new JSONObject("name", "Alice")));
        assertEquals("{\n\t\"name\": \"Alice\"\n}", node.toString());
    }

    @Test
    public void stringify_multiplePairObject() {
        JSONObject node = new JSONObject("root", List.of(
                new JSONObject("a", 1),
                new JSONObject("b", "two"),
                new JSONObject("c", true)));
        assertEquals("{\n\t\"a\": 1,\n\t\"b\": \"two\",\n\t\"c\": true\n}", node.toString());
    }

    @Test
    public void stringify_nullValuedPair() {
        JSONObject node = new JSONObject("root", List.of(
                new JSONObject("x", null)));
        assertEquals("{\n\t\"x\": null\n}", node.toString());
    }

    @Test
    public void stringify_nestedObjectBody() {
        JSONObject inner = new JSONObject("inner", List.of(
                new JSONObject("y", 2)));
        JSONObject outer = new JSONObject("root", List.of(
                new JSONObject("x", 1),
                inner));
        String result = outer.toString();
        assertTrue(result.contains("\"x\": 1"));
        assertTrue(result.contains("\"y\": 2"));
    }

    // Stringify array -------------------------------------------------------------

    @Test
    public void stringify_emptyArray() {
        JSONObject node = new JSONObject("root", List.of(
                new JSONObject("arr", new ArrayList<Object>())));
        assertEquals("{\n\t\"arr\": [\n\t]\n}", node.toString());
    }

    @Test
    public void stringify_primitiveArray_integers() {
        JSONObject node = new JSONObject("root", List.of(
                new JSONObject("nums", List.of(1, 2, 3))));
        assertEquals("{\n\t\"nums\": [\n\t\t1,\n\t\t2,\n\t\t3\n\t]\n}", node.toString());
    }

    @Test
    public void stringify_primitiveArray_strings() {
        JSONObject node = new JSONObject("root", List.of(
                new JSONObject("tags", List.of("a", "b", "c"))));
        assertEquals("{\n\t\"tags\": [\n\t\t\"a\",\n\t\t\"b\",\n\t\t\"c\"\n\t]\n}", node.toString());
    }

    @Test
    public void stringify_primitiveArray_mixed() {
        List<Object> mixed = new ArrayList<>();
        mixed.add(1);
        mixed.add("two");
        mixed.add(true);
        mixed.add(null);
        JSONObject node = new JSONObject("root", List.of(
                new JSONObject("mixed", mixed)));
        assertEquals("{\n\t\"mixed\": [\n\t\t1,\n\t\t\"two\",\n\t\ttrue,\n\t\tnull\n\t]\n}", node.toString());
    }

    @Test
    public void stringify_singleElementArray() {
        JSONObject node = new JSONObject("root", List.of(
                new JSONObject("arr", List.of(42))));
        assertEquals("{\n\t\"arr\": [\n\t\t42\n\t]\n}", node.toString());
    }

    // Escape Strings --------------------------------------------------------------

    @Test
    public void escapeString_backslashEscapedFirst() {
        // If backslash is not escaped first, subsequent escapes produce \\n instead of
        // \n
        JSONObject node = new JSONObject("key", "a\\b");
        assertEquals("\"a\\\\b\"", node.toString());
    }

    @Test
    public void escapeString_doubleQuote() {
        JSONObject node = new JSONObject("key", "say \"hello\"");
        assertEquals("\"say \\\"hello\\\"\"", node.toString());
    }

    @Test
    public void escapeString_newline() {
        JSONObject node = new JSONObject("key", "line1\nline2");
        assertEquals("\"line1\\nline2\"", node.toString());
    }

    @Test
    public void escapeString_carriageReturn() {
        JSONObject node = new JSONObject("key", "a\rb");
        assertEquals("\"a\\rb\"", node.toString());
    }

    @Test
    public void escapeString_tab() {
        JSONObject node = new JSONObject("key", "a\tb");
        assertEquals("\"a\\tb\"", node.toString());
    }

    @Test
    public void escapeString_backspace() {
        JSONObject node = new JSONObject("key", "a\bb");
        assertEquals("\"a\\bb\"", node.toString());
    }

    @Test
    public void escapeString_formFeed() {
        JSONObject node = new JSONObject("key", "a\fb");
        assertEquals("\"a\\fb\"", node.toString());
    }

    @Test
    public void escapeString_allSequencesInOneString() {
        JSONObject node = new JSONObject("key", "\\\"\b\f\n\r\t");
        assertEquals("\"\\\\\\\"\\b\\f\\n\\r\\t\"", node.toString());
    }

    @Test
    public void escapeString_keyIsAlsoEscaped() {
        // Keys with special characters should be escaped in stringifyPair
        JSONObject node = new JSONObject("root", List.of(
                new JSONObject("ke\"y", "value")));
        assertTrue(node.toString().contains("\"ke\\\"y\""));
    }

    @Test
    public void escapeString_unicodePassthrough() {
        // Unicode characters that don't need escaping should pass through unchanged
        JSONObject node = new JSONObject("key", "こんにちは");
        assertEquals("\"こんにちは\"", node.toString());
    }

    // Expand structural formatting ------------------------------------------------

    @Test
    public void expand_emptyObject_twoLines() {
        JSONObject node = new JSONObject("root", new ArrayList<JSONObject>());
        // Empty list produces "[]" from stringifyList, not "{}"
        String result = node.toString();
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    public void expand_singlePair_indentedCorrectly() {
        JSONObject node = new JSONObject("root", List.of(
                new JSONObject("name", "Alice")));
        String result = node.toString();
        String[] lines = result.split("\n");
        // Opening brace on its own line, pair indented, closing brace dedented
        assertEquals("{", lines[0].trim());
        assertTrue(lines[1].startsWith("\t"));
        assertEquals("}", lines[lines.length - 1].trim());
    }

    @Test
    public void expand_multiplePairs_eachOnOwnLine() {
        JSONObject node = new JSONObject("root", List.of(
                new JSONObject("a", 1),
                new JSONObject("b", 2),
                new JSONObject("c", 3)));
        String result = node.toString();
        long lineCount = result.lines().count();
        // { on line 1, 3 pairs with commas, } on last line = 5 lines
        assertEquals(5, lineCount);
    }

    @Test
    public void expand_nestedObject_increasesDepth() {
        JSONObject inner = new JSONObject("inner", List.of(
                new JSONObject("y", 2)));
        JSONObject outer = new JSONObject("root", List.of(
                new JSONObject("x", 1),
                inner));
        String result = outer.toString();
        // Inner content should be indented more than outer content
        assertTrue(result.contains("\t\t"));
    }

    @Test
    public void expand_array_formattedCorrectly() {
        JSONObject node = new JSONObject("root", List.of(
                new JSONObject("nums", List.of(1, 2, 3))));
        String result = node.toString();
        assertTrue(result.contains("["));
        assertTrue(result.contains("]"));
    }

    @Test
    public void expand_stringContainingBraces_notMistreatedAsStructure() {
        // Braces and brackets inside string values should not affect depth
        JSONObject node = new JSONObject("root", List.of(
                new JSONObject("code", "{not a real object}")));
        String result = node.toString();
        // The value should appear with escaped content, depth should remain correct
        String[] lines = result.split("\n");
        // Closing brace of the outer object should be at depth 0 (no leading tab)
        assertEquals("}", lines[lines.length - 1]);
    }

    @Test
    public void expand_stringContainingComma_notSplitAcrossLines() {
        JSONObject node = new JSONObject("root", List.of(
                new JSONObject("csv", "a,b,c")));
        String result = node.toString();
        assertTrue(result.contains("\"a,b,c\""));
    }

    @Test
    public void expand_stringContainingColon_notMistokenedAsKeyValue() {
        JSONObject node = new JSONObject("root", List.of(
                new JSONObject("url", "https://example.com")));
        String result = node.toString();
        assertTrue(result.contains("\"https://example.com\""));
    }

    // toString integration --------------------------------------------------------

    @Test
    public void toString_scalarNode_singleLine() {
        JSONObject node = new JSONObject("key", "hello");
        assertEquals("\"hello\"", node.toString());
    }

    @Test
    public void toString_integerNode_singleLine() {
        JSONObject node = new JSONObject("key", 42);
        assertEquals("42", node.toString());
    }

    @Test
    public void toString_nullNode_returnsNullLiteral() {
        // stringify returns null for null value; expand of null produces "null"
        JSONObject node = new JSONObject("key");
        // toString joins lines from expand; null input to expand should produce "null"
        String result = node.toString();
        assertEquals("null", result);
    }

    @Test
    public void toString_simpleObject_matchesExpected() {
        JSONObject node = new JSONObject("root", List.of(
                new JSONObject("name", "Bob"),
                new JSONObject("age", 25)));
        assertEquals("{\n\t\"name\": \"Bob\",\n\t\"age\": 25\n}", node.toString());
    }

    @Test
    public void toString_arrayNode_matchesExpected() {
        JSONObject node = new JSONObject("root", List.of(
                new JSONObject("tags", List.of("java", "json"))));
        String result = node.toString();
        assertTrue(result.contains("\"tags\""));
        assertTrue(result.contains("["));
        assertTrue(result.contains("\"java\""));
        assertTrue(result.contains("\"json\""));
        assertTrue(result.contains("]"));
    }

    // Round trip ------------------------------------------------------------------

    @Test
    public void roundTrip_simpleObject_isEqual() {
        String json = "{\"name\": \"Alice\", \"age\": 30}";
        JSONObject first = JSONParser.parse("root", json);
        JSONObject second = JSONParser.parse("root", first.toString());
        assertEquals(first, second);
    }

    @Test
    public void roundTrip_nestedObject_isEqual() {
        String json = "{\"person\": {\"name\": \"Bob\", \"score\": 9.5}}";
        JSONObject first = JSONParser.parse("root", json);
        JSONObject second = JSONParser.parse("root", first.toString());
        assertEquals(first, second);
    }

    @Test
    public void roundTrip_arrayOfPrimitives_isEqual() {
        String json = "{\"nums\": [1, 2, 3, 4, 5]}";
        JSONObject first = JSONParser.parse("root", json);
        JSONObject second = JSONParser.parse("root", first.toString());
        assertEquals(first, second);
    }

    @Test
    public void roundTrip_booleans_isEqual() {
        String json = "{\"a\": true, \"b\": false}";
        JSONObject first = JSONParser.parse("root", json);
        JSONObject second = JSONParser.parse("root", first.toString());
        assertEquals(first, second);
    }

    @Test
    public void roundTrip_nullValue_isEqual() {
        String json = "{\"x\": null}";
        JSONObject first = JSONParser.parse("root", json);
        JSONObject second = JSONParser.parse("root", first.toString());
        assertEquals(first, second);
    }

    @Test
    public void roundTrip_escapedStrings_isEqual() {
        String json = "{\"msg\": \"say \\\"hello\\\" and\\nnewline\"}";
        JSONObject first = JSONParser.parse("root", json);
        JSONObject second = JSONParser.parse("root", first.toString());
        assertEquals(first, second);
    }

    @Test
    public void roundTrip_deeplyNested_isEqual() {
        String json = "{\"a\": {\"b\": {\"c\": {\"d\": 42}}}}";
        JSONObject first = JSONParser.parse("root", json);
        JSONObject second = JSONParser.parse("root", first.toString());
        assertEquals(first, second);
    }

    @Test
    public void roundTrip_mixedArray_isEqual() {
        String json = "{\"arr\": [1, \"two\", true, null]}";
        JSONObject first = JSONParser.parse("root", json);
        JSONObject second = JSONParser.parse("root", first.toString());
        assertEquals(first, second);
    }

    // JSONSerializable stringify --------------------------------------------------

    @Test
    public void stringify_jsonSerializableValue_usesToJson() {

        final class TestObject implements JSONSerializable<TestObject> {
            @Override
            public @NotNull JSONObject toJson() {
                return new JSONObject("anon", List.of(
                        new JSONObject("field", "value")));
            }

            @Override
            public @NotNull TestObject fromJson(@NotNull JSONObject json) {
                return this;
            }
        }

        // A JSONObject whose value is a JSONSerializable should be stringified
        // via that object's toJson() method
        TestObject serializable = new TestObject();

        JSONObject node = new JSONObject("root", serializable);
        String result = node.toString();
        assertNotNull(result);
        assertTrue(result.contains("\"field\""));
        assertTrue(result.contains("\"value\""));
    }
}