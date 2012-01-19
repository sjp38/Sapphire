
package org.drykiss.android.app.sapphire.data;

import android.content.ContentValues;

import org.drykiss.android.app.sapphire.provider.SapphireProvider;

/**
 * Class for event. i.e. Christmas party with colleagues.
 * 
 * @author sj38_park
 */
public class Event implements Cloneable {
    public enum State {
        IDLE, NOTICED, COMPLETED
    }

    long mId;
    String mName;
    long mStartTime;
    long mEndTime;
    long mPaymentNoticedTime;
    String mNoticeMessage;
    State mState;

    public Event(long mId, String mName, long mStartTime, long mEndTime, long mPaymentNoticedTime,
            String mNoticeMessage, State mState) {
        super();
        this.mId = mId;
        this.mName = mName;
        this.mStartTime = mStartTime;
        this.mEndTime = mEndTime;
        this.mPaymentNoticedTime = mPaymentNoticedTime;
        this.mNoticeMessage = mNoticeMessage;
        this.mState = mState;
    }

    public Event(Event event) {
        this(event.mId, event.mName, event.mStartTime, event.mEndTime, event.mPaymentNoticedTime,
                event.mNoticeMessage, event.mState);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public ContentValues getContentValues() {
        ContentValues result = new ContentValues();
        result.put(SapphireProvider.Event.KEY_NAME, mName);
        result.put(SapphireProvider.Event.KEY_START_TIME, mStartTime);
        result.put(SapphireProvider.Event.KEY_END_TIME, mEndTime);
        result.put(SapphireProvider.Event.KEY_NOTICED_TIME, mPaymentNoticedTime);
        result.put(SapphireProvider.Event.KEY_NOTICE_MESSAGE, mNoticeMessage);
        result.put(SapphireProvider.Event.KEY_STATE, mState.toString());

        return result;
    }

    public ContentValues getDiff(Event newEvent) {
        ContentValues result = new ContentValues();
        if (this.equals(newEvent)) {
            return result;
        }
        if (!mName.equals(newEvent.mName)) {
            result.put(SapphireProvider.Event.KEY_NAME, newEvent.mName);
        }
        if (mStartTime != newEvent.mStartTime) {
            result.put(SapphireProvider.Event.KEY_START_TIME, newEvent.mStartTime);
        }
        if (mStartTime != newEvent.mEndTime) {
            result.put(SapphireProvider.Event.KEY_END_TIME, newEvent.mEndTime);
        }
        if (mStartTime != newEvent.mPaymentNoticedTime) {
            result.put(SapphireProvider.Event.KEY_NOTICED_TIME, newEvent.mPaymentNoticedTime);
        }
        if (!mNoticeMessage.equals(newEvent.mNoticeMessage)) {
            result.put(SapphireProvider.Event.KEY_NOTICE_MESSAGE, newEvent.mNoticeMessage);
        }
        if (mState != newEvent.mState) {
            result.put(SapphireProvider.Event.KEY_STATE, newEvent.mState.toString());
        }

        return result;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (mEndTime ^ (mEndTime >>> 32));
        result = prime * result + (int) (mId ^ (mId >>> 32));
        result = prime * result + ((mName == null) ? 0 : mName.hashCode());
        result = prime * result + ((mNoticeMessage == null) ? 0 : mNoticeMessage.hashCode());
        result = prime * result + (int) (mPaymentNoticedTime ^ (mPaymentNoticedTime >>> 32));
        result = prime * result + (int) (mStartTime ^ (mStartTime >>> 32));
        result = prime * result + ((mState == null) ? 0 : mState.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Event other = (Event) obj;
        if (mId != other.mId)
            return false;
        if (mStartTime != other.mStartTime)
            return false;
        if (mEndTime != other.mEndTime)
            return false;
        if (mPaymentNoticedTime != other.mPaymentNoticedTime)
            return false;
        if (mName == null) {
            if (other.mName != null)
                return false;
        } else if (!mName.equals(other.mName))
            return false;
        if (mNoticeMessage == null) {
            if (other.mNoticeMessage != null)
                return false;
        } else if (!mNoticeMessage.equals(other.mNoticeMessage))
            return false;
        if (mState != other.mState)
            return false;
        return true;
    }

    public long getmId() {
        return mId;
    }

    public void setmId(long mId) {
        this.mId = mId;
    }

    public String getmName() {
        return mName;
    }

    public void setmName(String mName) {
        this.mName = mName;
    }

    public long getmStartTime() {
        return mStartTime;
    }

    public void setmStartTime(long mStartTime) {
        this.mStartTime = mStartTime;
    }

    public long getmEndTime() {
        return mEndTime;
    }

    public void setmEndTime(long mEndTime) {
        this.mEndTime = mEndTime;
    }

    public long getmPaymentNoticedTime() {
        return mPaymentNoticedTime;
    }

    public void setmPaymentNoticedTime(long mPaymentNoticedTime) {
        this.mPaymentNoticedTime = mPaymentNoticedTime;
    }

    public String getmNoticeMessage() {
        return mNoticeMessage;
    }

    public void setmNoticeMessage(String mNoticeMessage) {
        this.mNoticeMessage = mNoticeMessage;
    }

    public State getmState() {
        return mState;
    }

    public void setmState(State mState) {
        this.mState = mState;
    }

}
