
package org.drykiss.android.app.sapphire;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {
    public static final String KEY_BANK_ACCOUNT = "bank_account";
    public static final String KEY_CURRENCY_UNIT = "currency_unit";
    public static final String KEY_BILL_FORMAT = "bill_format";
    public static final String KEY_DECIMAL_SPACES_FOR_MONEY = "decimal_spaces_for_money";
    public static final String KEY_SMS_PARSER_SERVICE_ON = "sms_parser_service_on";
    public static final String KEY_OUTGO_PHONE_NUMBER = "outgo_sms_phone_number";
    public static final String KEY_INCOME_PHONE_NUMBER = "income_sms_phone_number";
    public static final String KEY_OUTGO_KEYWORD = "outgo_sms_keyword";
    public static final String KEY_INCOME_KEYWORD = "income_sms_keyword";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        addPreferencesFromResource(R.xml.sapphire_preferences);
    }

}
