package com.jz.jsonpath;

import com.google.gson.*;

import java.io.File;
import java.util.*;

import static com.jz.jsonpath.Range.getRange;

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
        String json = Utils.convertFormattedJson2Raw(new File("/Users/jzfeng/Desktop/au.json"));
        JsonObject source = parser.parse(json).getAsJsonObject();


/*
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
*/


        String[] paths = new String[]{"URL"};
        String[] ignoredPaths = new String[]{
                "modules.SELLERPRESENCE.profileLogo.URL",
                "modules.COMMITTOBUY.redirect.url",
                "modules.COMMITTOBUY.fallBackUrl.URL",
                "modules.SELLERPRESENCE.askSeller.action.URL",
                "modules.SELLERPRESENCE.sellerName.action.URL",
                "$.modules.PICTURE",
                "$.modules.ITEMDESC.itemDescription.action.URL",
                "$.modules.EBAYGUARANTEE.embg.infoLink.URL",
                "$.modules.OTHER_ACTIONS.soltAction.action.URL",
                "$.modules.OTHER_ACTIONS.reportItemAction.action.URL",
                "$.modules.OTHER_ACTIONS.surveyAction.action.URL",
                "$.modules.INCENTIVES.incentivesURL.URL",
                "$.modules.BID_LAYER.thumbnail.URL",
                "$.modules.BID_LAYER.reviewBidAction.action.URL",
                "$.modules.BID_LAYER.confirmBidAction.action.URL",
                "$.modules.BIDSUMMARY.bidAction.action.URL",
                "$.modules.TOPRATEDSELLER.topRatedInfo.logo.action.URL",
                "$.modules.RSPSECTION.minView.logo.action.URL",
                "$.modules.THIRD_PARTY_RESOURCES.js[*].url",
                "$.modules.BINSUMMARY.minView.actions[0,1,2].action.URL",
                "$.modules.HANDLINGCONTENT.value[*].textSpans[1].action.URL",
                "RETURNS.maxView.value[3:5]",
                "BID_LAYER.powerbidButtons[*].action.URL",
                "REWARDS.value.textSpans[1].action.URL"
        };

        String path = "url";
        boolean ignoreCase = true;
        Map<String, JsonArray> a = Utils.getJsonArrayMap(source, ignoreCase);
        Map<String, List<IFilter>> ignoredFilters = getFilters(updatePaths2Full(ignoredPaths, source), ignoreCase);
        Map<String, JsonArray> cachedJsonArrays = Utils.getJsonArrayMap(source, ignoreCase); // save JsonArray to map, in order to reduce time complexibility
        Map<String, List<IFilter>> ignoredMatchedFilters = updateFilters2Full(cachedJsonArrays, ignoredFilters);


        for (String p : paths) {
            long startTime = System.currentTimeMillis();
            List<JsonElementWithPath> res = get(source, p, true, ignoredPaths);
            System.out.println("****" + res.size() + "****" + (long) (System.currentTimeMillis() - startTime) + "ms" + "," + p);
            for (JsonElementWithPath je : res) {
                System.out.println(je);
            }
        }
    }


    public static List<JsonElementWithPath> get(
            String source, String path) throws Exception {
        List<JsonElementWithPath> res = new ArrayList<>();
        return get(source, path, false, new String[]{});
    }

    /**
     * @param source
     * @param path
     * @return
     * @throws Exception
     * @author jzfeng
     */
    public static List<JsonElementWithPath> get(
            String source, String path, boolean ignoreCase, String[] ignoredPaths) throws Exception {
        List<JsonElementWithPath> res = new ArrayList<>();
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
    public static List<JsonElementWithPath> get(JsonObject source, String path) throws Exception {
        return get(source, path, false, new String[]{});
    }

    /**
     * @param source     the source of JsonObject
     * @param path       standard json path;
     * @param ignoreCase if true, it will ignore the case of path; if false, it will strictly match path;
     * @return returns a a list of {@link JsonElementWithPath}
     * @author jzfeng
     */
    public static List<JsonElementWithPath> get(
            JsonObject source, String path, boolean ignoreCase, String[] ignoredPaths) throws Exception {
        List<JsonElementWithPath> result = new ArrayList<>();
        if (path == null || path.length() == 0 || source == null || source.isJsonNull()) {
            return result;
        }

        String regex = generateRegex(path, ignoreCase);
        Map<String, JsonArray> cachedJsonArrays = Utils.getJsonArrayMap(source, ignoreCase); // save JsonArray to map, in order to reduce time complexibility

        Map<String, List<IFilter>> filters = getFilters(updatePaths2Full(new String[]{path}, source), ignoreCase); // generate filters from path;
        Map<String, List<IFilter>> matchedFilters = updateFilters2Full(cachedJsonArrays, filters);//filters with absolute path;


        boolean isAbsolutePath = isFullPath(path, source);
        boolean isFinished = false;

        // to support length() function;
        if (path.matches("(.*)(\\.length\\(\\)$)")) { // case: [path == $.listing.termsAndPolicies.length()]
            path = path.replaceAll("(.*)(\\.length\\(\\)$)", "$1");
            int length = length(source, path, ignoreCase);
            result.add(new JsonElementWithPath(new JsonPrimitive(length), path));
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
//                System.out.println(currentLevel);

                if (je.isJsonArray()) {
                    JsonArray ja = je.getAsJsonArray();
                    for (int j = 0; j < ja.size(); j++) {
                        String level = currentLevel + "[" + j + "]";
                        JsonElementWithPath tmp = new JsonElementWithPath(ja.get(j), level);
                        queue.offer(tmp);
                        if (ignoreCase) {
                            level = level.toLowerCase();
                        }
                        if (level.matches(regex)) {
                            isFinished = true;
                            if (isMatchingFilters(cachedJsonArrays, level, matchedFilters)) {
                                result.add(tmp);
                            }
                        }
                    }
                } else if (je.isJsonObject()) {
                    JsonObject jo = je.getAsJsonObject();
                    for (Map.Entry<String, JsonElement> entry : jo.entrySet()) {
                        String level = currentLevel + "." + entry.getKey();
                        JsonElementWithPath tmp = new JsonElementWithPath(entry.getValue(), level);
                        queue.offer(tmp);
                        if (ignoreCase) {
                            level = level.toLowerCase();
                        }
                        if (level.matches(regex)) {
                            isFinished = true;
                            if (isMatchingFilters(cachedJsonArrays, level, matchedFilters)) {
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

        return applyIgnoredPaths(result, ignoredPaths, ignoreCase, source, cachedJsonArrays);
    }


    /**
     * Very expensive if you do not enter full JsonPath, N square time complexibility;
     *
     * @param result
     * @param ignoredPaths
     * @return
     */
    public static List<JsonElementWithPath> applyIgnoredPaths(
            List<JsonElementWithPath> result,
            String[] ignoredPaths,
            boolean ignoreCase,
            JsonObject source,
            Map<String, JsonArray> cachedJsonArrays) throws Exception {

        if (ignoredPaths == null || ignoredPaths.length == 0) {
            return result;
        }

        ignoredPaths = updatePaths2Full(ignoredPaths, source);
        Set<String> absolutePaths = getFullPathsWithoutArray(ignoredPaths, source); // for performance purpose;
        Map<String, List<IFilter>> ignoredFilters = getFilters(ignoredPaths, ignoreCase);
        Map<String, List<IFilter>> ignoredMatchedFilters = updateFilters2Full(cachedJsonArrays, ignoredFilters);

        //get regex for each ignoredPath
        List<String> regexs = new ArrayList<>();
        for (String ignoredPath : ignoredPaths) {
            if (ignoredPath.indexOf('[') == -1) {
                regexs.add(generateRegex(ignoredPath.trim(), false) + ".*");
            }
        }

        Iterator<JsonElementWithPath> itr = result.iterator();
        while (itr.hasNext()) {
            JsonElementWithPath je = itr.next();
            String level = je.getLevel();
            if (absolutePaths.contains(level)) {
                itr.remove();
                continue;
            }

            for (String regex : regexs) {
                if (level.matches(regex)) {
                    itr.remove();
                    break;
                }
            }

            if (ignoreCase) {
                level = level.toLowerCase();
            }
            if (isMatchingIgnoredFilters(cachedJsonArrays, level, ignoredMatchedFilters)) {
                itr.remove();
                continue;
            }

        }

        return result;
    }


    /**
     * @param level          $.courses[i].grade
     * @param matchedFilters
     * @return true if i in matchedRange();
     */
    public static boolean isMatchingFilters(
            Map<String, JsonArray> cachedJsonArrays,
            String level,
            Map<String, List<IFilter>> matchedFilters
    ) throws Exception {

        if (matchedFilters == null || matchedFilters.size() == 0) {
            return true;
        }
        if (level.indexOf('[') == -1) {
            return true;
        }

        String tmp = level.substring(0, level.lastIndexOf("["));
        int length = cachedJsonArrays.get(tmp).getAsJsonArray().size();

        StringBuilder prefix = new StringBuilder();
        StringBuilder prepath = new StringBuilder();
        int index = 0;
        while ((index = level.indexOf('[')) != -1) {
            prepath.append(level.substring(0, level.indexOf(']') + 1));
            prefix.append(level.substring(0, index) + "[]");
            int i = Integer.parseInt(level.substring(index + 1, level.indexOf(']')));
            List<IFilter> filters = matchedFilters.get(prefix.toString().trim());

            if (filters == null || filters.size() == 0) {
                return true;
            }

            boolean isMatched = false;
            if (filters.get(0) instanceof Range) {
                isMatched = isMatchingRange(filters, i, length);
            } else if (filters.get(0) instanceof Condition) {
                List<Condition> c = new ArrayList<>();
                for (IFilter f : filters) {
                    c.add((Condition) f);
                }

                JsonArray jsonArray = cachedJsonArrays.get(prepath.substring(0, prepath.lastIndexOf("[")));
                isMatched = isMatchingConditions(jsonArray.get(i).getAsJsonObject(), c);
            }

            if (isMatched) {
                level = level.substring(level.indexOf(']') + 1);
            } else {
                return false;
            }

        }

        return true;
    }


    public static boolean isMatchingIgnoredFilters(
            Map<String, JsonArray> cachedJsonArrays,
            String level,
            Map<String, List<IFilter>> matchedFilters

    ) throws Exception {

        if (matchedFilters == null || matchedFilters.size() == 0) {
            return false;
        }
        if (level.indexOf('[') == -1) {
            return false;
        }

        String tmp = level.substring(0, level.lastIndexOf("["));
        System.out.print("tmp is " + tmp);
        int length = cachedJsonArrays.get(tmp).getAsJsonArray().size();

        StringBuilder prefix = new StringBuilder();
        StringBuilder prepath = new StringBuilder();

        int index = 0;
        while ((index = level.indexOf('[')) != -1) {
            prepath.append(level.substring(0, level.indexOf(']') + 1));
            prefix.append(level.substring(0, index) + "[]");
            int i = Integer.parseInt(level.substring(index + 1, level.indexOf(']')));
            List<IFilter> filters = matchedFilters.get(prefix.toString().trim());
            if (filters == null || filters.size() == 0) {
                return false;
            }

            boolean isMatched = false;
            if (filters.get(0) instanceof Range) {
                isMatched = isMatchingRange(filters, i, length);
            } else if (filters.get(0) instanceof Condition) {
                List<Condition> c = new ArrayList<>();
                for (IFilter f : filters) {
                    c.add((Condition) f);
                }

                JsonArray jsonArray = cachedJsonArrays.get(prepath.substring(0, prepath.lastIndexOf("[")));
                isMatched = isMatchingConditions(jsonArray.get(i).getAsJsonObject(), c);
            }

            if (isMatched) {
                return true;
            } else {
                level = level.substring(level.indexOf(']') + 1);
            }

        }

        return false;
    }


    /**
     * to check whether $.modules.RETURNS.maxView.value[4] is matching the range in matchedRanges;
     *
     * @param i index of a JsonArray
     * @return return true if  i in any of the range [(0, 0), (3,3)], otherwise return false;
     */
    public static boolean isMatchingRange(
            List<IFilter> filterList, int i, int length) throws Exception {
        if (filterList != null && filterList.size() > 0) {
            for (IFilter filter : filterList) {
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
    public static boolean isMatchingConditions(JsonObject jo, List<Condition> conditions) throws Exception {
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
    public static boolean isMatchingCondition(JsonObject jo, Condition condition) throws Exception {
        if (!condition.isValid()) {
            return false;
        }

        Set<String> keys = Utils.getKeys(jo);
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


    public static String generateRegex(String path, boolean ignoreCase) {
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


    public static int length(JsonObject jsonObject, String path, boolean ignoreCase) throws Exception {
        if (path == null || path.length() == 0) {
            return 0;
        }
        if (jsonObject == null || jsonObject.isJsonNull() || !jsonObject.isJsonObject()) {
            return 0;
        }

        int length = 0;
        List<JsonElementWithPath> result = get(jsonObject, path, ignoreCase, new String[]{});

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
    public static Map<String, List<IFilter>> getFilters(String path, boolean ignoreCase) throws Exception {
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

    public static Map<String, List<IFilter>> getFilters(String[] paths, boolean ignoreCase) throws Exception {
        if (paths == null || paths.length == 0) {
            return new LinkedHashMap<>();
        }


        Map<String, List<IFilter>> conditionMap = new LinkedHashMap<>();
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
                            conditionMap.put(key.toLowerCase(), new ArrayList<IFilter>(conditions));
                        } else {
                            conditionMap.put(key, new ArrayList<IFilter>(conditions));
                        }
                    }
                } else if (r.matches("(.*)([,:])(.*)") || r.contains("last()") || r.contains("first()") || r.contains("*") || r.matches("\\s{0,}(-{0,}\\d+)\\s{0,}")) { //ranges
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

        Map<String, List<IFilter>> rm = mergeRanges(rangeMap);

        return mergeTwoFilterMaps(conditionMap, rm);
    }


    public static Map<String, List<IFilter>> mergeTwoFilterMaps(
            Map<String, List<IFilter>> conditionMap, Map<String, List<IFilter>> rangeMap) {
        Map<String, List<IFilter>> res = new LinkedHashMap<>();
        for (Map.Entry<String, List<IFilter>> entry : conditionMap.entrySet()) {
            if (!res.containsKey(entry.getKey())) {
                res.put(entry.getKey(), entry.getValue());
            }
        }

        for (Map.Entry<String, List<IFilter>> entry : rangeMap.entrySet()) {
            if (!res.containsKey(entry.getKey())) {
                res.put(entry.getKey(), entry.getValue());
            }
        }

        return res;
    }

    public static Map<String, List<IFilter>> mergeRanges(Map<String, Set<Range>> rangeMap) {
        Map<String, List<IFilter>> res = new LinkedHashMap<>();
        if (rangeMap == null || rangeMap.size() == 0) {
            return res;
        }

        for (Map.Entry<String, Set<Range>> entry : rangeMap.entrySet()) {
            String key = entry.getKey();
            Set<Range> value = entry.getValue();
            if (value != null && value.size() > 0) {
                List<Range> ranges = Range.mergeRanges(new ArrayList<>(value));
                res.put(key, new ArrayList<IFilter>(ranges));
            }
        }

        return res;
    }

    public static Set<String> getFullPathsWithoutArray(String[] paths, JsonObject source) {
        Set<String> res = new LinkedHashSet<>();
        if (paths == null || paths.length == 0) {
            return res;
        }

        Set<String> keys = Utils.getKeys(source);
        for (String path : paths) {
            path = path.trim();
            if (path.indexOf('[') != -1) {
                continue;
            }
            if (path.startsWith("$")) {
                res.add(path);
                continue;
            }

            int index = path.indexOf(".");
            if ((index == -1 && keys.contains(path)) || (index != -1 && keys.contains(path.substring(0, index)))) {
                res.add("$." + path);
            }

        }

        return res;
    }

    public static boolean isFullPath(String[] paths, JsonObject source) {
        if (paths == null || paths.length == 0) {
            return true;
        }

        Set<String> keys = Utils.getKeys(source);
        for (String path : paths) {
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

    public static boolean isFullPath(String path, JsonObject source) {
        return isFullPath(new String[]{path}, source);
    }


    public static String[] updatePaths2Full(String[] paths, JsonObject source) {
        if (paths == null || paths.length == 0) {
            return paths;
        }

        String[] res = new String[paths.length];
        Set<String> keys = Utils.getKeys(source);
        for (int i = 0; i < paths.length; i++) {
            String path = paths[i].trim();
            if (!path.startsWith("$")) {
                int index = path.indexOf(".");
                String key = (index == -1 ? path : path.substring(0, index));
                if (keys.contains(key)) {
                    path = "$." + path;
                }
            }
            res[i] = path;
        }

        return res;
    }

    public static String updatePaths2Full(String path, JsonObject source) {
        if (path == null || path.length() == 0 || path.startsWith("$")) {
            return path;
        }

        if (isFullPath(path, source)) {
            path = "$." + path;
        }

        return path;
    }

    public static Map<String, List<IFilter>> updateFilters2Full(
            Map<String, JsonArray> cachedJsonArrays, Map<String, List<IFilter>> filters) {
        if (filters == null || filters.size() == 0 || isFullPathFilters(filters)) {
            return filters;
        }

        //update filters to full path filters
        Map<String, List<IFilter>> matchedFilters = new LinkedHashMap<>();
        Set<String> paths = cachedJsonArrays.keySet();
        for (String path : paths) {
            String level = path + "[]";
            StringBuilder prefix = new StringBuilder();
            int index = 0;
            while ((index = level.indexOf('[')) != -1) {
                prefix.append(level.substring(0, index) + "[]");
                if (!matchedFilters.containsKey(prefix)) {
                    for (Map.Entry<String, List<IFilter>> entry : filters.entrySet()) {
                        String key = entry.getKey();
                        List<IFilter> value = entry.getValue();
                        int idx = prefix.indexOf(key);
                        if (idx != -1 && prefix.toString().substring(idx).equals(key)) {
                            matchedFilters.put(prefix.toString(), value);
                        }
                    }
                }

                level = level.substring(level.indexOf(']') + 1);
            }
        }

        return matchedFilters;
    }


    public static boolean isFullPathFilters(Map<String, List<IFilter>> filters) {
        if (filters == null || filters.size() == 0) {
            return true;
        }

        Set<String> keys = filters.keySet();
        for (String key : keys) {
            if (!key.startsWith("$")) {
                return false;
            }
        }

        return true;
    }


    public static boolean isPathMatchingAbsolutePaths(String path, Set<String> absolutePaths) {
        if (path == null || path.length() == 0) {
            return false;
        }

        return absolutePaths.contains(path);
    }

    public static boolean isPathMatchingRegxs(String path, List<String> regexs) {
        if (path == null || path.length() == 0) {
            return false;
        }

        for (String regex : regexs) {
            if (path.matches(regex)) {
                return true;
            }
        }

        return false;
    }


    public static boolean isPathMatchingIgnoredFilters(
            Map<String, JsonArray> cachedJsonArrays,
            String level,
            Map<String, List<IFilter>> matchedFilters

    ) throws Exception {

        if (matchedFilters == null || matchedFilters.size() == 0) {
            return false;
        }
        if (level.indexOf('[') == -1) {
            return false;
        }

        String tmp = level.substring(0, level.lastIndexOf("["));
//        System.out.print("tmp is " + tmp);
        int length = cachedJsonArrays.get(tmp).getAsJsonArray().size();

        StringBuilder prefix = new StringBuilder();
        StringBuilder prepath = new StringBuilder();

        int index = 0;
        while ((index = level.indexOf('[')) != -1) {
            prepath.append(level.substring(0, level.indexOf(']') + 1));
            prefix.append(level.substring(0, index) + "[]");
            int i = Integer.parseInt(level.substring(index + 1, level.indexOf(']')));
            List<IFilter> filters = matchedFilters.get(prefix.toString().trim());
            if (filters == null || filters.size() == 0) {
                return false;
            }

            boolean isMatched = false;
            if (filters.get(0) instanceof Range) {
                isMatched = isMatchingRange(filters, i, length);
            } else if (filters.get(0) instanceof Condition) {
                List<Condition> c = new ArrayList<>();
                for (IFilter f : filters) {
                    c.add((Condition) f);
                }

                JsonArray jsonArray = cachedJsonArrays.get(prepath.substring(0, prepath.lastIndexOf("[")));
                isMatched = isMatchingConditions(jsonArray.get(i).getAsJsonObject(), c);
            }

            if (isMatched) {
                return true;
            } else {
                level = level.substring(level.indexOf(']') + 1);
            }

        }

        return false;
    }

}
