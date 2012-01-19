
package org.drykiss.android.app.sapphire;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;

import org.drykiss.android.app.sapphire.data.DataManager;
import org.drykiss.android.app.sapphire.data.Event;
import org.drykiss.android.app.sapphire.data.Member;
import org.drykiss.android.app.sapphire.data.Payment;
import org.drykiss.android.app.sapphire.util.TimeUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class MessageParserService extends Service implements DataManager.OnDataChangedListener {
    private static final boolean DEBUG = false;
    private static final String TAG = "Sapphire_message_parser_service";
    /**
     * Phone number seperators that can be element of number.
     */
    public static final String[] NUMBER_SEPERATORS = {
            "-", " ", "(", ")"
    };

    private boolean mOn = false;
    private String mOutgoNumber;
    private String mIncomeNumber;
    private String mOutgoKeyword;
    private String mIncomeKeyword;
    private String mCurrencyUnit;
    private Object[] mPdus;
    private int mOutgoCostTokenIndex = -1;
    private int mIncomeCostTokenIndex = -1;
    private ArrayList<Integer> mRepeatingOutgoTokens = new ArrayList<Integer>();
    private ArrayList<String> mRecentOutgoSMSs = new ArrayList<String>();
    private ArrayList<String> mRecentIncomeSMSs = new ArrayList<String>();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mOn = prefs.getBoolean(SettingsActivity.KEY_SMS_PARSER_SERVICE_ON, false);
        mOutgoNumber = prefs.getString(SettingsActivity.KEY_OUTGO_PHONE_NUMBER, "");
        mIncomeNumber = prefs.getString(SettingsActivity.KEY_INCOME_PHONE_NUMBER, "");
        mOutgoKeyword = prefs.getString(SettingsActivity.KEY_OUTGO_KEYWORD, "");
        mIncomeKeyword = prefs.getString(SettingsActivity.KEY_INCOME_KEYWORD, "");
        mCurrencyUnit = prefs.getString(SettingsActivity.KEY_CURRENCY_UNIT,
                getString(R.string.currency_unit_default));

        prefs.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                if (SettingsActivity.KEY_SMS_PARSER_SERVICE_ON.equals(key)) {
                    mOn = prefs.getBoolean(key, false);
                } else if (SettingsActivity.KEY_OUTGO_PHONE_NUMBER.equals(key)) {
                    mOutgoNumber = prefs.getString(key, "");
                } else if (SettingsActivity.KEY_INCOME_PHONE_NUMBER.equals(key)) {
                    mIncomeNumber = prefs.getString(key, "");
                } else if (SettingsActivity.KEY_OUTGO_KEYWORD.equals(key)) {
                    mOutgoKeyword = prefs.getString(key, "");
                } else if (SettingsActivity.KEY_INCOME_KEYWORD.equals(key)) {
                    mIncomeKeyword = prefs.getString(key, "");
                } else if (SettingsActivity.KEY_CURRENCY_UNIT.equals(key)) {
                    mOutgoNumber = prefs.getString(key, getString(R.string.currency_unit_default));
                }
            }
        });
        parseRecentSMSs();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!mOn) {
            return START_STICKY_COMPATIBILITY;
        }
        Bundle data = intent.getExtras();
        if (data == null) {
            return START_STICKY_COMPATIBILITY;
        }
        mPdus = (Object[]) data.get("pdus");

        if (!DataManager.INSTANCE.isActive()) {
            DataManager.INSTANCE.setContext(getApplicationContext());
            DataManager.INSTANCE.loadDatas();
            DataManager.INSTANCE.setEventChangedListener(this);
        } else {
            parseMessage();
        }
        return START_STICKY_COMPATIBILITY;
    }

    @Override
    public void onDataChanged() {
        parseMessage();
    }

    private void parseMessage() {
        for (Object pdu : mPdus) {
            SmsMessage msg = SmsMessage.createFromPdu((byte[]) pdu);

            if (DEBUG) {
                android.util.Log.d(TAG,
                        "DisplayOriginatingAddress : " + msg.getDisplayOriginatingAddress());
                android.util.Log.d(TAG,
                        "DisplayMessageBody : " + msg.getDisplayMessageBody());
                android.util.Log.d(TAG,
                        "TimestampMillis : " + TimeUtil.formatTimeToText(getApplicationContext(),
                                msg.getTimestampMillis(), false));
            }

            final String originatingAddress = msg.getDisplayOriginatingAddress();
            if (originatingAddress.equals(parseNumber(mOutgoNumber))) {
                parseOutgo(msg.getDisplayMessageBody(), msg.getTimestampMillis());
                addRecentSMSAndParseIfNeed(mRecentOutgoSMSs, msg.getDisplayMessageBody());

            } else if (originatingAddress.equals(parseNumber(mIncomeNumber))) {
                parseIncome(msg.getDisplayMessageBody(), msg.getTimestampMillis());
                addRecentSMSAndParseIfNeed(mRecentIncomeSMSs, msg.getDisplayMessageBody());
            }
        }
    }

    private void addRecentSMSAndParseIfNeed(ArrayList<String> recentSMSs, String msg) {
        recentSMSs.add(msg);
        if (recentSMSs.size() >= 2 * RECENT_SMS_LIMIT) {
            final int diff = recentSMSs.size() - RECENT_SMS_LIMIT;
            for (int i = 0; i < diff; i++) {
                recentSMSs.remove(0);
            }
            parseRecentSMSs();
        }
    }

    private static String parseNumber(String number) {
        for (String seperator : NUMBER_SEPERATORS) {
            number = number.replace(seperator, "");
        }
        return number;
    }

    private static final int RECENT_SMS_LIMIT = 10;

    private void getRecentSMSs(ArrayList<String> buffer, String number, String keyword) {
        if (buffer.size() < RECENT_SMS_LIMIT) {
            number = parseNumber(number);
            if (TextUtils.isEmpty(number)) {
                return;
            }
            final Uri uri = Uri.parse("content://sms");
            final Cursor cursor = getContentResolver().query(uri, null,
                    "address = " + number, null, "date");
            while (cursor.moveToNext()) {
                String message = cursor.getString(cursor.getColumnIndex("body"));
                if (message.contains(keyword)) {
                    buffer.add(message);
                    if (buffer.size() >= RECENT_SMS_LIMIT) {
                        break;
                    }
                }
            }
            cursor.close();
        }
    }

    private int getEstimatedCostTokenIndex(ArrayList<String> msgs) {
        final HashMap<Integer, Integer> costTokenVoteMap = new HashMap<Integer, Integer>();

        final int msgsCount = msgs.size();
        for (int i = 0; i < msgsCount; i++) {
            String msg = msgs.get(i);
            final int costTokenIndex = getCostTokenIndex(msg);
            Integer previousScore = costTokenVoteMap.get(costTokenIndex);
            if (previousScore == null) {
                previousScore = 0;
            }
            costTokenVoteMap.put(costTokenIndex, ++previousScore);
        }
        Set<Integer> costKeySet = costTokenVoteMap.keySet();
        int lastScore = 0;
        int candidate = -1;
        for (Integer key : costKeySet) {
            final int score = costTokenVoteMap.get(key);
            if (score > lastScore) {
                candidate = key;
            }
        }
        return candidate;
    }

    private void getRepeatingOutgoTokens() {
        ArrayList<String> msgs = mRecentOutgoSMSs;

        final HashMap<Integer, Integer> repeatTokenVoteMap = new HashMap<Integer, Integer>();

        final int msgsCount = msgs.size();
        for (int i = 0; i < msgsCount; i++) {
            String msg = msgs.get(i);
            if (i > 0) {
                compareMessages(msg, msgs.get(i - 1), repeatTokenVoteMap);
            }
        }
        Set<Integer> repeatKeySet = repeatTokenVoteMap.keySet();
        for (Integer key : repeatKeySet) {
            final int score = repeatTokenVoteMap.get(key);
            if (score >= msgsCount - 1) {
                mRepeatingOutgoTokens.add(key);
            }
        }
    }

    private void parseRecentSMSs() {
        if (!mOn) {
            return;
        }
        getRecentSMSs(mRecentOutgoSMSs, mOutgoNumber, mOutgoKeyword);
        getRepeatingOutgoTokens();
        mOutgoCostTokenIndex = getEstimatedCostTokenIndex(mRecentOutgoSMSs);

        getRecentSMSs(mRecentIncomeSMSs, mIncomeNumber, mIncomeKeyword);
        mIncomeCostTokenIndex = getEstimatedCostTokenIndex(mRecentIncomeSMSs);
    }

    private void compareMessages(String one, String two, HashMap<Integer, Integer> voteMap) {
        final String[] oneTokens = tokenizeMsg(one);
        final String[] twoTokens = tokenizeMsg(two);

        final int tokensCount = oneTokens.length > twoTokens.length ? twoTokens.length
                : oneTokens.length;
        for (int i = 0; i < tokensCount; i++) {
            if (oneTokens[i].equals(twoTokens[i])) {
                Integer prevScore = voteMap.get(i);
                if (prevScore == null) {
                    prevScore = 0;
                }
                voteMap.put(i, ++prevScore);
            }
        }
    }

    private int getCostTokenIndex(String msg) {
        final String[] tokens = tokenizeMsg(msg);

        int costTokenIndex = -1;
        String moneyToken = "";

        final int tokenCount = tokens.length;
        for (int i = 0; i < tokenCount; i++) {
            String token = tokens[i];
            if (token.startsWith(mCurrencyUnit)) {
                moneyToken = token;
            } else if (token.endsWith(mCurrencyUnit)) {
                moneyToken = token;
            } else if (i > 0 && tokens[i - 1].equals(mCurrencyUnit)) {
                moneyToken = token;
            } else if (i < tokenCount - 2 && tokens[i + 1].equals(mCurrencyUnit)) {
                moneyToken = token;
            }
            if (getCostFromCostToken(moneyToken) > 0) {
                costTokenIndex = i;
                break;
            }
        }
        return costTokenIndex;
    }

    private double getCostFromCostToken(String token) {
        token = token.replace(mCurrencyUnit, "");
        token = token.replace(",", "");
        double cost = -1;
        try {
            cost = Double.parseDouble(token);
        } catch (NumberFormatException e) {

        }
        return cost;
    }

    private String[] tokenizeMsg(String msg) {
        return msg.split("\\s+");
    }

    private double getCost(String msg, boolean isIncome) {
        String[] tokens = tokenizeMsg(msg);
        int index = isIncome ? mIncomeCostTokenIndex : mOutgoCostTokenIndex;
        if (index < 0 || index > tokens.length) {
            index = getCostTokenIndex(msg);
        }
        if (index < 0 || index > tokens.length) {
            return -1;
        }
        double cost = getCostFromCostToken(tokens[index]);
        if (cost < 0) {
            index = getCostTokenIndex(msg);
            cost = getCostFromCostToken(tokens[index]);
        }
        return cost;
    }

    private void parseOutgo(String msg, long timeStamp) {
        if (DEBUG) {
            Log.d(TAG, "parse outgo! msg : " + msg + ", keyword : " + mOutgoKeyword);
        }
        if (!msg.contains(mOutgoKeyword)) {
            Log.d(TAG, "No keyword on outgo msg. keyword is " + mOutgoKeyword);
            return;
        }
        double cost = getCost(msg, false);
        if (cost < 0) {
            Log.d(TAG, "Can't parse outgo msg!");
            return;
        }

        String[] tokens = tokenizeMsg(msg);
        String paymentTitle = "";
        int tokensCount = tokens.length;
        for (int i = 0; i < tokensCount; i++) {
            if (!mRepeatingOutgoTokens.contains(i) && mOutgoCostTokenIndex != i) {
                paymentTitle += tokens[i] + " ";
            }
        }

        ArrayList<Event> activeEvents = DataManager.INSTANCE.getActiveEventOn(timeStamp);
        if (DEBUG) {
            Log.d(TAG, "active events : " + activeEvents.size());
        }
        for (Event event : activeEvents) {
            if (event.getmEndTime() > timeStamp && event.getmStartTime() < timeStamp) {
                Payment payment = new Payment(-1, event.getmId(), paymentTitle, timeStamp, cost);
                DataManager.INSTANCE.addPayment(payment);
            }
        }
    }

    private static final double CHARGE_TOLERANCE = 5;

    private void parseIncome(String msg, long timeStamp) {
        if (DEBUG) {
            Log.d(TAG, "parse income! msg : " + msg + ", keyword : " + mIncomeKeyword);
        }
        if (!msg.contains(mIncomeKeyword)) {
            Log.d(TAG, "No keyword on income msg. keyword is " + mIncomeKeyword);
            return;
        }
        double cost = getCost(msg, true);
        if (cost < 0) {
            Log.d(TAG, "Can't parse income msg!");
            return;
        }

        ArrayList<Member> members = DataManager.INSTANCE.getUnpaidMembers();
        for (Member member : members) {
            if (!msg.contains(member.getmName())) {
                continue;
            }

            if (member.getmCharge() < 0) {
                DataManager.INSTANCE.calculate(member.getmParentEventId());
            }
            final double charge = member.getmCharge();
            double diffPercent = (Math.abs(charge - cost) / cost) * 100.0;
            if (diffPercent < CHARGE_TOLERANCE) {
                Member newMember = new Member(member);
                newMember.setmPaymentState(Member.PaymentState.COMPLETE);
                newMember.setmPaidTime(timeStamp);
                newMember.setmPaymentConfirmMessage(msg);
                DataManager.INSTANCE.updateMember(newMember);
            }
        }
    }
}
