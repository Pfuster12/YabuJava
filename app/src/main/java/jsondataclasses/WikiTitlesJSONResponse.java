package jsondataclasses;

/**
 * Moshi Json parsing object of Titles api call. Grabs a query json object containing a pages
 * object with the links property exposing the link titles in the Main Page.
 */
public class WikiTitlesJSONResponse {

    public WikiTitlesJSONQueryObject query;

    public WikiTitlesJSONResponse(WikiTitlesJSONQueryObject mQuery) {
        query = mQuery;
    }
}
