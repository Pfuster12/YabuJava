package sql;


import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Contract class for the Kanjis SQLite database
 */
public class KanjisContract {

    private KanjisContract() {

    }

    public static final String CONTENT_AUTHORITY = "com.yabu.android.yabujava";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String  PATH_NAME = "kanjis";

    // Columns inner class for entries in Kanji tables.
    public static class KanjisEntry implements BaseColumns {

        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_NAME);

        public static final String TABLE_NAME = "kanjis";
        public static final String COLUMN_SOURCE = "source";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_KANJI_WORD = "word";
        public static final String COLUMN_KANJI_READING = "reading";
        public static final String COLUMN_KANJI_PARTS_OF_SPEECH = "parts_of_speech";
        public static final String COLUMN_IS_COMMON = "common";
        public static final String COLUMN_JLPT = "jlpt";
        public static final String COLUMN_DEFINITION_1 = "definition_1";
        public static final String COLUMN_DEFINITION_2 = "definition_2";
        public static final String COLUMN_URL = "url";
        public static final String COLUMN_IS_REVIEW = "review";
        public static final String COLUMN_RANGE = "range";

    }
}
