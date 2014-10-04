package com.pghant.notifsaver;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.pghant.notifsaver.NotifSaverContract.Notifications;

public class NotifSaverWidgetService extends RemoteViewsService {
	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		return new NotifRemoteViewsFactory(getApplicationContext(), intent);
	}
}

class NotifRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
	private Context mContext;
	private Cursor mCursor;
	private int mAppWidgetId;
	
	public static final String TAG = "NotifRemoteViewsFactory";

	public NotifRemoteViewsFactory(Context context, Intent intent) {
		mContext = context;
		mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
				AppWidgetManager.INVALID_APPWIDGET_ID);
	}

	public void onDataSetChanged() {
		// Refresh the cursor
		if (mCursor != null) {
			mCursor.close();
		}
		// TODO: query ContentProvider to get new Cursor - choose how many to show at once
		String[] projectionToRetrieve = { Notifications._ID, Notifications.COLUMN_NAME_PACKAGE,
				Notifications.COLUMN_NAME_ICON_SMALL, Notifications.COLUMN_NAME_TITLE,
				Notifications.COLUMN_NAME_TIME, Notifications.COLUMN_NAME_REMOVED,
				Notifications.COLUMN_NAME_APPNAME, Notifications.COLUMN_NAME_TEXT,
				Notifications.COLUMN_NAME_FLAGS };
        String sortOrder = Notifications.COLUMN_NAME_TIME + " DESC LIMIT 10";
        mCursor = mContext.getContentResolver().query(Notifications.CONTENT_URI,
				projectionToRetrieve, null, null, sortOrder);
	}
	
/*	public RemoteViews getViewAt(int position) {
		Log.d(TAG, "getViewAt");
		if (!mCursor.moveToPosition(position)) {
			throw new IllegalStateException("Position not valid to move to: " + position);
		}

		String packageName = mCursor.getString(mCursor.getColumnIndex(Notifications.COLUMN_NAME_PACKAGE));

        Bitmap appIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_no_icon);
        Bitmap smallIcon = BitmapFactory.decodeResource(mContext.getResources(), android.R.drawable.ic_lock_lock);
        PackageManager pm = mContext.getPackageManager();
        try {
            ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            appIcon = ((BitmapDrawable) pm.getApplicationIcon(info)).getBitmap();
            Resources res = pm.getResourcesForApplication(info);
            smallIcon = BitmapFactory.decodeResource(res, mCursor.getInt(mCursor.getColumnIndex(Notifications.COLUMN_NAME_ICON_SMALL)));
        } catch (Exception e) {
            e.printStackTrace();
        }

		RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_item);
		rv.setTextViewText(R.id.widget_package_text, packageName);
        rv.setImageViewBitmap(R.id.widget_small_icon, appIcon);

		return rv;
	}*/

	public RemoteViews getViewAt(int position) {
		if (!mCursor.moveToPosition(position)) {
			throw new IllegalStateException("Position not valid to move to: " + position);
		}

		String packageName = mCursor.getString(mCursor
				.getColumnIndex(Notifications.COLUMN_NAME_PACKAGE));

		// default values for app icon, and notification small
		// icon
		Bitmap appIcon = BitmapFactory.decodeResource(mContext.getResources(),
				R.drawable.ic_no_icon);
		Bitmap smallIcon = BitmapFactory.decodeResource(mContext.getResources(),
				android.R.drawable.ic_lock_lock);

		// get the application icon from package manager
		PackageManager pm = mContext.getPackageManager();
		try {
			ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
			appIcon = ((BitmapDrawable) pm.getApplicationIcon(info)).getBitmap();
			Resources res = pm.getResourcesForApplication(info);
			smallIcon = BitmapFactory.decodeResource(res,
					mCursor.getInt(mCursor.getColumnIndex(Notifications.COLUMN_NAME_ICON_SMALL)));

		} catch (Exception e) {
			e.printStackTrace();
		}

		// set notification title and text views based on if they are empty or
		// not
		String notificationTitle = mCursor.getString(mCursor
				.getColumnIndex(Notifications.COLUMN_NAME_TITLE));
		if (notificationTitle == null || notificationTitle.equals(""))
			notificationTitle = mCursor.getString(mCursor
					.getColumnIndex(Notifications.COLUMN_NAME_APPNAME));
		String notificationText = mCursor.getString(mCursor
				.getColumnIndex(Notifications.COLUMN_NAME_TEXT));

		// convert the timestamp given in milliseconds from epoch to readable
		// time. Show only time for today's notifications, only date for
		// previous notifications
		long time = mCursor.getLong(mCursor.getColumnIndex(Notifications.COLUMN_NAME_TIME));
		GregorianCalendar now = new GregorianCalendar();
		GregorianCalendar then = new GregorianCalendar();
		now.setTimeInMillis(System.currentTimeMillis());
		then.setTimeInMillis(time);
		now.add(Calendar.DAY_OF_YEAR, -1);
		DateFormat df = null;
		if (then.get(Calendar.YEAR) == now.get(Calendar.YEAR)
				&& then.get(Calendar.DAY_OF_YEAR) <= now.get(Calendar.DAY_OF_YEAR)
				|| then.get(Calendar.YEAR) < now.get(Calendar.YEAR))
			df = DateFormat.getDateInstance(DateFormat.SHORT);
		else
			df = DateFormat.getTimeInstance(DateFormat.SHORT);
		String readableDate = df.format(new Date(time));

		RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_item);

		// TODO: update views within the RemoteViews object to the notification
		// details, also check translucency of removed notifications
		rv.setTextViewText(R.id.widget_text_notification_title, notificationTitle);
		rv.setTextViewText(R.id.widget_text_notification_text, notificationText);
		rv.setTextViewText(R.id.widget_text_notification_time, readableDate);
		rv.setImageViewBitmap(R.id.widget_image_app_icon, appIcon);
		rv.setImageViewBitmap(R.id.widget_image_small_icon, smallIcon);

		// if this notification was removed, then gray it out
		if (mCursor.getInt(mCursor.getColumnIndex(Notifications.COLUMN_NAME_REMOVED)) == 1) {
//			rv.setBoolean(R.id.widget_text_notification_title, "setEnabled", false);
//			rv.setBoolean(R.id.widget_text_notification_text, "setEnabled", false);
//			rv.setBoolean(R.id.widget_text_notification_time, "setEnabled", false);
			rv.setInt(R.id.widget_image_app_icon, "setImageAlpha", 125);
			rv.setInt(R.id.widget_image_small_icon, "setImageAlpha", 125);
		} else {
//			rv.setBoolean(R.id.widget_text_notification_title, "setEnabled", true);
//			rv.setBoolean(R.id.widget_text_notification_text, "setEnabled", true);
//			rv.setBoolean(R.id.widget_text_notification_time, "setEnabled", true);
			rv.setInt(R.id.widget_image_app_icon, "setImageAlpha", 255);
			rv.setInt(R.id.widget_image_small_icon, "setImageAlpha", 255);
		}

		return rv;
	}

	public int getViewTypeCount() {
		return 1;
	}

	public long getItemId(int position) {
		return mCursor.getLong(mCursor.getColumnIndex(Notifications._ID));
	}

	public void onCreate() {
	}

	public void onDestroy() {
		if (mCursor != null) {
			mCursor.close();
		}
	}

	public int getCount() {
        if (mCursor != null)
		    return mCursor.getCount();
        else
            return 0;
	}

	public boolean hasStableIds() {
		return true;
	}

	public RemoteViews getLoadingView() {
		return null;
	}
}
