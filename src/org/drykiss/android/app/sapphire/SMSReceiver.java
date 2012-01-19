
package org.drykiss.android.app.sapphire;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

import org.drykiss.android.app.sapphire.util.TimeUtil;

public class SMSReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        intent.setClass(context, MessageParserService.class);
        context.startService(intent);
    }
}
