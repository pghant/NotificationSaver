package com.pghant.notifsaver;

import android.app.Notification;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import com.pghant.notifsaver.NotifSaverContract.Filters;
import com.pghant.notifsaver.NotifSaverContract.Notifications;

public class NotifSaverListenerService extends NotificationListenerService {

	public final static String TAG = "NotifSaverListenerService";
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		// TODO: check active notifications in case of reboot
	}

	@Override
	public void onNotificationPosted(StatusBarNotification sbn) {

		Bundle noteBundle = sbn.getNotification().extras;

		// check title before putting into database
		CharSequence notificationTitle = noteBundle.getCharSequence(Notification.EXTRA_TITLE);
		String title = "";
		if (notificationTitle != null && !"".equals(notificationTitle.toString().trim()))
			title = notificationTitle.toString().trim();

		// check text before putting into database
		String noteText = "";
		CharSequence[] lines = noteBundle.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
		if (lines != null) {
			for (CharSequence str : lines)
				noteText += str.toString().trim() + "\n";
			noteText = noteText.trim();
		}
		// get text if not inbox style
		if (noteText.equals("")) {
			CharSequence notificationText = noteBundle.getCharSequence(Notification.EXTRA_TEXT);
			if (notificationText != null && !"".equals(notificationText.toString().trim()))
				noteText = notificationText.toString().trim();
		}
		// get ticker if no text
		if (noteText.equals("")) {
			CharSequence ticker = sbn.getNotification().tickerText;
			if (ticker != null && !"".equals(ticker.toString().trim()))
				noteText = ticker.toString().trim();
		}
		// get content info if no ticker
		if (noteText.equals("")) {
			CharSequence info = noteBundle.getCharSequence(Notification.EXTRA_INFO_TEXT);
			if (info != null && !"".equals(info.toString().trim()))
				noteText = info.toString().trim();
		}

		String packageName = sbn.getPackageName();
		String appName = packageName;
		// get the application name and icon from package manager
		PackageManager pm = getPackageManager();
		try {
			ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
			appName = (String) pm.getApplicationLabel(info);

		} catch (Exception e) {
			e.printStackTrace();
		}

		// if small icon isn't stored in the bundle, get it directly from the
		// Notification object
		int smallIcon = noteBundle.getInt(Notification.EXTRA_SMALL_ICON);
		if (smallIcon == 0) {
			smallIcon = sbn.getNotification().icon;
		}

		ContentValues newValues = new ContentValues();
		newValues.put(Notifications.COLUMN_NAME_TEXT, noteText);
		newValues.put(Notifications.COLUMN_NAME_TITLE, title);
		newValues.put(Notifications.COLUMN_NAME_TIME, sbn.getPostTime());
		newValues.put(Notifications.COLUMN_NAME_PACKAGE, packageName);
		newValues.put(Notifications.COLUMN_NAME_ICON_SMALL, smallIcon);
		newValues.put(Notifications.COLUMN_NAME_NOTIFID, sbn.getId());
		newValues.put(Notifications.COLUMN_NAME_FLAGS, sbn.getNotification().flags);
		newValues.put(Notifications.COLUMN_NAME_APPNAME, appName);

		new NewNotificationTask().execute(newValues);

	}

	@Override
	public void onNotificationRemoved(StatusBarNotification sbn) {

		// get cleared notification package name and ID to update the removed
		// flag in database
		ContentValues vals = new ContentValues();
		vals.put(Notifications.COLUMN_NAME_PACKAGE, sbn.getPackageName());
		vals.put(Notifications.COLUMN_NAME_NOTIFID, sbn.getId());
		vals.put(Notifications.COLUMN_NAME_REMOVED, 1);
		vals.put(Notifications.COLUMN_NAME_DISMISSED, System.currentTimeMillis());

		new NotificationClearedTask().execute(vals);
	}

	private class NewNotificationTask extends AsyncTask<ContentValues, Void, Void> {

		@Override
		protected Void doInBackground(ContentValues... insertValues) {
			ContentResolver resolver = getContentResolver();

			// check filters to see if notification should be saved, exits out
			// otherwise
			if (!shouldSaveNotification(insertValues[0], resolver))
				return null;

			// update existing row if there is already existing record with the
			// same package name and notification id and has not been removed
			String where = Notifications.COLUMN_NAME_PACKAGE + " = '"
					+ insertValues[0].getAsString(Notifications.COLUMN_NAME_PACKAGE) + "' AND "
					+ Notifications.COLUMN_NAME_NOTIFID + " = "
					+ insertValues[0].getAsInteger(Notifications.COLUMN_NAME_NOTIFID) + " AND "
					+ Notifications.COLUMN_NAME_REMOVED + " = 0";
			int rows = resolver.update(Notifications.CONTENT_URI, insertValues[0], where, null);

			// if there weren't any rows to update, insert a new row
			if (rows == 0)
				resolver.insert(Notifications.CONTENT_URI, insertValues[0]);

			return null;
		}

		private boolean shouldSaveNotification(ContentValues vals, ContentResolver resolver) {
			String[] projection = { Filters.COLUMN_NAME_APPNAME, Filters.COLUMN_NAME_APPNAME_EXACT,
					Filters.COLUMN_NAME_PACKAGENAME, Filters.COLUMN_NAME_PACKAGENAME_EXACT,
					Filters.COLUMN_NAME_TEXT, Filters.COLUMN_NAME_TEXT_EXACT,
					Filters.COLUMN_NAME_TITLE, Filters.COLUMN_NAME_TITLE_EXACT };
			Cursor c = resolver.query(Filters.CONTENT_URI, projection, null, null, null);
			boolean shouldSave = true;

			// no filters
			if (c == null)
				return shouldSave;

			String[] filterCols = { Filters.COLUMN_NAME_APPNAME, Filters.COLUMN_NAME_PACKAGENAME,
					Filters.COLUMN_NAME_TITLE, Filters.COLUMN_NAME_TEXT };
			String[] notifCols = { Notifications.COLUMN_NAME_APPNAME,
					Notifications.COLUMN_NAME_PACKAGE, Notifications.COLUMN_NAME_TITLE,
					Notifications.COLUMN_NAME_TEXT };
			String[] exactCols = { Filters.COLUMN_NAME_APPNAME_EXACT,
					Filters.COLUMN_NAME_PACKAGENAME_EXACT, Filters.COLUMN_NAME_TITLE_EXACT,
					Filters.COLUMN_NAME_TEXT_EXACT };

			while (c.moveToNext() && shouldSave) {
				for (int i = 0; i < filterCols.length; i++) {
					boolean exact = false;
					String filterParam = c.getString(c.getColumnIndex(filterCols[i]));
					String notifParam = vals.getAsString(notifCols[i]);
					if (!TextUtils.isEmpty(filterParam)) {
						exact = c.getInt(c.getColumnIndex(exactCols[i])) == 1;
						if (exact)
							shouldSave = !filterParam.equals(notifParam);
						else
							shouldSave = !notifParam.contains(filterParam);
					}
				}
			}

			c.close();
			return shouldSave;
		}
	}

	private class NotificationClearedTask extends AsyncTask<ContentValues, Void, Void> {

		@Override
		protected Void doInBackground(ContentValues... contentValues) {
			ContentResolver resolver = getContentResolver();

			// update removed flag and dismissed time for notification with
			// specified package and notification ID
			String where = Notifications.COLUMN_NAME_PACKAGE + " = '"
					+ contentValues[0].getAsString(Notifications.COLUMN_NAME_PACKAGE) + "' AND "
					+ Notifications.COLUMN_NAME_NOTIFID + " = "
					+ contentValues[0].getAsInteger(Notifications.COLUMN_NAME_NOTIFID);
			resolver.update(Notifications.CONTENT_URI, contentValues[0], where, null);

			return null;
		}
	}

}
