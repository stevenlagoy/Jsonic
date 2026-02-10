import static org.junit.Assert.*;
import org.junit.Test;
import core.JSONObject;
import core.JSONStringifier;
import java.util.ArrayList;
import java.util.List;

public class StringifierTests {

    @Test
    public void stringifyJson() {
        ArrayList<JSONObject> inner = new ArrayList<>();
        inner.add(new JSONObject("key1", "value1"));
        inner.add(new JSONObject("key2", 42));
        JSONObject nested = new JSONObject("nested", inner);
        String result = JSONStringifier.stringifyJson(nested);
        assertEquals("\"nested\" : {\"key1\" : \"value1\", \"key2\" : 42}", result);
    }

    @Test
    public void stringifyValue() {
        // Test primitive values
        assertEquals("\"hello\"", JSONStringifier.stringifyValue("hello"));
        assertEquals("42", JSONStringifier.stringifyValue(42));
        assertEquals("true", JSONStringifier.stringifyValue(true));
        assertEquals("null", JSONStringifier.stringifyValue(new Object()));
        
        // Test with type information
        List<Integer> numbers = new ArrayList<>();
        numbers.add(1);
        numbers.add(2);
        numbers.add(3);
        assertEquals("[1, 2, 3]", JSONStringifier.stringifyValue(numbers, ArrayList.class));
        
        // Test nested objects with type
        List<JSONObject> objects = new ArrayList<>();
        objects.add(new JSONObject("key1", "value1"));
        objects.add(new JSONObject("key2", "value2"));
        String result = JSONStringifier.stringifyValue(objects, JSONObject.class);
        assertEquals("{\"key1\" : \"value1\"}{\"key2\" : \"value2\"}", result);
    }

    @Test
    public void stringifyNestedObjects() {
        JSONObject json = new JSONObject("root", List.of(new JSONObject("inner_1", "value 1"), new JSONObject("inner_2", "value_2")));
        String expected = "\"root\" : {\n" +
                          "\t\"inner_1\" : \"value 1\",\n" +
                          "\t\"inner_2\" : \"value_2\"\n" +
                          "}";
        String actual = json.toString();
        assertEquals(expected, actual);
    }

    @Test
    public void stringifyNestedSingleObject() {
        JSONObject json = new JSONObject("parent", new JSONObject("child", null));
        String expected = "\"parent\" : {\n" +
                          "\t\"child\" : null\n" +
                          "}";
        String actual = json.toString();
        assertEquals(expected, actual);
    }

}