package com.jz.jsonpath;

import com.google.common.collect.Sets;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author jzfeng
 */

public class Condition implements IFilter {
    private String left;
    private String operator;
    private String right;
    private String logical_operator;
    public static final Set<String> OPERATORS = Sets.newHashSet("<", ">", "<=", ">=", "==", "!=", "=~", "in", "nin", "subsetof", "size", "empty", "notempty");
    public static final Set<String> LOGICAL_OPERATORS = Sets.newHashSet("&&", "||");

    public boolean isValid() {
        if (left == null || left.length() == 0) {
            return false;
        } else if (operator == null || operator.length() == 0) {
            return false;
        } else if (!OPERATORS.contains(operator)) {
            return false;
        }

        if (logical_operator != null && logical_operator.trim().length() != 0) {
            if (!LOGICAL_OPERATORS.contains(logical_operator.trim())) {
                return false;
            }
        }

        return true;
    }

    //Unary operator
    Condition(String left, String operator) {
        this.left = left.trim();
        this.operator = operator.trim();
        this.logical_operator = "";
    }

    //    Binary operator
    Condition(String left, String operator, String right) {
        this.left = left.trim();
        this.operator = operator.trim();
        this.right = right;
        this.logical_operator = "";
    }


    Condition(String left, String operator, String right, String logical_operator) {
        this.left = left.trim();
        this.operator = operator.trim();
        this.right = right;
        this.logical_operator = logical_operator;
    }


    public String getLeft() {
        return left.trim();
    }

    public String getOperator() {
        return operator.trim();
    }

    public String getRight() {
        return right;
    }

    public String getLogicalOperator() {
        return logical_operator.trim();
    }

    public void setLogicalOperator(String logical_operator) {
        this.logical_operator = logical_operator.trim();
    }

    /**
     * Now only support "&&", "||" between conditions
     *
     * @param r sample parameter: "@.category == 'fiction' && @.price < 10 || @.color == \"red\" || @.name size 10";
     * @return sample return: [&&, ||, ||]
     */
    private static List<String> getLogicalOperators(String r) {
        r = r.trim();
        List<String> operatorsBWConditions = new ArrayList<>();
        if (r == null || r.length() == 0 || !r.contains("@.")) {
            return operatorsBWConditions;
        }

        String[] strs = r.split("\\s{0,}&&\\s{0,}|\\s{0,}\\|\\|\\s{0,}");
        for (int i = 0; i < strs.length - 1; i++) {
            String operator = r.substring(r.indexOf(strs[i]) + strs[i].length(), r.indexOf(strs[i + 1])).trim();
            if (LOGICAL_OPERATORS.contains(operator)) {
                operatorsBWConditions.add(operator.trim());
            }
        }

        return operatorsBWConditions;
    }


    // to-do: refactoring : merge getRange and getLogicalOperators

    /**
     * @param r sample parameter:  r = "?(@.author=="Evelyn Waugh" && @.price > 12 || @.category == "reference")"
     * @return
     */
    public static List<Condition> getConditions(String r) throws Exception {
        List<Condition> conditions = new ArrayList<>();
        if(r == null || r.length() == 0 || !r.contains("@.")) {
            return conditions;
        }

        r = r.trim();
        List<String> logicalOperators = getLogicalOperators(r);
        if (r.trim().startsWith("?")) {
            r = r.substring(r.indexOf('(') + 1, r.lastIndexOf(')'));
        }

        String[] strs = r.split("\\s{0,}&&\\s{0,}|\\s{0,}\\|\\|\\s{0,}");
        for (String str : strs) {
            Condition condition = getCondition(str);
            if (condition != null) {
                conditions.add(condition);
            }
        }

        if (conditions.size() == logicalOperators.size() + 1) {
            for (int i = 0; i < conditions.size() - 1; i++) {
                conditions.get(i).setLogicalOperator(logicalOperators.get(i).trim());
            }
        } else {
            System.out.println("conditions.size() : " + conditions.size() + " ; logicalOperators.size()" + logicalOperators.size() + " .Please check the JsonPath.");
            throw new Exception(" CONDITION_STRING : " + r + " ; CONDITIONS_GENERATED : " + conditions + ";");
        }

        return conditions;
    }

    /**
     * @param str sample str like "price < 10" "name size 10"
     * @return instance of Condition
     */
    private static Condition getCondition(String str) throws Exception {
        str = str.trim();
        Condition condition = null;
        if (str == null || str.length() == 0) {
            return condition;
        }
        if (str.matches("(\\?{0,1}\\s{0,}\\({0,1}@{0,1}\\.{0,1})(.*)")) {
            str = str.replaceFirst("(\\?\\({0,}@\\.{0,})(.*)", "$2");
        }
        if (str.startsWith("@")) {
            str = str.substring(2);
        }


        List<String> fields = new ArrayList<>();
        String regExp = "(\\s{0,}[><=!]{1}[=~]{0,1}\\s{0,})";

        if (str.matches(".*" + regExp + ".*")) {
            Pattern pattern = Pattern.compile(regExp);
            Matcher m = pattern.matcher(str);
            while (m.find()) {
                String[] items = str.split("\\s{0,}[><=!]{1}[=~]{0,1}\\s{0,}");
                condition = new Condition(items[0].trim(), m.group(1).trim(), items[1].trim());
            }
        } else if (str.length() > 5 && str.indexOf(" ") != -1) {
            // handle cases like "in", "nin" etc
            String regex = "(.*)(\\s+in\\s+|\\s+nin\\s+|\\s+subsetof\\s+|\\s+empty\\s{0,}|\\s+notempty\\s{0,}|\\s+size\\s+)(.*)";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(str);
            while (m.find()) {
                int count = m.groupCount();
                if (count == 3) {
                    condition = new Condition(m.group(1).trim(), m.group(2).trim(), m.group(3).trim());
                } else if (count == 2) {
                    condition = new Condition(m.group(1).trim(), m.group(2).trim());
                } else {
                    throw new Exception("Wrong condition");
                }
            }

        }

        return (condition.isValid()) ? condition : null;

    }

    @Override
    public String toString() {
        return left + " " + operator + " " + (right == null ? "" : right);
    }

    @Override
    public int hashCode() {
        return (left.hashCode() + right.hashCode()) * operator.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (this == obj)
            return true;
        if (obj instanceof Condition) {
            Condition condition = (Condition) obj;
            if (this.left.equals(condition.left) && this.operator.equals(condition.operator) && this.right.equals(condition.right)) {
                return true;
            }
        }

        return false;
    }
}
