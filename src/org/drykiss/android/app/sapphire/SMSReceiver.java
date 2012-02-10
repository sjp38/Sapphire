
package org.drykiss.android.app.sapphire;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SMSReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        intent.setClass(context, MessageParserService.class);
        context.startService(intent);
    }
}
