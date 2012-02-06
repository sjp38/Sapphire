
package org.drykiss.android.app.sapphire.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.drykiss.android.app.sapphire.data.ChargeCalcResult.Bill;
import org.drykiss.android.app.sapphire.data.DataManager.OnDataChangedListener;
import org.drykiss.android.app.sapphire.provider.SapphireProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class MembersManager implements DataManager.DataPracticeManager {
    private static final String TAG = "Sapphire_data_member_manager";

    private static final int LOAD_EVENT_MEMBER = 1;
    private static final int LOAD_PAYMENT_MEMBER = 2;

    private OnDataChangedListener mListener;
    private Context mContext;

    private HashMap<Long, ArrayList<Member>> mEventMembers = new HashMap<Long, ArrayList<Member>>();
    private HashMap<Long, ArrayList<Member>> mPaymentMembers = new HashMap<Long, ArrayList<Member>>();

    public MembersManager(Context context) {
        mContext = context;
    }

    @Override
    public void loadDatas() {
        new MembersLoaderTask().execute(LOAD_EVENT_MEMBER);
        new MembersLoaderTask().execute(LOAD_PAYMENT_MEMBER);
    }

    private ArrayList<Member> getMembers(long eventId, long paymentId) {
        ArrayList<Member> members = null;
        if (paymentId == -1) {
            members = mEventMembers.get(eventId);
        } else {
            members = mPaymentMembers.get(paymentId);
        }
        return members;
    }

    public Member get(long eventId, long paymentId, int position) {
        final ArrayList<Member> members = getMembers(eventId, paymentId);
        if (members == null || position >= members.size()) {
            return null;
        }
        return members.get(position);
    }

    public ArrayList<Member> getUnpaidMembers() {
        ArrayList<Member> result = new ArrayList<Member>();
        Collection<ArrayList<Member>> memberLists = mEventMembers.values();
        for (ArrayList<Member> members : memberLists) {
            for (Member member : members) {
                if (member.mPaymentState != Member.PaymentState.COMPLETE) {
                    result.add(member);
                }
            }
        }
        return result;
    }

    public void update(int position, Member member) {
        final ArrayList<Member> members = getMembers(member.mParentEventId,
                member.mParentPaymentId);
        if (members == null || position >= members.size()) {
            return;
        }
        final Member oldMember = members.get(position);
        final ContentValues values = oldMember.getDiff(member);
        if (values.size() <= 0) {
            return;
        }
        final Uri uri = ContentUris.withAppendedId(
                SapphireProvider.Member.CONTENT_URI, member.mId);
        mContext.getContentResolver().update(uri, values, null, null);

        members.remove(position);
        members.add(position, member);

        notifyDataChanged();
    }

    public void update(Member member) {
        final ArrayList<Member> members = getMembers(member.mParentEventId,
                member.mParentPaymentId);

        if (members == null) {
            return;
        }
        final int membersCount = members.size();
        int position = -1;
        for (int i = 0; i < membersCount; i++) {
            if (members.get(i).mId == member.mId) {
                position = i;
            }
        }
        if (position < 0) {
            Log.e(TAG, "Failed to update member.");
            return;
        }
        final Member oldMember = members.get(position);
        final ContentValues values = oldMember.getDiff(member);
        if (values.size() <=0) {
            return;
        }
        final Uri uri = ContentUris.withAppendedId(
                SapphireProvider.Member.CONTENT_URI, member.mId);
        mContext.getContentResolver().update(uri, values, null, null);

        members.remove(position);
        members.add(position, member);

        notifyDataChanged();
    }

    public void add(Member member) {
        ArrayList<Member> members = getMembers(member.mParentEventId,
                member.mParentPaymentId);
        if (members == null) {
            members = new ArrayList<Member>();
            if (member.mParentPaymentId == -1) {
                mEventMembers.put(member.mParentEventId, members);
            } else {
                mPaymentMembers.put(member.mParentPaymentId, members);
            }
        }
        final ContentValues values = member.getContentValues();
        final Uri uri = mContext.getContentResolver().insert(
                SapphireProvider.Member.CONTENT_URI, values);
        final List<String> segments = uri.getPathSegments();
        member.mId = Long.valueOf(segments.get(segments.size() - 1));

        members.add(member);
        notifyDataChanged();
    }

    public void remove(long eventId, long paymentId, int position) {
        final ArrayList<Member> members = getMembers(eventId, paymentId);
        if (members == null || position >= members.size()) {
            return;
        }
        final Member member = members.get(position);
        final Uri uri = ContentUris.withAppendedId(SapphireProvider.Member.CONTENT_URI, member.mId);
        mContext.getContentResolver().delete(uri, null, null);

        members.remove(position);
        notifyDataChanged();
    }

    /**
     * Package public method. Remove all member datas with parent event /
     * payment id silently.
     */
    void remove(long eventId, long paymentId) {
        if (paymentId == -1) {
            mEventMembers.remove(eventId);
            mContext.getContentResolver()
                    .delete(SapphireProvider.Member.CONTENT_URI,
                            SapphireProvider.Member.KEY_PARENT_EVENT + "=" + eventId + " AND "
                                    + SapphireProvider.Member.KEY_PARENT_PAYMENT + "=-1", null);
        } else {
            mPaymentMembers.remove(paymentId);
            mContext.getContentResolver().delete(
                    SapphireProvider.Member.CONTENT_URI,
                    SapphireProvider.Member.KEY_PARENT_PAYMENT + "=" + paymentId, null);
        }

    }

    public int getCount(long eventId, long paymentId) {
        final ArrayList<Member> members = getMembers(eventId, paymentId);
        if (members == null) {
            return 0;
        }
        return members.size();
    }

    public void notifyDataChanged() {
        if (mListener != null) {
            mListener.onDataChanged();
        }
    }

    @Override
    public void setDataChangedListener(OnDataChangedListener listener) {
        mListener = listener;
    }

    @Override
    public void clearDataChangedListener(OnDataChangedListener listener) {
        mListener = null;
    }

    /**
     * Point for member = member.allocPercentage *
     * payment.member.allocPercentage / 100 Total point for payment = sum (Point
     * for member) Charge for member = payment.cost / Total point for payment *
     * point for member.
     */
    public ArrayList<ChargeCalcResult> calculate(long eventId) {
        ArrayList<Member> members = mEventMembers.get(eventId);
        HashMap<Long, ChargeCalcData> chargeCalcs = new HashMap<Long, ChargeCalcData>();

        if (members == null) {
            return null;
        }
        for (Member member : members) {
            chargeCalcs.put(member.mContactId, new ChargeCalcData(member.mAllocPercentage, 100,
                    member));
        }

        final ArrayList<ChargeCalcResult> results = new ArrayList<ChargeCalcResult>();
        ChargeCalcResult totalResult = new ChargeCalcResult(ChargeCalcResult.TOTAL_CHARGE, 0);
        final int paymentsCount = DataManager.INSTANCE.getPaymentsCount(eventId);
        for (int i = 0; i < paymentsCount; i++) {
            final Payment payment = DataManager.INSTANCE.getPayment(eventId, i);
            totalResult.mPaymentCost += payment.mCost;
            final ChargeCalcResult result = new ChargeCalcResult(payment.mName, payment.mCost);
            results.add(result);

            ArrayList<Member> paymentMembers = mPaymentMembers.get(payment.mId);
            if (paymentMembers != null) {
                for (Member member : paymentMembers) {
                    ChargeCalcData data = chargeCalcs.get(member.mContactId);
                    data.mPaymentAllocPercentage = member.mAllocPercentage;
                }
            }

            double paymentTotalPoint = 0;
            for (Member member : members) {
                ChargeCalcData data = chargeCalcs.get(member.mContactId);
                paymentTotalPoint += (double) data.mEventAllocPercentage
                        * (double) data.mPaymentAllocPercentage
                        / 100.0;
            }
            for (Member member : members) {
                ChargeCalcData data = chargeCalcs.get(member.mContactId);
                double charge = (payment.mCost / (double) paymentTotalPoint)
                        * ((double) data.mEventAllocPercentage
                                * (double) data.mPaymentAllocPercentage / 100.0);
                data.mTotalCharge += charge;
                Bill bill = new Bill(member.mName, member.mAddress, data.mEventAllocPercentage,
                        data.mPaymentAllocPercentage, charge);
                result.mBills.add(bill);
                if (i == paymentsCount - 1) {
                    Bill totalBill = new Bill(member.mName, member.mAddress,
                            data.mEventAllocPercentage, data.mPaymentAllocPercentage,
                            data.mTotalCharge);
                    totalResult.mBills.add(totalBill);
                    member.mCharge = data.mTotalCharge;
                }
                data.mPaymentAllocPercentage = 100;
            }
        }
        results.add(totalResult);
        return results;
    }

    private class ChargeCalcData {
        public ChargeCalcData(int eventAlloc, int paymentAlloc, Member member) {
            mEventAllocPercentage = eventAlloc;
            mPaymentAllocPercentage = paymentAlloc;
            mMember = member;
        }

        int mEventAllocPercentage;
        int mPaymentAllocPercentage;
        Member mMember;
        double mTotalCharge;
    }

    private static final String[] MEMBER_PROJECTION = {
            SapphireProvider.Member.KEY_ID,
            SapphireProvider.Member.KEY_CONTACT_ID,
            SapphireProvider.Member.KEY_CONTACT_LOOKUP_ID,
            SapphireProvider.Member.KEY_PHOTO_ID,
            SapphireProvider.Member.KEY_NAME,
            SapphireProvider.Member.KEY_ADDRESS,
            SapphireProvider.Member.KEY_PAYMENT_STATE,
            SapphireProvider.Member.KEY_PAID_TIME,
            SapphireProvider.Member.KEY_ALLOC_REASON,
            SapphireProvider.Member.KEY_ALLOC_PERCENTAGE,
            SapphireProvider.Member.KEY_PAYMENT_CONFIRM_MESSAGE,
            SapphireProvider.Member.KEY_PARENT_PAYMENT,
            SapphireProvider.Member.KEY_PARENT_EVENT
    };

    private class MembersLoaderTask extends AsyncTask<Integer, Void, Boolean> {
        ContentResolver mResolver;

        @Override
        protected void onPreExecute() {
            mResolver = mContext.getContentResolver();
        }

        @Override
        protected Boolean doInBackground(Integer... params) {
            final int task = params[0];
            Uri uri = null;
            String[] projection = null;
            String sortOrder = null;
            String selection = null;
            if (task == LOAD_EVENT_MEMBER) {
                uri = SapphireProvider.Member.CONTENT_URI;
                projection = MEMBER_PROJECTION;
                selection = SapphireProvider.Member.KEY_PARENT_PAYMENT + "=-1";
                sortOrder = SapphireProvider.Member.KEY_PARENT_EVENT + " ASC, "
                        + SapphireProvider.Member.KEY_NAME + " ASC";
            } else if (task == LOAD_PAYMENT_MEMBER) {
                uri = SapphireProvider.Member.CONTENT_URI;
                projection = MEMBER_PROJECTION;
                selection = SapphireProvider.Member.KEY_PARENT_PAYMENT
                        + " NOT NULL AND "
                        + SapphireProvider.Member.KEY_PARENT_PAYMENT + "!=-1";
                sortOrder = SapphireProvider.Member.KEY_PARENT_PAYMENT
                        + " ASC, " + SapphireProvider.Member.KEY_NAME + " ASC";
            }

            Cursor cursor = mResolver.query(uri, projection, selection, null,
                    sortOrder);
            if (cursor == null) {
                return false;
            }
            if (!cursor.moveToFirst()) {
                cursor.close();
                return false;
            }
            switch (task) {
                case LOAD_EVENT_MEMBER:
                    getMembersFromCursor(cursor, task);
                    break;
                case LOAD_PAYMENT_MEMBER:
                    getMembersFromCursor(cursor, task);
                    break;
                default:
                    break;
            }

            cursor.close();
            return true;
        }

        private void getMembersFromCursor(Cursor cursor, int task) {
            ArrayList<Member> newMembers = new ArrayList<Member>();
            long lastParentEvent = -1;
            long lastParentPayment = -1;
            do {
                final long id = cursor
                        .getLong(SapphireProvider.Member.ID_COLUMN);
                final long contactId = cursor
                        .getLong(SapphireProvider.Member.CONTACT_ID_COLUMN);
                final String contactLookupId = cursor
                        .getString(SapphireProvider.Member.CONTACT_LOOKUP_ID_COLUMN);
                final long photoId = cursor
                        .getLong(SapphireProvider.Member.PHOTO_ID_COLUMN);
                final String name = cursor
                        .getString(SapphireProvider.Member.NAME_COLUMN);
                final String address = cursor
                        .getString(SapphireProvider.Member.ADDRESS_COLUMN);
                final String paymentState = cursor
                        .getString(SapphireProvider.Member.PAYMENT_STATE_COLUMN);
                final long paidTime = cursor.getLong(SapphireProvider.Member.PAID_TIME_COLUMN);
                final String paymentException = cursor
                        .getString(SapphireProvider.Member.ALLOC_REASON_COLUMN);
                final int paymentPercentage = cursor
                        .getInt(SapphireProvider.Member.ALLOC_PERCENTAGE_COLUMN);
                final String paymentConfirmMsg = cursor
                        .getString(SapphireProvider.Member.PAYMENT_CONFIRM_MESSAGE);
                final long parentPayment = cursor
                        .getLong(SapphireProvider.Member.PARENT_PAYMENT_COLUMN);
                final long parentEvent = cursor
                        .getLong(SapphireProvider.Member.PARENT_EVENT_COLUMN);

                if (task == LOAD_EVENT_MEMBER && lastParentEvent != -1
                        && lastParentEvent != parentEvent) {
                    mEventMembers.put(lastParentEvent, newMembers);
                    newMembers = new ArrayList<Member>();
                } else if (task == LOAD_PAYMENT_MEMBER
                        && lastParentPayment != -1
                        && lastParentPayment != parentPayment) {
                    mPaymentMembers.put(lastParentPayment, newMembers);
                    newMembers = new ArrayList<Member>();
                }
                lastParentPayment = parentPayment;
                lastParentEvent = parentEvent;

                final Member member = new Member(id, parentEvent, parentPayment, contactId,
                        contactLookupId, photoId, name, address,
                        Member.PaymentState.valueOf(paymentState), paidTime, paymentException,
                        paymentPercentage, paymentConfirmMsg);
                newMembers.add(member);
            } while (cursor.moveToNext());
            if (task == LOAD_EVENT_MEMBER) {
                mEventMembers.put(lastParentEvent, newMembers);
            } else {
                mPaymentMembers.put(lastParentPayment, newMembers);
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                notifyDataChanged();
            }
        }
    }

}
