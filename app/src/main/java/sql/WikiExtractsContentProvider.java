package sql;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import sql.WikiExtractsContract.WikiExtractsEntry;

/**
 * Content Provider to access the Wiki extracts database. The provider gives a second layer of
 * abstraction and safety to access the database.
 */
public class WikiExtractsContentProvider extends ContentProvider {
    // Global db helper var
    private WikiExtractsDbHelper mDbHelper;

    private static UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private static final int WIKI_ALL_ID = 100;
    private static final int WIKI_SINGLE_ID = 101;

    // Add the Uri for all the table and a single Id to the matcher
    static {
        sUriMatcher.addURI(WikiExtractsContract.CONTENT_AUTHORITY,
                WikiExtractsEntry.TABLE_NAME, WIKI_ALL_ID);
        sUriMatcher.addURI(WikiExtractsContract.CONTENT_AUTHORITY,
                WikiExtractsEntry.TABLE_NAME + "/#", WIKI_SINGLE_ID);
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new WikiExtractsDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        // Get a readable db to query
        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        Cursor cursor;

        // Match the uri to find which corresponds to
        switch (sUriMatcher.match(uri)) {
            // All the entries
            case WIKI_ALL_ID: {
                cursor = database.query(WikiExtractsEntry.TABLE_NAME, projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        null);
                break;
            }
            // One entry
            case WIKI_SINGLE_ID: {
                cursor = database.query(WikiExtractsEntry.TABLE_NAME, projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        null);
                break;
            }
            default: throw new IllegalArgumentException("Uri not recognised");
        }

        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case WIKI_ALL_ID: {
                return "vnd.android.cursor.dir/" +
                        "vnd.com.example.KanjisContentProvider." +
                        WikiExtractsEntry.TABLE_NAME;
            }
            case WIKI_SINGLE_ID: {
                return "vnd.android.cursor.item/" +
                        "vnd.com.example.KanjisContentProvider." +
                        WikiExtractsEntry.TABLE_NAME;
            }
            default: throw new IllegalArgumentException("Uri not recognised");
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        long id = 0;

        switch (sUriMatcher.match(uri)) {
            case WIKI_ALL_ID: {
                id = database.insert(WikiExtractsEntry.TABLE_NAME, null, values);
                break;
            }
            case WIKI_SINGLE_ID: {
                // Do nothing
                break;
            }
            default: throw new IllegalArgumentException("Uri not recognised");
        }

        return Uri.withAppendedPath(uri, String.valueOf(id));
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // begin a transaction for a bulk insert.
        database.beginTransaction();
        int rows = 0;
        try {
            // Loop for every wiki extract
            for (ContentValues value : values) {
                // insert into database
                database.insert(WikiExtractsEntry.TABLE_NAME, null, value);
                rows++;
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }

        return rows;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        int id = 0;

        switch (sUriMatcher.match(uri)) {
            case WIKI_ALL_ID: {
                id = database.delete(WikiExtractsEntry.TABLE_NAME, selection, null);
                break;
            }
            case WIKI_SINGLE_ID: {
                String sel = WikiExtractsEntry._ID + " = ?";
                String[] selArgs = new String[]{uri.getLastPathSegment()};

                // Delete the entry
                id = database.delete(WikiExtractsEntry.TABLE_NAME, sel, selArgs);
                break;
            }
            default: throw new IllegalArgumentException("Uri not recognised");
        }

        return id;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        int id = 0;

        switch (sUriMatcher.match(uri)) {
            case WIKI_ALL_ID: {
                // Do nothing
            }
            case WIKI_SINGLE_ID: {
                String whereClause = WikiExtractsEntry._ID + " = ?";
                String[] whereArgs = new String[]{uri.getLastPathSegment()};

                id = database.update(WikiExtractsEntry.TABLE_NAME, values, whereClause, whereArgs);
            }
        }

        return id;
    }

}
