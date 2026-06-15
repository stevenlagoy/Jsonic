package com.stevenlagoy.jsonic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * A JSONObject is a custom data structure that represents a JSON object. It
 * supports nested key-value pairs, arrays (as
 * Lists), and primitive values such as strings, numbers, booleans, and null.
 * This class provides methods for type
 * casting, searching, and string representation.
 */
public class JSONObject implements Iterable<Object> {

    /*
     * Valid JSON types include:
     * String:
     * "String"
     * ""
     * Any characters inside double quotes.
     * In Java, instance of java.lang.String
     * Number:
     * 10
     * 4.2
     * 1.5E2
     * Integer or Floating Point number, consisting of digits, at most one decimal
     * place, and possibly E/e followed by an exponent.
     * In Java, instance of java.lang.Number (Integer, Long, Float, Double)
     * Object:
     * {"key1" : "value", "key2 : "value"}
     * {}
     * A set of key-value pairs separated by commas inside curly braces.
     * In Java, instance of core.JSONObject
     * Array:
     * ["value", 10, ...]
     * []
     * A list of bare values separated by commas inside square braces.
     * In Java, instance of java.util.List<?>
     * Boolean:
     * true
     * false
     * Either true or false.
     * In Java, instance of java.lang.Boolean
     * Null:
     * null
     * Represents nothing.
     * In Java, null
     */

    /**
     * Checks whether an Object value is a valid JSON type: {@code String},
     * {@code Number}, {@code JSONObject},
     * {@code Array}, {@code Boolean}, or {@code null}.
     *
     * @param value
     *              The Object to check the type of.
     *
     * @return {@code true} if the Object is of a valid JSON type, otherwise
     *         {@code false}
     */
    private static boolean isValidJsonType(Object value) {
        return (value == null ||
                value instanceof String ||
                value instanceof Number ||
                value instanceof JSONObject ||
                value instanceof List<?> ||
                value instanceof Boolean ||
                value instanceof Object);
    }

    private String key;
    private Object value;
    private Class<? extends Object> type;

    /**
     * Create an empty JSONObject instance with a key of {@code ""} and a value of
     * {@code null}.
     */
    public JSONObject() {
        this.key = "";
        this.value = null;
        this.type = null;
    }

    /**
     * Create a {@code JSONObject} with the given key and a value of {@code null}.
     *
     * @param key
     *            A {@code String} key
     */
    public JSONObject(String key) {
        this.key = key;
        this.value = null;
        this.type = null;
    }

    /**
     * Create a {@code JSONObject} with the given key and value.
     *
     * @param key   Valid {@code String} key for JSON Object
     * @param value Valid JSON value ({@code String}, {@code Number},
     *              {@code JSONObject}, {@code List<>}, {@code Boolean},
     *              {@code Object extends Jsonic})
     */
    public JSONObject(String key, Object value) {
        if (!isValidJsonType(value)) {
            throw new IllegalArgumentException("Invalid JSON value type.");
        }
        this.key = key;
        this.value = value;
        if (value == null)
            type = null;
        else if (value instanceof List<?>) {
            if (isJSONList(getAsList()))
                type = JSONObject.class;
            else
                type = ArrayList.class;
        } else
            type = value.getClass();
    }

    /**
     * Create a new JSONObject with the given key, value, and type
     * 
     * @param key   Key for the new JSONObject
     * @param value Value for this key
     * @param type  Type of the value. Should match value's actual type.
     */
    public JSONObject(String key, Object value, Class<? extends Object> type) {
        this.key = key;
        this.value = value;
        this.type = type;
    }

    /**
     * Determine whether a list contains only JSONObjects
     * 
     * @param list List to evaluate
     * @return {@code true} if the list contains only JSONObjects, {@code false}
     *         otherwise
     */
    private boolean isJSONList(List<?> list) {
        for (Object o : list)
            if (!(o instanceof JSONObject))
                return false;
        return true;
    }

    /**
     * Get the key of this JSONObject
     * 
     * @return Key of this JSONObject
     */
    public String getKey() {
        return key;
    }

    /**
     * Set the key of this JSONObject
     * 
     * @param key New key for this JSONObject
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Get the value, as an Object, of this JSONObject
     * 
     * @return The value of this JSONObject
     * @see #get(String, Class)
     */
    public Object getValue() {
        return value;
    }

    /**
     * Return the value as the given class.
     *
     * @param clazz Existant class to cast this object's value into.
     * @param <T>   Any type
     * @return Value as a type of the given class.
     * @throws ClassCastException if the value is not null and is not assignable to
     *                            the type T.
     */
    public <T> T getValueAs(Class<T> clazz) throws ClassCastException {
        return clazz.cast(value);
    }

    /**
     * Returns the value of this JSONObject as a String, or throws a
     * ClassCastException if unable.
     * 
     * @return Value of this JSONObject as a String
     */
    public String getAsString() {
        if (value instanceof String)
            return (String) value;
        else
            throw new ClassCastException("Cannot cast non-String value to String.");
    }

    /**
     * Returns the value of this JSONObject as a Number, or throws a
     * ClassCastException if unable.
     * 
     * @return Value of this JSONObject as a Number
     */
    public Number getAsNumber() {
        if (value instanceof Number)
            return (Number) value;
        else
            throw new ClassCastException("Cannot cast non-Number value to Number.");
    }

    /**
     * Returns the value of this JSONObject as a Boolean, or throws a
     * ClassCastException if unable.
     * 
     * @return Value of this JSONObject as a Boolean
     */
    public Boolean getAsBoolean() {
        if (value instanceof Boolean)
            return (Boolean) value;
        else
            throw new ClassCastException("Cannot cast non-Boolean value to Boolean.");
    }

    /**
     * Returns the value of this JSONObject as another JSONObject, or {@code null}
     * if unable.
     * 
     * @return Value of this JSONObject as a JSONObject
     */
    public JSONObject getAsObject() {

        return this;

        // if (value instanceof JSONObject) return (JSONObject) value;

        // if (value instanceof List<?> && type.equals(JSONObject.class)) {
        // return this;
        // }

        // return null;
    }

    /**
     * Returns the value of this JSONObject as a List, or throws a
     * ClassCastException if unable.
     * 
     * @return Value of this JSONObject as a List
     */
    public List<?> getAsList() {
        if (value instanceof List<?>)
            return (List<?>) value;
        else
            throw new ClassCastException("Cannot cast non-List value to List.");
    }

    /**
     * Set the value of this JSONObject
     * 
     * @param value New value of this JSONObject
     */
    public void setValue(Object value) {
        this.value = value;
        if (value instanceof List<?> && !((List<?>) value).isEmpty()) {
            // For non-empty lists, store the type of the first element
            Object first = ((List<?>) value).get(0);
            if (first instanceof JSONObject) {
                this.type = JSONObject.class;
            } else {
                this.type = value.getClass();
            }
        } else {
            this.type = value != null ? value.getClass() : null;
        }

    }

    /**
     * Set the value of this JSONObject with a certain type
     * 
     * @param value New value of this JSONObject
     * @param type  Type of the value. Should match the value's class
     */
    public void setValue(Object value, Class<? extends Object> type) {
        this.value = value;
        // Commented out because it is possible to have a mixed list
        // Check that the type is correct
        // if (value instanceof List<?>) {
        // List<?> list = (List<?>) value;
        // if (!list.isEmpty()) {
        // Object first = list.get(0);
        // if (first.getClass() != type)
        // throw new InvalidParameterException("The given type, " + type.getName() + ",
        // does not match the actual type of the list entries, which is " +
        // first.getClass().getName());
        // }
        // }
        this.type = type;
    }

    /**
     * Returns the type of the inner object, even if technically empty or null.
     * 
     * @return Type of this JSONObject's value
     */
    public Class<? extends Object> getType() {
        return type;
    }

    /**
     * Search the JSONObject tree structure for a JSONObject with the given key, and
     * return the value of that
     * JSONObject.
     *
     * @param key String key to search for within the tree structure.
     *
     * @return Value of the JSONObject with the given key, or null if unfound.
     */
    public Object get(String key) {
        if (this.key.equals(key))
            return this.value;

        if (value instanceof JSONObject)
            return getAsObject().get(key);

        if (value instanceof List<?>) {
            for (Object item : getAsList()) {
                if (item instanceof JSONObject) {
                    Object result = ((JSONObject) item).get(key);
                    if (result != null)
                        return result;
                }
            }
        }
        return null;
    }

    /**
     * Get a value from the JSONObject tree as the specified type.
     * Performs implicit type conversion where reasonable.
     *
     * @param <T>  Any type
     * @param key  The key to search for
     * @param type The desired type class
     * @return The value converted to the specified type, or null if not found
     * @throws ClassCastException if the value cannot be converted to the specified
     *                            type
     */
    public <T> T get(String key, Class<T> type) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        return convertToType(value, type);
    }

    /**
     * Get a value from the JSONObject tree as an Optional of the specified type.
     * This provides safe retrieval without exception throwing.
     *
     * @param <T>  Any type
     * @param key  The key to search for
     * @param type The desired type class
     * @return An Optional containing the converted value, or empty if not found or
     *         conversion fails
     */
    public <T> Optional<T> getOptional(String key, Class<T> type) {
        try {
            return Optional.ofNullable(get(key, type));
        } catch (ClassCastException e) {
            return Optional.empty();
        }
    }

    /**
     * Get a value from the JSONObject tree, with a fallback default value.
     * Returns the default if the key is not found or type conversion fails.
     *
     * @param <T>          Any type
     * @param key          The key to search for
     * @param type         The desired type class
     * @param defaultValue The value to return if key is not found or conversion
     *                     fails
     * @return The converted value or the default value
     */
    public <T> T getWithDefault(String key, Class<T> type, T defaultValue) {
        try {
            T result = get(key, type);
            return result != null ? result : defaultValue;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    /**
     * Convenience method to get a String value with a default fallback.
     *
     * @param key          The key to search for
     * @param defaultValue The value to return if key is not found or is not a
     *                     String
     * @return The string value or the default value
     */
    public String getString(String key, String defaultValue) {
        return getWithDefault(key, String.class, defaultValue);
    }

    /**
     * Convenience method to get a String value with null as fallback.
     *
     * @param key The key to search for
     * @return The string value or null if not found or not a String
     */
    public String getString(String key) {
        return getString(key, null);
    }

    /**
     * Convenience method to get an Integer value with a default fallback.
     * Supports conversion from Number types.
     *
     * @param key          The key to search for
     * @param defaultValue The value to return if key is not found or cannot be
     *                     converted to int
     * @return The integer value or the default value
     */
    public Integer getInt(String key, Integer defaultValue) {
        try {
            Object value = get(key);
            if (value == null) {
                return defaultValue;
            }
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            if (value instanceof String) {
                try {
                    return Integer.parseInt((String) value);
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
            return defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Convenience method to get an Integer value with null as fallback.
     *
     * @param key The key to search for
     * @return The integer value or null if not found or cannot be converted
     */
    public Integer getInt(String key) {
        return getInt(key, null);
    }

    /**
     * Convenience method to get a Double value with a default fallback.
     * Supports conversion from Number types.
     *
     * @param key          The key to search for
     * @param defaultValue The value to return if key is not found or cannot be
     *                     converted to double
     * @return The double value or the default value
     */
    public Double getDouble(String key, Double defaultValue) {
        try {
            Object value = get(key);
            if (value == null) {
                return defaultValue;
            }
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            if (value instanceof String) {
                try {
                    return Double.parseDouble((String) value);
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
            return defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Convenience method to get a Double value with null as fallback.
     *
     * @param key The key to search for
     * @return The double value or null if not found or cannot be converted
     */
    public Double getDouble(String key) {
        return getDouble(key, null);
    }

    /**
     * Convenience method to get a Long value with a default fallback.
     * Supports conversion from Number types.
     *
     * @param key          The key to search for
     * @param defaultValue The value to return if key is not found or cannot be
     *                     converted to long
     * @return The long value or the default value
     */
    public Long getLong(String key, Long defaultValue) {
        try {
            Object value = get(key);
            if (value == null) {
                return defaultValue;
            }
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            if (value instanceof String) {
                try {
                    return Long.parseLong((String) value);
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
            return defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Convenience method to get a Long value with null as fallback.
     *
     * @param key The key to search for
     * @return The long value or null if not found or cannot be converted
     */
    public Long getLong(String key) {
        return getLong(key, null);
    }

    /**
     * Convenience method to get a Boolean value with a default fallback.
     * Supports conversion from String ("true"/"false", case-insensitive).
     *
     * @param key          The key to search for
     * @param defaultValue The value to return if key is not found or cannot be
     *                     converted to boolean
     * @return The boolean value or the default value
     */
    public Boolean getBoolean(String key, Boolean defaultValue) {
        try {
            Object value = get(key);
            if (value == null) {
                return defaultValue;
            }
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            if (value instanceof String) {
                String str = ((String) value).toLowerCase();
                if ("true".equals(str)) {
                    return true;
                } else if ("false".equals(str)) {
                    return false;
                }
                return defaultValue;
            }
            return defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Convenience method to get a Boolean value with null as fallback.
     *
     * @param key The key to search for
     * @return The boolean value or null if not found or cannot be converted
     */
    public Boolean getBoolean(String key) {
        return getBoolean(key, null);
    }

    /**
     * Convenience method to get a nested JSONObject with the given key.
     *
     * @param key The key to search for
     * @return The JSONObject if found and is an object, or a new empty JSONObject
     *         otherwise
     */
    public JSONObject getObject(String key) {
        Object value = get(key);
        if (value instanceof JSONObject) {
            return (JSONObject) value;
        }
        if (value instanceof List<?>) {
            List<?> list = (List<?>) value;
            if (!list.isEmpty() && list.get(0) instanceof JSONObject) {
                return (JSONObject) list.get(0);
            }
        }
        return new JSONObject(key);
    }

    /**
     * Get a List with elements converted to the specified type.
     * Attempts to convert each element to the specified type.
     *
     * @param <T>         Any type
     * @param key         The key to search for
     * @param elementType The desired element type
     * @return A list of converted elements, or empty list if not found
     */
    public <T> List<T> getListAs(String key, Class<T> elementType) {
        Object value = get(key);
        if (value == null) {
            return new ArrayList<>();
        }

        if (value instanceof List<?>) {
            List<?> list = (List<?>) value;
            List<T> result = new ArrayList<>();
            for (Object item : list) {
                try {
                    result.add(convertToType(item, elementType));
                } catch (ClassCastException e) {
                    // Skip items that cannot be converted
                }
            }
            return result;
        }

        return new ArrayList<>();
    }

    /**
     * Add a JSONObject to the value of this JSONObject, if the value is a
     * {@code List<JSONObject>}
     * 
     * @param added JSONObject to add to the value
     * @throws IllegalStateException When the inner type of this JSONObject is not
     *                               {@code List<JSONObject>}
     */
    public void add(JSONObject added) {
        if (!(value instanceof List<?> list))
            throw new IllegalStateException(
                    "Cannot add an inner object unless this JSONObject stores a List<JSONObject>.");

        for (Object item : list) {
            if (!(item instanceof JSONObject)) {
                throw new IllegalStateException("Cannot add to a JSONObject with a mixed list value.");
            }
        }

        @SuppressWarnings("unchecked")
        List<JSONObject> objects = (List<JSONObject>) list;
        objects.add(added);
    }

    /**
     * Add JSONObjects to the value of this JSONObject, if the value is a
     * {@code List<JSONObject>}
     * 
     * @param added JSONObjects to add to the value
     * @throws IllegalStateException When the inner type of this JSONObject is not
     *                               {@code List<JSONObject>}
     */
    public void addAll(JSONObject... added) {
        Arrays.stream(added).forEach(this::add);
    }

    /**
     * Merge this JSONObject with others and return this JSONObject as the root.
     * 
     * @param fields JSONObjects to add to this JSONObject's value
     * @return This JSONObject, which will contain the passed fields in its value.
     */
    public JSONObject merge(JSONObject... fields) {
        addAll(fields);
        return this;
    }

    /**
     * Internal helper method to convert an object to the specified type.
     * Supports implicit type conversions where reasonable.
     *
     * @param value The value to convert
     * @param type  The target type class
     * @return The converted value
     * @throws ClassCastException if conversion is not possible
     */
    @SuppressWarnings("unchecked")
    private <T> T convertToType(Object value, Class<T> type) throws ClassCastException {
        if (value == null) {
            return null;
        }

        // Direct type match
        if (type.isInstance(value)) {
            return (T) value;
        }

        // String conversions
        if (type == String.class) {
            return (T) String.valueOf(value);
        }

        // Number conversions
        if (type == Integer.class && value instanceof Number) {
            return (T) Integer.valueOf(((Number) value).intValue());
        }
        if (type == Long.class && value instanceof Number) {
            return (T) Long.valueOf(((Number) value).longValue());
        }
        if (type == Double.class && value instanceof Number) {
            return (T) Double.valueOf(((Number) value).doubleValue());
        }
        if (type == Float.class && value instanceof Number) {
            return (T) Float.valueOf(((Number) value).floatValue());
        }
        if (type == Number.class && value instanceof Number) {
            return (T) value;
        }

        // String to Number conversions
        if (value instanceof String) {
            String str = (String) value;
            try {
                if (type == Integer.class) {
                    return (T) Integer.valueOf(str);
                }
                if (type == Long.class) {
                    return (T) Long.valueOf(str);
                }
                if (type == Double.class) {
                    return (T) Double.valueOf(str);
                }
                if (type == Float.class) {
                    return (T) Float.valueOf(str);
                }
                if (type == Number.class) {
                    try {
                        return (T) Integer.valueOf(str);
                    } catch (NumberFormatException e1) {
                        return (T) Double.valueOf(str);
                    }
                }
            } catch (NumberFormatException e) {
                throw new ClassCastException("Cannot convert string '" + str + "' to " + type.getName());
            }
        }

        // Boolean conversions
        if (type == Boolean.class) {
            if (value instanceof String) {
                String str = ((String) value).toLowerCase();
                if ("true".equals(str)) {
                    return (T) Boolean.TRUE;
                }
                if ("false".equals(str)) {
                    return (T) Boolean.FALSE;
                }
            }
        }

        // If no conversion is possible, throw exception
        throw new ClassCastException("Cannot convert " + value.getClass().getName() + " to " + type.getName());
    }

    /**
     * Get the native type of the inner value of this JSONObject.
     *
     * @return Class of the value (String, Number, JSONObject, List&lt;?&gt;,
     *         Boolean, or
     *         null)
     */
    public Class<?> getInnerType() {
        return value.getClass();
    }

    // ITERATOR METHODS -------------------------------------------------------

    /**
     * Traverse the tree structure of this JSONObject inorder and return an
     * indexable list of the objects' values. Start
     * with the leftmost value, then the root value, then the rightmost value,
     * recursively.
     *
     * @param result List&lt;Object&gt; to be populated with the values from the
     *               inorder
     *               traversal.
     */
    public void inorderTraversal(List<Object> result) {
        if (value == null) {
            result.add(null);
        } else if (value instanceof JSONObject) {
            getAsObject().inorderTraversal(result);
        } else if (value instanceof List<?>) {
            for (Object item : getAsList()) {
                if (item instanceof JSONObject) {
                    ((JSONObject) item).inorderTraversal(result);
                } else
                    result.add(item);
            }
        } else
            result.add(value);
    }

    /**
     * Iterate through the tree structure of this JSONObject inorder
     * 
     * @return Iterator over the value of this JSONObject
     */
    @Override
    public Iterator<Object> iterator() {
        List<Object> traversal = new ArrayList<>();
        inorderTraversal(traversal);
        return traversal.iterator();
    }

    // Stringify methods

    /**
     * Turn this JSONObject into a String representation. Will result in a
     * functionally identical String to the theoretical JSON
     * file which was parsed to create this JSONObject.
     *
     * @return String representation of this JSONObject
     */
    @Override
    public String toString() {
        return String.join("\n", JSONStringifier.expandJson(JSONStringifier.stringifyJson(this)));
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    /**
     * Determine whether this JSONObect is equal to the other JSONObject by
     * comparing their String representations.
     *
     * @param other JSONObject with which to compare this JSONObject
     *
     * @return True if the String representations are the same, False otherwise
     *
     * @see JSONObject#toString()
     */
    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (other == null || getClass() != other.getClass())
            return false;
        return this.toString().equals(((JSONObject) other).toString());
    }

}