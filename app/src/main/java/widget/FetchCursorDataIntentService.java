package widget;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

/**
 * Intent service to handle the update action of the widget.
 */
public class FetchCursorDataIntentService extends IntentService {

    public FetchCursorDataIntentService() {
        super("FetchCursorDataIntentService");
    }

    public static final String ACTION_UPDATE = "com.yabu.android.yabujava.ACTION_UPDATE";

    /**
     * Sends the intent for the update action service.
     */
    public static void startActionUpdateReviewWords(Context context) {
        Intent intent = new Intent(context, FetchCursorDataIntentService.class);
        intent.setAction(ACTION_UPDATE);
        context.startService(intent);
    }

    /**
     * Handles the sent intent
     */
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent.getAction() != null) {
            String action = intent.getAction();
            if (ACTION_UPDATE.equals(action)) {
                handleUpdateAction();
            }
        }
    }

    private void handleUpdateAction() {
        // get vals from the widget manager
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.
                getAppWidgetIds(new ComponentName(this, YabuWidget.class));
        // Update all widgets
        YabuWidget.updateYabuWidgets(this, appWidgetManager, appWidgetIds);
    }
}
