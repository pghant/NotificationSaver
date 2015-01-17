package com.pghant.notifsaver;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends ActionBarActivity {

	public static final String TAG = "MainActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		
		// test clear SharedPreferences pref_filterNames
		//SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		//prefs.edit().remove(SettingsFragment.PREF_FILTERNAMES).commit();

        Toolbar toolbar = (Toolbar) findViewById(R.id.action_bar_toolbar_main);
        setSupportActionBar(toolbar);

		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		Fragment f = fm.findFragmentByTag(NotifList.TAG);
		if (f == null) {
			ft.add(R.id.main_fragment_container, new NotifList(), NotifList.TAG);
		}
		ft.commit();
	}

	@Override
	public void onResume() {
		
		super.onResume();

		// check to see if app has been added to notification listeners
		ContentResolver cr = getContentResolver();
		String enabledNotificationListeners = Settings.Secure.getString(cr,
				"enabled_notification_listeners");
		String packageName = getPackageName();
		if (enabledNotificationListeners == null
				|| !enabledNotificationListeners.contains(packageName)) {
			// popup dialog to direct user to notification listener settings
			new InactiveNotificationListenerDialogFragment().show(getFragmentManager(),
					InactiveNotificationListenerDialogFragment.TAG);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			openSettings();
			return true;
		case android.R.id.home:
			getFragmentManager().popBackStack();
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	

	private void openSettings() {
		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
		ft.replace(R.id.main_fragment_container, new SettingsFragment(), SettingsFragment.TAG);
		ft.addToBackStack(null);
		ft.commit();
	}

	public static class InactiveNotificationListenerDialogFragment extends DialogFragment {

		public static final String TAG = "InactiveNotificationListenerDialogFragment";

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

			builder.setTitle(R.string.listener_not_enabled_title)
					.setMessage(R.string.listener_not_enabled_warning)
					.setPositiveButton(R.string.open_settings,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									Intent i = new Intent(
											"android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
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
	}

}
