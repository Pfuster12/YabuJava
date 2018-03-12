package repository;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Web Service class for the Jisho API returning word definitions. @GET Retrofit functions
 * take in the keyword parameter extracted from the wiki extract and send an api call to
 * get the definition.
 */
public interface JishoAPIService {

    /**
     * GET function of Retrofit to return word furigana from Jisho.
     * The api calls for a query of the inputted keyword. Returns a string of html to scrape.
     */
    @GET("search/{keyword}")
    Call<String> getWordsFromJisho(@Path("keyword") String keyword);

    /**
     * GET function of Retrofit to return word definitions from Jisho.
     * The api calls for a query hof the inputted keyword. Returns a string of html to scrape.
     */
    @GET("search/{keyword}")
    Call<String> getDefinitionFromJisho(@Path("keyword") String keyword);
}
