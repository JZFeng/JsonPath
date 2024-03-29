# Background
When I was working in eBay ViewItem team, for backend service testing, most times, we are manipulating Json files. 
One important scenario is , passing a standard JsonPath, get the JsonElements and do the assertions.

It is difficult for me to use existent jayway JsonPath. So I decide to implement my own JsonPath to have more features.

Phase 1: implement the JsonArray slice to finish my checkoutURL testing JIRA ID : GOTHAM-210
Phase 2: implement the filter function, something like " textSpans[?(@.text =~ \"(.*)\\d{3,}(.*)\" || @.text in {\"Have a nice day\", \"Return policy\"})]"
Phase 3: implement the ignore Case and ignore JsonPaths function to finish mWeb Redirection testing
NOTE: ignoring JsonPaths is an expensive action if the JsonPath is partial JsonPaths. Time complex is O(N2).


# Reinvent the wheels?
I would not call it "Reinventing the wheels " because the existent JayWay jsonpath does not have the features that I want.
My requirement is quite simple:
passing a standard JsonPath, giving me a list of JsonElement along with complete JsonPath.
If it contains some JsonElements that i do not want, simply passing a string array to remove them from the result.
If I want result to be case-insensitive, please ignore case for the JsonPath.

``` java
public static List<JsonElementWithLevel> get(
            JsonObject source, 
            String path, 
            boolean ignoreCase, 
            String[] ignoredPaths) 

public class JsonElementWithLevel {
    private JsonElement jsonElement;
    private String level;

    public JsonElementWithLevel(JsonElement jsonElement, String level) {
        this.jsonElement = jsonElement;
        this.level = level;
    }
}
```

* It supports standard JsonPath. 
* It supports partial JsonPath, for example, if user enters "URL", it gets all the JsonElements that has "URL" as the key;
* It supports ignoring case, for example, if user enters "URL", it gets all the JsonElements that has "URL" / "url" / "Url" as the key;
* It supports ignoring some JsonPaths by passing JsonPath array;

            String[] ignoredPaths = new String[]{
                            "PICTURE.mediaList[0].image.originalImg.URL", 
                            "RETURNS.maxView.value[3].value[0].textSpans[0].action.URL" // 1
                            , "THIRD_PARTY_RESOURCES.js[0].url"  // 1
                            , "BINSUMMARY.minView.actions[1].action.URL" // 2
                            , "$.WATCH.watching.watchAction.action.URL" // 1
                            , "$.modules.WATCH.watch.watchAction.action.URL"  // 1
                            , "BINSUMMARY.minView.actions[2].value.cartSigninUrl.URL" // 2
                    }; 

# Sample JsonPaths
                "$.modules.BINSUMMARY.minView.actions[0]"
                "SELLERPRESENCE.sellerName.action.URL"
                "RETURNS.maxView.value.length()"
                "RETURNS.maxView.value[0:].label"
                "RETURNS.maxView.value[*].label.textSpans[0]"
                "RETURNS.maxView.value[1,3,4].label.textSpans[0].text"
                "RETURNS.maxView.value[1,3,4].label.textSpans[?(@.text == \"Refund\" || @.text == \"Return policy\")].text"
                "RETURNS.maxView.value[*].label.textSpans[?(@.text =~ \"(.*)\\d{3,}(.*)\" || @.text in {\"Have a nice day\", \"Return policy\"})]"
                "URL"
                "RETURNS.maxView.value[1:3]"
                "RETURNS.maxView.value[-3:-1]"
                "RETURNS.maxView.value[-2]"
 
JsonPath expressions always refer to a JSON structure in the same way as XPath expression are used in combination 
with an XML document. The "root member object" in JsonPath is always referred to as `$` regardless if it is an 
object or array.

JsonPath expressions can use the dot–notation

`$.store.book[0].title`

or the bracket–notation

`$['store']['book'][0]['title']`

Operators
---------

| Operator                  | Description                                                        |
| :------------------------ | :----------------------------------------------------------------- |
| `$`                       | The root element to query. This starts all path expressions.       |
| `@`                       | The current node being processed by a filter predicate.            |
| `*`                       | Wildcard. Available anywhere a name or numeric are required.       |
| `..`                      | Deep scan. Available anywhere a name is required.                  |
| `.<name>`                 | Dot-notated child                                                  |
| `['<name>' (, '<name>')]` | Bracket-notated child or children                                  |
| `[<number> (, <number>)]` | Array index or indexes                                             |
| `[start:end]`             | Array slice operator                                               |
| `[?(<expression>)]`       | Filter expression. Expression must evaluate to a boolean value.    |


Functions
---------

Functions can be invoked at the tail end of a path - the input to a function is the output of the path expression.
The function output is dictated by the function itself.

| Function                  | Description                                                        | Output    |
| :------------------------ | :----------------------------------------------------------------- |-----------|
| min() TO-DO               | Provides the min value of an array of numbers                      | Double    |
| max() TO-DO               | Provides the max value of an array of numbers                      | Double    |
| avg() TO-DO               | Provides the average value of an array of numbers                  | Double    |
| length()                  | Provides the length of an array                                    | Integer   |


Filter Operators
-----------------

Filters are logical expressions used to filter arrays. A typical filter would be `[?(@.age > 18)]` where `@` represents the current item being processed. More complex filters can be created with logical operators `&&` and `||`. String literals must be enclosed by single or double quotes (`[?(@.color == 'blue')]` or `[?(@.color == "blue")]`).   

| Operator                 | Description                                                       |
| :----------------------- | :---------------------------------------------------------------- |
| ==                       | left is equal to right (note that 1 is not equal to '1')          |
| !=                       | left is not equal to right                                        |
| <                        | left is less than right                                           |
| <=                       | left is less or equal to right                                    |
| >                        | left is greater than right                                        |
| >=                       | left is greater than or equal to right                            |
| =~                       | left matches regular expression  [?(@.name =~ /foo.*?/i)]         |
| in                       | left exists in right [?(@.size in ['S', 'M'])]                    |
| nin                      | left does not exists in right                                     |
| subsetof                 | left is a subset of right [?(@.sizes subsetof ['S', 'M', 'L'])]     |
| size                     | size of left (array or string) should match right                 |
| empty                    | left (array or string) should be empty                            |


Path Examples
-------------

Given the json

```javascript
{
    "store": {
        "book": [
            {
                "category": "reference",
                "author": "Nigel Rees",
                "title": "Sayings of the Century",
                "price": 8.95
            },
            {
                "category": "fiction",
                "author": "Evelyn Waugh",
                "title": "Sword of Honour",
                "price": 12.99
            },
            {
                "category": "fiction",
                "author": "Herman Melville",
                "title": "Moby Dick",
                "isbn": "0-553-21311-3",
                "price": 8.99
            },
            {
                "category": "fiction",
                "author": "J. R. R. Tolkien",
                "title": "The Lord of the Rings",
                "isbn": "0-395-19395-8",
                "price": 22.99
            }
        ],
        "bicycle": {
            "color": "red",
            "price": 19.95
        }
    },
    "expensive": 10
}
```

| JsonPath | Result |
| :------- | :----- |
$.store.book[*].author| The authors of all books     |
$..author                   | All authors                         |
$.store.*                  | All things, both books and bicycles  |
$.store..price             | The price of everything         |
$..book[2]                 | The third book                      |
$..book[-2]                 | The second to last book            |
$..book[0,1]               | The first two books               |
$..book[:2]                | All books from index 0 (inclusive) until index 2 (exclusive) |
$..book[1:2]                | All books from index 1 (inclusive) until index 2 (exclusive) |
$..book[-2:]                | Last two books                   |
$..book[2:]                | Book number two from tail          |
$..book[?(@.isbn)]          | All books with an ISBN number         |
$.store.book[?(@.price < 10)] | All books in store cheaper than 10  |
$..book[?(@.price <= $['expensive'])] | All books in store that are not "expensive"  |
$..book[?(@.author =~ /.*REES/i)] | All books matching regex (ignore case)  |
$..*                        | Give me every thing   
$..book.length()                 | The number of books                      |

