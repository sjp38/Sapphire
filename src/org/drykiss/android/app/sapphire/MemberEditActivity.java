
package org.drykiss.android.app.sapphire;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.QuickContactBadge;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import org.drykiss.android.app.sapphire.ad.AdvertisementManager;
import org.drykiss.android.app.sapphire.data.DataManager;
import org.drykiss.android.app.sapphire.data.Member;
import org.drykiss.android.app.sapphire.util.TimeUtil;

import java.util.ArrayList;
import java.util.Calendar;

public class MemberEditActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "Sapphire_member_edit_activity";

    private Member mMember = null;
    private long mEventId = -1;
    private long mPaymentId = -1;
    private int mMemberPosition = -1;

    private RadioGroup mAddressRadioGroup = null;
    private ArrayList<CharSequence> mAddressList = new ArrayList<CharSequence>();
    private EditText mAllocPercentageEditText = null;
    private EditText mAllocReasonEditText = null;
    private Spinner mStateSpinner = null;
    private View mPaidTimeLayout = null;
    private TextView mPaidDateTextView = null;
    private TextView mPaidTimeTextView = null;
    private Calendar mCalendar = null;
    private boolean mWaitMemberLoading = false;

    private View mAdView;

    private TimePickerDialog.OnTimeSetListener mPaidTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            mCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            mCalendar.set(Calendar.MINUTE, minute);
            mPaidTimeTextView.setText(TimeUtil.formatTimeToText(
                    MemberEditActivity.this, mCalendar.getTimeInMillis(),
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
            mPaidDateTextView.setText(TimeUtil.formatTimeToText(MemberEditActivity.this,
                    mCalendar.getTimeInMillis(), true));
        }
    };

    private DataManager.OnDataChangedListener mMembersChangedListener = new DataManager.OnDataChangedListener() {
        @Override
        public void onDataChanged() {
            mWaitMemberLoading = false;
            if (DataManager.INSTANCE.getMember(mEventId, mPaymentId, mMemberPosition) != mMember) {
                initDataAndViews();
            }
        }
    };

    private void initDataAndViews() {
        mMember = DataManager.INSTANCE.getMember(mEventId, mPaymentId, mMemberPosition);
        if (mMember == null) {
            Log.d(TAG, "Init with null member. Maybe removed in background. finish.");
            finish();
            return;
        }
        final long paidTime = mMember.getmPaidTime();
        mCalendar = Calendar.getInstance();

        if (mMember.getmPaymentState() == Member.PaymentState.COMPLETE && paidTime > 0) {
            mCalendar.setTimeInMillis(paidTime);
        }

        initViews();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.member_edit);

        final Intent intent = getIntent();
        mEventId = intent.getLongExtra(EventDetailActivity.EXTRA_EVENT_ID, -1);
        mPaymentId = intent.getLongExtra(EventDetailActivity.EXTRA_PAYMENT_ID,
                -1);
        mMemberPosition = intent.getIntExtra(
                EventDetailActivity.EXTRA_MEMBER_POSITION, -1);
        if (mMemberPosition == -1 || mEventId == -1) {
            Log.e(TAG, "Created with event id " + mEventId + ", position "
                    + mMemberPosition + ". Something wrong! finish.");
            finish();
        }

        if (!DataManager.INSTANCE.isActive()) {
            DataManager.INSTANCE.setContext(getApplicationContext());
            DataManager.INSTANCE.registerMembersChangedListener(mMembersChangedListener);
            mWaitMemberLoading = true;
            DataManager.INSTANCE.loadDatas();
        }

        if (!mWaitMemberLoading) {
            initDataAndViews();
        }

        mAdView = AdvertisementManager.getAdvertisementView(this);
        LinearLayout adLayout = (LinearLayout) findViewById(R.id.advertiseLayout);
        adLayout.addView(mAdView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AdvertisementManager.destroyAd(mAdView);

        DataManager.INSTANCE.unregisterMembersChangedListener(mMembersChangedListener);
    }

    private void initViews() {
        final QuickContactBadge quickContact = (QuickContactBadge) findViewById(R.id.member_edit_quickContactBadge);
        quickContact.assignContactUri(Contacts.getLookupUri(mMember.getmContactId(),
                mMember.getmContactLookupId()));

        final TextView nameTextView = (TextView) findViewById(R.id.member_edit_name);
        nameTextView.setText(mMember.getmName());

        mAllocPercentageEditText = (EditText) findViewById(R.id.member_edit_allocPercentageEditText);
        mAllocPercentageEditText.setText(String.valueOf(mMember.getmAllocPercentage()));

        mAllocReasonEditText = (EditText) findViewById(R.id.member_edit_allocReasonEditText);
        mAllocReasonEditText.setText(mMember.getmAllocReason());

        if (mPaymentId != -1) {
            final LinearLayout forEventMemberLayout = (LinearLayout) findViewById(R.id.member_edit_forEventMemberLayout);
            forEventMemberLayout.setVisibility(View.GONE);
            return;
        }
        mAddressRadioGroup = (RadioGroup) findViewById(R.id.member_edit_sendNoticeToRadioGroup);

        EventDetailActivity.appendEmailAddress(this, mMember.getmContactId(), mAddressList);
        EventDetailActivity.appendPhoneNumbers(this, mMember.getmContactId(), mAddressList);
        int addressCount = mAddressList.size();
        int position;

        for (position = 0; position < addressCount; position++) {
            final RadioButton radioButton = new RadioButton(this);
            radioButton.setId(position);
            radioButton.setText(mAddressList.get(position));
            LinearLayout.LayoutParams layoutParams = new RadioGroup.LayoutParams(
                    RadioGroup.LayoutParams.WRAP_CONTENT, RadioGroup.LayoutParams.WRAP_CONTENT);
            mAddressRadioGroup.addView(radioButton, -1, layoutParams);
            if (mMember.getmAddress().equals(mAddressList.get(position))) {
                mAddressRadioGroup.check(position);
            }
        }

        mStateSpinner = (Spinner) findViewById(R.id.member_edit_stateSpinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.payment_states, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mStateSpinner.setAdapter(adapter);
        mStateSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (Member.PaymentState.values()[position] == Member.PaymentState.COMPLETE) {
                    mPaidTimeLayout.setVisibility(View.VISIBLE);
                } else {
                    mPaidTimeLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }

        });
        mStateSpinner.setSelection(mMember.getmPaymentState().ordinal());

        mPaidTimeLayout = findViewById(R.id.member_edit_paid_timeLayout);
        mPaidDateTextView = (TextView) findViewById(R.id.member_edit_paidDateTextView);
        mPaidTimeTextView = (TextView) findViewById(R.id.member_edit_paidTimeTextView);
        mPaidDateTextView.setOnClickListener(this);
        mPaidTimeTextView.setOnClickListener(this);

        mPaidDateTextView.setText(TimeUtil.formatTimeToText(this, mCalendar.getTimeInMillis(),
                true));
        mPaidTimeTextView.setText(TimeUtil.formatTimeToText(this, mCalendar.getTimeInMillis(),
                false));

        final View confirmMsgLayout = findViewById(R.id.member_edit_confirmedBySmsLayout);
        if (TextUtils.isEmpty(mMember.getmPaymentConfirmMessage())) {
            confirmMsgLayout.setVisibility(View.GONE);
        } else {
            final TextView confirmMsgTextView = (TextView) findViewById(R.id.member_edit_paid_confirm_messageTextView);
            confirmMsgTextView.setText(mMember.getmPaymentConfirmMessage());
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.member_edit_paidDateTextView:
                new DatePickerDialog(this, mPaidDateSetListener, mCalendar.get(Calendar.YEAR),
                        mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH)).show();
                break;
            case R.id.member_edit_paidTimeTextView:
                new TimePickerDialog(this, mPaidTimeSetListener,
                        mCalendar.get(Calendar.HOUR_OF_DAY), mCalendar.get(Calendar.MINUTE), false)
                        .show();
                break;
            default:
                break;
        }
    }

    public void onSaveButtonClicked(View v) {
        final Member newMember = new Member(mMember);
        newMember.setmAllocPercentage(Integer
                .valueOf(mAllocPercentageEditText.getText().toString()));
        newMember.setmAllocReason(mAllocReasonEditText.getText().toString());
        if (mPaymentId == -1) {
            newMember.setmAddress(mAddressList.get(mAddressRadioGroup.getCheckedRadioButtonId())
                    .toString());
            final Member.PaymentState state = Member.PaymentState.values()[mStateSpinner
                    .getSelectedItemPosition()];
            newMember.setmPaymentState(state);
            if (state != Member.PaymentState.COMPLETE) {
                newMember.setmPaymentConfirmMessage("");
            }
            newMember.setmPaidTime(mCalendar.getTimeInMillis());
        }
        DataManager.INSTANCE.updateMember(mMemberPosition, newMember);
        finish();
    }

    public void onDiscardButtonClicked(View v) {
        finish();
    }

    public void onDeleteButtonClicked(View v) {
        DataManager.INSTANCE.removeMember(mEventId, mPaymentId, mMemberPosition);
        finish();
    }
}
