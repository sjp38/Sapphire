
package org.drykiss.android.app.sapphire.data;

import android.content.ContentValues;

import org.drykiss.android.app.sapphire.provider.SapphireProvider;

public class Payment {
    long mId;
    long mParentEventId;
    String mName;
    long mTime;
    double mCost;

    public Payment(long mId, long mParentEventId, String mName, long mTime, double mCost) {
        super();
        this.mId = mId;
        this.mParentEventId = mParentEventId;
        this.mName = mName;
        this.mTime = mTime;
        this.mCost = mCost;
    }

    public ContentValues getContentValues() {
        ContentValues result = new ContentValues();
        result.put(SapphireProvider.Payment.KEY_PARENT_EVENT, mParentEventId);
        result.put(SapphireProvider.Payment.KEY_NAME, mName);
        result.put(SapphireProvider.Payment.KEY_TIME, mTime);
        result.put(SapphireProvider.Payment.KEY_COST, mCost);

        return result;
    }

    public ContentValues getDiff(Payment newPayment) {
        ContentValues result = new ContentValues();
        if (this.equals(newPayment)) {
            return result;
        }
        if (mParentEventId != newPayment.mParentEventId) {
            result.put(SapphireProvider.Payment.KEY_PARENT_EVENT, newPayment.mParentEventId);
        }
        if (!mName.equals(newPayment.mName)) {
            result.put(SapphireProvider.Payment.KEY_NAME, newPayment.mName);
        }
        if (mTime != newPayment.mTime) {
            result.put(SapphireProvider.Payment.KEY_TIME, newPayment.mTime);
        }
        if (mCost != newPayment.mCost) {
            result.put(SapphireProvider.Payment.KEY_COST, newPayment.mCost);
        }

        return result;
    }

    @Override
    public String toString() {
        return "Payment [mId=" + mId + ", mParentEventId=" + mParentEventId + ", mName=" + mName
                + ", mTime=" + mTime + ", mCost=" + mCost + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(mCost);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + (int) (mId ^ (mId >>> 32));
        result = prime * result + ((mName == null) ? 0 : mName.hashCode());
        result = prime * result + (int) (mParentEventId ^ (mParentEventId >>> 32));
        result = prime * result + (int) (mTime ^ (mTime >>> 32));
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
        Payment other = (Payment) obj;
        if (mId != other.mId)
            return false;
        if (mTime != other.mTime)
            return false;
        if (mParentEventId != other.mParentEventId)
            return false;
        if (mCost != other.mCost)
            return false;
        if (mName == null) {
            if (other.mName != null)
                return false;
        } else if (!mName.equals(other.mName))
            return false;
        return true;
    }

    public long getmId() {
        return mId;
    }

    public void setmId(long mId) {
        this.mId = mId;
    }

    public long getmParentEventId() {
        return mParentEventId;
    }

    public void setmParentEventId(long mParentEventId) {
        this.mParentEventId = mParentEventId;
    }

    public String getmName() {
        return mName;
    }

    public void setmName(String mName) {
        this.mName = mName;
    }

    public long getmTime() {
        return mTime;
    }

    public void setmTime(long mTime) {
        this.mTime = mTime;
    }

    public double getmCost() {
        return mCost;
    }

    public void setmCost(int mCost) {
        this.mCost = mCost;
    }
}
