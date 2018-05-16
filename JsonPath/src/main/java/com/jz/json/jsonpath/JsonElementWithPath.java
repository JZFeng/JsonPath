package com.jz.json.jsonpath;

import com.google.gson.JsonElement;

/**
 * @author jzfeng
 */

public class JsonElementWithPath {
    private JsonElement jsonElement;
    private String level;

    public JsonElementWithPath(JsonElement jsonElement, String level) {
        this.jsonElement = jsonElement;
        this.level = level;
    }

    public JsonElement getJsonElement() {
        return this.jsonElement;
    }

    public String getLevel() {
        return this.level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    @Override
    public String toString() {
        return level + " : " + jsonElement;
    }


}
