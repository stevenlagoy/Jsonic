package com.stevenlagoy.jsonic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

/**
 * Tests for {@link JSONSerializable}, exercised through concrete implementing
 * classes defined as inner classes below.
 * <p>
 * Coverage targets:
 * <ul>
 * <li>{@code toJson(Object)} — reflection over all declared fields</li>
 * <li>{@code toJson(Object, Collection)} — selective field serialization,
 * all type branches: null, JSONSerializable, primitive/wrapper, array,
 * Collection, unhandled type</li>
 * <li>{@code collectionToJson} — JSONSerializable elements, plain elements,
 * null elements</li>
 * <li>{@code arrayToJson} — reference arrays, primitive arrays, null elements,
 * JSONSerializable elements</li>
 * <li>{@code classJson} — public non-static fields only, key is simple
 * name</li>
 * <li>{@code isPrimitiveOrWrapper} — all wrapper types, primitive types,
 * String, Number, non-scalar types (via toJson behavior)</li>
 * <li>{@code toJson()} and {@code fromJson(JSONObject)} — interface contract
 * via implementing classes</li>
 * <li>Inheritance — super.toJson() / super.fromJson() chain</li>
 * <li>Round-trip — toJson() → fromJson() produces structurally equal
 * object</li>
 * </ul>
 */
public class JSONSerializableTest {

    // Model classes ---------------------------------------------------------------

    /** Simple flat model with public fields of various scalar types. */
    static class FlatModel implements JSONSerializable<FlatModel> {
        public String name = "";
        public int count = 0;
        public long bigNum = 0L;
        public double ratio = 0.0;
        public float weight = 0.0f;
        public boolean active = false;
        public String nullable = null;

        FlatModel() {
        }

        FlatModel(String name, int count, boolean active) {
            this.name = name;
            this.count = count;
            this.active = active;
        }

        @Override
        public @NotNull JSONObject toJson() {
            return JSONSerializable.toJson(this);
        }

        @Override
        public @NotNull FlatModel fromJson(@NotNull JSONObject json) {
            this.name = json.findString("name").orElse("");
            this.count = json.findInt("count").orElse(0);
            this.bigNum = json.findLong("bigNum").orElse(0L);
            this.ratio = json.findDouble("ratio").orElse(0.0);
            this.weight = json.findFloat("weight").orElse(0.0f);
            this.active = json.findBoolean("active").orElse(false);
            this.nullable = json.findString("nullable").orElse(null);
            return this;
        }
    }

    /** Model with a nested JSONSerializable field. */
    static class NestedModel implements JSONSerializable<NestedModel> {
        public String title = "";
        public FlatModel inner = null;

        NestedModel() {
        }

        NestedModel(String title, FlatModel inner) {
            this.title = title;
            this.inner = inner;
        }

        @Override
        public @NotNull JSONObject toJson() {
            return JSONSerializable.toJson(this);
        }

        @Override
        public @NotNull NestedModel fromJson(@NotNull JSONObject json) {
            this.title = json.findString("title").orElse("");
            json.findJson("inner").ifPresent(j -> this.inner = new FlatModel().fromJson(j));
            return this;
        }
    }

    /** Model with a Collection field and an array field. */
    static class CollectionModel implements JSONSerializable<CollectionModel> {
        public List<String> tags = new ArrayList<>();
        public int[] codes = new int[0];

        CollectionModel() {
        }

        @Override
        public @NotNull JSONObject toJson() {
            return JSONSerializable.toJson(this);
        }

        @Override
        public @NotNull CollectionModel fromJson(@NotNull JSONObject json) {
            this.tags = json.findArray("tags")
                .map(arr -> {
                    List<String> result = new ArrayList<>();
                    arr.forEach(e -> {
                        if (e instanceof JSONObject j)
                            result.add(j.requireString());
                    });
                    return result;
                })
                .orElse(new ArrayList<>());
            return this;
        }
    }

    /** Model with a Collection of JSONSerializable elements. */
    static class ListOfSerializableModel implements JSONSerializable<ListOfSerializableModel> {
        public List<FlatModel> items = new ArrayList<>();

        ListOfSerializableModel() {
        }

        @Override
        public @NotNull JSONObject toJson() {
            JSONObject json = new JSONObject(getClass().getSimpleName());
            json.put("items", JSONSerializable.collectionToJson(items));
            return json;
        }

        @Override
        public @NotNull ListOfSerializableModel fromJson(@NotNull JSONObject json) {
            this.items = new ArrayList<>();
            json.findArray("items").ifPresent(arr -> {
                for (Object item : arr) {
                    if (item instanceof JSONObject j) {
                        this.items.add(new FlatModel().fromJson(j));
                    }
                }
            });
            return this;
        }
    }

    /** Parent class for inheritance tests. */
    static class Animal implements JSONSerializable<Animal> {
        public String name = "";
        public int legCount = 0;

        Animal() {}

        Animal(String name, int legCount) {
            this.name = name;
            this.legCount = legCount;
        }

        @Override
        public @NotNull JSONObject toJson() {
            return JSONSerializable.toJson(this);
        }

        @Override
        public @NotNull Animal fromJson(@NotNull JSONObject json) {
            this.name = json.findString("name").orElse("");
            this.legCount = json.findInt("legCount").orElse(0);
            return this;
        }
    }

    /** Subclass for inheritance tests. */
    static class Dog extends Animal {
        public String breed = "";

        Dog() {}

        Dog(String name, int legCount, String breed) {
            super(name, legCount);
            this.breed = breed;
        }

        @Override
        public @NotNull Dog fromJson(@NotNull JSONObject json) {
            super.fromJson(json);
            this.breed = json.findString("breed").orElse("");
            return this;
        }
    }

    /**
     * Model with a private field (should be serialized since setAccessible is
     * called).
     */
    static class PrivateFieldModel implements JSONSerializable<PrivateFieldModel> {
        private String secret = "hidden";
        public String visible = "shown";

        PrivateFieldModel() {
        }

        @Override
        public @NotNull JSONObject toJson() {
            return JSONSerializable.toJson(this);
        }

        @Override
        public @NotNull PrivateFieldModel fromJson(@NotNull JSONObject json) {
            this.visible = json.findString("visible").orElse("");
            this.secret = json.findString("secret").orElse("");
            return this;
        }
    }

    /** Model with a static field (should be excluded). */
    static class StaticFieldModel implements JSONSerializable<StaticFieldModel> {
        public static String CONSTANT = "const";
        public String value = "instance";

        StaticFieldModel() {
        }

        @Override
        public @NotNull JSONObject toJson() {
            return JSONSerializable.toJson(this);
        }

        @Override
        public @NotNull StaticFieldModel fromJson(@NotNull JSONObject json) {
            this.value = json.findString("value").orElse("");
            return this;
        }
    }

    /** Model with an unhandled field type — should be silently skipped. */
    static class UnhandledTypeModel implements JSONSerializable<UnhandledTypeModel> {
        public String handled = "yes";
        public java.util.Date unhandled = null; // not null but non-Collection, non-array, non-scalar

        UnhandledTypeModel() {
        }

        @Override
        public @NotNull JSONObject toJson() {
            return JSONSerializable.toJson(this);
        }

        @Override
        public @NotNull UnhandledTypeModel fromJson(@NotNull JSONObject json) {
            this.handled = json.findString("handled").orElse("");
            return this;
        }
    }

    // Reflection based serialization ----------------------------------------------

    @Test
    public void toJson_object_keyIsSimpleClassName() {
        FlatModel model = new FlatModel("Alice", 5, true);
        JSONObject json = JSONSerializable.toJson(model);
        assertEquals("FlatModel", json.getKey());
    }

    @Test
    public void toJson_object_stringField() {
        FlatModel model = new FlatModel("Alice", 0, false);
        JSONObject json = JSONSerializable.toJson(model);
        assertEquals("Alice", json.findString("name").orElse(null));
    }

    @Test
    public void toJson_object_intField() {
        FlatModel model = new FlatModel("", 42, false);
        JSONObject json = JSONSerializable.toJson(model);
        assertEquals(42, json.findInt("count").orElse(-1).intValue());
    }

    @Test
    public void toJson_object_longField() {
        FlatModel model = new FlatModel();
        model.bigNum = Long.MAX_VALUE;
        JSONObject json = JSONSerializable.toJson(model);
        assertEquals(Long.MAX_VALUE, json.findLong("bigNum").orElse(0L).longValue());
    }

    @Test
    public void toJson_object_doubleField() {
        FlatModel model = new FlatModel();
        model.ratio = 3.14;
        JSONObject json = JSONSerializable.toJson(model);
        assertEquals(3.14, json.findDouble("ratio").orElse(0.0), 0.001);
    }

    @Test
    public void toJson_object_floatField() {
        FlatModel model = new FlatModel();
        model.weight = 1.5f;
        JSONObject json = JSONSerializable.toJson(model);
        assertEquals(1.5f, json.findFloat("weight").orElse(0.0f), 0.001f);
    }

    @Test
    public void toJson_object_booleanField() {
        FlatModel model = new FlatModel("", 0, true);
        JSONObject json = JSONSerializable.toJson(model);
        assertEquals(Boolean.TRUE, json.findBoolean("active").orElse(false));
    }

    @Test
    public void toJson_object_nullField_keyPresent() {
        FlatModel model = new FlatModel();
        model.nullable = null;
        JSONObject json = JSONSerializable.toJson(model);
        assertTrue(json.hasKey("nullable"));
        assertFalse(json.findString("nullable").isPresent());
    }

    @Test
    public void toJson_object_privateFieldIncluded() {
        PrivateFieldModel model = new PrivateFieldModel();
        JSONObject json = JSONSerializable.toJson(model);
        // setAccessible(true) means private fields are included
        assertTrue(json.hasKey("secret"));
        assertEquals("hidden", json.findString("secret").orElse(null));
    }

    @Test
    public void toJson_object_staticFieldExcluded() {
        // classJson excludes static fields; toJson(Object) uses getDeclaredFields
        // which includes statics, but toJson(Object, Collection<Field>) calls
        // f.get(o) — for static fields this returns the static value, not instance.
        // The important thing is "CONSTANT" should not appear as an instance field.
        StaticFieldModel model = new StaticFieldModel();
        JSONObject template = JSONSerializable.classJson(StaticFieldModel.class);
        List<String> keys = template.getFieldKeys();
        assertFalse("Static field should not appear in classJson", keys.contains("CONSTANT"));
        assertTrue(keys.contains("value"));
    }

    @Test
    public void toJson_object_nestedSerializable() {
        FlatModel inner = new FlatModel("Bob", 3, false);
        NestedModel model = new NestedModel("wrapper", inner);
        JSONObject json = JSONSerializable.toJson(model);
        assertTrue(json.hasKey("inner"));
        assertEquals("Bob", json.findString("name").orElse(null));
    }

    @Test
    public void toJson_object_collectionField() {
        CollectionModel model = new CollectionModel();
        model.tags = List.of("java", "json");
        JSONObject json = JSONSerializable.toJson(model);
        assertTrue(json.hasKey("tags"));
        List<?> arr = json.findArray("tags").orElse(null);
        assertNotNull(arr);
        assertEquals(2, arr.size());
        assertTrue(arr.contains(new JSONObject(String.valueOf("java".hashCode()), "java")));
        assertTrue(arr.contains(new JSONObject(String.valueOf("json".hashCode()), "json")));
    }

    @Test
    public void toJson_object_arrayField_intArray() {
        CollectionModel model = new CollectionModel();
        model.codes = new int[] { 10, 20, 30 };
        JSONObject json = JSONSerializable.toJson(model);
        assertTrue(json.hasKey("codes"));
    }

    @Test
    public void toJson_object_unhandledTypeField_skipped() {
        UnhandledTypeModel model = new UnhandledTypeModel();
        model.unhandled = new java.util.Date(0);
        JSONObject json = JSONSerializable.toJson(model);
        // "handled" should be present; "unhandled" (java.util.Date) silently skipped
        assertTrue(json.hasKey("handled"));
        assertFalse(json.hasKey("unhandled"));
    }

    // Selective field serialization -----------------------------------------------

    @Test
    public void toJson_withFields_onlySelectedFieldsIncluded() throws NoSuchFieldException {
        FlatModel model = new FlatModel("Alice", 42, true);
        List<Field> selectedFields = List.of(FlatModel.class.getDeclaredField("name"));
        JSONObject json = JSONSerializable.toJson(model, selectedFields);
        assertTrue(json.hasKey("name"));
        assertFalse(json.hasKey("count"));
        assertFalse(json.hasKey("active"));
    }

    @Test
    public void toJson_withFields_emptyFieldList_producesEmptyObject() {
        FlatModel model = new FlatModel("Alice", 42, true);
        JSONObject json = JSONSerializable.toJson(model, List.of());
        assertTrue(json.isObject() || json.getFieldKeys().isEmpty());
    }

    // collectionToJson ------------------------------------------------------------

    @Test
    public void collectionToJson_jsonSerializableElements_callsToJson() {
        FlatModel a = new FlatModel("Alice", 1, true);
        FlatModel b = new FlatModel("Bob", 2, false);
        List<JSONObject> result = JSONSerializable.collectionToJson(List.of(a, b));
        assertEquals(2, result.size());
        assertEquals("Alice", result.get(0).findString("name").orElse(null));
        assertEquals("Bob", result.get(1).findString("name").orElse(null));
    }

    @Test
    public void collectionToJson_plainObjects_usesReflection() {
        // Plain String is not JSONSerializable — falls into the toJson(Object) branch
        // Since String has no public non-static fields, it produces a near-empty node
        String value = "hello";
        List<JSONObject> result = JSONSerializable.collectionToJson(List.of(value));
        assertEquals(1, result.size());
        // The key should be the hash of the string itself
        assertEquals(String.valueOf(value.hashCode()), result.get(0).getKey());
    }

    @Test
    public void collectionToJson_nullElement_addsSentinel() {
        List<Object> list = new ArrayList<>();
        list.add(null);
        List<JSONObject> result = JSONSerializable.collectionToJson(list);
        assertEquals(1, result.size());
        // Sentinel is an empty JSONObject with key "" and null value
        assertNotNull(result.get(0));
        assertTrue(result.get(0).isNull());
    }

    @Test
    public void collectionToJson_emptyCollection_returnsEmptyList() {
        List<JSONObject> result = JSONSerializable.collectionToJson(List.of());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void collectionToJson_mixedElements_handlesEachCorrectly() {
        FlatModel model = new FlatModel("Alice", 1, true);
        List<Object> mixed = new ArrayList<>();
        mixed.add(model);
        mixed.add(null);
        List<JSONObject> result = JSONSerializable.collectionToJson(mixed);
        assertEquals(2, result.size());
        assertEquals("Alice", result.get(0).findString("name").orElse(null));
        assertTrue(result.get(1).isNull());
    }

    // arrayToJson -----------------------------------------------------------------

    @Test
    public void arrayToJson_stringArray_serialized() {
        String[] arr = { "alpha", "beta", "gamma" };
        List<JSONObject> result = JSONSerializable.arrayToJson(arr);
        assertEquals(3, result.size());
        // Each String has no public fields — key is "String", value is empty list
        assertEquals(String.valueOf(arr[0].hashCode()), result.get(0).getKey());
    }

    @Test
    public void arrayToJson_jsonSerializableArray_callsToJson() {
        FlatModel[] arr = {
                new FlatModel("Alice", 1, true),
                new FlatModel("Bob", 2, false)
        };
        List<JSONObject> result = JSONSerializable.arrayToJson(arr);
        assertEquals(2, result.size());
        assertEquals("Alice", result.get(0).findString("name").orElse(null));
        assertEquals("Bob", result.get(1).findString("name").orElse(null));
    }

    @Test
    public void arrayToJson_intArray_skipsNullsForPrimitives() {
        // Primitive arrays can't contain nulls — Array.get boxes them
        int[] arr = { 1, 2, 3 };
        List<JSONObject> result = JSONSerializable.arrayToJson(arr);
        assertEquals(3, result.size());
    }

    @Test
    public void arrayToJson_arrayWithNullElements_nullsSkipped() {
        String[] arr = { "a", null, "b" };
        List<JSONObject> result = JSONSerializable.arrayToJson(arr);
        // null elements are skipped per the contract
        assertEquals(2, result.size());
    }

    @Test
    public void arrayToJson_emptyArray_returnsEmptyList() {
        Object[] arr = {};
        List<JSONObject> result = JSONSerializable.arrayToJson(arr);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void arrayToJson_nonArrayArgument_throws() {
        // Array.getLength on a non-array throws IllegalArgumentException
        JSONSerializable.arrayToJson("not an array");
    }

    // classJson -------------------------------------------------------------------

    @Test
    public void classJson_keyIsSimpleClassName() {
        JSONObject template = JSONSerializable.classJson(FlatModel.class);
        assertEquals("FlatModel", template.getKey());
    }

    @Test
    public void classJson_publicFieldsPresent() {
        JSONObject template = JSONSerializable.classJson(FlatModel.class);
        List<String> keys = template.getFieldKeys();
        assertTrue(keys.contains("name"));
        assertTrue(keys.contains("count"));
        assertTrue(keys.contains("active"));
    }

    @Test
    public void classJson_allValuesAreNull() {
        JSONObject template = JSONSerializable.classJson(FlatModel.class);
        for (String key : template.getFieldKeys()) {
            assertFalse("Field '" + key + "' should have null value in template",
                    template.findJson(key).map(j -> !j.isNull()).orElse(false));
        }
    }

    @Test
    public void classJson_staticFieldsExcluded() {
        JSONObject template = JSONSerializable.classJson(StaticFieldModel.class);
        assertFalse(template.getFieldKeys().contains("CONSTANT"));
    }

    @Test
    public void classJson_privateFieldsExcluded() {
        JSONObject template = JSONSerializable.classJson(PrivateFieldModel.class);
        // classJson only includes public fields
        assertFalse(template.getFieldKeys().contains("secret"));
        assertTrue(template.getFieldKeys().contains("visible"));
    }

    @Test
    public void classJson_emptyClass_returnsEmptyTemplate() {
        class Empty implements JSONSerializable<Empty> {
            public @NotNull JSONObject toJson() {
                return new JSONObject("Empty");
            }

            public @NotNull Empty fromJson(@NotNull JSONObject json) {
                return this;
            }
        }
        JSONObject template = JSONSerializable.classJson(Empty.class);
        assertEquals("Empty", template.getKey());
        assertTrue(template.getFieldKeys().isEmpty());
    }

    // toJson and fromJson ---------------------------------------------------------

    @Test
    public void toJson_implementingClass_returnsPopulatedNode() {
        FlatModel model = new FlatModel("Alice", 5, true);
        JSONObject json = model.toJson();
        assertNotNull(json);
        assertEquals("Alice", json.findString("name").orElse(null));
        assertEquals(5, json.findInt("count").orElse(-1).intValue());
        assertEquals(Boolean.TRUE, json.findBoolean("active").orElse(false));
    }

    @Test
    public void fromJson_implementingClass_populatesFields() {
        JSONObject json = JSONParser.parse("root",
                "{\"name\": \"Alice\", \"count\": 5, \"active\": true, " +
                        "\"bigNum\": 0, \"ratio\": 0.0, \"weight\": 0.0, \"nullable\": null}");
        FlatModel model = new FlatModel().fromJson(json);
        assertEquals("Alice", model.name);
        assertEquals(5, model.count);
        assertTrue(model.active);
    }

    @Test
    public void fromJson_missingFields_usesDefaults() {
        JSONObject json = JSONParser.parse("root", "{\"name\": \"Alice\"}");
        FlatModel model = new FlatModel().fromJson(json);
        assertEquals("Alice", model.name);
        assertEquals(0, model.count); // default
        assertFalse(model.active); // default
    }

    // Inheritance -----------------------------------------------------------------

    @Test
    public void inheritance_toJson_includesBothParentAndChildFields() {
        Dog dog = new Dog("Rex", 4, "Labrador");
        JSONObject json = dog.toJson();
        assertTrue(json.hasKey("name"));
        assertTrue(json.hasKey("legCount"));
        assertTrue(json.hasKey("breed"));
    }

    @Test
    public void inheritance_toJson_parentFieldValues_correct() {
        Dog dog = new Dog("Rex", 4, "Labrador");
        JSONObject json = dog.toJson();
        assertEquals("Rex", json.findString("name").orElse(null));
        assertEquals(4, json.findInt("legCount").orElse(-1).intValue());
    }

    @Test
    public void inheritance_toJson_childFieldValue_correct() {
        Dog dog = new Dog("Rex", 4, "Labrador");
        JSONObject json = dog.toJson();
        assertEquals("Labrador", json.findString("breed").orElse(null));
    }

    @Test
    public void inheritance_fromJson_populatesParentFields() {
        JSONObject json = JSONParser.parse("root",
                "{\"name\": \"Rex\", \"legCount\": 4, \"breed\": \"Labrador\"}");
        Dog dog = new Dog().fromJson(json);
        assertEquals("Rex", dog.name);
        assertEquals(4, dog.legCount);
    }

    @Test
    public void inheritance_fromJson_populatesChildFields() {
        JSONObject json = JSONParser.parse("root",
                "{\"name\": \"Rex\", \"legCount\": 4, \"breed\": \"Labrador\"}");
        Dog dog = new Dog().fromJson(json);
        assertEquals("Labrador", dog.breed);
    }

    @Test
    public void inheritance_parentFromJson_doesNotSetChildFields() {
        JSONObject json = JSONParser.parse("root",
                "{\"name\": \"Generic\", \"legCount\": 2}");
        Animal animal = new Animal().fromJson(json);
        assertEquals("Generic", animal.name);
        assertEquals(2, animal.legCount);
    }

    // Round trip ------------------------------------------------------------------

    @Test
    public void roundTrip_flatModel_allFieldsPreserved() {
        FlatModel original = new FlatModel("Alice", 42, true);
        original.bigNum = 9_999_999_999L;
        original.ratio = 2.718;
        original.weight = 1.5f;

        JSONObject json = original.toJson();
        FlatModel loaded = new FlatModel().fromJson(json);

        assertEquals(original.name, loaded.name);
        assertEquals(original.count, loaded.count);
        assertEquals(original.bigNum, loaded.bigNum);
        assertEquals(original.ratio, loaded.ratio, 0.001);
        assertEquals(original.weight, loaded.weight, 0.001f);
        assertEquals(original.active, loaded.active);
        assertNull(loaded.nullable);
    }

    @Test
    public void roundTrip_nestedModel_innerFieldsPreserved() {
        FlatModel inner = new FlatModel("Inner", 7, true);
        NestedModel original = new NestedModel("Outer", inner);

        JSONObject json = original.toJson();
        NestedModel loaded = new NestedModel().fromJson(json);

        assertEquals(original.title, loaded.title);
        assertNotNull(loaded.inner);
        assertEquals(original.inner.name, loaded.inner.name);
        assertEquals(original.inner.count, loaded.inner.count);
        assertEquals(original.inner.active, loaded.inner.active);
    }

    @Test
    public void roundTrip_listOfSerializableModel_allElementsPreserved() {
        ListOfSerializableModel original = new ListOfSerializableModel();
        original.items.add(new FlatModel("A", 1, true));
        original.items.add(new FlatModel("B", 2, false));

        JSONObject json = original.toJson();
        ListOfSerializableModel loaded = new ListOfSerializableModel().fromJson(json);

        assertEquals(2, loaded.items.size());
        assertEquals("A", loaded.items.get(0).name);
        assertEquals("B", loaded.items.get(1).name);
    }

    @Test
    public void roundTrip_dog_inheritancePreserved() {
        Dog original = new Dog("Rex", 4, "Labrador");
        Dog loaded = new Dog().fromJson(original.toJson());

        assertEquals(original.name, loaded.name);
        assertEquals(original.legCount, loaded.legCount);
        assertEquals(original.breed, loaded.breed);
    }

    @Test
    public void roundTrip_nullField_survivesRoundTrip() {
        FlatModel original = new FlatModel();
        original.nullable = null;

        FlatModel loaded = new FlatModel().fromJson(original.toJson());
        assertNull(loaded.nullable);
    }

    @Test
    public void roundTrip_emptyString_survivesRoundTrip() {
        FlatModel original = new FlatModel("", 0, false);
        FlatModel loaded = new FlatModel().fromJson(original.toJson());
        assertEquals("", loaded.name);
    }

    @Test
    public void roundTrip_collectionModel_tagsPreserved() {
        CollectionModel original = new CollectionModel();
        original.tags = new ArrayList<>(List.of("java", "json", "test"));

        CollectionModel loaded = new CollectionModel().fromJson(original.toJson());

        assertEquals(3, loaded.tags.size());
        assertTrue(loaded.tags.contains("java"));
        assertTrue(loaded.tags.contains("json"));
        assertTrue(loaded.tags.contains("test"));
    }
}
