package com.pghant.notifsaver;

import android.app.ActionBar;
import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class SettingsFragment extends PreferenceFragment {
	
	public static final String TAG = "SettingsFragment";
	public static final String PREF_FILTERNAMES = "pref_filterNames";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
        ((ActionBarActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ((ActionBarActivity) getActivity()).getSupportActionBar().setTitle(R.string.action_settings);
        ((ActionBarActivity) getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(true);
//        ((ActionBarActivity) getActivity()).getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		
		// load preferences from xml file
		addPreferencesFromResource(R.xml.preferences);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// hide the settings action button
		MenuItem item = menu.findItem(R.id.action_settings);
		item.setVisible(false);
		
	}
	
	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference pref) {
		if (pref.getKey().equals("pref_exportCSV")) {
			exportCSV();
			return true;
		}
		
		return super.onPreferenceTreeClick(screen, pref);
	}
	
	private void exportCSV() {
		HistoryExporter exporter = new HistoryExporter(getActivity().getApplicationContext());
		exporter.exportNotificationHistory();
	}
	

}
