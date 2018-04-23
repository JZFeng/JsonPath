import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jz.json.jsonpath.JsonElementWithLevel;
import com.jz.json.jsonpath.JsonPath;
import com.jz.json.jsonpath.Utils;
import org.junit.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.jz.json.jsonpath.JsonPath.get;


public class TestJsonPath {

    final String[] paths = new String[]{
            "$.modules.BINSUMMARY.minView.actions[0]"
            , "modules.SELLERPRESENCE.sellerName.action.URL"
            , "RETURNS.maxView.value.length()"
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

    final String[] ignoredPaths = new String[]{
            "PICTURE.mediaList[0].image.originalImg.URL",  //3
            "RETURNS.maxView.value[3].value[0].textSpans[0].action.URL" // 1
            , "THIRD_PARTY_RESOURCES.js[0].url"  // 1
            , "BINSUMMARY.minView.actions[1].action.URL" // 2
            , "$.modules.WATCH.watching.watchAction.action.URL" // 1
            , "$.modules.WATCH.watch.watchAction.action.URL"  // 1
            , "BINSUMMARY.minView.actions[2].value.cartSigninUrl.URL" // 2
    };

    @Test
    public void testJsonPath_CaseSensitive_noIgnoredPaths() throws Exception {
        JsonParser parser = new JsonParser();
        String json = Utils.convertFormattedJson2Raw(new File("./src/test/java/testdata.json"));
        JsonObject source = parser.parse(json).getAsJsonObject();

        int[] expectedSize = {1, 1, 1, 5, 5, 3, 1, 4, 23, 2, 2, 1};

        for (int i = 0; i < paths.length; i++) {
            List<JsonElementWithLevel> res = JsonPath.get(source, paths[i]);
            System.out.println("***SIZE: " + res.size() + ";" + paths[i] + ";\r\n" + res);
            Assert.assertTrue(res.size() == expectedSize[i]);
        }
    }


    @Test
    public void testJsonPath_ignoreCase_noIgnoredPaths() throws Exception {
        JsonParser parser = new JsonParser();
        String json = Utils.convertFormattedJson2Raw(new File("./src/test/java/testdata.json"));
        JsonObject source = parser.parse(json).getAsJsonObject();

        int[] expectedSize = {1, 1, 1, 5, 5, 3, 1, 4, 26, 2, 2, 1};

        for (int i = 0; i < paths.length; i++) {
            List<JsonElementWithLevel> res = get(source, paths[i], true, new String[]{});
            System.out.println("***SIZE: " + res.size() + ";" + paths[i] + ";\r\n" + res);
            Assert.assertTrue(res.size() == expectedSize[i]);
        }
    }

    @Test
    public void testJsonPath_CaseSensitive_ignoredPaths() throws Exception {
        JsonParser parser = new JsonParser();
        String json = Utils.convertFormattedJson2Raw(new File("./src/test/java/testdata.json"));
        JsonObject source = parser.parse(json).getAsJsonObject();

        int[] expectedSize = {1, 1, 1, 4, 4, 2, 1, 4, 13, 2, 1, 0};

        for (int i = 0; i < paths.length; i++) {
            List<JsonElementWithLevel> res = get(source, paths[i], false, ignoredPaths);
            System.out.println("***SIZE: " + res.size() + ";" + paths[i] + ";\r\n" + res);
            Assert.assertTrue(res.size() == expectedSize[i]);
        }
    }

    @Test
    public void testJsonPath_ignoreCase_ignoredPaths() throws Exception {
        JsonParser parser = new JsonParser();
        String json = Utils.convertFormattedJson2Raw(new File("./src/test/java/testdata.json"));
        JsonObject source = parser.parse(json).getAsJsonObject();

        int[] expectedSize = {1, 1, 1, 4, 4, 2, 1, 4, 15, 2, 1, 0};

        for (int i = 0; i < paths.length; i++) {
            List<JsonElementWithLevel> res = get(source, paths[i], true, ignoredPaths);
            System.out.println("***SIZE: " + res.size() + ";" + paths[i] + ";\r\n" + res);
            Assert.assertTrue(res.size() == expectedSize[i]);
        }
    }
}
