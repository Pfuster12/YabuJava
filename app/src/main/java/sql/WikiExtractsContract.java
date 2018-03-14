package sql;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Contract class for the Wiki extracts SQLite database
 */
public class WikiExtractsContract {

    public static final String CONTENT_AUTHORITY = "com.yabu.android.yabujava.wiki";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_NAME = "wikiextracts";

    // Columns inner class for entries in Kanji tables.
    public static class WikiExtractsEntry implements BaseColumns {

        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_NAME);

        public static final String TABLE_NAME = "wikiextracts";
        public static final String COLUMN_IS_READ = "isread";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_PAGE_ID = "pageid";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_EXTRACT = "extract";
        public static final String COLUMN_THUMBNAIL = "thumbnail";
    }
}
