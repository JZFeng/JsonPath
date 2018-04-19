package com.jz.json.jsonpath;

import com.google.gson.*;

import java.io.File;
import java.util.*;

import static com.jz.json.jsonpath.Utils.getKeys;

/**
 * @author jzfeng
 * <p>
 * get(JsonObject source, String path), support standard JsonPath;
 * Here are some sample JsonPaths
 * String[] paths = new String[]{
        "$.modules.BINSUMMARY.minView.actions[0]",
        "modules.SELLERPRESENCE.sellerName.action.URL",
        "RETURNS.maxView.value.length()",
        "RETURNS.maxView.value[0:].label",
        "RETURNS.maxView.value[*].label.textSpans[0]",
        "RETURNS.maxView.value[1,3,4].label.textSpans[0].text",
        "RETURNS.maxView.value[1,3,4].label.textSpans[?(@.text == \"Refund\" || @.text == \"Return policy\")].text",
        "RETURNS.maxView.value[*].label.textSpans[?(@.text =~ \"(.*)\\d{3,}(.*)\" || @.text in {\"Have a nice day\", \"Return policy\"})]",
        "URL" };
 */

public class JsonPath {

    public static void main(String[] args) throws Exception {

        JsonParser parser = new JsonParser();
        String json = Utils.convertFormattedJson2Raw(new File("/Users/jzfeng/Desktop/O.json"));
        JsonObject source = parser.parse(json).getAsJsonObject();

        String[] paths = new String[]
                {"$.modules.BINSUMMARY.minView.actions[0]",
                        "modules.SELLERPRESENCE.sellerName.action.URL",
                        "RETURNS.maxView.value.length()",
                        "RETURNS.maxView.value[0:].label",
                        "RETURNS.maxView.value[*].label.textSpans[0]",
                        "RETURNS.maxView.value[1,3,4].label.textSpans[0].text",
                        "RETURNS.maxView.value[1,3,4].label.textSpans[?(@.text == \"Refund\" || @.text == \"Return policy\")].text",
                        "RETURNS.maxView.value[*].label.textSpans[?(@.text =~ \"(.*)\\d{3,}(.*)\" || @.text in {\"Have a nice day\", \"Return policy\"})]",
                        "URL"
                };

        for (String path : paths) {
            long startTime = System.currentTimeMillis();
            List<JsonElementWithLevel> res = get(source, path, false);
            System.out.println("****" + res.size() + "****" + (long) (System.currentTimeMillis() - startTime) + "ms");
            for (JsonElementWithLevel je : res) {
                System.out.println(je);
            }
        }
    }


    public static List<JsonElementWithLevel> get(
            String source, String path) throws Exception {
        List<JsonElementWithLevel> res = new ArrayList<>();
        return get(source, path, false);
    }

    /**
     * @author jzfeng
     * @param source
     * @param path
     * @return
     * @throws Exception
     */
    public static List<JsonElementWithLevel> get(
            String source, String path, boolean ignoreCase) throws Exception {
        List<JsonElementWithLevel> res = new ArrayList<>();
        if(source == null  || source.length() == 0 || path == null || path.length() == 0) {
            return res;
        }

        JsonParser parser = new JsonParser();
        JsonObject src = parser.parse(source).getAsJsonObject();
        res = get(src, path, ignoreCase);

        return res;
    }


    /**
     * @param source the source of JsonObject
     * @param path   standard json path;
     * @return
     * @throws Exception
     * @author jzfeng
     */
    public static List<JsonElementWithLevel> get(JsonObject source, String path) throws Exception {
        return get(source, path, false);
    }


    /**
     * @param source     the source of JsonObject
     * @param path       standard json path;
     * @param ignoreCase if true, it will ignore the case of path; if false, it will strictly match path;
     * @return returns a a list of {@link JsonElementWithLevel}
     * @author jzfeng
     */
    public static List<JsonElementWithLevel> get(
            JsonObject source, String path, boolean ignoreCase) throws Exception {
        List<JsonElementWithLevel> result = new ArrayList<>();
        if (path == null || path.length() == 0 || source == null || source.isJsonNull()) {
            return result;
        }

        Map<String, List<Filter>> filters = getFilters(path, ignoreCase); // generate filters from path;;

        String regex = null;
        if (ignoreCase) {
            regex = generateRegex(path.toLowerCase());
        } else {
            regex = generateRegex(path);
        }

        Map<String, List<Filter>> matchedFilters = new LinkedHashMap<>(); //filters with absolute path;
        Map<String, JsonArray> cachedJsonArrays = new LinkedHashMap<>(); // save JsonArray to map, in order to reduce time complexibility

        boolean isAbsolutePath = isAbsoluteJsonPath(path, source);
        boolean isFinished = false;

        // to support length() function;
        if (path.matches("(.*)(\\.length\\(\\)$)")) { // case: [path == $.listing.termsAndPolicies.length()]
            path = path.replaceAll("(.*)(\\.length\\(\\)$)", "$1");
            int length = length(source, path, ignoreCase);
            result.add(new JsonElementWithLevel(new JsonPrimitive(length), path));
            return result;
        }


        Queue<JsonElementWithLevel> queue = new LinkedList<JsonElementWithLevel>();
        queue.offer(new JsonElementWithLevel(source, "$"));

        while (!queue.isEmpty()) {
            int size = queue.size();
            //Traverse by level
            for (int i = 0; i < size; i++) {
                JsonElementWithLevel org = queue.poll();
                String currentLevel = org.getLevel();
                JsonElement je = org.getJsonElement();
//                System.out.println(currentLevel);

                if (je.isJsonArray()) {
                    JsonArray ja = je.getAsJsonArray();
                    cachedJsonArrays.put(currentLevel, ja);
                    int length = ja.size();
                    for (int j = 0; j < ja.size(); j++) {
                        String level = currentLevel + "[" + j + "]";
                        JsonElementWithLevel tmp = new JsonElementWithLevel(ja.get(j), level);
                        queue.offer(tmp);
                        if (ignoreCase) {
                            updateMatchedFilters(level.toLowerCase(), filters, matchedFilters);
                            if (level.toLowerCase().matches(regex)) {
                                isFinished = true;
                                if (isMatchingFilters(cachedJsonArrays, level, matchedFilters, length, ignoreCase)) {
                                    result.add(tmp);
                                }
                            }
                        } else {
                            updateMatchedFilters(level, filters, matchedFilters);
                            if (level.matches(regex)) {
                                isFinished = true;
                                if (isMatchingFilters(cachedJsonArrays, level, matchedFilters, length, ignoreCase)) {
                                    result.add(tmp);
                                }
                            }
                        }
                    }
                } else if (je.isJsonObject()) {
                    JsonObject jo = je.getAsJsonObject();
                    int length = jo.entrySet().size();
                    for (Map.Entry<String, JsonElement> entry : jo.entrySet()) {
                        String level = currentLevel + "." + entry.getKey();
                        JsonElementWithLevel tmp = new JsonElementWithLevel(entry.getValue(), level);
                        queue.offer(tmp);
                        if (ignoreCase) {
                            updateMatchedFilters(level.toLowerCase(), filters, matchedFilters);
                            if (level.toLowerCase().matches(regex)) {
                                isFinished = true;
                                if (isMatchingFilters(cachedJsonArrays, level, matchedFilters, length, ignoreCase)) {
                                    result.add(tmp);
                                }
                            }
                        } else {
                            updateMatchedFilters(level, filters, matchedFilters);
                            if (level.matches(regex)) {
                                isFinished = true;
                                if (isMatchingFilters(cachedJsonArrays, level, matchedFilters, length, ignoreCase)) {
                                    result.add(tmp);
                                }
                            }
                        }
                    }
                }
            }

            // current level is BFS done which means all possible candidates are already captured in the result,
            // end BFS by directly returning result;
            if (isAbsolutePath && isFinished) {
                return result;
            }

        }


        return result;
    }



    /**
     * @param currentLevel   $.courses[i].grade
     * @param matchedFilters
     * @return true if i in matchedRange();
     */
    private static boolean isMatchingFilters(
            Map<String, JsonArray> cachedJsonArrays,
            String currentLevel,
            Map<String, List<Filter>> matchedFilters,
            int length,
            boolean ignoreCase
                                            ) throws Exception {
        if (matchedFilters == null || matchedFilters.size() == 0) {
            return true;
        }


        StringBuilder prefix = new StringBuilder();
        StringBuilder prepath = new StringBuilder();
        int index = 0;
        while ((index = currentLevel.indexOf('[')) != -1) {
            prepath.append(currentLevel.substring(0, currentLevel.indexOf(']') + 1));
            prefix.append(currentLevel.substring(0, index) + "[]");
            int i = Integer.parseInt(currentLevel.substring(index + 1, currentLevel.indexOf(']')));
            List<Filter> filters = null;
            if (ignoreCase) {
                filters = matchedFilters.get(prefix.toString().trim().toLowerCase());
            } else {
                filters = matchedFilters.get(prefix.toString().trim());
            }

            if (filters == null || filters.size() == 0) {
                return true;
            }

            boolean isMatched = false;
            if (filters.get(0) instanceof Range) {
                isMatched = isMatchingRange(filters, i, length);
            } else if (filters.get(0) instanceof Condition) {
                List<Condition> c = new ArrayList<>();
                for (Filter f : filters) {
                    c.add((Condition) f);
                }

                JsonArray jsonArray = cachedJsonArrays.get(prepath.substring(0, prepath.lastIndexOf("[")));
                isMatched = isMatchingConditions(jsonArray.get(i).getAsJsonObject(), c);
            }

            if (isMatched) {
                currentLevel = currentLevel.substring(currentLevel.indexOf(']') + 1);
            } else {
                return false;
            }

        }

        return true;
    }

    /**
     * @param currentLevel
     * @param filters
     * @param matchedFilters
     */
    private static void updateMatchedFilters(
            String currentLevel, Map<String, List<Filter>> filters, Map<String, List<Filter>> matchedFilters) {

        StringBuilder prefix = new StringBuilder();
        int index = 0;
        while ((index = currentLevel.indexOf('[')) != -1) {
            //update matchedRanges
            prefix.append(currentLevel.substring(0, index) + "[]");
            if (!matchedFilters.containsKey(prefix)) {
                for (Map.Entry<String, List<Filter>> entry : filters.entrySet()) {
                    String key = entry.getKey();
                    List<Filter> value = entry.getValue();
                    int idx = prefix.indexOf(key);
                    if (idx != -1 && prefix.toString().substring(idx).equals(key)) {
                        matchedFilters.put(prefix.toString(), value);
                    }
                }
            }

            currentLevel = currentLevel.substring(currentLevel.indexOf(']') + 1);

        }
    }


    /**
     * to check whether $.modules.RETURNS.maxView.value[4] is matching the range in matchedRanges;
     *
     * @param i index of a JsonArray
     * @return return true if  i in any of the range [(0, 0), (3,3)], otherwise return false;
     */
    private static boolean isMatchingRange(
            List<Filter> filterList, int i, int length) throws Exception {
        if (filterList != null && filterList.size() > 0) {
            for (Filter filter : filterList) {
                if (filter instanceof Range) {
                    if (((Range) filter).getStart() < 0 && ((Range) filter).getEnd() < 0) {
                        if ((i - length) >= ((Range) filter).getStart() && (i - length) <= ((Range) filter).getEnd()) {
                            return true;
                        }
                    } else {
                        if (i >= ((Range) filter).getStart() && i <= ((Range) filter).getEnd()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Identify whether a JsonObject matches the conditions
     *
     * @param jo         an JsonArray element as a JsonObject,
     * @param conditions conditions;
     * @return
     */
    private static boolean isMatchingConditions(JsonObject jo, List<Condition> conditions) throws Exception {

        boolean result = isMatchingCondition(jo, conditions.get(0));

        for (int i = 1; i < conditions.size(); i++) {
            String logicalOperator = conditions.get(i - 1).getLogicalOperator();
            if (logicalOperator.equals("&&")) {
                result = result && (isMatchingCondition(jo, conditions.get(i)));
            } else if (logicalOperator.equals("||")) {
                result = result || (isMatchingCondition(jo, conditions.get(i)));
            }
        }

        return result;
    }


    //"<", ">", "<=", ">=", "==", "!=", "=~", "in", "nin", "subsetof", "size", "empty", "notempty"
    private static boolean isMatchingCondition(JsonObject jo, Condition condition) throws Exception {
        if (!condition.isValid()) {
            return false;
        }

        Set<String> keys = getKeys(jo);
        String left = condition.getLeft().trim();
        String operator = condition.getOperator().trim();
        String right = condition.getRight();
        if (!keys.contains(left)) {
            return false;
        }

        boolean result = false;
        JsonElement je = jo.get(left);
        String valueAsString = je.getAsString();
        String valueAsJE = je.toString();

        if (!Condition.OPERATORS.contains(operator)) {
            throw new Exception("Unsupported Operator : " + operator);
        }

        //to-do, refactoring, using enum??
        if (operator.equals("<")) {
            result = ((valueAsString.matches("(\\d+)\\.(\\d+)")) ? (Double.parseDouble(valueAsString) < Double.parseDouble(right)) : (Integer.parseInt(valueAsString) < Integer.parseInt(right)));
        } else if (operator.equals(">")) {
            result = ((valueAsString.matches("(\\d+)\\.(\\d+)")) ? (Double.parseDouble(valueAsString) > Double.parseDouble(right)) : (Integer.parseInt(valueAsString) > Integer.parseInt(right)));
        } else if (operator.equals(">=")) {
            result = ((valueAsString.matches("(\\d+)\\.(\\d+)")) ? (Double.parseDouble(valueAsString) >= Double.parseDouble(right)) : (Integer.parseInt(valueAsString) >= Integer.parseInt(right)));
        } else if (operator.equals("<=")) {
            result = ((valueAsString.matches("(\\d+)\\.(\\d+)")) ? (Double.parseDouble(valueAsString) <= Double.parseDouble(right)) : (Integer.parseInt(valueAsString) <= Integer.parseInt(right)));
        } else if (operator.equals("==")) {
            result = valueAsJE.equals(right);
        } else if (operator.equals("!=")) {
            result = !valueAsJE.equals(right);
        } else if (operator.equals("=~")) {
            result = valueAsJE.matches(right);
        } else if (operator.equals("in")) {
            String[] strs = right.trim().substring(1, right.length() - 1).split("\\s{0,},\\s{0,}");
            Set<String> set = new HashSet<>();
            for (String str : strs) {
                set.add(str.trim());
            }
            result = set.contains(valueAsJE);
        } else if (operator.equals("nin")) {
            String[] strs = right.trim().substring(1, right.length() - 1).split("\\s{0,},\\s{0,}");
            Set<String> set = new HashSet<>();
            for (String str : strs) {
                set.add(str.trim());
            }
            result = !set.contains(valueAsJE);
        } else if (operator.equals("subsetof")) {
            //if(je.isJsonPrimitive()) {
            //} else if (je.isJsonArray()) {
            //} else if (je.isJsonObject()) {
            //}
        } else if (operator.equals("size")) {
            if (je.isJsonPrimitive()) {
                result = (valueAsString.length() == Integer.parseInt(right));
            } else if (je.isJsonArray()) {
                result = (je.getAsJsonArray().size() == Integer.parseInt(right));
            } else if (je.isJsonObject()) {
                result = (je.getAsJsonObject().entrySet().size() == Integer.parseInt(right));
            }
        } else if (operator.equals("empty")) {
            if (je.isJsonPrimitive()) {
                result = valueAsString.equals("");
            } else if (je.isJsonArray()) {
                result = (je.getAsJsonArray().size() == 0);
            } else if (je.isJsonObject()) {
                result = (je.getAsJsonObject().entrySet().size() == 0);
            }
        } else if (operator.equals("notempty")) {
            if (je.isJsonPrimitive()) {
                result = !valueAsString.equals("");
            } else if (je.isJsonArray()) {
                result = (je.getAsJsonArray().size() != 0);
            } else if (je.isJsonObject()) {
                result = (je.getAsJsonObject().entrySet().size() != 0);
            }
        }

        return result;
    }


    private static String generateRegex(String path) {
        if (path.startsWith("$")) {
            path = path.substring(2);
        }

        StringBuilder prefix = new StringBuilder();
        int index = 0;
        while ((index = path.indexOf('[')) != -1) {
            prefix.append(path.substring(0, index) + "[]");
            path = path.substring(path.indexOf(']') + 1);
        }
        prefix.append(path);
        String regex = "(.*)(\\." + prefix.toString().trim().replaceAll("\\[\\]", "\\\\[.*\\\\]") + ")";

        return regex;
    }


    private static int length(JsonObject jsonObject, String path, boolean ignoreCase) throws Exception {
        if (path == null || path.length() == 0) {
            return 0;
        }
        if (jsonObject == null || jsonObject.isJsonNull() || !jsonObject.isJsonObject()) {
            return 0;
        }

        int length = 0;
        List<JsonElementWithLevel> result = get(jsonObject, path, ignoreCase);

        if (result == null || result.size() == 0) {
            length = 0;
        } else if (result.size() > 1) {
            throw new Exception("Please correct your json path to match a single JsonElement.");
        } else {
            JsonElement jsonElement = result.get(0).getJsonElement();
            if (jsonElement.isJsonObject()) {
                length = jsonElement.getAsJsonObject().entrySet().size();
            } else if (jsonElement.isJsonArray()) {
                length = jsonElement.getAsJsonArray().size();
            } else if (jsonElement.isJsonPrimitive()) {
                length = jsonElement.getAsJsonPrimitive().getAsString().length();
            }

        }

        return length;
    }

    /**
     * @param path sample path : $.modules.RETURNS.maxView.store[1,3].book[@.category > 'fiction' and @.price < 10 or @.color == \"red\"].textSpans[0].text
     * @return minView.actions[] : [(2,2),(3,3)]
     * minView.actions[].action[] : [(2,5)]
     */
    private static Map<String, List<Filter>> getFilters(String path, boolean ignoreCase) throws Exception {
        Map<String, List<Filter>> res = new LinkedHashMap<>();
        if (path == null || path.trim().length() == 0) {
            return res;
        }

        StringBuilder prefix = new StringBuilder();
        int index = 0;
        while ((index = path.indexOf('[')) != -1) {
            prefix.append(path.substring(0, index) + "[]");
            String r = path.substring(index + 1, path.indexOf(']')).trim();
            if (r.contains("@")) {
                //filters, "?(@.text =~ "(.*)\d{3,}(.*)" || @.text in {"Have a nice day", "Return policy"})"
                List<Filter> filters = Condition.getConditions(r);
                if (filters != null && filters.size() > 0) {
                    if (ignoreCase) {
                        res.put(prefix.toString().trim().toLowerCase(), new ArrayList<Filter>(filters));
                    } else {
                        res.put(prefix.toString().trim(), new ArrayList<Filter>(filters));
                    }
                }
            } else if (r.matches("(.*)([,:])(.*)") || r.contains("last()") || r.contains("first()") || r.contains("*") || r.matches("\\s{0,}(-{0,}\\d+)\\s{0,}")) {
                //filters, [2:],[-2],[1,3,5] etc
                List<Filter> filters = Range.getRange(r);
                if (filters != null && filters.size() > 0) {
                    if (ignoreCase) {
                        res.put(prefix.toString().trim().toLowerCase(), new ArrayList<Filter>(filters));
                    } else {
                        res.put(prefix.toString().trim(), new ArrayList<Filter>(filters));
                    }
                }
            } else {
                throw new Exception("Invalid JsonPath : " + path);
            }

            path = path.substring(path.indexOf(']') + 1);
        }

        return res;
    }

    private static boolean isAbsoluteJsonPath(String path, JsonObject source) {
        if (path == null || path.length() == 0) {
            throw new IllegalArgumentException("Jason path can not be empty.");
        }

        path = path.trim();
        if (path.startsWith("$")) {
            return true;
        }
        Set<String> keys = getKeys(source);
        int index = path.indexOf(".");
        if (index == -1) {
            return false;
        } else {
            return keys.contains(path.substring(0, index));
        }
    }
}
