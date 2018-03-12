package sql;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import sql.KanjisContract.KanjisEntry;

/**
 * SQLite database creator helper.
 */
class KanjisDbHelper extends SQLiteOpenHelper {

    public KanjisDbHelper(Context context) {
        super(context, "kanjis.db", null, 1);
    }

    private static final String SQL_CREATE_ENTRIES = "CREATE TABLE " + KanjisEntry.TABLE_NAME + " (" +
            KanjisEntry._ID + " INTEGER PRIMARY KEY," +
            KanjisEntry.COLUMN_KANJI_WORD + " TEXT NOT NULL," +
            KanjisEntry.COLUMN_DATE + " INTEGER NOT NULL," +
            KanjisEntry.COLUMN_SOURCE + " TEXT NOT NULL," +
            KanjisEntry.COLUMN_KANJI_READING + " TEXT," +
            KanjisEntry.COLUMN_KANJI_PARTS_OF_SPEECH + " TEXT," +
            // Boolean values are 0 (false) and 1 (true).
            KanjisEntry.COLUMN_IS_COMMON + " INTEGER," +
            KanjisEntry.COLUMN_JLPT + " INTEGER," +
            KanjisEntry.COLUMN_DEFINITION_1 + " TEXT," +
            KanjisEntry.COLUMN_DEFINITION_2 + " TEXT," +
            KanjisEntry.COLUMN_RANGE + " TEXT," +
            KanjisEntry.COLUMN_IS_REVIEW + " INTEGER," +
            KanjisEntry.COLUMN_URL + " TEXT)";

    /*
  ----- ID ----    KANJI WORD   ----  KANJI READING  --- etc..
  ------------------------------------------------------------
  -----  1 ----     eg.         ----      blabal     --- etc..
  -----  2 ----    ex..         ----    kqkweoidk5   --- etc..
   */

    private static final String DROP_TABLE = "DROP TABLE IF EXISTS " + KanjisEntry.TABLE_NAME;

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
