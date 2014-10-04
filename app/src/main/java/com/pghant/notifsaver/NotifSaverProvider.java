package com.pghant.notifsaver;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.pghant.notifsaver.NotifSaverContract.Filters;
import com.pghant.notifsaver.NotifSaverContract.Notifications;

public class NotifSaverProvider extends ContentProvider {

	private static final String TAG = "NotifSaverProvider";

	private static final int DATABASE_VERSION = 10;
	private static final String DATABASE_NAME = "Notifs.db";

	// projection maps to be used for notifs and filters tables
	private static HashMap<String, String> sNotifsProjectionMap;
	private static HashMap<String, String> sFiltersProjectionMap;

	// fields for URI matching
	private static final UriMatcher sUriMatcher;
	private static final int URI_CODE_MATCH_NOTIFS = 1;
	private static final int URI_CODE_MATCH_NOTIF_ID = 2;
	private static final int URI_CODE_MATCH_FILTERS = 3;
	private static final int URI_CODE_MATCH_FILTER_ID = 4;

	private NotifsDB mNotifsDB;

	static {
		// add URI patterns for URI matcher to match to
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher
				.addURI(NotifSaverContract.AUTHORITY, "notifsaver/notifs", URI_CODE_MATCH_NOTIFS);
		sUriMatcher.addURI(NotifSaverContract.AUTHORITY, "notifsaver/notifs/#",
				URI_CODE_MATCH_NOTIF_ID);
		sUriMatcher.addURI(NotifSaverContract.AUTHORITY, "notifsaver/filters",
				URI_CODE_MATCH_FILTERS);
		sUriMatcher.addURI(NotifSaverContract.AUTHORITY, "notifsaver/filters/#",
				URI_CODE_MATCH_FILTER_ID);

		// maps column names to given strings, use the same strings. This is
		// just an abstraction layer to be used by the query builder. Maps what
		// the content resolver requests to the actual column name in the
		// database table
		sNotifsProjectionMap = new HashMap<String, String>();
		sNotifsProjectionMap.put(Notifications._ID, Notifications._ID);
		sNotifsProjectionMap.put(Notifications.COLUMN_NAME_ICON_SMALL,
				Notifications.COLUMN_NAME_ICON_SMALL);
		sNotifsProjectionMap.put(Notifications.COLUMN_NAME_PACKAGE,
				Notifications.COLUMN_NAME_PACKAGE);
		sNotifsProjectionMap.put(Notifications.COLUMN_NAME_TEXT, Notifications.COLUMN_NAME_TEXT);
		sNotifsProjectionMap.put(Notifications.COLUMN_NAME_TIME, Notifications.COLUMN_NAME_TIME);
		sNotifsProjectionMap.put(Notifications.COLUMN_NAME_TITLE, Notifications.COLUMN_NAME_TITLE);
		sNotifsProjectionMap.put(Notifications.COLUMN_NAME_NOTIFID,
				Notifications.COLUMN_NAME_NOTIFID);
		sNotifsProjectionMap.put(Notifications.COLUMN_NAME_FLAGS, Notifications.COLUMN_NAME_FLAGS);
		sNotifsProjectionMap.put(Notifications.COLUMN_NAME_REMOVED,
				Notifications.COLUMN_NAME_REMOVED);
		sNotifsProjectionMap.put(Notifications.COLUMN_NAME_APPNAME,
				Notifications.COLUMN_NAME_APPNAME);
		sNotifsProjectionMap.put(Notifications.COLUMN_NAME_DISMISSED,
				Notifications.COLUMN_NAME_DISMISSED);
		sNotifsProjectionMap.put(Notifications.COLUMN_NAME_ROWSTATUS,
				Notifications.COLUMN_NAME_ROWSTATUS);

		sFiltersProjectionMap = new HashMap<String, String>();
		sFiltersProjectionMap.put(Filters._ID, Filters._ID);
		sFiltersProjectionMap.put(Filters.COLUMN_NAME_APPNAME, Filters.COLUMN_NAME_APPNAME);
		sFiltersProjectionMap.put(Filters.COLUMN_NAME_APPNAME_EXACT,
				Filters.COLUMN_NAME_APPNAME_EXACT);
		sFiltersProjectionMap.put(Filters.COLUMN_NAME_FILTERNAME, Filters.COLUMN_NAME_FILTERNAME);
		sFiltersProjectionMap.put(Filters.COLUMN_NAME_PACKAGENAME, Filters.COLUMN_NAME_PACKAGENAME);
		sFiltersProjectionMap.put(Filters.COLUMN_NAME_PACKAGENAME_EXACT,
				Filters.COLUMN_NAME_PACKAGENAME_EXACT);
		sFiltersProjectionMap.put(Filters.COLUMN_NAME_TEXT, Filters.COLUMN_NAME_TEXT);
		sFiltersProjectionMap.put(Filters.COLUMN_NAME_TEXT_EXACT, Filters.COLUMN_NAME_TEXT_EXACT);
		sFiltersProjectionMap.put(Filters.COLUMN_NAME_TITLE, Filters.COLUMN_NAME_TITLE);
		sFiltersProjectionMap.put(Filters.COLUMN_NAME_TITLE_EXACT, Filters.COLUMN_NAME_TITLE_EXACT);

	}

	static class NotifsDB extends SQLiteOpenHelper {

		public NotifsDB(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			createNotifTable(db);
			createFiltersTable(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			/*
			 * String sql = "DROP TABLE IF EXISTS " + Notifications.TABLE_NAME;
			 * db.execSQL(sql);
			 */
			if (newVersion > 9 && oldVersion <= 9)
				createFiltersTable(db);
		}

		private void createNotifTable(SQLiteDatabase db) {
			String createString = "CREATE TABLE " + Notifications.TABLE_NAME + " ("
					+ Notifications._ID + " INTEGER PRIMARY KEY, "
					+ Notifications.COLUMN_NAME_APPNAME + " TEXT, "
					+ Notifications.COLUMN_NAME_DISMISSED + " INTEGER, "
					+ Notifications.COLUMN_NAME_ROWSTATUS + " INTEGER, "
					+ Notifications.COLUMN_NAME_NOTIFID + " INTEGER, "
					+ Notifications.COLUMN_NAME_FLAGS + " INTEGER, "
					+ Notifications.COLUMN_NAME_REMOVED + " INTEGER, "
					+ Notifications.COLUMN_NAME_ICON_SMALL + " INTEGER, "
					+ Notifications.COLUMN_NAME_PACKAGE + " TEXT, "
					+ Notifications.COLUMN_NAME_TEXT + " TEXT, " + Notifications.COLUMN_NAME_TIME
					+ " INTEGER, " + Notifications.COLUMN_NAME_TITLE + " TEXT);";
			db.execSQL(createString);
		}

		private void createFiltersTable(SQLiteDatabase db) {
			String createString = "CREATE TABLE " + Filters.TABLE_NAME + " (" + Filters._ID
					+ " INTEGER PRIMARY KEY, " + Filters.COLUMN_NAME_APPNAME + " TEXT, "
					+ Filters.COLUMN_NAME_APPNAME_EXACT + " INTEGER, "
					+ Filters.COLUMN_NAME_PACKAGENAME_EXACT + " INTEGER, "
					+ Filters.COLUMN_NAME_TEXT_EXACT + " INTEGER, "
					+ Filters.COLUMN_NAME_TITLE_EXACT + " INTEGER, "
					+ Filters.COLUMN_NAME_FILTERNAME + " TEXT, " + Filters.COLUMN_NAME_PACKAGENAME
					+ " TEXT, " + Filters.COLUMN_NAME_TEXT + " TEXT, " + Filters.COLUMN_NAME_TITLE
					+ " TEXT);";
			db.execSQL(createString);
		}

	}

	@Override
	public boolean onCreate() {
		mNotifsDB = new NotifsDB(getContext());

		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		// determine if the URI being queried for is for the Notifications table
		// as a whole, or just a single row
		switch (sUriMatcher.match(uri)) {
		case URI_CODE_MATCH_NOTIFS:
			qb.setTables(Notifications.TABLE_NAME);
			qb.setProjectionMap(sNotifsProjectionMap);
			// set default sort order if none is defined in requested query
			if (sortOrder == null || sortOrder.isEmpty())
				sortOrder = Notifications.DEFAULT_SORT_ORDER;
			break;
		case URI_CODE_MATCH_NOTIF_ID:
			qb.setTables(Notifications.TABLE_NAME);
			qb.setProjectionMap(sNotifsProjectionMap);
			qb.appendWhere(Notifications._ID + " = "
					+ uri.getPathSegments().get(Notifications.NOTIF_ID_PATH_POSITION));
			// set default sort order if none is defined in requested query
			if (sortOrder == null || sortOrder.isEmpty())
				sortOrder = Notifications.DEFAULT_SORT_ORDER;
			break;
		case URI_CODE_MATCH_FILTERS:
			qb.setTables(Filters.TABLE_NAME);
			qb.setProjectionMap(sFiltersProjectionMap);
			break;
		case URI_CODE_MATCH_FILTER_ID:
			qb.setTables(Filters.TABLE_NAME);
			qb.setProjectionMap(sFiltersProjectionMap);
			qb.appendWhere(Filters._ID + " = "
					+ uri.getPathSegments().get(Filters.FILTER_ID_PATH_POSITION));
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		SQLiteDatabase db = mNotifsDB.getReadableDatabase();

		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

		c.setNotificationUri(getContext().getContentResolver(), uri);

		return c;
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		// check which table to insert to and call appropriate method
		if (sUriMatcher.match(uri) == URI_CODE_MATCH_NOTIFS) {
			return insertNotif(uri, initialValues);
		} else if (sUriMatcher.match(uri) == URI_CODE_MATCH_FILTERS) {
			return insertFilter(uri, initialValues);
		} else {
			throw new IllegalArgumentException("Invalid URI: " + uri);
		}
	}

	private Uri insertFilter(Uri uri, ContentValues initialValues) {
		SQLiteDatabase db = mNotifsDB.getWritableDatabase();

		long rowid = db.insert(Filters.TABLE_NAME, null, initialValues);
		
		// if the insert was successful, return the URI of the new row and
		// notifies any listeners that the data has changed, checks if count
		// exceeds notifications to keep
		if (rowid > 0) {
			Uri newUri = ContentUris.withAppendedId(Filters.CONTENT_URI, rowid);
			getContext().getContentResolver().notifyChange(newUri, null);
			return newUri;
		} else {
			throw new SQLException("Failed to insert row into " + uri);
		}
	}

	private Uri insertNotif(Uri uri, ContentValues initialValues) {

		// create new ContentValues that will be inserted into the new row, copy
		// whatever is passed in if it exists. This prepares to add column
		// values that are not passed in with default values
		ContentValues insertValues;
		if (initialValues != null)
			insertValues = new ContentValues(initialValues);
		else
			insertValues = new ContentValues();

		// if any required columns are missing, then throw an exception. All
		// notifications should have these passed in
		if (!insertValues.containsKey(Notifications.COLUMN_NAME_ICON_SMALL)
				|| !insertValues.containsKey(Notifications.COLUMN_NAME_PACKAGE)
				|| !insertValues.containsKey(Notifications.COLUMN_NAME_TEXT)
				|| !insertValues.containsKey(Notifications.COLUMN_NAME_TIME)
				|| !insertValues.containsKey(Notifications.COLUMN_NAME_TITLE)
				|| !insertValues.containsKey(Notifications.COLUMN_NAME_NOTIFID)
				|| !insertValues.containsKey(Notifications.COLUMN_NAME_FLAGS))
			throw new IllegalArgumentException("Missing column in ContentValues " + insertValues);

		// add default removed flag = 0, row_status = 1, dismissed time = -1 for
		// new notifications
		if (!insertValues.containsKey(Notifications.COLUMN_NAME_REMOVED))
			insertValues.put(Notifications.COLUMN_NAME_REMOVED, 0);
		if (!insertValues.containsKey(Notifications.COLUMN_NAME_ROWSTATUS))
			insertValues.put(Notifications.COLUMN_NAME_ROWSTATUS, 1);
		if (!insertValues.containsKey(Notifications.COLUMN_NAME_DISMISSED))
			insertValues.put(Notifications.COLUMN_NAME_DISMISSED, -1);

		SQLiteDatabase db = mNotifsDB.getWritableDatabase();

		long rowid = db.insert(Notifications.TABLE_NAME, null, insertValues);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
		int notesToKeep = Integer.parseInt(prefs.getString("pref_notesToKeep", "-1"));

		// if the insert was successful, return the URI of the new row and
		// notifies any listeners that the data has changed, checks if count
		// exceeds notifications to keep
		if (rowid > 0) {
			Uri newUri = ContentUris.withAppendedId(Notifications.CONTENT_URI, rowid);
			getContext().getContentResolver().notifyChange(newUri, null);

			if (notesToKeep > 0)
				checkCount(db, notesToKeep);
			return newUri;
		} else {
			throw new SQLException("Failed to insert row into " + uri);
		}
	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		SQLiteDatabase db = mNotifsDB.getWritableDatabase();
		int count = 0;

		// matches URI to see if updating table as a whole or specific row,
		// updates the where clause if the URI specifies individual row
		switch (sUriMatcher.match(uri)) {
		case URI_CODE_MATCH_NOTIFS:
			count = db.delete(Notifications.TABLE_NAME, where, whereArgs);
			break;
		case URI_CODE_MATCH_NOTIF_ID:
			String notifID = uri.getPathSegments().get(Notifications.NOTIF_ID_PATH_POSITION);
			if (where != null)
				where += " AND " + Notifications._ID + " = " + notifID;
			count = db.delete(Notifications.TABLE_NAME, where, whereArgs);
			break;
		case URI_CODE_MATCH_FILTERS:
			count = db.delete(Filters.TABLE_NAME, where, whereArgs);
			break;
		case URI_CODE_MATCH_FILTER_ID:
			String filterID = uri.getPathSegments().get(Filters.FILTER_ID_PATH_POSITION);
			if (where != null)
				where += " AND " + Filters._ID + " = " + filterID;
			count = db.delete(Filters.TABLE_NAME, where, whereArgs);
			break;
		default:
			throw new IllegalArgumentException("Unkown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues initialValues, String where, String[] whereArgs) {
		SQLiteDatabase db = mNotifsDB.getWritableDatabase();
		int count;

		// matches URI to see if updating table as a whole or specific row,
		// updates the where clause if the URI specifies individual row
		switch (sUriMatcher.match(uri)) {
		case URI_CODE_MATCH_NOTIFS:
			count = db.update(Notifications.TABLE_NAME, initialValues, where, whereArgs);
			break;
		case URI_CODE_MATCH_NOTIF_ID:
			String notifID = uri.getPathSegments().get(Notifications.NOTIF_ID_PATH_POSITION);
			if (where != null)
				where += " AND " + Notifications._ID + " = " + notifID;
			count = db.update(Notifications.TABLE_NAME, initialValues, where, whereArgs);
			break;
		case URI_CODE_MATCH_FILTERS:
			count = db.update(Filters.TABLE_NAME, initialValues, where, whereArgs);
			break;
		case URI_CODE_MATCH_FILTER_ID:
			String filterID = uri.getPathSegments().get(Filters.FILTER_ID_PATH_POSITION);
			if (where != null)
				where += " AND " + Filters._ID + " = " + filterID;
			count = db.update(Filters.TABLE_NAME, initialValues, where, whereArgs);
			break;
		default:
			throw new IllegalArgumentException("Unkown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public String getType(Uri uri) {
		String mimeType = "";

		switch (sUriMatcher.match(uri)) {
		case URI_CODE_MATCH_NOTIFS: // mime type for
			mimeType = Notifications.CONTENT_TYPE;
			break;
		case URI_CODE_MATCH_NOTIF_ID:
			mimeType = Notifications.CONTENT_ITEM_TYPE;
			break;
		case URI_CODE_MATCH_FILTERS: // mime type for
			mimeType = Filters.CONTENT_TYPE;
			break;
		case URI_CODE_MATCH_FILTER_ID:
			mimeType = Filters.CONTENT_ITEM_TYPE;
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		return mimeType;
	}

	// checks the count of notifications in the database and deletes
	// notifications if they are greater than the number that should be saved
	private void checkCount(SQLiteDatabase db, int notesToKeep) {
		Cursor c = db.rawQuery("SELECT COUNT(*) AS c FROM " + Notifications.TABLE_NAME, null);
		int cnt = 0;
		if (c.moveToNext()) {
			cnt = c.getInt(c.getColumnIndex("c"));
		}
		c.close();

		// larger count of notifications than need to keep, delete extra
		if (notesToKeep < cnt) {
			c = db.query(Notifications.TABLE_NAME, new String[] { Notifications._ID }, null, null,
					null, null, Notifications.COLUMN_NAME_TIME + " ASC ", "" + (cnt - notesToKeep));
			String ids = "";
			while (c.moveToNext()) {
				ids += c.getInt(c.getColumnIndex(Notifications._ID)) + ",";
			}
			c.close();
			delete(Notifications.CONTENT_URI,
					Notifications._ID + " IN (" + ids.substring(0, ids.length() - 1) + ")", null);
		}
	}

}
