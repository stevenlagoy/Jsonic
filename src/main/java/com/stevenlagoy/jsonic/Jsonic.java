package com.stevenlagoy.jsonic;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Interface allowing Json methods to be used to create and store JSONObjects
 * representing instances of classes which implement this interface.
 * <p>
 * <b> Example Usage: </b>
 * <p>
 * <code> class MyClass implements Jsonic&lt;MyClass&gt; { ... } </code>
 * 
 * @param <T> The type of the object implementing this interface (should be the
 *            same as the Class Name).
 */
public interface Jsonic<T extends Jsonic<T>> {

    /**
     * Turn this object's fields into an accurately-modeled JSONObject.
     * 
     * @return A JSONObject representing this object.
     */
    public JSONObject toJson();

    /**
     * Interpret a JSONObject into an object of this type.
     * 
     * @param json A JSONObject containing any number of the fields for this object
     *             as key-value pairs.
     * @return The Object interpreted from the Json. Note that this object's fields
     *         will also be set.
     */
    public T fromJson(JSONObject json);

    /**
     * Turns a collection into a JSONObject list. Any elements which extend Jsonic<>
     * will use their {@code .toJson()} methods,
     * and any other not-null elements will be passed to the
     * {@code Jsonic.toJson(Object)} function.
     * 
     * @param collection Collection containing any type.
     * @return List of JSONObjects with the contents of the passed collection.
     * @see #toJson()
     * @see #toJson(Object)
     */
    public static List<JSONObject> collectionToJson(Collection<?> collection) {
        List<JSONObject> json = new ArrayList<>();
        for (Object item : collection) {
            if (item instanceof Jsonic<?> jsonic) {
                json.add(jsonic.toJson());
            } else if (item != null) {
                json.add(toJson(item));
            } else
                json.add(new JSONObject()); // empty object representing null
        }
        return json;
    }

    /**
     * Turns an array into a JSONObject list. Any elements which extend Jsonic<>
     * will use their {@code .toJson()} methods,
     * and any other not-null elements will be passed to the
     * {@code Jsonic.toJson(Object)} function.
     * 
     * @param array Array containing any type.
     * @return List of JSONObjects with the contents of the passed array.
     * @see #toJson()
     * @see #toJson(Object)
     */
    public static List<JSONObject> arrayToJson(Object array) {
        int length = Array.getLength(array);
        List<JSONObject> json = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            Object element = Array.get(array, i);
            if (element instanceof Jsonic<?> jsonic) {
                json.add(jsonic.toJson());
            } else if (element != null) {
                json.add(toJson(element));
            }
        }
        return json;
    }

    private static boolean isPrimitiveOrWrapper(Class<?> type) {
        Class<?>[] types = { Boolean.class, Byte.class, Character.class, Short.class, Integer.class, Long.class,
                Float.class, Double.class, String.class };
        if (type.isPrimitive())
            return true;
        for (Class<?> t : types) {
            if (type == t)
                return true;
        }
        return false;
    }

    /**
     * Reflects the passed Class's fields into an unvalued "template" JSONObject.
     * 
     * @param clazz Any class.
     * @return A JSONObject "template" of the class's fields.
     * @see #toJson(Object)
     */
    public static JSONObject classJson(Class<? extends Object> clazz) {
        List<JSONObject> fields = new ArrayList<>();
        for (java.lang.reflect.Field f : clazz.getDeclaredFields()) {
            // Only take public, non-static fields
            int modifiers = f.getModifiers();
            if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)) {
                String field = f.getName();
                fields.add(new JSONObject(field));
            }
        }
        return new JSONObject(clazz.getSimpleName(), fields);
    }

    /**
     * Reflects the passed Object's public instance fields and their values into a
     * JSONObject.
     * 
     * @param o Object of any type.
     * @return JSONObject containing the passed object's fields and their values.
     * @see #classJson(Class)
     */
    public static JSONObject toJson(Object o) {
        List<JSONObject> fields = new ArrayList<>();
        Class<?> clazz = o.getClass();

        // Get all fields declared by O.class
        for (java.lang.reflect.Field f : clazz.getDeclaredFields()) {
            // Only take public, non-static fields
            int modifiers = f.getModifiers();
            if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)) {
                Object value;
                try {
                    value = f.get(o);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    continue;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }

                if (value instanceof Jsonic<?> jsonic) {
                    fields.add(new JSONObject(f.getName(), jsonic.toJson()));
                } else if (value != null && isPrimitiveOrWrapper(value.getClass())) {
                    fields.add(new JSONObject(f.getName(), value));
                } else if (value != null && value.getClass().isArray()) {
                    fields.add(new JSONObject(f.getName(), arrayToJson(value)));
                } else if (value != null && value instanceof Collection<?> collection) {
                    fields.add(new JSONObject(f.getName(), collectionToJson(collection)));
                } else if (value != null) {
                    fields.add(new JSONObject(f.getName(), toJson(value)));
                } else
                    fields.add(new JSONObject(f.getName(), null));
            }
        }
        return new JSONObject(o.getClass().getSimpleName(), fields);
    }
}
