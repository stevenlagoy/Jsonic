# Jsonic - JSON Java Objectifier

A Java library for parsing, validating, and manipulating JSON data with strong type safety and customizable object structures.

## Features

- **JSON Validation**: Strict validation according to JSON specification
- **Type Safety**: Strong type checking and conversion
- **Custom Object Mapping**: Convert JSON to typed Java objects
- **Duplicate Key Detection**: Validation for unique keys within objects
- **Pretty Printing**: Configurable JSON string formatting
- **Unicode Support**: Full support for Unicode characters and escape sequences

## Installation

Add this dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.github.stevenlagoy</groupId>
    <artifactId>json-java-objectifier</artifactId>
    <version>1.0.5</version>
</dependency>
```

## Usage

### JSON Processing

From Java String:
```java
String jsonString = "{\"key\":\"value\"}";
JSONObject obj = JSONProcessor.processJson("root", jsonString);
```

From JSON File:
```java
Path jsonPath = Path.of("path", "to", "file.json");
JSONObject obj = JSONProcessor.processJson(jsonPath);
```

### Validate JSON

From Java String:
```java
String jsonString = "{\"numbers\":[1,2,3]}";
boolean isValid = JSONValidator.validateJson(jsonString);
```

From JSON File:
```java
Path jsonPath = Path.of("path", "to", "file.json");
JSONObject obj = JSONValidator.validateJson(jsonPath);
```

### Access Typed Values

```java
JSONObject obj = new JSONObject("key", "value");
String str = obj.getAsString();         // Type-safe string access
Number num = obj.getAsNumber();         // Type-safe number access
Boolean bool = obj.getAsBoolean();      // Type-safe boolean access
List<?> list = obj.getAsList();         // Type-safe list access
```

### Pretty Print JSON

With JSONObject.toString():
```java
JSONObject obj = JSONProcessor.processJson("root", jsonString);
String formatted = obj.toString();  // Returns formatted JSON string
```

With JSONStringifier:
```java
JSONObject obj = JSONProcessor.processJson("root", jsonString);
String formatted = JSONStringifier.stringifyJson(obj);  // Returns formatted JSON string on one line
```

## Features in Detail

### Type Support
- Strings (with Unicode)
- Numbers (Integer, Long, Double)
- Objects (nested)
- Arrays
- Booleans
- Null values

### Validation
- Strict JSON syntax checking (ECMA-404 specification)
- Duplicate key detection
- Type validation
- Unicode escape sequence validation

### Error Handling
- Detailed error messages
- Type conversion safety
- Null safety

## Building from Source

```bash
git clone https://github.com/stevenlagoy/Jsonic.git
cd json-java-objectifier
mvn clean install
```

## Requirements

- Java 21 or higher
- Maven 3.x

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Built with Java 21
- Uses JUnit for testing
- SpotBugs for code quality
- Maven for build management
