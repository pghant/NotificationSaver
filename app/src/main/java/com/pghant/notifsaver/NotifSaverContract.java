package com.pghant.notifsaver;

import android.net.Uri;
import android.provider.BaseColumns;

public final class NotifSaverContract {

	public static final String AUTHORITY = "com.pghant.notifsaver.provider";

	private NotifSaverContract() {
	}

	public static class Notifications implements BaseColumns {

		public static final String TABLE_NAME = "notifs";

		private static final String SCHEME = "content://";
		private static final String PATH_NOTIFS = "/notifsaver/notifs";
		private static final String PATH_NOTIFS_ID = "/notifsaver/notifs/";

		// 0-relative position of a notification ID segment in the path part of
		// a notification ID URI
		// i.e. path is /notifs/#, the # is the 1st element of the path segments
		// "notifs" is the 0th element of the path segments
		public static final int NOTIF_ID_PATH_POSITION = 2;

		public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY + PATH_NOTIFS);
		public static final Uri CONTENT_ID_URI_BASE = Uri
				.parse(SCHEME + AUTHORITY + PATH_NOTIFS_ID);

		// Define MIME types
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.pghant.notifsaver.provider.notifs";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.pghant.notifsaver.provider.notifs";

		// column names
		public static final String COLUMN_NAME_TITLE = "title";
		public static final String COLUMN_NAME_TEXT = "text";
		public static final String COLUMN_NAME_ICON_SMALL = "icon_small";
		public static final String COLUMN_NAME_TIME = "time";
		public static final String COLUMN_NAME_PACKAGE = "package";
		public static final String COLUMN_NAME_NOTIFID = "notif_id";
		public static final String COLUMN_NAME_REMOVED = "removed";
		public static final String COLUMN_NAME_FLAGS = "flags";
		public static final String COLUMN_NAME_DISMISSED = "dismissed";
		public static final String COLUMN_NAME_APPNAME = "appname";
		public static final String COLUMN_NAME_ROWSTATUS = "row_status";

		// default sort order by time, most recent first
		public static final String DEFAULT_SORT_ORDER = COLUMN_NAME_TIME + " DESC";
	}

	public static class Filters implements BaseColumns {

		public static final String TABLE_NAME = "filters";

		private static final String SCHEME = "content://";
		private static final String PATH_FILTERS = "/notifsaver/filters";
		private static final String PATH_FILTERS_ID = "/notifsaver/filters/";

		// 0-relative position of a filter ID segment in the path part of
		// a filter ID URI
		public static final int FILTER_ID_PATH_POSITION = 2;

		public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY + PATH_FILTERS);
		public static final Uri CONTENT_ID_URI_BASE = Uri
				.parse(SCHEME + AUTHORITY + PATH_FILTERS_ID);

		// Define MIME types
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.pghant.notifsaver.provider.filters";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.pghant.notifsaver.provider.filters";
		
		// Columns names
		public static final String COLUMN_NAME_FILTERNAME = "filter_name";
		public static final String COLUMN_NAME_TITLE = "title";
		public static final String COLUMN_NAME_TEXT = "text";
		public static final String COLUMN_NAME_APPNAME = "appname";
		public static final String COLUMN_NAME_PACKAGENAME = "packagename";
		public static final String COLUMN_NAME_TITLE_EXACT = "title_exact";
		public static final String COLUMN_NAME_TEXT_EXACT = "text_exact";
		public static final String COLUMN_NAME_APPNAME_EXACT = "appname_exact";
		public static final String COLUMN_NAME_PACKAGENAME_EXACT = "packagename_exact";

	}

}
