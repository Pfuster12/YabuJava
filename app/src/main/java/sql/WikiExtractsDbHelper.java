package sql;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import sql.WikiExtractsContract.WikiExtractsEntry;

/**
 * SQLite database creator helper.
 */
public class WikiExtractsDbHelper extends SQLiteOpenHelper {

    public WikiExtractsDbHelper(Context context) {
        super(context, "wikiextracts.db", null, 1);
    }

    private static final String SQL_CREATE_ENTRIES = "CREATE TABLE " + WikiExtractsEntry.TABLE_NAME + " (" +
            WikiExtractsEntry._ID + " INTEGER PRIMARY KEY," +
            WikiExtractsEntry.COLUMN_IS_READ + " INTEGER," +
            WikiExtractsEntry.COLUMN_PAGE_ID + " INTEGER," +
            WikiExtractsEntry.COLUMN_DATE + " INTEGER," +
            WikiExtractsEntry.COLUMN_TITLE + " TEXT NOT NULL," +
            WikiExtractsEntry.COLUMN_EXTRACT + " TEXT," +
            WikiExtractsEntry.COLUMN_THUMBNAIL + " TEXT)";

    /*
  ----- ID ----   WIKI TITLE    ----  WIKI EXTRACT   --- etc..
  ------------------------------------------------------------
  -----  1 ----     eg.         ----      blabal     --- etc..
  -----  2 ----    ex..         ----    kqkweoidk5   --- etc..
   */

    private static final String DROP_TABLE = "DROP TABLE IF EXISTS " + WikiExtractsEntry.TABLE_NAME;

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_TABLE);
        onCreate(db);
    }
}
