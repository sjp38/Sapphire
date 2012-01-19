
package org.drykiss.android.app.sapphire.util;

import android.content.Context;

import org.drykiss.android.app.sapphire.R;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class TimeUtil {
    public static String formatTimeToText(Context context, long time, boolean isDate) {
        String format = null;
        if (isDate) {
            format = context.getResources().getString(R.string.date_format);
        } else {
            format = context.getResources().getString(R.string.time_format);
        }
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        Timestamp timeStamp = new Timestamp(time);
        return simpleDateFormat.format(timeStamp);
    }
}
