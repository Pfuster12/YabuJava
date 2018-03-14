package repository;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.util.Pair;
import android.util.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import jsondataclasses.Kanji;
import jsondataclasses.WikiExtract;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import sql.KanjisSQLDao;
import utils.WordScanner;

/**
 * Repo class to retrieve data from the Jisho.org API service. The repo will provide
 * to the viewmodel the definitions from each kanji found in the text.
 */
public class JishoRepository {

    public JishoRepository() {
        // empty constructor
    }

    // API endpoint address for Wiki API
    public static final String endpoint = "http://jisho.org/";

    // create() fun for api service
    public static JishoAPIService create() {
        // The Retrofit class generates an implementation of the WikiAPIService interface.
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(endpoint)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        return retrofit.create(JishoAPIService.class);
    }

    // Executor variable to execute in worker threads
    public static ExecutorService executor;

    // Get an instance helper function
    public static JishoRepository getInstance() {
        if (executor != null) {
            if (executor.isShutdown() || executor.isTerminated()) {
                executor = Executors.newCachedThreadPool();
            }
        } else {
            executor = Executors.newCachedThreadPool();
        }
        return new JishoRepository();
    }

    // Create a jisho service instance
    private JishoAPIService apiService = create();

    // Variable for the LiveData Kanji pair to be set in the response callback of Retrofit.
    private MutableLiveData<ArrayList<Pair<Pair<Integer, Integer>, Kanji>>> readingData =
            new MutableLiveData<>();

    // Variable for the LiveData Kanji pair to be set in the response callback of Retrofit.
    private MutableLiveData<Pair<Integer, Kanji>>  wordData = new MutableLiveData<>();

    // SQL dao instance
    private KanjisSQLDao kanjiDao = KanjisSQLDao.getInstance();

    /**
     * Repo fun to retrieve the word furigana from Jisho as retrieved from the extract
     * texts by the word scanner.
     */
    public MutableLiveData<ArrayList<Pair<Pair<Integer, Integer>, Kanji>>> getWords(Context context, WikiExtract wikiExtract) {
        refreshWords(context, wikiExtract);

        readingData.setValue(kanjiDao.getReadings(context, wikiExtract.title));
        // Return the LiveData object that is updated when onResponse is called
        return readingData;
    }

    private boolean refreshWords(final Context context, final WikiExtract wikiExtract) {
        Future<Boolean> future = null;
        // launch a worker thread
        try {
            future = executor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() {
                    // check for furigana readings
                    boolean hasReadings = kanjiDao.hasReadings(context, wikiExtract.title);

                    if (!hasReadings && isConnected(context)) {
                        // check if words are from yesterday
                        boolean isToday = kanjiDao.isTodayWords(context);
                        if (!isToday) {
                            // delete only if there is internet to load new words.
                            kanjiDao.deleteYesterdayKanjis(context);
                        }
                        // Build the query string for the api call
                        String words = WordScanner.getUtils().buildJishoQuery(wikiExtract.extract);
                        // If there aren't, execute a web call
                        try  {
                            Response<String> response = apiService.getWordsFromJisho(words).execute();
                            if (response.isSuccessful()) {
                                // Grab the response body which is the string of the html.
                                String htmlString = response.body();

                                // Grab the kanji pojos from the html string by scraping with jsoup
                                List<Kanji> kanjis = parseHtmlForFurigana(htmlString);

                                // get the index and kanji pairs in the text
                                ArrayList<Pair<Pair<Integer, Integer>, Kanji>>  kanjiPairs =
                                        combineKanjisAndIndex(wikiExtract.extract, kanjis);

                                // save in database the pairs
                                kanjiDao.saveKanjiReadings(context, kanjiPairs, wikiExtract.title);
                                return true;
                            } else {
                                return false;
                            }
                        } catch (Exception e) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            });
        } catch (RejectedExecutionException e) {
        }

        try {
            if (future != null) {
                return future.get(10, TimeUnit.SECONDS);
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Repo fun to retrieve the definition of a word from Jisho as touched by a user to show
     * the callout bubble definition.
     */
    public LiveData<Pair<Integer, Kanji>> getDefinitions(Context context, Kanji protoKanji) {
        // see if there is an existing definition, if not do a retrofit call
        int id = refreshDefinitions(context, protoKanji);

        wordData.setValue(new Pair<>(id, kanjiDao.getKanjiDefinition(context, protoKanji, id)));
        // return a live data directly from the database
        return wordData;
    }

    /**
     * Helper function to get definitions if there isn't any.
     */
    private Integer refreshDefinitions(final Context context, final Kanji protoKanji) {
        Future<Integer> future = null;

        try {
            future = executor.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    String keyword = protoKanji.mWord;

                    // Check if it is one char to add the #kanji meta-tag
                    if (protoKanji.mWord.length() == 1) {
                        keyword = protoKanji.mWord + "#kanji";
                    }

                    // build the url
                    String url = "http://jisho.org/search/" + keyword;
                    // running in a background thread
                    // Check if word has definition
                    Pair<Integer, Boolean> id = kanjiDao.hasDefinition(context, protoKanji);

                    // Check to see if there isn't a definition
                    if (!id.second && isConnected(context)) {
                        // refresh the data
                        // execute the call, and check for error
                        Response<String> response = apiService.getDefinitionFromJisho(keyword).execute();
                        if (response.isSuccessful()) {
                            // Update the database. The live data will automatically refresh.
                            // Grab the response body which is the string of the html.
                            String htmlString = response.body();

                            // Parse html for definitions
                            Kanji kanji = parseHtmlForDefinitions(protoKanji, htmlString, url);

                            kanjiDao.updateKanjiDefinition(context, kanji, id.first);
                        } else {
                            // Error in web call.
                        }
                    }

                    return id.first;
                }
            });
        } catch (RejectedExecutionException e) {
        }

        try {
            if (future != null) {
                return future.get(10, TimeUnit.SECONDS);
            } else
                return -1;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Helper fun to scrape the jisho html for the word definitions and furigana using
     * the Jsoup library.
     */
    private List<Kanji> parseHtmlForFurigana(String html) {
        // init a list of Kanji pojos
        ArrayList<Kanji> kanjis = new ArrayList<>();

        // Parse the string response from the Retrofit call into a Jsoup html doc.
        Document jsoup = Jsoup.parse(html);

        // Grab the content of the word and furigana with the id zen_bar
        Element content = jsoup.getElementById("zen_bar");
        // Grab the class with each word as a list.
        Elements wordClass = content.getElementsByClass("clearfix japanese_word");

        // Iterate through the words class to grab the furigana and the kanji data of each word.
        for (Element word : wordClass) {
            // Grab the part of speech
            String partsOfSpeech = word.attr("data-pos");
            // Grab the kanji char
            String kanji = word
                    .getElementsByClass("japanese_word__text_with_furigana")
                    .text();
            // Grab the furigana chars
            String furigana = word
                    .getElementsByClass("japanese_word__furigana")
                    .text();

            // Create the Kanji pojo
            Kanji kanjiPojo = new Kanji(kanji, furigana, partsOfSpeech);
            // Add to the list.
            kanjis.add(kanjiPojo);
        }
        ArrayList<Kanji> filteredKanji = new ArrayList<>();

        // Filter the list
        for (Kanji kanji : kanjis) {
          if (!kanji.mWord.isEmpty()) {
              filteredKanji.add(kanji);
          }
        }

        // Return the list
        return filteredKanji;

    }

    /**
     * Get defintions
     * @return kanji
     */
    private Kanji parseHtmlForDefinitions(Kanji protoKanji, String html, String url) {
        // Boolean to see if common tag is present
        boolean isCommon = false;
        String jlptTag = "";
        int jlptid = -1;
        ArrayList<String> meaningList = new ArrayList<>();

        // Parse the string response from the Retrofit call into a Jsoup html doc.
        Document jsoup = Jsoup.parse(html);

        if (protoKanji.mWord.length() == 1) {
            // Grab the content of the exact word definition div
            Elements content = jsoup.getElementsByClass("kanji details");

            if (content.size() != 0) {
                // Grab the kanji meaning element
                Elements definitions = content.get(0).getElementsByClass("kanji-details__main-meanings");

                if (definitions.size() != 0) {
                    meaningList.add(definitions.get(0).text().trim());
                }

                Elements stats = content.get(0).getElementsByClass("kanji_stats");

                if (stats.size() != 0) {
                    Elements jlptElement = stats.get(0).getElementsByClass("jlpt");

                    if (jlptElement.size() != 0) {
                        String jlptText = jlptElement.get(0).text();

                        switch (jlptText) {
                            case "JLPT level N1": jlptid = 1;
                            break;
                            case "JLPT level N2": jlptid = 2;
                                break;
                            case "JLPT level N3": jlptid = 3;
                                break;
                            case "JLPT level N4": jlptid = 4;
                                break;
                            case "JLPT level N5": jlptid = 5;
                                break;
                        }
                    }
                }
            }
        } else {
            // Grab the content of the exact word definition div
            Elements content = jsoup.getElementsByClass("exact_block");

            if (content.size() != 0) {
                // Grab the tags element
                Elements tags = content.get(0).getElementsByClass("concept_light-status");

                // Check if there are tags
                if (tags.size() != 0) {
                    // Grab the common tag
                    Elements common = tags.get(0).
                            getElementsByClass("concept_light-tag concept_light-common success label");

                    // Check if there is a common tag
                    if (common.size() != 0) {
                        isCommon = true;
                    }

                    // Grab the JLPT tag
                    Elements jlptElement = tags.get(0).getElementsByClass("concept_light-tag label");

                    if (jlptElement.size() != 0) {
                        jlptTag = jlptElement.get(0).data();

                        switch (jlptTag) {
                            case "JLPT N1": jlptid = 1;
                            break;
                            case "JLPT N2": jlptid = 2;
                                break;
                            case "JLPT N3": jlptid = 3;
                                break;
                            case "JLPT N4": jlptid = 4;
                                break;
                            case "JLPT N5": jlptid = 5;
                                break;
                        }
                    }
                }

                // Grab the definitions
                Elements meanings = content.get(0).getElementsByClass("meaning-meaning");

                for (Element meaning : meanings) {
                    meaningList.add(meaning.text().trim());
                }
            }
        }

        // Return a kanji with definitions and tags.
        return new Kanji(protoKanji.mWord, protoKanji.mReading, protoKanji.mPartsOfSpeech,
                meaningList, isCommon, jlptid, url, false);
    }

    /**
     * Helper fun to combine the Kanjis from the html to the int ranges.
     */
    private ArrayList<Pair<Pair<Integer, Integer>, Kanji>> combineKanjisAndIndex(String extract,
                                                                         List<Kanji> kanjis) {
        // Init the index and kanji pairs
        ArrayList<Pair<Pair<Integer, Integer>, Kanji>> kanjiIndexPairs = new ArrayList<>();

        // Start a while loop with start index.
        int startIndex = 0;
        int i = 0;
        while (i < kanjis.size()) {
            // Grab the index of the kanji in the extract text.
            int index1 = extract.indexOf(kanjis.get(i).mWord, startIndex);

            // To get the int range, add the size of the kanji chars to index1
            int index2 = index1 + (kanjis.get(i).mWord.length() - 1);
            // Create the Int Range
            Pair<Integer, Integer> intRange = new Pair<>(index1, index2);

            // Add the pair to the list
            kanjiIndexPairs.add(new Pair<>(intRange, kanjis.get(i)));
            // Increment i
            i++;
            // Make startIndex the index of the kanji in the extract since the list is ascending.
            startIndex = index1 + 1;
        }


        // Return the kanji-index pairs
        return kanjiIndexPairs;
    }

    /**
     * Helper fun to check internet connection whether wi-fi or mobile
     */
    private boolean isConnected(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr != null) {
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            return (networkInfo != null && networkInfo.isConnected());
        } else {
            return false;
        }
    }
}
