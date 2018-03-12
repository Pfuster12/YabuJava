package jsondataclasses;

import java.util.List;

/**
 * Object holding the query. Grabs a pages object containing the list of wiki extracts.
 */
public class WikiExtractsJSONQueryObject {

    public List<WikiExtract> pages;

    public WikiExtractsJSONQueryObject(List<WikiExtract> mPages) {
        pages = mPages;
    }
}

