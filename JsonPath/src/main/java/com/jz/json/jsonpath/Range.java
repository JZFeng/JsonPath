
package com.jz.json.jsonpath;

import com.google.common.collect.Lists;

import java.util.*;

/**
 * @author jzfeng
 */

//  x >= start && x <= end
public class Range implements Filter {
    private static final int LARGEST_PRIME_NUMBER = 2147483629;
    private int start;
    private int end;

    Range(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public boolean isValid() {
        return (start < end) ? false : true;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    /**
     * @param r String in []
     *          [2]
     *          [-2]
     *          [0,1]
     *          [:2]
     *          [1:2]
     *          [-2:]
     *          [2:]
     *          last()
     *          first()
     *          <p>
     *          $..book[?(@.isbn)]	All books with an ISBN number (convert to notempty ), [?@.isbn notempty]
     *          <p>
     *          $.store.book[?(@.price < 10)]	All books in store cheaper than 10
     *          $..book[?(@.price <= $['expensive'])]	All books in store that are not "expensive"
     *          $..book[?(@.author =~ /.*REES/i)]	All books matching regex (ignore case)
     */

    public static List<Range> getRange(String r) {
        List<Range> result = new ArrayList<>();
        if (r == null || r.length() == 0 || r.matches("(\\s{0,}[><=!]{1}[=~]{0,1}\\s{0,})")) {
            return result;
        }

        r = r.trim();
        if (r.equalsIgnoreCase("first()")) {
            result.add(new Range(0, 0));
        } else if (r.equalsIgnoreCase("last()")) {
            result.add(new Range(-1, -1));
        } else if (r.contains("positon()")) {
            //to-do
        } else if (r.contains(",")) {
            String[] strs = r.split("\\s*,\\s*|\\s*:\\s*");
            for (String str : strs) {
                result.add(new Range(Integer.parseInt(str), Integer.parseInt(str)));
            }
        } else if (r.contains(":")) {
            String[] strs = r.split("\\s*,\\s*|\\s*:\\s*");
            if (strs.length == 2) { //[0:3]
                if (strs[0].length() == 0 || strs[0].equalsIgnoreCase("")) {
                    result.add(new Range(0, Integer.parseInt(strs[1]) - 1));
                } else {
                    result.add(new Range(Integer.parseInt(strs[0]), Integer.parseInt(strs[1]) - 1));
                }
            } else if (strs.length == 1) {
                int index = Integer.parseInt(strs[0]);
                if (r.startsWith(":")) { //[ : 2]
                    result.add(new Range(0, index - 1));
                } else {
                    if (index >= 0) {
                        result.add(new Range(index, Integer.MAX_VALUE));
                    } else {
                        result.add(new Range(index, -1)); // [-2],for negative range, i - array.length;
                    }
                }
            }
        } else if (r.equalsIgnoreCase("*")) { //[*]
            result.add(new Range(0, Integer.MAX_VALUE));
        } else if (Integer.parseInt(r) >= 0) {
            result.add(new Range(Integer.parseInt(r), Integer.parseInt(r)));
        } else if (Integer.parseInt(r) < 0) { //[-2]
            result.add(new Range(Integer.parseInt(r), Integer.parseInt(r)));
        }

        return result;
    }



    private static boolean isPrime(int n) {
        if (n < 2) {
            return false;
        }
        if (n == 2) {
            return true;
        }
        if (n % 2 == 0) {
            return false;
        }

        for (int i = 3; i * i <= n; i = i + 2) {
            if (n % i == 0) {
                return false;
            }
        }

        return true;
    }

    private static int nextPrime(int n) {
        if(n % 2 == 0) {
            n = n - 1;
        }
        while(!isPrime(n)) {
            n = n - 2;
        }

        return n;
    }


    @Override
    public String toString() {
        return "Start : " + start + " , end :" + end;
    }

    @Override
    public int hashCode() {
        long hashcode = (long)String.valueOf(start).hashCode() * (long)(String.valueOf(end).hashCode());
        return (int)(hashcode % (long)LARGEST_PRIME_NUMBER);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (this == obj)
            return true;
        if (obj instanceof Range) {
            Range range = (Range) obj;
            if (this.start == range.start && this.end == range.end) {
                return true;
            }
        }

        return false;
    }


    public static List<Range> mergeRanges(List<Range> ranges) {
        if (ranges == null || ranges.size() <= 1) {
            return new ArrayList<Range>(ranges);
        }

        Collections.sort(ranges, new RangeComparator());

        List<Range> result = new ArrayList<Range>();
        Range last = ranges.get(0);
        for (int i = 1; i < ranges.size(); i++) {
            Range curt = ranges.get(i);
            if (curt.start <= last.end) {
                last.end = Math.max(last.end, curt.end);
            } else {
                result.add(last);
                last = curt;
            }
        }
        result.add(last);

        return result;
    }


    private static class RangeComparator implements Comparator<Range> {
        public int compare(Range a, Range b) {
            return a.start - b.start;
        }
    }

}
