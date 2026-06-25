package com.stevenlagoy.jsonic;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a single node in a JSON structure, storing a key-value pair where
 * the value may be any valid JSON type: {@link String}, {@link Number},
 * {@link Boolean}, {@code null}, a nested {@link JSONObject}, or a {@link List}
 * of either {@link JSONObject}s (representing a JSON object array) or
 * primitives (representing a JSON primitive array).
 * <p>
 * A {@link JSONObject} is a recursive tree structure. A node whose value is a
 * {@code List<JSONObject>} acts as a parent containing child nodes, each of
 * which may themselves contain further nested structures. Primitive values
 * ({@link String}, {@link Number}, {@link Boolean}, {@code null}) are leaf
 * nodes.
 * <p>
 * The class provides three categories of value access:
 * <ul>
 * <li><b>Direct access</b> — {@code getValue()}, {@code getString()},
 * {@code getNumber()}, etc.
 * Read the value of this node directly without searching the tree.</li>
 * <li><b>Tree search</b> — {@code find(...)} and {@code findAll(...)} search
 * the subtree rooted at this node for a key by name, returning the first match
 * or all matches respectively. Typed variants such as {@code findString},
 * {@code findInt}, etc. additionally filter results by type.</li>
 * <li><b>Structural queries</b> — {@code hasKey()}, {@code isNull()},
 * {@code isArray()}, {@code isObject()}, {@code isScalar()}, and {@code size()}
 * inspect the structure of this node without returning its value.</li>
 * </ul>
 * <p>
 * <b>Null values:</b> A node may have a key but a {@code null} value,
 * representing a JSON {@code null}. {@code hasKey} correctly detects such
 * nodes, but {@code find} will return {@code Optional.empty()} for them since
 * {@code Optional} cannot hold {@code null}. For this library's intended use
 * cases,JSON {@code null} values and absent keys are treated equivalently by
 * the search API.
 * <p>
 * This class implements {@link Iterable}, iterating over the values of the tree
 * in inorder traversal. It implements {@link Cloneable}, producing a deep copy
 * of the node and all descendant {@link JSONObject}s (primitive values within
 * lists are shared, as they are immutable).
 */
public class JSONObject implements Iterable<Object>, Cloneable {

    /**
     * The key of this JSON node. For the root node of a parsed JSON file, this may
     * be the name of the parsed file. Keys are never {@code null}.
     */
    private @NotNull String key;

    /**
     * The value of this JSON node. May be any of the following:
     * <ul>
     * <li>{@code null} — represents a JSON {@code null}</li>
     * <li>{@link String} — represents a JSON string</li>
     * <li>{@link Number} — represents a JSON number (typically {@link Integer},
     * {@link Long}, {@link Double}, or {@link Float})</li>
     * <li>{@link Boolean} — represents a JSON boolean</li>
     * <li>{@link JSONObject} — represents a single nested JSON object</li>
     * <li>{@link List}{@code <JSONObject>} — represents an array of JSON
     * objects</li>
     * <li>{@link List}{@code <?>} — represents a JSON array of primitive
     * values</li>
     * </ul>
     */
    private @Nullable Object value;

    // CONSTRUCTORS ----------------------------------------------------------------

    /**
     * Creates an empty {@link JSONObject} with an empty string key and a
     * {@code null} value. Equivalent to {@code new JSONObject("")}.
     */
    public JSONObject() {
        this("");
    }

    /**
     * Creates a {@link JSONObject} by parsing the JSON file at the given path. The
     * key of this node is set from the file name of the given path, and value of
     * this node is set from the value of the root node of the parsed structure.
     * <p>
     * If the given path is not a valid JSON file, the value of this node will be
     * {@code null}.
     *
     * @param path path to a valid JSON file
     * @throws RuntimeException if the file cannot be read or parsed
     */
    public JSONObject(@NotNull Path path) {
        List<String> contents = FileOperations.readFile(path);
        Path fileName = path.getFileName();
        String key = fileName.toString();
        JSONObject json = JSONParser.parse(key, contents);
        this.key = json.getKey();
        this.value = json.getValue();
    }

    /**
     * Creates a {@link JSONObject} from the String representation of a JSON
     * structure (like a JSON file). The key of this node is set from the passed
     * key, and the value of this node are set from the root node of the parsed
     * structure.
     * <p>
     * If the String representation is invalid, the value of this node will be
     * {@code null}.
     * 
     * @param key   key for this {@link JSONObject}
     * @param lines String representation of a JSON structure (like a JSON file)
     */
    public JSONObject(@NotNull String key, @Nullable Iterable<String> lines) {
        JSONObject json = JSONParser.parse(key, lines);
        this.key = json.getKey();
        this.value = json.getValue();
    }

    /**
     * Creates a {@link JSONObject} with the given key and a {@code null} value.
     *
     * @param key the key for this node; must not be {@code null}
     */
    public JSONObject(@NotNull String key) {
        this(key, (Object) null);
    }

    /**
     * Creates a {@link JSONObject} with the given key and value.
     *
     * @param key   the key for this node; must not be {@code null}
     * @param value the value for this node; may be {@code null} or any valid JSON
     *              type: {@link String}, {@link Number}, {@link Boolean},
     *              {@link JSONObject}, or {@link List}
     */
    public JSONObject(@NotNull String key, @Nullable Object value) {
        this.key = key;
        this.value = value;
    }

    // KEY -------------------------------------------------------------------------

    /**
     * Returns the key of this node.
     *
     * @return the key; never {@code null}
     */
    public @NotNull String getKey() {
        return key;
    }

    /**
     * Sets the key of this node.
     *
     * @param key the new key; must not be {@code null}
     */
    public void setKey(@NotNull String key) {
        this.key = key;
    }

    // STRUCTURAL QUERIES ----------------------------------------------------------

    /**
     * Returns {@code true} if this node's value is {@code null}.
     *
     * @return {@code true} if the value is {@code null}; {@code false} otherwise
     */
    public boolean isNull() {
        return value == null;
    }

    /**
     * Returns {@code true} if this node's value is a {@link List} of primitive
     * values (i.e., a JSON primitive array). Returns {@code false} if the list
     * contains {@link JSONObject}s (use {@link #isObject()} for that case) or if
     * the value is not a list.
     *
     * @return {@code true} if this node holds a primitive array; {@code false}
     *         otherwise
     */
    public boolean isArray() {
        return value instanceof List<?> && !isObject();
    }

    /**
     * Returns {@code true} if this node's value is a non-empty {@link List} of
     * {@link JSONObject}s (i.e., a JSON object array acting as a parent node).
     * Returns {@code false} for empty lists, primitive lists, or non-list values.
     * 
     * @return {@code true} if this node holds a non-empty list of
     *         {@link JSONObject}s; {@code false} otherwise
     */
    public boolean isObject() {
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            return false;
        }
        return list.get(0) instanceof JSONObject; // List<?>.getFirst() is a Java 21 feature
    }

    /**
     * Returns {@code true} if this node holds a single scalar value: a
     * {@link String}, {@link Number}, or {@link Boolean}. Returns {@code false} for
     * {@code null} values, object arrays, and primitive arrays.
     * 
     * @return {@code true} if this node's value is a non-null, non-list scalar,
     *         {@code false} otherwise
     */
    public boolean isScalar() {
        return !isNull() && !isObject() && !isArray();
    }

    /**
     * Returns the number of elements in this node's value.
     * <ul>
     * <li>If the value is a {@link List}, returns the list's size.</li>
     * <li>If the value is {@code null}, returns {@code 0}.</li>
     * <li>Otherwise (scalar value), returns {@code 1}.</li>
     * </ul>
     *
     * @return the number of elements in this node's value
     */
    public int size() {
        if (value instanceof List<?> valueList) {
            return valueList.size();
        } else if (isNull()) {
            return 0;
        } else {
            return 1;
        }
    }

    /**
     * Returns {@code true} if any node in the subtree rooted at this node has the
     * given key. Unlike {@link #find(String)}, this method correctly returns
     * {@code true} for keys whose value is {@code null}.
     *
     * @param key the key to search for
     * @return {@code true} if a node with the given key exists in this subtree;
     *         {@code false} otherwise
     */
    public boolean hasKey(@NotNull String key) {
        if (this.key.equals(key)) {
            return true;
        } else if (value instanceof JSONObject valueJson) {
            return valueJson.hasKey(key);
        } else if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof JSONObject itemJson && itemJson.hasKey(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Requires that the subtree rooted at this node contains the given key, and
     * throws an {@link IllegalArgumentException} if the key cannot be found. If the
     * key is present but is {@code null}, no exception will be thrown.
     * 
     * @param key the key to search for
     * @throws IllegalArgumentException if no node with the given key exists in this
     *                                  subtree
     */
    public void requireKey(@NotNull String key) throws IllegalArgumentException {
        if (!hasKey(key))
            throw new IllegalArgumentException(
                    "Missing required JSON property key: '" + key + "' for object '" + this.key + "'");
    }

    /**
     * Requires that the subtree rooted at this node contains the given key, and
     * throws an {@link IllegalArgumentException} from the given supplier if the key
     * cannot be found. If the key is present but is {@code null}, no exception will
     * be thrown.
     * 
     * @param key               the key to search for
     * @param exceptionSupplier supplier of the exception to be thrown if the given
     *                          key cannot be found
     * @throws IllegalArgumentException if no node with the given key exists in this
     *                                  subtree, gotten from the given supplier
     */
    public void requireKey(@NotNull String key, Supplier<IllegalArgumentException> exceptionSupplier)
            throws IllegalArgumentException {
        if (!hasKey(key)) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * Returns {@code true} if any of the given keys are present anywhere in the
     * subtree rooted at this node. Equivalent to calling {@link #hasKey(String)}
     * for each key and returning {@code true} on the first match.
     *
     * @param keys the keys to search for
     * @return {@code true} if any of the given keys exist in this subtree;
     *         {@code false} otherwise
     */
    public boolean hasAnyKey(@NotNull String... keys) {
        return hasAnyKey(Arrays.asList(keys));
    }

    /**
     * Requires that one or more of the given keys are present anywhere in the
     * subtree rooted at this node.
     * 
     * @param keys the keys to search for
     * @throws IllegalArgumentException if none of the given keys exist in this
     *                                  subtree
     */
    public void requireAnyKey(@NotNull String... keys) throws IllegalArgumentException {
        requireAnyKey(Arrays.asList(keys));
    }

    /**
     * Returns {@code true} if all of the given keys are present anywhere in the
     * subtree rooted at this node. Equivalent to calling {@link #hasKey(String)}
     * for each key and returning {@code true} if all of these calls return
     * {@code true}.
     * 
     * @param keys the keys to search for
     * @return {@code true} if all of the given keys exist in this subtree;
     *         {@code false} otherwise
     */
    public boolean hasAllKeys(@NotNull String... keys) {
        return hasAllKeys(Arrays.asList(keys));
    }

    /**
     * Requires that all of the given keys are present anywhere in the subtree
     * rooted at this node. Equivalent to calling {@link #requireKey(String)} for
     * each key.
     * 
     * @param keys the keys to search for
     * @throws IllegalArgumentException if any of the given keys do not exist in
     *                                  this subtree
     */
    public void requireAllKeys(@NotNull String... keys) throws IllegalArgumentException {
        requireAllKeys(Arrays.asList(keys));
    }

    /**
     * Returns {@code true} if any of the given keys are present anywhere in the
     * subtree rooted at this node. Equivalent to calling {@link #hasKey(String)}
     * for each key and returning {@code true} on the first match.
     *
     * @param keys the keys to search for
     * @return {@code true} if any of the given keys exist in this subtree;
     *         {@code false} otherwise
     */
    public boolean hasAnyKey(@NotNull Collection<String> keys) {
        return keys.stream().anyMatch(this::hasKey);
    }

    /**
     * Requires that one or more of the given keys are present anywhere in the
     * subtree rooted at this node, and throws an {@link IllegalArgumentException}
     * if none can be found. Equivalent to calling {@link #requireKey(String)} for
     * each key.
     * 
     * @param keys the keys to search for
     * @throws IllegalArgumentException if none of the given keys exist in this
     *                                  subtree
     */
    public void requireAnyKey(@NotNull Collection<String> keys) throws IllegalArgumentException {
        if (!keys.stream().anyMatch(this::hasKey)) {
            throw new IllegalArgumentException("The keys, '" + String.join("', '", keys)
                    + "', one of which is required, are all missing in subtree '" + this.key + "'");
        }
    }

    /**
     * Requires that one or more of the given keys are present anywhere in the
     * subtree rooted at this node, and throws an {@link IllegalArgumentException}
     * from the given supplier if none can be found. Equivalent to calling
     * {@link #requireKey(String)} for each key.
     * 
     * @param keys              the keys to search for
     * @param exceptionSupplier supplier of the exception to be thrown if the given
     *                          key cannot be found
     * @throws IllegalArgumentException if none of the given keys exist in this
     *                                  subtree
     */
    public void requireAnyKey(@NotNull Collection<String> keys,
            @NotNull Supplier<IllegalArgumentException> exceptionSupplier) throws IllegalArgumentException {
        if (!hasAnyKey(keys)) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * Returns {@code true} if all of the given keys are present anywhere in the
     * subtree rooted at this node. Equivalent to calling {@link #hasKey(String)}
     * for each key and returning {@code true} if all of these calls return
     * {@code true}.
     * 
     * @param keys the keys to search for
     * @return {@code true} if all of the given keys exist in this subtree;
     *         {@code false} otherwise
     */
    public boolean hasAllKeys(@NotNull Collection<String> keys) {
        return keys.stream().allMatch(this::hasKey);
    }

    /**
     * Requires that all of the given keys are present anywhere in the subtree
     * rooted at this node. Equivalent to calling {@link #requireKey(String)} for
     * each key.
     * 
     * @param keys the keys to search for
     * @throws IllegalArgumentException if any of the given keys do not exist in
     *                                  this subtree
     */
    public void requireAllKeys(@NotNull Collection<String> keys) throws IllegalArgumentException {
        for (String key : keys) {
            if (!hasKey(key)) {
                throw new IllegalArgumentException(
                        "The required key, '" + key + "', is missing in subtree '" + this.key + "'");
            }
        }
    }

    /**
     * Requires that all of the given keys are present anywhere in the subtree
     * rooted at this node. Equivalent to calling {@link #requireKey(String)} for
     * each key. If any key is not present, an {@link IllegalArgumentException} from
     * the given supplier will be thrown.
     * 
     * @param keys              the keys to search for
     * @param exceptionSupplier supplier of the exception to be thrown if any of the
     *                          given keys cannot be found
     * @throws IllegalArgumentException if any of the given keys do not exist in
     *                                  this subtree
     */
    public void requireAllKeys(@NotNull Collection<String> keys,
            @NotNull Supplier<IllegalArgumentException> exceptionSupplier) throws IllegalArgumentException {
        if (!hasAllKeys(keys)) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * Validates a nested JSON object structure scoped under the given key by
     * executing a functional block of validations against it.
     *
     * @param key       the key of the nested object to isolate and validate
     * @param validator a consumer block containing assertions to execute on the
     *                  nested object
     * @throws IllegalArgumentException if the nested object is missing or fails
     *                                  constraints
     */
    public void requireStructure(@NotNull String key, @NotNull Consumer<JSONObject> validator)
            throws IllegalArgumentException {
        JSONObject nested = requireJson(key);
        validator.accept(nested);
    }

    /**
     * Validates a nested JSON object structure with the given path by executing a
     * functional block of validations against it.
     * 
     * @param validator a consumer block containing assertions to execute on the
     *                  nested object
     * @param path      the path segments or a dot-separated path sequence
     * @throws IllegalArgumentException if the path is invalid or the nested object
     *                                  is missing or fails constraints
     */
    public void requireStructure(Consumer<JSONObject> validator, String... path) throws IllegalArgumentException {
        JSONObject nested = requireJsonAt(path);
        validator.accept(nested);
    }

    /**
     * Returns a list of keys belonging to the immediate child fields of this node,
     * provided this node's value holds an object structure.
     *
     * @return a list of immediate field key strings; never {@code null}
     */
    public @NotNull List<String> getFieldKeys() {
        List<String> keys = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof JSONObject itemJson) {
                    keys.add(itemJson.getKey());
                }
            }
        }
        return keys;
    }

    /**
     * Asserts a strict schema rule that ONLY the allowed keys are permitted as
     * immediate children of this object. Throws an error if an extraneous key is
     * encountered.
     *
     * @param allowedKeys the collection of allowed key names
     * @throws IllegalArgumentException if an unexpected extra key is found
     */
    public void requireAllowedKeys(@NotNull Collection<String> allowedKeys) {
        for (String fieldKey : getFieldKeys()) {
            if (!allowedKeys.contains(fieldKey)) {
                throw new IllegalArgumentException(
                        "Unexpected key '" + fieldKey + "' found in object '" + this.key + "'");
            }
        }
    }

    // GET VALUE -------------------------------------------------------------------

    /**
     * Returns the raw value of this node as an {@link Object}. The returned value
     * may be {@code null} or any valid JSON type. Prefer the typed accessors
     * ({@link #getString()}, {@link #getNumber()}, etc.) when the expected type is
     * known.
     * 
     * @return the value of this node, or {@code null}
     */
    public @Nullable Object getValue() {
        return value;
    }

    /**
     * Requires that the value of this node is not {@code null}, throwing an
     * {@link IllegalArgumentException} otherwise, and returns it.
     * 
     * @return the value of this node
     * @throws IllegalArgumentException if the value is {@code null}
     */
    public @NotNull Object requireValue() throws IllegalArgumentException {
        if (value != null) {
            return value;
        }
        throw new IllegalArgumentException("Required non-null value of node '" + this.key + "' was null");
    }

    /**
     * Returns the value of a direct child node of this node as an {@link Object}.
     * The returned value may be any valid JSON type, including {@code null}. Prefer
     * the typed accessors ({@link #getString(String)}, {@link #getNumber(String)},
     * etc.) when the expected type is known.
     * 
     * @param key the key of a child node
     * @return the value of the child node with the given key, or {@code null}
     */
    public @Nullable Object getValue(@NotNull String key) {
        Optional<JSONObject> child = childStream().filter(node -> node.getKey().equals(key)).findFirst();
        if (child.isPresent()) {
            return child.get().getValue();
        }
        return null;
    }

    /**
     * Returns the value of a direct child node of this node as an {@link Object}.
     * The returned value may be any valid JSON type, including {@code null}. If no
     * child node has the given key, an {@link IllegalArgumentException} will be
     * thrown.
     * 
     * @param key the key of a child node
     * @return the value of the child node with the given key, or {@code null}
     * @throws IllegalArgumentException if there is no child with the given key
     */
    public @Nullable Object requireValue(@NotNull String key) throws IllegalArgumentException {
        return childStream().filter(child -> child.getKey().equals(key)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("The required key, '" + key
                        + "' was not present for any of the children of the node '" + this.key + "'"));
    }

    /**
     * Returns the value of a direct child node of this node as an {@link Object}.
     * The returned value may be {@code null} if the child's value is null, or any
     * valid JSON type. If no child node has the given key, an
     * {@link IllegalArgumentException} from the given supplier will be thrown.
     * 
     * @param key               the key of a child node
     * @param exceptionSupplier supplier of the exception to be thrown if no child
     *                          node has the given key
     * @return the value of the child node with the given key, or {@code null}
     * @throws IllegalArgumentException if there is no child with the given key,
     *                                  gotten from the supplier
     */
    public @NotNull Object requireValue(@NotNull String key,
            @NotNull Supplier<IllegalArgumentException> exceptionSupplier) throws IllegalArgumentException {
        return childStream().filter(child -> child.getKey().equals(key)).findFirst().orElseThrow(exceptionSupplier);
    }

    /**
     * Returns the value of this node cast to the given type, or {@code null} if the
     * value is not an instance of that type.
     *
     * @param <R>  the desired return type
     * @param type the class to cast the value to
     * @return the value cast to {@code R}, or {@code null} if the cast is not
     *         possible
     */
    public <R> @Nullable R getValue(@NotNull Class<R> type) {
        return getValue(type, () -> null);
    }

    /**
     * Returns the value of this node cast to the given type, or the result of
     * {@code defaultSupplier} if the value is not an instance of that type.
     *
     * @param <R>             the desired return type
     * @param type            the class to cast the value to
     * @param defaultSupplier supplier invoked to produce a fallback value
     * @return the value cast to {@code R}, or the supplier's result if the cast is
     *         not possible
     */
    public <R> @Nullable R getValue(@NotNull Class<R> type, @NotNull Supplier<R> defaultSupplier) {
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return defaultSupplier.get();
    }

    /**
     * Returns the value of this node cast to the given type, throwing an
     * {@link IllegalArgumentException} if the value is not an instance
     * of that type.
     * 
     * @param <R>  the desired return type
     * @param type the class to cast the value to
     * @return the value cast to {@code R}
     * @throws IllegalArgumentException if this node's value is not castable to
     *                                  {@code R}
     */
    public <R> @NotNull R requireValue(@NotNull Class<R> type) throws IllegalArgumentException {
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        throw new IllegalArgumentException(
                "Value of node '" + key + "' is not of required type " + type.getSimpleName());
    }

    /**
     * Returns the value of this node cast to the given type, throwing an
     * {@link IllegalArgumentException} from the given supplier if the value is not
     * an instance of that type.
     * 
     * @param <R>               the desired return type
     * @param type              the class to cast the value to
     * @param exceptionSupplier supplier of the exception to be thrown if this
     *                          node's value is not castable to {@code R}
     * @return the value cast to {@code R}
     * @throws IllegalArgumentException if this node's value is not
     *                                  castable to {@code R}
     */
    public <R> @NotNull R requireValue(@NotNull Class<R> type, Supplier<IllegalArgumentException> exceptionSupplier)
            throws IllegalArgumentException {
        if (value != null && type.isInstance(value)) {
            return type.cast(value);
        }
        throw exceptionSupplier.get();
    }

    /**
     * Returns the value of this node as a {@link String}, or {@code null} if the
     * value is not a {@link String}. Does not search the tree; operates on this
     * node's value directly.
     *
     * @return the value as a {@link String}, or {@code null}
     */
    public @Nullable String getString() {
        return getValue(String.class);
    }

    /**
     * Returns the value of this node as a {@link String}, throwing an
     * {@link IllegalArgumentException} if the value is not a {@link String}. Does
     * not search the tree; operates on this node's value directly.
     * 
     * @return the value as a {@link String}
     * @throws IllegalArgumentException if this node's value is null, or cannot be
     *                                  casted to a {@link String}
     */
    public @NotNull String requireString() throws IllegalArgumentException {
        return requireValue(String.class);
    }

    /**
     * Returns the value of this node as a {@link String}, throwing an
     * {@link IllegalArgumentException} from the given supplier if the value is null
     * or is not a {@link String}. Does not search the tree; operates on this node's
     * value directly.
     * 
     * @param exceptionSupplier supplier of the exception to be thrown if the value
     *                          is {@code null} or cannot be casted to a
     *                          {@link String}
     * @return the value as a {@link String}
     * @throws IllegalArgumentException if this node's value is null, or cannot be
     *                                  casted to a {@link String}
     */
    public @NotNull String requireString(@NotNull Supplier<IllegalArgumentException> exceptionSupplier)
            throws IllegalArgumentException {
        return requireValue(String.class, exceptionSupplier);
    }

    /**
     * Returns the value of this node as a {@link Number}, or {@code null} if the
     * value is not a {@link Number}. Does not search the tree; operates on this
     * node's value directly.
     *
     * @return the value as a {@link Number}, or {@code null}
     */
    public @Nullable Number getNumber() {
        return getValue(Number.class);
    }

    /**
     * Returns the value of this node as a {@link Number}, throwing an
     * {@link IllegalArgumentException} if the value is not a {@link Number}. Does
     * not search the tree; operates on this node's value directly.
     * 
     * @return the value as a {@link Number}
     * @throws IllegalArgumentException if this node's value is not a {@link Number}
     */
    public @NotNull Number requireNumber() throws IllegalArgumentException {
        return requireValue(Number.class);
    }

    /**
     * Returns the value of this node as a {@link Number}, throwing an
     * {@link IllegalArgumentException} from the given supplier if the value is not
     * a {@link Number}. Does not search the tree; operates on this node's value
     * directly.
     * 
     * @param exceptionSupplier supplier of the exception to be thrown if this
     *                          node's value is not a {@link Number}
     * @return the value as a {@link Number}
     * @throws IllegalArgumentException if this node's value is not a {@link Number}
     */
    public @NotNull Number requireNumber(@NotNull Supplier<IllegalArgumentException> exceptionSupplier)
            throws IllegalArgumentException {
        return requireValue(Number.class, exceptionSupplier);
    }

    /**
     * Returns the value of this node as an {@link Integer}, or {@code null} if the
     * value is not a {@link Number}. Coerces any {@link Number} subtype (e.g.
     * {@link Long}, {@link Double}) to {@code int} via {@link Number#intValue()}.
     * Does not search the tree; operates on this node's value directly.
     *
     * @return the value as an {@link Integer}, or {@code null}
     */
    public @Nullable Integer getInt() {
        Number n = getNumber();
        return n != null ? n.intValue() : null;
    }

    /**
     * Returns the value of this node as an {@link Integer}, throwing an
     * {@link IllegalArgumentException} if the value is not an {@link Integer}. Does
     * not search the tree; operates on this node's value directly.
     * 
     * @return the value as an {@link Integer}
     * @throws IllegalArgumentException if this node's value is not an
     *                                  {@link Integer}
     */
    public @NotNull Integer requireInt() throws IllegalArgumentException {
        return requireValue(Integer.class);
    }

    public @NotNull Integer requireInt(@NotNull Supplier<IllegalArgumentException> exceptionSupplier)
            throws IllegalArgumentException {
        return requireValue(Integer.class, exceptionSupplier);
    }

    /**
     * Returns the value of this node as a {@link Long}, or {@code null} if the
     * value is not a {@link Number}. Coerces any {@link Number} subtype (e.g.
     * {@link Float}, {@link Double}) to {@link Long} via
     * {@link Number#longValue()}. Does not search the tree; operates on this node's
     * value directly.
     *
     * @return the value as a {@link Long}, or {@code null}
     */
    public @Nullable Long getLong() {
        Number n = getNumber();
        return n != null ? n.longValue() : null;
    }

    /**
     * Returns the value of this node as a {@link Long}, throwing an
     * {@link IllegalArgumentException} if the value is not a {@link Long}. Does
     * not search the tree; operates on this node's value directly.
     * 
     * @return the value as a {@link Long}
     * @throws IllegalArgumentException if this node's value is not a {@link Long}
     */
    public @NotNull Long requireLong() throws IllegalArgumentException {
        return requireValue(Long.class);
    }

    public @NotNull Long requireLong(@NotNull Supplier<IllegalArgumentException> exceptionSupplier)
            throws IllegalArgumentException {
        return requireValue(Long.class, exceptionSupplier);
    }

    /**
     * Returns the value of this node as a {@link Double}, or {@code null} if the
     * value is not a {@link Number}. Coerces any {@link Number} subtype (e.g.
     * {@link Long}, {@link Float}) to {@link Double} via
     * {@link Number#doubleValue()}. Does not search the tree; operates on this
     * node's value directly.
     *
     * @return the value as a {@link Double}, or {@code null}
     */
    public @Nullable Double getDouble() {
        Number n = getNumber();
        return n != null ? n.doubleValue() : null;
    }

    /**
     * Returns the value of this node as a {@link Double}, throwing an
     * {@link IllegalArgumentException} if the value is not a {@link Double}. Does
     * not search the tree; operates on this node's value directly.
     * 
     * @return the value as a {@link Double}
     * @throws IllegalArgumentException if this node's value is not a {@link Double}
     */
    public @NotNull Double requireDouble() throws IllegalArgumentException {
        return requireValue(Double.class);
    }

    public @NotNull Double requireDouble(@NotNull Supplier<IllegalArgumentException> exceptionSupplier)
            throws IllegalArgumentException {
        return requireValue(Double.class, exceptionSupplier);
    }

    /**
     * Returns the value of this node as a {@link Float}, or {@code null} if the
     * value is not a {@link Number}. Coerces any {@link Number} subtype (e.g.
     * {@link Long}, {@link Double}) to {@link Float} via
     * {@link Number#floatValue()}. Does not search the tree; operates on this
     * node's value directly.
     *
     * @return the value as a {@link Float}, or {@code null}
     */
    public @Nullable Float getFloat() {
        Number n = getNumber();
        return n != null ? n.floatValue() : null;
    }

    /**
     * Returns the value of this node as a {@link Float}, throwing an
     * {@link IllegalArgumentException} if the value is not a {@link Float}. Does
     * not search the tree; operates on this node's value directly.
     * 
     * @return the value as a {@link Float}
     * @throws IllegalArgumentException if this node's value is not a {@link Float}
     */
    public @NotNull Float requireFloat() throws IllegalArgumentException {
        return requireValue(Float.class);
    }

    public @NotNull Float requireFloat(@NotNull Supplier<IllegalArgumentException> exceptionSupplier)
            throws IllegalArgumentException {
        return requireValue(Float.class, exceptionSupplier);
    }

    /**
     * Returns the value of this node as a {@link Boolean}, or {@code null} if the
     * value is not a {@link Boolean}. Does not search the tree; operates on this
     * node's value directly.
     *
     * @return the value as a {@link Boolean}, or {@code null}
     */
    public @Nullable Boolean getBoolean() {
        return getValue(Boolean.class);
    }

    /**
     * Returns the value of this node as a {@link Boolean}, throwing an
     * {@link IllegalArgumentException} if the value is not a {@link Boolean}. Does
     * not search the tree; operates on this node's value directly.
     * 
     * @return the value as a {@link Boolean}
     * @throws IllegalArgumentException if this node's value is not a
     *                                  {@link Boolean}
     */
    public @NotNull Boolean requireBoolean() throws IllegalArgumentException {
        return requireValue(Boolean.class);
    }

    /**
     * Returns the value of this node as a {@link Boolean}, throwing an
     * {@link IllegalArgumentException} from the given supplier if the value is null
     * or is not a {@link Boolean}. Does not search the tree; operates on this
     * node's value directly.
     * 
     * @param exceptionSupplier supplier of the exception to be thrown if the value
     *                          is {@code null} or cannot be casted to a
     *                          {@link Boolean}
     * @return the value as a {@link Boolean}
     * @throws IllegalArgumentException if this node's value is null, or cannot be
     *                                  casted to a {@link Boolean}
     */
    public @NotNull Boolean requireBoolean(@NotNull Supplier<IllegalArgumentException> exceptionSupplier)
            throws IllegalArgumentException {
        return requireValue(Boolean.class, exceptionSupplier);
    }

    /**
     * Returns the value of this node as a {@link JSONObject}, or {@code null} if
     * the value is not a {@link JSONObject}. Does not search the tree; operates on
     * this node's value directly.
     *
     * @return the value as a {@link JSONObject}, or {@code null}
     */
    public @Nullable JSONObject getJson() {
        return getValue(JSONObject.class);
    }

    /**
     * Returns the value of this node as a {@link JSONObject}, throwing an
     * {@link IllegalArgumentException} if the value is not a {@link JSONObject}.
     * Does
     * not search the tree; operates on this node's value directly.
     * 
     * @return the value as a {@link JSONObject}
     * @throws IllegalArgumentException if this node's value is not a
     *                                  {@link JSONObject}
     */
    public @NotNull JSONObject requireJson() throws IllegalArgumentException {
        return requireValue(JSONObject.class);
    }

    /**
     * Returns the value of this node as a {@link JSONObject}, throwing an
     * {@link IllegalArgumentException} from the given supplier if the value is null
     * or is not a {@link JSONObject}. Does not search the tree; operates on this
     * node's value directly.
     * 
     * @param exceptionSupplier supplier of the exception to be thrown if the value
     *                          is {@code null} or cannot be casted to a
     *                          {@link JSONObject}
     * @return the value as a {@link JSONObject}
     * @throws IllegalArgumentException if this node's value is null, or cannot be
     *                                  casted to a {@link JSONObject}
     */
    public @NotNull JSONObject requireJson(@NotNull Supplier<IllegalArgumentException> exceptionSupplier)
            throws IllegalArgumentException {
        return requireValue(JSONObject.class, exceptionSupplier);
    }

    /**
     * Returns the value of this node as a {@link List}, or {@code null} if the
     * value is not a {@link List}. The list may contain either
     * {@link JSONObject}s or primitive values; use {@link #isObject()} and
     * {@link #isArray()} to distinguish these cases. Does not search the tree;
     * operates on this node's value directly.
     *
     * @return the value as a {@link List}, or {@code null}
     */
    public @Nullable List<?> getArray() {
        // Cannot use getValue() because of raw typing
        if (value instanceof List<?> list) {
            return list;
        }
        return null;
    }

    /**
     * Returns the value of this node as a {@link List}, throwing an
     * {@link IllegalArgumentException} if the value is not a {@link List}. Does
     * not search the tree; operates on this node's value directly.
     * 
     * @return the value as a {@link List}
     * @throws IllegalArgumentException if this node's value is not a
     *                                  {@link List}
     */
    public @NotNull List<?> requireArray() throws IllegalArgumentException {
        // Cannot use requireValue() because of raw typing
        if (value instanceof List<?> list) {
            return list;
        }
        throw new IllegalArgumentException("Value is not of required type List<?>");
    }

    /**
     * Returns the value of this node as a {@link List}, throwing an
     * {@link IllegalArgumentException} from the given supplier if the value is null
     * or is not a {@link List}. Does not search the tree; operates on this node's
     * value directly.
     * 
     * @param exceptionSupplier supplier of the exception to be thrown if the value
     *                          is {@code null} or cannot be casted to a
     *                          {@link List}
     * @return the value as a {@link List}
     * @throws IllegalArgumentException if this node's value is null, or cannot be
     *                                  casted to a {@link List}
     */
    public @NotNull List<?> requireArray(@NotNull Supplier<IllegalArgumentException> exceptionSupplier)
            throws IllegalArgumentException {
        // Cannot use requireValue() because of raw typing
        if (value instanceof List<?> list) {
            return list;
        }
        throw exceptionSupplier.get();
    }

    // FIND VALUE ------------------------------------------------------------------

    /**
     * Searches the subtree rooted at this node for the first node with the given
     * key and returns its value as an {@link Optional}.
     * <p>
     * Search is depth-first. If this node's key matches, its value is returned
     * immediately without descending further. If the value is a nested
     * {@link JSONObject} or a {@link List} of {@link JSONObject}s, those are
     * searched recursively.
     * <p>
     * <b>Note:</b> If the matching node's value is {@code null}, this method
     * returns {@code Optional.empty()}, which is indistinguishable from the key
     * being absent. Use {@link #hasKey(String)} to distinguish these cases.
     *
     * @param key the key to search for
     * @return an {@code Optional} containing the matching node's value, or
     *         {@code Optional.empty()} if no match is found or the matching value
     *         is {@code null}
     */
    public @NotNull Optional<?> find(@NotNull String key) {
        if (this.key.equals(key)) {
            return Optional.ofNullable(this.value);
        }
        if (value instanceof JSONObject valueJson) {
            return valueJson.find(key);
        }
        if (value instanceof Collection<?> valueCollection) {
            List<Object> values = getValues(valueCollection, key);
            if (values.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(values.get(0));
        }
        return Optional.empty();
    }

    /**
     * Searches the subtree rooted at this node for the first node with the given
     * key and returns its value, or the result of {@code defaultSupplier} if no
     * match is found.
     *
     * @param key             the key to search for
     * @param defaultSupplier supplier invoked to produce a fallback value if no
     *                        match is found
     * @return the matching value, or the result of {@code defaultSupplier}
     * @see #find(String)
     */
    public @Nullable Object find(@NotNull String key, @NotNull Supplier<Object> defaultSupplier) {
        Optional<?> res = find(key);
        if (res.isPresent()) {
            return res.get();
        }
        return defaultSupplier.get();
    }

    /**
     * Searches the subtree rooted at this node for the first node with the given
     * key and returns its value. Throws an exception if the key is missing or its
     * value is null.
     *
     * @param key the key to search for
     * @return the matching value; never {@code null}
     * @throws IllegalArgumentException if the key is missing or its value is null
     */
    public @NotNull Object require(@NotNull String key) throws IllegalArgumentException {
        return find(key).orElseThrow(() -> new IllegalArgumentException(
                "Required key '" + key + "' is missing or null in subtree '" + this.key + "'"));
    }

    /**
     * Searches the subtree rooted at this node for the first node matching any of
     * the given keys and returns its value as an {@link Optional}. Keys are tried
     * in the order provided; the first match found across the entire subtree is
     * returned.
     *
     * @param keys the keys to search for, in priority order
     * @return an {@code Optional} containing the first matching value found, or
     *         {@code Optional.empty()} if no match is found
     * @see #find(String)
     */
    public @NotNull Optional<?> find(@NotNull String... keys) {
        return find(Arrays.asList(keys));
    }

    /**
     * Searches the subtree rooted at this node for the first node matching any of
     * the given keys and returns its value, throwing an
     * {@link IllegalArgumentException} if none is found. Keys are tried in the
     * order provided; the first match across the entire subtree is returned.
     * 
     * @param keys the keys to search for, in priority order
     * @return the first matching value found
     * @throws IllegalArgumentException if no node with any of the given keys exists
     *                                  in this subtree
     */
    public @NotNull Object require(@NotNull String... keys) throws IllegalArgumentException {
        return require(Arrays.asList(keys));
    }

    /**
     * Searches the subtree rooted at this node for the first node matching any of
     * the given keys and returns its value as an {@link Optional}. Keys are tried
     * in the order provided; the first match found across the entire subtree is
     * returned.
     *
     * @param keys the keys to search for, in priority order
     * @return an {@code Optional} containing the first matching value found, or
     *         {@code Optional.empty()} if no match is found
     * @see #find(String)
     */
    public @NotNull Optional<?> find(@NotNull Collection<String> keys) {
        for (String key : keys) {
            Optional<?> res = find(key);
            if (res.isPresent()) {
                return res;
            }
        }
        return Optional.empty();
    }

    /**
     * Searches the subtree rooted at this node for the first node matching any of
     * the given keys and returns its value, or the result of
     * {@code defaultSupplier} if none is found. Keys are tried in the order
     * provided; the first match found across the entire subtree is returned.
     *
     * @param keys            the keys to search for, in priority order
     * @param defaultSupplier supplier invoked to produce a fallback value if no
     *                        match is found
     * @return the first matching value found, or the result of
     *         {@code defaultSupplier}
     */
    public @Nullable Object find(@NotNull Collection<String> keys, @NotNull Supplier<Object> defaultSupplier) {
        Optional<?> res = find(keys);
        if (res.isPresent()) {
            return res.get();
        }
        return defaultSupplier.get();
    }

    /**
     * Searches the subtree rooted at this node for the first node matching any of
     * the given keys and returns its value, throwing an
     * {@link IllegalArgumentException} if none is found. Keys are tried in the
     * order provided; the first match across the entire subtree is returned.
     * 
     * @param keys the keys to search for, in priority order
     * @return the first matching value found
     * @throws IllegalArgumentException if no node with any of the given keys exists
     *                                  in this subtree
     */
    public @NotNull Object require(@NotNull Collection<String> keys) throws IllegalArgumentException {
        return find(keys).orElseThrow(() -> new IllegalArgumentException("The keys, '"
                + String.join("', '", keys) + "', one of which is required, are all missing in subtree '"
                + this.key + "'"));
    }

    /**
     * Searches the subtree rooted at this node for the first node matching any of
     * the given keys and returns its value, throwing an
     * {@link IllegalArgumentException} from the given supplier if none is found.
     * Keys are tried in the order provided; the first match across the entire
     * subtree is returned.
     * 
     * @param keys              the keys to search for, in priority order
     * @param exceptionSupplier Supplier of the exception to be thrown if no match
     *                          is found for any of the keys
     * @return the first matching value found
     * @throws IllegalArgumentException if no node with any of the given keys exists
     *                                  in this subtree, gotten from the supplier
     */
    public @NotNull Object require(@NotNull Collection<String> keys,
            @NotNull Supplier<IllegalArgumentException> exceptionSupplier) throws IllegalArgumentException {
        return find(keys).orElseThrow(exceptionSupplier);
    }

    /**
     * Searches the subtree rooted at this node for all nodes with the given key and
     * returns their values as a list. Unlike {@link #find(String)}, which stops at
     * the first match, this method accumulates every matching node's value across
     * the entire subtree.
     * <p>
     * Values are returned in the order they are encountered during depth-first
     * traversal. If no nodes match, an empty list is returned.
     *
     * @param key the key to search for
     * @return a list of all values found for the given key; never {@code null}
     */
    public @NotNull List<Object> findAll(@NotNull String key) {
        List<Object> values = new ArrayList<>();
        if (this.key.equals(key)) {
            values.add(this.value);
        }
        if (value instanceof JSONObject valueJson) {
            values.addAll(valueJson.findAll(key));
        } else if (value instanceof Collection<?> valueCollection) {
            values.addAll(getValues(valueCollection, key));
        }
        return values;
    }

    /**
     * Searches the subtree rooted at this node for all nodes matching any of the
     * given keys and returns their values as a list. Equivalent to calling
     * {@link #findAll(String)} for each key in order and concatenating the results.
     *
     * @param keys the keys to search for
     * @return a list of all values found for any of the given keys; never
     *         {@code null}
     */
    public @NotNull List<Object> findAll(@NotNull String... keys) {
        return findAll(Arrays.asList(keys));
    }

    /**
     * Searches the subtree rooted at this node for all nodes matching any of the
     * given keys and returns their values as a list. Equivalent to calling
     * {@link #findAll(String)} for each key in order and concatenating the results.
     *
     * @param keys the keys to search for
     * @return a list of all values found for any of the given keys; never
     *         {@code null}
     */
    public @NotNull List<Object> findAll(@NotNull Collection<String> keys) {
        List<Object> values = new ArrayList<>();
        for (String key : keys) {
            values.addAll(findAll(key));
        }
        return values;
    }

    /**
     * Searches a collection of items for all nodes with the given key and
     * accumulates their values. Items that are {@link JSONObject}s are searched
     * via {@link #findAll(String)}; items that are nested {@link Collection}s are
     * recursed into. Non-{@link JSONObject}, non-{@code Collection} items are
     * ignored.
     * <p>
     * This is a helper for {@link #find(String)} and {@link #findAll(String)}
     * when this node's value is a {@code Collection}.
     *
     * @param collection the collection of items to search
     * @param key        the key to search for
     * @return a list of all values found; never {@code null}
     */
    private @NotNull List<Object> getValues(@NotNull Collection<?> collection, @NotNull String key) {
        List<Object> values = new ArrayList<>();
        for (Object item : collection) {
            if (item instanceof JSONObject itemJson) {
                values.addAll(itemJson.findAll(key));
            } else if (item instanceof Collection<?> itemCollection) {
                values.addAll(getValues(itemCollection, key));
            }
        }
        return values;
    }

    /**
     * Searches the subtree rooted at this node for the first node with the given
     * key whose value is an instance of {@code type}, and returns it as a typed
     * {@link Optional}.
     * <p>
     * If multiple nodes share the given key, the first one whose value matches
     * {@code type} is returned. Nodes whose values do not match {@code type} are
     * skipped.
     *
     * @param <R>  the desired return type
     * @param type the class to match and cast the value to
     * @param key  the key to search for
     * @return an {@code Optional} containing the first matching value cast to
     *         {@code R}, or {@code Optional.empty()} if no match is found or no
     *         match has the correct type
     */
    public <R> @NotNull Optional<R> find(@NotNull Class<R> type, @NotNull String key) {
        for (Object value : findAll(key)) {
            if (type.isInstance(value)) {
                return Optional.of(type.cast(value));
            }
        }
        return Optional.empty();
    }

    /**
     * Searches the subtree rooted at this node for the first node with the given
     * key whose value is an instance of {@code type}, and returns it, or the result
     * of {@code defaultSupplier} if no match is found.
     *
     * @param <R>             the desired return type
     * @param type            the class to match and cast the value to
     * @param key             the key to search for
     * @param defaultSupplier supplier invoked to produce a fallback value
     * @return the first matching value cast to {@code R}, or the result of
     *         {@code defaultSupplier}
     */
    public <R> @Nullable R find(@NotNull Class<R> type, @NotNull String key, @NotNull Supplier<R> defaultSupplier) {
        Optional<R> res = find(type, key);
        if (res.isPresent()) {
            return res.get();
        }
        return defaultSupplier.get();
    }

    /**
     * Searches the subtree rooted at this node for the first node with the given
     * key whose value is an instance of {@code type}, and returns it.
     * Throws an exception if the key is missing or of an incorrect type.
     *
     * @param <R>  the desired return type
     * @param type the class to match and cast the value to
     * @param key  the key to search for
     * @return the matching value cast to {@code R}; never {@code null}
     * @throws IllegalArgumentException if the key is missing or of an incorrect
     *                                  type
     */
    public <R> @NotNull R require(@NotNull Class<R> type, @NotNull String key) throws IllegalArgumentException {
        return find(type, key).orElseThrow(() -> new IllegalArgumentException("Required key '" + key + "' of type "
                + type.getSimpleName() + " is missing or incorrect in subtree '" + this.key + "'"));
    }

    /**
     * Searches the subtree rooted at this node for the first node matching any of
     * the given keys whose value is an instance of {@code type}, and returns it as
     * a typed {@link Optional}. Keys are tried in the order provided.
     *
     * @param <R>  the desired return type
     * @param type the class to match and cast the value to
     * @param keys the keys to search for, in priority order
     * @return an {@code Optional} containing the first matching value cast to
     *         {@code R}, or {@code Optional.empty()} if no match is found
     */
    public <R> @NotNull Optional<R> find(@NotNull Class<R> type, @NotNull String... keys) {
        return find(type, Arrays.asList(keys));
    }

    /**
     * Searches the subtree rooted at this node for the first node matching any of
     * the given keys whose value is an instance of {@code type}, and returns it. If
     * no matching node with any of the keys and the required type is found, throws
     * an {@link IllegalArgumentException}. Keys are tried in the order provided.
     * 
     * @param <R>  the desired return type
     * @param type the class to match and cast the value to
     * @param keys the keys to search for, in priority order
     * @return the first matching value cast to {@code R}
     * @throws IllegalArgumentException if no matching node with any of the keys and
     *                                  the required type is found
     */
    public <R> @NotNull R require(@NotNull Class<R> type, @NotNull String... keys) throws IllegalArgumentException {
        return require(type, Arrays.asList(keys));
    }

    /**
     * Searches the subtree rooted at this node for the first node matching any of
     * the given keys whose value is an instance of {@code type}, and returns it as
     * a typed {@link Optional}. Keys are tried in the order provided.
     *
     * @param <R>  the desired return type
     * @param type the class to match and cast the value to
     * @param keys the keys to search for, in priority order
     * @return an {@code Optional} containing the first matching value cast to
     *         {@code R}, or {@code Optional.empty()} if no match is found
     */
    public <R> @NotNull Optional<R> find(@NotNull Class<R> type, @NotNull Collection<String> keys) {
        for (String key : keys) {
            Optional<R> res = find(type, key);
            if (res.isPresent()) {
                return res;
            }
        }
        return Optional.empty();
    }

    /**
     * Searches the subtree rooted at this node for the first node matching any of
     * the given keys whose value is an instance of {@code type}, and returns it, or
     * the result of {@code defaultSupplier} if no match is found. Keys are tried in
     * the order provided.
     *
     * @param <R>             the desired return type
     * @param type            the class to match and cast the value to
     * @param keys            the keys to search for, in priority order
     * @param defaultSupplier supplier invoked to produce a fallback value
     * @return the first matching value cast to {@code R}, or the result of
     *         {@code defaultSupplier}
     */
    public <R> @Nullable R find(@NotNull Class<R> type, @NotNull Collection<String> keys,
            @NotNull Supplier<R> defaultSupplier) {
        Optional<R> res = find(type, keys);
        if (res.isPresent()) {
            return res.get();
        }
        return defaultSupplier.get();
    }

    /**
     * Searches the subtree rooted at this node for the first node matching any of
     * the given keys whose value is an instance of {@code type}, and returns it. If
     * no matching node with any of the keys and the required type is found, throws
     * an {@link IllegalArgumentException}. Keys are tried in the order provided.
     * 
     * @param <R>  the desired return type
     * @param type the class to match and cast the value to
     * @param keys the keys to search for, in priority order
     * @return the first matching value cast to {@code R}
     * @throws IllegalArgumentException if no matching node with any of the keys and
     *                                  the required type is found
     */
    public <R> @NotNull R require(@NotNull Class<R> type, @NotNull Collection<String> keys) {
        return find(type, keys).orElseThrow(() -> new IllegalArgumentException("The keys, '"
                + String.join("', '", keys) + "', one of which is required of type " + type.getSimpleName()
                + ", are all missing or incorrect in subtree '" + this.key + "'"));
    }

    /**
     * Resolves and returns the {@link JSONObject} node situated at the end of the
     * specified path segments. Path strings can be supplied as explicit varargs,
     * or a single string split by dots (e.g., {@code "person.address.city"}). To
     * include dots in a segment of the path, preceed each with a backslash (e.g.,
     * {@code "dates.01\.10.temperature"}).
     * <p>
     * If the path's first element matches this node's actual root key, navigation
     * gracefully steps into the node rather than failing.
     *
     * @param path the path segments or a dot-separated path sequence
     * @return an {@code Optional} holding the matched {@link JSONObject} node,
     *         or {@code Optional.empty()} if the path is invalid or unresolvable
     */
    public @NotNull Optional<JSONObject> findNodeAt(@NotNull String... path) {
        List<String> segments = new ArrayList<>();
        for (String p : path) {
            if (p.contains(".")) {
                segments.addAll(Arrays.asList(p.split("(?<!\\\\)\\.")));
            } else {
                segments.add(p);
            }
        }
        if (segments.isEmpty()) {
            return Optional.of(this);
        }

        JSONObject current = this;
        int startIndex = 0;
        if (current.key.equals(segments.get(0))) {
            startIndex = 1;
        }

        for (int i = startIndex; i < segments.size(); i++) {
            String segment = segments.get(i);
            if (current.value instanceof List<?> list) {
                JSONObject found = null;
                for (Object item : list) {
                    if (item instanceof JSONObject child && child.getKey().equals(segment)) {
                        found = child;
                        break;
                    }
                }
                if (found == null) {
                    return Optional.empty();
                }
                current = found;
            } else if (current.value instanceof JSONObject child && child.getKey().equals(segment)) {
                current = child;
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(current);
    }

    /**
     * Resolves and returns the {@link JSONObject} node situated at the end of the
     * specified path segments. Path strings can be supplied as explicit varargs,
     * or a single string split by dots (e.g., {@code "person.address.city"}). To
     * include dots in a segment of the path, preceed each with a backslash (e.g.,
     * {@code "dates.01\.10.temperature"}).
     * <p>
     * If the path is invalid or cannot be resolved, throws an
     * {@link IllegalArgumentException}.
     * <p>
     * If the path's first element matches this node's actual root key, navigation
     * gracefully steps into the node rather than failing.
     *
     * @param path the path segments or a dot-separated path sequence
     * @return the matched {@link JSONObject} node
     * @throws IllegalArgumentException if the required path is invalid or
     *                                  unresolvable
     */
    public @NotNull JSONObject requireNodeAt(@NotNull String... path) {
        return findNodeAt(path).orElseThrow(() -> new IllegalArgumentException("The required path, '"
                + String.join(".", path) + " is invalid or could not be resolved for subtree '" + this.key + "'"));
    }

    /**
     * Resolves the given key path and returns the target raw unwrapped value. Path
     * strings can be supplied as explicity varargs, or a single string split by
     * dots (e.g., {@code "person.address.city"}). To include dots in a segment of
     * the path, preceed each with a backslash (e.g.,
     * {@code "dates.01\.10.temperature"}).
     *
     * @param path the path segments or a dot-separated path sequence
     * @return an {@code Optional} containing the raw value at the path,
     *         or {@code Optional.empty()} if unresolved
     */
    public @NotNull Optional<?> findAt(@NotNull String... path) {
        return findNodeAt(path).map(JSONObject::getValue);
    }

    /**
     * Resolves the given key path and returns the target raw unwrapped value. Path
     * strings can be supplied as explicity varargs, or a single string split by
     * dots (e.g., {@code "person.address.city"}). To include dots in a segment of
     * the path, preceed each with a backslash (e.g.,
     * {@code "dates.01\.10.temperature"}).
     * <p>
     * If the path is invalid or cannot be resolved, throws an
     * {@link IllegalArgumentException}.
     *
     * @param path the path segments or a dot-separated path sequence
     * @return the raw value at the path
     * @throws IllegalArgumentException if the path is invalid or unresolvable
     */
    public @NotNull Object requireAt(@NotNull String... path) {
        return findAt(path).orElseThrow(() -> new IllegalArgumentException("The required path, '"
                + String.join(".", path) + " is invalid or could not be resolved for subtree '" + this.key + "'"));
    }

    // FIND STRING VALUE -----------------------------------------------------------

    /**
     * Searches the subtree rooted at this node for the first {@link String} value
     * stored under the given key.
     *
     * @param key the key to search for
     * @return an {@code Optional} containing the first matching {@link String}
     *         value, or {@code Optional.empty()} if not found
     */
    public @NotNull Optional<String> findString(@NotNull String key) {
        return find(String.class, key);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link String} value
     * stored under the given key, returning the result of {@code defaultSupplier}
     * if not found.
     *
     * @param key             the key to search for
     * @param defaultSupplier supplier invoked to produce a fallback value
     * @return the first matching {@link String} value, or the result of
     *         {@code defaultSupplier}
     */
    public @Nullable String findString(@NotNull String key, @NotNull Supplier<String> defaultSupplier) {
        return find(String.class, key, defaultSupplier);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link String} value
     * stored under the given key, throwing an {@link IllegalArgumentException} if
     * not found.
     * 
     * @param key the key to search for
     * @return the first matching {@link String} value
     * @throws IllegalArgumentException if no {@link String} value can be found with
     *                                  the given key
     */
    public @NotNull String requireString(@NotNull String key) throws IllegalArgumentException {
        return findString(key).orElseThrow(() -> new IllegalArgumentException("The required String value '" + key
                + "' could not be found, could not be casted, or was null for object '" + this.key + "'"));
    }

    /**
     * Searches the subtree rooted at this node for the first {@link String} value
     * stored under the given key, throwing an {@link IllegalArgumentException} from
     * the given supplier if not found.
     * 
     * @param key               the key to search for
     * @param exceptionSupplier supplier of the exception to be thrown if no
     *                          {@link String} value can be found with the given key
     * @return the first matching {@link String} value
     * @throws IllegalArgumentException if no {@link String} value can be found with
     *                                  the given key, gotten from the given
     *                                  supplier
     */
    public @NotNull String requireString(@NotNull String key,
            @NotNull Supplier<IllegalArgumentException> exceptionSupplier) throws IllegalArgumentException {
        return findString(key).orElseThrow(exceptionSupplier);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link String} value
     * stored under any of the given keys, tried in the order provided.
     *
     * @param keys the keys to search for, in priority order
     * @return an {@code Optional} containing the first matching {@link String}
     *         value, or {@code Optional.empty()} if not found
     */
    public @NotNull Optional<String> findString(@NotNull String... keys) {
        return find(String.class, keys);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link String} value
     * stored under any of the given keys, tried in the order provided. Throws an
     * {@link IllegalArgumentException} if no {@link String} can be found with the
     * given keys.
     * 
     * @param keys the keys to search for, in priority order
     * @return the first matching {@link String} value
     * @throws IllegalArgumentException if no {@link String} value can be found with
     *                                  the given keys
     */
    public @NotNull String requireString(@NotNull String... keys) throws IllegalArgumentException {
        return requireString(Arrays.asList(keys));
    }

    /**
     * Searches the subtree rooted at this node for the first {@link String} value
     * stored under any of the given keys, tried in the order provided.
     *
     * @param keys the keys to search for, in priority order
     * @return an {@code Optional} containing the first matching {@link String}
     *         value, or {@code Optional.empty()} if not found
     */
    public @NotNull Optional<String> findString(@NotNull Collection<String> keys) {
        return find(String.class, keys);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link String} value
     * stored under any of the given keys, tried in the order provided, returning
     * the result of {@code defaultSupplier} if not found.
     *
     * @param keys            the keys to search for, in priority order
     * @param defaultSupplier supplier invoked to produce a fallback value
     * @return the first matching {@link String} value, or the result of
     *         {@code defaultSupplier}
     */
    public @Nullable String findString(@NotNull Collection<String> keys, @NotNull Supplier<String> defaultSupplier) {
        return find(String.class, keys, defaultSupplier);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link String} value
     * stored under any of the given keys, tried in the order provided. Throws an
     * {@link IllegalArgumentException} if no {@link String} can be found with the
     * given keys.
     * 
     * @param keys the keys to search for, in priority order
     * @return the first matching {@link String} value
     * @throws IllegalArgumentException if no {@link String} value can be found with
     *                                  the given keys
     */
    public @NotNull String requireString(@NotNull Collection<String> keys) throws IllegalArgumentException {
        return findString(keys).orElseThrow(() -> new IllegalArgumentException("The keys, " + String.join("', '", keys)
                + "', one of which is required of type String, are all missing, null, or could not be casted in subtree '"
                + this.key + "'"));
    }

    /**
     * Searches the subtree rooted at this node for the first {@link String} value
     * stored under any of the given keys, tried in the order provided. Throws an
     * {@link IllegalArgumentException} from the given supplier if no {@link String}
     * can be found with the given keys.
     * 
     * @param keys              the keys to search for, in priority order
     * @param exceptionSupplier supplier of the exception to be thrown if no
     *                          {@link String} value can be found with the given
     *                          keys
     * @return the first matching {@link String} value
     * @throws IllegalArgumentException if no {@link String} value can be found with
     *                                  the given keys
     */
    public @NotNull String requireString(@NotNull Collection<String> keys,
            @NotNull Supplier<IllegalArgumentException> exceptionSupplier) throws IllegalArgumentException {
        return findString(keys).orElseThrow(exceptionSupplier);
    }

    /**
     * Resolves the key path and returns the value cast to a {@link String}.
     *
     * @param path the path segments or a dot-separated path sequence
     * @return an {@code Optional} containing the {@link String} value, or
     *         {@code Optional.empty()}
     */
    public @NotNull Optional<String> findStringAt(@NotNull String... path) {
        return findAt(path).filter(String.class::isInstance).map(String.class::cast);
    }

    /**
     * Resolves the key path and returns the value cast to a {@link String},
     * throwing an {@link IllegalArgumentException} if the path is invalid or
     * unresolvable, or if the value is {@code null} or cannot be casted to a
     * {@link String}.
     * 
     * @param path the path segments or a dot-separated path sequence
     * @return the {@link String} value
     * @throws IllegalArgumentException if the path is invalid or unresolvable, or
     *                                  if the value is {@code null} or cannot be
     *                                  casted to a {@link String}
     */
    public @NotNull String requireStringAt(@NotNull String... path) {
        return findStringAt(path).orElseThrow(() -> new IllegalArgumentException("The path " + String.join(".", path)
                + " could not be resolved or resulted in a null or non-String value in subtree '" + this.key + "'"));
    }

    // FIND NUMBER VALUE -----------------------------------------------------------

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under the given key.
     *
     * @param key the key to search for
     * @return an {@code Optional} containing the first matching {@link Number}
     *         value, or {@code Optional.empty()} if not found
     */
    public @NotNull Optional<Number> findNumber(@NotNull String key) {
        return find(Number.class, key);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under the given key, returning the result of {@code defaultSupplier}
     * if not found.
     *
     * @param key             the key to search for
     * @param defaultSupplier supplier invoked to produce a fallback value
     * @return the first matching {@link Number} value, or the result of
     *         {@code defaultSupplier}
     */
    public @Nullable Number findNumber(@NotNull String key, @NotNull Supplier<Number> defaultSupplier) {
        return find(Number.class, key, defaultSupplier);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under the given key, throwing an {@link IllegalArgumentException} if
     * not found.
     * 
     * @param key the key to search for
     * @return the first matching {@link Number} value
     * @throws IllegalArgumentException if no {@link Number} value can be found with
     *                                  the given key
     */
    public @NotNull Number requireNumber(@NotNull String key) throws IllegalArgumentException {
        return findNumber(key).orElseThrow(() -> new IllegalArgumentException("The required Number value '" + key +
                "' could not be found, could not be casted, or was null for object '" + this.key + "'"));
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under the given key, throwing an {@link IllegalArgumentException} from
     * the given supplier if not found.
     * 
     * @param key               the key to search for
     * @param exceptionSupplier supplier of the exception to be thrown if no
     *                          {@link Number} value can be found with the given key
     * @return the first matching {@link Number} value
     * @throws IllegalArgumentException if no {@link Number} value can be found with
     *                                  the given key, gotten from the given
     *                                  supplier
     */
    public @NotNull Number requireNumber(@NotNull String key,
            @NotNull Supplier<IllegalArgumentException> exceptionSupplier) throws IllegalArgumentException {
        return findNumber(key).orElseThrow(exceptionSupplier);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under any of the given keys, tried in the order provided.
     *
     * @param keys the keys to search for, in priority order
     * @return an {@code Optional} containing the first matching {@link Number}
     *         value, or {@code Optional.empty()} if not found
     */
    public @NotNull Optional<Number> findNumber(@NotNull String... keys) {
        return find(Number.class, keys);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under any of the given keys, tried in the order provided. Throws an
     * {@link IllegalArgumentException} if no {@link Number} can be found with the
     * given keys.
     * 
     * @param keys the keys to search for, in priority order
     * @return the first matching {@link Number} value
     * @throws IllegalArgumentException if no {@link Number} value can be found with
     *                                  the given keys
     */
    public @NotNull Number requireNumber(@NotNull String... keys) throws IllegalArgumentException {
        return requireNumber(Arrays.asList(keys));
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under any of the given keys, tried in the order provided.
     *
     * @param keys the keys to search for, in priority order
     * @return an {@code Optional} containing the first matching {@link Number}
     *         value, or {@code Optional.empty()} if not found
     */
    public @NotNull Optional<Number> findNumber(@NotNull Collection<String> keys) {
        return find(Number.class, keys);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under any of the given keys, tried in the order provided, returning
     * the result of {@code defaultSupplier} if not found.
     *
     * @param keys            the keys to search for, in priority order
     * @param defaultSupplier supplier invoked to produce a fallback value
     * @return the first matching {@link Number} value, or the result of
     *         {@code defaultSupplier}
     */
    public @Nullable Number findNumber(@NotNull Collection<String> keys, @NotNull Supplier<Number> defaultSupplier) {
        return find(Number.class, keys, defaultSupplier);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under any of the given keys, tried in the order provided. Throws an
     * {@link IllegalArgumentException} if no {@link Number} can be found with the
     * given keys.
     * 
     * @param keys the keys to search for, in priority order
     * @return the first matching {@link Number} value
     * @throws IllegalArgumentException if no {@link Number} value can be found with
     *                                  the given keys
     */
    public @NotNull Number requireNumber(@NotNull Collection<String> keys) throws IllegalArgumentException {
        return findNumber(keys).orElseThrow(() -> new IllegalArgumentException("The keys, " + String.join("', '", keys)
                + "', one of which is required of type Number, are all missing, null, or could not be casted in subtree '"
                + this.key + "'"));
    }

    public @NotNull Number requireNumber(@NotNull Collection<String> keys,
            @NotNull Supplier<IllegalArgumentException> exceptionSupplier) throws IllegalArgumentException {
        return findNumber(keys).orElseThrow(exceptionSupplier);
    }

    /**
     * Resolves the key path and returns the value cast to a {@link Number}.
     *
     * @param path the path segments or a dot-separated path sequence
     * @return an {@code Optional} containing the number value, or
     *         {@code Optional.empty()}
     */
    public @NotNull Optional<Number> findNumberAt(@NotNull String... path) {
        return findAt(path).filter(Number.class::isInstance).map(Number.class::cast);
    }

    public @NotNull Number requireNumberAt(@NotNull String... path) {
        return findNumberAt(path).orElseThrow(() -> new IllegalArgumentException("The path " + String.join(".", path)
                + " could not be resolved or resulted in a null or non-Number value in subtree '" + this.key + "'"));
    }

    // FIND INTEGER VALUE ----------------------------------------------------------

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under the given key and returns it as an {@link Integer}. Any
     * {@link Number} subtype is accepted and coerced via {@link Number#intValue()}.
     *
     * @param key the key to search for
     * @return an {@code Optional} containing the first matching value as an
     *         {@link Integer}, or {@code Optional.empty()} if not found
     */
    public @NotNull Optional<Integer> findInt(@NotNull String key) {
        return findNumber(key).map(Number::intValue);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under the given key and returns it as an {@link Integer}, returning
     * the result of {@code defaultSupplier} if not found. Any {@link Number}
     * subtype is accepted and coerced via {@link Number#intValue()}.
     *
     * @param key             the key to search for
     * @param defaultSupplier supplier invoked to produce a fallback value
     * @return the first matching value as an {@link Integer}, or the result of
     *         {@code defaultSupplier}
     */
    public @Nullable Integer findInt(@NotNull String key, @NotNull Supplier<Integer> defaultSupplier) {
        return findNumber(key).map(Number::intValue).orElseGet(defaultSupplier);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under the given key and casts it to an {@link Integer}, throwing an
     * {@link IllegalArgumentException} if not found.
     * 
     * @param key the key to search for
     * @return the first matching {@link Number} value casted to an {@link Integer}
     * @throws IllegalArgumentException if no {@link Number} value can be found with
     *                                  the given key
     */
    public @NotNull Integer requireInt(@NotNull String key) throws IllegalArgumentException {
        return findInt(key).orElseThrow(() -> new IllegalArgumentException("The required Integer value '" + key +
                "' could not be found, could not be casted, or was null for object '" + this.key + "'"));
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under the given key and casts it to an {@link Integer}, throwing an
     * {@link IllegalArgumentException} from the given supplier if not found.
     * 
     * @param key               the key to search for
     * @param exceptionSupplier supplier of the exception to be thrown if no
     *                          {@link String} value can be found with the given key
     * @return the first matching {@link Number} value casted to an {@link Integer}
     * @throws IllegalArgumentException if no {@link Number} value can be found with
     *                                  the given key, gotten from the given
     *                                  supplier
     */
    public @NotNull Integer requireInt(@NotNull String key,
            @NotNull Supplier<IllegalArgumentException> exceptionSupplier) throws IllegalArgumentException {
        return findInt(key).orElseThrow(exceptionSupplier);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under any of the given keys, tried in the order provided, and returns
     * it as an {@link Integer}. Any {@link Number} subtype is accepted and coerced
     * via {@link Number#intValue()}.
     *
     * @param keys the keys to search for, in priority order
     * @return an {@code Optional} containing the first matching value as an
     *         {@link Integer}, or {@code Optional.empty()} if not found
     */
    public @NotNull Optional<Integer> findInt(@NotNull String... keys) {
        return findNumber(keys).map(Number::intValue);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under any of the given keys and casts it to an {@link Integer}, tried
     * in the order provided. Throws an {@link IllegalArgumentException} if no
     * {@link Number} can be found with the given keys.
     * 
     * @param keys the keys to search for, in priority order
     * @return the first matching {@link Number} value casted to an {@link Integer}
     * @throws IllegalArgumentException if no {@link Number} value can be found with
     *                                  the given keys
     */
    public @NotNull Integer requireInt(@NotNull String... keys) throws IllegalArgumentException {
        return requireInt(Arrays.asList(keys));
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under any of the given keys, tried in the order provided, and returns
     * it as an {@link Integer}. Any {@link Number} subtype is accepted and coerced
     * via {@link Number#intValue()}.
     *
     * @param keys the keys to search for, in priority order
     * @return an {@code Optional} containing the first matching value as an
     *         {@link Integer}, or {@code Optional.empty()} if not found
     */
    public @NotNull Optional<Integer> findInt(@NotNull Collection<String> keys) {
        return findNumber(keys).map(Number::intValue);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under any of the given keys, tried in the order provided, and returns
     * it as an {@link Integer}, returning the result of {@code defaultSupplier} if
     * not found. Any {@link Number} subtype is accepted and coerced via
     * {@link Number#intValue()}.
     *
     * @param keys            the keys to search for, in priority order
     * @param defaultSupplier supplier invoked to produce a fallback value
     * @return the first matching value as an {@link Integer}, or the result of
     *         {@code defaultSupplier}
     */
    public @Nullable Integer findInt(@NotNull Collection<String> keys, @NotNull Supplier<Integer> defaultSupplier) {
        return findNumber(keys).map(Number::intValue).orElseGet(defaultSupplier);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under any of the given keys and casts it to an {@link Integer}, tried
     * in the order provided. Throws an {@link IllegalArgumentException} if no
     * {@link Number} can be found with the given keys.
     * 
     * @param keys the keys to search for, in priority order
     * @return the first matching {@link Number} value casted to an {@link Integer}
     * @throws IllegalArgumentException if no {@link Number} value can be found with
     *                                  the given keys
     */
    public @NotNull Integer requireInt(@NotNull Collection<String> keys) throws IllegalArgumentException {
        return findInt(keys).orElseThrow(() -> new IllegalArgumentException("The keys, " + String.join("', '", keys)
                + "', one of which is required of type Integer, are all missing, null, or could not be casted in subtree '"
                + this.key + "'"));
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under any of the given keys and casts it to an {@link Integer}, tried
     * in the order provided. Throws an {@link IllegalArgumentException} from the
     * given supplier if no {@link Number} can be found with the given keys.
     * 
     * @param keys the keys to search for, in priority order
     * @return the first matching {@link Number} value casted to an {@link Integer}
     * @throws IllegalArgumentException if no {@link Number} value can be found with
     *                                  the given keys, gotten from the given
     *                                  supplier
     */
    public @NotNull Integer requireInt(@NotNull Collection<String> keys,
            @NotNull Supplier<IllegalArgumentException> exceptionSupplier) throws IllegalArgumentException {
        return findInt(keys).orElseThrow(exceptionSupplier);
    }

    /**
     * Resolves the key path and returns the value coerced to an {@link Integer}.
     *
     * @param path the path segments or a dot-separated path sequence
     * @return an {@code Optional} containing the integer value, or
     *         {@code Optional.empty()}
     */
    public @NotNull Optional<Integer> findIntAt(@NotNull String... path) {
        return findNumberAt(path).map(Number::intValue);
    }

    /**
     * Resolves the key path and returns the value cast to an {@link Integer},
     * throwing an {@link IllegalArgumentException} if the path is invalid or
     * unresolvable, or if the value is {@code null} or cannot be casted to an
     * {@link Integer}.
     * 
     * @param path the path segments or a dot-separated path sequence
     * @return the {@link Integer} value
     * @throws IllegalArgumentException if the path is invalid or unresolvable, or
     *                                  if the value is {@code null} or cannot be
     *                                  casted to an {@link Integer}
     */
    public @NotNull Integer requireIntAt(@NotNull String... path) throws IllegalArgumentException {
        return findIntAt(path).orElseThrow(() -> new IllegalArgumentException("The path " + String.join(".", path)
                + " could not be resolved or resulted in a null or non-Integer value in subtree '" + this.key + "'"));
    }

    // FIND LONG VALUE -------------------------------------------------------------

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under the given key and returns it as a {@link Long}. Any
     * {@link Number} subtype is accepted and coerced via
     * {@link Number#longValue()}.
     *
     * @param key the key to search for
     * @return an {@code Optional} containing the first matching value as a
     *         {@link Long}, or {@code Optional.empty()} if not found
     */
    public @NotNull Optional<Long> findLong(@NotNull String key) {
        return findNumber(key).map(Number::longValue);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under the given key and returns it as a {@link Long}, returning
     * the result of {@code defaultSupplier} if not found. Any {@link Number}
     * subtype is accepted and coerced via {@link Number#longValue()}.
     *
     * @param key             the key to search for
     * @param defaultSupplier supplier invoked to produce a fallback value
     * @return the first matching value as a {@link Long}, or the result of
     *         {@code defaultSupplier}
     */
    public @Nullable Long findLong(@NotNull String key, @NotNull Supplier<Long> defaultSupplier) {
        return findNumber(key).map(Number::longValue).orElseGet(defaultSupplier);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under the given key and casts it to an {@link Long}, throwing an
     * {@link IllegalArgumentException} if not found.
     * 
     * @param key the key to search for
     * @return the first matching {@link Number} value casted to an {@link Long}
     * @throws IllegalArgumentException if no {@link Number} value can be found with
     *                                  the given key
     */
    public @NotNull Long requireLong(@NotNull String key) throws IllegalArgumentException {
        return findLong(key).orElseThrow(() -> new IllegalArgumentException("The required Long value '" + key +
                "' could not be found, could not be casted, or was null for object '" + this.key + "'"));
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under the given key and casts it to an {@link Long}, throwing an
     * {@link IllegalArgumentException} from the given supplier if not found.
     * 
     * @param key               the key to search for
     * @param exceptionSupplier supplier of the exception to be thrown if no
     *                          {@link String} value can be found with the given key
     * @return the first matching {@link Number} value casted to an {@link Long}
     * @throws IllegalArgumentException if no {@link Number} value can be found with
     *                                  the given key, gotten from the given
     *                                  supplier
     */
    public @NotNull Long requireLong(@NotNull String key, @NotNull Supplier<IllegalArgumentException> exceptionSupplier)
            throws IllegalArgumentException {
        return findLong(key).orElseThrow(exceptionSupplier);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under any of the given keys, tried in the order provided, and returns
     * it as a {@link Long}. Any {@link Number} subtype is accepted and coerced via
     * {@link Number#longValue()}.
     *
     * @param keys the keys to search for, in priority order
     * @return an {@code Optional} containing the first matching value as a
     *         {@link Long}, or {@code Optional.empty()} if not found
     */
    public @NotNull Optional<Long> findLong(@NotNull String... keys) {
        return findNumber(keys).map(Number::longValue);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under any of the given keys and casts it to an {@link Long}, tried
     * in the order provided. Throws an {@link IllegalArgumentException} if no
     * {@link Number} can be found with the given keys.
     * 
     * @param keys the keys to search for, in priority order
     * @return the first matching {@link Number} value casted to an {@link Long}
     * @throws IllegalArgumentException if no {@link Number} value can be found with
     *                                  the given keys
     */
    public @NotNull Long requireLong(@NotNull String... keys) throws IllegalArgumentException {
        return requireLong(Arrays.asList(keys));
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under any of the given keys, tried in the order provided, and returns
     * it as a {@link Long}. Any {@link Number} subtype is accepted and coerced via
     * {@link Number#longValue()}.
     *
     * @param keys the keys to search for, in priority order
     * @return an {@code Optional} containing the first matching value as a
     *         {@link Long}, or {@code Optional.empty()} if not found
     */
    public @NotNull Optional<Long> findLong(@NotNull Collection<String> keys) {
        return findNumber(keys).map(Number::longValue);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under any of the given keys, tried in the order provided, and returns
     * it as a {@link Long}, returning the result of {@code defaultSupplier} if not
     * found. Any {@link Number} subtype is accepted and coerced via
     * {@link Number#longValue()}.
     *
     * @param keys            the keys to search for, in priority order
     * @param defaultSupplier supplier invoked to produce a fallback value
     * @return the first matching value as a {@link Long}, or the result of
     *         {@code defaultSupplier}
     */
    public @Nullable Long findLong(@NotNull Collection<String> keys, @NotNull Supplier<Long> defaultSupplier) {
        return findNumber(keys).map(Number::longValue).orElseGet(defaultSupplier);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under any of the given keys and casts it to an {@link Long}, tried
     * in the order provided. Throws an {@link IllegalArgumentException} if no
     * {@link Number} can be found with the given keys.
     * 
     * @param keys the keys to search for, in priority order
     * @return the first matching {@link Number} value casted to an {@link Long}
     * @throws IllegalArgumentException if no {@link Number} value can be found with
     *                                  the given keys
     */
    public @NotNull Long requireLong(@NotNull Collection<String> keys) throws IllegalArgumentException {
        return findLong(keys).orElseThrow(() -> new IllegalArgumentException("The keys, " + String.join("', '", keys)
                + "', one of which is required of type Long, are all missing, null, or could not be casted in subtree '"
                + this.key + "'"));
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under any of the given keys and casts it to an {@link Long}, tried
     * in the order provided. Throws an {@link IllegalArgumentException} from the
     * given supplier if no {@link Number} can be found with the given keys.
     * 
     * @param keys the keys to search for, in priority order
     * @return the first matching {@link Number} value casted to an {@link Long}
     * @throws IllegalArgumentException if no {@link Number} value can be found with
     *                                  the given keys, gotten from the given
     *                                  supplier
     */
    public @NotNull Long requireLong(@NotNull Collection<String> keys,
            @NotNull Supplier<IllegalArgumentException> exceptionSupplier) throws IllegalArgumentException {
        return findLong(keys).orElseThrow(exceptionSupplier);
    }

    /**
     * Resolves the key path and returns the value coerced to a {@link Long}.
     *
     * @param path the path segments or a dot-separated path sequence
     * @return an {@code Optional} containing the long value, or
     *         {@code Optional.empty()}
     */
    public @NotNull Optional<Long> findLongAt(@NotNull String... path) {
        return findNumberAt(path).map(Number::longValue);
    }

    /**
     * Resolves the key path and returns the value cast to an {@link Long},
     * throwing an {@link IllegalArgumentException} if the path is invalid or
     * unresolvable, or if the value is {@code null} or cannot be casted to an
     * {@link Long}.
     * 
     * @param path the path segments or a dot-separated path sequence
     * @return the {@link Long} value
     * @throws IllegalArgumentException if the path is invalid or unresolvable, or
     *                                  if the value is {@code null} or cannot be
     *                                  casted to an {@link Long}
     */
    public @NotNull Long requireLongAt(@NotNull String... path) {
        return findLongAt(path).orElseThrow(() -> new IllegalArgumentException("The path " + String.join(".", path)
                + " could not be resolved or resulted in a null or non-Long value in subtree '" + this.key + "'"));
    }

    // FIND DOUBLE VALUE -----------------------------------------------------------

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under the given key and returns it as a {@link Double}. Any
     * {@link Number} subtype is accepted and coerced via
     * {@link Number#doubleValue()}.
     *
     * @param key the key to search for
     * @return an {@code Optional} containing the first matching value as a
     *         {@link Double}, or {@code Optional.empty()} if not found
     */
    public @NotNull Optional<Double> findDouble(@NotNull String key) {
        return findNumber(key).map(Number::doubleValue);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under the given key and returns it as a {@link Double}, returning
     * the result of {@code defaultSupplier} if not found. Any {@link Number}
     * subtype is accepted and coerced via {@link Number#doubleValue()}.
     *
     * @param key             the key to search for
     * @param defaultSupplier supplier invoked to produce a fallback value
     * @return the first matching value as a {@link Double}, or the result of
     *         {@code defaultSupplier}
     */
    public @Nullable Double findDouble(@NotNull String key, @NotNull Supplier<Double> defaultSupplier) {
        return findNumber(key).map(Number::doubleValue).orElseGet(defaultSupplier);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under the given key and casts it to an {@link Double}, throwing an
     * {@link IllegalArgumentException} if not found.
     * 
     * @param key the key to search for
     * @return the first matching {@link Number} value casted to an {@link Double}
     * @throws IllegalArgumentException if no {@link Number} value can be found with
     *                                  the given key
     */
    public @NotNull Double requireDouble(@NotNull String key) throws IllegalArgumentException {
        return findDouble(key).orElseThrow(() -> new IllegalArgumentException("The required Double value '" + key +
                "' could not be found, could not be casted, or was null for object '" + this.key + "'"));
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under the given key and casts it to an {@link Double}, throwing an
     * {@link IllegalArgumentException} from the given supplier if not found.
     * 
     * @param key               the key to search for
     * @param exceptionSupplier supplier of the exception to be thrown if no
     *                          {@link String} value can be found with the given key
     * @return the first matching {@link Number} value casted to an {@link Double}
     * @throws IllegalArgumentException if no {@link Number} value can be found with
     *                                  the given key, gotten from the given
     *                                  supplier
     */
    public @NotNull Double requireDouble(@NotNull String key,
            @NotNull Supplier<IllegalArgumentException> exceptionSupplier) throws IllegalArgumentException {
        return findDouble(key).orElseThrow(exceptionSupplier);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under any of the given keys, tried in the order provided, and returns
     * it as a {@link Double}. Any {@link Number} subtype is accepted and coerced
     * via {@link Number#doubleValue()}.
     *
     * @param keys the keys to search for, in priority order
     * @return an {@code Optional} containing the first matching value as a
     *         {@link Double}, or {@code Optional.empty()} if not found
     */
    public @NotNull Optional<Double> findDouble(@NotNull String... keys) {
        return findNumber(keys).map(Number::doubleValue);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under any of the given keys and casts it to an {@link Double}, tried
     * in the order provided. Throws an {@link IllegalArgumentException} if no
     * {@link Number} can be found with the given keys.
     * 
     * @param keys the keys to search for, in priority order
     * @return the first matching {@link Number} value casted to an {@link Double}
     * @throws IllegalArgumentException if no {@link Number} value can be found with
     *                                  the given keys
     */
    public @NotNull Double requireDouble(@NotNull String... keys) throws IllegalArgumentException {
        return requireDouble(Arrays.asList(keys));
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under any of the given keys, tried in the order provided, and returns
     * it as a {@link Double}. Any {@link Number} subtype is accepted and coerced
     * via {@link Number#doubleValue()}.
     *
     * @param keys the keys to search for, in priority order
     * @return an {@code Optional} containing the first matching value as a
     *         {@link Double}, or {@code Optional.empty()} if not found
     */
    public @NotNull Optional<Double> findDouble(@NotNull Collection<String> keys) {
        return findNumber(keys).map(Number::doubleValue);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under any of the given keys, tried in the order provided, and returns
     * it as a {@link Double}, returning the result of {@code defaultSupplier} if
     * not found. Any {@link Number} subtype is accepted and coerced via
     * {@link Number#doubleValue()}.
     *
     * @param keys            the keys to search for, in priority order
     * @param defaultSupplier supplier invoked to produce a fallback value
     * @return the first matching value as a {@link Double}, or the result of
     *         {@code defaultSupplier}
     */
    public @Nullable Double findDouble(@NotNull Collection<String> keys, @NotNull Supplier<Double> defaultSupplier) {
        return findNumber(keys).map(Number::doubleValue).orElseGet(defaultSupplier);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under any of the given keys and casts it to an {@link Double}, tried
     * in the order provided. Throws an {@link IllegalArgumentException} if no
     * {@link Number} can be found with the given keys.
     * 
     * @param keys the keys to search for, in priority order
     * @return the first matching {@link Number} value casted to an {@link Double}
     * @throws IllegalArgumentException if no {@link Number} value can be found with
     *                                  the given keys
     */
    public @NotNull Double requireDouble(@NotNull Collection<String> keys) throws IllegalArgumentException {
        return findDouble(keys).orElseThrow(() -> new IllegalArgumentException("The keys, " + String.join("', '", keys)
                + "', one of which is required of type Double, are all missing, null, or could not be casted in subtree '"
                + this.key + "'"));
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under any of the given keys and casts it to an {@link Double}, tried
     * in the order provided. Throws an {@link IllegalArgumentException} from the
     * given supplier if no {@link Number} can be found with the given keys.
     * 
     * @param keys the keys to search for, in priority order
     * @return the first matching {@link Number} value casted to an {@link Double}
     * @throws IllegalArgumentException if no {@link Number} value can be found with
     *                                  the given keys, gotten from the given
     *                                  supplier
     */
    public @NotNull Double requireDouble(@NotNull Collection<String> keys,
            @NotNull Supplier<IllegalArgumentException> exceptionSupplier) throws IllegalArgumentException {
        return findDouble(keys).orElseThrow(exceptionSupplier);
    }

    /**
     * Resolves the key path and returns the value coerced to a {@link Double}.
     *
     * @param path the path segments or a dot-separated path sequence
     * @return an {@code Optional} containing the double value, or
     *         {@code Optional.empty()}
     */
    public @NotNull Optional<Double> findDoubleAt(@NotNull String... path) {
        return findNumberAt(path).map(Number::doubleValue);
    }

    /**
     * Resolves the key path and returns the value cast to an {@link Double},
     * throwing an {@link IllegalArgumentException} if the path is invalid or
     * unresolvable, or if the value is {@code null} or cannot be casted to an
     * {@link Double}.
     * 
     * @param path the path segments or a dot-separated path sequence
     * @return the {@link Double} value
     * @throws IllegalArgumentException if the path is invalid or unresolvable, or
     *                                  if the value is {@code null} or cannot be
     *                                  casted to an {@link Double}
     */
    public @NotNull Double requireDoubleAt(@NotNull String... path) {
        return findDoubleAt(path).orElseThrow(() -> new IllegalArgumentException("The path " + String.join(".", path)
                + " could not be resolved or resulted in a null or non-Double value in subtree '" + this.key + "'"));
    }

    // FIND FLOAT VALUE ------------------------------------------------------------

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under the given key and returns it as a {@link Float}. Any
     * {@link Number} subtype is accepted and coerced via
     * {@link Number#floatValue()}.
     *
     * @param key the key to search for
     * @return an {@code Optional} containing the first matching value as a
     *         {@link Float}, or {@code Optional.empty()} if not found
     */
    public @NotNull Optional<Float> findFloat(@NotNull String key) {
        return findNumber(key).map(Number::floatValue);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under the given key and returns it as a {@link Float}, returning
     * the result of {@code defaultSupplier} if not found. Any {@link Number}
     * subtype is accepted and coerced via {@link Number#floatValue()}.
     *
     * @param key             the key to search for
     * @param defaultSupplier supplier invoked to produce a fallback value
     * @return the first matching value as a {@link Float}, or the result of
     *         {@code defaultSupplier}
     */
    public @Nullable Float findFloat(@NotNull String key, @NotNull Supplier<Float> defaultSupplier) {
        return findNumber(key).map(Number::floatValue).orElseGet(defaultSupplier);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under the given key and casts it to an {@link Float}, throwing an
     * {@link IllegalArgumentException} if not found.
     * 
     * @param key the key to search for
     * @return the first matching {@link Number} value casted to an {@link Float}
     * @throws IllegalArgumentException if no {@link Number} value can be found with
     *                                  the given key
     */
    public @NotNull Float requireFloat(@NotNull String key) throws IllegalArgumentException {
        return findFloat(key).orElseThrow(() -> new IllegalArgumentException("The required Float value '" + key +
                "' could not be found, could not be casted, or was null for object '" + this.key + "'"));
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under the given key and casts it to an {@link Float}, throwing an
     * {@link IllegalArgumentException} from the given supplier if not found.
     * 
     * @param key               the key to search for
     * @param exceptionSupplier supplier of the exception to be thrown if no
     *                          {@link String} value can be found with the given key
     * @return the first matching {@link Number} value casted to an {@link Float}
     * @throws IllegalArgumentException if no {@link Number} value can be found with
     *                                  the given key, gotten from the given
     *                                  supplier
     */
    public @NotNull Float requireFloat(@NotNull String key,
            @NotNull Supplier<IllegalArgumentException> exceptionSupplier) throws IllegalArgumentException {
        return findFloat(key).orElseThrow(exceptionSupplier);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under any of the given keys, tried in the order provided, and returns
     * it as a {@link Float}. Any {@link Number} subtype is accepted and coerced
     * via {@link Number#floatValue()}.
     *
     * @param keys the keys to search for, in priority order
     * @return an {@code Optional} containing the first matching value as a
     *         {@link Float}, or {@code Optional.empty()} if not found
     */
    public @NotNull Optional<Float> findFloat(@NotNull String... keys) {
        return findNumber(keys).map(Number::floatValue);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under any of the given keys and casts it to an {@link Float}, tried
     * in the order provided. Throws an {@link IllegalArgumentException} if no
     * {@link Number} can be found with the given keys.
     * 
     * @param keys the keys to search for, in priority order
     * @return the first matching {@link Number} value casted to an {@link Float}
     * @throws IllegalArgumentException if no {@link Number} value can be found with
     *                                  the given keys
     */
    public @NotNull Float requireFloat(@NotNull String... keys) throws IllegalArgumentException {
        return requireFloat(Arrays.asList(keys));
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under any of the given keys, tried in the order provided, and returns
     * it as a {@link Float}. Any {@link Number} subtype is accepted and coerced
     * via {@link Number#floatValue()}.
     *
     * @param keys the keys to search for, in priority order
     * @return an {@code Optional} containing the first matching value as a
     *         {@link Float}, or {@code Optional.empty()} if not found
     */
    public @NotNull Optional<Float> findFloat(@NotNull Collection<String> keys) {
        return findNumber(keys).map(Number::floatValue);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under any of the given keys, tried in the order provided, and returns
     * it as a {@link Float}, returning the result of {@code defaultSupplier} if
     * not found. Any {@link Number} subtype is accepted and coerced via
     * {@link Number#floatValue()}.
     *
     * @param keys            the keys to search for, in priority order
     * @param defaultSupplier supplier invoked to produce a fallback value
     * @return the first matching value as a {@link Float}, or the result of
     *         {@code defaultSupplier}
     */
    public @Nullable Float findFloat(@NotNull Collection<String> keys, @NotNull Supplier<Float> defaultSupplier) {
        return findNumber(keys).map(Number::floatValue).orElseGet(defaultSupplier);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under any of the given keys and casts it to an {@link Float}, tried
     * in the order provided. Throws an {@link IllegalArgumentException} if no
     * {@link Number} can be found with the given keys.
     * 
     * @param keys the keys to search for, in priority order
     * @return the first matching {@link Number} value casted to an {@link Float}
     * @throws IllegalArgumentException if no {@link Number} value can be found with
     *                                  the given keys
     */
    public @NotNull Float requireFloat(@NotNull Collection<String> keys) throws IllegalArgumentException {
        return findFloat(keys).orElseThrow(() -> new IllegalArgumentException("The keys, " + String.join("', '", keys)
                + "', one of which is required of type Float, are all missing, null, or could not be casted in subtree '"
                + this.key + "'"));
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Number} value
     * stored under any of the given keys and casts it to an {@link Float}, tried
     * in the order provided. Throws an {@link IllegalArgumentException} from the
     * given supplier if no {@link Number} can be found with the given keys.
     * 
     * @param keys the keys to search for, in priority order
     * @return the first matching {@link Number} value casted to an {@link Float}
     * @throws IllegalArgumentException if no {@link Number} value can be found with
     *                                  the given keys, gotten from the given
     *                                  supplier
     */
    public @NotNull Float requireFloat(@NotNull Collection<String> keys,
            @NotNull Supplier<IllegalArgumentException> exceptionSupplier) throws IllegalArgumentException {
        return findFloat(keys).orElseThrow(exceptionSupplier);
    }

    /**
     * Resolves the key path and returns the value coerced to a {@link Float}.
     *
     * @param path the path segments or a dot-separated path sequence
     * @return an {@code Optional} containing the float value, or
     *         {@code Optional.empty()}
     */
    public @NotNull Optional<Float> findFloatAt(@NotNull String... path) {
        return findNumberAt(path).map(Number::floatValue);
    }

    /**
     * Resolves the key path and returns the value cast to an {@link Float},
     * throwing an {@link IllegalArgumentException} if the path is invalid or
     * unresolvable, or if the value is {@code null} or cannot be casted to an
     * {@link Float}.
     * 
     * @param path the path segments or a dot-separated path sequence
     * @return the {@link Float} value
     * @throws IllegalArgumentException if the path is invalid or unresolvable, or
     *                                  if the value is {@code null} or cannot be
     *                                  casted to an {@link Float}
     */
    public @NotNull Float requireFloatAt(@NotNull String... path) {
        return findFloatAt(path).orElseThrow(() -> new IllegalArgumentException("The path " + String.join(".", path)
                + " could not be resolved or resulted in a null or non-Float value in subtree '" + this.key + "'"));
    }

    // FIND BOOLEAN VALUE ----------------------------------------------------------

    /**
     * Searches the subtree rooted at this node for the first {@link Boolean} value
     * stored under the given key.
     *
     * @param key the key to search for
     * @return an {@code Optional} containing the first matching {@link Boolean}
     *         value, or {@code Optional.empty()} if not found
     */
    public @NotNull Optional<Boolean> findBoolean(@NotNull String key) {
        return find(Boolean.class, key);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Boolean} value
     * stored under the given key, returning the result of {@code defaultSupplier}
     * if not found.
     *
     * @param key             the key to search for
     * @param defaultSupplier supplier invoked to produce a fallback value
     * @return the first matching {@link Boolean} value, or the result of
     *         {@code defaultSupplier}
     */
    public @Nullable Boolean findBoolean(@NotNull String key, @NotNull Supplier<Boolean> defaultSupplier) {
        return find(Boolean.class, key, defaultSupplier);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Boolean} value
     * stored under the given key, throwing an {@link IllegalArgumentException} if
     * not found.
     * 
     * @param key the key to search for
     * @return the first matching {@link Boolean} value
     * @throws IllegalArgumentException if no {@link Boolean} value can be found
     *                                  with the given key
     */
    public @NotNull Boolean requireBoolean(@NotNull String key) throws IllegalArgumentException {
        return findBoolean(key).orElseThrow(() -> new IllegalArgumentException("The required Boolean value '" + key
                + "' could not be found, could not be casted, or was null for object '" + this.key + "'"));
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Boolean} value
     * stored under the given key, throwing an {@link IllegalArgumentException} from
     * the given supplier if not found.
     * 
     * @param key               the key to search for
     * @param exceptionSupplier supplier of the exception to be thrown if no
     *                          {@link Boolean} value can be found with the given
     *                          key
     * @return the first matching {@link Boolean} value
     * @throws IllegalArgumentException if no {@link Boolean} value can be found
     *                                  with
     *                                  the given key, gotten from the given
     *                                  supplier
     */
    public @NotNull Boolean requireBoolean(@NotNull String key,
            @NotNull Supplier<IllegalArgumentException> exceptionSupplier) throws IllegalArgumentException {
        return findBoolean(key).orElseThrow(exceptionSupplier);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Boolean} value
     * stored under any of the given keys, tried in the order provided.
     *
     * @param keys the keys to search for, in priority order
     * @return an {@code Optional} containing the first matching {@link Boolean}
     *         value, or {@code Optional.empty()} if not found
     */
    public @NotNull Optional<Boolean> findBoolean(@NotNull String... keys) {
        return find(Boolean.class, keys);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Boolean} value
     * stored under any of the given keys, tried in the order provided. Throws an
     * {@link IllegalArgumentException} if no {@link Boolean} can be found with the
     * given keys.
     * 
     * @param keys the keys to search for, in priority order
     * @return the first matching {@link Boolean} value
     * @throws IllegalArgumentException if no {@link Boolean} value can be found
     *                                  with
     *                                  the given keys
     */
    public @NotNull Boolean requireBoolean(@NotNull String... keys) throws IllegalArgumentException {
        return requireBoolean(Arrays.asList(keys));
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Boolean} value
     * stored under any of the given keys, tried in the order provided.
     *
     * @param keys the keys to search for, in priority order
     * @return an {@code Optional} containing the first matching {@link Boolean}
     *         value, or {@code Optional.empty()} if not found
     */
    public @NotNull Optional<Boolean> findBoolean(@NotNull Collection<String> keys) {
        return find(Boolean.class, keys);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Boolean} value
     * stored under any of the given keys, tried in the order provided, returning
     * the result of {@code defaultSupplier} if not found.
     *
     * @param keys            the keys to search for, in priority order
     * @param defaultSupplier supplier invoked to produce a fallback value
     * @return the first matching {@link Boolean} value, or the result of
     *         {@code defaultSupplier}
     */
    public @Nullable Boolean findBoolean(@NotNull Collection<String> keys, @NotNull Supplier<Boolean> defaultSupplier) {
        return find(Boolean.class, keys, defaultSupplier);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Boolean} value
     * stored under any of the given keys, tried in the order provided. Throws an
     * {@link IllegalArgumentException} if no {@link Boolean} can be found with the
     * given keys.
     * 
     * @param keys the keys to search for, in priority order
     * @return the first matching {@link Boolean} value
     * @throws IllegalArgumentException if no {@link Boolean} value can be found
     *                                  with
     *                                  the given keys
     */
    public @NotNull Boolean requireBoolean(@NotNull Collection<String> keys) throws IllegalArgumentException {
        return findBoolean(keys).orElseThrow(() -> new IllegalArgumentException("The keys, " + String.join("', '", keys)
                + "', one of which is required of type Boolean, are all missing, null, or could not be casted in subtree '"
                + this.key + "'"));
    }

    /**
     * Searches the subtree rooted at this node for the first {@link Boolean} value
     * stored under any of the given keys, tried in the order provided. Throws an
     * {@link IllegalArgumentException} from the given supplier if no
     * {@link Boolean}
     * can be found with the given keys.
     * 
     * @param keys              the keys to search for, in priority order
     * @param exceptionSupplier supplier of the exception to be thrown if no
     *                          {@link Boolean} value can be found with the given
     *                          keys
     * @return the first matching {@link Boolean} value
     * @throws IllegalArgumentException if no {@link Boolean} value can be found
     *                                  with
     *                                  the given keys
     */
    public @NotNull Boolean requireBoolean(@NotNull Collection<String> keys,
            @NotNull Supplier<IllegalArgumentException> exceptionSupplier) throws IllegalArgumentException {
        return findBoolean(keys).orElseThrow(exceptionSupplier);
    }

    /**
     * Resolves the key path and returns the value cast to a {@link Boolean}.
     *
     * @param path the path segments or a dot-separated path sequence
     * @return an {@code Optional} containing the boolean value, or
     *         {@code Optional.empty()}
     */
    public @NotNull Optional<Boolean> findBooleanAt(@NotNull String... path) {
        return findAt(path).filter(Boolean.class::isInstance).map(Boolean.class::cast);
    }

    /**
     * Resolves the key path and returns the value cast to a {@link Boolean},
     * throwing an {@link IllegalArgumentException} if the path is invalid or
     * unresolvable, or if the value is {@code null} or cannot be casted to a
     * {@link Boolean}.
     * 
     * @param path the path segments or a dot-separated path sequence
     * @return the {@link Boolean} value
     * @throws IllegalArgumentException if the path is invalid or unresolvable, or
     *                                  if the value is {@code null} or cannot be
     *                                  casted to a {@link Boolean}
     */
    public @NotNull Boolean requireBooleanAt(@NotNull String... path) {
        return findBooleanAt(path).orElseThrow(() -> new IllegalArgumentException("The path " + String.join(".", path)
                + " could not be resolved or resulted in a null or non-Boolean value in subtree '" + this.key + "'"));
    }

    // FIND JSON VALUE -------------------------------------------------------------

    /**
     * Searches the subtree rooted at this node for the first nested
     * {@link JSONObject} stored under the given key.
     *
     * @param key the key to search for
     * @return an {@code Optional} containing the first matching
     *         {@link JSONObject}, or {@code Optional.empty()} if not found
     */
    public @NotNull Optional<JSONObject> findJson(@NotNull String key) {
        return find(JSONObject.class, key);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link JSONObject}
     * value stored under the given key, returning the result of
     * {@code defaultSupplier} if not found.
     *
     * @param key             the key to search for
     * @param defaultSupplier supplier invoked to produce a fallback value
     * @return the first matching {@link JSONObject} value, or the result of
     *         {@code defaultSupplier}
     */
    public @Nullable JSONObject findJson(@NotNull String key, @NotNull Supplier<JSONObject> defaultSupplier) {
        return find(JSONObject.class, key, defaultSupplier);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link JSONObject}
     * value stored under the given key, throwing an
     * {@link IllegalArgumentException} if not found.
     * 
     * @param key the key to search for
     * @return the first matching {@link JSONObject} value
     * @throws IllegalArgumentException if no {@link JSONObject} value can be found
     *                                  with the given key
     */
    public @NotNull JSONObject requireJson(@NotNull String key) throws IllegalArgumentException {
        return findJson(key).orElseThrow(() -> new IllegalArgumentException("The required JSONObject value '" + key
                + "' could not be found, could not be casted, or was null for object '" + this.key + "'"));
    }

    /**
     * Searches the subtree rooted at this node for the first {@link JSONObject}
     * value stored under the given key, throwing an
     * {@link IllegalArgumentException} from the given supplier if not found.
     * 
     * @param key               the key to search for
     * @param exceptionSupplier supplier of the exception to be thrown if no
     *                          {@link JSONObject} value can be found with the given
     *                          key
     * @return the first matching {@link JSONObject} value
     * @throws IllegalArgumentException if no {@link JSONObject} value can be found
     *                                  with the given key, gotten from the given
     *                                  supplier
     */
    public @NotNull JSONObject requireJson(@NotNull String key,
            @NotNull Supplier<IllegalArgumentException> exceptionSupplier) throws IllegalArgumentException {
        return findJson(key).orElseThrow(exceptionSupplier);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link JSONObject}
     * value stored under any of the given keys, tried in the order provided.
     *
     * @param keys the keys to search for, in priority order
     * @return an {@code Optional} containing the first matching {@link JSONObject}
     *         value, or {@code Optional.empty()} if not found
     */
    public @NotNull Optional<JSONObject> findJson(@NotNull String... keys) {
        return find(JSONObject.class, Arrays.asList(keys));
    }

    /**
     * Searches the subtree rooted at this node for the first {@link JSONObject}
     * value
     * stored under any of the given keys, tried in the order provided. Throws an
     * {@link IllegalArgumentException} if no {@link JSONObject} can be found with
     * the given keys.
     * 
     * @param keys the keys to search for, in priority order
     * @return the first matching {@link JSONObject} value
     * @throws IllegalArgumentException if no {@link JSONObject} value can be found
     *                                  with the given keys
     */
    public @NotNull JSONObject requireJson(@NotNull String... keys) throws IllegalArgumentException {
        return requireJson(Arrays.asList(keys));
    }

    /**
     * Searches the subtree rooted at this node for the first {@link JSONObject}
     * value stored under any of the given keys, tried in the order provided.
     *
     * @param keys the keys to search for, in priority order
     * @return an {@code Optional} containing the first matching {@link JSONObject}
     *         value, or {@code Optional.empty()} if not found
     */
    public @NotNull Optional<JSONObject> findJson(@NotNull Collection<String> keys) {
        return find(JSONObject.class, keys);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link JSONObject}
     * value stored under any of the given keys, tried in the order provided,
     * returning the result of {@code defaultSupplier} if not found.
     *
     * @param keys            the keys to search for, in priority order
     * @param defaultSupplier supplier invoked to produce a fallback value
     * @return the first matching {@link JSONObject} value, or the result of
     *         {@code defaultSupplier}
     */
    public @Nullable JSONObject findJson(@NotNull Collection<String> keys,
            @NotNull Supplier<JSONObject> defaultSupplier) {
        return find(JSONObject.class, keys, defaultSupplier);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link JSONObject}
     * value
     * stored under any of the given keys, tried in the order provided. Throws an
     * {@link IllegalArgumentException} if no {@link JSONObject} can be found with
     * the
     * given keys.
     * 
     * @param keys the keys to search for, in priority order
     * @return the first matching {@link JSONObject} value
     * @throws IllegalArgumentException if no {@link JSONObject} value can be found
     *                                  with the given keys
     */
    public @NotNull JSONObject requireJson(@NotNull Collection<String> keys) throws IllegalArgumentException {
        return findJson(keys).orElseThrow(() -> new IllegalArgumentException("The keys, " + String.join("', '", keys)
                + "', one of which is required of type JSONObject, are all missing, null, or could not be casted in subtree '"
                + this.key + "'"));
    }

    /**
     * Searches the subtree rooted at this node for the first {@link JSONObject}
     * value
     * stored under any of the given keys, tried in the order provided. Throws an
     * {@link IllegalArgumentException} from the given supplier if no
     * {@link JSONObject}
     * can be found with the given keys.
     * 
     * @param keys              the keys to search for, in priority order
     * @param exceptionSupplier supplier of the exception to be thrown if no
     *                          {@link JSONObject} value can be found with the given
     *                          keys
     * @return the first matching {@link JSONObject} value
     * @throws IllegalArgumentException if no {@link JSONObject} value can be found
     *                                  with the given keys
     */
    public @NotNull JSONObject requireJson(@NotNull Collection<String> keys,
            @NotNull Supplier<IllegalArgumentException> exceptionSupplier) throws IllegalArgumentException {
        return findJson(keys).orElseThrow(exceptionSupplier);
    }

    /**
     * Resolves the key path and returns the value cast to a {@link JSONObject}.
     *
     * @param path the path segments or a dot-separated path sequence
     * @return an {@code Optional} containing the {@link JSONObject} value, or
     *         {@code Optional.empty()}
     */
    public @NotNull Optional<JSONObject> findJsonAt(@NotNull String... path) {
        return findAt(path).filter(JSONObject.class::isInstance).map(JSONObject.class::cast);
    }

    /**
     * Resolves the key path and returns the value cast to a {@link JSONObject},
     * throwing an {@link IllegalArgumentException} if the path is invalid or
     * unresolvable, or if the value is {@code null} or cannot be casted to a
     * {@link JSONObject}.
     * 
     * @param path the path segments or a dot-separated path sequence
     * @return the {@link JSONObject} value
     * @throws IllegalArgumentException if the path is invalid or unresolvable, or
     *                                  if the value is {@code null} or cannot be
     *                                  casted to a {@link JSONObject}
     */
    public @NotNull JSONObject requireJsonAt(@NotNull String... path) {
        return findJsonAt(path).orElseThrow(() -> new IllegalArgumentException("The path " + String.join(".", path)
                + " could not be resolved or resulted in a null or non-JSONObject value in subtree '" + this.key
                + "'"));
    }

    // FIND ARRAY VALUE ------------------------------------------------------------

    /**
     * Searches the subtree rooted at this node for the first {@link List} value
     * stored under the given key. The returned list may contain either
     * {@link JSONObject}s or primitive values; use {@link #isObject()} and
     * {@link #isArray()} on the parent node to distinguish these cases before
     * calling this method.
     *
     * <p>
     * Note: this method cannot delegate to the generic
     * {@link #find(Class, String)} infrastructure due to Java's raw-type
     * restrictions on {@code List.class}. It instead searches via
     * {@link #findAll(String)} and filters by {@code instanceof List<?>}.
     *
     * @param key the key to search for
     * @return an {@code Optional} containing the first matching {@link List},
     *         or {@code Optional.empty()} if not found
     */
    public @NotNull Optional<List<?>> findArray(@NotNull String key) {
        for (Object v : findAll(key)) {
            if (v instanceof List<?> list) {
                return Optional.of(list);
            }
        }
        return Optional.empty();
    }

    /**
     * Searches the subtree rooted at this node for the first {@link List} value
     * stored under the given key, returning the result of {@code defaultSupplier}
     * if not found.
     *
     * @param key             the key to search for
     * @param defaultSupplier supplier invoked to produce a fallback value
     * @return the first matching {@link List}, or the result of
     *         {@code defaultSupplier}
     */
    public @Nullable List<?> findArray(@NotNull String key, @NotNull Supplier<List<?>> defaultSupplier) {
        Optional<List<?>> res = findArray(key); // Must use findArray instead of find(Array.class) to avoid raw typing
        if (res.isPresent()) {
            return res.get();
        }
        return defaultSupplier.get();
    }

    /**
     * Searches the subtree rooted at this node for the first {@link List}
     * value stored under the given key, throwing an
     * {@link IllegalArgumentException} if not found.
     * 
     * @param key the key to search for
     * @return the first matching {@link List} value
     * @throws IllegalArgumentException if no {@link List} value can be found
     *                                  with the given key
     */
    public @NotNull List<?> requireArray(@NotNull String key) throws IllegalArgumentException {
        return findArray(key).orElseThrow(() -> new IllegalArgumentException("The required List value '" + key
                + "' could not be found, could not be casted, or was null for object '" + this.key + "'"));
    }

    /**
     * Searches the subtree rooted at this node for the first {@link List}
     * value stored under the given key, throwing an
     * {@link IllegalArgumentException} from the given supplier if not found.
     * 
     * @param key               the key to search for
     * @param exceptionSupplier supplier of the exception to be thrown if no
     *                          {@link List} value can be found with the given
     *                          key
     * @return the first matching {@link List} value
     * @throws IllegalArgumentException if no {@link List} value can be found
     *                                  with the given key, gotten from the given
     *                                  supplier
     */
    public @NotNull List<?> requireArray(@NotNull String key,
            @NotNull Supplier<IllegalArgumentException> exceptionSupplier) throws IllegalArgumentException {
        return findArray(key).orElseThrow(exceptionSupplier);
    }

    /**
     * Searches the subtree rooted at this node for the first {@link List} value
     * stored under any of the given keys, tried in the order provided.
     *
     * @param keys the keys to search for, in priority order
     * @return an {@code Optional} containing the first matching {@link List}
     *         value, or {@code Optional.empty()} if not found
     */
    public @NotNull Optional<List<?>> findArray(@NotNull String... keys) {
        return findArray(Arrays.asList(keys));
    }

    /**
     * Searches the subtree rooted at this node for the first {@link List}
     * value
     * stored under any of the given keys, tried in the order provided. Throws an
     * {@link IllegalArgumentException} if no {@link List} can be found with
     * the given keys.
     * 
     * @param keys the keys to search for, in priority order
     * @return the first matching {@link List} value
     * @throws IllegalArgumentException if no {@link List} value can be found
     *                                  with the given keys
     */
    public @NotNull List<?> requireArray(@NotNull String... keys) throws IllegalArgumentException {
        return requireArray(Arrays.asList(keys));
    }

    /**
     * Searches the subtree rooted at this node for the first {@link List} value
     * stored under any of the given keys, tried in the order provided.
     *
     * @param keys the keys to search for, in priority order
     * @return an {@code Optional} containing the first matching {@link List}
     *         value, or {@code Optional.empty()} if not found
     */
    public @NotNull Optional<List<?>> findArray(@NotNull Collection<String> keys) {
        for (String key : keys) {
            Optional<List<?>> res = findArray(key);
            if (res.isPresent()) {
                return res;
            }
        }
        return Optional.empty();
    }

    /**
     * Searches the subtree rooted at this node for the first {@link List} value
     * stored under any of the given keys, tried in the order provided, returning
     * the result of {@code defaultSupplier} if not found.
     *
     * @param keys            the keys to search for, in priority order
     * @param defaultSupplier supplier invoked to produce a fallback value
     * @return the first matching {@link List} value, or the result of
     *         {@code defaultSupplier}
     */
    public @Nullable List<?> findArray(@NotNull Collection<String> keys, @NotNull Supplier<List<?>> defaultSupplier) {
        Optional<List<?>> res = findArray(keys);
        if (res.isPresent()) {
            return res.get();
        }
        return defaultSupplier.get();
    }

    /**
     * Searches the subtree rooted at this node for the first {@link List}
     * value
     * stored under any of the given keys, tried in the order provided. Throws an
     * {@link IllegalArgumentException} if no {@link List} can be found with
     * the
     * given keys.
     * 
     * @param keys the keys to search for, in priority order
     * @return the first matching {@link List} value
     * @throws IllegalArgumentException if no {@link List} value can be found
     *                                  with the given keys
     */
    public @NotNull List<?> requireArray(@NotNull Collection<String> keys) throws IllegalArgumentException {
        return findArray(keys).orElseThrow(() -> new IllegalArgumentException("The keys, " + String.join("', '", keys)
                + "', one of which is required of type List, are all missing, null, or could not be casted in subtree '"
                + this.key + "'"));
    }

    /**
     * Searches the subtree rooted at this node for the first {@link List}
     * value
     * stored under any of the given keys, tried in the order provided. Throws an
     * {@link IllegalArgumentException} from the given supplier if no
     * {@link List}
     * can be found with the given keys.
     * 
     * @param keys              the keys to search for, in priority order
     * @param exceptionSupplier supplier of the exception to be thrown if no
     *                          {@link List} value can be found with the given
     *                          keys
     * @return the first matching {@link List} value
     * @throws IllegalArgumentException if no {@link List} value can be found
     *                                  with the given keys
     */
    public @NotNull List<?> requireArray(@NotNull Collection<String> keys,
            @NotNull Supplier<IllegalArgumentException> exceptionSupplier) throws IllegalArgumentException {
        return findArray(keys).orElseThrow(exceptionSupplier);
    }

    /**
     * Resolves the key path and returns the value cast to a {@link List}.
     *
     * @param path the path segments or a dot-separated path sequence
     * @return an {@code Optional} containing the array structure, or
     *         {@code Optional.empty()}
     */
    public @NotNull Optional<List<?>> findArrayAt(@NotNull String... path) {
        return findAt(path).filter(List.class::isInstance).map(v -> (List<?>) v);
    }

    /**
     * Resolves the key path and returns the value cast to a {@link List},
     * throwing an {@link IllegalArgumentException} if the path is invalid or
     * unresolvable, or if the value is {@code null} or cannot be casted to a
     * {@link List}.
     * 
     * @param path the path segments or a dot-separated path sequence
     * @return the {@link List} value
     * @throws IllegalArgumentException if the path is invalid or unresolvable, or
     *                                  if the value is {@code null} or cannot be
     *                                  casted to a {@link List}
     */
    public @NotNull List<?> requireArrayAt(@NotNull String... path) throws IllegalArgumentException {
        return findArrayAt(path).orElseThrow(() -> new IllegalArgumentException("The path " + String.join(".", path)
                + " could not be resolved or resulted in a null or non-List value in subtree '" + this.key
                + "'"));
    }

    // VALUE MUTATION --------------------------------------------------------------

    /**
     * Sets the value of this node. If the value is {@link JSONSerializable} or is a
     * list containing {@link JSONSerializable} elements, those will be converted to
     * JSONObjects using their {@link JSONSerializable#toJson()} method.
     *
     * @param value the new value; may be {@code null} or any valid JSON type
     */
    public void setValue(@Nullable Object value) {
        if (value == null) {
            this.value = null;
        } else if (value instanceof JSONSerializable serializable) {
            this.value = new ArrayList<>().add(serializable.toJson());
        } else if (value instanceof List<?> list) {
            List<Object> res = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof JSONSerializable serializable) {
                    res.add(serializable.toJson());
                } else {
                    res.add(item);
                }
            }
            this.value = res;
        } else {
            this.value = value;
        }
    }

    /**
     * Adds a {@link JSONObject} to the list stored as this node's value.
     *
     * @param addend the {@link JSONObject} to add
     * @return {@code true} if the list was modified; {@code false} otherwise (as
     *         per {@link List#add})
     * @throws IllegalStateException if this node's value is not a {@link List}, or
     *                               if the list contains any non-{@link JSONObject}
     *                               elements
     */
    public boolean add(@NotNull JSONObject addend) {
        if (!(value instanceof List<?> list)) {
            throw new IllegalStateException(
                    "Cannot add an inner object unless this JSONObject stores a List of JSONObjects.");
        }
        for (Object item : list) {
            if (!(item instanceof JSONObject)) {
                throw new IllegalStateException(
                        "Cannot add an inner object unless this JSONObject stores a List of JSONObjects.");
            }
        }
        @SuppressWarnings("unchecked")
        List<JSONObject> inner = (List<JSONObject>) value;
        return inner.add(addend);
    }

    /**
     * Adds one or more {@link JSONObject}s to the list stored as this node's value.
     * Equivalent to calling {@link #add(JSONObject)} for each element in order.
     *
     * @param addend the {@link JSONObject}s to add
     * @throws IllegalStateException if this node's value is not a {@link List} of
     *                               {@link JSONObject}s (as per
     *                               {@link #add(JSONObject)})
     */
    public void addAll(@NotNull JSONObject... addend) {
        Arrays.stream(addend).forEach(this::add);
    }

    /**
     * Associates the specified value with the specified key within this node.
     * If this node currently holds a {@code null} value, it is initialized
     * as an empty list to act natively as a JSON object container.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the value to be assigned to the key
     * @return the previous value mapped to this key, or {@code null} if there was
     *         no mapping
     * @throws IllegalStateException if this node stores a scalar type instead of an
     *                               object/list structure
     */
    public @Nullable Object put(@NotNull String key, @Nullable Object value) {
        if (this.value == null) {
            this.value = new ArrayList<>();
        }
        if (!(this.value instanceof List<?>)) {
            throw new IllegalStateException("Cannot update or put property into a scalar JSON node.");
        }
        @SuppressWarnings("unchecked")
        List<JSONObject> innerList = (List<JSONObject>) this.value;
        for (JSONObject child : innerList) {
            if (child.getKey().equals(key)) {
                Object previousValue = child.value;
                child.value = value;
                return previousValue;
            }
        }
        innerList.add(new JSONObject(key, value));
        return null;
    }

    /**
     * Associates the specified value with the specified key within this node only
     * if there is no current association with the given key, or the association is
     * {@code null}. If this node currently holds a {@code null} value, it is
     * initialized as an empty list to act natively as a JSON object container.
     * 
     * @param key   the key with which the specified value is to be associated
     * @param value the value to be assigned to the key, if there is no previous
     *              association
     * @return the value currently mapped to the key, if present and not
     *         {@code null}, or {@code null} if there was no previous mapping or the
     *         previous mapping was {@code null}
     * @throws IllegalStateException if this node stores a scalar type instead of an
     *                               object/list structure
     */
    public @Nullable Object putIfAbsent(@NotNull String key, @Nullable Object value) {
        if (this.value == null) {
            this.value = new ArrayList<>();
        }
        if (!(this.value instanceof List<?>)) {
            throw new IllegalStateException("Cannot update or put property into a scalar JSON node.");
        }
        @SuppressWarnings("unchecked")
        List<JSONObject> innerList = (List<JSONObject>) this.value;
        for (JSONObject child : innerList) {
            if (child.getKey().equals(key)) {
                if (child.getValue() == null) {
                    child.value = value;
                    return null;
                }
                return child.getValue();
            }
        }
        innerList.add(new JSONObject(key, value));
        return null;
    }

    /**
     * Evicts the mapping for the specified key from this node's object attributes
     * list.
     *
     * @param key the key whose property assignment is to be removed
     * @return {@code true} if a matching key was found and successfully unmapped;
     *         {@code false} otherwise (as per {@link List#add})
     */
    public boolean remove(@NotNull String key) {
        if (this.value instanceof List<?> list) {
            Iterator<?> it = list.iterator();
            while (it.hasNext()) {
                Object item = it.next();
                if (item instanceof JSONObject child && child.getKey().equals(key)) {
                    it.remove();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Adds one or more {@link JSONObject}s to this node's list value and returns
     * this node. Convenience method for chaining construction of a parent node:
     * 
     * <pre>{@code
     * JSONObject parent = new JSONObject("root").merge(child1, child2, child3);
     * }</pre>
     *
     * @param fields the {@link JSONObject}s to add to this node's list value
     * @return this {@link JSONObject}, after adding all fields
     * @throws IllegalStateException if this node's value is not a {@link List} of
     *                               {@link JSONObject}s (as per
     *                               {@link #add(JSONObject)})
     */
    public @NotNull JSONObject merge(@NotNull JSONObject... fields) {
        addAll(fields);
        return this;
    }

    // CONVERSION METHODS ----------------------------------------------------------

    /**
     * Deeply converts this {@link JSONObject} structure into a standard native Java
     * {@link Map}.
     *
     * @return a {@code Map<String, Object>} mirror representation of this object
     *         tree
     * @throws IllegalStateException if this node holds a primitive scalar value
     *                               instead of an object structure
     */
    public @NotNull Map<String, Object> toMap() {
        if (this.value == null) {
            return new LinkedHashMap<>();
        }
        if (this.value instanceof List<?> list) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Object item : list) {
                if (item instanceof JSONObject child) {
                    map.put(child.getKey(), child.toJavaStructure());
                } else {
                    throw new IllegalStateException("Node contains a primitive list, cannot map to Key-Value Map.");
                }
            }
            return map;
        }
        throw new IllegalStateException("Node holds a scalar value, cannot map to Key-Value Map.");
    }

    /**
     * Deeply converts this node's array structures into standard native Java
     * {@link List} types.
     *
     * @return a {@code List<Object>} mirror representation of this JSON array
     *         structure
     * @throws IllegalStateException if this node does not possess an array or list
     *                               assignment
     */
    public @NotNull List<Object> toList() {
        if (this.value instanceof List<?> list) {
            List<Object> converted = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof JSONObject child) {
                    converted.add(child.toJavaStructure());
                } else {
                    converted.add(item);
                }
            }
            return converted;
        }
        throw new IllegalStateException("Node does not hold a valid list or array assignment.");
    }

    /**
     * Recursive structural converter utility that translates nested tokens into
     * core Java frameworks.
     */
    private @Nullable Object toJavaStructure() {
        if (this.value == null) {
            return null;
        }
        if (isObject()) {
            return toMap();
        }
        if (isArray() || this.value instanceof List<?>) {
            return toList();
        }
        return this.value;
    }

    /**
     * Applies the given funtion to this node and returns the result.
     * 
     * @param <T>      the target type after applying the given function
     * @param function function which accepts this {@link JSONObject} and returns
     *                 with the desired type
     * @return the result of applying this node to the given function
     */
    public <T> T toObject(@NotNull Function<JSONObject, T> function) {
        return function.apply(this);
    }

    // ITERABLE METHODS ------------------------------------------------------------

    /**
     * Performs an inorder traversal of the subtree rooted at this node, appending
     * each leaf value to {@code result}. The traversal visits nodes in the order
     * they appear in the JSON structure, depth-first.
     * <p>
     * Leaf values appended are the raw values of scalar nodes: {@link String},
     * {@link Number}, {@link Boolean}, or {@code null}. {@link JSONObject} nodes
     * are descended into rather than appended directly.
     *
     * @param result the list to append traversed values to; must not be
     *               {@code null}
     */
    private void inorderTraversal(@NotNull List<Object> result) {
        if (value == null) {
            result.add(null);
        } else if (value instanceof JSONObject valueJson) {
            valueJson.inorderTraversal(result);
        } else if (value instanceof List<?> valueList) {
            for (Object item : valueList) {
                if (item instanceof JSONObject itemJson) {
                    itemJson.inorderTraversal(result);
                } else {
                    result.add(item);
                }
            }
        } else {
            result.add(value);
        }
    }

    /**
     * Returns an {@link Iterator} over the leaf values of this node's subtree in
     * inorder traversal. Each element is a scalar value ({@link String},
     * {@link Number}, {@link Boolean}, or {@code null}); {@link JSONObject} nodes
     * are descended into rather than yielded directly.
     * <p>
     * The iterator does not support {@link Iterator#remove()}.
     *
     * @return an iterator over the inorder traversal of this subtree's leaf values
     */
    @Override
    public @NotNull Iterator<Object> iterator() {
        List<Object> traversal = new ArrayList<>();
        inorderTraversal(traversal);
        return traversal.iterator();
    }

    /**
     * Returns an {@link Iterator} over the direct {@link JSONObject} children of
     * this node in order.
     * <p>
     * The iterator does not support {@link Iterator#remove()}.
     * 
     * @return an iterator over the ordered {@link JSONObject} children of this node
     */
    public @NotNull Iterator<JSONObject> childIterator() {
        List<JSONObject> children = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof JSONObject child) {
                    children.add(child);
                }
            }
        }
        return children.iterator();
    }

    /**
     * Returns a {@link Stream} over the leaf values of this node's subtree in
     * inorder traversal. Each element is a scalar value ({@link String},
     * {@link Number}, {@link Boolean}, or {@code null}); {@link JSONObject} nodes
     * are descended into rather than yielded directly.
     * 
     * @return a stream over the inorder traversal of this subtree's leaf values
     */
    public Stream<Object> stream() {
        Spliterator<Object> spliterator = Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED);
        return StreamSupport.stream(spliterator, false);
    }

    /**
     * Returns a {@link Stream} over the direct {@link JSONObject} children of
     * this node in order.
     * 
     * @return a stream over the ordered {@link JSONObject} children of this node
     */
    public Stream<JSONObject> childStream() {
        Spliterator<JSONObject> spliterator = Spliterators.spliteratorUnknownSize(childIterator(), Spliterator.ORDERED);
        return StreamSupport.stream(spliterator, false);
    }

    // CLONEABLE METHODS -----------------------------------------------------------

    /**
     * Returns a deep copy of this node. The cloned node has the same key and an
     * independently copied value:
     * <ul>
     * <li>If the value is a {@link JSONObject}, it is recursively cloned.</li>
     * <li>If the value is a {@link List}, a new list is created and any
     * {@link JSONObject} elements within it are recursively cloned. Primitive
     * elements ({@link String}, {@link Number}, {@link Boolean}) are shared
     * between the original and the clone, as they are immutable.</li>
     * <li>Scalar values ({@link String}, {@link Number}, {@link Boolean},
     * {@code null}) are shared, as they are immutable.</li>
     * </ul>
     *
     * @return a deep copy of this {@link JSONObject}
     */
    @Override
    public @NotNull JSONObject clone() {
        try {
            JSONObject cloned = (JSONObject) super.clone();
            if (value instanceof JSONObject valueJson) {
                cloned.value = valueJson.clone();
            } else if (value instanceof List<?> valueList) {
                List<Object> deepList = new ArrayList<>();
                for (Object item : valueList) {
                    if (item instanceof JSONObject itemJson) {
                        deepList.add(itemJson.clone());
                    } else {
                        deepList.add(item);
                    }
                }
                cloned.value = deepList;
            }
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError("Unreachable", e);
        }
    }

    // OBJECT METHODS --------------------------------------------------------------

    /**
     * Returns a JSON string representation of this node and its entire subtree. The
     * output is formatted with newline-separated lines and is equivalent to the
     * JSON source that would produce this structure if parsed.
     *
     * @return a formatted JSON string representation of this node
     */
    @Override
    public @NotNull String toString() {
        return String.join("\n", JSONStringifier.expand(JSONStringifier.stringify(this)));
    }

    /**
     * Returns a hash code based on this node's string representation. Two
     * {@link JSONObject}s that are {@link #equals(Object) equal} will have the same
     * hash code.
     *
     * @return hash code derived from {@link #toString()}
     */
    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    /**
     * Returns {@code true} if {@code other} is a {@link JSONObject} with the same
     * string representation as this node. Equality is structural: two nodes are
     * equal if and only if their JSON representations are identical, meaning the
     * same keys, values, and nesting in the same order.
     *
     * @param other the object to compare to
     * @return {@code true} if {@code other} is a {@link JSONObject} with an
     *         identical string representation; {@code false} otherwise
     */
    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return this.toString().equals(((JSONObject) other).toString());
    }

}
