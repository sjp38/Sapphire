
package org.drykiss.android.app.sapphire;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.actionbarcompat.ActionBarActivity;

import org.drykiss.android.app.sapphire.ad.AdvertisementManager;
import org.drykiss.android.app.sapphire.data.ChargeCalcResult;
import org.drykiss.android.app.sapphire.data.ChargeCalcResult.Bill;
import org.drykiss.android.app.sapphire.data.DataManager;
import org.drykiss.android.app.sapphire.data.Event;
import org.drykiss.android.app.sapphire.data.Member;
import org.drykiss.android.app.sapphire.data.Payment;
import org.drykiss.android.app.sapphire.util.TimeUtil;

import java.util.ArrayList;
import java.util.Collections;

public class EventDetailActivity extends ActionBarActivity {
    private static final String TAG = "Sapphire_event_detail_activity";
    public static final String EXTRA_EVENT_ID = "org.drykiss.android.app.sapphire.EXTRA_EVENT_ID";
    public static final String EXTRA_PAYMENT_POSITION = "org.drykiss.android.app.sapphire.EXTRA_PAYMENT_POSITION";
    public static final String EXTRA_PAYMENT_ID = "org.drykiss.android.app.sapphire.EXTRA_PAYMENT_ID";
    public static final String EXTRA_MEMBER_POSITION = "org.drykiss.android.app.sapphire.EXTRA_MEMBER_POSITION";

    private static final int PICK_CONTACT_REQUEST = 1;
    private static final int EVENT_DETAIL_REQUEST = 2;

    private int mEventPosition = -1;
    private Event mEvent;
    private boolean mWaitEventLoading = false;

    private View mAdView;

    private View.OnClickListener mAddPaymentButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(EventDetailActivity.this, PaymentEditActivity.class);
            intent.putExtra(EXTRA_EVENT_ID, mEvent.getmId());
            startActivity(intent);
        }
    };

    private View.OnClickListener mAddMemberButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            addMember(-1);
        }
    };

    private View.OnClickListener mAddExceptionalRuleListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final long paymentId = (Long) v.getTag();
            addMember(paymentId);
        }
    };

    private View.OnClickListener mMemberClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MemberDataHolder holder = (MemberDataHolder) v.getTag();
            Intent intent = new Intent(EventDetailActivity.this, MemberEditActivity.class);
            intent.putExtra(EXTRA_EVENT_ID, mEvent.getmId());
            intent.putExtra(EXTRA_PAYMENT_ID, holder.mPaymentId);
            intent.putExtra(EXTRA_MEMBER_POSITION, holder.mPosition);
            startActivity(intent);
        }
    };

    private View.OnClickListener mPaymentClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final int position = (Integer) v.getTag();
            Intent intent = new Intent(EventDetailActivity.this, PaymentEditActivity.class);
            intent.putExtra(EXTRA_EVENT_ID, mEvent.getmId());
            intent.putExtra(EXTRA_PAYMENT_POSITION, position);
            startActivity(intent);
        }
    };

    private DataManager.OnDataChangedListener mEventsChangedListener = new DataManager.OnDataChangedListener() {
        @Override
        public void onDataChanged() {
            if (DataManager.INSTANCE.getEvent(mEventPosition) != mEvent) {
                initDataAndViews();
            }
            mWaitEventLoading = false;
        }
    };

    private DataManager.OnDataChangedListener mPaymentsChangedListener = new DataManager.OnDataChangedListener() {
        @Override
        public void onDataChanged() {
            if (mWaitEventLoading) {
                return;
            }
            addPaymentViews();
            final TextView calculateResult = (TextView) findViewById(R.id.event_detail_calculateTextView);
            calculateResult.setText(formatChargeResult(false));
        }
    };

    private DataManager.OnDataChangedListener mMembersChangedListener = new DataManager.OnDataChangedListener() {
        @Override
        public void onDataChanged() {
            if (mWaitEventLoading) {
                return;
            }
            addPaymentViews();
            addMemberViews(mEvent.getmId(), -1, null);
            final TextView calculateResult = (TextView) findViewById(R.id.event_detail_calculateTextView);
            calculateResult.setText(formatChargeResult(false));
        }
    };

    private View.OnClickListener mSendNoticeButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(EventDetailActivity.this);
            final String account = getString(R.string.account,
                    prefs.getString(SettingsActivity.KEY_BANK_ACCOUNT, ""));
            final String billFormat = prefs.getString(SettingsActivity.KEY_BILL_FORMAT,
                    getString(R.string.default_bill_format));
            final String message = account + "\n" + formatChargeResult(!billFormat
                    .equals(R.string.default_bill_format));

            final int membersCount = DataManager.INSTANCE.getMemberCount(mEvent.getmId(), -1);
            final ArrayList<Member> emailMembers = new ArrayList<Member>();
            final ArrayList<Member> phoneNumberMembers = new ArrayList<Member>();

            final ArrayList<String> phoneNumbers = new ArrayList<String>();
            final ArrayList<String> emails = new ArrayList<String>();
            for (int i = 0; i < membersCount; i++) {
                Member member = DataManager.INSTANCE.getMember(mEvent.getmId(), -1, i);
                final String address = member.getmAddress();
                if (isEmailAddress(address)) {
                    emailMembers.add(member);
                    emails.add(address);
                } else {
                    phoneNumberMembers.add(member);
                    phoneNumbers.add(address);
                }
            }
            if (emails.size() > 0 && phoneNumbers.size() > 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(EventDetailActivity.this);
                builder.setTitle(R.string.dialog_notice_to_title);
                CharSequence[] selection = getResources().getTextArray(R.array.notice_to);
                builder.setItems(selection, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                sendNotice(message, emails, emailMembers, true);
                                break;
                            case 1:
                                sendNotice(message, phoneNumbers, phoneNumberMembers, false);
                                break;
                            default:
                                break;
                        }
                    }
                });
                builder.show();

            } else if (phoneNumbers.size() <= 0) {
                sendNotice(message, emails, emailMembers, true);
            } else {
                sendNotice(message, phoneNumbers, phoneNumberMembers, false);
            }

            if (phoneNumbers.size() <= 0 || emails.size() <= 0) {
                if (mEvent.getmState() == Event.State.IDLE) {
                    Event newEvent = new Event(mEvent);
                    newEvent.setmState(Event.State.NOTICED);
                    DataManager.INSTANCE.updateEvent(mEventPosition, newEvent);
                    mEvent = newEvent;
                } else {
                    int idleStateCount = 0;
                    for (Member member : emailMembers) {
                        if (member.getmPaymentState() == Member.PaymentState.IDLE) {
                            idleStateCount++;
                        }
                    }
                    for (Member member : phoneNumberMembers) {
                        if (member.getmPaymentState() == Member.PaymentState.IDLE) {
                            idleStateCount++;
                        }
                    }
                    if (idleStateCount == 0 && mEvent.getmState() == Event.State.IDLE) {
                        Event newEvent = new Event(mEvent);
                        newEvent.setmState(Event.State.NOTICED);
                        DataManager.INSTANCE.updateEvent(mEventPosition, newEvent);
                        mEvent = newEvent;
                    }
                }
            }
        }
    };

    private void addMember(final long paymentId) {
        if (paymentId == -1) {
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            startActivityForResult(intent, PICK_CONTACT_REQUEST);
            return;
        }
        final int membersCount = DataManager.INSTANCE.getMemberCount(mEvent.getmId(), -1);
        if (membersCount <= 0) {
            Toast.makeText(this, R.string.warning_member_should_exist, Toast.LENGTH_SHORT).show();
            return;
        }
        CharSequence[] members = new CharSequence[membersCount];

        final long eventId = mEvent.getmId();
        for (int i = 0; i < membersCount; i++) {
            members[i] = DataManager.INSTANCE.getMember(eventId, -1, i).getmName();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_select_exceptional_member);
        builder.setItems(members, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final Member originalMember = DataManager.INSTANCE.getMember(eventId, -1, which);
                final Member newMember = new Member(originalMember);
                newMember.setmParentPaymentId(paymentId);
                DataManager.INSTANCE.addMember(newMember);
                addPaymentViews();
            }
        });
        builder.show();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.event_detail);

        final Intent intent = getIntent();
        mEventPosition = intent.getIntExtra(EventsListActivity.EXTRA_EVENT_POSITION, -1);
        if (mEventPosition == -1) {
            Log.e(TAG, "Wrong event position! finish!");
            finish();
        }
        setAddButtonsListener();

        if (!DataManager.INSTANCE.isActive()) {
            DataManager.INSTANCE.setContext(getApplicationContext());
            DataManager.INSTANCE.registerEventChangedListener(mEventsChangedListener);
            mWaitEventLoading = true;
            DataManager.INSTANCE.loadDatas();
        }

        DataManager.INSTANCE.registerPaymentsChangedListener(mPaymentsChangedListener);
        DataManager.INSTANCE.registerMembersChangedListener(mMembersChangedListener);

        mAdView = AdvertisementManager.getAdvertisementView(this);
        LinearLayout adLayout = (LinearLayout) findViewById(R.id.advertiseLayout);
        adLayout.addView(mAdView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AdvertisementManager.destroyAd(mAdView);

        DataManager.INSTANCE.unregisterPaymentsChangedListener(mEventsChangedListener);
        DataManager.INSTANCE.unregisterPaymentsChangedListener(mPaymentsChangedListener);
        DataManager.INSTANCE.unregisterMembersChangedListener(mMembersChangedListener);
    }

    private void initDataAndViews() {
        mEvent = DataManager.INSTANCE.getEvent(mEventPosition);
        if (mEvent == null) {
            Log.d(TAG, "Init with null event. Maybe event removed in background. finish.");
            finish();
            return;
        }
        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mWaitEventLoading) {
            initDataAndViews();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == EVENT_DETAIL_REQUEST && resultCode == RESULT_OK) {
            if (data.getBooleanExtra(EventEditActivity.EXTRA_EVENT_DELETED, false)) {
                finish();
            }
        } else if (requestCode == PICK_CONTACT_REQUEST) {
            if (resultCode != RESULT_OK) {
                return;
            }
            final Cursor cursor = (managedQuery(data.getData(), null, null, null, null));
            while (cursor.moveToNext()) {
                final String name = cursor.getString(cursor
                        .getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                final long contactId = cursor.getLong(cursor
                        .getColumnIndex(ContactsContract.Contacts._ID));
                final String lookupId = cursor.getString(cursor
                        .getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
                final long photoId = cursor.getLong(cursor
                        .getColumnIndex(ContactsContract.Contacts.PHOTO_ID));

                final ArrayList<CharSequence> addressList = new ArrayList<CharSequence>();

                appendEmailAddress(this, contactId, addressList);

                final int hasPhone = cursor.getInt(cursor
                        .getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
                if (hasPhone == 1) {
                    appendPhoneNumbers(this, contactId, addressList);
                }
                if (addressList.size() <= 0) {
                    Toast.makeText(this, R.string.warning_no_address, Toast.LENGTH_SHORT).show();
                    continue;
                }

                CharSequence[] addressArray = new CharSequence[addressList.size()];
                addressArray = addressList.toArray(addressArray);

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.dialog_select_address_to_send_notice_title,
                        name));
                builder.setItems(addressArray, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Member member = new Member(-1, mEvent.getmId(), -1,
                                contactId, lookupId, photoId, name, addressList.get(which)
                                        .toString(),
                                Member.PaymentState.IDLE, -1, "", 100, "");
                        DataManager.INSTANCE.addMember(member);

                        addMemberViews(mEvent.getmId(), -1, null);
                    }
                });
                builder.show();
            }
        }
    }

    public static void appendPhoneNumbers(final Context context, final long contactId,
            final ArrayList<CharSequence> addressList) {
        final Cursor phones = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId,
                null, null);
        if (phones != null) {
            while (phones.moveToNext()) {
                addressList.add(phones.getString(phones
                        .getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
            }
            phones.close();
        }
    }

    public static void appendEmailAddress(final Context context, final long contactId,
            final ArrayList<CharSequence> addressList) {
        final Cursor emails = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI, null,
                ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = " + contactId, null,
                null);
        if (emails != null) {
            while (emails.moveToNext()) {
                addressList.add(emails.getString(emails
                        .getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)));
            }
            emails.close();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.event_detail_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_action_edit:
                Intent intent = new Intent(this, EventEditActivity.class);
                intent.putExtra(EventsListActivity.EXTRA_EVENT_POSITION, mEventPosition);
                startActivityForResult(intent, EVENT_DETAIL_REQUEST);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setAddButtonsListener() {
        final ImageButton addPaymentButton = (ImageButton) findViewById(R.id.event_detail_addPaymentImageButton);
        addPaymentButton.setOnClickListener(mAddPaymentButtonClickListener);

        final ImageButton addMemberButton = (ImageButton) findViewById(R.id.event_detail_addMemberImageButton);
        addMemberButton.setOnClickListener(mAddMemberButtonClickListener);
    }

    private void initViews() {
        final TextView nameTextView = (TextView) findViewById(R.id.event_detail_nameTextView);
        nameTextView.setText(mEvent.getmName());

        final CharSequence[] stateTextArray = getResources().getTextArray(
                R.array.event_states);
        final TextView stateTextView = (TextView) findViewById(R.id.event_detail_stateTextView);
        stateTextView.setText(stateTextArray[mEvent.getmState().ordinal()]);

        final TextView fromDateTextView = (TextView) findViewById(R.id.event_detail_fromDateTextView);
        fromDateTextView.setText(TimeUtil.formatTimeToText(this, mEvent.getmStartTime(), true));

        final TextView fromTimeTextView = (TextView) findViewById(R.id.event_detail_fromTimeTextView);
        fromTimeTextView.setText(TimeUtil.formatTimeToText(this, mEvent.getmStartTime(), false));

        final TextView toDateTextView = (TextView) findViewById(R.id.event_detail_toDateTextView);
        toDateTextView.setText(TimeUtil.formatTimeToText(this, mEvent.getmEndTime(), true));

        final TextView toTimeTextView = (TextView) findViewById(R.id.event_detail_toTimeTextView);
        toTimeTextView.setText(TimeUtil.formatTimeToText(this, mEvent.getmEndTime(), false));

        TextView calculateResult = (TextView) findViewById(R.id.event_detail_calculateTextView);
        calculateResult.setText(formatChargeResult(false));

        final View sendNoticeButton = findViewById(R.id.event_detail_sendNoticeButton);
        sendNoticeButton.setOnClickListener(mSendNoticeButtonListener);

        addPaymentViews();
        addMemberViews(mEvent.getmId(), -1, null);
    }

    private void sendNotice(String message, ArrayList<String> addressList,
            ArrayList<Member> members,
            boolean isEmail) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, message);
        if (isEmail) {
            String[] emailAddressArray = new String[addressList.size()];
            emailAddressArray = addressList.toArray(emailAddressArray);
            intent.putExtra(android.content.Intent.EXTRA_EMAIL, emailAddressArray);
        } else {
            String address = "";
            for (String phoneNumber : addressList) {
                address += phoneNumber + "; ";
            }
            intent.putExtra("address", address);
            intent.putExtra("exit_on_sent", true);
        }
        for (Member member : members) {
            Member newMember = new Member(member);
            newMember.setmPaymentState(Member.PaymentState.NOTICED);
            DataManager.INSTANCE.updateMember(newMember);
        }
        startActivity(intent);
    }

    private static boolean isEmailAddress(String address) {
        return address.contains("@");
    }

    private String formatChargeResult(boolean onlyTotal) {
        ArrayList<ChargeCalcResult> results = DataManager.INSTANCE.calculate(mEvent
                .getmId());
        if (results == null) {
            return "";
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String currencyUnit = prefs.getString(SettingsActivity.KEY_CURRENCY_UNIT,
                getString(R.string.currency_unit_default));
        final int decimalSpaces = Integer.valueOf(prefs.getString(
                SettingsActivity.KEY_DECIMAL_SPACES_FOR_MONEY,
                getString(R.string.default_decimal_spaces_for_money)));
        String resultText = "";
        final int resultsCount = results.size();
        int position = onlyTotal ? resultsCount - 1 : 0;
        for (; position < resultsCount; position++) {
            ChargeCalcResult result = results.get(position);
            ArrayList<Bill> bills = result.getmBills();
            Collections.sort(bills);
            resultText += result.getmPaymentName() + " : "
                    + getDisplayableCost(result.getmPaymentCost(), decimalSpaces)
                    + currencyUnit + "\n";
            for (Bill bill : bills) {
                resultText += bill.getmName() + " : "
                        + getDisplayableCost(bill.getmCharge(), decimalSpaces)
                        + currencyUnit + "\n";
            }
            resultText += "\n";
        }
        return resultText;
    }

    private String getDisplayableCost(double cost, int decimalSpaces) {
        cost = cost * Math.pow(10, decimalSpaces);
        cost = Math.ceil(cost);
        cost = cost / Math.pow(10, decimalSpaces);
        if (decimalSpaces < 0) {
            decimalSpaces = 0;
        }
        final String moneyFormat = "%." + decimalSpaces + "f";
        return String.format(moneyFormat, cost);
    }

    private void addPaymentViews() {
        final int paymentCount = DataManager.INSTANCE.getPaymentsCount(mEvent.getmId());
        if (paymentCount <= 0) {
            return;
        }
        final long eventId = mEvent.getmId();
        final LinearLayout paymentsLayout = (LinearLayout) findViewById(R.id.event_detail_paymentsLayout);
        paymentsLayout.removeAllViews();
        for (int i = 0; i < paymentCount; i++) {
            final Payment payment = DataManager.INSTANCE.getPayment(eventId, i);

            final View paymentView = getLayoutInflater()
                    .inflate(R.layout.payment_item, null, false);

            final TextView nameTextView = (TextView) paymentView
                    .findViewById(R.id.payment_item_nameTextView);
            nameTextView.setText(payment.getmName());

            final TextView costTextView = (TextView) paymentView
                    .findViewById(R.id.payment_item_costTextView);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            final String currencyUnit = prefs.getString(SettingsActivity.KEY_CURRENCY_UNIT,
                    getString(R.string.currency_unit_default));
            final int decimalSpaces = Integer.valueOf(prefs.getString(
                    SettingsActivity.KEY_DECIMAL_SPACES_FOR_MONEY,
                    getString(R.string.default_decimal_spaces_for_money)));
            costTextView.setText(getString(R.string.cost_format,
                    getDisplayableCost(payment.getmCost(), decimalSpaces), currencyUnit));

            final TextView dateTextView = (TextView) paymentView
                    .findViewById(R.id.payment_item_dateTextView);
            dateTextView.setText(TimeUtil.formatTimeToText(getApplicationContext(),
                    payment.getmTime(), true));

            final TextView timeTextView = (TextView) paymentView
                    .findViewById(R.id.payment_item_timeTextView);
            timeTextView.setText(TimeUtil.formatTimeToText(getApplicationContext(),
                    payment.getmTime(), false));
            if (DataManager.INSTANCE.getMemberCount(eventId, payment.getmId()) > 0) {
                final LinearLayout layout = (LinearLayout) paymentView
                        .findViewById(R.id.payment_item_exceptionalPaymentRulesLayout);
                addMemberViews(eventId, payment.getmId(), layout);
            }

            final ImageButton exceptionalRules = (ImageButton) paymentView
                    .findViewById(R.id.payment_item_addExceptionalPaymentRuleImageButton);
            exceptionalRules.setTag(Long.valueOf(payment.getmId()));
            exceptionalRules.setOnClickListener(mAddExceptionalRuleListener);

            final LinearLayout summaryLayout = (LinearLayout) paymentView
                    .findViewById(R.id.payment_item_summaryLinearLayout);
            summaryLayout.setTag(Integer.valueOf(i));
            summaryLayout.setOnClickListener(mPaymentClickListener);

            paymentsLayout.addView(paymentView);
        }
    }

    private void addMemberViews(final long eventId, final long paymentId, LinearLayout parentLayout) {
        final int membersCount = DataManager.INSTANCE.getMemberCount(eventId, paymentId);
        if (membersCount <= 0) {
            return;
        }
        if (parentLayout == null) {
            parentLayout = (LinearLayout) findViewById(R.id.event_detail_membersLayout);
        }
        parentLayout.removeAllViews();
        for (int i = 0; i < membersCount; i++) {
            final Member member = DataManager.INSTANCE.getMember(eventId, paymentId, i);
            final View memberView = getLayoutInflater().inflate(R.layout.member_item, null, false);

            final TextView nameTextView = (TextView) memberView
                    .findViewById(R.id.member_item_nameTextView);
            nameTextView.setText(member.getmName());

            final TextView allocRateTextView = (TextView) memberView
                    .findViewById(R.id.member_item_allocRatioTextView);
            String allocPercentage = member.getmAllocPercentage()
                    + getString(R.string.symbol_percent);
            allocRateTextView.setText(getString(R.string.member_item_alloc_ratio,
                    allocPercentage, member.getmAllocReason()));

            final CharSequence[] stateTextArray = getResources().getTextArray(
                    R.array.payment_states);

            final TextView stateTextView = (TextView) memberView
                    .findViewById(R.id.member_item_stateTextView);
            if (paymentId != -1) {
                stateTextView.setVisibility(View.GONE);
            } else {
                stateTextView.setText(stateTextArray[member.getmPaymentState().ordinal()]);
            }

            final QuickContactBadge quickContact = (QuickContactBadge) memberView
                    .findViewById(R.id.member_item_quickContactBadge);
            quickContact.assignContactUri(Contacts.getLookupUri(member.getmContactId(),
                    member.getmContactLookupId()));

            MemberDataHolder holder = new MemberDataHolder(paymentId, i);
            memberView.setTag(holder);
            memberView.setOnClickListener(mMemberClickListener);

            parentLayout.addView(memberView);
        }
    }

    private class MemberDataHolder {
        private MemberDataHolder(long paymentId, int position) {
            mPaymentId = paymentId;
            mPosition = position;
        }

        long mPaymentId;
        int mPosition;
    }
}
