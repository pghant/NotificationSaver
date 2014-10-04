package com.pghant.notifsaver;

import java.util.HashSet;
import java.util.Set;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ListView;

import com.pghant.notifsaver.NotifSaverContract.Filters;

public class FiltersActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> {

	public static final String TAG = "FiltersActivity";

	private FilterCursorAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_filters);

		getActionBar().setDisplayHomeAsUpEnabled(true);

		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		getListView().setMultiChoiceModeListener(new MultiChoiceModeListener() {

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				switch (item.getItemId()) {
				case R.id.action_delete:
					long[] ids = getListView().getCheckedItemIds();
					new DeleteFiltersTask().execute(ids);
					mode.finish();
					return true;
				default:
					return false;
				}
			}

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				MenuInflater inflater = mode.getMenuInflater();
				inflater.inflate(R.menu.context_menu_filters, menu);
				return true;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
					boolean checked) {
				mode.setTitle("" + getListView().getCheckedItemCount() + " selected");
			}

		});

		mAdapter = new FilterCursorAdapter(this, null, 0);
		setListAdapter(mAdapter);
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_filters, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// finish this activity and go back to previous screen when up
			// button is pressed
			finish();
			return true;
		case R.id.action_newFilter:
			new FilterDialogFragment().show(getFragmentManager(), FilterDialogFragment.TAG);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Cursor c = (Cursor) l.getItemAtPosition(position);
		Bundle args = new Bundle();
		
		args.putString(Filters.COLUMN_NAME_FILTERNAME, c.getString(c.getColumnIndex(Filters.COLUMN_NAME_FILTERNAME)));
		args.putString(Filters.COLUMN_NAME_TITLE, c.getString(c.getColumnIndex(Filters.COLUMN_NAME_TITLE)));
		args.putString(Filters.COLUMN_NAME_TEXT, c.getString(c.getColumnIndex(Filters.COLUMN_NAME_TEXT)));
		args.putString(Filters.COLUMN_NAME_APPNAME, c.getString(c.getColumnIndex(Filters.COLUMN_NAME_APPNAME)));
		args.putString(Filters.COLUMN_NAME_PACKAGENAME, c.getString(c.getColumnIndex(Filters.COLUMN_NAME_PACKAGENAME)));
		args.putInt(Filters.COLUMN_NAME_TITLE_EXACT, c.getInt(c.getColumnIndex(Filters.COLUMN_NAME_TITLE_EXACT)));
		args.putInt(Filters.COLUMN_NAME_TEXT_EXACT, c.getInt(c.getColumnIndex(Filters.COLUMN_NAME_TEXT_EXACT)));
		args.putInt(Filters.COLUMN_NAME_APPNAME_EXACT, c.getInt(c.getColumnIndex(Filters.COLUMN_NAME_APPNAME_EXACT)));
		args.putInt(Filters.COLUMN_NAME_PACKAGENAME_EXACT, c.getInt(c.getColumnIndex(Filters.COLUMN_NAME_PACKAGENAME_EXACT)));
		args.putInt(Filters._ID, c.getInt(c.getColumnIndex(Filters._ID)));
		args.putBoolean(FilterDialogFragment.IS_EDITING_TAG, true);
		
		FilterDialogFragment df = new FilterDialogFragment();
		df.setArguments(args);
		df.show(getFragmentManager(), FilterDialogFragment.TAG);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(this, Filters.CONTENT_URI, null, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	// AsyncTask to delete filters with specified ids from the database
	private class DeleteFiltersTask extends AsyncTask<long[], Void, Void> {

		@Override
		protected Void doInBackground(long[]... idsAr) {
			ContentResolver resolver = getContentResolver();
			String whereIn = "";
			for (long id : idsAr[0]) {
				whereIn += id + ",";
			}
			whereIn = whereIn.substring(0, whereIn.length() - 1);
			String where = Filters._ID + " IN (" + whereIn + ")";
			
			// remove filter names from SharedPreferences
			String[] projection = {Filters.COLUMN_NAME_FILTERNAME};
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(FiltersActivity.this);
			SharedPreferences.Editor editor = prefs.edit();
			Set<String> filterNames = prefs.getStringSet(SettingsFragment.PREF_FILTERNAMES, null);
			HashSet<String> newFilterNames = new HashSet<String>(filterNames);
			Cursor c = resolver.query(Filters.CONTENT_URI, projection, where, null, null);
			while (c != null && c.moveToNext()) {
				newFilterNames.remove(c.getString(c.getColumnIndex(Filters.COLUMN_NAME_FILTERNAME)));
			}
			c.close();
			editor.putStringSet(SettingsFragment.PREF_FILTERNAMES, newFilterNames);
			editor.commit();
			
			resolver.delete(Filters.CONTENT_URI, where, null);

			return null;
		}
	}

}
