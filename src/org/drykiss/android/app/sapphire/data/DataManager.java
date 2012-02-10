
package org.drykiss.android.app.sapphire.data;

import android.content.Context;

import java.util.ArrayList;

public class DataManager {
    public static final DataManager INSTANCE = new DataManager();

    private EventsManager mEventsManager;
    private PaymentsManager mPaymentsManager;
    private MembersManager mMembersManager;
    private boolean mIsActive = false;

    private Context mContext;

    private DataManager() {
    }

    public boolean isActive() {
        return mContext != null && mIsActive;
    }

    public void setContext(Context context) {
        if (mEventsManager == null) {
            mEventsManager = new EventsManager(context);
        }
        if (mPaymentsManager == null) {
            mPaymentsManager = new PaymentsManager(context);
        }
        if (mMembersManager == null) {
            mMembersManager = new MembersManager(context);
        }
        mContext = context;
    }

    public void loadDatas() {
        mEventsManager.loadDatas();
        mPaymentsManager.loadDatas();
        mMembersManager.loadDatas();
        mIsActive = true;
    }

    public Event getEvent(int position) {
        return mEventsManager.get(position);
    }

    public ArrayList<Event> getActiveEventOn(long timeStamp) {
        return mEventsManager.getActiveEventOn(timeStamp);
    }

    public void updateEvent(int position, Event event) {
        mEventsManager.update(position, event);
    }

    public void addEvent(Event event) {
        mEventsManager.add(event);
    }

    public void removeEvent(int position) {
        final Event event = mEventsManager.get(position);
        final ArrayList<Payment> payments = mPaymentsManager.get(event.mId);
        if (payments != null) {
            for (Payment payment : payments) {
                mMembersManager.remove(event.mId, payment.mId);
            }
        }

        mMembersManager.remove(event.mId, -1);
        mPaymentsManager.remove(event.mId);

        mEventsManager.remove(position);
    }

    public int getEventsCount() {
        return mEventsManager.getCount();
    }

    public void registerEventChangedListener(OnDataChangedListener listener) {
        mEventsManager.registerDataChangedListener(listener);
    }

    public void unregisterEventChangedListener(OnDataChangedListener listener) {
        mEventsManager.unregisterDataChangedListener(listener);
    }

    public Payment getPayment(long eventId, int position) {
        return mPaymentsManager.get(eventId, position);
    }

    public void updatePayment(int position, Payment payment) {
        mPaymentsManager.update(position, payment);
    }

    public void addPayment(Payment payment) {
        mPaymentsManager.add(payment);
    }

    public void removePayment(long eventId, int position) {
        final Payment payment = mPaymentsManager.get(eventId, position);
        mMembersManager.remove(eventId, payment.mId);

        mPaymentsManager.remove(eventId, position);
    }

    public int getPaymentsCount(long eventId) {
        return mPaymentsManager.getCount(eventId);
    }

    public void registerPaymentsChangedListener(OnDataChangedListener listener) {
        mPaymentsManager.registerDataChangedListener(listener);
    }

    public void unregisterPaymentsChangedListener(OnDataChangedListener listener) {
        mPaymentsManager.unregisterDataChangedListener(listener);
    }

    /**
     * @param eventId Id of member's parent event.
     * @param paymentId Id of member's parent payment. -1 if want event's
     *            members.
     * @param position Position of member on parent.
     * @return Member for event or payment.
     */
    public Member getMember(long eventId, long paymentId, int position) {
        return mMembersManager.get(eventId, paymentId, position);
    }

    public ArrayList<Member> getUnpaidMembers() {
        return mMembersManager.getUnpaidMembers();
    }

    public void updateMember(int position, Member member) {
        mMembersManager.update(position, member);
    }

    public void updateMember(Member member) {
        mMembersManager.update(member);
    }

    public void addMember(Member member) {
        mMembersManager.add(member);
    }

    public void removeMember(long eventId, long paymentId, int position) {
        mMembersManager.remove(eventId, paymentId, position);
    }

    public int getMemberCount(long eventId, long paymentId) {
        return mMembersManager.getCount(eventId, paymentId);
    }

    public void registerMembersChangedListener(OnDataChangedListener listener) {
        mMembersManager.registerDataChangedListener(listener);
    }

    public void unregisterMembersChangedListener(OnDataChangedListener listener) {
        mMembersManager.unregisterDataChangedListener(listener);
    }

    public ArrayList<ChargeCalcResult> calculate(long eventId) {
        return mMembersManager.calculate(eventId);
    }

    public interface DataPracticeManager {
        void loadDatas();

        void registerDataChangedListener(OnDataChangedListener listener);

        void unregisterDataChangedListener(OnDataChangedListener listener);
    }

    public interface OnDataChangedListener {
        void onDataChanged();
    }
}
