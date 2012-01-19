
package org.drykiss.android.app.sapphire.data;

import android.content.ContentValues;

import org.drykiss.android.app.sapphire.provider.SapphireProvider;

public class Member {
    public static enum PaymentState {
        IDLE, NOTICED, COMPLETE;
    }

    long mId;
    long mParentEventId;
    long mParentPaymentId;
    long mContactId = -1;
    String mContactLookupId;
    long mContactPhotoId = -1;

    String mName;
    String mAddress;
    PaymentState mPaymentState;
    long mPaidTime;
    String mAllocReason;
    int mAllocPercentage;
    String mPaymentConfirmMessage;
    double mCharge = -1;

    public Member(long mId, long mParentEventId, long mParentPaymentId, long mContactId,
            String mContactLookupId, long mContactPhotoId, String mName, String mAddress,
            PaymentState mPaymentState, long mPaidTime, String mAllocReason,
            int mAllocPercentage, String mPaymentConfirmMessage) {
        super();
        this.mId = mId;
        this.mParentEventId = mParentEventId;
        this.mParentPaymentId = mParentPaymentId;
        this.mContactId = mContactId;
        this.mContactLookupId = mContactLookupId;
        this.mContactPhotoId = mContactPhotoId;
        this.mName = mName;
        this.mAddress = mAddress;
        this.mPaymentState = mPaymentState;
        this.mPaidTime = mPaidTime;
        this.mAllocReason = mAllocReason;
        this.mAllocPercentage = mAllocPercentage;
        this.mPaymentConfirmMessage = mPaymentConfirmMessage;
    }

    public Member(Member member) {
        this(member.mId, member.mParentEventId, member.mParentPaymentId, member.mContactId,
                member.mContactLookupId, member.mContactPhotoId, member.mName, member.mAddress,
                member.mPaymentState, member.mPaidTime, member.mAllocReason,
                member.mAllocPercentage,
                member.mPaymentConfirmMessage);
    }

    public ContentValues getContentValues() {
        ContentValues result = new ContentValues();
        result.put(SapphireProvider.Member.KEY_PARENT_EVENT, mParentEventId);
        result.put(SapphireProvider.Member.KEY_PARENT_PAYMENT, mParentPaymentId);
        result.put(SapphireProvider.Member.KEY_CONTACT_ID, mContactId);
        result.put(SapphireProvider.Member.KEY_CONTACT_LOOKUP_ID, mContactLookupId);
        result.put(SapphireProvider.Member.KEY_PHOTO_ID, mContactPhotoId);
        result.put(SapphireProvider.Member.KEY_NAME, mName);
        result.put(SapphireProvider.Member.KEY_ADDRESS, mAddress);
        result.put(SapphireProvider.Member.KEY_PAYMENT_STATE, mPaymentState.toString());
        result.put(SapphireProvider.Member.KEY_PAID_TIME, mPaidTime);
        result.put(SapphireProvider.Member.KEY_ALLOC_REASON, mAllocReason);
        result.put(SapphireProvider.Member.KEY_ALLOC_PERCENTAGE, mAllocPercentage);
        result.put(SapphireProvider.Member.KEY_PAYMENT_CONFIRM_MESSAGE, mPaymentConfirmMessage);

        return result;
    }

    public ContentValues getDiff(Member newMember) {
        ContentValues result = new ContentValues();
        if (this.equals(newMember)) {
            return result;
        }
        if (mContactId != newMember.mContactId) {
            result.put(SapphireProvider.Member.KEY_CONTACT_ID, newMember.mContactId);
        }
        if (!mContactLookupId.equals(newMember.mContactLookupId)) {
            result.put(SapphireProvider.Member.KEY_CONTACT_LOOKUP_ID, newMember.mContactLookupId);
        }
        if (mContactPhotoId != newMember.mContactPhotoId) {
            result.put(SapphireProvider.Member.KEY_PHOTO_ID, newMember.mContactPhotoId);
        }
        if (!mName.equals(newMember.mName)) {
            result.put(SapphireProvider.Member.KEY_NAME, newMember.mName);
        }
        if (!mAddress.equals(newMember.mAddress)) {
            result.put(SapphireProvider.Member.KEY_ADDRESS, newMember.mAddress);
        }
        if (mPaymentState != newMember.mPaymentState) {
            result.put(SapphireProvider.Member.KEY_PAYMENT_STATE,
                    newMember.mPaymentState.toString());
        }
        if (mPaidTime != newMember.mPaidTime) {
            result.put(SapphireProvider.Member.KEY_PAID_TIME, newMember.mPaidTime);
        }
        if (!mAllocReason.equals(newMember.mAllocReason)) {
            result.put(SapphireProvider.Member.KEY_ALLOC_REASON, newMember.mAllocReason);
        }
        if (mAllocPercentage != newMember.mAllocPercentage) {
            result.put(SapphireProvider.Member.KEY_ALLOC_PERCENTAGE, newMember.mAllocPercentage);
        }
        if (mPaymentConfirmMessage != newMember.mPaymentConfirmMessage) {
            result.put(SapphireProvider.Member.KEY_PAYMENT_CONFIRM_MESSAGE,
                    newMember.mPaymentConfirmMessage);
        }
        return result;
    }

    @Override
    public String toString() {
        return "Member [mId=" + mId + ", mParentEventId=" + mParentEventId + ", mParentPaymentId="
                + mParentPaymentId + ", mContactId=" + mContactId + ", mContactLookupId="
                + mContactLookupId + ", mContactPhotoId=" + mContactPhotoId + ", mName=" + mName
                + ", mAddress=" + mAddress + ", mPaymentState=" + mPaymentState + ", mPaidTime="
                + mPaidTime + ", mAllocReason=" + mAllocReason + ", mAllocPercentage="
                + mAllocPercentage + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mAddress == null) ? 0 : mAddress.hashCode());
        result = prime * result + (int) (mContactId ^ (mContactId >>> 32));
        result = prime * result + ((mContactLookupId == null) ? 0 : mContactLookupId.hashCode());
        result = prime * result + (int) (mContactPhotoId ^ (mContactPhotoId >>> 32));
        result = prime * result + (int) (mId ^ (mId >>> 32));
        result = prime * result + ((mName == null) ? 0 : mName.hashCode());
        result = prime * result + (int) (mPaidTime ^ (mPaidTime >>> 32));
        result = prime * result + (int) (mParentEventId ^ (mParentEventId >>> 32));
        result = prime * result + (int) (mParentPaymentId ^ (mParentPaymentId >>> 32));
        result = prime * result + ((mAllocReason == null) ? 0 : mAllocReason.hashCode());
        result = prime * result + mAllocPercentage;
        result = prime * result + ((mPaymentState == null) ? 0 : mPaymentState.hashCode());
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
        Member other = (Member) obj;
        if (mId != other.mId)
            return false;
        if (mParentEventId != other.mParentEventId)
            return false;
        if (mParentPaymentId != other.mParentPaymentId)
            return false;
        if (mPaidTime != other.mPaidTime)
            return false;
        if (mContactId != other.mContactId)
            return false;
        if (mContactLookupId == null) {
            if (other.mContactLookupId != null)
                return false;
        } else if (!mContactLookupId.equals(other.mContactLookupId))
            return false;
        if (mContactPhotoId != other.mContactPhotoId)
            return false;
        if (mAddress == null) {
            if (other.mAddress != null)
                return false;
        } else if (!mAddress.equals(other.mAddress))
            return false;
        if (mName == null) {
            if (other.mName != null)
                return false;
        } else if (!mName.equals(other.mName))
            return false;
        if (mAllocReason == null) {
            if (other.mAllocReason != null)
                return false;
        } else if (!mAllocReason.equals(other.mAllocReason))
            return false;
        if (mAllocPercentage != other.mAllocPercentage)
            return false;
        if (mPaymentState != other.mPaymentState)
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

    public long getmParentPaymentId() {
        return mParentPaymentId;
    }

    public void setmParentPaymentId(long mParentPaymentId) {
        this.mParentPaymentId = mParentPaymentId;
    }

    public long getmContactId() {
        return mContactId;
    }

    public void setmContactId(long mContactId) {
        this.mContactId = mContactId;
    }

    public String getmContactLookupId() {
        return mContactLookupId;
    }

    public void setmContactLookupId(String mContactLookupId) {
        this.mContactLookupId = mContactLookupId;
    }

    public long getmContactPhotoId() {
        return mContactPhotoId;
    }

    public void setmContactPhotoId(long mContactPhotoId) {
        this.mContactPhotoId = mContactPhotoId;
    }

    public String getmName() {
        return mName;
    }

    public void setmName(String mName) {
        this.mName = mName;
    }

    public String getmAddress() {
        return mAddress;
    }

    public void setmAddress(String mAddress) {
        this.mAddress = mAddress;
    }

    public PaymentState getmPaymentState() {
        return mPaymentState;
    }

    public void setmPaymentState(PaymentState mPaymentState) {
        this.mPaymentState = mPaymentState;
    }

    public long getmPaidTime() {
        return mPaidTime;
    }

    public void setmPaidTime(long mPaidTime) {
        this.mPaidTime = mPaidTime;
    }

    public String getmAllocReason() {
        return mAllocReason;
    }

    public void setmAllocReason(String mAllocReason) {
        this.mAllocReason = mAllocReason;
    }

    public int getmAllocPercentage() {
        return mAllocPercentage;
    }

    public void setmAllocPercentage(int mAllocPercentage) {
        this.mAllocPercentage = mAllocPercentage;
    }

    public String getmPaymentConfirmMessage() {
        return mPaymentConfirmMessage;
    }

    public void setmPaymentConfirmMessage(String mPaymentConfirmMessage) {
        this.mPaymentConfirmMessage = mPaymentConfirmMessage;
    }

    public double getmCharge() {
        return mCharge;
    }

    public void setmCharge(double mCharge) {
        this.mCharge = mCharge;
    }
}
