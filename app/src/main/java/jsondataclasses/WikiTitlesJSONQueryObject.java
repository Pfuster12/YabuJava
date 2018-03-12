package jsondataclasses;

import java.util.List;

/**
 * Object of query key in titles json response. Grabs a pages json object
 * containing the object with the links property exposing the titles in the Main Page.
 */
public class WikiTitlesJSONQueryObject {

    public List<WikiTitlesJSONContainer> pages;

    public WikiTitlesJSONQueryObject(List<WikiTitlesJSONContainer> mPages) {
        pages = mPages;
    }
}
