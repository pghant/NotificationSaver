package com.pghant.notifsaver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import au.com.bytecode.opencsv.CSVWriter;

import com.pghant.notifsaver.NotifSaverContract.Notifications;

public class HistoryExporter {

	private Context mContext;

	public HistoryExporter(Context context) {
		mContext = context;
	}

	// exports notification history to a file in the downloads folder
	public void exportNotificationHistory() {
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			Toast.makeText(mContext,
					mContext.getResources().getString(R.string.inaccessible_storage),
					Toast.LENGTH_SHORT).show();
			return;
		}
		File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
		File file = new File(path, getFileName());
		path.mkdirs();
		new ExportCSVTask().execute(file);
	}

	
	// gets the filename for the history export using datetime created
	private String getFileName() {
		String fileName = "NotificationHistory_";
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmm", Locale.US);
		fileName += df.format(new Date(System.currentTimeMillis()));
		fileName += ".csv";
		return fileName;
	}
	
	// AsyncTask to export all notification history data as CSV file to Downloads
	private class ExportCSVTask extends AsyncTask<File, Void, Boolean> {
		
		private NotificationManager mNotifyManager;
		private int id = 1;
		private Notification.Builder mBuilder;
		private File mFile;
		
		
		@Override
		protected void onPreExecute() {
			mBuilder = new Notification.Builder(mContext);
			mBuilder.setSmallIcon(R.drawable.ic_stat_export_history)
					.setContentTitle(mContext.getString(R.string.export_notification_title))
					.setContentText(mContext.getString(R.string.export_in_progress))
					.setTicker(mContext.getString(R.string.export_notification_ticker))
					.setOngoing(true)
					.setProgress(0, 0, true);
			mNotifyManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
			mNotifyManager.notify(id, mBuilder.build());
		}

		@Override
		protected Boolean doInBackground(File... args) {
			try {
				mFile = args[0];
				CSVWriter writer = new CSVWriter(new FileWriter(mFile), ',', '"');
				writer.writeAll(getLines());
				writer.close();
			} catch (IOException e) {
				Log.e("HistoryExporter", e.getMessage());
				e.printStackTrace();
				return false;
			} 
			
			return true;
		}
		
		@Override
		protected void onPostExecute(Boolean success) {
			if (success.booleanValue()) {
				// intent to open file when notification tapped
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.fromFile(mFile), "text/csv");
				PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
				
				// intent to share the file in notification action button
				Intent shareIntent = new Intent(Intent.ACTION_SEND);
				shareIntent.setType("text/csv");
				shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(mFile));
				PendingIntent actionShareIntent = PendingIntent.getActivity(mContext, 0, shareIntent, 0);
				
				mBuilder.setContentText(mContext.getResources().getString(R.string.export_success))
						.setContentIntent(contentIntent)
						.addAction(R.drawable.ic_action_share, mContext.getText(R.string.action_share), actionShareIntent);
			} else {
				mBuilder.setContentText(mContext.getResources().getString(R.string.export_failed));
			}
			mBuilder.setOngoing(false)
					.setProgress(0, 0, false)
					.setTicker(mContext.getText(R.string.export_notification_ticker_complete))
					.setAutoCancel(true);
			mNotifyManager.notify(id, mBuilder.build());
		}
		
		// get list of all lines to be included in CSV file
		private List<String[]> getLines() {
			List<String[]> lines = new ArrayList<String[]>();

			String[] projection = { Notifications.COLUMN_NAME_APPNAME,
					Notifications.COLUMN_NAME_PACKAGE, Notifications.COLUMN_NAME_TIME,
					Notifications.COLUMN_NAME_FLAGS, Notifications.COLUMN_NAME_TITLE,
					Notifications.COLUMN_NAME_TEXT };
			Cursor c = mContext.getContentResolver().query(Notifications.CONTENT_URI, projection, null, null, null);
			
			lines.add(projection); // add headers
			
			while (c.moveToNext()) {
				String[] line = new String[6];
				line[0] = c.getString(c.getColumnIndex(Notifications.COLUMN_NAME_APPNAME));
				line[1] = c.getString(c.getColumnIndex(Notifications.COLUMN_NAME_PACKAGE));
				line[2] = formatDate(c.getLong(c.getColumnIndex(Notifications.COLUMN_NAME_TIME)));
				line[3] = convertFlagInt(c.getInt(c.getColumnIndex(Notifications.COLUMN_NAME_FLAGS)));
				line[4] = c.getString(c.getColumnIndex(Notifications.COLUMN_NAME_TITLE));
				line[5] = c.getString(c.getColumnIndex(Notifications.COLUMN_NAME_TEXT));
				lines.add(line);
			}
			
			c.close();
			return lines;
		}
		
	}
	
	private String convertFlagInt(int flags) {
		String allFlags = "";
		if ((Notification.FLAG_AUTO_CANCEL & flags) != 0)
			allFlags += "auto cancel, ";
		if ((Notification.FLAG_FOREGROUND_SERVICE & flags) != 0)
			allFlags += "foreground service, ";
		if ((Notification.FLAG_INSISTENT & flags) != 0)
			allFlags += "insistent, ";
		if ((Notification.FLAG_NO_CLEAR & flags) != 0)
			allFlags += "no clear, ";
		if ((Notification.FLAG_ONGOING_EVENT & flags) != 0)
			allFlags += "ongoing, ";
		if ((Notification.FLAG_ONLY_ALERT_ONCE & flags) != 0)
			allFlags += "only alert once, ";
		if ((Notification.FLAG_SHOW_LIGHTS & flags) != 0)
			allFlags += "led, ";
		if (allFlags.length() > 0)
			allFlags = allFlags.substring(0, allFlags.length() - 2);
		return allFlags;
	}
	
	private String formatDate(long time) {
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
		return df.format(new Date(time));
	}

}
