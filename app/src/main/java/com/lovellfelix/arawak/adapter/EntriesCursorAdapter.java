package com.lovellfelix.arawak.adapter;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.lovellfelix.arawak.Constants;
import com.lovellfelix.arawak.MainApplication;
import com.lovellfelix.arawak.R;
import com.lovellfelix.arawak.provider.FeedData;
import com.lovellfelix.arawak.provider.FeedData.EntryColumns;
import com.lovellfelix.arawak.provider.FeedData.FeedColumns;
import com.lovellfelix.arawak.utils.UiUtils;

import java.util.Vector;

public class EntriesCursorAdapter extends ResourceCursorAdapter {

    private static class ViewHolder {
        public TextView titleTextView;
        public TextView dateTextView;
        public ImageView starImgView;
        public CheckBox isReadCb;
    }

    private int mTitlePos, mDatePos, mIsReadPos, mFavoritePos, mIdPos, mFeedIconPos, mFeedNamePos;

    private final Uri mUri;
    private final boolean mShowFeedInfo;

    private final Vector<Long> mMarkedAsReadEntries = new Vector<Long>();
    private final Vector<Long> mMarkedAsUnreadEntries = new Vector<Long>();
    private final Vector<Long> mFavoriteEntries = new Vector<Long>();
    private final Vector<Long> mNotFavoriteEntries = new Vector<Long>();

    public EntriesCursorAdapter(Context context, Uri uri, Cursor cursor, boolean showFeedInfo) {
        super(context, R.layout.item_entry_list, cursor, 0);
        mUri = uri;
        mShowFeedInfo = showFeedInfo;

        reinit(cursor);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        if (view.getTag() == null) {
            ViewHolder holder = new ViewHolder();
            holder.titleTextView = (TextView) view.findViewById(android.R.id.text1);
            holder.dateTextView = (TextView) view.findViewById(android.R.id.text2);
            holder.starImgView = (ImageView) view.findViewById(android.R.id.icon);
            holder.isReadCb = (CheckBox) view.findViewById(android.R.id.checkbox);
            view.setTag(holder);
        }

        final ViewHolder holder = (ViewHolder) view.getTag();

        holder.titleTextView.setText(cursor.getString(mTitlePos));

        final long id = cursor.getLong(mIdPos);
        final boolean favorite = !mNotFavoriteEntries.contains(id) && (cursor.getInt(mFavoritePos) == 1 || mFavoriteEntries.contains(id));

        holder.starImgView.setImageResource(favorite ? R.drawable.dimmed_rating_important : R.drawable.dimmed_rating_not_important);
        holder.starImgView.setTag(favorite ? Constants.TRUE : Constants.FALSE);
        holder.starImgView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean newFavorite = !Constants.TRUE.equals(view.getTag());

                if (newFavorite) {
                    view.setTag(Constants.TRUE);
                    holder.starImgView.setImageResource(R.drawable.dimmed_rating_important);
                    mFavoriteEntries.add(id);
                    mNotFavoriteEntries.remove(id);
                } else {
                    view.setTag(Constants.FALSE);
                    holder.starImgView.setImageResource(R.drawable.dimmed_rating_not_important);
                    mNotFavoriteEntries.add(id);
                    mFavoriteEntries.remove(id);
                }

                ContentValues values = new ContentValues();
                values.put(EntryColumns.IS_FAVORITE, newFavorite ? 1 : 0);

                ContentResolver cr = MainApplication.getContext().getContentResolver();
                Uri entryUri = ContentUris.withAppendedId(mUri, id);
                cr.update(entryUri, values, null, null);
            }
        });

        if (mShowFeedInfo && mFeedIconPos > -1) {
            byte[] iconBytes = cursor.getBlob(mFeedIconPos);
            Bitmap bitmap = UiUtils.getScaledBitmap(iconBytes, 18);
            if (bitmap != null) {
                holder.dateTextView.setCompoundDrawablesWithIntrinsicBounds(new BitmapDrawable(context.getResources(), bitmap), null, null, null);
            } else {
                holder.dateTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
            }
        }

	    holder.dateTextView.setText(UiUtils.easyreadDateTimeString(cursor.getLong(mDatePos)));

        holder.isReadCb.setOnCheckedChangeListener(null);
        if (mMarkedAsUnreadEntries.contains(id) || (cursor.isNull(mIsReadPos) && !mMarkedAsReadEntries.contains(id))) {
            holder.titleTextView.setEnabled(true);
            holder.dateTextView.setEnabled(true);
            holder.isReadCb.setChecked(false);
        } else {
            holder.titleTextView.setEnabled(false);
            holder.dateTextView.setEnabled(false);
            holder.isReadCb.setChecked(true);
        }

        holder.isReadCb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    markAsRead(id);
                    holder.titleTextView.setEnabled(false);
                    holder.dateTextView.setEnabled(false);
                } else {
                    markAsUnread(id);
                    holder.titleTextView.setEnabled(true);
                    holder.dateTextView.setEnabled(true);
                }
            }
        });
    }

    public void markAllAsRead() {
        mMarkedAsReadEntries.clear();
        mMarkedAsUnreadEntries.clear();

        new Thread() {
            @Override
            public void run() {
                ContentResolver cr = MainApplication.getContext().getContentResolver();
                cr.update(mUri, FeedData.getReadContentValues(), EntryColumns.WHERE_UNREAD, null);
            }
        }.start();
    }

    private void markAsRead(final long id) {
        mMarkedAsReadEntries.add(id);
        mMarkedAsUnreadEntries.remove(id);

        new Thread() {
            @Override
            public void run() {
                ContentResolver cr = MainApplication.getContext().getContentResolver();
                Uri entryUri = ContentUris.withAppendedId(mUri, id);
                cr.update(entryUri, FeedData.getReadContentValues(), null, null);
            }
        }.start();
    }

    private void markAsUnread(final long id) {
        mMarkedAsUnreadEntries.add(id);
        mMarkedAsReadEntries.remove(id);

        new Thread() {
            @Override
            public void run() {
                ContentResolver cr = MainApplication.getContext().getContentResolver();
                Uri entryUri = ContentUris.withAppendedId(mUri, id);
                cr.update(entryUri, FeedData.getUnreadContentValues(), null, null);
            }
        }.start();
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
        mMarkedAsReadEntries.clear();
        mMarkedAsUnreadEntries.clear();
        mFavoriteEntries.clear();
        mNotFavoriteEntries.clear();

        if (cursor != null) {
            mTitlePos = cursor.getColumnIndex(EntryColumns.TITLE);
            mDatePos = cursor.getColumnIndex(EntryColumns.DATE);
            mIsReadPos = cursor.getColumnIndex(EntryColumns.IS_READ);
            mFavoritePos = cursor.getColumnIndex(EntryColumns.IS_FAVORITE);
            mIdPos = cursor.getColumnIndex(EntryColumns._ID);
            if (mShowFeedInfo) {
                mFeedIconPos = cursor.getColumnIndex(FeedColumns.ICON);
                mFeedNamePos = cursor.getColumnIndex(FeedColumns.NAME);
            }
        }
    }
}