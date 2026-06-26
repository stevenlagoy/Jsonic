package com.stevenlagoy.jsonic;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Marker interface that enables JSON serialization and deserialization for
 * implementing classes.
 * <p>
 * Classes implementing {@code JSONSerializable} commit to two contracts:
 * converting
 * themselves to a {@link JSONObject} via {@link #toJson()}, and reconstructing
 * themselves from a {@link JSONObject} via {@link #fromJson(JSONObject)}.
 * <p>
 * Static utility methods are provided for serializing arbitrary objects,
 * collections, and arrays via reflection, as well as for generating structural
 * templates from class definitions.
 * <p>
 * <b>Example usage:</b>
 * 
 * <pre>{@code
 * class MyClass implements JSONSerializable<MyClass> {
 *
 *     public String name;
 *     public int age;
 *
 *     &#64;Override
 *     public JSONObject toJson() {
 *         return JSONSerializable.toJson(this);
 *     }
 *
 *     &#64;Override
 *     public MyClass fromJson(JSONObject json) {
 *         this.name = json.findString("name").orElse(null);
 *         this.age = json.findInt("age").orElse(0);
 *         return this;
 *     }
 * }
 * }</pre>
 *
 * @param <T> the implementing class itself; used to type the return value of
 *            {@link #fromJson(JSONObject)}
 */
public interface JSONSerializable<T extends JSONSerializable<T>> {

    /**
     * Converts this object into a {@link JSONObject} representation. Implementing
     * classes should include all fields that are meaningful for serialization.
     * <p>
     * The simplest correct implementation delegates to
     * {@link #toJson(Object)}:
     * 
     * <pre>{@code
     * public JSONObject toJson() {
     *     return JSONSerializable.toJson(this);
     * }
     * }</pre>
     *
     * @return a {@code JSONObject} representing this object's state
     */
    @NotNull JSONObject toJson();

    /**
     * Reconstructs this object's state from the given {@link JSONObject} and
     * returns {@code this}. Fields present in {@code json} should be applied to
     * the object; fields absent in {@code json} may be left at their defaults.
     * <p>
     * The method both mutates {@code this} and returns it, enabling use as a
     * factory-style call on a fresh instance:
     * 
     * <pre>{@code
     * MyClass obj = new MyClass().fromJson(json);
     * }</pre>
     *
     * @param json a {@code JSONObject} containing key-value pairs corresponding
     *             to fields of this object; may be partial
     * @return this object, after applying fields from {@code json}
     */
    @NotNull T fromJson(@NotNull JSONObject json);

    /**
     * Converts a {@link Collection} to a {@link List} of {@link JSONObject}s.
     * <p>
     * Each element is serialized as follows:
     * <ul>
     * <li>If the element implements {@link JSONSerializable}, its {@link #toJson()}
     * method
     * is used.</li>
     * <li>If the element is non-null, {@link #toJson(Object)} is used.</li>
     * <li>If the element is {@code null}, a {@code JSONObject} with key
     * {@code ""} and a {@code null} value is added as a sentinel.</li>
     * </ul>
     *
     * @param collection the collection to serialize; must not be {@code null}
     * @return a list of {@code JSONObject}s representing the collection's elements;
     *         never {@code null}
     * @see #toJson(Object)
     */
    static @NotNull List<JSONObject> collectionToJson(@NotNull Collection<?> collection) {
        List<JSONObject> json = new ArrayList<>();
        for (Object item : collection) {
            if (item instanceof JSONSerializable<?> JSONSerializable) {
                json.add(JSONSerializable.toJson());
            } else if (item != null) {
                json.add(toJson(item));
            } else {
                json.add(new JSONObject());
            }
        }
        return json;
    }

    /**
     * Converts an array to a {@link List} of {@link JSONObject}s. Accepts any
     * array type (primitive or reference) via {@link java.lang.reflect.Array}.
     * {@code null} elements are skipped.
     * <p>
     * Each non-null element is serialized as follows:
     * <ul>
     * <li>If the element implements {@link JSONSerializable}, its {@link #toJson()}
     * method
     * is used.</li>
     * <li>Otherwise, {@link #toJson(Object)} is used.</li>
     * </ul>
     *
     * @param array an array of any type; must not be {@code null}
     * @return a list of {@code JSONObject}s representing the array's non-null
     *         elements; never {@code null}
     * @throws IllegalArgumentException if {@code array} is not an array type
     * @see #toJson(Object)
     */
    static @NotNull List<JSONObject> arrayToJson(@NotNull Object array) {
        int length = Array.getLength(array);
        List<JSONObject> json = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            Object element = Array.get(array, i);
            if (element instanceof JSONSerializable<?> JSONSerializable) {
                json.add(JSONSerializable.toJson());
            } else if (element != null) {
                json.add(toJson(element));
            }
        }
        return json;
    }

    /**
     * Returns {@code true} if the given type is a Java primitive, a primitive
     * wrapper, a {@link String}, or a {@link Number}.
     * <p>
     * These types are treated as JSON leaf values and are stored directly as a
     * {@link JSONObject}'s value rather than being reflected further.
     *
     * @param type the class to test; must not be {@code null}
     * @return {@code true} if {@code type} is a scalar JSON-compatible type
     */
    private static boolean isPrimitiveOrWrapper(@NotNull Class<?> type) {
        if (type.isPrimitive()) {
            return true;
        }
        for (Class<?> t : new Class<?>[] {
                Boolean.class, Byte.class, Character.class, Short.class,
                Integer.class, Long.class, Float.class, Double.class,
                Number.class, String.class }) {
            if (type == t) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reflects the public non-static fields of the given class into a structural
     * "template" {@link JSONObject} with no values set. Useful for inspecting the
     * expected shape of a class's JSON representation without needing an instance.
     * <p>
     * Only fields that are both {@code public} and non-{@code static} are included.
     * The returned node's key is the simple class name; its value is a list of
     * key-only {@code JSONObject}s, one per included field.
     *
     * @param clazz the class to reflect; must not be {@code null}
     * @return a template {@code JSONObject} whose children are the public
     *         non-static field names of {@code clazz}, each with a {@code null}
     *         value
     * @see #toJson(Object)
     */
    static @NotNull JSONObject classJson(@NotNull Class<?> clazz) {
        List<JSONObject> fields = new ArrayList<>();
        for (Field f : clazz.getDeclaredFields()) {
            int modifiers = f.getModifiers();
            if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)) {
                fields.add(new JSONObject(f.getName()));
            }
        }
        return new JSONObject(clazz.getSimpleName(), fields);
    }

    /**
     * Reflects all declared fields of the given object's class into a
     * {@link JSONObject}. Equivalent to calling
     * {@link #toJson(Object, Collection)} with all declared fields.
     * <p>
     * Fields that are inaccessible (e.g. private fields in a security-restricted
     * context) will be skipped silently.
     *
     * @param o the object to serialize; must not be {@code null}
     * @return a {@code JSONObject} whose key is the simple class name and whose
     *         value is a list of field name-value pairs
     * @see #toJson(Object, Collection)
     */
    static @NotNull JSONObject toJson(@NotNull Object o) {
        return toJson(o, getAllFields(o.getClass()));
    }

    private static @NotNull List<Field> getAllFields(@NotNull Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }

    /**
     * Reflects the given fields of the given object into a {@link JSONObject}.
     * The returned node's key is the simple class name; its value is a list of
     * {@code JSONObject}s, one per successfully read field.
     * <p>
     * Fields are serialized as follows:
     * <ul>
     * <li>{@code null} values produce a {@code JSONObject} with a {@code null}
     * value.</li>
     * <li>Values implementing {@link JSONSerializable} are serialized via their
     * {@link #toJson()} method and nested as the field's value.</li>
     * <li>Primitive, wrapper, {@link String}, and {@link Number} values are stored
     * directly.</li>
     * <li>Array values are serialized via {@link #arrayToJson(Object)}.</li>
     * <li>{@link Collection} values are serialized via
     * {@link #collectionToJson(Collection)}.</li>
     * <li>Fields that cannot be accessed or whose type is not handled are skipped
     * silently.</li>
     * </ul>
     *
     * @param o      the object whose fields are to be serialized; must not be
     *               {@code null}
     * @param fields the fields to include in the output; must not be {@code null}
     * @return a {@code JSONObject} representing the specified fields and their
     *         current values
     */
    static @NotNull JSONObject toJson(@NotNull Object o, @NotNull Collection<Field> fields) {
        List<JSONObject> jsonFields = new ArrayList<>();
        for (Field f : fields) {
            Object value;
            try {
                f.setAccessible(true);
                value = f.get(o);
            } catch (IllegalAccessException e) {
                continue;
            } catch (InaccessibleObjectException e) {
                return new JSONObject(String.valueOf(o.hashCode()), o);
            }
            if (value == null) {
                jsonFields.add(new JSONObject(f.getName(), null));
            } else if (value instanceof JSONSerializable<?> JSONSerializable) {
                jsonFields.add(new JSONObject(f.getName(), JSONSerializable.toJson()));
            } else if (isPrimitiveOrWrapper(value.getClass())) {
                jsonFields.add(new JSONObject(f.getName(), value));
            } else if (value.getClass().isArray()) {
                jsonFields.add(new JSONObject(f.getName(), arrayToJson(value)));
            } else if (value instanceof Collection<?> collection) {
                jsonFields.add(new JSONObject(f.getName(), collectionToJson(collection)));
            }
        }
        return new JSONObject(o.getClass().getSimpleName(), jsonFields);
    }
}
