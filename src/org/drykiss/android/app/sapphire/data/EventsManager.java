
package org.drykiss.android.app.sapphire.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.drykiss.android.app.sapphire.data.DataManager.OnDataChangedListener;
import org.drykiss.android.app.sapphire.provider.SapphireProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EventsManager implements DataManager.DataPracticeManager {
    private Context mContext;
    private ArrayList<Event> mEvents = new ArrayList<Event>();
    private HashMap<Long, Integer> mEventsMap = new HashMap<Long, Integer>();
    private OnDataChangedListener mListener;

    public EventsManager(Context context) {
        mContext = context;
    }

    @Override
    public void loadDatas() {
        new EventsLoaderTask().execute();
    }

    public Event get(int position) {
        if (position >= mEvents.size()) {
            return null;
        }
        return mEvents.get(position);
    }

    public ArrayList<Event> getActiveEventOn(long timeStamp) {
        ArrayList<Event> events = new ArrayList<Event>();
        for (Event event : mEvents) {
            if (event.mStartTime < timeStamp && event.mEndTime > timeStamp) {
                events.add(event);
            }
        }
        return events;
    }

    public void update(int position, Event event) {
        Event oldEvent = mEvents.get(position);
        if (oldEvent.mId != event.mId) {
            return;
        }
        final ContentValues values = oldEvent.getDiff(event);
        if (values.size() <= 0) {
            return;
        }
        mEvents.remove(position);
        mEvents.add(position, event);
        final Uri uri = ContentUris.withAppendedId(SapphireProvider.Event.CONTENT_URI, event.mId);
        mContext.getContentResolver().update(uri, values, null, null);
        notifyDataChanged();
    }

    public void add(Event event) {
        final ContentValues values = event.getContentValues();
        if (values.size() <= 0) {
            return;
        }
        Uri uri = mContext.getContentResolver().insert(SapphireProvider.Event.CONTENT_URI,
                values);
        final List<String> segments = uri.getPathSegments();
        event.mId = Long.valueOf(segments.get(segments.size() - 1));
        mEvents.add(0, event);
        notifyDataChanged();
    }

    public void remove(int position) {
        final Event event = mEvents.get(position);
        final Uri uri = ContentUris.withAppendedId(SapphireProvider.Event.CONTENT_URI, event.mId);
        mContext.getContentResolver().delete(uri, null, null);

        mEvents.remove(position);
        notifyDataChanged();
    }

    public int getCount() {
        return mEvents.size();
    }

    @Override
    public void setDataChangedListener(OnDataChangedListener listener) {
        mListener = listener;
    }

    @Override
    public void clearDataChangedListener(OnDataChangedListener listener) {
        mListener = null;
    }

    private void notifyDataChanged() {
        if (mListener != null) {
            mListener.onDataChanged();
        }
    }

    private static final String[] EVENT_PROJECTION = {
            SapphireProvider.Event.KEY_ID, SapphireProvider.Event.KEY_NAME,
            SapphireProvider.Event.KEY_START_TIME, SapphireProvider.Event.KEY_END_TIME,
            SapphireProvider.Event.KEY_NOTICED_TIME, SapphireProvider.Event.KEY_NOTICE_MESSAGE,
            SapphireProvider.Event.KEY_STATE
    };

    private class EventsLoaderTask extends AsyncTask<Void, Void, Boolean> {
        ContentResolver mResolver;

        @Override
        protected void onPreExecute() {
            mResolver = mContext.getContentResolver();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            Cursor cursor = mResolver.query(SapphireProvider.Event.CONTENT_URI, EVENT_PROJECTION,
                    null, null, SapphireProvider.Event.KEY_START_TIME + " desc");
            if (cursor == null) {
                return false;
            }
            if (!cursor.moveToFirst()) {
                cursor.close();
                return false;
            }
            ArrayList<Event> newEvents = new ArrayList<Event>();
            HashMap<Long, Integer> newEventsMap = new HashMap<Long, Integer>();
            do {
                final long id = cursor.getLong(SapphireProvider.Event.ID_COLUMN);
                final String name = cursor.getString(SapphireProvider.Event.NAME_COLUMN);
                final long startTime = cursor.getLong(SapphireProvider.Event.START_TIME_COLUMN);
                final long endTime = cursor.getLong(SapphireProvider.Event.END_TIME_COLUMN);
                final long noticedTime = cursor.getLong(SapphireProvider.Event.NOTICED_TIME_COLUMN);
                final String noticeMessage = cursor
                        .getString(SapphireProvider.Event.NOTICE_MESSAGE_COLUMN);
                final String state = cursor.getString(SapphireProvider.Event.STATE_COLUMN);
                Event event = new Event(id, name, startTime, endTime, noticedTime, noticeMessage,
                        Event.State.valueOf(state));
                newEvents.add(event);
                newEventsMap.put(id, newEvents.size() - 1);
            } while (cursor.moveToNext());
            mEvents = newEvents;
            mEventsMap = newEventsMap;
            cursor.close();
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                notifyDataChanged();
            }
        }
    }
}
