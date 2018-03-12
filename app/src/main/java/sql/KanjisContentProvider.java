package sql;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import sql.KanjisContract.KanjisEntry;

/**
 * Content Provider to access the Kanjis database. The provider gives a second layer of
 * abstraction and safety to access the database.
 */
public class KanjisContentProvider extends ContentProvider {

    // Global db helper var
    private KanjisDbHelper mDbHelper;
    // Global db helper var
    private static UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private static final int KANJIS_ALL_ID = 100;
    private static final int KANJIS_SINGLE_ID = 101;

    static {
        sUriMatcher.addURI(KanjisContract.CONTENT_AUTHORITY,
                KanjisContract.KanjisEntry.TABLE_NAME, KANJIS_ALL_ID);
        sUriMatcher.addURI(KanjisContract.CONTENT_AUTHORITY,
                KanjisContract.KanjisEntry.TABLE_NAME + "/#", KANJIS_SINGLE_ID);
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new KanjisDbHelper(getContext());
        return true;
    }

    /**
     * Query function for the content provider. Takes in all columns to query
     */
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        Cursor cursor;

        // Match the uri to find which corresponds to
        switch (sUriMatcher.match(uri)) {
            // All the entries
            case KANJIS_ALL_ID: {
                // Get a readable db to query
                SQLiteDatabase database = mDbHelper.getReadableDatabase();
                cursor = database.query(KanjisEntry.TABLE_NAME, projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        null);
                break;
            }
            // One entry
            case KANJIS_SINGLE_ID: {
                // Get a readable db to query
                SQLiteDatabase database = mDbHelper.getReadableDatabase();
                cursor = database.query(KanjisEntry.TABLE_NAME, projection,
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

    /**
     * Function to get vnd type of each uri.
     */
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case KANJIS_ALL_ID: {
                return "vnd.android.cursor.dir/" +
                        "vnd.com.example.KanjisContentProvider." +
                        KanjisContract.KanjisEntry.TABLE_NAME;
            }
            case KANJIS_SINGLE_ID: {
                return "vnd.android.cursor.item/" +
                        "vnd.com.example.KanjisContentProvider." +
                        KanjisContract.KanjisEntry.TABLE_NAME;
            }
            default: throw new IllegalArgumentException("Uri not recognised");
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        Long id = Long.valueOf(0);

        switch (sUriMatcher.match(uri)) {
            case KANJIS_ALL_ID: {
                SQLiteDatabase database = mDbHelper.getWritableDatabase();
                id = database.insert(KanjisEntry.TABLE_NAME, null, values);
                break;
            }
            case KANJIS_SINGLE_ID: {
                // Do nothing
                break;
            }
            default: throw new IllegalArgumentException("Uri not recognised");
        }

        return Uri.withAppendedPath(uri, id.toString());
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
                database.insert(KanjisEntry.TABLE_NAME, null, value);
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
            case KANJIS_ALL_ID: {
                id = database.delete(KanjisEntry.TABLE_NAME, selection, null);
                break;
            }
            case KANJIS_SINGLE_ID: {
                // Delete the entry
                id = database.delete(KanjisEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }
            default: throw new IllegalArgumentException("Uri not recognised");
        }

        return id;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values,
                      @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        int id = 0;

        switch (sUriMatcher.match(uri)) {
            case KANJIS_ALL_ID: {
                // Do nothing
            }
            case KANJIS_SINGLE_ID: {
                String whereClause = KanjisEntry._ID + " = ?";
                String[] whereArgs = new String[]{uri.getLastPathSegment()};

                id = database.update(KanjisEntry.TABLE_NAME, values, whereClause, whereArgs);
            }
        }

        return id;
    }


}
