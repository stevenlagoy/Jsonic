# Jsonic: JSON-Java Objectifier

A Java library for parsing, validating, navigating, and manipulating JSON data. Jsonic provides a unified tree-node architecture with type-safe accessors, path-based navigation, declarative schema validation, and seamless object-relational mapping.

<img src="https://stevenlagoy.github.io/assets/jsonic_thumb-DWv7fErC.png" width="300" alt="Jsonic logo, a simple coffee cup with rising steam drawn with square brackets, curly braces, and parentheses">

## Features

- **Unified recursive tree architecture**: Each `JSONObject` is a standalone node holding a key-value pair. Nodes model scalars, primitive arrays, or nested object hierarchies, forming a recursive tree that mirrors any JSON structure.
- **Constructor-based parsing**: Parse JSON files or raw strings directly during `JSONObject` instantiation. No separate parser step required.
- **Granular type-safe accessors**: Retrieve typed values using optional lookups (`getString()`, `getInt()`) or strict requirements (`requireString()`, `requireInt()`) that throw descriptive exceptions on failure.
- **Path-based navigation**: Resolve deeply nested values using dot-separated string paths or vararg segments (`findStringAt("user.profile/city")`), with escaped-dot support for keys that contain dots.
- **Prioritized subtree searching**: Search across multiple fallback keys in priority order (`findString(List.of("email", "phone", "username"))`), returning the first match found anywhere in the subtree.
- **Deep structural inspection**: Query node type with `isNull()`, `isArray()`, `isObject()`, `isScalar()`, and `size()`, or check key presence with `hasKey()`, `hasAllKeys()`, and `hasAnyKey()`.
- **Declarative schema validation**: Enforce structural rules inline: required keys, allowed-key whitelists, and isolated structure validation blocks with `requireStructure()`.
- **Custom exception suppliers**: Inject your own exception factories into any `require*` call for integration with application-specific error handling.
- **Default value suppliers**: Provide `Supplier<T>` fallbacks to any `find*` call for clean, null-free value resolution.
- **Mutation API**: Update existing keys with `put()`, remove keys with `remove()`, and chain node construction with `merge()`.
- **Native Java conversion**: Export tree structures to standard `Map<String, Object>` or `List<Object>` with `toMap()` and `toList()`.
- **Object mapping via `JSONSerializable`**: Implement `JSONSerializable<T>` on your model classes to enable reflection-based serialization and custom deserialization.
- **Functional deserialization**: Convert any node to a typed object using `toObject(Function<JSONObject, T>)` without requiring an interface.
- **Structural equality and copying**: Compare nodes structurally with `equals()` and deep-clone entire subtrees with `clone()`.
- **Pretty printing**: Produce human-readable, multi-line, indented JSON serialization with `toString()`, suitable for logging and file output.
- **Full Unicode support**: Strings support the complete Unicode range, including all JSON escape sequences and surrogate pairs.

## Requirements

- Java 17 or higher
- Maven 3.x or Gradle 7.x

## Installation

### Maven
```xml
<dependency>
    <groupId>io.github.stevenlagoy</groupId>
    <artifactId>jsonic</artifactId>
    <version>2.0.0</version>
</dependency>
```

### Gradle (Groovy DSL)
```groovy
dependencies {
    implementation 'io.github.stevenlagoy:jsonic:2.0.0'
}
```

### Gradle (Kotlin DSL)
```kotlin
dependencies {
    implementation("io.github.stevenlagoy:jsonic:2.0.0")
}
```

## Usage

### 1. JSON Parsing & Instantiation

Parse from a local JSON file:
```java
Path jsonPath = Path.of("path", "to", "file.json");
JSONObject json = new JSONObject(jsonPath);
```

Parse from a raw string:
```java
String rawJson = "{\"title\": \"Jsonic\",\"version\": 2}";
JSONObject json = new JSONObject("root", List.of(rawJson));
```

Parse from multiple lines (e.g. from a file reader):
```java
List<?> lines = Files.readAllLines(Path.of("data.json"));
JSONObject json = new JSONObject("data", lines);
```

In **Kotlin**:
```kotlin
val jsonFromFile   = JSONObject(Path.of("config", "settings.json"))
val jsonFromString = JSONObject("root", listOf("{\"title\": \"Jsonic\"}"))
```

### 2. Direct Node Access

Access the value of the current node directly, without searching the tree:
```java
JSONObject node = new JSONObject("score", 99);

Integer score   = node.getInt();    // 99
String asString = node.getString(); // null -- value is not a string
```

Use `require*()` to assert the type and throw if it doesn't match:
```java
int score = node.requireInt();    // 99
String s  = node.requireString(); // throws IllegalArgumentException
```

Provide a custom exception for integration with your error handling:
```java
int level = node.requireInt(() -> new IllegalArgumentException("'level' field must be a number"));
```

### 3. Searching the Subtree

`find*()` methods search depth-first across the entire subtree rooted at the current node and return `Optional` results. `require*()` variants throw if nothing is found.

**Note**: `find*()` searches the *entire subtree*, not just the immediate children of the current node. If a kye appears at multiple levels of nesting, the first depth-first match is returned. Use path-based navigation (section 4) to target a specific level.

```java
JSONObject json = new JSONObject(Path.of("player.json"));

// Optional lookup -- safe to call on any node
Optional<String>  name  = json.findString("name");
Optional<Integer> level = json.findInt("level");

// Lookup with a default value supplier
String displayName = json.findString("displayName", () -> "Anonymous");
double rating      = json.findDouble("rating", () -> 0.0);
int    maxRetries  = json.findInt("maxRetries", () -> 3);

// Strict lookup -- throws with a descriptive message if not found
String id  = json.requireString("id");
int health = json.requireInt("health");

// Strict lookup with a custom exception
String token = json.requireString("authToken", () -> new IllegalArgumentException("Authentication token is required"));
```

Search across multiple fallback keys in priority order:
```java
// Returns the value of the first key found: "email", then "phone", then "username"
Optional<String> contact = json.findString(List.of("email", "phone", "username"));
String contactRequired = json.requireString(List.of("email", "phone", "username"));

// With a default supplier
String contactOrDefault = json.findString(List.of("email", "phone", "username"), () -> "no-contact-provided");
```

In **Kotlin**, `Optional` interops cleanly with `orElse` and `orElseGet`, and the supplier-based overload avoid `Optional` entirely:
```kotlin
val name    = json.findString("name").orElse("Unknown")
val contact = json.findString(listOf("email", "phone")).orElse(null)

// With a default supplier
val display = json.findString("displayName") { "Anonymous" }
val retries = json.findInt("maxRetries") { 3 }
```

### 4. Path-Based Navigation

Navigate nested structures using dot-separated paths or vararg segments:
```java
JSONObject json = new JSONObject(Path.of("organization.json"));

// Dot-separated path
Optional<String> city = json.findStringAt("address.city");
double lat = json.requireDoubleAt("location.coordinates.lat");

// Varargs path -- equivalent to the above
Optional<String> city2 = json.findStringAt("address", "city");

// Keys that contain dots can be escaped with a backslash
Optional<String> temp = json.findStringAt("dates.2024\\.01\\.15.temperature");

// Navigate to a node rather than its value
Optional<JSONObject> addressNode = json.findNodeAt("address");
JSONObject required = json.requireNodeAt("address");
```

In **Kotlin**:
```kotlin
val city = json.findStringAt("address.city").orElse("Unknown")
val lat  = json.requireDoubleAt("location.coordinates.lat")
```

### 5. Structural Inspection

```java
JSONObject node = new JSONObject(Path.of("response.json"));

node.isNull();   // true if value is null
node.isScalar(); // true if value is a String, Number, or Boolean
node.isArray();  // true if value is a list of primitives
node.isObject(); // true if value is a list of JSONObjects
node.size();     // number of elements (0 for null, 1 for scalar, n for list)

// Key presence -- correctly returns true even for null-valued keys
boolean hasId  = node.hasKey("id");
boolean hasAny = node.hasAnyKey("email", "phone");
boolean hasAll = node.hasAllKeys("id", "name", "version");

// Immediate child keys of this node
List<?> keys = node.getFieldKeys();
```

### 6. Declarative Schema Validation

Validate structure inline before processing:
```java
JSONObject payload = new JSONObject(Path.of("request.json"));

// Assert required keys exist (throws IllegalArgumentException if missing)
payload.requireKey("id");
payload.requireAllKeys("title", "version", "organization");
payload.requireAnyKey("apiKey", "auth_token");

// Reject unexpected keys
payload.requireAllowedKeys(List.of("id", "title", "version", "organization"));

// Validate a nested object in isolation
payload.requireStructure("organization", org -> {
    org.requireKey("name");
    org.requireKey("address");
    org.requireAllowedKeys(List.of("name", "address", "contactEmail"));
});
```

With custom exceptions:
```java
payload.requireKey("id", () -> new IllegalStateException("Request payload must include 'id'"));

payload.requireAllKeys(
    List.of("title", "version"),
    () -> new IllegalStateException("Payload is missing required metadata fields"));
```

### 7. Implementing `JSONSerializable`

`JSONSerializable<T>` is an interface that adds standardized JSON serialization and deserialization to your model classes. Implementing it requires two methods:

- **`toJson()`**: Converts this object into a `JSONObject` tree. The static helper `JSONSerializable.toJson(Object)` uses reflection to automatically map all public non-static fields, which is sufficient for simple models. For more control, build the `JSONObject` manually.
- **`fromJson(JSONObject)`**: Reads field values from a `JSONObject`, applies them to `this`, and returns `this`. This pattern allows the same instance to be reused as a deserialization target, and enables the JSON constructor pattern described below.

A common and recommended pattern is to add a constructor that accepts a `JSONObject` and deserializes itself immediately:
```java
public MyClass(JSONObject json) {
    this(); // Call no-arg (or another) constructor to set defaults
    this.fromJson(json); // Deserialize
}
```
This allows clean deserialization at the call site: `new MyClass(json)`.

#### Simple model
```java
public class Tag implements JSONSerializable<Tag> {

    public String name;
    public String color;

    public Tag() {}

    public Tag(JSONObject json) {
        this();
        this.fromJson(json);
    }

    @Override
    public JSONObject toJson() {
        // Reflects all public non-static fields automatically
        return JSONSerializable.toJson(this);
    }

    @Override
    public Tag fromJson(JSONObject json) {
        this.name  = json.requireString("name");
        this.color = json.findString("color", () -> "#000000");
        return this;
    }
}
```

Usage:
```java
Tag tag = new Tag(json);            // JSON constructor
Tag tag = new Tag().fromJson(json); // Equivalent explicit form
```

#### Model with custom `toJson` and nested `JSONSerializable` fields

When your class has private fields, computed properties, or fields you want
to exclude, build the `JSONObject` manually instead of using
`JSONSerializable.toJson(this)`:
```java
public class Article implements JSONSerializable<Article> {

    public String    title;
    public int       wordCount;
    public boolean   published;
    public Tag       primaryTag;  // nested JSONSerializable
    public List<Tag> relatedTags; // collection of JSONSerializable

    public Article() {}

    public Article(JSONObject json) {
        this();
        this.fromJson(json);
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject(getClass().getSimpleName());

        json.put("title",     title);
        json.put("wordCount", wordCount);
        json.put("published", published);

        // Nested object -- serialized recursively
        if (primaryTag != null) {
            json.put("primaryTag", primaryTag.toJson());
        }

        // Collection -- convert each element
        if (relatedTags != null) {
            json.put("relatedTags", JSONSerializable.collectionToJson(relatedTags));
        }

        return json;
    }

    @Override
    public Article fromJson(JSONObject json) {
        this.title     = json.requireString("title");
        this.wordCount = json.findInt("wordCount").orElse(0);
        this.published = json.findBoolean("published").orElse(false);

        // Deserialize nested object
        json.findJson("primaryTag")
            .ifPresent(tagJson -> this.primaryTag = new Tag().fromJson(tagJson));

        // Deserialize collection
        this.relatedTags = new ArrayList<>();
        json.findArray("relatedTags").ifPresent(arr -> {
            for (Object item : arr) {
                if (item instanceof JSONObject tagJson) {
                    this.relatedTags.add(new Tag(tagJson));
                }
            }
        });

        return this;
    }
}
```

#### Inheritance hierarchies

Only the parent class may implement `JSONSerializable<T>`. Subclasses extend the parent's serialization by calling `super.toJson()` and `super.fromJson()`, then handling their own additional fields.

```java
public class FeaturedArticle extends Article {

    public String featuredImageUrl;
    public int    featuredRank;

    public FeaturedArticle() {}

    public FeaturedArticle(JSONObject json) {
        this();
        this.fromJson(json);
    }

    @Override
    public JSONObject toJson() {
        // Start with the parent's serialization, then add subclass fields
        JSONObject json = super.toJson();
        json.put("featuredImageUrl", featuredImageUrl);
        json.put("featuredRank",     featuredRank);
        return json;
    }

    @Override
    public FeaturedArticle fromJson(JSONObject json) {
        // Apply parent fields first, then subclass fields
        super.fromJson(json);
        this.featuredImageUrl = json.findString("featuredImageUrl", () -> null);
        this.featuredRank     = json.findInt("featuredRank", () -> 0);
        return this;
    }
}
```
This pattern naturally extends to depper hierarchies: each level calls its parent's methods and handles only its own fields.

#### Kotlin: Simple Model

Kotlin's `val` properties cannot be assigned after construction, so fields that will be populated by `fromJson()` should be declared as `var` with `internal set` to allow mutation from within the class while keeping them read-only externally:
```kotlin
class Tag(
    name: String = "",
    color: String = "#000000",
) : JSONSerializable<Tag> {
    
    var name: String = name
        internal set
    var color: String = color
        internal set

    constructor(json: JSONObject) : this() {
        fromJson(json)
    }

    override fun toJson() = JSONSerializable.toJson(this)

    override fun fromJson(json: JSONObject) = this.apply {
        name  = json.requireString("name")
        color = json.findString("color") { "#000000" }
    }
}
```

Usage:
```kotlin
val tag = Tag(json)            // JSON constructor
val tag = Tag().fromJson(json) // Equivalent explicit form
```

#### Kotlin: Custom `toJson`

When your class has private fields, computed properties, or fields you want to exclude, build the `JSONObject` manually instead of using `JSONSerializable.toJson(this)`:
```kotlin
class Product(
    name: String = "",
    var price: Double = 0.0,
    var inStock: Boolean = true,
) : JSONSerializable<Product> {

    var name: String = name
        internal set

    constructor(json: JSONObject) : this() {
        fromJson(json)
    }

    override fun toJson() = JSONObject(
        requireNotNull(this::class.simpleName), // Anonymous classes cannot be serialized; remember that keys in a JSON layer must be unique
        listOf(
            JSONObject("name",    name),
            JSONObject("price",   price),
            JSONObject("inStock", inStock),
        )
    )

    override fun fromJson(json: JSONObject) = apply {
        name    = json.requireString("name")
        price   = json.findDouble("price") { 0.0.also { println("Could not find price for $name") } } // Scope functions for additional logging
        inStock = json.findBoolean("inStock") { true }
    }
}
```

#### Kotlin: Inheritance
The parent class implements `JSONSerializable<T>` directly. Its `toJson` and `fromJson` handle only its own fields:
```kotlin
open class Article(
    title: String = "",
    wordCount: Int = 0,
    var published: Boolean = false,
) : JSONSerializable<Article> {

    var title: String = title
        internal set
    var wordCount: Int = wordCount
        internal set

    constructor(json: JSONObject) : this() {
        fromJson(json)
    }

    override fun toJson() = JSONObject(
        title,
        listOf(
            JSONObject("title",     title),
            JSONObject("wordCount", wordCount),
            JSONObject("published", published),
        )
    )

    override fun fromJson(json: JSONObject) = apply {
        title     = json.requireString("title")
        wordCount = json.findInt("wordCount") { 0.also { println("Could not find 'wordCount' for $title") } } // Scope functions for additional logging
        published = json.findBoolean("published") { false }
    }
}

class FeaturedArticle(
    title: String = "",
    wordCount: Int = 0,
    published: Boolean = false,
    featuredImageUrl: String = "",
    var featuredRank: Int = 0,
) : Article(title, wordCount, published) {

    var featuredImageUrl: String = featuredImageUrl
        internal set

    constructor(json: JSONObject) : this() {
        fromJson(json)
    }

    // super.toJson() builds the parent's node, then merge() appends child fields
    override fun toJson() = super.toJson().merge(
        JSONObject("featuredImageUrl", featuredImageUrl),
        JSONObject("featuredRank",     featuredRank),
    )

    // super.fromJson() populates parent fields first, apply continues with child fields
    override fun fromJson(json: JSONObject) = apply {
        super.fromJson(json)
        featuredImageUrl = json.findString("featuredImageUrl") { "" }
        featuredRank = json.findInt("featuredRank") { 0.also { println("Could not find 'featuredRank' for $title, assigning lowest priority") } }
    }
}
```

#### Kotlin: Class with a nested `JSONSerializable` field and a collection

```kotlin
class Campaign(
    name: String = "",
    primaryTag: Tag = Tag(),
    relatedTags: List = emptyList(),
) : JSONSerializable {

    var name: String = name
        internal set
    var primaryTag: Tag = primaryTag
        internal set
    var relatedTags: List = relatedTags
        internal set

    constructor(json: JSONObject) : this() {
        fromJson(json)
    }

    override fun toJson() = JSONObject(
        name,
        listOf(
            JSONObject("name",       name),
            JSONObject("primaryTag", primaryTag.toJson()),
            // collectionToJson calls toJson() on each JSONSerializable element
            // Consider adding an extension function `Collection.toJson()` in your project
            JSONObject("relatedTags", JSONSerializable.collectionToJson(relatedTags)),
        )
    )

    override fun fromJson(json: JSONObject) = apply {
        name       = json.requireString("name")
        primaryTag = json.findJson("primaryTag")
                         .map { Tag(it) }
                         .orElseGet { Tag() }
        relatedTags = json.findArray("relatedTags")
                          .map { arr -> arr.filterIsInstance().map { Tag(it) } }
                          .orElseGet { emptyList() }
    }
}
```

#### Serialization and deserialization in action
```java
Article article = new Article();
article.title            = "Getting Started with Jsonic";
article.wordCount        = 1200;
article.published        = true;
article.primaryTag       = new Tag();
article.primaryTag.name  = "java";
article.primaryTag.color = "#E76F00";

// Serialize to JSON tree
JSONObject json = article.toJson();
System.out.println(json);

// Deserialize from JSON tree
Article loaded = new Article().fromJson(json);
System.out.println(loaded.title); // "Getting Started with Jsonic"
```

#### Functional deserialization without `JSONSerializable`

If your class does not implement `JSONSerializable<T>`, `toObject()` applies a function to the `JSONObject` and returns the result. This is useful for one-off conversion or for integrating with classes you don't control:
```java
JSONObject json  = new JSONObject(Path.of("user.json"));

// Method reference: calls a static or instance method that accepts a JSONObject
User user  = json.toObject(User::fromJson);

// Lambda: any transformation that produces a value from a JSONObject
String label = json.toObject(j -> j.requireString("label"));
int count =    json.toObject(j -> j.findInt("count", () -> 0));
```

In **Kotlin**:
```kotlin
val user  = json.toObject(User::fromJson)
val label = json.toObject { it.requireString("label") }
val count = json.toObject { it.findInt("count" { 0 }) }
```

### 8. Mutating JSON

```java
JSONObject response = new JSONObject("response");

// put() initializes the node as an object container if needed,
// and replaces an existing key's value if one is already present.
// Returns the previous value, or null if the key was not present.
response.put("status", "success");
response.put("code", 200);
response.put("timestamp", System.currentTimeMillis());

// Remove a key: returns true if the key was found and removed
response.remove("timestamp");

// Merge child nodes: adds fields from another JSONObject into this one
JSONObject meta = new JSONObject("meta");
meta.put("version", "2.0.0");
meta.put("author", "stevenlagoy");
response.merge(meta);

System.out.println(response);
// {
//     "status" : "success",
//     "code" : 200,
//     "meta" : {
//         "version" : "2.0.0",
//         "author" : "stevenlagoy"
//     }
// }
```

### 9. Converting to Standard Java Types

`toMap()` and `toList()` convert the `JSONObject` tree into standard Java collections, which is useful for interoperability with other libraries or frameworks which expect plan `Map` and `List` types rather than `JSONObject`:
```java
JSONObject json = new JSONObject(Path.of("config.json"));

// Export the entire tree as a nested Map
// Nested objects become nested Maps; arrays become Lists
Map<String, Object> map = json.toMap();

// Export a specific array node as a List
List<Object> list = json.findNodeAt("tags")
                        .map(JSONObject::toList)
                        .orElse(Collections.emptyList());
```

## Migration from v1.x

Version 2.0.0 is a full rewrite. The following changes affect all v1.x users:

| v1.x                              | v2.0.0                                  |
|-----------------------------------|-----------------------------------------|
| `JSONProcessor.processJson(path)` | `new JSONObject(path)`                  |
| `JSONProcessor.processValue(str)` | `new JSONObject("key", List.of(str))`   |
| `json.getValue("key")`            | `json.find("key")`                      |
| `json.getString("key", default)`  | `json.findString("key", () -> default)` |
| `Jsonic<T>` interface             | `JSONSerializable<T>` interface         |

## Building from Source

```bash
git clone https://github.com/stevenlagoy/Jsonic.git
cd json-java-objectifier
mvn clean install
```

Or with Gradle:
```bash
./gradlew build
```

## Contributing

Contributions are welcome. Before opening a pull request, please check the open issues to see if the problem or feature is already being tracked.

### Reporting Issues

If you find a bug or have a feature request, open an issue on GitHub:

1. Go to the [Issues](https://github.com/stevenlagoy/Jsonic/issues) tab
2. Click **New Issue** and choose the appropriate template (Bug Report or Feature Request)
3. Include as much detail as possible: your Java version, a minimal reproduction case for bugs, and the expected vs. actual behavior
4. Label the issue appropriately if you have permission to do so

### Submitting Changes

1. Fork the repository and create your branch from `main`:
```bash
   git checkout -b feature/my-feature
   # or
   git checkout -b fix/issue-123
```
2. Make your changes. For bug fixes, add a test that would have caught the bug. For new features, add tests covering the new behavior.
3. Ensure all existing tests pass:
```bash
   mvn test
   # or
   ./gradlew test
```
4. Commit your changes with a clear message:
```bash
   git commit -m "Fix: correct null handling in findInt for missing keys"
   git commit -m "Feature: add findNodeAt path navigation"
```
5. Push your branch and open a Pull Request against `main`
6. In the Pull Request description, reference any related issues (e.g. `Closes #42`) and summarize what changed and why

### Code Style

- Follow the existing formatting conventions in the codebase
- All public methods must have Javadoc
- Null safety annotations (`@NotNull`, `@Nullable`) are required on all method parameters and return types
- Keep changes focused: one concern per pull request

## License

This project is licensed under the Apache 2.0 License -- see `LICENSE.md` for details.

## Acknowledgements

- Built with Java 17
- JUnit for testing
- Maven for build management
