
package org.drykiss.android.app.sapphire.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import org.drykiss.android.app.sapphire.data.DataManager.OnDataChangedListener;
import org.drykiss.android.app.sapphire.provider.SapphireProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PaymentsManager implements DataManager.DataPracticeManager {
    private HashMap<Long, ArrayList<Payment>> mPayments = new HashMap<Long, ArrayList<Payment>>();
    private ArrayList<OnDataChangedListener> mListeners = new ArrayList<OnDataChangedListener>();

    private Context mContext;

    public PaymentsManager(Context context) {
        mContext = context;
    }

    @Override
    public void loadDatas() {
        new PaymentsLoaderTask().execute();
    }

    public Payment get(long eventId, int position) {
        ArrayList<Payment> payments = mPayments.get(eventId);
        if (payments == null || position >= payments.size()) {
            return null;
        }
        return payments.get(position);
    }

    /**
     * Package public method. Get all payments with specific parent event id.
     * 
     * @return All payments of eventId's child.
     */
    ArrayList<Payment> get(long eventId) {
        return mPayments.get(eventId);
    }

    public void update(int position, Payment payment) {
        ArrayList<Payment> payments = mPayments.get(payment.mParentEventId);
        if (payments == null || position >= payments.size()) {
            return;
        }
        Payment oldPayment = payments.get(position);
        ContentValues values = oldPayment.getDiff(payment);
        if (values.size() <= 0) {
            return;
        }
        Uri uri = ContentUris.withAppendedId(
                SapphireProvider.Payment.CONTENT_URI, oldPayment.mId);
        mContext.getContentResolver().update(uri, values, null, null);

        payments.remove(position);
        payments.add(position, payment);
        notifyDataChanged();
    }

    public void add(Payment payment) {
        ArrayList<Payment> payments = mPayments.get(payment.mParentEventId);
        if (payments == null) {
            payments = new ArrayList<Payment>();
            mPayments.put(payment.mParentEventId, payments);
        }
        payments.add(payment);

        ContentValues values = payment.getContentValues();
        final Uri uri = mContext.getContentResolver().insert(
                SapphireProvider.Payment.CONTENT_URI, values);
        final List<String> segments = uri.getPathSegments();
        payment.mId = Long.valueOf(segments.get(segments.size() - 1));
        notifyDataChanged();
    }

    public void remove(long eventId, int position) {
        ArrayList<Payment> payments = mPayments.get(eventId);
        if (payments == null || position >= payments.size()) {
            return;
        }
        final Payment payment = payments.get(position);
        payments.remove(position);

        Uri uri = ContentUris.withAppendedId(
                SapphireProvider.Payment.CONTENT_URI, payment.mId);
        mContext.getContentResolver().delete(uri, null, null);
        notifyDataChanged();
    }

    /**
     * Package-public. Remove all payments data with specific parent event id
     * silently.
     */
    void remove(long eventId) {
        mPayments.remove(eventId);
        mContext.getContentResolver()
                .delete(SapphireProvider.Payment.CONTENT_URI,
                        SapphireProvider.Payment.KEY_PARENT_EVENT + "="
                                + eventId, null);
    }

    public int getCount(long eventId) {
        ArrayList<Payment> payments = mPayments.get(eventId);
        if (payments == null) {
            return 0;
        }
        return payments.size();
    }

    private void notifyDataChanged() {
        for (OnDataChangedListener listener : mListeners) {
            listener.onDataChanged();
        }
    }

    @Override
    public void registerDataChangedListener(OnDataChangedListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    @Override
    public void unregisterDataChangedListener(OnDataChangedListener listener) {
        mListeners.remove(listener);
    }

    private static final String[] PAYMENT_PROJECTION = {
            SapphireProvider.Payment.KEY_ID, SapphireProvider.Payment.KEY_NAME,
            SapphireProvider.Payment.KEY_TIME,
            SapphireProvider.Payment.KEY_COST,
            SapphireProvider.Payment.KEY_PARENT_EVENT
    };

    private class PaymentsLoaderTask extends AsyncTask<Void, Void, Boolean> {
        ContentResolver mResolver;

        @Override
        protected void onPreExecute() {
            mResolver = mContext.getContentResolver();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            final String sortOrder = SapphireProvider.Payment.KEY_PARENT_EVENT
                    + " ASC, " + SapphireProvider.Payment.KEY_TIME + " ASC";

            Cursor cursor = mResolver.query(
                    SapphireProvider.Payment.CONTENT_URI, PAYMENT_PROJECTION,
                    null, null, sortOrder);
            if (cursor == null) {
                return false;
            }
            if (!cursor.moveToFirst()) {
                cursor.close();
                return false;
            }
            getPaymentsFromCursor(cursor);

            cursor.close();
            return true;
        }

        private void getPaymentsFromCursor(Cursor cursor) {
            ArrayList<Payment> newPayments = new ArrayList<Payment>();
            long lastParentEvent = -1;
            do {
                final long id = cursor
                        .getLong(SapphireProvider.Payment.ID_COLUMN);
                final String name = cursor
                        .getString(SapphireProvider.Payment.NAME_COLUMN);
                final long time = cursor
                        .getLong(SapphireProvider.Payment.TIME_COLUMN);
                final int cost = cursor
                        .getInt(SapphireProvider.Payment.COST_COLUMN);
                final long parentEvent = cursor
                        .getLong(SapphireProvider.Payment.PARENT_EVENT_COLUMN);

                if (lastParentEvent != -1) {
                    if (lastParentEvent != parentEvent) {
                        mPayments.put(lastParentEvent, newPayments);
                        newPayments = new ArrayList<Payment>();
                    }
                }
                lastParentEvent = parentEvent;
                Payment payment = new Payment(id, parentEvent, name, time, cost);

                newPayments.add(payment);
            } while (cursor.moveToNext());
            mPayments.put(lastParentEvent, newPayments);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                notifyDataChanged();
            }
        }
    }

}
