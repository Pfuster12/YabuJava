package widget;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.yabu.android.yabujava.R;

import java.util.ArrayList;

import sql.KanjisContract.KanjisEntry;

/**
 * List View remote views implementation
 */
public class ListViewWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new ListRemoteViewsFactory(getApplicationContext());

    }

    private class ListRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

        private Context mContext;
        private Cursor mCursor;

        public ListRemoteViewsFactory(Context context) {
            mContext = context;
        }

        @Override
        public void onCreate() {

        }

        @Override
        public void onDataSetChanged() {
            // query the cursor to get new data
            // Set the query params for the definition query.
            String[] projection = new String[]{BaseColumns._ID,
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

            mCursor = mContext.getContentResolver().query(KanjisEntry.CONTENT_URI, projection, selection,
                    selArgs, null, null);
        }

        @Override
        public void onDestroy() {
            mCursor.close();
        }

        @Override
        public int getCount() {
            return mCursor.getCount();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            RemoteViews views = new RemoteViews(mContext.getPackageName(), R.layout.widget_item);
            mCursor.moveToPosition(position);

            String word =
                    mCursor.getString(mCursor.getColumnIndex(KanjisEntry.COLUMN_KANJI_WORD));
            String reading =
                    mCursor.getString(mCursor.getColumnIndex(KanjisEntry.COLUMN_KANJI_READING));
            String partsOfSpeech =
                    mCursor.getString(mCursor.getColumnIndex(KanjisEntry.COLUMN_KANJI_PARTS_OF_SPEECH));

            boolean isCommon = false;
            // Grab all the new data
            if (!mCursor.isNull(mCursor.getColumnIndex(KanjisEntry.COLUMN_IS_COMMON))) {
                int commonBool = mCursor.getInt(mCursor.getColumnIndex(KanjisEntry.COLUMN_IS_COMMON));
                isCommon = commonBool == 1;
            }
            int jlpt = -1;
            if (!mCursor.isNull(mCursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_1))) {
                jlpt = mCursor.getInt(mCursor.getColumnIndex(KanjisEntry.COLUMN_JLPT));
            }
            // init a definition list
            ArrayList<String> defs = new ArrayList<>();
            if (!mCursor.isNull(mCursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_1))) {
                String def1 = mCursor.getString(mCursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_1));
                defs.add(def1);
            }
            // Check for a second definition
            if (!mCursor.isNull(mCursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_2))) {
                String def2 = mCursor.getString(mCursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_2));
                defs.add(def2);
            }

            String url = "";
            // Check for a second definition
            if (!mCursor.isNull(mCursor.getColumnIndex(KanjisEntry.COLUMN_URL))) {
                url = mCursor.getString(mCursor.getColumnIndex(KanjisEntry.COLUMN_URL));
            }

            // Set title.
            views.setTextViewText(R.id.widget_review_title, word);
            // Set text.
            views.setTextViewText(R.id.widget_review_reading, reading);
            // set the definitions
            switch (defs.size()) {
                case 0: {
                    views.setTextViewText(R.id.widget_review_definition_1,
                            mContext.getString(R.string.no_definition));
                    views.setViewVisibility(R.id.widget_review_definition_2, View.GONE);
                    break;
                }
                case 1: {
                    if (defs.get(0).equals(mContext.getString(R.string.no_definition))) {
                        views.setTextViewText(R.id.widget_review_definition_1,
                                mContext.getString(R.string.no_definition));
                    } else {
                        views.setTextViewText(R.id.widget_review_definition_1,
                                mContext.getString(R.string.definition_1_placeholder, defs.get(0)));
                    }
                    views.setViewVisibility(R.id.widget_review_definition_2, View.GONE);
                }
                case 2: {
                    views.setViewVisibility(R.id.widget_review_definition_2, View.VISIBLE);
                    views.setTextViewText(R.id.widget_review_definition_1,
                            mContext.getString(R.string.definition_1_placeholder, defs.get(0)));
                    views.setTextViewText(R.id.widget_review_definition_2,
                            mContext.getString(R.string.definition_2_placeholder, defs.get(1)));
                }
            }

            // Check for the common tag
            if (isCommon) {
                views.setViewVisibility(R.id.widget_review_common_tag, View.VISIBLE);
            } else {
                views.setViewVisibility(R.id.widget_review_common_tag, View.GONE);
            }

            // Check the jlpt level and set color and text
            switch (jlpt) {
                case -1: views.setViewVisibility(R.id.widget_review_jlpt_tag, View.GONE);
                case 1: {
                    views.setViewVisibility(R.id.widget_review_jlpt_tag, View.VISIBLE);
                    views.setTextViewText(R.id.widget_review_jlpt_tag,
                            mContext.getString(R.string.JLPTN1));
                    break;
                }
                case 2: {
                    views.setViewVisibility(R.id.widget_review_jlpt_tag, View.VISIBLE);
                    views.setTextViewText(R.id.widget_review_jlpt_tag,
                            mContext.getString(R.string.JLPTN2));
                    break;
                }
                case 3: {
                    views.setViewVisibility(R.id.widget_review_jlpt_tag, View.VISIBLE);
                    views.setTextViewText(R.id.widget_review_jlpt_tag,
                            mContext.getString(R.string.JLPTN3));
                    break;
                }
                case 4: {
                    views.setViewVisibility(R.id.widget_review_jlpt_tag, View.VISIBLE);
                    views.setTextViewText(R.id.widget_review_jlpt_tag,
                            mContext.getString(R.string.JLPTN4));
                    break;
                }
                case 5: {
                    views.setViewVisibility(R.id.widget_review_jlpt_tag, View.VISIBLE);
                    views.setTextViewText(R.id.widget_review_jlpt_tag,
                            mContext.getString(R.string.JLPTN5));
                    break;
                }
            }

            if (!url.isEmpty()) {
                // if there is jisho data show details link
                views.setViewVisibility(R.id.widget_review_details_link, View.VISIBLE);
                // make fill intent with url to load web app
                Intent fillInIntent = new Intent();
                fillInIntent.putExtra(YabuWidget.URL_EXTRA, url);
                // Set a listener to know when the alpha ends to return to 1.0f alpha
                views.setOnClickFillInIntent(R.id.widget_review_details_link, fillInIntent);
            } else {
                // if there is jisho data show details link
                views.setViewVisibility(R.id.widget_review_details_link, View.INVISIBLE);
            }

            return views;
        }

        @Override
        public RemoteViews getLoadingView() {
            return new RemoteViews(mContext.getPackageName(), R.layout.callout_bubble_loading);

        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }
}
