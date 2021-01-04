package com.jz.jsonpath;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Utils {

    public static List<JsonElement> jsonArrayToList(JsonArray expected) {
        List<JsonElement> jsonElements = new ArrayList<JsonElement>(expected.size());
        for (int i = 0; i < expected.size(); ++i) {
            jsonElements.add(expected.get(i));
        }
        return jsonElements;
    }

    /**
     * Creates a cardinality map from {@code coll}.
     *
     * @param coll the collection of items to convert
     * @param <T>  the type of elements in the input collection
     * @return the cardinality map
     */
    public static <T> Map<T, Integer> getCardinalityMap(final Collection<T> coll) {
        Map map = new HashMap<T, Integer>();
        for (T item : coll) {
            Integer c = (Integer) (map.get(item));
            if (c == null) {
                map.put(item, new Integer(1));
            } else {
                map.put(item, new Integer(c.intValue() + 1));
            }
        }
        return map;
    }

    /**
     * Searches for the unique key of the {@code expected} JSON array.
     *
     * @param array the array to find the unique key of
     * @return the unique key if there's any, otherwise null
     */
    public static String findUniqueKey(JsonArray array) {
        // Find a unique key for the object (id, name, whatever)
        if (array.size() > 0 && (array.get(0) instanceof JsonObject)) {
            JsonObject o = ((JsonObject) array.get(0)).getAsJsonObject();
            Set<String> keys = getKeys(o);
            for (String candidate : keys) {
                if (isUsableAsUniqueKey(candidate, array)) {
//                    System.out.println("The unique key of JsonArray is ["  + candidate +"]");
                    return candidate;
                }
            }
        }

        return null;
    }


    /**
     * @param candidate is usable as a unique key if every element in the
     * @param array     is a JSONObject having that key, and no two values are the same.
     * @return true if the candidate can work as a unique id across array
     */

    public static boolean isUsableAsUniqueKey(String candidate, JsonArray array) {
        Set<JsonElement> seenValues = new HashSet<JsonElement>();
        for (int i = 0; i < array.size(); i++) {
            JsonElement item = array.get(i);
            if (item instanceof JsonObject) {
                JsonObject o = (JsonObject) item;
                if (o.has(candidate)) {
                    JsonElement value = o.get(candidate);
                    if (isSimpleValue(value) && !seenValues.contains(value)) {
                        seenValues.add(value);
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        return true;
    }

    //get keys from a JsonObject
    public static Set<String> getKeys(JsonObject o) {
        Set<String> keys = new TreeSet<String>();
        for (Map.Entry<String, JsonElement> entry : o.entrySet()) {
            keys.add(entry.getKey().trim());
        }

        return keys;
    }

    //JsonPrimitive value as unique key
    public static boolean isSimpleValue(Object o) {
        return !(o instanceof JsonObject) && !(o instanceof JsonArray) && (o instanceof JsonPrimitive);
    }

    // build hashmap, KEY is UniqueKey's Value, VALUE is JsonObject;
    public static Map<JsonPrimitive, JsonObject> arrayOfJsonObjectToMap(JsonArray array, String uniqueKey) {
        Map<JsonPrimitive, JsonObject> valueMap = new HashMap<JsonPrimitive, JsonObject>();
        if (uniqueKey != null) {
            for (int i = 0; i < array.size(); ++i) {
                JsonObject jsonObject = (JsonObject) array.get(i);
                JsonPrimitive id = jsonObject.get(uniqueKey).getAsJsonPrimitive();
                valueMap.put(id, jsonObject);
            }
        }

        return valueMap;
    }

    //    refactor, using try with resource statement
    public static String convertFormattedJson2Raw(File f) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String json = br.readLine();
            StringBuilder sb = new StringBuilder();
            while (json != null && json.length() > 0) {
                json = json.trim();
                while (json.startsWith("\t")) {
                    json = json.replaceFirst("\t", "");
                }
                sb.append(json);
                json = br.readLine();
            }
            br.close();

            return sb.toString().trim();
        }

    }


    public static boolean allSimpleValues(JsonArray array) {
        for (int i = 0; i < array.size(); ++i) {
            if (!isSimpleValue(array.get(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean allJsonObjects(JsonArray array) {
        for (int i = 0; i < array.size(); ++i) {
            if (!(array.get(i) instanceof JsonObject)) {
                return false;
            }
        }
        return true;
    }

    // save JsonArray to map, in order to reduce time complexibility
    public static Map<String, JsonArray> getJsonArrayMap(JsonObject source , boolean ignoreCase)  {
        Map<String, JsonArray> result = new LinkedHashMap<>();
        if(source == null || source.isJsonNull() || !source.isJsonObject()) {
            return result;
        }

        Queue<JsonElementWithPath> queue = new LinkedList<JsonElementWithPath>();
        queue.offer(new JsonElementWithPath(source, "$"));
        while (!queue.isEmpty()) {
            int size = queue.size();
            //Traverse by level
            for (int i = 0; i < size; i++) {
                JsonElementWithPath org = queue.poll();
                String currentLevel = org.getLevel();
                JsonElement je = org.getJsonElement();

                if (je.isJsonArray()) {
                    JsonArray ja = je.getAsJsonArray();
                    result.put(currentLevel, ja);
                    for (int j = 0; j < ja.size(); j++) {
                        String level = currentLevel + "[" + j + "]";
                        if(ignoreCase) {
                            level = level.toLowerCase();
                        }
                        JsonElementWithPath tmp = new JsonElementWithPath(ja.get(j), level);
                        queue.offer(tmp);
                    }
                } else if (je.isJsonObject()) {
                    JsonObject jo = je.getAsJsonObject();
                    for (Map.Entry<String, JsonElement> entry : jo.entrySet()) {
                        String level = currentLevel + "." + entry.getKey();
                        if(ignoreCase) {
                            level = level.toLowerCase();
                        }
                        JsonElementWithPath tmp = new JsonElementWithPath(entry.getValue(), level);
                        queue.offer(tmp);
                    }
                }
            }
        }

        return result;
    }


}

