
package com.jz.json.jsonpath;

//  ==,!=,>,<,>=, <=,  "=~", "in", "nin", "subsetof", "size", "empty", "notempty"
public enum Operators {
    EQUAL_TO,
    NOT_EQUAL_TO,
    GREATER_THAN,
    LESS_THAN,
    GREATER_THAN_OR_EQUAL_TO,
    LESS_THAN_OR_EQUAL_TO,
    MATCHING_REGEX,
    IN,
    NIN,
    SUBSET_OF,
    SIZE,
    EMPTY,
    NOT_EMPTY,
    LOGICAL_AND,
    LOGICAL_OR;
}
