package com.jz.json.jsonpath;

import com.google.gson.*;
import java.io.File;
import java.util.*;
import static com.jz.json.jsonpath.Range.getRange;
import static com.jz.json.jsonpath.Range.mergeRanges;
import static com.jz.json.jsonpath.Utils.getKeys;

/**
 * @author jzfeng
 * <p>
 * get(JsonObject source, String path), support standard JsonPath;
 * Here are some sample JsonPaths
 * String[] paths = new String[]{
 * "$.modules.BINSUMMARY.minView.actions[0]",
 * "modules.SELLERPRESENCE.sellerName.action.URL",
 * "RETURNS.maxView.value.length()",
 * "RETURNS.maxView.value[0:].label",
 * "RETURNS.maxView.value[*].label.textSpans[0]",
 * "RETURNS.maxView.value[1,3,4].label.textSpans[0].text",
 * "RETURNS.maxView.value[1,3,4].label.textSpans[?(@.text == \"Refund\" || @.text == \"Return policy\")].text",
 * "RETURNS.maxView.value[*].label.textSpans[?(@.text =~ \"(.*)\\d{3,}(.*)\" || @.text in {\"Have a nice day\", \"Return policy\"})]",
 * "URL" };
 */

public class JsonPath {

    public static void main(String[] args) throws Exception {

        JsonParser parser = new JsonParser();
        String json = Utils.convertFormattedJson2Raw(new File("/Users/jzfeng/Desktop/O.json"));
        JsonObject source = parser.parse(json).getAsJsonObject();

        String[] paths = new String[]{
                "$.modules.BINSUMMARY.minView.actions[0]",
                "SELLERPRESENCE.sellerName.action.URL",
                 "RETURNS.maxView.value.length()"
                , "RETURNS.maxView.value[0:].label"
                , "RETURNS.maxView.value[*].label.textSpans[0]"
                , "RETURNS.maxView.value[1,3,4].label.textSpans[0].text"
                , "RETURNS.maxView.value[1,3,4].label.textSpans[?(@.text == \"Refund\" || @.text == \"Return policy\")].text"
                , "RETURNS.maxView.value[*].label.textSpans[?(@.text =~ \"(.*)\\d{3,}(.*)\" || @.text in {\"Have a nice day\", \"Return policy\"})]"
                , "URL"
                , "RETURNS.maxView.value[1:3]"
                , "RETURNS.maxView.value[-3:-1]"
                , "RETURNS.maxView.value[-2]"
        };


        String[] ignoredPaths = new String[]{
                "PICTURE.mediaList[0].image.originalImg.URL",  //3
                "RETURNS.maxView.value[3].value[0].textSpans[0].action.URL" // 1
                , "THIRD_PARTY_RESOURCES.js[0].url"  // 1
                , "BINSUMMARY.minView.actions[1].action.URL", // 2
                "$.modules.WATCH.watching.watchAction.action.URL" // 1
                , "modules.WATCH.watch.watchAction.action.URL"  // 1
                , "BINSUMMARY.minView.actions[2].value.cartSigninUrl.URL" // 2
//                total = 11;
        };

        for (String path : paths) {
            long startTime = System.currentTimeMillis();
            List<JsonElementWithLevel> res = get(source, path, true, ignoredPaths);
            System.out.println("****" + res.size() + "****" + (long) (System.currentTimeMillis() - startTime) + "ms" + "," + path);
            for (JsonElementWithLevel je : res) {
                System.out.println(je);
            }
        }
    }


    public static List<JsonElementWithLevel> get(
            String source, String path) throws Exception {
        List<JsonElementWithLevel> res = new ArrayList<>();
        return get(source, path, false, new String[]{});
    }

    /**
     * @param source
     * @param path
     * @return
     * @throws Exception
     * @author jzfeng
     */
    public static List<JsonElementWithLevel> get(
            String source, String path, boolean ignoreCase, String[] ignoredPaths) throws Exception {
        List<JsonElementWithLevel> res = new ArrayList<>();
        if (source == null || source.length() == 0 || path == null || path.length() == 0) {
            return res;
        }

        JsonParser parser = new JsonParser();
        JsonObject src = parser.parse(source).getAsJsonObject();
        res = get(src, path, ignoreCase, ignoredPaths);

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
        return get(source, path, false, new String[]{});
    }

    /**
     * @param source     the source of JsonObject
     * @param path       standard json path;
     * @param ignoreCase if true, it will ignore the case of path; if false, it will strictly match path;
     * @return returns a a list of {@link JsonElementWithLevel}
     * @author jzfeng
     */
    public static List<JsonElementWithLevel> get(
            JsonObject source, String path, boolean ignoreCase, String[] ignoredPaths) throws Exception {
        List<JsonElementWithLevel> result = new ArrayList<>();
        if (path == null || path.length() == 0 || source == null || source.isJsonNull()) {
            return result;
        }

        String regex = generateRegex(path, ignoreCase);
        Map<String, List<Filter>> filters = getFilters(path, ignoreCase); // generate filters from path;
        Map<String, List<Filter>> matchedFilters = new LinkedHashMap<>(); //filters with absolute path;
        Map<String, List<Filter>> ignoredFilters = getFilters(ignoredPaths, ignoreCase);
        Map<String, List<Filter>> ignoredMatchedFilters = new LinkedHashMap<>();
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
                    cachedJsonArrays.put(ignoreCase ? currentLevel.toLowerCase() : currentLevel, ja);
                    int length = ja.size();
                    for (int j = 0; j < ja.size(); j++) {
                        String level = currentLevel + "[" + j + "]";
                        JsonElementWithLevel tmp = new JsonElementWithLevel(ja.get(j), level);
                        queue.offer(tmp);
                        if (ignoreCase) {
                            level = level.toLowerCase();
                        }
                        updateMatchedFilters(level, filters, matchedFilters);
                        updateMatchedFilters(level, ignoredFilters, ignoredMatchedFilters);
                        if (level.matches(regex)) {
                            isFinished = true;
                            if (isMatchingFilters(cachedJsonArrays, level, matchedFilters, length) && !isMatchingIgnoredFilters(cachedJsonArrays, level, ignoredMatchedFilters, length)) {
                                result.add(tmp);
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
                            level = level.toLowerCase();
                        }
                        updateMatchedFilters(level, filters, matchedFilters);
                        updateMatchedFilters(level, ignoredFilters, ignoredMatchedFilters);
                        if (level.matches(regex)) {
                            isFinished = true;
                            if (isMatchingFilters(cachedJsonArrays, level, matchedFilters, length) && !isMatchingIgnoredFilters(cachedJsonArrays, level, ignoredMatchedFilters, length)) {
                                result.add(tmp);
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

        return applyIgnoredPathsWithoutArray(result, ignoredPaths, source);
    }



    private static List<JsonElementWithLevel> applyIgnoredPathsWithoutArray(List<JsonElementWithLevel> result, String[] ignoredPaths, JsonObject source) {
        boolean allAbsoluteJsonPaths =  isAbsoluteJsonPath(ignoredPaths, source );
        if(allAbsoluteJsonPaths) {
            return applyIgnoredAbsolutePathsWithoutArray(result, ignoredPaths, source);
        } else {
            return applyIgnoredPartialPathsWithoutArray(result, ignoredPaths, source);
        }
    }


    /**
     * Very expensive if you do not enter full JsonPath, N square time complexibility;
     * @param result
     * @param ignoredPaths
     * @return
     */
    private static List<JsonElementWithLevel> applyIgnoredAbsolutePathsWithoutArray(List<JsonElementWithLevel> result, String[] ignoredPaths, JsonObject source) {
        if(ignoredPaths == null || ignoredPaths.length == 0) {
            return result;
        }

        Set<String> set1  = new HashSet<>();
        for (String path : ignoredPaths) {
            if (!path.startsWith("$")) {
                path = "$." + path;
            }
            set1.add(path);
        }

        Iterator<JsonElementWithLevel> itr = result.iterator();
        while(itr.hasNext()) {
            JsonElementWithLevel je = itr.next();
            String level = je.getLevel();
            if(set1.contains(level)) {
                itr.remove();
            }
        }

        return result;
    }


    /**
     * Very expensive if you do not enter full JsonPath, N square time complexibility;
     * @param result
     * @param ignoredPaths
     * @return
     */
    private static List<JsonElementWithLevel> applyIgnoredPartialPathsWithoutArray(List<JsonElementWithLevel> result, String[] ignoredPaths, JsonObject source) {
        if (ignoredPaths == null || ignoredPaths.length == 0) {
            return result;
        }

        Set<String> set = new HashSet<>();
        for (String ignoredPath : ignoredPaths) {
            if (ignoredPath.indexOf('[') == -1) {
                set.add(ignoredPath);
            }
        }

        List<String> regexs = new ArrayList<>();
        for (String str : set) {
            regexs.add(generateRegex(str, false));
        }

        Iterator<JsonElementWithLevel> itr = result.iterator();
        while (itr.hasNext()) {
            JsonElementWithLevel je = itr.next();
            String level = je.getLevel();
            for (String regex : regexs) {
                if (level.matches(regex)) {
                    itr.remove();
                    break;
                }
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
            int length

                                            ) throws Exception {

        if (matchedFilters == null || matchedFilters.size() == 0) {
            return true;
        }
        if (currentLevel.indexOf('[') == -1) {
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

            filters = matchedFilters.get(prefix.toString().trim());

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


    private static boolean isMatchingIgnoredFilters(
            Map<String, JsonArray> cachedJsonArrays,
            String currentLevel,
            Map<String, List<Filter>> matchedFilters,
            int length

                                                   ) throws Exception {

        if (matchedFilters == null || matchedFilters.size() == 0) {
            return false;

        }
        if (currentLevel.indexOf('[') == -1) {
            return false;
        }

        StringBuilder prefix = new StringBuilder();
        StringBuilder prepath = new StringBuilder();
        int index = 0;
        while ((index = currentLevel.indexOf('[')) != -1) {
            prepath.append(currentLevel.substring(0, currentLevel.indexOf(']') + 1));
            prefix.append(currentLevel.substring(0, index) + "[]");
            int i = Integer.parseInt(currentLevel.substring(index + 1, currentLevel.indexOf(']')));
            List<Filter> filters = null;

            filters = matchedFilters.get(prefix.toString().trim());

            if (filters == null || filters.size() == 0) {

                return false;

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
                return true;
            } else {
                currentLevel = currentLevel.substring(currentLevel.indexOf(']') + 1);
            }

        }

        return false;
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


    private static String generateRegex(String path, boolean ignoreCase) {
        if (ignoreCase) {
            path = path.toLowerCase();
        }

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
        String regex = "(.*)\\." + prefix.toString().trim().replaceAll("\\[\\]", "\\\\[\\\\d+\\\\]");

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
        List<JsonElementWithLevel> result = get(jsonObject, path, ignoreCase, new String[]{});

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
     * @param path
     * @param ignoreCase
     * @return
     * @throws Exception
     */
    private static Map<String, List<Filter>> getFilters(String path, boolean ignoreCase) throws Exception {
        return getFilters(new String[]{path}, ignoreCase);
    }

    /**
     * In most cases, you will not need ignore multiple JsonPaths. However in case you have one scenario,
     * it does support.
     * But keep in mind:
     * 1 For same JsonPath, both has List<Condition> and List<Range>, it chooses List<Condition>;
     * 2 For same JsonPath, if it has multiple List<Condition>, it chooses the last one;
     *
     * @param paths
     * @param ignoreCase
     * @return
     * @throws Exception
     * @Author jzfeng
     */

    private static Map<String, List<Filter>> getFilters(String[] paths, boolean ignoreCase) throws Exception {
        if (paths == null || paths.length == 0) {
            return new LinkedHashMap<>();
        }

        Map<String, List<Filter>> conditionMap = new LinkedHashMap<>();
        Map<String, Set<Range>> rangeMap = new LinkedHashMap<>();
        for (String path : paths) {
            if (path == null || path.trim().length() == 0) {
                continue;
            }

            StringBuilder prefix = new StringBuilder();
            int index = 0;
            while ((index = path.indexOf('[')) != -1) {
                prefix.append(path.substring(0, index) + "[]");
                String r = path.substring(index + 1, path.indexOf(']')).trim();
                String key = prefix.toString().trim();
                if (r.contains("@")) { //conditions, "?(@.text =~ "(.*)\d{3,}(.*)" || @.text in {"Have a nice day", "Return policy"})"
                    List<Condition> conditions = Condition.getConditions(r);
                    if (conditions != null && conditions.size() > 0) {
                        if (ignoreCase) {
                            conditionMap.put(key.toLowerCase(), new ArrayList<Filter>(conditions));
                        } else {
                            conditionMap.put(key, new ArrayList<Filter>(conditions));
                        }
                    }
                } else if (r.matches("(.*)([,:])(.*)") || r.contains("last()") || r.contains("first()") || r.contains("*") || r.matches("\\s{0,}(-{0,}\\d+)\\s{0,}")) {
                    List<Range> ranges = getRange(r);
                    if (ranges != null && ranges.size() > 0) {
                        if (ignoreCase) {
                            key = key.toLowerCase();
                        }

                        if (rangeMap.containsKey(key)) {
                            rangeMap.get(key).addAll(ranges);
                        } else {
                            rangeMap.put(key, new HashSet<>(ranges));
                        }
                    }
                } else {
                    throw new Exception("Invalid JsonPath : " + path);
                }

                path = path.substring(path.indexOf(']') + 1);
            }
        }

        Map<String, List<Filter>> rm = processRangeMap(rangeMap);

        return mergeTwoMaps(conditionMap, rm);
    }


    private static Map<String, List<Filter>> mergeTwoMaps(
            Map<String, List<Filter>> conditionMap, Map<String, List<Filter>> rangeMap) {
        Map<String, List<Filter>> res = new LinkedHashMap<>();
        for (Map.Entry<String, List<Filter>> entry : conditionMap.entrySet()) {
            if (!res.containsKey(entry.getKey())) {
                res.put(entry.getKey(), entry.getValue());
            }
        }

        for (Map.Entry<String, List<Filter>> entry : rangeMap.entrySet()) {
            if (!res.containsKey(entry.getKey())) {
                res.put(entry.getKey(), entry.getValue());
            }
        }

        return res;
    }

    private static Map<String, List<Filter>> processRangeMap(Map<String, Set<Range>> rangeMap) {
        Map<String, List<Filter>> res = new LinkedHashMap<>();
        if (rangeMap == null || rangeMap.size() == 0) {
            return res;
        }

        for (Map.Entry<String, Set<Range>> entry : rangeMap.entrySet()) {
            String key = entry.getKey();
            Set<Range> value = entry.getValue();
            if (value != null && value.size() > 0) {
                List<Range> ranges = mergeRanges(new ArrayList<>(value));
                res.put(key, new ArrayList<Filter>(ranges));
            }
        }

        return res;
    }


    private static boolean isAbsoluteJsonPath(String[] paths, JsonObject source) {
        if (paths == null || paths.length == 0) {
            return true;
        }

        Set<String> keys = getKeys(source);
        for(String path : paths) {
            path = path.trim();
            if (path.startsWith("$")) {
                continue;
            }

            int index = path.indexOf(".");
            if (index == -1) {
                if (!keys.contains(path)) {
                    return false;
                }
            } else {
                if (!keys.contains(path.substring(0, index))) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean isAbsoluteJsonPath(String path, JsonObject source) {
        return isAbsoluteJsonPath(new String[]{path}, source);
    }

}
