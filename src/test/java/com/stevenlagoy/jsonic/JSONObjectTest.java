package com.stevenlagoy.jsonic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Test;

/**
 * Tests for {@link JSONObject}.
 * <p>
 * Coverage targets:
 * <ul>
 * <li>Constructors — no-arg, key-only, key-value, key-lines, key-object</li>
 * <li>Structural queries — isNull, isArray, isObject, isScalar, size</li>
 * <li>Key presence — hasKey, hasAnyKey, hasAllKeys and their require*
 * variants</li>
 * <li>requireAllowedKeys, requireStructure, getFieldKeys</li>
 * <li>Direct value access — getValue(), getValue(Class), getValue(String),
 * requireValue, getString/Number/Int/Long/Double/Float/Boolean/Json/Array,
 * and their require* variants</li>
 * <li>Subtree search — find, findAll, require (untyped and typed),
 * all findString/Number/Int/Long/Double/Float/Boolean/Json/Array families</li>
 * <li>Path navigation — findNodeAt, requireNodeAt, findAt, requireAt,
 * and all findXxxAt/requireXxxAt methods</li>
 * <li>Mutation — setValue, add, addAll, put, putIfAbsent, remove, merge</li>
 * <li>Conversion — toMap, toList, toObject</li>
 * <li>Iteration — iterator, childIterator, stream, childStream</li>
 * <li>Cloning — clone deep-copies JSONObject values and lists</li>
 * <li>Object methods — toString, equals, hashCode</li>
 * </ul>
 */
public class JSONObjectTest {

    // Constructors ----------------------------------------------------------------

    @Test
    public void constructor_noArg_emptyKeyNullValue() {
        JSONObject node = new JSONObject();
        assertEquals("", node.getKey());
        assertNull(node.getValue());
    }

    @Test
    public void constructor_keyOnly_nullValue() {
        JSONObject node = new JSONObject("key");
        assertEquals("key", node.getKey());
        assertNull(node.getValue());
    }

    @Test
    public void constructor_keyValue_string() {
        JSONObject node = new JSONObject("key", "hello");
        assertEquals("key", node.getKey());
        assertEquals("hello", node.getValue());
    }

    @Test
    public void constructor_keyValue_integer() {
        JSONObject node = new JSONObject("n", 42);
        assertEquals(42, node.getValue());
    }

    @Test
    public void constructor_keyValue_boolean() {
        JSONObject node = new JSONObject("b", true);
        assertEquals(Boolean.TRUE, node.getValue());
    }

    @Test
    public void constructor_keyValue_nullExplicit() {
        JSONObject node = new JSONObject("key", (Object) null);
        assertNull(node.getValue());
    }

    @Test
    public void constructor_keyLines_validJson() {
        JSONObject node = new JSONObject("root", List.of("{\"x\": 1}"));
        assertEquals(1, node.findInt("x").orElse(-1).intValue());
    }

    @Test
    public void constructor_keyLines_nullLines_nullValue() {
        JSONObject node = new JSONObject("root", null);
        assertNull(node.getValue());
    }

    @Test
    public void constructor_keyLines_invalidJson_listValue() {
        JSONObject node = new JSONObject("root", List.of("not json"));
        assertEquals(List.of("not json"), node.getValue());
    }

    // getKey and setKey -----------------------------------------------------------

    @Test
    public void getKey_returnsKey() {
        assertEquals("myKey", new JSONObject("myKey").getKey());
    }

    @Test
    public void setKey_updatesKey() {
        JSONObject node = new JSONObject("old");
        node.setKey("new");
        assertEquals("new", node.getKey());
    }

    // Structural queries ----------------------------------------------------------

    @Test
    public void isNull_nullValue_true() {
        assertTrue(new JSONObject("k").isNull());
    }

    @Test
    public void isNull_nonNullValue_false() {
        assertFalse(new JSONObject("k", "v").isNull());
    }

    @Test
    public void isObject_listOfJsonObjects_true() {
        JSONObject node = new JSONObject("root", List.of(new JSONObject("a", 1)));
        assertTrue(node.isObject());
    }

    @Test
    public void isObject_emptyList_false() {
        JSONObject node = new JSONObject("root", new ArrayList<>());
        assertFalse(node.isObject());
    }

    @Test
    public void isObject_primitiveList_false() {
        JSONObject node = new JSONObject("root", List.of(1, 2, 3));
        assertFalse(node.isObject());
    }

    @Test
    public void isObject_scalar_false() {
        assertFalse(new JSONObject("k", "v").isObject());
    }

    @Test
    public void isArray_primitiveList_true() {
        JSONObject node = new JSONObject("root", List.of(1, 2, 3));
        assertTrue(node.isArray());
    }

    @Test
    public void isArray_listOfJsonObjects_false() {
        JSONObject node = new JSONObject("root", List.of(new JSONObject("a")));
        assertFalse(node.isArray());
    }

    @Test
    public void isArray_scalar_false() {
        assertFalse(new JSONObject("k", 1).isArray());
    }

    @Test
    public void isScalar_string_true() {
        assertTrue(new JSONObject("k", "v").isScalar());
    }

    @Test
    public void isScalar_number_true() {
        assertTrue(new JSONObject("k", 42).isScalar());
    }

    @Test
    public void isScalar_boolean_true() {
        assertTrue(new JSONObject("k", true).isScalar());
    }

    @Test
    public void isScalar_null_false() {
        assertFalse(new JSONObject("k").isScalar());
    }

    @Test
    public void isScalar_list_false() {
        assertFalse(new JSONObject("k", List.of(1)).isScalar());
    }

    @Test
    public void size_null_zero() {
        assertEquals(0, new JSONObject("k").size());
    }

    @Test
    public void size_scalar_one() {
        assertEquals(1, new JSONObject("k", "v").size());
    }

    @Test
    public void size_list_returnsListSize() {
        JSONObject node = new JSONObject("k", List.of(1, 2, 3));
        assertEquals(3, node.size());
    }

    @Test
    public void size_objectList_returnsChildCount() {
        JSONObject node = new JSONObject("root", List.of(
                new JSONObject("a"), new JSONObject("b")));
        assertEquals(2, node.size());
    }

    // hasKey and hasAnyKey and hasAllKeys -----------------------------------------

    @Test
    public void hasKey_thisNode_true() {
        JSONObject node = new JSONObject("target", 1);
        assertTrue(node.hasKey("target"));
    }

    @Test
    public void hasKey_childNode_true() {
        JSONObject root = JSONParser.parse("root", "{\"name\": \"Alice\"}");
        assertTrue(root.hasKey("name"));
    }

    @Test
    public void hasKey_missingKey_false() {
        JSONObject root = JSONParser.parse("root", "{\"name\": \"Alice\"}");
        assertFalse(root.hasKey("age"));
    }

    @Test
    public void hasKey_nullValuedKey_true() {
        JSONObject root = JSONParser.parse("root", "{\"x\": null}");
        assertTrue(root.hasKey("x"));
    }

    @Test
    public void hasKey_deeplyNested_true() {
        JSONObject root = JSONParser.parse("root", "{\"a\": {\"b\": {\"c\": 1}}}");
        assertTrue(root.hasKey("c"));
    }

    @Test
    public void hasAnyKey_firstPresent_true() {
        JSONObject root = JSONParser.parse("root", "{\"a\": 1}");
        assertTrue(root.hasAnyKey("a", "b", "c"));
    }

    @Test
    public void hasAnyKey_nonePresent_false() {
        JSONObject root = JSONParser.parse("root", "{\"x\": 1}");
        assertFalse(root.hasAnyKey("a", "b", "c"));
    }

    @Test
    public void hasAllKeys_allPresent_true() {
        JSONObject root = JSONParser.parse("root", "{\"a\": 1, \"b\": 2, \"c\": 3}");
        assertTrue(root.hasAllKeys("a", "b", "c"));
    }

    @Test
    public void hasAllKeys_oneMissing_false() {
        JSONObject root = JSONParser.parse("root", "{\"a\": 1, \"b\": 2}");
        assertFalse(root.hasAllKeys("a", "b", "c"));
    }

    @Test
    public void requireKey_present_noException() {
        JSONObject root = JSONParser.parse("root", "{\"id\": 1}");
        root.requireKey("id"); // should not throw
    }

    @Test(expected = IllegalArgumentException.class)
    public void requireKey_absent_throws() {
        JSONParser.parse("root", "{\"id\": 1}").requireKey("missing");
    }

    @Test(expected = IllegalArgumentException.class)
    public void requireKey_customSupplier_throwsCustomException() {
        JSONParser.parse("root", "{\"id\": 1}").requireKey("missing",
                () -> new IllegalArgumentException("custom message"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void requireAnyKey_nonePresent_throws() {
        JSONParser.parse("root", "{\"x\": 1}").requireAnyKey("a", "b");
    }

    @Test
    public void requireAnyKey_onePresent_noException() {
        JSONParser.parse("root", "{\"a\": 1}").requireAnyKey("a", "b"); // should not throw
    }

    @Test(expected = IllegalArgumentException.class)
    public void requireAllKeys_oneMissing_throws() {
        JSONParser.parse("root", "{\"a\": 1}").requireAllKeys("a", "b");
    }

    @Test
    public void requireAllKeys_allPresent_noException() {
        JSONParser.parse("root", "{\"a\": 1, \"b\": 2}").requireAllKeys("a", "b"); // should not throw
    }

    @Test(expected = IllegalArgumentException.class)
    public void requireAllKeys_customSupplier_throwsCustomException() {
        JSONParser.parse("root", "{\"a\": 1}").requireAllKeys(
                List.of("a", "missing"),
                () -> new IllegalArgumentException("custom"));
    }

    // requireAllowedKeys and requireStructure and getFieldKeys --------------------

    @Test
    public void requireAllowedKeys_allAllowed_noException() {
        JSONObject root = JSONParser.parse("root", "{\"a\": 1, \"b\": 2}");
        root.requireAllowedKeys(List.of("a", "b", "c")); // "c" allowed but not present
    }

    @Test(expected = IllegalArgumentException.class)
    public void requireAllowedKeys_extraKey_throws() {
        JSONObject root = JSONParser.parse("root", "{\"a\": 1, \"unexpected\": 2}");
        root.requireAllowedKeys(List.of("a"));
    }

    @Test
    public void getFieldKeys_returnsImmediateChildKeys() {
        JSONObject root = JSONParser.parse("root", "{\"a\": 1, \"b\": 2, \"c\": 3}");
        List<String> keys = root.getFieldKeys();
        assertEquals(3, keys.size());
        assertTrue(keys.containsAll(List.of("a", "b", "c")));
    }

    @Test
    public void getFieldKeys_scalarNode_empty() {
        List<String> keys = new JSONObject("k", "v").getFieldKeys();
        assertTrue(keys.isEmpty());
    }

    @Test
    public void requireStructure_key_validatesNestedObject() {
        JSONObject root = JSONParser.parse("root", "{\"org\": {\"name\": \"Acme\"}}");
        root.requireStructure("org", org -> org.requireKey("name")); // should not throw
    }

    @Test(expected = IllegalArgumentException.class)
    public void requireStructure_key_validationFails_throws() {
        JSONObject root = JSONParser.parse("root", "{\"org\": {\"name\": \"Acme\"}}");
        root.requireStructure("org", org -> org.requireKey("missing"));
    }

    @Test
    public void requireStructure_path_validatesNestedObject() {
        JSONObject root = JSONParser.parse("root", "{\"a\": {\"b\": {\"c\": 1}}}");
        root.requireStructure(node -> node.requireKey("c"), "a", "b");
    }

    // Direct value access ---------------------------------------------------------

    @Test
    public void getValue_returnsRawValue() {
        assertEquals("hello", new JSONObject("k", "hello").getValue());
    }

    @Test
    public void getValue_null_returnsNull() {
        assertNull(new JSONObject("k").getValue());
    }

    @Test
    public void getValue_class_matchingType_returnsValue() {
        assertEquals("hello", new JSONObject("k", "hello").getValue(String.class));
    }

    @Test
    public void getValue_class_mismatchedType_returnsNull() {
        assertNull(new JSONObject("k", "hello").getValue(Integer.class));
    }

    @Test
    public void getValue_class_withDefault_mismatch_returnsDefault() {
        Integer result = new JSONObject("k", "hello").getValue(Integer.class, () -> 99);
        assertEquals(99, result.intValue());
    }

    @Test
    public void getValue_byChildKey_returnsChildValue() {
        JSONObject root = JSONParser.parse("root", "{\"x\": 42}");
        Object val = root.getValue("x");
        assertEquals(42, ((Number) val).intValue());
    }

    @Test
    public void getValue_byChildKey_missingKey_returnsNull() {
        JSONObject root = JSONParser.parse("root", "{\"x\": 42}");
        assertNull(root.getValue("missing"));
    }

    @Test
    public void getValue_byChildKey_doesNotSearchDeep() {
        // getValue(String) searches only immediate children, not the whole subtree
        JSONObject root = JSONParser.parse("root", "{\"a\": {\"b\": 1}}");
        // "b" is not an immediate child of root
        assertNull(root.getValue("b"));
    }

    @Test
    public void requireValue_nonNull_returnsValue() {
        assertEquals("hello", new JSONObject("k", "hello").requireValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void requireValue_null_throws() {
        new JSONObject("k").requireValue();
    }

    @Test
    public void requireValue_class_matchingType() {
        assertEquals("hello", new JSONObject("k", "hello").requireValue(String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void requireValue_class_mismatch_throws() {
        new JSONObject("k", "hello").requireValue(Integer.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void requireValue_class_customSupplier_throws() {
        new JSONObject("k", "hello").requireValue(Integer.class,
                () -> new IllegalArgumentException("wrong type"));
    }

    // Typed direct accessors ------------------------------------------------------

    @Test
    public void getString_stringValue_returnsString() {
        assertEquals("hello", new JSONObject("k", "hello").getString());
    }

    @Test
    public void getString_nonStringValue_returnsNull() {
        assertNull(new JSONObject("k", 42).getString());
    }

    @Test
    public void requireString_stringValue_returnsString() {
        assertEquals("hello", new JSONObject("k", "hello").requireString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void requireString_nonStringValue_throws() {
        new JSONObject("k", 42).requireString();
    }

    @Test
    public void getNumber_numberValue_returnsNumber() {
        assertNotNull(new JSONObject("k", 3.14).getNumber());
    }

    @Test
    public void getNumber_nonNumberValue_returnsNull() {
        assertNull(new JSONObject("k", "text").getNumber());
    }

    @Test
    public void getInt_longValue_coercesCorrectly() {
        // Parser stores integers as Long or Integer; getInt() coerces via intValue()
        JSONObject node = JSONParser.parse("root", "{\"x\": 99}");
        JSONObject child = (JSONObject) ((List<?>) node.getValue()).get(0);
        assertEquals(99, child.getInt().intValue());
    }

    @Test
    public void getLong_returnsLong() {
        JSONObject node = new JSONObject("k", Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, node.getLong().longValue());
    }

    @Test
    public void getDouble_returnsDouble() {
        JSONObject node = new JSONObject("k", 2.718);
        assertEquals(2.718, node.getDouble(), 0.001);
    }

    @Test
    public void getFloat_returnsFloat() {
        JSONObject node = new JSONObject("k", 1.5f);
        assertEquals(1.5f, node.getFloat(), 0.001f);
    }

    @Test
    public void getBoolean_booleanValue_returnsBoolean() {
        assertEquals(Boolean.TRUE, new JSONObject("k", true).getBoolean());
    }

    @Test
    public void getBoolean_nonBooleanValue_returnsNull() {
        assertNull(new JSONObject("k", 1).getBoolean());
    }

    @Test
    public void getJson_jsonObjectValue_returnsJson() {
        JSONObject inner = new JSONObject("inner", "val");
        JSONObject outer = new JSONObject("outer", inner);
        assertEquals(inner, outer.getJson());
    }

    @Test
    public void getJson_nonJsonValue_returnsNull() {
        assertNull(new JSONObject("k", "v").getJson());
    }

    @Test
    public void getArray_listValue_returnsList() {
        JSONObject node = new JSONObject("k", List.of(1, 2, 3));
        assertNotNull(node.getArray());
        assertEquals(3, node.getArray().size());
    }

    @Test
    public void getArray_nonListValue_returnsNull() {
        assertNull(new JSONObject("k", "v").getArray());
    }

    @Test
    public void requireArray_listValue_returnsList() {
        JSONObject node = new JSONObject("k", List.of(1, 2));
        assertEquals(2, node.requireArray().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void requireArray_nonListValue_throws() {
        new JSONObject("k", "v").requireArray();
    }

    @Test(expected = IllegalArgumentException.class)
    public void requireArray_customSupplier_throwsCustom() {
        new JSONObject("k", "v").requireArray(
                () -> new IllegalArgumentException("not an array"));
    }

    // Subtree search --------------------------------------------------------------

    @Test
    public void find_presentKey_returnsValue() {
        JSONObject root = JSONParser.parse("root", "{\"name\": \"Alice\"}");
        assertTrue(root.find("name").isPresent());
        assertEquals("Alice", root.find("name").get());
    }

    @Test
    public void find_missingKey_empty() {
        assertFalse(JSONParser.parse("root", "{\"a\": 1}").find("missing").isPresent());
    }

    @Test
    public void find_nullValuedKey_empty() {
        // find returns empty for null values (Optional cannot hold null)
        JSONObject root = JSONParser.parse("root", "{\"x\": null}");
        assertFalse(root.find("x").isPresent());
        assertTrue(root.hasKey("x")); // but hasKey correctly returns true
    }

    @Test
    public void find_withDefault_missingKey_returnsDefault() {
        Object result = JSONParser.parse("root", "{\"a\": 1}").find("missing", () -> "default");
        assertEquals("default", result);
    }

    @Test
    public void find_withDefault_presentKey_returnsValue() {
        Object result = JSONParser.parse("root", "{\"a\": 1}").find("a", () -> "default");
        assertNotNull(result);
        assertEquals(1, ((Number) result).intValue());
    }

    @Test
    public void require_presentKey_returnsValue() {
        assertEquals("Alice", JSONParser.parse("root", "{\"name\": \"Alice\"}").require("name"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void require_missingKey_throws() {
        JSONParser.parse("root", "{\"a\": 1}").require("missing");
    }

    @Test
    public void find_varargs_firstMatchReturned() {
        JSONObject root = JSONParser.parse("root", "{\"b\": 2}");
        assertTrue(root.find("a", "b", "c").isPresent());
        assertEquals(2, ((Number) root.find("a", "b", "c").get()).intValue());
    }

    @Test
    public void find_collection_withDefault_returnsDefault() {
        Object result = JSONParser.parse("root", "{\"x\": 1}").find(List.of("a", "b"), () -> "fallback");
        assertEquals("fallback", result);
    }

    @Test
    public void require_collection_present() {
        Object result = JSONParser.parse("root", "{\"b\": 2}").require(List.of("a", "b"));
        assertEquals(2, ((Number) result).intValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void require_collection_nonePresent_throws() {
        JSONParser.parse("root", "{\"x\": 1}").require(List.of("a", "b"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void require_collection_customSupplier_throws() {
        JSONParser.parse("root", "{\"x\": 1}").require(List.of("a", "b"),
                () -> new IllegalArgumentException("custom"));
    }

    @Test
    public void findAll_singleMatch_returnsOne() {
        List<Object> result = JSONParser.parse("root", "{\"a\": 1}").findAll("a");
        assertEquals(1, result.size());
    }

    @Test
    public void findAll_noMatch_empty() {
        assertTrue(JSONParser.parse("root", "{\"a\": 1}").findAll("missing").isEmpty());
    }

    @Test
    public void findAll_multipleMatchesDifferentDepths_returnsAll() {
        JSONObject root = JSONParser.parse("root", "{\"x\": 1, \"nested\": {\"x\": 2}}");
        List<Object> results = root.findAll("x");
        assertEquals(2, results.size());
    }

    // Typed search String ---------------------------------------------------------

    @Test
    public void findString_present_returnsString() {
        assertEquals("Alice", JSONParser.parse("root", "{\"name\": \"Alice\"}").findString("name").orElse(null));
    }

    @Test
    public void findString_absent_empty() {
        assertFalse(JSONParser.parse("root", "{\"a\": 1}").findString("name").isPresent());
    }

    @Test
    public void findString_wrongType_empty() {
        assertFalse(JSONParser.parse("root", "{\"count\": 5}").findString("count").isPresent());
    }

    @Test
    public void findString_withDefault_absent_returnsDefault() {
        assertEquals("anon", JSONParser.parse("root", "{\"a\": 1}").findString("name", () -> "anon"));
    }

    @Test
    public void requireString_present_returnsString() {
        assertEquals("Alice", JSONParser.parse("root", "{\"name\": \"Alice\"}").requireString("name"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void requireString_absent_throws() {
        JSONParser.parse("root", "{\"a\": 1}").requireString("name");
    }

    @Test(expected = IllegalArgumentException.class)
    public void requireString_customSupplier_throwsCustom() {
        JSONParser.parse("root", "{\"a\": 1}").requireString("name",
                () -> new IllegalArgumentException("no name"));
    }

    @Test
    public void findString_varargs_firstMatchReturned() {
        assertEquals("Bob",
                JSONParser.parse("root", "{\"username\": \"Bob\"}").findString("name", "username").orElse(null));
    }

    @Test
    public void findString_collection_withDefault() {
        String result = JSONParser.parse("root", "{\"x\": 1}").findString(List.of("a", "b"), () -> "default");
        assertEquals("default", result);
    }

    @Test
    public void requireString_collection_present() {
        assertEquals("yes", JSONParser.parse("root", "{\"b\": \"yes\"}").requireString(List.of("a", "b")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void requireString_collection_absent_throws() {
        JSONParser.parse("root", "{\"x\": 1}").requireString(List.of("a", "b"));
    }

    // Typed search Integer --------------------------------------------------------

    @Test
    public void findInt_present_returnsInt() {
        assertEquals(42, JSONParser.parse("root", "{\"n\": 42}").findInt("n").orElse(-1).intValue());
    }

    @Test
    public void findInt_longStoredValue_coercesCorrectly() {
        // Large integers may be stored as Long; findInt coerces via Number::intValue
        JSONObject root = JSONParser.parse("root", "{\"n\": 99}");
        assertEquals(99, root.findInt("n").orElse(-1).intValue());
    }

    @Test
    public void findInt_absent_empty() {
        assertFalse(JSONParser.parse("root", "{\"a\": \"x\"}").findInt("n").isPresent());
    }

    @Test
    public void findInt_withDefault() {
        assertEquals(0, JSONParser.parse("root", "{\"a\": 1}").findInt("missing", () -> 0).intValue());
    }

    @Test
    public void requireInt_present_returnsInt() {
        assertEquals(5, JSONParser.parse("root", "{\"x\": 5}").requireInt("x").intValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void requireInt_absent_throws() {
        JSONParser.parse("root", "{\"a\": 1}").requireInt("missing");
    }

    @Test
    public void findDouble_present_returnsDouble() {
        assertEquals(3.14, JSONParser.parse("root", "{\"pi\": 3.14}").findDouble("pi").orElse(0.0), 0.001);
    }

    @Test
    public void findLong_present_returnsLong() {
        long val = (long) Integer.MAX_VALUE + 1;
        JSONObject node = new JSONObject("root", List.of(new JSONObject("big", val)));
        assertEquals(val, node.findLong("big").orElse(0L).longValue());
    }

    @Test
    public void findBoolean_true() {
        assertEquals(Boolean.TRUE, JSONParser.parse("root", "{\"flag\": true}").findBoolean("flag").orElse(null));
    }

    @Test
    public void findBoolean_false() {
        assertEquals(Boolean.FALSE, JSONParser.parse("root", "{\"flag\": false}").findBoolean("flag").orElse(null));
    }

    // findJson and requireJson ----------------------------------------------------

    @Test
    public void findJson_present_returnsJsonObject() {
        JSONObject root = JSONParser.parse("root", "{\"inner\": {\"x\": 1}}");
        assertTrue(root.findJson("inner").isPresent());
        // findJson finds a value that IS a JSONObject instance
        // Verify via findInt reaching through the structure instead
        assertEquals(1, root.findInt("x").orElse(-1).intValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void requireJson_absent_throws() {
        JSONParser.parse("root", "{\"a\": 1}").requireJson("missing");
    }

    // findArray and requireArray --------------------------------------------------

    @Test
    public void findArray_present_returnsList() {
        JSONObject root = JSONParser.parse("root", "{\"tags\": [\"a\", \"b\"]}");
        Optional<List<?>> arr = root.findArray("tags");
        assertTrue(arr.isPresent());
        assertEquals(2, arr.get().size());
    }

    @Test
    public void findArray_absent_empty() {
        assertFalse(JSONParser.parse("root", "{\"a\": 1}").findArray("tags").isPresent());
    }

    @Test
    public void findArray_withDefault_returnsDefault() {
        List<?> result = JSONParser.parse("root", "{\"a\": 1}").findArray("tags", ArrayList::new);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void requireArray_key_present() {
        List<?> result = JSONParser.parse("root", "{\"nums\": [1,2,3]}").requireArray("nums");
        assertEquals(3, result.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void requireArray_key_absent_throws() {
        JSONParser.parse("root", "{\"a\": 1}").requireArray("missing");
    }

    // Path navigation -------------------------------------------------------------

    @Test
    public void findNodeAt_dotPath_resolvesCorrectly() {
        JSONObject root = JSONParser.parse("root", "{\"a\": {\"b\": {\"c\": 42}}}");
        Optional<JSONObject> node = root.findNodeAt("a.b.c");
        assertTrue(node.isPresent());
        assertEquals(42, node.get().getInt().intValue());
    }

    @Test
    public void findNodeAt_varargs_resolvesCorrectly() {
        JSONObject root = JSONParser.parse("root", "{\"a\": {\"b\": 1}}");
        assertTrue(root.findNodeAt("a", "b").isPresent());
    }

    @Test
    public void findNodeAt_rootKeyInPath_skipsRoot() {
        JSONObject root = JSONParser.parse("root", "{\"x\": 5}");
        // If the first segment matches root's key, it is stepped into
        Optional<JSONObject> node = root.findNodeAt("root.x");
        assertTrue(node.isPresent());
    }

    @Test
    public void findNodeAt_emptyPath_returnsThis() {
        JSONObject root = JSONParser.parse("root", "{\"a\": 1}");
        Optional<JSONObject> result = root.findNodeAt();
        assertTrue(result.isPresent());
        assertSame(root, result.get());
    }

    @Test
    public void findNodeAt_invalidPath_empty() {
        JSONObject root = JSONParser.parse("root", "{\"a\": 1}");
        assertFalse(root.findNodeAt("a.b.c").isPresent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void requireNodeAt_invalid_throws() {
        JSONParser.parse("root", "{\"a\": 1}").requireNodeAt("a.b.missing");
    }

    @Test
    public void findAt_resolvesValue() {
        JSONObject root = JSONParser.parse("root", "{\"a\": {\"b\": 99}}");
        Optional<?> val = root.findAt("a.b");
        assertTrue(val.isPresent());
        assertEquals(99, ((Number) val.get()).intValue());
    }

    @Test
    public void requireAt_present_returnsValue() {
        Object val = JSONParser.parse("root", "{\"a\": {\"b\": 7}}").requireAt("a.b");
        assertEquals(7, ((Number) val).intValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void requireAt_missing_throws() {
        JSONParser.parse("root", "{\"a\": 1}").requireAt("a.b.missing");
    }

    @Test
    public void findStringAt_resolvesString() {
        JSONObject root = JSONParser.parse("root", "{\"user\": {\"name\": \"Alice\"}}");
        assertEquals("Alice", root.findStringAt("user.name").orElse(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void requireStringAt_missing_throws() {
        JSONParser.parse("root", "{\"a\": 1}").requireStringAt("a.b.missing");
    }

    @Test
    public void findIntAt_resolvesInt() {
        JSONObject root = JSONParser.parse("root", "{\"stats\": {\"score\": 100}}");
        assertEquals(100, root.findIntAt("stats.score").orElse(-1).intValue());
    }

    @Test
    public void findDoubleAt_resolvesDouble() {
        JSONObject root = JSONParser.parse("root", "{\"pos\": {\"lat\": 51.5}}");
        assertEquals(51.5, root.findDoubleAt("pos.lat").orElse(0.0), 0.01);
    }

    @Test
    public void findBooleanAt_resolvesBoolean() {
        JSONObject root = JSONParser.parse("root", "{\"config\": {\"enabled\": true}}");
        assertEquals(Boolean.TRUE, root.findBooleanAt("config.enabled").orElse(null));
    }

    @Test
    public void findArrayAt_resolvesList() {
        JSONObject root = JSONParser.parse("root", "{\"data\": {\"tags\": [\"a\", \"b\"]}}");
        Optional<List<?>> arr = root.findArrayAt("data.tags");
        assertTrue(arr.isPresent());
        assertEquals(2, arr.get().size());
    }

    @Test
    public void findNumberAt_resolvesNumber() {
        JSONObject root = JSONParser.parse("root", "{\"x\": {\"val\": 3}}");
        assertTrue(root.findNumberAt("x.val").isPresent());
    }

    // setValue --------------------------------------------------------------------

    @Test
    public void setValue_null_setsNull() {
        JSONObject node = new JSONObject("k", "v");
        node.setValue(null);
        assertNull(node.getValue());
    }

    @Test
    public void setValue_string_setsString() {
        JSONObject node = new JSONObject("k");
        node.setValue("hello");
        assertEquals("hello", node.getValue());
    }

    @Test
    public void setValue_list_setsList() {
        JSONObject node = new JSONObject("k");
        node.setValue(List.of(1, 2, 3));
        assertTrue(node.isArray());
    }

    // add and addAll --------------------------------------------------------------

    @Test
    public void add_toObjectList_succeeds() {
        JSONObject root = new JSONObject("root", new ArrayList<>(List.of(new JSONObject("a", 1))));
        boolean result = root.add(new JSONObject("b", 2));
        assertTrue(result);
        assertEquals(2, root.size());
    }

    @Test(expected = IllegalStateException.class)
    public void add_toScalarNode_throws() {
        new JSONObject("k", "v").add(new JSONObject("x"));
    }

    @Test(expected = IllegalStateException.class)
    public void add_toPrimitiveList_throws() {
        JSONObject node = new JSONObject("k", new ArrayList<>(List.of(1, 2)));
        node.add(new JSONObject("x"));
    }

    @Test
    public void addAll_addsMultiple() {
        JSONObject root = new JSONObject("root", new ArrayList<>(List.of(new JSONObject("a", 1))));
        root.addAll(new JSONObject("b", 2), new JSONObject("c", 3));
        assertEquals(3, root.size());
    }

    // put and putIfAbsent and remove ----------------------------------------------

    @Test
    public void put_newKey_addsChild() {
        JSONObject root = new JSONObject("root");
        Object prev = root.put("x", 1);
        assertNull(prev);
        assertEquals(1, root.findInt("x").orElse(-1).intValue());
    }

    @Test
    public void put_existingKey_replacesAndReturnsPrevious() {
        JSONObject root = new JSONObject("root");
        root.put("x", 1);
        Object prev = root.put("x", 2);
        assertEquals(1, ((Number) prev).intValue());
        assertEquals(2, root.findInt("x").orElse(-1).intValue());
    }

    @Test(expected = IllegalStateException.class)
    public void put_scalarNode_throws() {
        new JSONObject("k", "v").put("x", 1);
    }

    @Test
    public void put_nullValue_nullNode_initializes() {
        JSONObject root = new JSONObject("root");
        root.put("x", 1);
        assertTrue(root.isObject());
    }

    @Test
    public void putIfAbsent_keyAbsent_putsValue() {
        JSONObject root = new JSONObject("root");
        Object prev = root.putIfAbsent("x", 42);
        assertNull(prev);
        assertEquals(42, root.findInt("x").orElse(-1).intValue());
    }

    @Test
    public void putIfAbsent_keyPresent_doesNotReplace() {
        JSONObject root = new JSONObject("root");
        root.put("x", 1);
        Object existing = root.putIfAbsent("x", 99);
        assertEquals(1, ((Number) existing).intValue());
        assertEquals(1, root.findInt("x").orElse(-1).intValue());
    }

    @Test
    public void putIfAbsent_keyPresentWithNull_replacesNull() {
        JSONObject root = new JSONObject("root");
        root.put("x", null);
        Object prev = root.putIfAbsent("x", 42);
        assertNull(prev);
        assertEquals(42, root.findInt("x").orElse(-1).intValue());
    }

    @Test
    public void remove_existingKey_removesAndReturnsTrue() {
        JSONObject root = new JSONObject("root");
        root.put("x", 1);
        assertTrue(root.remove("x"));
        assertFalse(root.hasKey("x"));
    }

    @Test
    public void remove_missingKey_returnsFalse() {
        JSONObject root = new JSONObject("root");
        assertFalse(root.remove("missing"));
    }

    @Test
    public void remove_scalarNode_returnsFalse() {
        assertFalse(new JSONObject("k", "v").remove("k"));
    }

    // merge -----------------------------------------------------------------------

    @Test
    public void merge_addsFieldsAndReturnsThis() {
        JSONObject root = new JSONObject("root", new ArrayList<>(List.of(new JSONObject("a", 1))));
        JSONObject result = root.merge(new JSONObject("b", 2), new JSONObject("c", 3));
        assertSame(root, result);
        assertEquals(3, root.size());
        assertEquals(2, root.findInt("b").orElse(-1).intValue());
    }

    // Conversion ------------------------------------------------------------------

    @Test
    public void toMap_flatObject_returnsMap() {
        JSONObject root = JSONParser.parse("root", "{\"a\": 1, \"b\": \"two\"}");
        Map<String, Object> map = root.toMap();
        assertEquals(2, map.size());
        assertEquals(1, ((Number) map.get("a")).intValue());
        assertEquals("two", map.get("b"));
    }

    @Test
    public void toMap_nestedObject_returnsNestedMap() {
        JSONObject root = JSONParser.parse("root", "{\"outer\": {\"inner\": 42}}");
        Map<String, Object> map = root.toMap();
        assertTrue(map.get("outer") instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> inner = (Map<String, Object>) map.get("outer");
        assertEquals(42, ((Number) inner.get("inner")).intValue());
    }

    @Test(expected = IllegalStateException.class)
    public void toMap_scalarNode_throws() {
        new JSONObject("k", "v").toMap();
    }

    @Test
    public void toMap_nullNode_returnsEmptyMap() {
        assertTrue(new JSONObject("k").toMap().isEmpty());
    }

    @Test
    public void toList_primitiveArray_returnsList() {
        JSONObject node = new JSONObject("root", List.of(1, 2, 3));
        List<Object> list = node.toList();
        assertEquals(3, list.size());
        assertEquals(1, ((Number) list.get(0)).intValue());
    }

    @Test(expected = IllegalStateException.class)
    public void toList_scalarNode_throws() {
        new JSONObject("k", "v").toList();
    }

    @Test
    public void toObject_appliesFunction() {
        JSONObject root = JSONParser.parse("root", "{\"score\": 88}");
        Integer score = root.toObject(j -> j.findInt("score").orElse(0));
        assertEquals(88, score.intValue());
    }

    @Test
    public void toObject_methodReference() {
        JSONObject root = JSONParser.parse("root", "{\"name\": \"Alice\"}");
        String name = root.toObject(j -> j.requireString("name"));
        assertEquals("Alice", name);
    }

    // Iteration -------------------------------------------------------------------

    @Test
    public void iterator_scalarNode_yieldsValue() {
        Iterator<Object> it = new JSONObject("k", "hello").iterator();
        assertTrue(it.hasNext());
        assertEquals("hello", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void iterator_nullNode_yieldsNull() {
        Iterator<Object> it = new JSONObject("k").iterator();
        assertTrue(it.hasNext());
        assertNull(it.next());
    }

    @Test
    public void iterator_objectNode_yieldsLeafValues() {
        JSONObject root = JSONParser.parse("root", "{\"a\": 1, \"b\": 2}");
        List<Object> values = new ArrayList<>();
        root.iterator().forEachRemaining(values::add);
        assertEquals(2, values.size());
    }

    @Test
    public void childIterator_objectNode_yieldsDirectChildren() {
        JSONObject root = JSONParser.parse("root", "{\"a\": 1, \"b\": 2}");
        List<JSONObject> children = new ArrayList<>();
        root.childIterator().forEachRemaining(children::add);
        assertEquals(2, children.size());
        List<String> keys = children.stream().map(JSONObject::getKey).toList();
        assertTrue(keys.containsAll(List.of("a", "b")));
    }

    @Test
    public void childIterator_scalarNode_empty() {
        assertFalse(new JSONObject("k", "v").childIterator().hasNext());
    }

    @Test
    public void childIterator_doesNotDescendIntoGrandchildren() {
        JSONObject root = JSONParser.parse("root", "{\"a\": {\"b\": 1}}");
        List<JSONObject> children = new ArrayList<>();
        root.childIterator().forEachRemaining(children::add);
        // Only "a" is a direct child; "b" is a grandchild
        assertEquals(1, children.size());
        assertEquals("a", children.get(0).getKey());
    }

    @Test
    public void stream_objectNode_collectsLeafValues() {
        JSONObject root = JSONParser.parse("root", "{\"x\": 1, \"y\": 2, \"z\": 3}");
        long count = root.stream().count();
        assertEquals(3, count);
    }

    @Test
    public void childStream_objectNode_collectsChildren() {
        JSONObject root = JSONParser.parse("root", "{\"a\": 1, \"b\": 2}");
        List<String> keys = root.childStream()
                .map(JSONObject::getKey)
                .toList();
        assertEquals(2, keys.size());
        assertTrue(keys.containsAll(List.of("a", "b")));
    }

    @Test
    public void childStream_canFilter() {
        JSONObject root = JSONParser.parse("root", "{\"a\": 1, \"b\": 2, \"c\": 3}");
        long count = root.childStream()
                .filter(child -> child.getKey().equals("b"))
                .count();
        assertEquals(1, count);
    }

    // Clone -----------------------------------------------------------------------

    @Test
    public void clone_scalarNode_independentCopy() {
        JSONObject original = new JSONObject("k", "hello");
        JSONObject cloned = original.clone();
        assertEquals(original, cloned);
        cloned.setValue("world");
        assertNotEquals(original, cloned);
    }

    @Test
    public void clone_objectNode_deepCopiesChildren() {
        JSONObject original = JSONParser.parse("root", "{\"a\": 1, \"b\": 2}");
        JSONObject cloned = original.clone();
        assertEquals(original, cloned);
        cloned.put("c", 3);
        assertNotEquals(original, cloned);
    }

    @Test
    public void clone_nestedObject_mutatingCloneDoesNotAffectOriginal() {
        JSONObject original = JSONParser.parse("root", "{\"outer\": {\"inner\": 42}}");
        JSONObject cloned = original.clone();
        // Mutate the clone's child
        cloned.findNodeAt("outer", "inner").ifPresent(n -> n.setValue(99));
        // Original should be unchanged
        assertEquals(42, original.findInt("inner").orElse(-1).intValue());
    }

    @Test
    public void clone_nullNode_cloneIsAlsoNull() {
        JSONObject original = new JSONObject("k");
        JSONObject cloned = original.clone();
        assertTrue(cloned.isNull());
    }

    // equals and hashCode and toString --------------------------------------------

    @Test
    public void equals_sameStructure_true() {
        JSONObject a = JSONParser.parse("root", "{\"x\": 1}");
        JSONObject b = JSONParser.parse("root", "{\"x\": 1}");
        assertEquals(a, b);
    }

    @Test
    public void equals_differentStructure_false() {
        JSONObject a = JSONParser.parse("root", "{\"x\": 1}");
        JSONObject b = JSONParser.parse("root", "{\"x\": 2}");
        assertNotEquals(a, b);
    }

    @Test
    public void equals_sameInstance_true() {
        JSONObject a = JSONParser.parse("root", "{\"x\": 1}");
        assertEquals(a, a);
    }

    @Test
    public void equals_null_false() {
        assertNotEquals(null, JSONParser.parse("root", "{\"x\": 1}"));
    }

    @Test
    public void equals_differentClass_false() {
        assertNotEquals("not a json object", JSONParser.parse("root", "{\"x\": 1}"));
    }

    @Test
    public void hashCode_equalObjects_sameHash() {
        JSONObject a = JSONParser.parse("root", "{\"x\": 1}");
        JSONObject b = JSONParser.parse("root", "{\"x\": 1}");
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void toString_scalarNode_returnsValue() {
        assertEquals("\"hello\"", new JSONObject("k", "hello").toString());
    }

    @Test
    public void toString_objectNode_containsKeys() {
        JSONObject root = JSONParser.parse("root", "{\"name\": \"Alice\"}");
        String s = root.toString();
        assertTrue(s.contains("\"name\""));
        assertTrue(s.contains("\"Alice\""));
    }

    @Test
    public void toString_roundTrip_parsesBack() {
        JSONObject original = JSONParser.parse("root", "{\"a\": 1, \"b\": true, \"c\": \"hello\"}");
        JSONObject reparsed = JSONParser.parse("root", original.toString());
        assertEquals(original, reparsed);
    }
}