package sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.util.Pair;
import android.util.Log;
import com.yabu.android.yabujava.R;
import com.yabu.android.yabujava.ui.MainActivity;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import jsondataclasses.Kanji;
import repository.JishoRepository;
import sql.KanjisContract.KanjisEntry;

/**
 * Class to contain helper methods to save internet loaded information into the
 * kanjis database for a persistent model
 */
public class KanjisSQLDao {

    public static KanjisSQLDao getInstance() {
            return new KanjisSQLDao();
        }

    /**
     * Helper fun to check if there is the kanji in the database already
     * and get the id. Returns -1 if it hasn't.
     */
    public Pair<Integer, Boolean> hasDefinition(Context context, Kanji kanji) {
        // init kanji id in the database to -1, ie there isn't
        int id = -1;
        boolean hasDef = false;

        // Set the query params
        String[] projection = new String[]{KanjisEntry._ID,
                KanjisEntry.COLUMN_KANJI_WORD,
                KanjisEntry.COLUMN_DEFINITION_1};
        String selection = KanjisEntry.COLUMN_KANJI_WORD + " = ?";
        String[] selArgs = new String[]{kanji.mWord};

        Cursor cursor = context.getContentResolver().query(KanjisEntry.CONTENT_URI, projection, selection,
                selArgs, null, null);

        // Check for empty cursor
        if (cursor != null && cursor.getCount() > 0){
            // Move to results
            cursor.moveToFirst();
            // Cycle through cursor
            do {
                id = cursor.getInt(cursor.getColumnIndex(KanjisEntry._ID));
                // Check for each result if the def column is null
                if (!cursor.isNull(cursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_1))) {
                    hasDef = true;
                    // If there is a result, get the id
                    id = cursor.getInt(cursor.getColumnIndex(KanjisEntry._ID));
                }
            } while (cursor.moveToNext());

            if (!cursor.isClosed()) {
                cursor.close();
            }
        }

        // Return id and boolean
        return new Pair<>(id, hasDef);
    }

    /**
     * Function to grab kanji with definitions from the database, among other info retrieved
     * from jisho from the single word search.
     */
    public Kanji getKanjiDefinition(final Context context, final Kanji kanji, final Integer id) {
        Future<Kanji> future = null;

        try {
            future = JishoRepository.executor.submit(new Callable<Kanji>() {
                @Override
                public Kanji call() throws Exception {
                    Kanji kanjiFromDatabase = kanji;
                    // Set the query params for the definition query.
                    String[] projection = new String[]{KanjisEntry._ID,
                            KanjisEntry.COLUMN_IS_COMMON,
                            KanjisEntry.COLUMN_JLPT,
                            KanjisEntry.COLUMN_DEFINITION_1,
                            KanjisEntry.COLUMN_DEFINITION_2,
                            KanjisEntry.COLUMN_URL,
                            KanjisEntry.COLUMN_IS_REVIEW};
                    String selection = KanjisEntry._ID + " = ?";
                    String[] selArgs = new String[]{id.toString()};

                    Cursor cursor = context.getContentResolver().query(KanjisEntry.CONTENT_URI, projection, selection,
                            selArgs, null, null);

                    // Check for empty cursor
                    if (cursor != null && cursor.getCount() > 0){
                        cursor.moveToFirst();
                        boolean isCommon = false;
                        // Grab all the new data
                        if (!cursor.isNull(cursor.getColumnIndex(KanjisEntry.COLUMN_IS_COMMON))) {
                            int commonBool = cursor.getInt(cursor.getColumnIndex(KanjisEntry.COLUMN_IS_COMMON));
                            isCommon = commonBool == 1;
                        }
                        int jlpt = -1;
                        if (!cursor.isNull(cursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_1))) {
                            jlpt = cursor.getInt(cursor.getColumnIndex(KanjisEntry.COLUMN_JLPT));
                        }
                        // init a definition list
                        ArrayList<String> defs = new ArrayList<>();
                        if (!cursor.isNull(cursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_1))) {
                            String def1 = cursor.getString(cursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_1));
                            defs.add(def1);
                        }
                        // Check for a second definition
                        if (!cursor.isNull(cursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_2))) {
                            String def2 = cursor.getString(cursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_2));
                            defs.add(def2);
                        }
                        String url = "";
                        // Check for a second definition
                        if (!cursor.isNull(cursor.getColumnIndex(KanjisEntry.COLUMN_URL))) {
                            url = cursor.getString(cursor.getColumnIndex(KanjisEntry.COLUMN_URL));
                        }

                        int isReviewBool = cursor.getInt(cursor.getColumnIndex(KanjisEntry.COLUMN_IS_REVIEW));
                        boolean isReview = isReviewBool == 1;

                        // Create the new kanji with data from database
                        kanjiFromDatabase = new Kanji(kanji.mWord, kanji.mReading,
                                kanji.mPartsOfSpeech, defs, isCommon, jlpt, url, isReview);

                        if (!cursor.isClosed()) {
                            cursor.close();
                        }
                    }
                    return kanjiFromDatabase;
                }
            });
        } catch (RejectedExecutionException e) {
        }

        // return the live data object.
        try {
            if (future != null) {
                return future.get(10, TimeUnit.SECONDS);
            } else {
                return kanji;
            }
        } catch (Exception e) {
            return kanji;
        }
    }


    /**
     * Dao fun to check if a wiki extract kanji has readings in the database already
     */
    public boolean isTodayWords(Context context) {
        int date = -1;
        // get todays day
        int today = java.util.Calendar.getInstance().get(java.util.Calendar.DATE);
        // Set the query params
        String[] projection = new String[]{KanjisEntry._ID,
                KanjisEntry.COLUMN_DATE};

        String selection = KanjisEntry.COLUMN_DATE + " = ?";

        Cursor cursor = context.getContentResolver().query(KanjisEntry.CONTENT_URI, projection, selection,
                null, null, null);

        // Check for empty cursor
        if (cursor != null && cursor.getCount() > 0) {
            // Move to results
            cursor.moveToFirst();
            date = cursor.getInt(cursor.getColumnIndex(KanjisEntry.COLUMN_DATE));
            // close cursor
            if (!cursor.isClosed()) {
                cursor.close();
            }

        }
        // Return id and boolean
        return date == today;
    }

    /**
     * Dao fun to check if a wiki extract kanji has readings in the database already
     */
    public boolean hasReadings(Context context, String wikiTitle) {
        // Set the query params
        String[] projection = new String[]{KanjisEntry._ID,
                KanjisEntry.COLUMN_SOURCE};
        String selection = KanjisEntry.COLUMN_SOURCE + " = ?";
        String[] selArgs = new String[]{wikiTitle};

        Cursor cursor = context.getContentResolver().query(KanjisEntry.CONTENT_URI, projection, selection,
                selArgs, null, null);
        boolean hasReadingsBool = false;
        if (cursor != null) {
            // Move to results
            cursor.moveToFirst();
            // Check for empty cursor
            hasReadingsBool = cursor.getCount() > 0;
            // close cursor
            if (!cursor.isClosed()) {
                cursor.close();
            }
        }

        // Return id and boolean
        return hasReadingsBool;
    }

    public ArrayList<Pair<Pair<Integer, Integer>, Kanji>> getReadings(final Context context, final String wikiTitle) {
        final ArrayList<Pair<Pair<Integer, Integer>, Kanji>> kanjiPairs = new ArrayList<>();

        Future<ArrayList<Pair<Pair<Integer, Integer>, Kanji>>> future = null;
        try {
            future =
                    JishoRepository.executor.submit(new Callable<ArrayList<Pair<Pair<Integer, Integer>, Kanji>>>() {
                        @Override
                        public ArrayList<Pair<Pair<Integer, Integer>, Kanji>> call() throws Exception {
                            // Set the query params for the definition query.
                            String[] projection = new String[]{KanjisEntry._ID,
                                    KanjisEntry.COLUMN_SOURCE,
                                    KanjisEntry.COLUMN_KANJI_WORD,
                                    KanjisEntry.COLUMN_KANJI_READING,
                                    KanjisEntry.COLUMN_KANJI_PARTS_OF_SPEECH,
                                    KanjisEntry.COLUMN_RANGE};
                            String selection = KanjisEntry.COLUMN_SOURCE + " = ?";
                            String[] selArgs = new String[]{wikiTitle};

                            Cursor cursor = context.getContentResolver().query(KanjisEntry.CONTENT_URI, projection, selection,
                                    selArgs, null, null);

                            // Check for empty cursor
                            if (cursor != null && cursor.getCount() > 0){
                                cursor.moveToFirst();
                                do {
                                    String word =
                                            cursor.getString(cursor.getColumnIndex(KanjisEntry.COLUMN_KANJI_WORD));
                                    String reading =
                                            cursor.getString(cursor.getColumnIndex(KanjisEntry.COLUMN_KANJI_READING));
                                    String partsOfSpeech =
                                            cursor.getString(cursor.getColumnIndex(KanjisEntry.COLUMN_SOURCE));
                                    String range =
                                            cursor.getString(cursor.getColumnIndex(KanjisEntry.COLUMN_RANGE));
                                    // split the range in two
                                    StringTokenizer ranges = new StringTokenizer(range, "..");
                                    ArrayList<String> rangesList = new ArrayList<>();

                                    // Create the new kanji with data from database
                                    Kanji kanji = new Kanji(word, reading, partsOfSpeech);
                                    while (ranges.hasMoreTokens()) {
                                        rangesList.add(ranges.nextToken());
                                    }
                                    Pair<Integer, Integer> intRange = new Pair<>(Integer.valueOf(rangesList.get(0)), Integer.valueOf(rangesList.get(1)));
                                    // add to list
                                    kanjiPairs.add(new Pair<>(intRange, kanji));
                                } while (cursor.moveToNext());

                                // close the cursor
                                if (!cursor.isClosed()) {
                                    cursor.close();
                                }
                            }

                            return kanjiPairs;
                        }
                    });
        } catch (RejectedExecutionException e) {
        }

        // return the live data object.
        try {
            if (future != null) {
                return future.get(10, TimeUnit.SECONDS);
            } else {
                return kanjiPairs;
            }
        } catch (Exception e) {
            return kanjiPairs;
        }
    }

    /**
     * Dao function to insert a new kanji entry to the database.
     */
    public void saveKanjiReadings(Context context,
                                  ArrayList<Pair<Pair<Integer, Integer>, Kanji>> kanjiPairs, String wikiTitle) {
        // get todays day
        int today = java.util.Calendar.getInstance().get(java.util.Calendar.DATE);

        ArrayList<ContentValues> values = new ArrayList<>();
        for (Pair<Pair<Integer, Integer>, Kanji> kanji : kanjiPairs) {
            ContentValues value = new ContentValues();
            value.put(KanjisEntry.COLUMN_DATE, today);
            value.put(KanjisEntry.COLUMN_SOURCE, wikiTitle);
            value.put(KanjisEntry.COLUMN_KANJI_WORD, kanji.second.mWord);
            value.put(KanjisEntry.COLUMN_KANJI_READING, kanji.second.mReading);
            value.put(KanjisEntry.COLUMN_KANJI_PARTS_OF_SPEECH, kanji.second.mPartsOfSpeech);
            value.put(KanjisEntry.COLUMN_RANGE, String.valueOf(kanji.first.first) + ".." + String.valueOf(kanji.first.second));
            value.put(KanjisEntry.COLUMN_IS_REVIEW, false);

            // put the value in the list
            values.add(value);
        }

        // insert kanji into database
        context.getContentResolver().bulkInsert(KanjisEntry.CONTENT_URI, values.toArray(new ContentValues[]{}));
    }

    /**
     * Dao function to update the existing kanji entry with the definitions returned
     * from a jisho call and html scrape in the Jisho Repo.
     */
    public void updateKanjiDefinition(Context context, Kanji kanji, Integer id) {
        ContentValues values = new ContentValues();
        values.put(KanjisEntry.COLUMN_KANJI_WORD, kanji.mWord);
        values.put(KanjisEntry.COLUMN_IS_COMMON, kanji.mIsCommon);
        values.put(KanjisEntry.COLUMN_JLPT, kanji.mJlptTag);
        values.put(KanjisEntry.COLUMN_URL, kanji.mUrl);
        if (kanji.mDefinitions.size() > 0) {
            values.put(KanjisContract.KanjisEntry.COLUMN_DEFINITION_1, kanji.mDefinitions.get(0));
        } else {
            values.put(KanjisContract.KanjisEntry.COLUMN_DEFINITION_1, context.getString(R.string.no_definition));
        }
        if (kanji.mDefinitions.size() > 1) {
            values.put(KanjisContract.KanjisEntry.COLUMN_DEFINITION_2, kanji.mDefinitions.get(0));
        }
        Uri entryUri = Uri.withAppendedPath(KanjisEntry.CONTENT_URI, id.toString());
        // update kanji entry
        context.getContentResolver().update(entryUri, values, null, null);
    }

    /**
     * Dao function to get all the kanji with the review boolean set to true in the
     * favorite button.
     */
    public ArrayList<Pair<Integer, Kanji>> getReviewKanjis(final Context context) {
        Future<ArrayList<Pair<Integer, Kanji>>> future = null;
        ArrayList<Pair<Integer, Kanji>> reviewWords = new ArrayList<>();

        // launch a worker thread to query the review words
        try {
            future =
                    MainActivity.executor.submit(new Callable<ArrayList<Pair<Integer, Kanji>>>() {
                        @Override
                        public ArrayList<Pair<Integer, Kanji>> call() throws Exception {
                            ArrayList<Pair<Integer, Kanji>> reviewWords = new ArrayList<>();
                            // Set the query params for the definition query.
                            String[] projection = new String[]{KanjisEntry._ID,
                                    KanjisEntry.COLUMN_KANJI_WORD,
                                    KanjisEntry.COLUMN_KANJI_READING,
                                    KanjisEntry.COLUMN_KANJI_PARTS_OF_SPEECH,
                                    KanjisEntry.COLUMN_DEFINITION_1,
                                    KanjisEntry.COLUMN_DEFINITION_2,
                                    KanjisEntry.COLUMN_IS_COMMON,
                                    KanjisEntry.COLUMN_JLPT,
                                    KanjisEntry.COLUMN_URL,
                                    KanjisEntry.COLUMN_IS_REVIEW};
                            String selection = KanjisEntry.COLUMN_IS_REVIEW + " = ?";
                            String[] selArgs = new String[]{String.valueOf(1)};

                            Cursor cursor = context.getContentResolver().query(KanjisEntry.CONTENT_URI, projection, selection,
                                    selArgs, null, null);

                            // Check for empty cursor
                            if (cursor != null && cursor.getCount() > 0){
                                cursor.moveToFirst();
                                do {
                                    String word =
                                            cursor.getString(cursor.getColumnIndex(KanjisEntry.COLUMN_KANJI_WORD));
                                    String reading =
                                            cursor.getString(cursor.getColumnIndex(KanjisEntry.COLUMN_KANJI_READING));
                                    String partsOfSpeech =
                                            cursor.getString(cursor.getColumnIndex(KanjisEntry.COLUMN_KANJI_PARTS_OF_SPEECH));

                                    boolean isCommon = false;
                                    // Grab all the new data
                                    if (!cursor.isNull(cursor.getColumnIndex(KanjisEntry.COLUMN_IS_COMMON))) {
                                        int commonBool = cursor.getInt(cursor.getColumnIndex(KanjisEntry.COLUMN_IS_COMMON));
                                        isCommon = commonBool == 1;
                                    }
                                    int jlpt = -1;
                                    if (!cursor.isNull(cursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_1))) {
                                        jlpt = cursor.getInt(cursor.getColumnIndex(KanjisEntry.COLUMN_JLPT));
                                    }
                                    // init a definition list
                                    ArrayList<String> defs = new ArrayList<>();
                                    if (!cursor.isNull(cursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_1))) {
                                        String def1 = cursor.getString(cursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_1));
                                        defs.add(def1);
                                    }
                                    // Check for a second definition
                                    if (!cursor.isNull(cursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_2))) {
                                        String def2 = cursor.getString(cursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_2));
                                        defs.add(def2);
                                    }
                                    String url = "";
                                    // Check for a second definition
                                    if (!cursor.isNull(cursor.getColumnIndex(KanjisEntry.COLUMN_URL))) {
                                        url = cursor.getString(cursor.getColumnIndex(KanjisEntry.COLUMN_URL));
                                    }

                                    int isReviewBool = cursor.getInt(cursor.getColumnIndex(KanjisEntry.COLUMN_IS_COMMON));
                                    boolean isReview = isReviewBool == 1;

                                    // Create the new kanji with data from database
                                    Kanji reviewKanji = new Kanji(word, reading,
                                            partsOfSpeech, defs, isCommon, jlpt, url, isReview);

                                    int entryId = cursor.getInt(cursor.getColumnIndex(KanjisEntry._ID));

                                    reviewWords.add(new Pair<>(entryId, reviewKanji));
                                } while (cursor.moveToNext());
                                // close the cursor
                                if (!cursor.isClosed()) {
                                    cursor.close();
                                }
                            }

                            return reviewWords;
                        }
                    });
        } catch (RejectedExecutionException e) {
        }

        // return the live data object.
        try {
            if (future != null) {
                return future.get(10, TimeUnit.SECONDS);
            } else {
                return reviewWords;
            }
        } catch (Exception e) {
            return reviewWords;
        }
    }

    /**
     * Dao function to update the review boolean in the table. This will make the word
     * part of the review list.
     */
    public Integer updateReviewKanji(final Context context, final Integer id, final Boolean isReview) {
        Future<Integer> future = null;

        try {
            future = MainActivity.executor.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    ContentValues values = new ContentValues();
                    values.put(KanjisEntry.COLUMN_IS_REVIEW, isReview);

                    Uri entryUri = Uri.withAppendedPath(KanjisEntry.CONTENT_URI, id.toString());
                    // update kanji entry
                    return context.getContentResolver().update(entryUri, values, null, null);
                }
            });
        } catch (RejectedExecutionException e) {
        }

        try {
            if (future != null) {
                return future.get(10, TimeUnit.SECONDS);
            } else {
                return -1;
            }
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Dao function to delete all non review kanjis
     */
    public void deleteYesterdayKanjis(Context context) {
        String whereClause = KanjisEntry.COLUMN_IS_REVIEW + " = ?";
        String[] whereArgs = new String[]{String.valueOf(0)};

        context.getContentResolver().delete(KanjisEntry.CONTENT_URI, whereClause, whereArgs);
    }
}
