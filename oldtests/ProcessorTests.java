import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import core.JSONObject;
import core.JSONProcessor;

public class ProcessorTests {

    @Test
    public void testProcessValue() {
        // Test strings
        assertEquals("hello", JSONProcessor.processValue("\"hello\""));
        assertEquals("", JSONProcessor.processValue("\"\""));

        // Test numbers
        Number num = (Number) JSONProcessor.processValue("42");
        assertEquals(42, num.intValue());
        num = (Number) JSONProcessor.processValue("-3.14");
        assertEquals(-3.14, num.doubleValue(), 0.001);
        num = (Number) JSONProcessor.processValue("1.23e-4");
        assertEquals(1.23e-4, num.doubleValue(), 0.00001);

        // Test booleans
        assertEquals(Boolean.TRUE, JSONProcessor.processValue("true"));
        assertEquals(Boolean.FALSE, JSONProcessor.processValue("false"));

        // Test null
        assertNotNull(JSONProcessor.processValue("null"));

        // Test arrays
        Object arr = JSONProcessor.processValue("[1,2,3]");
        assertTrue(arr instanceof List);
        assertEquals(3, ((List<?>) arr).size());

        // Test objects
        Object obj = JSONProcessor.processValue("{\"key\":\"value\"}");
        assertTrue(obj instanceof List);
        assertTrue(((List<?>) obj).get(0) instanceof JSONObject);
    }

    @Test
    public void testProcessString() {
        // Test basic strings
        assertEquals("hello", JSONProcessor.processString("\"hello\""));
        assertEquals("", JSONProcessor.processString("\"\""));

        // Test escape sequences
        assertEquals("hello\nworld", JSONProcessor.processString("\"hello\\nworld\""));
        assertEquals("\"quoted\"", JSONProcessor.processString("\"\\\"quoted\\\"\""));
        assertEquals("\\backslash", JSONProcessor.processString("\"\\\\backslash\""));

        // Test invalid strings
        assertNull(JSONProcessor.processString("hello")); // No quotes
        assertNull(JSONProcessor.processString("\"")); // Unclosed quote
        assertNull(JSONProcessor.processString(null));
    }

    @Test
    public void testProcessNumber() {
        // Test integers
        Number num = JSONProcessor.processNumber("42");
        assertEquals(42, num.intValue());
        num = JSONProcessor.processNumber("-42");
        assertEquals(-42, num.intValue());

        // Test decimals
        num = JSONProcessor.processNumber("3.14");
        assertEquals(3.14, num.doubleValue(), 0.001);
        num = JSONProcessor.processNumber("-3.14");
        assertEquals(-3.14, num.doubleValue(), 0.001);

        // Test scientific notation
        assertEquals(1.23e-4, (double) JSONProcessor.processNumber("1.23e-4"), 0.00001);
        assertEquals(1.23E4, (double) JSONProcessor.processNumber("1.23E4"), 0.001);

        // Test invalid numbers
        assertNull(JSONProcessor.processNumber("abc"));
        assertNull(JSONProcessor.processNumber(""));
        assertNull(JSONProcessor.processNumber(null));
    }

    @Test
    public void testProcessArray() {
        // Test empty array
        List<Object> empty = JSONProcessor.processArray("[]");
        assertNotNull(empty);
        assertTrue(empty.isEmpty());

        // Test simple array
        List<Object> numbers = JSONProcessor.processArray("[1,2,3]");
        assertEquals(3, numbers.size());
        Number num = (Number) numbers.get(0);
        assertEquals(1, num.intValue());

        // Test mixed array
        List<Object> mixed = JSONProcessor.processArray("[1.0,\"two\",true]");
        assertEquals(3, mixed.size());
        assertEquals(1.0, mixed.get(0));
        assertEquals("two", mixed.get(1));
        assertEquals(Boolean.TRUE, mixed.get(2));

        // Test nested arrays
        List<Object> nested = JSONProcessor.processArray("[[1,2],[3,4]]");
        assertEquals(2, nested.size());
        assertTrue(nested.get(0) instanceof List);
    }

    @Test
    public void testProcessObject() {
        // Test empty object
        assertEquals(new ArrayList<>(), JSONProcessor.processObject("{}"));

        // Test simple object
        List<JSONObject> simple = JSONProcessor.processObject("{\"key\":\"value\"}");
        assertNotNull(simple);
        assertEquals(1, simple.size());
        assertEquals("value", simple.get(0).getValue());

        // Test nested object
        List<JSONObject> nested = JSONProcessor.processObject("{\"outer\":{\"inner\":\"value\"}}");
        assertNotNull(nested);
        assertTrue(nested.get(0).getValue() instanceof List);
    }

    @Test
    public void testProcessEscape() {
        // Test basic escapes
        assertEquals("\"", JSONProcessor.processEscape("\\\""));
        assertEquals("\\", JSONProcessor.processEscape("\\\\"));
        assertEquals("\n", JSONProcessor.processEscape("\\n"));
        assertEquals("\r", JSONProcessor.processEscape("\\r"));
        assertEquals("\t", JSONProcessor.processEscape("\\t"));

        // Test unicode escapes
        assertEquals("♥", JSONProcessor.processEscape("\\u2665"));

        // Test invalid escapes
        assertNull(JSONProcessor.processEscape("\\z"));
        assertNull(JSONProcessor.processEscape("\\"));
        assertNull(JSONProcessor.processEscape(null));
    }

    @Test
    public void testProcessHex() {
        // Test digits
        assertEquals(0, JSONProcessor.processHex('0'));
        assertEquals(9, JSONProcessor.processHex('9'));

        // Test lowercase letters
        assertEquals(10, JSONProcessor.processHex('a'));
        assertEquals(15, JSONProcessor.processHex('f'));

        // Test uppercase letters
        assertEquals(10, JSONProcessor.processHex('A'));
        assertEquals(15, JSONProcessor.processHex('F'));

        // Test invalid hex
        assertEquals(-1, JSONProcessor.processHex('g'));
        assertEquals(-1, JSONProcessor.processHex('G'));
        assertEquals(-1, JSONProcessor.processHex('x'));
    }

    @Test
    public void processDigit() {
        assertEquals(0, JSONProcessor.processDigit('0'));
        assertEquals(1, JSONProcessor.processDigit('1'));
        assertEquals(2, JSONProcessor.processDigit('2'));
        assertEquals(3, JSONProcessor.processDigit('3'));
        assertEquals(4, JSONProcessor.processDigit('4'));
        assertEquals(5, JSONProcessor.processDigit('5'));
        assertEquals(6, JSONProcessor.processDigit('6'));
        assertEquals(7, JSONProcessor.processDigit('7'));
        assertEquals(8, JSONProcessor.processDigit('8'));
        assertEquals(9, JSONProcessor.processDigit('9'));
    }

    @Test
    public void processHex() {
        assertEquals(0, JSONProcessor.processHex('0'));
        assertEquals(1, JSONProcessor.processHex('1'));
        assertEquals(2, JSONProcessor.processHex('2'));
        assertEquals(3, JSONProcessor.processHex('3'));
        assertEquals(4, JSONProcessor.processHex('4'));
        assertEquals(5, JSONProcessor.processHex('5'));
        assertEquals(6, JSONProcessor.processHex('6'));
        assertEquals(7, JSONProcessor.processHex('7'));
        assertEquals(8, JSONProcessor.processHex('8'));
        assertEquals(9, JSONProcessor.processHex('9'));
        assertEquals(10, JSONProcessor.processHex('A'));
        assertEquals(11, JSONProcessor.processHex('B'));
        assertEquals(12, JSONProcessor.processHex('C'));
        assertEquals(13, JSONProcessor.processHex('D'));
        assertEquals(14, JSONProcessor.processHex('E'));
        assertEquals(15, JSONProcessor.processHex('F'));

    }

}
