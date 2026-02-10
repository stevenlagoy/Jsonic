import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import core.JSONValidator;

public class ValidatorTests {

    @Test
    public void testValidateJsonString() {

        assertTrue(JSONValidator.validateJson("{}"));
        assertTrue(JSONValidator.validateJson("{\"key\":\"value\"}"));
        assertTrue(JSONValidator.validateJson("{\n\"key\"\n:\n\"value\"\n}"));
        assertTrue(JSONValidator.validateJson("{\"nested\":{\"key\":\"value\"}}"));
        assertTrue(JSONValidator.validateJson("{\"array\":[1,2,3]}"));
        assertTrue(JSONValidator.validateJson("{\"mixed\":[1,{\"key\":\"value\"},\"string\"]}"));
        assertTrue(JSONValidator.validateJson("{\"unicode\":\"🌟\"}"));
        assertTrue(JSONValidator.validateJson("{\"number\":42.5e-10}"));

        // assertFalse(JSONValidator.validateJson(null));
        assertFalse(JSONValidator.validateJson(""));
        assertFalse(JSONValidator.validateJson(" "));
        assertFalse(JSONValidator.validateJson("not json"));
        assertFalse(JSONValidator.validateJson("{key:\"value\"}")); // Unquoted key
        assertFalse(JSONValidator.validateJson("{\"key\":value}")); // Unquoted value
        assertFalse(JSONValidator.validateJson("{\"key\":\"value\",")); // Unclosed object
        assertFalse(JSONValidator.validateJson("{\"key\":\"value\"}}")); // Extra closing brace
        assertFalse(JSONValidator.validateJson("{\"key\":\"value\",,}")); // Double comma
        assertFalse(JSONValidator.validateJson("{\"key\":\"unterminated string}")); // Unterminated string
        assertFalse(JSONValidator.validateJson("{\"key\":\"invalid\\escape\"}")); // Invalid escape
        assertFalse(JSONValidator.validateJson("{\"duplicate\":1,\"duplicate\":2}")); // Duplicate keys
    }

    @Test
    public void testValidateJsonMultiline() {
        String multiline = String.join("\n",
            "{",
            "    \"key1\": \"value1\",",
            "    \"key2\": {",
            "        \"nested\": \"value2\"",
            "    },",
            "    \"key3\": [",
            "        1,",
            "        2,",
            "        3",
            "    ]",
            "}"
        );
        assertTrue(JSONValidator.validateJson(multiline));
    }

    @Test
    public void testValidateJsonEdgeCases() {
        assertTrue(JSONValidator.validateJson("{\"empty_string\":\"\"}"));
        assertTrue(JSONValidator.validateJson("{\"special_chars\":\"\\u0021\\n\\t\"}"));
        assertTrue(JSONValidator.validateJson("{\"empty_object\":{}}"));
        assertTrue(JSONValidator.validateJson("{\"empty_array\":[]}"));
        assertTrue(JSONValidator.validateJson("{\"null\":null}"));
        assertTrue(JSONValidator.validateJson("{\"bool\":true}"));
    }

    @Test
    public void testDuplicateKeys() {
        // Invalid - duplicate keys at root level
        assertFalse(JSONValidator.validateJson("{\"key\":1,\"key\":2}"));

        // Invalid - duplicate keys in nested object
        assertFalse(JSONValidator.validateJson("{\"outer\":{\"key\":1,\"key\":2}}"));

        // Valid - same key name but in different objects
        assertTrue(JSONValidator.validateJson("{\"obj1\":{\"key\":1},\"obj2\":{\"key\":2}}"));

        // Invalid - duplicate keys with different value types
        assertFalse(JSONValidator.validateJson("{\"key\":1,\"key\":\"string\"}"));

        // Invalid - duplicate keys with same values
        assertFalse(JSONValidator.validateJson("{\"key\":1,\"key\":1}"));
    }

    @Test
    public void validateObject() {

        assertFalse(JSONValidator.validateObject(null));
        assertFalse(JSONValidator.validateObject(""));
        assertFalse(JSONValidator.validateObject("    "));

        assertTrue(JSONValidator.validateObject("{}"));
        assertTrue(JSONValidator.validateObject("{ }"));
        assertTrue(JSONValidator.validateObject("{\"key\" : \"string\"}"));
        assertTrue(JSONValidator.validateObject("{     \"key\" : \"string\"     }"));
        assertTrue(JSONValidator.validateObject("{\"key1\" : \"value1\", \"key2\" : \"value2\"}"));
        assertTrue(JSONValidator.validateObject("{\"object\" : {}}"));
        assertTrue(JSONValidator.validateObject("{\"object\" : {\"key\" : \"value\"}}"));
        assertTrue(JSONValidator.validateObject("{\"array\" : []}"));
        assertTrue(JSONValidator.validateObject("{\"array\" : [10, 4, 6]}"));

        assertFalse(JSONValidator.validateObject("{{}"));
        assertFalse(JSONValidator.validateObject("{}}"));
        assertFalse(JSONValidator.validateObject("{\"unmatched\"}"));
        assertFalse(JSONValidator.validateObject("\"too many colons\" : : \"value\""));
        assertFalse(JSONValidator.validateObject("{50}"));
        assertFalse(JSONValidator.validateObject("{false}"));
        assertFalse(JSONValidator.validateObject("{10 : \"bad key\"}"));

    }

    @Test
    public void validateMembers() {

        assertFalse(JSONValidator.validateMembers(null));
        assertFalse(JSONValidator.validateMembers(""));
        assertFalse(JSONValidator.validateMembers("    "));

        assertTrue(JSONValidator.validateMembers("\"key\" : \"string\""));
        assertTrue(JSONValidator.validateMembers("\"key1\" : \"value1\", \"key2\" : \"value2\""));
        assertTrue(JSONValidator.validateMembers("    \"key1\" : \"value\"    ,    \"key2\" : \"value\""));
        assertTrue(JSONValidator.validateMembers("\"array\" : [], \"array_of_array\" : [[]]"));
        assertTrue(JSONValidator.validateMembers("\"true\" : true, \"false\" : false, \"null\" : null"));
        assertTrue(JSONValidator.validateMembers("\"number1\" : 10, \"number2\" : 534.25E-49"));
        assertTrue(JSONValidator
                .validateMembers("\"key containing , comma\" : \"value containing comma\", \"key\" : \"value\""));

        assertFalse(JSONValidator.validateMembers("bad key : bad value"));
        assertFalse(JSONValidator.validateMembers("\"unpaired\" :"));
        assertFalse(JSONValidator.validateMembers(": \"unpaired\""));
        assertFalse(JSONValidator.validateMembers("\"unseparated\" : \"value1\" \"missing comma\" : \"value2\""));
        assertFalse(JSONValidator.validateMembers("\"extra comma\" : \"value\" ,"));
        assertFalse(JSONValidator.validateMembers(" : , :"));
        assertFalse(JSONValidator.validateMembers("\"no colon\" , \"key\" : \"value\""));
    }

    @Test
    public void validatePair() {

        assertFalse(JSONValidator.validatePair(null));
        assertFalse(JSONValidator.validatePair(""));
        assertFalse(JSONValidator.validatePair("    "));

        assertTrue(JSONValidator.validatePair("\"key\" : \"string\""));
        assertTrue(JSONValidator.validatePair("\"key\" : 9.5e-9"));
        assertTrue(JSONValidator.validatePair("\"key with \\\" escaped quote\" : \"value with \\\" escaped quote\""));
        assertTrue(JSONValidator.validatePair("\"key\" : []"));
        // assertTrue(JSONValidator.validatePair("\"key\" : {}"));
        assertTrue(JSONValidator.validatePair("\"key\" : [[], \"string\"]"));
        assertTrue(JSONValidator.validatePair("\"true\" : true"));
        assertTrue(JSONValidator.validatePair("\"false\" : false"));
        assertTrue(JSONValidator.validatePair("\"null\" : null"));
        assertTrue(JSONValidator.validatePair("\"\" : \"\""));

        assertFalse(JSONValidator.validatePair("\"bad key : \"string\""));
        assertFalse(JSONValidator.validatePair(" : \"string\""));
        assertFalse(JSONValidator.validatePair("bad key : \"string\""));
        assertFalse(JSONValidator.validatePair("\"key\" : bad value"));
        assertFalse(JSONValidator.validatePair("\"key\" : 10.0.4"));
        assertFalse(JSONValidator.validatePair("55 : true"));
        assertFalse(JSONValidator.validatePair("\"key\" : : \"string\""));
        assertFalse(JSONValidator.validatePair("\"key\" : \"string1\" : \"string2\""));
        assertFalse(JSONValidator.validatePair("\"key\" : [[]"));
        assertFalse(JSONValidator.validatePair("\"key\" : ]"));
    }

    @Test
    public void validateArray() {

        assertFalse(JSONValidator.validateArray(null));
        assertFalse(JSONValidator.validateArray(""));
        assertFalse(JSONValidator.validateArray("    "));

        assertTrue(JSONValidator.validateArray("[]"));
        assertTrue(JSONValidator.validateArray("[ ]"));
        assertTrue(JSONValidator.validateArray("[\"string\"]"));
        assertTrue(JSONValidator.validateArray("[\"string1\",\"string2\"]"));
        assertTrue(JSONValidator.validateArray("[\"string1\",    \"string2\"]"));
        assertTrue(JSONValidator.validateArray("[\"string1\"    ,\"string2\"]"));
        assertTrue(JSONValidator.validateArray("[\"string1\"    ,    \"string2\"]"));
        assertTrue(JSONValidator.validateArray("[    \"string1\",\"string2\"]"));
        assertTrue(JSONValidator.validateArray("[\"string1\",\"string2\"    ]"));
        assertTrue(JSONValidator.validateArray("    [    \"string1\"    ,    \"string2\"    ]    "));
        assertTrue(JSONValidator.validateArray("[[]]"));
        assertTrue(JSONValidator.validateArray("[[], [[]]]"));
        assertTrue(JSONValidator.validateArray("[[\"string1\"], \"string2\"]"));
        assertTrue(JSONValidator.validateArray("[1, 2, 3]"));
        assertTrue(JSONValidator.validateArray("[1.23E4, -987.6]"));

        assertFalse(JSONValidator.validateArray("["));
        assertFalse(JSONValidator.validateArray("]"));
        assertFalse(JSONValidator.validateArray("[[ ]"));
        assertFalse(JSONValidator.validateArray("[ ]]"));
        assertFalse(JSONValidator.validateArray("[\"string1\" \"string2\"]"));
        assertFalse(JSONValidator.validateArray("[\"string\",    ]"));
        assertFalse(JSONValidator.validateArray("[    , \"string\"]"));
        assertFalse(JSONValidator.validateArray("[\"key\" : \"value\"]"));
    }

    @Test
    public void validateElements() {

        assertFalse(JSONValidator.validateElements(null));
        assertFalse(JSONValidator.validateElements(""));
        assertFalse(JSONValidator.validateElements("    "));

        assertTrue(JSONValidator.validateElements("\"string\""));
        assertTrue(JSONValidator.validateElements("\"string with , comma\""));
        assertTrue(JSONValidator.validateElements("\"string1\", \"string2\""));
        assertTrue(JSONValidator.validateElements("-10.3e10"));
        assertTrue(JSONValidator.validateElements("1, 2, 3"));
        assertTrue(JSONValidator.validateElements("\"string\", -4.9"));
        // assertTrue(JSONValidator.validateElements(""));

        assertFalse(JSONValidator.validateElements(","));
        assertFalse(JSONValidator.validateElements("\"string\", "));
        assertFalse(JSONValidator.validateElements("\"string\", ,"));
        assertFalse(JSONValidator.validateElements("\"string1\" \"string2\""));
        assertFalse(JSONValidator.validateElements("14.0, "));
        assertFalse(JSONValidator.validateElements("87 0.73"));
    }

    @Test
    public void validateValue() {

        assertFalse(JSONValidator.validateValue(null));
        assertFalse(JSONValidator.validateValue(""));
        assertFalse(JSONValidator.validateValue("    "));

        assertTrue(JSONValidator.validateValue("true"));
        assertTrue(JSONValidator.validateValue("false"));
        assertTrue(JSONValidator.validateValue("null"));
        assertTrue(JSONValidator.validateValue("\"string value\""));
        assertTrue(JSONValidator.validateValue("10.5e-6"));
        assertTrue(JSONValidator.validateValue("[]"));
        assertTrue(JSONValidator.validateValue("[\"string\"]"));
        // assertTrue(JSONValidator.validateValue("{}"));

        assertFalse(JSONValidator.validateValue("none"));
        assertFalse(JSONValidator.validateValue("\"string value"));
        assertFalse(JSONValidator.validateValue("a10.5e-6"));
        assertFalse(JSONValidator.validateValue("[]]"));
        assertFalse(JSONValidator.validateValue("[\"string1\" \"string2\"]"));
        // assertFalse(JSONValidator.validateValue("{}}"));
    }

    @Test
    public void validateString() {

        assertFalse(JSONValidator.validateString(null));
        assertFalse(JSONValidator.validateString(""));

        assertTrue(JSONValidator.validateString("\"string\""));
        assertTrue(JSONValidator.validateString("\"\""));
        assertTrue(JSONValidator.validateString("\" string \""));
        assertTrue(JSONValidator.validateString("\"hello world\""));
        assertTrue(JSONValidator.validateString("\"\\\"\""));

        assertFalse(JSONValidator.validateString("\"\"\""));
        assertFalse(JSONValidator.validateString("string"));
        assertFalse(JSONValidator.validateString("\"string1\", \"string2\""));
        assertFalse(JSONValidator.validateString("\""));
    }

    @Test
    public void validateCharacters() {

        assertFalse(JSONValidator.validateCharacters(null));
        assertFalse(JSONValidator.validateCharacters(""));

        assertTrue(JSONValidator.validateCharacters("hello"));
        assertTrue(JSONValidator.validateCharacters("hello world"));
        assertTrue(JSONValidator.validateCharacters("hello\\n"));
        assertTrue(JSONValidator.validateCharacters("hello\\u263A"));
        assertTrue(JSONValidator.validateCharacters("汉字"));
        assertTrue(JSONValidator.validateCharacters("🌟✨"));
        assertTrue(JSONValidator.validateCharacters("👨‍👩‍👧‍👦"));

        assertFalse(JSONValidator.validateCharacters("hello\"world")); // Contains quote
        assertFalse(JSONValidator.validateCharacters("hello\\zworld")); // Invalid escape
        assertFalse(JSONValidator.validateCharacters("\n\t")); // Control chars
    }

    @Test
    public void validateCharacter() {

        assertTrue(JSONValidator.validateCharacter("a"));
        assertTrue(JSONValidator.validateCharacter("5"));
        assertTrue(JSONValidator.validateCharacter(" "));
        assertTrue(JSONValidator.validateCharacter("🌟"));
        // assertTrue(JSONValidator.validateCharacter("👨‍👩‍👧‍👦")); // This is a grapheme cluster containing
        // zero-width joiners.
        assertTrue(JSONValidator.validateCharacter("é"));
        assertTrue(JSONValidator.validateCharacter("汉"));
        assertTrue(JSONValidator.validateCharacter("\\n"));
        assertTrue(JSONValidator.validateCharacter("\\u263A"));

        assertFalse(JSONValidator.validateCharacter("\""));
        assertFalse(JSONValidator.validateCharacter("\\"));
        assertFalse(JSONValidator.validateCharacter("\n"));
        assertFalse(JSONValidator.validateCharacter("ab"));
        assertFalse(JSONValidator.validateCharacter("🌟🌟"));
    }

    @Test
    public void validateEscape() {
        String[] validEscapes = { "\\\"", "\\\\", "\\/", "\\b", "\\f", "\\n", "\\r", "\\t" };

        assertFalse(JSONValidator.validateEscape(null));
        assertFalse(JSONValidator.validateEscape(""));
        assertFalse(JSONValidator.validateEscape("    "));
        for (String escape : validEscapes) {
            assertTrue(JSONValidator.validateEscape(escape));
        }
        assertTrue(JSONValidator.validateEscape("\\u1048"));
        assertTrue(JSONValidator.validateEscape("\\uABFC"));
        assertTrue(JSONValidator.validateEscape("\\u04fA"));
        assertFalse(JSONValidator.validateEscape("\\g"));
        assertFalse(JSONValidator.validateEscape("n"));
        assertFalse(JSONValidator.validateEscape("\\bad"));
        assertFalse(JSONValidator.validateEscape("\\u"));
        assertFalse(JSONValidator.validateEscape("\u11111"));
    }

    @Test
    public void validateNumber() {
        String validNumber1 = "100";
        String validNumber2 = "-20";
        String validNumber3 = "1.25";
        String validNumber4 = "-4.75";
        String validNumber5 = "4e5";
        String validNumber6 = "-6e-3";
        String validNumber7 = "5.8e12";
        String validNumber8 = "-23.17e-33";
        String invalidNumber1 = "40a";
        String invalidNumber2 = "--5";
        String invalidNumber3 = "1.";
        String invalidNumber4 = "--2.6";
        String invalidNumber5 = "84Ee9";
        String invalidNumber6 = "3E--9";
        String invalidNumber7 = "4r.-3E4g";
        String invalidNumber8 = "--3i.6nE--2m";

        assertFalse(JSONValidator.validateNumber(null));
        assertFalse(JSONValidator.validateNumber(""));
        assertFalse(JSONValidator.validateNumber("    "));
        assertTrue(JSONValidator.validateNumber(validNumber1));
        assertTrue(JSONValidator.validateNumber(validNumber2));
        assertTrue(JSONValidator.validateNumber(validNumber3));
        assertTrue(JSONValidator.validateNumber(validNumber4));
        assertTrue(JSONValidator.validateNumber(validNumber5));
        assertTrue(JSONValidator.validateNumber(validNumber6));
        assertTrue(JSONValidator.validateNumber(validNumber7));
        assertTrue(JSONValidator.validateNumber(validNumber8));
        assertFalse(JSONValidator.validateNumber(invalidNumber1));
        assertFalse(JSONValidator.validateNumber(invalidNumber2));
        assertFalse(JSONValidator.validateNumber(invalidNumber3));
        assertFalse(JSONValidator.validateNumber(invalidNumber4));
        assertFalse(JSONValidator.validateNumber(invalidNumber5));
        assertFalse(JSONValidator.validateNumber(invalidNumber6));
        assertFalse(JSONValidator.validateNumber(invalidNumber7));
        assertFalse(JSONValidator.validateNumber(invalidNumber8));
    }

    @Test
    public void validateFrac() {

        assertFalse(JSONValidator.validateFrac(null));
        assertFalse(JSONValidator.validateFrac(""));
        assertFalse(JSONValidator.validateFrac("    "));

        assertTrue(JSONValidator.validateFrac(".1"));
        assertTrue(JSONValidator.validateFrac(".99999999"));

        assertFalse(JSONValidator.validateFrac("1"));
        assertFalse(JSONValidator.validateFrac("."));
        assertFalse(JSONValidator.validateFrac(".5."));
        assertFalse(JSONValidator.validateFrac(".25a"));
    }

    @Test
    public void validateExp() {

        assertFalse(JSONValidator.validateExp(null));
        assertFalse(JSONValidator.validateExp(""));
        assertFalse(JSONValidator.validateExp("    "));

        assertTrue(JSONValidator.validateExp("e100"));
        assertTrue(JSONValidator.validateExp("E100"));
        assertTrue(JSONValidator.validateExp("e+100"));
        assertTrue(JSONValidator.validateExp("E-100"));

        assertFalse(JSONValidator.validateExp("a100"));
        assertFalse(JSONValidator.validateExp("e"));
        assertFalse(JSONValidator.validateExp("ee100"));
        assertFalse(JSONValidator.validateExp("E_100"));
        assertFalse(JSONValidator.validateExp("e100.0"));
    }

    @Test
    public void validateDigits() {
        String validDigits = "00112233445566778899";
        String invalidDigits = "00112233aa";
        assertFalse(JSONValidator.validateDigits(null));
        assertFalse(JSONValidator.validateDigits(""));
        assertFalse(JSONValidator.validateDigits("    "));
        assertTrue(JSONValidator.validateDigits(validDigits));
        assertFalse(JSONValidator.validateDigits(invalidDigits));
    }

    @Test
    public void validateDigit() {
        String validDigit = "0123456789";
        String invalidDigit = "!\\\"#$%&'()*+,-./:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";
        for (char digit : validDigit.toCharArray()) {
            assertTrue(JSONValidator.validateDigit(digit));
        }
        for (char digit : invalidDigit.toCharArray()) {
            assertFalse(JSONValidator.validateDigit(digit));
        }
    }

    @Test
    public void validateHex() {
        String validHex = "0123456789abcdefABCDEF";
        String invalidHex = "!\"#$%&'()*+,-./:;<=>?@GHIJKLMNOPQRSTUVWXYZ[\\]^_`ghijklmnopqrstuvwxyz{|}~";
        for (char hex : validHex.toCharArray()) {
            assertTrue(JSONValidator.validateHex(hex));
        }
        for (char hex : invalidHex.toCharArray()) {
            assertFalse(JSONValidator.validateHex(hex));
        }
    }
}
