package com.pghant.notifsaver;

import com.pghant.notifsaver.NotifSaverContract.Filters;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

public class FilterCursorAdapter extends CursorAdapter {
	
	public FilterCursorAdapter(Context context, Cursor c, int flags) {
		super(context, c, flags);
	}
	
	@Override
	public void bindView(View view, Context context, Cursor c) {
		ViewHolder holder = (ViewHolder) view.getTag();
		
		String filterName = c.getString(c.getColumnIndex(Filters.COLUMN_NAME_FILTERNAME));
		holder.filterNameView.setText(filterName);
		
		// check each filter field to see if it exists and determines if it's an exact match or just a part match
		
		String title = c.getString(c.getColumnIndex(Filters.COLUMN_NAME_TITLE));
		if (!TextUtils.isEmpty(title)) {
			boolean titleExact = c.getInt(c.getColumnIndex(Filters.COLUMN_NAME_TITLE_EXACT)) == 1;
			String matchesOrContains = titleExact ? context.getResources().getString(R.string.list_item_filter_matches) : context.getResources().getString(R.string.list_item_filter_contains);
			String titleViewText = context.getResources().getString(R.string.list_item_filter_title_default) + " " + matchesOrContains + " \"" + title + "\"";
			holder.titleView.setText(titleViewText);
			holder.titleView.setVisibility(View.VISIBLE);
		} else {
			holder.titleView.setVisibility(View.GONE);
		}
		
		String noteText = c.getString(c.getColumnIndex(Filters.COLUMN_NAME_TEXT));
		if (!TextUtils.isEmpty(noteText)) {
			boolean noteTextExact = c.getInt(c.getColumnIndex(Filters.COLUMN_NAME_TEXT_EXACT)) == 1;
			String matchesOrContains = noteTextExact ? context.getResources().getString(R.string.list_item_filter_matches) : context.getResources().getString(R.string.list_item_filter_contains);
			String noteTextViewText = context.getResources().getString(R.string.list_item_filter_text_default) + " " + matchesOrContains + " \"" + noteText + "\"";
			holder.noteTextView.setText(noteTextViewText);
			holder.noteTextView.setVisibility(View.VISIBLE);
		} else {
			holder.noteTextView.setVisibility(View.GONE);
		}
		
		String appName = c.getString(c.getColumnIndex(Filters.COLUMN_NAME_APPNAME));
		if (!TextUtils.isEmpty(appName)) {
			boolean appNameExact = c.getInt(c.getColumnIndex(Filters.COLUMN_NAME_APPNAME_EXACT)) == 1;
			String matchesOrContains = appNameExact ? context.getResources().getString(R.string.list_item_filter_matches) : context.getResources().getString(R.string.list_item_filter_contains);
			String appNameViewText = context.getResources().getString(R.string.list_item_filter_appname_default) + " " + matchesOrContains + " \"" + appName + "\"";
			holder.appNameView.setText(appNameViewText);
			holder.appNameView.setVisibility(View.VISIBLE);
		} else {
			holder.appNameView.setVisibility(View.GONE);
		}
		
		String packageName = c.getString(c.getColumnIndex(Filters.COLUMN_NAME_PACKAGENAME));
		if (!TextUtils.isEmpty(packageName)) {
			boolean packageNameExact = c.getInt(c.getColumnIndex(Filters.COLUMN_NAME_PACKAGENAME_EXACT)) == 1;
			String matchesOrContains = packageNameExact ? context.getResources().getString(R.string.list_item_filter_matches) : context.getResources().getString(R.string.list_item_filter_contains);
			String packageNameViewText = context.getResources().getString(R.string.list_item_filter_packagename_default) + " " + matchesOrContains + " \"" + packageName + "\"";
			holder.packageNameView.setText(packageNameViewText);
			holder.packageNameView.setVisibility(View.VISIBLE);
		} else {
			holder.packageNameView.setVisibility(View.GONE);
		}
	}
	
	@Override
	public View newView(Context context, Cursor c, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.list_item_filter, parent, false);
		
		ViewHolder holder = new ViewHolder();
		holder.filterNameView = (TextView) v.findViewById(R.id.list_item_filter_filtername);
		holder.titleView = (TextView) v.findViewById(R.id.list_item_filter_title);
		holder.noteTextView = (TextView) v.findViewById(R.id.list_item_filter_text);
		holder.appNameView = (TextView) v.findViewById(R.id.list_item_filter_appname);
		holder.packageNameView = (TextView) v.findViewById(R.id.list_item_filter_packagename);
		
		v.setTag(holder);
		
		return v;
	}
	
	static class ViewHolder {
		public TextView filterNameView;
		public TextView titleView;
		public TextView noteTextView;
		public TextView appNameView;
		public TextView packageNameView;
	}

}
