
package org.drykiss.android.app.sapphire.data;

import java.util.ArrayList;

public class ChargeCalcResult {
    public static final String TOTAL_CHARGE = "Total";
    String mPaymentName;
    double mPaymentCost;
    ArrayList<Bill> mBills = new ArrayList<Bill>();

    public ChargeCalcResult(String paymentName, double paymentCost) {
        super();
        mPaymentName = paymentName;
        mPaymentCost = paymentCost;
    }

    @Override
    public String toString() {
        return "ChargeCalcResult [mPaymentName=" + mPaymentName + ", mPaymentCost=" + mPaymentCost
                + ", mBills=" + mBills + "]";
    }

    public String getmPaymentName() {
        return mPaymentName;
    }

    public void setmPaymentName(String mPaymentName) {
        this.mPaymentName = mPaymentName;
    }

    public double getmPaymentCost() {
        return mPaymentCost;
    }

    public void setmPaymentCost(double mPaymentCost) {
        this.mPaymentCost = mPaymentCost;
    }

    public ArrayList<Bill> getmBills() {
        return mBills;
    }

    public void setmBills(ArrayList<Bill> mBills) {
        this.mBills = mBills;
    }

    public static class Bill implements Comparable<Bill> {
        String mName;
        String mAddress;
        int mGlobalAllocPercentage;
        int mLocalAllocPercentage;
        double mCharge;

        public Bill(String mName, String mAddress, int mGlobalAllocPercentage,
                int mLocalAllocPercentage, double mCharge) {
            super();
            this.mName = mName;
            this.mAddress = mAddress;
            this.mGlobalAllocPercentage = mGlobalAllocPercentage;
            this.mLocalAllocPercentage = mLocalAllocPercentage;
            this.mCharge = mCharge;
        }
        
        @Override
        public int compareTo(Bill other) {            
            return Double.compare(mCharge,other.mCharge);            
        }

        @Override
        public String toString() {
            return "Bill [mName=" + mName + ", mAddress=" + mAddress + ", mGlobalAllocPercentage="
                    + mGlobalAllocPercentage + ", mLocalAllocPercentage=" + mLocalAllocPercentage
                    + ", mCharge=" + mCharge + "]";
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

        public int getmGlobalAllocPercentage() {
            return mGlobalAllocPercentage;
        }

        public void setmGlobalAllocPercentage(int mGlobalAllocPercentage) {
            this.mGlobalAllocPercentage = mGlobalAllocPercentage;
        }

        public int getmLocalAllocPercentage() {
            return mLocalAllocPercentage;
        }

        public void setmLocalAllocPercentage(int mLocalAllocPercentage) {
            this.mLocalAllocPercentage = mLocalAllocPercentage;
        }

        public double getmCharge() {
            return mCharge;
        }

        public void setmCharge(double mCharge) {
            this.mCharge = mCharge;
        }
    }
}
