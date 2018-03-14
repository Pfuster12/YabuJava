package sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jsondataclasses.WikiExtract;
import jsondataclasses.WikiThumbnail;
import repository.WikiExtractRepository;
import sql.WikiExtractsContract.WikiExtractsEntry;

/**
 * Class to contain helper methods to save and get internet loaded information into the
 * wiki extracts database for a persistent model.
 */
public class WikiExtractsSQLDao {

    public static WikiExtractsSQLDao getInstance() {
            return new WikiExtractsSQLDao();
        }

    /**
     * Function to check whether the articles loaded are from today's featured page. If not,
     * it will trigger a refresh to load the most recent ones from the web.
     */
    public Boolean isToday(Context context) {
        boolean isTodayBool = false;
        // set the query params
        String[] projection = new String[]{WikiExtractsContract.WikiExtractsEntry._ID,
                WikiExtractsEntry.COLUMN_DATE};
        String selection = WikiExtractsEntry.COLUMN_DATE + " = ?";
        int today = java.util.Calendar.getInstance().get(java.util.Calendar.DATE);
        String[] selArgs = new String[]{String.valueOf(today)};
        Cursor cursor = context.getContentResolver().query(WikiExtractsEntry.CONTENT_URI, projection, selection,
                selArgs, null, null);

        // check for empty cursor
        if (cursor != null && cursor.getCount() > 0){
            // move to results
            cursor.moveToFirst();
            // cycle through cursor
            int date = -1;
            // Check for null date entry
            if (!cursor.isNull(cursor.getColumnIndex(WikiExtractsEntry.COLUMN_DATE))) {
                date = cursor.getInt(cursor.getColumnIndex(WikiExtractsEntry.COLUMN_DATE));
            }

            // check if date saved is the same day as today
            if (today == date) {
                isTodayBool = true;
            }

            if (!cursor.isClosed()) {
                cursor.close();
            }
        }

        return isTodayBool;
    }

    /**
     * Function to check whether the article loaded has been marked as read
     */
    public boolean isRead(final Context context, final WikiExtract wikiExtract) {
        Future<Boolean> future = null;

        try {
            future = WikiExtractRepository.executor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    boolean isRead = false;
                    // set the query params
                    String[] projection = new String[]{WikiExtractsEntry._ID,
                            WikiExtractsEntry.COLUMN_TITLE,
                            WikiExtractsEntry.COLUMN_IS_READ};
                    String selection = WikiExtractsEntry.COLUMN_TITLE + " = ?";
                    String[] selArgs = new String[]{wikiExtract.title};

                    Cursor cursor = context.getContentResolver().query(WikiExtractsEntry.CONTENT_URI, projection, selection,
                            selArgs, null, null);

                    // check for empty cursor
                    if (cursor != null && cursor.getCount() > 0) {
                        // move to results
                        cursor.moveToFirst();
                        // cycle through cursor
                        isRead = 1 == cursor.getInt(cursor.getColumnIndex(WikiExtractsEntry.COLUMN_IS_READ));
                        if (!cursor.isClosed()) {
                            cursor.close();
                        }
                    }
                    return isRead;
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
     * Function to check whether the article loaded has been marked as read
     */
    public boolean setIsRead(final Context context, final WikiExtract wikiExtract, final Boolean isRead) {
        Future<Boolean> future = null;
        try {
            future = WikiExtractRepository.executor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    ContentValues values = new ContentValues();
                    values.put(WikiExtractsEntry.COLUMN_IS_READ, isRead);

                    // Set the query params
                    String[] projection = new String[]{WikiExtractsEntry._ID,
                            WikiExtractsEntry.COLUMN_TITLE};
                    String selection = WikiExtractsEntry.COLUMN_TITLE + " = ?";
                    String[] selArgs = new String[]{wikiExtract.title};

                    Cursor cursor = context.getContentResolver().query(WikiExtractsEntry.CONTENT_URI, projection, selection,
                            selArgs, null, null);

                    int id = -1;
                    // check for empty cursor
                    if (cursor != null && cursor.getCount() > 0) {
                        // Move to results
                        cursor.moveToFirst();
                        id = cursor.getInt(cursor.getColumnIndex(WikiExtractsEntry._ID));
                        if (!cursor.isClosed()) {
                            cursor.close();
                        }
                    }

                    Uri uri = Uri.withAppendedPath(WikiExtractsEntry.CONTENT_URI, String.valueOf(id));
                    int updatedInt = context.getContentResolver().
                            update(uri, values, null, null);

                    return updatedInt > 0;
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
     * Save the wiki extract into the database with a time stamp
     */
    public int saveWikiExtracts(Context context, List<WikiExtract> wikiExtracts) {
        // get todays day
        int today = java.util.Calendar.getInstance().get(java.util.Calendar.DATE);

        // init a list of content values
        ArrayList<ContentValues> values = new ArrayList<>();

        // loop through the extracts to make an array of content of values
        if (wikiExtracts != null) {
            for (WikiExtract extract : wikiExtracts) {
                ContentValues value = new ContentValues();
                value.put(WikiExtractsEntry.COLUMN_DATE, today);
                value.put(WikiExtractsEntry.COLUMN_IS_READ, false);
                value.put(WikiExtractsEntry.COLUMN_PAGE_ID, extract.pageId);
                value.put(WikiExtractsEntry.COLUMN_TITLE, extract.title);
                value.put(WikiExtractsEntry.COLUMN_EXTRACT, extract.extract);
                if (extract.thumbnail != null) {
                    value.put(WikiExtractsEntry.COLUMN_THUMBNAIL, extract.thumbnail.source);
                }

                values.add(value);
            }
        }

        // perform bulk insert to database to save the wiki extracts. returns rows added.
        return context.getContentResolver().bulkInsert(WikiExtractsEntry.CONTENT_URI,
                values.toArray(new ContentValues[]{}));
    }

    /**
     * Function to grab extracts from database
     */
    public ArrayList<WikiExtract> getWikiExtracts(final Context context) {
        // init a list of wiki extracts
        ArrayList<WikiExtract> wikiExtracts = new ArrayList<>();

        Future<ArrayList<WikiExtract>> future = null;
        try {
            future = WikiExtractRepository.executor.submit(new Callable<ArrayList<WikiExtract>>() {
                @Override
                public ArrayList<WikiExtract> call() throws Exception {
                    // init a list of wiki extracts
                    ArrayList<WikiExtract> wikiExtracts = new ArrayList<>();
                    // Set the query params for the definition query.
                    String[] projection = new String[]{WikiExtractsEntry._ID,
                            WikiExtractsEntry.COLUMN_PAGE_ID,
                            WikiExtractsEntry.COLUMN_TITLE,
                            WikiExtractsEntry.COLUMN_EXTRACT,
                            WikiExtractsEntry.COLUMN_THUMBNAIL};

                    Cursor cursor = context.getContentResolver().query(WikiExtractsEntry.CONTENT_URI, projection,
                            null, null, null, null);

                    // Check for empty cursor
                    if (cursor != null && cursor.getCount() > 0){
                        cursor.moveToFirst();
                        do {
                            // Grab all the new data
                            int pageId = cursor.getInt(cursor.getColumnIndex(WikiExtractsEntry.COLUMN_PAGE_ID));
                            String title = cursor.getString(cursor.getColumnIndex(WikiExtractsEntry.COLUMN_TITLE));
                            String extract = cursor.getString(cursor.getColumnIndex(WikiExtractsEntry.COLUMN_EXTRACT));
                            String thumbnail = cursor.getString(cursor.getColumnIndex(WikiExtractsEntry.COLUMN_THUMBNAIL));

                            // Create the new wiki extract with data from database
                            WikiExtract wikiExtract =
                                    new WikiExtract(pageId, title, extract, new WikiThumbnail(thumbnail));
                            wikiExtracts.add(wikiExtract);
                        } while (cursor.moveToNext());
                        if (!cursor.isClosed()) {
                            cursor.close();
                        }
                    }

                    return wikiExtracts;
                }
            });
        } catch (RejectedExecutionException e) {
        }

        try {
            if (future != null) {
                return future.get(10, TimeUnit.SECONDS);
            } else {
                return wikiExtracts;
            }
        } catch (Exception e) {
            return wikiExtracts;
        }
    }

    /**
     * Delete all entries.
     */
    public void deleteYesterdayEntries(Context context) {
        context.getContentResolver().delete(WikiExtractsEntry.CONTENT_URI, null, null);
    }
}
