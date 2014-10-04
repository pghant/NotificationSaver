package com.pghant.notifsaver;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.pghant.notifsaver.NotifSaverContract.Notifications;

public class NotifCursorAdapter extends CursorAdapter {

	public static final String TAG = "NotifCursorAdapter";

	public NotifCursorAdapter(Context context, Cursor c, int flags) {
		super(context, c, flags);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		String packageName = cursor.getString(cursor
				.getColumnIndex(Notifications.COLUMN_NAME_PACKAGE));

		// default values for app icon, and notification small
		// icon
		Drawable appIcon = context.getResources().getDrawable(R.drawable.ic_no_icon);
		Drawable smallIcon = context.getResources().getDrawable(android.R.drawable.ic_lock_lock);

		// get the application icon from package manager
		PackageManager pm = context.getPackageManager();
		try {
			ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
			appIcon = pm.getApplicationIcon(info);
			Resources res = pm.getResourcesForApplication(info);
			smallIcon = res.getDrawable(cursor.getInt(cursor
					.getColumnIndex(Notifications.COLUMN_NAME_ICON_SMALL)));

		} catch (Exception e) {
			e.printStackTrace();
		}

		// set notification title and text views based on if they are empty or
		// not
		String notificationTitle = cursor.getString(cursor
				.getColumnIndex(Notifications.COLUMN_NAME_TITLE));
		if (notificationTitle == null || notificationTitle.equals(""))
			notificationTitle = cursor.getString(cursor.getColumnIndex(Notifications.COLUMN_NAME_APPNAME));
		String notificationText = cursor.getString(cursor
				.getColumnIndex(Notifications.COLUMN_NAME_TEXT));

		// convert the timestamp given in milliseconds from epoch to readable
		// time. Show only time for today's notifications, only date for
		// previous notifications
		long time = cursor.getLong(cursor.getColumnIndex(Notifications.COLUMN_NAME_TIME));
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

		// bind the data from the cursor to the views in the ViewHolder
		ViewHolder holder = (ViewHolder) view.getTag();

		holder.titleView.setText(notificationTitle);
		holder.noteTextView.setText(notificationText);
		holder.appIconView.setImageDrawable(appIcon);
		holder.smallIconView.setImageDrawable(smallIcon);
		holder.timeView.setText(readableDate);

		// if this notification was removed, then gray it out
		if (cursor.getInt(cursor.getColumnIndex(Notifications.COLUMN_NAME_REMOVED)) == 1) {
			holder.titleView.setEnabled(false);
			holder.appIconView.setImageAlpha(125);
			holder.noteTextView.setEnabled(false);
			holder.smallIconView.setImageAlpha(125);
			holder.timeView.setEnabled(false);
		} else {
			holder.titleView.setEnabled(true);
			holder.appIconView.setImageAlpha(255);
			holder.noteTextView.setEnabled(true);
			holder.smallIconView.setImageAlpha(255);
			holder.timeView.setEnabled(true);
		}

	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.list_item_notification, parent, false);

		// find all the views and put them in ViewHolder object
		ViewHolder holder = new ViewHolder();
		holder.appIconView = (ImageView) v.findViewById(R.id.image_app_icon);
		holder.noteTextView = (TextView) v.findViewById(R.id.text_notification_text);
		holder.smallIconView = (ImageView) v.findViewById(R.id.image_small_icon);
		holder.timeView = (TextView) v.findViewById(R.id.text_notification_time);
		holder.titleView = (TextView) v.findViewById(R.id.text_notification_title);

		v.setTag(holder);

		return v;
	}

	// class to hold view objects of each notification entry, prevents the need
	// to find the view each time you bind data
	static class ViewHolder {
		public TextView titleView;
		public TextView timeView;
		public TextView noteTextView;
		public ImageView smallIconView;
		public ImageView appIconView;
	}

}
