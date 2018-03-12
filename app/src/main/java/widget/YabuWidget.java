package widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.yabu.android.yabujava.R;

/**
 * Implementation of App Widget functionality.
 */
public class YabuWidget extends AppWidgetProvider {

    public static final String URL_ACTION = "com.yabu.android.yabujava.URL_ACTION";
    public static final String URL_EXTRA = "com.yabu.android.yabujava.URL_EXTRA";

    /**
     * update widgets fun
     */
    public static void updateYabuWidgets(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    /**
     * update a single widget with the manager
     */
    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // update widget
        RemoteViews views = getListViewRemoteView(context, appWidgetId);
        appWidgetManager.updateAppWidget(appWidgetId, views);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list_view);
    }

    /**
     * get the list view remote view and set the onclick pending template
     */
    private static RemoteViews getListViewRemoteView(Context context, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.yabu_widget_list);
        // set the listview widget service intent to act as the adapter for the listview
        Intent intent = new Intent(context, ListViewWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        views.setRemoteAdapter(R.id.widget_list_view, intent);

        // Set on click behaviour for each item
        Intent urlIntent = new Intent(context, YabuWidget.class);
        urlIntent.setAction(YabuWidget.URL_ACTION);
        PendingIntent urlPendingIntent = PendingIntent.getBroadcast(context, 0, urlIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        views.setPendingIntentTemplate(R.id.widget_list_view, urlPendingIntent);
        views.setEmptyView(R.id.widget_list_view, R.id.empty_view);

        return views;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        FetchCursorDataIntentService.startActionUpdateReviewWords(context);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view);
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    /**
     * Override function to receive broadcast of pending intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null){
            if (intent.getAction().equals(URL_ACTION)) {
                String url = intent.getStringExtra(YabuWidget.URL_EXTRA);
                if (url != null) {
                    // Set the url with an intent.
                    Uri webpage = Uri.parse(url);
                    Intent intentWeb = new Intent(Intent.ACTION_VIEW, webpage);
                    if (intent.resolveActivity(context.getPackageManager()) != null) {
                        context.startActivity(intentWeb);
                    }
                }
            }
        }
        super.onReceive(context, intent);
    }
}

