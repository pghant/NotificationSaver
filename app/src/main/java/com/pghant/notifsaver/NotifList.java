package com.pghant.notifsaver;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.app.Notification;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SpinnerAdapter;

import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;

import com.pghant.notifsaver.NotifSaverContract.Filters;
import com.pghant.notifsaver.NotifSaverContract.Notifications;

public class NotifList extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>,
		ActionBar.OnNavigationListener, SearchView.OnQueryTextListener {

	public static final String TAG = "NotifList";
	public static final int DELETE_DIALOG_REQUEST_CODE = 1;

	private int mSpinnerPos = -1;
	private String mFilter;
	private SearchView mSearchView;

	private NotifCursorAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

		SpinnerAdapter mSpinnerAdapter = ArrayAdapter
				.createFromResource(((ActionBarActivity) getActivity()), R.array.pref_defaultViewMode_entries,
						android.R.layout.simple_spinner_dropdown_item);
//		((ActionBarActivity) getActivity()).getSupportActionBar().setListNavigationCallbacks(mSpinnerAdapter, this);

		// if recreating the fragment, save the spinnerPos
		if (savedInstanceState != null)
			mSpinnerPos = savedInstanceState.getInt("savedSpinnerPos");

	}

	@Override
	public void onResume() {

		super.onResume();

        ((ActionBarActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);
		((ActionBarActivity) getActivity()).getSupportActionBar().setHomeButtonEnabled(false);
		((ActionBarActivity) getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(false);
//		((ActionBarActivity) getActivity()).getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
	}

	@Override
	public void onStop() {

		mSpinnerPos = ((ActionBarActivity) getActivity()).getSupportActionBar().getSelectedNavigationIndex();
//		((ActionBarActivity) getActivity()).getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

		super.onStop();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {

		// save current spinner position

		outState.putInt("savedSpinnerPos", mSpinnerPos);
		super.onSaveInstanceState(outState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View v = inflater.inflate(R.layout.fragment_notiflist, container, false);

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {

		super.onActivityCreated(savedInstanceState);

		// set view mode, use default unless there is a saved state
		if (savedInstanceState != null) {
			mSpinnerPos = savedInstanceState.getInt("savedSpinnerPos");
		} else if (mSpinnerPos == -1) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(((ActionBarActivity) getActivity()));
			String defaultViewPref = prefs.getString("pref_defaultViewMode", "day");
			mSpinnerPos = getPositionStringInArray(defaultViewPref,
					getResources().getStringArray(R.array.pref_defaultViewMode_values));
		}

//		((ActionBarActivity) getActivity()).getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
//		((ActionBarActivity) getActivity()).getSupportActionBar().setSelectedNavigationItem(mSpinnerPos);

		ListView list = getListView();
		list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		list.setMultiChoiceModeListener(new MultiChoiceModeListener() {

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				long[] ids = getListView().getCheckedItemIds();
				switch (item.getItemId()) {
				case R.id.action_delete:
					ConfirmDeleteDialogFragment dialog = new ConfirmDeleteDialogFragment();
					Bundle args = new Bundle();
					args.putLongArray("ids", ids);
					dialog.setArguments(args);
					dialog.setTargetFragment(NotifList.this, DELETE_DIALOG_REQUEST_CODE);
					dialog.show(getFragmentManager(), ConfirmDeleteDialogFragment.TAG);
					mode.finish();
					return true;
				case R.id.action_new_filter_context:
					new StartFilterDialogTask().execute(ids[0]);
					mode.finish();
					return true;
				default:
					return false;
				}
			}

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				MenuInflater inflater = mode.getMenuInflater();
				inflater.inflate(R.menu.context_menu, menu);
				return true;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				MenuItem filterItem = menu.findItem(R.id.action_new_filter_context);
				if (getListView().getCheckedItemCount() > 1)
					filterItem.setVisible(false);
				else
					filterItem.setVisible(true);
				return true;
			}

			@Override
			public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
					boolean checked) {
				mode.setTitle("" + getListView().getCheckedItemCount() + " selected");
				mode.invalidate();
			}

		});

		mAdapter = new NotifCursorAdapter(((ActionBarActivity) getActivity()), null, 0);
		setListAdapter(mAdapter);

		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// Inflate the menu; this adds items to the action bar if it is present.
		inflater.inflate(R.menu.fragment_notiflist, menu);

		mSearchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
		mSearchView.setOnQueryTextListener(this);
		mSearchView.setQueryHint(getResources().getText(R.string.search_hint));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// case R.id.action_search:
		// Toast.makeText(((ActionBarActivity) getActivity()), "Search", Toast.LENGTH_SHORT).show();
		// return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Cursor c = (Cursor) l.getItemAtPosition(position);
		NotificationInfoDialogFragment dialog = new NotificationInfoDialogFragment();
		Bundle args = new Bundle();

		args.putInt(Notifications.COLUMN_NAME_FLAGS,
				c.getInt(c.getColumnIndex(Notifications.COLUMN_NAME_FLAGS)));
		args.putString(Notifications.COLUMN_NAME_PACKAGE,
				c.getString(c.getColumnIndex(Notifications.COLUMN_NAME_PACKAGE)));
		args.putString(Notifications.COLUMN_NAME_APPNAME,
				c.getString(c.getColumnIndex(Notifications.COLUMN_NAME_APPNAME)));
		args.putString(Notifications.COLUMN_NAME_TEXT,
				c.getString(c.getColumnIndex(Notifications.COLUMN_NAME_TEXT)));
		args.putString(Notifications.COLUMN_NAME_TITLE,
				c.getString(c.getColumnIndex(Notifications.COLUMN_NAME_TITLE)));
		args.putInt(Notifications.COLUMN_NAME_ICON_SMALL,
				c.getInt(c.getColumnIndex(Notifications.COLUMN_NAME_ICON_SMALL)));
		args.putLong(Notifications.COLUMN_NAME_TIME,
				c.getLong(c.getColumnIndex(Notifications.COLUMN_NAME_TIME)));

		dialog.setArguments(args);
		dialog.show(getFragmentManager(), NotificationInfoDialogFragment.TAG);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == DELETE_DIALOG_REQUEST_CODE) {
			long[] ids = data.getLongArrayExtra("ids");
			new DeleteNotificationsTask().execute(ids);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {

		String filter = "";
		if (!TextUtils.isEmpty(mFilter)) {
			mFilter = mFilter.trim().replace("'", "''");
			filter += Notifications.COLUMN_NAME_TITLE + " LIKE '%" + mFilter + "%' OR "
					+ Notifications.COLUMN_NAME_TEXT + " LIKE '%" + mFilter + "%' OR "
					+ Notifications.COLUMN_NAME_APPNAME + " LIKE '%" + mFilter + "%' OR "
					+ Notifications.COLUMN_NAME_PACKAGE + " LIKE '%" + mFilter + "%' ";
		} else {
			filter = getTimeFilter(mSpinnerPos);
		}

		String[] projectionToRetrieve = { Notifications._ID, Notifications.COLUMN_NAME_PACKAGE,
				Notifications.COLUMN_NAME_ICON_SMALL, Notifications.COLUMN_NAME_TITLE,
				Notifications.COLUMN_NAME_TIME, Notifications.COLUMN_NAME_REMOVED,
				Notifications.COLUMN_NAME_APPNAME, Notifications.COLUMN_NAME_TEXT,
				Notifications.COLUMN_NAME_FLAGS };
		return new CursorLoader(((ActionBarActivity) getActivity()), Notifications.CONTENT_URI, projectionToRetrieve,
				filter, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	@Override
	public boolean onNavigationItemSelected(int position, long itemId) {
		mSpinnerPos = position;
		// restart loader with new view mode
		getLoaderManager().restartLoader(0, null, this);

		return true;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		// hide the keyboard when the user presses the search button
		InputMethodManager imm = (InputMethodManager) ((ActionBarActivity) getActivity()).getSystemService(
				Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(getListView().getWindowToken(), 0);
		return true;
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		// update filter to be used to display the notification list
		mFilter = newText;
		getLoaderManager().restartLoader(0, null, this);
		return true;
	}

	// gets the selection filter for time based on the view mode spinner
	// position
	private String getTimeFilter(int spinnerPos) {
		String filter = Notifications.COLUMN_NAME_TIME + " > ";
		GregorianCalendar now = new GregorianCalendar();
		now.setTimeInMillis(System.currentTimeMillis());
		switch (spinnerPos) {
		case 0:
			// day view
			now.set(Calendar.HOUR_OF_DAY, 0);
			filter += now.getTimeInMillis();
			break;
		case 1:
			// week view
			now.add(Calendar.WEEK_OF_YEAR, -1);
			now.set(Calendar.HOUR_OF_DAY, 0);
			filter += now.getTimeInMillis();
			break;
		case 2:
			// month view
			now.add(Calendar.MONTH, -1);
			now.set(Calendar.HOUR_OF_DAY, 0);
			filter += now.getTimeInMillis();
			break;
		case 3:
			// all view
			filter = null;
			break;
		default:
			throw new IllegalArgumentException("Invalid view mode: " + spinnerPos);
		}

		return filter;
	}

	// returns the index of the String in the array, -1 if not found
	private int getPositionStringInArray(String toFind, String[] array) {
		for (int i = 0; i < array.length; i++) {
			if (array[i].equals(toFind))
				return i;
		}

		return -1;
	}

	// AsyncTask to delete notifications with specified ids from the database
	private class DeleteNotificationsTask extends AsyncTask<long[], Void, Void> {

		@Override
		protected Void doInBackground(long[]... idsAr) {
			ContentResolver resolver = ((ActionBarActivity) getActivity()).getContentResolver();
			String whereIn = "";
			for (long id : idsAr[0]) {
				whereIn += id + ",";
			}
			whereIn = whereIn.substring(0, whereIn.length() - 1);

			String where = Notifications._ID + " IN (" + whereIn + ")";
			resolver.delete(Notifications.CONTENT_URI, where, null);

			return null;
		}
	}

	private class StartFilterDialogTask extends AsyncTask<Long, Void, Void> {

		@Override
		protected Void doInBackground(Long... ids) {
			long id = ids[0];
			ContentResolver resolver = ((ActionBarActivity) getActivity()).getContentResolver();
			Bundle args = new Bundle();
			String[] projection = { Notifications.COLUMN_NAME_APPNAME,
					Notifications.COLUMN_NAME_TEXT, Notifications.COLUMN_NAME_TITLE,
					Notifications.COLUMN_NAME_PACKAGE };
			Cursor c = resolver.query(Notifications.CONTENT_URI, projection, Notifications._ID + " = ?",
					new String[] { "" + id }, null);

			c.moveToFirst();
			args.putString(Filters.COLUMN_NAME_APPNAME,
					c.getString(c.getColumnIndex(Notifications.COLUMN_NAME_APPNAME)));
			args.putString(Filters.COLUMN_NAME_PACKAGENAME,
					c.getString(c.getColumnIndex(Notifications.COLUMN_NAME_PACKAGE)));
			args.putString(Filters.COLUMN_NAME_TEXT,
					c.getString(c.getColumnIndex(Notifications.COLUMN_NAME_TEXT)));
			args.putString(Filters.COLUMN_NAME_TITLE,
					c.getString(c.getColumnIndex(Notifications.COLUMN_NAME_TITLE)));
			args.putInt(Filters.COLUMN_NAME_APPNAME_EXACT, 1);
			args.putInt(Filters.COLUMN_NAME_PACKAGENAME_EXACT, 1);
			args.putInt(Filters.COLUMN_NAME_TEXT_EXACT, 1);
			args.putInt(Filters.COLUMN_NAME_TITLE_EXACT, 1);
			
			c.close();
			
			FilterDialogFragment df = new FilterDialogFragment();
			df.setArguments(args);
			df.show(getFragmentManager(), FilterDialogFragment.TAG);

			return null;
		}
	}

	// dialog to confirm deleting notifications from history
	public static class ConfirmDeleteDialogFragment extends DialogFragment {

		public static final String TAG = "ConfirmDeleteDialogFragment";

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			Bundle args = getArguments();
			final long[] ids = args.getLongArray("ids");

			AlertDialog.Builder builder = new AlertDialog.Builder(((ActionBarActivity) getActivity()));

			builder.setMessage(R.string.confirm_delete_title)
					.setPositiveButton(R.string.action_delete,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									sendResult(ids);
								}
							})
					.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
					});

			return builder.create();
		}

		private void sendResult(long[] ids) {
			getTargetFragment().onActivityResult(getTargetRequestCode(), 0,
					new Intent().putExtra("ids", ids));
		}
	}

	// dialog to show detailed notification info when a notification is clicked
	// in the list
	public static class NotificationInfoDialogFragment extends DialogFragment {

		public static final String TAG = "NotificationInfoDialogFragment";

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			Bundle args = getArguments();
			AlertDialog.Builder builder = new AlertDialog.Builder(((ActionBarActivity) getActivity()));
			String message = getMessage(args);

			final String pkg = args.getString(Notifications.COLUMN_NAME_PACKAGE);

			builder.setMessage(message)
					.setPositiveButton(R.string.app_info_settings,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									Uri uri = Uri.parse("package:" + pkg);
									Intent i = new Intent(
											Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
									i.setData(uri);
									startActivity(i);
								}
							})
					.setNeutralButton(R.string.play_store_link,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									Uri uri = Uri
											.parse("https://play.google.com/store/apps/details?id="
													+ pkg);
									Intent i = new Intent(Intent.ACTION_VIEW);
									i.setData(uri);
									startActivity(i);
								}
							})
					.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
					});

			return builder.create();

		}

		private String getMessage(Bundle args) {
			String message = "";

			String title = args.getString(Notifications.COLUMN_NAME_TITLE);
			String text = args.getString(Notifications.COLUMN_NAME_TEXT);
			String pkg = args.getString(Notifications.COLUMN_NAME_PACKAGE);
			String appName = args.getString(Notifications.COLUMN_NAME_APPNAME);

			message += "TITLE: " + (title.isEmpty() ? "--" : title) + "\n";
			message += "TEXT: " + (text.isEmpty() ? "--" : text) + "\n";
			message += "APP: " + (appName.isEmpty() ? "--" : appName) + "\n";
			message += "PACKAGE: " + (pkg.isEmpty() ? "--" : pkg) + "\n";

			DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
			message += "TIME: " + df.format(new Date(args.getLong(Notifications.COLUMN_NAME_TIME)))
					+ "\n";

			String allFlags = "";
			int flags = args.getInt(Notifications.COLUMN_NAME_FLAGS);
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
			message += "FLAGS: " + allFlags + "\n";

			return message;
		}
	}

}
