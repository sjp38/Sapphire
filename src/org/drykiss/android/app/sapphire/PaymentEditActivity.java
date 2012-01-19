
package org.drykiss.android.app.sapphire;

import java.util.Calendar;

import org.drykiss.android.app.sapphire.ad.AdvertisementManager;
import org.drykiss.android.app.sapphire.data.DataManager;
import org.drykiss.android.app.sapphire.data.Payment;
import org.drykiss.android.app.sapphire.util.TimeUtil;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

public class PaymentEditActivity extends Activity implements
        View.OnClickListener {
    private static final String TAG = "Sapphire_payment_edit_activity";

    private long mEventId;
    private int mPaymentPosition;
    private Payment mPayment;
    private Calendar mCalendar;
    private EditText mNameEditText;
    private EditText mCostEditText;
    private TextView mPaidDateTextView;
    private TextView mPaidTimeTextView;
    
    private View mAdView;

    private TimePickerDialog.OnTimeSetListener mPaidTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            mCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            mCalendar.set(Calendar.MINUTE, minute);
            mPaidTimeTextView.setText(TimeUtil.formatTimeToText(
                    PaymentEditActivity.this, mCalendar.getTimeInMillis(),
                    false));
        }
    };

    private DatePickerDialog.OnDateSetListener mPaidDateSetListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear,
                int dayOfMonth) {
            mCalendar.set(Calendar.YEAR, year);
            mCalendar.set(Calendar.MONTH, monthOfYear);
            mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            mPaidDateTextView
                    .setText(TimeUtil.formatTimeToText(
                            PaymentEditActivity.this,
                            mCalendar.getTimeInMillis(), true));
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.payment_edit);
        
        if (!DataManager.INSTANCE.isActive()) {
            DataManager.INSTANCE.setContext(getApplicationContext());
            DataManager.INSTANCE.loadDatas();
        }

        final Intent intent = getIntent();
        mEventId = intent.getLongExtra(EventDetailActivity.EXTRA_EVENT_ID, -1);
        mPaymentPosition = intent.getIntExtra(
                EventDetailActivity.EXTRA_PAYMENT_POSITION, -1);
        if (mEventId == -1) {
            Log.e(TAG, "Created with event id -1. Something wrong! finish.");
            finish();
        }
        mCalendar = Calendar.getInstance();
        if (mPaymentPosition == -1) {
            mPayment = new Payment(-1, mEventId, "",
                    mCalendar.getTimeInMillis(), 0);
        } else {
            mPayment = DataManager.INSTANCE.getPayment(mEventId,
                    mPaymentPosition);
            mCalendar.setTimeInMillis(mPayment.getmTime());
        }
        initViews();

        mAdView = AdvertisementManager.getAdvertisementView(this);
        LinearLayout adLayout = (LinearLayout) findViewById(R.id.advertiseLayout);
        adLayout.addView(mAdView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AdvertisementManager.destroyAd(mAdView);
    }

    private void initViews() {
        mNameEditText = (EditText) findViewById(R.id.payment_edit_nameEditText);
        mNameEditText.setText(mPayment.getmName());

        mCostEditText = (EditText) findViewById(R.id.payment_edit_costEditText);
        mCostEditText.setText(String.valueOf(mPayment.getmCost()));

        mPaidDateTextView = (TextView) findViewById(R.id.payment_edit_paidDateTextView);
        mPaidDateTextView.setText(TimeUtil.formatTimeToText(this,
                mPayment.getmTime(), true));
        mPaidDateTextView.setOnClickListener(this);

        mPaidTimeTextView = (TextView) findViewById(R.id.payment_edit_paidTimeTextView);
        mPaidTimeTextView.setText(TimeUtil.formatTimeToText(this,
                mPayment.getmTime(), false));
        mPaidTimeTextView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.payment_edit_paidDateTextView:
                new DatePickerDialog(this, mPaidDateSetListener, mCalendar.get(Calendar.YEAR),
                        mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH)).show();
                break;
            case R.id.payment_edit_paidTimeTextView:

                new TimePickerDialog(this, mPaidTimeSetListener,
                        mCalendar.get(Calendar.HOUR_OF_DAY), mCalendar.get(Calendar.MINUTE), false)
                        .show();
                break;
            default:
                break;
        }
    }

    public void onSaveButtonClicked(View v) {
        final String name = mNameEditText.getText().toString();
        final double cost = Double.valueOf(mCostEditText.getText().toString());
        final long time = mCalendar.getTimeInMillis();

        final Payment payment = new Payment(mPayment.getmId(), mEventId, name, time, cost);
        if (mPaymentPosition == -1) {
            DataManager.INSTANCE.addPayment(payment);
        } else {
            DataManager.INSTANCE.updatePayment(mPaymentPosition, payment);
        }
        finish();
    }

    public void onDiscardButtonClicked(View v) {
        finish();
    }

    public void onDeleteButtonClicked(View v) {
        DataManager.INSTANCE.removePayment(mEventId, mPaymentPosition);
        finish();
    }

}
