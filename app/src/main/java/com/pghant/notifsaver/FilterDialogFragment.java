package com.pghant.notifsaver;

import java.util.HashSet;
import java.util.Set;

import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.pghant.notifsaver.NotifSaverContract.Filters;

public class FilterDialogFragment extends DialogFragment {
	public static final String TAG = "AddNewFilterDialogFragment";
	public static final String IS_EDITING_TAG = "isEditing";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.fragment_dialog_newfilter, container, false);
		final boolean isEditing = getArguments() != null
				&& getArguments().getBoolean(IS_EDITING_TAG);

		if (isEditing)
			getDialog().setTitle(R.string.dialog_newfilter_dialogtitle_edit);
		else
			getDialog().setTitle(R.string.dialog_newfilter_dialogtitle);

		// initialize values if arguments passed in
		if (getArguments() != null) {
			initializeDialog(v);
		}

		// show keyboard when dialog pops up
		getDialog().getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

		// set on click listener for add filter button, checks to make sure
		// there is a name
		((Button) v.findViewById(R.id.dialog_newfilter_button_addfilter))
				.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View view) {
						EditText nameTxt = (EditText) v
								.findViewById(R.id.dialog_newfilter_filtername);
						if (TextUtils.isEmpty(nameTxt.getText().toString().trim())) {
							Toast.makeText(
									getActivity(),
									v.getContext().getResources()
											.getString(R.string.dialog_newfilter_noname_message),
									Toast.LENGTH_SHORT).show();
						} else if (!isEditing
								&& filterNameExists(nameTxt.getText().toString().trim())) {
							Toast.makeText(
									getActivity(),
									v.getContext()
											.getResources()
											.getString(R.string.dialog_newfilter_nameexists_message),
									Toast.LENGTH_SHORT).show();
						} else {
							addNewFilter();
							getDialog().cancel();
						}
					}
				});

		// set on click listener for cancel button
		((Button) v.findViewById(R.id.dialog_newfilter_button_cancel))
				.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View view) {
						getDialog().cancel();
					}
				});

		return v;
	}

	// checks if the passed in filter name is contained in the SharedPreferences
	private boolean filterNameExists(String filterName) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		Set<String> filterNames = prefs.getStringSet(SettingsFragment.PREF_FILTERNAMES, null);
		if (filterNames == null)
			return false;
		return filterNames.contains(filterName);
	}

	private void initializeDialog(View v) {
		Bundle args = getArguments();
		EditText nameTxt = (EditText) v.findViewById(R.id.dialog_newfilter_filtername);
		EditText titleTxt = (EditText) v.findViewById(R.id.dialog_newfilter_title);
		EditText textTxt = (EditText) v.findViewById(R.id.dialog_newfilter_text);
		EditText appNameTxt = (EditText) v.findViewById(R.id.dialog_newfilter_appname);
		EditText packageNameTxt = (EditText) v.findViewById(R.id.dialog_newfilter_packagename);
		CheckBox titleChk = (CheckBox) v.findViewById(R.id.dialog_newfilter_title_exactcheck);
		CheckBox textChk = (CheckBox) v.findViewById(R.id.dialog_newfilter_text_exactcheck);
		CheckBox appNameChk = (CheckBox) v.findViewById(R.id.dialog_newfilter_appname_exactcheck);
		CheckBox packageNameChk = (CheckBox) v
				.findViewById(R.id.dialog_newfilter_packagename_exactcheck);

		if (!TextUtils.isEmpty(args.getString(Filters.COLUMN_NAME_FILTERNAME)))
			nameTxt.setText(args.getString(Filters.COLUMN_NAME_FILTERNAME));
		if (!TextUtils.isEmpty(args.getString(Filters.COLUMN_NAME_APPNAME)))
			appNameTxt.setText(args.getString(Filters.COLUMN_NAME_APPNAME));
		if (!TextUtils.isEmpty(args.getString(Filters.COLUMN_NAME_TEXT)))
			textTxt.setText(args.getString(Filters.COLUMN_NAME_TEXT));
		if (!TextUtils.isEmpty(args.getString(Filters.COLUMN_NAME_PACKAGENAME)))
			packageNameTxt.setText(args.getString(Filters.COLUMN_NAME_PACKAGENAME));
		if (!TextUtils.isEmpty(args.getString(Filters.COLUMN_NAME_TITLE)))
			titleTxt.setText(args.getString(Filters.COLUMN_NAME_TITLE));

		if (args.getInt(Filters.COLUMN_NAME_APPNAME_EXACT) == 1)
			appNameChk.setChecked(true);
		if (args.getInt(Filters.COLUMN_NAME_PACKAGENAME_EXACT) == 1)
			packageNameChk.setChecked(true);
		if (args.getInt(Filters.COLUMN_NAME_TEXT_EXACT) == 1)
			textChk.setChecked(true);
		if (args.getInt(Filters.COLUMN_NAME_TITLE_EXACT) == 1)
			titleChk.setChecked(true);
	}

	private void addNewFilter() {
		View v = getView();
		EditText nameTxt = (EditText) v.findViewById(R.id.dialog_newfilter_filtername);
		EditText titleTxt = (EditText) v.findViewById(R.id.dialog_newfilter_title);
		EditText textTxt = (EditText) v.findViewById(R.id.dialog_newfilter_text);
		EditText appNameTxt = (EditText) v.findViewById(R.id.dialog_newfilter_appname);
		EditText packageNameTxt = (EditText) v.findViewById(R.id.dialog_newfilter_packagename);
		CheckBox titleChk = (CheckBox) v.findViewById(R.id.dialog_newfilter_title_exactcheck);
		CheckBox textChk = (CheckBox) v.findViewById(R.id.dialog_newfilter_text_exactcheck);
		CheckBox appNameChk = (CheckBox) v.findViewById(R.id.dialog_newfilter_appname_exactcheck);
		CheckBox packageNameChk = (CheckBox) v
				.findViewById(R.id.dialog_newfilter_packagename_exactcheck);

		String filterName = nameTxt.getText().toString().trim();
		String title = titleTxt.getText().toString();
		String text = textTxt.getText().toString();
		String appName = appNameTxt.getText().toString();
		String packageName = packageNameTxt.getText().toString();
		boolean exactTitle = titleChk.isChecked();
		boolean exactText = textChk.isChecked();
		boolean exactAppName = appNameChk.isChecked();
		boolean exactPackageName = packageNameChk.isChecked();

		ContentValues vals = new ContentValues();
		vals.put(Filters.COLUMN_NAME_FILTERNAME, filterName);
		vals.put(Filters.COLUMN_NAME_TITLE, title == null ? "" : title);
		vals.put(Filters.COLUMN_NAME_TITLE_EXACT, exactTitle ? 1 : 0);
		vals.put(Filters.COLUMN_NAME_TEXT, text == null ? "" : text);
		vals.put(Filters.COLUMN_NAME_TEXT_EXACT, exactText ? 1 : 0);
		vals.put(Filters.COLUMN_NAME_APPNAME, appName == null ? "" : appName);
		vals.put(Filters.COLUMN_NAME_APPNAME_EXACT, exactAppName ? 1 : 0);
		vals.put(Filters.COLUMN_NAME_PACKAGENAME, packageName == null ? "" : packageName);
		vals.put(Filters.COLUMN_NAME_PACKAGENAME_EXACT, exactPackageName ? 1 : 0);

		new NewFilterTask().execute(vals);
	}

	private class NewFilterTask extends AsyncTask<ContentValues, Void, Void> {

		@Override
		protected Void doInBackground(ContentValues... insertValues) {
			ContentResolver resolver = getActivity().getContentResolver();

			// insert filter name into SharedPreferences
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
			Set<String> filterNames = prefs.getStringSet(SettingsFragment.PREF_FILTERNAMES, null);
			HashSet<String> newFilterNames = null;
			if (filterNames != null)
				newFilterNames = new HashSet<String>(filterNames);
			else
				newFilterNames = new HashSet<String>();
			newFilterNames.add(insertValues[0].getAsString(Filters.COLUMN_NAME_FILTERNAME));
			SharedPreferences.Editor editor = prefs.edit();
			editor.putStringSet(SettingsFragment.PREF_FILTERNAMES, newFilterNames);
			editor.commit();

			// update existing filter
			if (getArguments() != null && getArguments().getBoolean(IS_EDITING_TAG)) {
				String where = Filters._ID + " = " + getArguments().getInt(Filters._ID);
				resolver.update(Filters.CONTENT_URI, insertValues[0], where, null);
			} else { // insert new filter
				resolver.insert(Filters.CONTENT_URI, insertValues[0]);
			}

			return null;
		}
	}

}
