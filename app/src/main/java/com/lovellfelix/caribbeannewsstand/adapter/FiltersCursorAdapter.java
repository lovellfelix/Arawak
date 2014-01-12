package com.lovellfelix.caribbeannewsstand.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.lovellfelix.caribbeannewsstand.R;
import com.lovellfelix.caribbeannewsstand.provider.FeedData.FilterColumns;

public class FiltersCursorAdapter extends ResourceCursorAdapter {

    private int filterTextColumnPosition;
    private int isAppliedToTitleColumnPosition;

    private int mSelectedFilter = -1;

    public FiltersCursorAdapter(Context context, Cursor cursor) {
        super(context, android.R.layout.simple_list_item_2, cursor, 0);

        reinit(cursor);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        TextView filterTextTextView = (TextView) view.findViewById(android.R.id.text1);
        TextView isAppliedToTitleTextView = (TextView) view.findViewById(android.R.id.text2);

        if (cursor.getPosition() == mSelectedFilter) {
            view.setBackgroundResource(android.R.color.holo_blue_dark);
        } else {
            view.setBackgroundResource(android.R.color.transparent);
        }

        filterTextTextView.setText(cursor.getString(filterTextColumnPosition));
        isAppliedToTitleTextView.setText(cursor.getInt(isAppliedToTitleColumnPosition) == 1 ? R.string.filter_apply_to_title : R.string.filter_apply_to_content);
    }

    @Override
    public void changeCursor(Cursor cursor) {
        reinit(cursor);
        super.changeCursor(cursor);
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        reinit(newCursor);
        return super.swapCursor(newCursor);
    }

    @Override
    public void notifyDataSetChanged() {
        reinit(null);
        super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        reinit(null);
        super.notifyDataSetInvalidated();
    }

    private void reinit(Cursor cursor) {
        if (cursor != null) {
            filterTextColumnPosition = cursor.getColumnIndex(FilterColumns.FILTER_TEXT);
            isAppliedToTitleColumnPosition = cursor.getColumnIndex(FilterColumns.IS_APPLIED_TO_TITLE);
        }
    }

    public void setSelectedFilter(int filterPos) {
        mSelectedFilter = filterPos;
    }

    public int getSelectedFilter() {
        return mSelectedFilter;
    }
}
