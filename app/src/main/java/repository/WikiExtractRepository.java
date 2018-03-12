package repository;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import jsondataclasses.WikiExtract;
import jsondataclasses.WikiExtractsJSONResponse;
import jsondataclasses.WikiTitles;
import jsondataclasses.WikiTitlesJSONResponse;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;
import sql.WikiExtractsSQLDao;

/**
 * Repository class responsible for handling data operations. Mediates between different
 * data sources abstracting the logic from the ViewModel. Therefore the ViewModel does not
 * know from where the data comes from, since it does not need to. It is only concerned with
 * supplying it to the View.
 *
 * The repo implements the Retrofit Callback interface to override what happens on response
 * and on failure to the async launched by Call<T>.enqueue()
 */
public class WikiExtractRepository {

    public WikiExtractRepository() {

    }

    // Executor variable to execute in worker threads
    public static ExecutorService executor;

    // Instance getter helper function.
    public static WikiExtractRepository getInstance() {
        if (executor != null) {
            if (executor.isShutdown() || executor.isTerminated()) {
                executor = Executors.newCachedThreadPool();
            }
        } else {
            executor = Executors.newCachedThreadPool();
        }
        return new WikiExtractRepository();
    }

    // API endpoint address for Wiki API
    private static final String endpoint = "https://ja.wikipedia.org";

    /**
     * generates an instance of WikiAPIService from Retrofit. The endpoint url
     * is fed to the retrofit object to build the query call.
     */
        // companion create() fun for api service
        public static WikiAPIService create() {
            // The Retrofit class generates an implementation of the WikiAPIService interface.
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(endpoint)
                    .addConverterFactory(MoshiConverterFactory.create())
                    .build();
            return retrofit.create(WikiAPIService.class);
        }


    // Create a service instance from the companion object function.
    private WikiAPIService apiService = create();

    // Variable for the LiveData object to be set in the response callback of Retrofit.
    private MutableLiveData<ArrayList<WikiExtract>> data = new MutableLiveData<>();

    private WikiExtractsSQLDao wikiDao = WikiExtractsSQLDao.getInstance();

    /**
     * Repo function to launch an async through the retrofit Call and return the list of
     * daily titles to use and insert in the extract query. In onResponse the received titles
     * will be used for the getExtracts() parameter.
     */
    public LiveData<ArrayList<WikiExtract>> getDailyExtracts(Context context) {
        // check if extracts are old and need to be refreshed
        refreshExtracts(context);

        data.setValue(wikiDao.getWikiExtracts(context));
        // Return the live data object holding the list set by the getExtracts() onResponse.
        return data;
    }


    private boolean refreshExtracts(final Context context) {
        Future<Boolean> future = null;
        // execute worker thread
        try {
            future = executor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    Boolean isSaved = false;
                    // running in a background thread
                    // Check to see if the extracts need to refresh to the daily articles
                    boolean isToday = wikiDao.isToday(context);

                    if (!isToday && isConnected(context)) {
                        // refresh the data
                        // execute the call, and check for error
                        Response<WikiTitlesJSONResponse> response =
                                apiService.requestDailyTitles().execute();
                        if (response.isSuccessful()) {
                            // Update the database. The live data will automatically refresh.
                            // Grab the response which is the Build the titles query.
                            String titleQuery = buildTitlesQuery(response.body());

                            // With the query titles built, send the call to get the extracts
                            isSaved = saveExtracts(context, titleQuery);
                        } else {
                            // Error in web call.
                            Log.e("WikiRepo", "Titles Http request failed");
                        }
                    }
                    return isSaved;
                }
            });
        } catch (RejectedExecutionException e) {
            Log.e("WikiRepoRefresh", e.toString());
        }

        try {
            if (future != null) {
                return future.get(10, TimeUnit.SECONDS);
            } else {
                return false;
            }
        } catch (Exception e) {
            Log.e("WikiRepoRefresh", e.toString());
            return false;
        }
    }


    /**
     * Repo function to launch an async through the retrofit Call and return the LiveData
     * object to which the results will be set in the onResponse callbacks.
     */
    private Boolean saveExtracts(Context context, String titles) {
        int rows = -1;
        // execute the extracts query json call
        try {
            Response<WikiExtractsJSONResponse> response = apiService.requestExtracts(titles).execute();
            if (response.isSuccessful()) {
                // delete entries only if there is internet connection
                wikiDao.deleteYesterdayEntries(context);
                // Grab the json response
                WikiExtractsJSONResponse jsonResponse = response.body();
                // save the wiki extracts into the database
                if (jsonResponse != null) {
                    rows = wikiDao.saveWikiExtracts(context, jsonResponse.query.pages);
                }
            } else {
                Log.e("WikiRepo", "Extracts Http request failed");
            }
        } catch (IOException e) {
            Log.e("WikiRepoSave", e.toString());

        }

        return rows != -1;
    }

    /**
     * check if read in the database
     */
    public boolean isRead(Context context, WikiExtract wikiExtract) {
        return wikiDao.isRead(context, wikiExtract);
    }

    /**
     * set as read
     */
    public void setRead(Context context, WikiExtract wikiExtract, Boolean isRead) {
        wikiDao.setIsRead(context, wikiExtract, isRead);
    }

    /**
     * Helper fun to filter list out from number titles and Wikipedia meta pages.
     */
    private List<WikiTitles> filterExtractList(List<WikiTitles> titlesArg) {
        List<WikiTitles> titles = titlesArg;
        List<WikiTitles> toRemove = new ArrayList<>();

        // Check if there are titles since safe null boolean is not allowed for
        // filter() predicate.
        if (titles.size() != 0) {
            for (int i = 0; i <= 9; i++) {
                for (WikiTitles title : titles) {
                    if (title.title.startsWith(String.valueOf(i))) {
                        toRemove.add(title);
                    }
                }
            }
            // Filter to take out any extracts starting with an english character
            for (WikiTitles title : titles) {
                if (title.title.startsWith("w")) {
                    toRemove.add(title);
                }
            }
        }

        titles.removeAll(toRemove);
        // Return list
        return titles;
    }

    /**
     * Helper fun to build the title query string
     */
    private String buildTitlesQuery(WikiTitlesJSONResponse jsonResponse) {
        // init the title string empty.
        String buildTitleQuery = "";
        if (jsonResponse != null) {
            // Grab the container with the links of titles.
            List<WikiTitles> titles = jsonResponse.query.pages.get(0).links;
            // Filter the list to contain only proper titles.
            titles = filterExtractList(titles);

            // init a starting index.
            final Integer start = 10;
            // math op to find the range to get ~10 items, no matter the size of
            //  the list. Decimal results are rounded to the nearest int.
            int range = Math.round((titles.size() - start) / 10);

            for (int i = start; i < titles.size(); i += range) {
                // Grab the current title.
                String currentTitle = titles.get(i).title;
                // build the title string by adding '...|{Title}'
                if (i == start) {
                    buildTitleQuery = buildTitleQuery.concat(currentTitle);
                } else {
                    buildTitleQuery = buildTitleQuery.concat("|" + currentTitle);
                }
            }
        }

        // Return the query string
        return buildTitleQuery;
    }

    /**
     * Helper fun to check internet connection whether wi-fi or mobile
     */
    private boolean isConnected(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr != null) {
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        } else {
            return false;
        }
    }
}
