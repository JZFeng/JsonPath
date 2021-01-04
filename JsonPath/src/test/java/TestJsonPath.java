import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jz.jsonpath.JsonElementWithPath;
import com.jz.jsonpath.JsonPath;
import com.jz.jsonpath.Utils;
import org.junit.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

import static com.jz.jsonpath.JsonPath.get;


public class TestJsonPath {

    @Test
    public void testJsonPath_CaseSensitive_noIgnoredPaths() throws Exception {
        JsonParser parser = new JsonParser();
        String json = Utils.convertFormattedJson2Raw(new File("./src/test/java/us.json"));
        JsonObject source = parser.parse(json).getAsJsonObject();

        int[] expectedSize = {1, 1, 1, 5, 5, 3, 1, 4, 23, 2, 2, 1};

        final String[] us_paths = new String[]{
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

        for (int i = 0; i < us_paths.length; i++) {
            List<JsonElementWithPath> res = JsonPath.get(source, us_paths[i]);
            System.out.println("***SIZE: " + res.size() + ";" + us_paths[i] + ";\r\n" + res);
            Assert.assertTrue(res.size() == expectedSize[i]);
        }
    }


    @Test
    public void testJsonPath_ignoreCase_noIgnoredPaths() throws Exception {
        JsonParser parser = new JsonParser();
        String json = Utils.convertFormattedJson2Raw(new File("./src/test/java/us.json"));
        JsonObject source = parser.parse(json).getAsJsonObject();

        int[] expectedSize = {1, 1, 1, 5, 5, 3, 1, 4, 26, 2, 2, 1};

        final String[] us_paths = new String[]{
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

        for (int i = 0; i < us_paths.length; i++) {
            List<JsonElementWithPath> res = get(source, us_paths[i], true, new String[]{});
            System.out.println("***SIZE: " + res.size() + ";" + us_paths[i] + ";\r\n" + res);
            Assert.assertTrue(res.size() == expectedSize[i]);
        }
    }

    @Test
    public void testJsonPath_CaseSensitive_ignoredPaths() throws Exception {
        JsonParser parser = new JsonParser();
        String json = Utils.convertFormattedJson2Raw(new File("./src/test/java/us.json"));
        JsonObject source = parser.parse(json).getAsJsonObject();

        int[] expectedSize = {1, 1, 1, 4, 4, 2, 1, 4, 13, 2, 1, 0};

        final String[] us_paths = new String[]{
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

        final String[] us_ignoredPaths = new String[]{
                "PICTURE.mediaList[0].image.originalImg.URL",  //3
                "RETURNS.maxView.value[3].value[0].textSpans[0].action.URL" // 1
                , "THIRD_PARTY_RESOURCES.js[0].url"  // 1
                , "BINSUMMARY.minView.actions[1].action.URL" // 2
                , "$.modules.WATCH.watching.watchAction.action.URL" // 1
                , "$.modules.WATCH.watch.watchAction.action.URL"  // 1
                , "BINSUMMARY.minView.actions[2].value.cartSigninUrl.URL" // 2
        };

        for (int i = 0; i < us_paths.length; i++) {
            List<JsonElementWithPath> res = get(source, us_paths[i], false, us_ignoredPaths);
            System.out.println("***SIZE: " + res.size() + ";" + us_paths[i] + ";\r\n" + res);
            Assert.assertTrue(res.size() == expectedSize[i]);
        }
    }

    @Test
    public void testJsonPath_ignoreCase_ignoredPaths() throws Exception {
        JsonParser parser = new JsonParser();
        String json = Utils.convertFormattedJson2Raw(new File("./src/test/java/us.json"));
        JsonObject source = parser.parse(json).getAsJsonObject();

        int[] expectedSize = {1, 1, 1, 4, 4, 2, 1, 4, 15, 2, 1, 0};

        final String[] us_paths = new String[]{
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

        final String[] us_ignoredPaths = new String[]{
                "PICTURE.mediaList[0].image.originalImg.URL",  //3
                "RETURNS.maxView.value[3].value[0].textSpans[0].action.URL" // 1
                , "THIRD_PARTY_RESOURCES.js[0].url"  // 1
                , "BINSUMMARY.minView.actions[1].action.URL" // 2
                , "$.modules.WATCH.watching.watchAction.action.URL" // 1
                , "$.modules.WATCH.watch.watchAction.action.URL"  // 1
                , "BINSUMMARY.minView.actions[2].value.cartSigninUrl.URL" // 2
        };

        for (int i = 0; i < us_paths.length; i++) {
            List<JsonElementWithPath> res = get(source, us_paths[i], true, us_ignoredPaths);
            System.out.println("***SIZE: " + res.size() + ";" + us_paths[i] + ";\r\n" + res);
            Assert.assertTrue(res.size() == expectedSize[i]);
        }
    }

    @Test
    public void testJsonPath_ignoreCase_ignoredPaths_noArray() throws Exception {
        JsonParser parser = new JsonParser();
        String json = Utils.convertFormattedJson2Raw(new File("./src/test/java/au.json"));
        JsonObject source = parser.parse(json).getAsJsonObject();

        final String au_path = "URL";

        final String[] au_ignoredPaths_noArray = new String[]{
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
        };


        int expectedSize = 15;
        List<JsonElementWithPath> res = get(source, au_path, true, au_ignoredPaths_noArray);
        System.out.println("***SIZE: " + res.size() + ";" + au_path + ";\r\n" + res);
        Assert.assertTrue(res.size() == expectedSize);
    }

    @Test
    public void testJsonPath_ignoreCase_ignoredPaths_hasArray() throws Exception {
        JsonParser parser = new JsonParser();
        String json = Utils.convertFormattedJson2Raw(new File("./src/test/java/au.json"));
        JsonObject source = parser.parse(json).getAsJsonObject();

        final String au_path = "URL";
        final String[] au_ignoredPaths_hasArray = new String[]{
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
                "$.modules.RETURNS.maxView.value[3:5]",
                "$.modules.BID_LAYER.powerbidButtons[*].action.URL",
                "$.modules.REWARDS.value.textSpans[1].action.URL"
        };

        int expectedSize = 9;
        List<JsonElementWithPath> res = get(source, au_path, true, au_ignoredPaths_hasArray);
        System.out.println("***SIZE: " + res.size() + ";" + au_path + ";\r\n" + res);
        Assert.assertTrue(res.size() == expectedSize);
    }

}
