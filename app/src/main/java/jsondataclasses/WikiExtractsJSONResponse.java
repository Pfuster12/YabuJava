package jsondataclasses;

/**
 * Moshi Json parsing object of Extracts api call. Grabs a query json object containing a pages
 * array with the different extracts.
 */
public class WikiExtractsJSONResponse {

    public  WikiExtractsJSONQueryObject query;

    public WikiExtractsJSONResponse(WikiExtractsJSONQueryObject mQuery) {
         query = mQuery;
    }
}
