package com.pghant.notifsaver;

import com.pghant.notifsaver.NotifSaverContract.Notifications;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.RemoteViews;

public class NotifSaverWidgetProvider extends AppWidgetProvider {
	
	public static final String TAG = "NotifSaverWidgetProvider";
	
	private static NotifSaverProviderObserver sDataObserver;
	private static HandlerThread sWorkerThread;
	private static Handler sWorkerQueue;
	
	public NotifSaverWidgetProvider() {
		// start worker thread
		sWorkerThread = new HandlerThread("NotifSaverWidgetProvider-worker");
		sWorkerThread.start();
		sWorkerQueue = new Handler(sWorkerThread.getLooper());
	}
	
	@Override
	public void onEnabled(Context context) {
		// register the data observer when a widget is created to update the widget when there are changes
		final ContentResolver r = context.getContentResolver();
		if (sDataObserver == null) {
			final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
			final ComponentName cn = new ComponentName(context, NotifSaverWidgetProvider.class);
			sDataObserver = new NotifSaverProviderObserver(mgr, cn, sWorkerQueue);
			r.registerContentObserver(Notifications.CONTENT_URI, true, sDataObserver);
		}
	}
	
	@Override
	public void onDisabled(Context context) {
		// unregister data observer when no widgets and clear thread
		final ContentResolver r = context.getContentResolver();
		if (r != null && sDataObserver != null)
            r.unregisterContentObserver(sDataObserver);
		sDataObserver = null;
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		// update layout for each app widget
		for (int i = 0; i < appWidgetIds.length; i++) {
			RemoteViews layout = buildLayout(context, appWidgetIds[i]);
			appWidgetManager.updateAppWidget(appWidgetIds[i], layout);
		}
		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}

	private RemoteViews buildLayout(Context context, int appWidgetId) {
		// instantiate RemoteViews with widget_container layout and specify
		// intent for service that provides data to widget
		RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_container);
		final Intent intent = new Intent(context, NotifSaverWidgetService.class);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
		rv.setRemoteAdapter(R.id.widget_list, intent);
		rv.setEmptyView(R.id.widget_list, R.id.widget_empty);
		
		return rv;
	}
}

// observes ContentProvider to update widgets when there are new notifications
class NotifSaverProviderObserver extends ContentObserver {
	private AppWidgetManager mAppWidgetManager;
	private ComponentName mComponentName;
	public static final String TAG = "NotifSaverProviderObserver";
	
	NotifSaverProviderObserver(AppWidgetManager mgr, ComponentName cn, Handler h) {
		super(h);
		mAppWidgetManager = mgr;
		mComponentName = cn;
	}
	
	@Override
	public void onChange(boolean selfChange) {
		mAppWidgetManager.notifyAppWidgetViewDataChanged(mAppWidgetManager.getAppWidgetIds(mComponentName), R.id.widget_list);
	}
}