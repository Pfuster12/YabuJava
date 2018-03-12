package repository;

import jsondataclasses.WikiExtractsJSONResponse;
import jsondataclasses.WikiTitlesJSONResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Wiki API service interface using Retrofit library for seamless HTTP calls within Java/Kotlin.
 */
public interface WikiAPIService {

    // Set a constant of links limits.
    int titleLimits = 200;

    /**
     * GET HTTP function to return Wiki extracts from url. Query parameter for the title of
     * the article is added to the function via the @Query annotation. The rest of query
     * parameters are hard-coded since they are always needed.
     *
     * The query parameter 'titles' are added in a {Title1}|{Title2}|...etc. format.
     */
    @GET("w/api.php?action=query" +
            // Retrieve extracts and thumbnail image associated with article
            "&prop=extracts|pageimages" +
            "&redirects" +
            // Return extract in plain text (Boolean)
            "&explaintext=1" +
            // Return only the intro section (Boolean)
            "&exintro=1" +
            // Return the page ids as a list
            "&indexpageids" +
            // The max width of the thumbnail.
            "&pithumbsize=1080" +
            // Return in a JSON format.
            "&format=json" +
            // Return new json format with proper page array.
            "&formatversion=2")
    Call<WikiExtractsJSONResponse> requestExtracts(@Query("titles") String titles);

    /**
     * GET HTTP function to return a number of featured Wikipedia link titles. These
     * link titles will be chosen at random to then be inserted into the requestExtracts()
     * function that will bring the extracts of these titles.
     */
    @GET("w/api.php?action=query" +
            // Retrieve title links
            "&prop=links" +
            // Retrieve links in the main page
            "&titles=メインページ" +
            // Only return articles (Not users or meta pages)
            "&plnamespace=0" +
            // Return a limit number defined in companion object
            "&pllimit=" + titleLimits +
            // Return in json format
            "&format=json" +
            // Return new json format
            "&formatversion=2")
    Call<WikiTitlesJSONResponse> requestDailyTitles();
}
