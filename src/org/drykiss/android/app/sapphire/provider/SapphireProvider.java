
package org.drykiss.android.app.sapphire.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

public class SapphireProvider extends ContentProvider {
    private static final boolean DEBUG = false;
    private static final String TAG = "sapphire_dataProvider";
    private static final String DB_NAME = "sapphire.db";
    private static final int DB_VERSION = 1;
    private static final String AUTHORITY = "org.drykiss.android.app.sapphire.provider";

    private static final int EVENT = 1;
    private static final int EVENTS = 2;
    private static final int PAYMENT = 3;
    private static final int PAYMENTS = 4;
    private static final int PAYMENTS_FOR_EVENT = 5;
    private static final int MEMBER = 6;
    private static final int MEMBERS = 7;
    private static final int MEMBERS_FOR_PAYMENT = 8;
    private static final int MEMBERS_FOR_EVENT = 9;

    private static final UriMatcher sUriMatcher;
    private SQLiteDatabase mSapphireDB;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, "event/#", EVENT);
        sUriMatcher.addURI(AUTHORITY, "event", EVENTS);
        sUriMatcher.addURI(AUTHORITY, "payment/#", PAYMENT);
        sUriMatcher.addURI(AUTHORITY, "payment", PAYMENTS);
        sUriMatcher.addURI(AUTHORITY, "payment_event/#", PAYMENTS_FOR_EVENT);
        sUriMatcher.addURI(AUTHORITY, "member/#", MEMBER);
        sUriMatcher.addURI(AUTHORITY, "member", MEMBERS);
        sUriMatcher.addURI(AUTHORITY, "member_payment/#", MEMBERS_FOR_PAYMENT);
        sUriMatcher.addURI(AUTHORITY, "member_event/#", MEMBERS_FOR_EVENT);
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        SapphireDBHelper dbHelper = new SapphireDBHelper(context, DB_NAME, null, DB_VERSION);
        mSapphireDB = dbHelper.getWritableDatabase();
        return (mSapphireDB == null) ? false : true;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case EVENT:
                return "vnd.android.cursor.item/vnd.drykiss.event";
            case EVENTS:
                return "vnd.android.cursor.dir/vnd.drykiss.event";
            case PAYMENT:
                return "vnd.android.cursor.item/vnd.drykiss.payment";
            case PAYMENTS:
                return "vnd.android.cursor.dir/vnd.drykiss.payment";
            case PAYMENTS_FOR_EVENT:
                return "vnd.android.cursor.dir/vnd.drykiss.payment";
            case MEMBER:
                return "vnd.android.cursor.item/vnd.drykiss.member";
            case MEMBERS:
                return "vnd.android.cursor.dir/vnd.drykiss.member";
            case MEMBERS_FOR_PAYMENT:
                return "vnd.android.cursor.dir/vnd.drykiss.member";
            case MEMBERS_FOR_EVENT:
                return "vnd.android.cursor.dir/vnd.drykiss.member";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        final List<String> segments = uri.getPathSegments();
        final String lastParam = segments.get(segments.size() - 1);
        switch (sUriMatcher.match(uri)) {
            case EVENT:
                qb.setTables(Event.TABLE_NAME);
                qb.appendWhere(Event.KEY_ID + "=" + lastParam);
                break;
            case EVENTS:
                qb.setTables(Event.TABLE_NAME);
                break;
            case PAYMENT:
                qb.setTables(Payment.TABLE_NAME);
                qb.appendWhere(Payment.KEY_ID + "=" + lastParam);
                break;
            case PAYMENTS:
                qb.setTables(Payment.TABLE_NAME);
                break;
            case PAYMENTS_FOR_EVENT:
                qb.setTables(Payment.TABLE_NAME);
                qb.appendWhere(Payment.KEY_PARENT_EVENT + "=" + lastParam);
                break;
            case MEMBER:
                qb.setTables(Member.TABLE_NAME);
                qb.appendWhere(Member.KEY_ID + "=" + lastParam);
                break;
            case MEMBERS:
                qb.setTables(Member.TABLE_NAME);
                break;
            case MEMBERS_FOR_PAYMENT:
                qb.setTables(Member.TABLE_NAME);
                qb.appendWhere(Member.KEY_PARENT_PAYMENT + "=" + lastParam);
                break;
            case MEMBERS_FOR_EVENT:
                qb.setTables(Member.TABLE_NAME);
                qb.appendWhere(Member.KEY_PARENT_EVENT + "=" + lastParam);
                break;
            default:
                break;
        }

        Cursor c = qb.query(mSapphireDB, projection, selection, selectionArgs, null, null,
                sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (DEBUG) {
            Log.d(TAG, "insert. uri : " + uri + ", values : " + values);
        }
        String tableName;
        Uri resultUri;
        final int matched = sUriMatcher.match(uri);
        if (matched == EVENT || matched == EVENTS) {
            tableName = Event.TABLE_NAME;
            resultUri = Event.CONTENT_URI;
        } else if (matched >= PAYMENT && matched <= PAYMENTS_FOR_EVENT) {
            tableName = Payment.TABLE_NAME;
            resultUri = Payment.CONTENT_URI;
            if (matched == PAYMENTS_FOR_EVENT) {
                resultUri = Payment.FOR_EVENT_CONTENT_URI;
            }
        } else {
            tableName = Member.TABLE_NAME;
            resultUri = Member.CONTENT_URI;
            if (matched == MEMBERS_FOR_EVENT) {
                resultUri = Member.FOR_EVENT_CONTENT_URI;
            } else if (matched == MEMBERS_FOR_PAYMENT) {
                resultUri = Member.FOR_PAYMENT_CONTENT_URI;
            }
        }

        long rowId = mSapphireDB.insert(tableName, null, values);
        if (rowId > 0) {
            resultUri = ContentUris.withAppendedId(resultUri, rowId);
            getContext().getContentResolver().notifyChange(uri, null);
            return resultUri;
        }
        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (DEBUG) {
            Log.d(TAG, "delete. uri : " + uri + ", selection : " + selection + ", selectionArgs : "
                    + selectionArgs);
        }
        int count;
        final List<String> segments = uri.getPathSegments();
        final String lastParam = segments.get(segments.size() - 1);
        String selectionWithUri = "=" + lastParam
                + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : "");
        switch (sUriMatcher.match(uri)) {
            case EVENT:
                selectionWithUri = Event.KEY_ID + selectionWithUri;
                count = mSapphireDB.delete(Event.TABLE_NAME, selectionWithUri, selectionArgs);
                break;
            case EVENTS:
                count = mSapphireDB.delete(Event.TABLE_NAME, selection, selectionArgs);
                break;
            case PAYMENT:
                selectionWithUri = Payment.KEY_ID + selectionWithUri;
                count = mSapphireDB.delete(Payment.TABLE_NAME, selectionWithUri, selectionArgs);
                break;
            case PAYMENTS:
                count = mSapphireDB.delete(Payment.TABLE_NAME, selection, selectionArgs);
                break;
            case PAYMENTS_FOR_EVENT:
                selectionWithUri = Payment.KEY_PARENT_EVENT + selectionWithUri;
                count = mSapphireDB.delete(Payment.TABLE_NAME, selectionWithUri, selectionArgs);
                break;
            case MEMBER:
                selectionWithUri = Member.KEY_ID + selectionWithUri;
                count = mSapphireDB.delete(Member.TABLE_NAME, selectionWithUri, selectionArgs);
                break;
            case MEMBERS:
                count = mSapphireDB.delete(Member.TABLE_NAME, selection, selectionArgs);
                break;
            case MEMBERS_FOR_PAYMENT:
                selectionWithUri = Member.KEY_PARENT_PAYMENT + selectionWithUri;
                count = mSapphireDB.delete(Member.TABLE_NAME, selectionWithUri, selectionArgs);
                break;
            case MEMBERS_FOR_EVENT:
                selectionWithUri = Member.KEY_PARENT_EVENT + selectionWithUri;
                count = mSapphireDB.delete(Member.TABLE_NAME, selectionWithUri, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unsupported uri : " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count;
        final List<String> segments = uri.getPathSegments();
        final String lastParam = segments.get(segments.size() - 1);
        String selectionWithUri = "=" + lastParam
                + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : "");
        if (DEBUG) {
            Log.d(TAG, "update. uri : " + uri + ", values : " + values
                    + ", selectionWithUri : " + selectionWithUri);
        }
        switch (sUriMatcher.match(uri)) {
            case EVENT:
                selectionWithUri = Event.KEY_ID + selectionWithUri;
                count = mSapphireDB.update(Event.TABLE_NAME, values, selectionWithUri,
                        selectionArgs);
                break;
            case EVENTS:
                count = mSapphireDB.update(Event.TABLE_NAME, values, selection, selectionArgs);
                break;
            case PAYMENT:
                selectionWithUri = Payment.KEY_ID + selectionWithUri;
                count = mSapphireDB.update(Payment.TABLE_NAME, values, selectionWithUri,
                        selectionArgs);
                break;
            case PAYMENTS:
                count = mSapphireDB.update(Payment.TABLE_NAME, values, selection, selectionArgs);
                break;
            case PAYMENTS_FOR_EVENT:
                selectionWithUri = Payment.KEY_PARENT_EVENT + selectionWithUri;
                count = mSapphireDB.update(Payment.TABLE_NAME, values, selectionWithUri,
                        selectionArgs);
                break;
            case MEMBER:
                selectionWithUri = Member.KEY_ID + selectionWithUri;
                count = mSapphireDB
                        .update(Member.TABLE_NAME, values, selectionWithUri, selectionArgs);
                break;
            case MEMBERS:
                count = mSapphireDB.update(Member.TABLE_NAME, values, selection, selectionArgs);
                break;
            case MEMBERS_FOR_PAYMENT:
                selectionWithUri = Member.KEY_PARENT_PAYMENT + selectionWithUri;
                count = mSapphireDB
                        .update(Member.TABLE_NAME, values, selectionWithUri, selectionArgs);
                break;
            case MEMBERS_FOR_EVENT:
                selectionWithUri = Member.KEY_PARENT_EVENT + selectionWithUri;
                count = mSapphireDB
                        .update(Member.TABLE_NAME, values, selectionWithUri, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unsupported uri : " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    public static final class Event {
        public static final String TABLE_NAME = "events";
        public static final Uri CONTENT_URI = Uri
                .parse("content://" + AUTHORITY + "/event");

        public static final String KEY_ID = "_id";
        public static final String KEY_NAME = "name";
        public static final String KEY_START_TIME = "start_time";
        public static final String KEY_END_TIME = "end_time";
        public static final String KEY_NOTICED_TIME = "noticed_time";
        public static final String KEY_NOTICE_MESSAGE = "notice_msg";
        public static final String KEY_STATE = "state";

        public static final int ID_COLUMN = 0;
        public static final int NAME_COLUMN = 1;
        public static final int START_TIME_COLUMN = 2;
        public static final int END_TIME_COLUMN = 3;
        public static final int NOTICED_TIME_COLUMN = 4;
        public static final int NOTICE_MESSAGE_COLUMN = 5;
        public static final int STATE_COLUMN = 6;
    }

    public static final class Payment {
        public static final String TABLE_NAME = "payments";
        public static final Uri CONTENT_URI = Uri
                .parse("content://" + AUTHORITY + "/payment");
        public static final Uri FOR_EVENT_CONTENT_URI = Uri
                .parse("content://" + AUTHORITY + "/payment_event");

        public static final String KEY_ID = "_id";
        public static final String KEY_NAME = "name";
        public static final String KEY_TIME = "time";
        public static final String KEY_COST = "cost";
        public static final String KEY_PARENT_EVENT = "event";

        public static final int ID_COLUMN = 0;
        public static final int NAME_COLUMN = 1;
        public static final int TIME_COLUMN = 2;
        public static final int COST_COLUMN = 3;
        public static final int PARENT_EVENT_COLUMN = 4;
    }

    public static final class Member {
        public static final String TABLE_NAME = "members";
        public static final Uri CONTENT_URI = Uri
                .parse("content://" + AUTHORITY + "/member");
        public static final Uri FOR_PAYMENT_CONTENT_URI = Uri
                .parse("content://" + AUTHORITY + "/member_payment");
        public static final Uri FOR_EVENT_CONTENT_URI = Uri
                .parse("content://" + AUTHORITY + "/member_event");

        public static final String KEY_ID = "_id";
        public static final String KEY_CONTACT_ID = "contactid";
        public static final String KEY_CONTACT_LOOKUP_ID = "lookupid";
        public static final String KEY_PHOTO_ID = "photoid";
        public static final String KEY_NAME = "name";
        public static final String KEY_ADDRESS = "adress";
        public static final String KEY_PAYMENT_STATE = "payment_state";
        public static final String KEY_PAID_TIME = "paid_time";
        public static final String KEY_ALLOC_REASON = "alloc_reason";
        public static final String KEY_ALLOC_PERCENTAGE = "alloc_percent";
        public static final String KEY_PAYMENT_CONFIRM_MESSAGE = "payment_confirm_msg";
        public static final String KEY_PARENT_PAYMENT = "payment";
        public static final String KEY_PARENT_EVENT = "event";

        public static final int ID_COLUMN = 0;
        public static final int CONTACT_ID_COLUMN = 1;
        public static final int CONTACT_LOOKUP_ID_COLUMN = 2;
        public static final int PHOTO_ID_COLUMN = 3;
        public static final int NAME_COLUMN = 4;
        public static final int ADDRESS_COLUMN = 5;
        public static final int PAYMENT_STATE_COLUMN = 6;
        public static final int PAID_TIME_COLUMN = 7;
        public static final int ALLOC_REASON_COLUMN = 8;
        public static final int ALLOC_PERCENTAGE_COLUMN = 9;
        public static final int PAYMENT_CONFIRM_MESSAGE = 10;
        public static final int PARENT_PAYMENT_COLUMN = 11;
        public static final int PARENT_EVENT_COLUMN = 12;
    }

    private static class SapphireDBHelper extends SQLiteOpenHelper {
        private static final String DATABASE_CREATE_EVENT = "CREATE TABLE " + Event.TABLE_NAME
                + " ("
                + Event.KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + Event.KEY_NAME + " TEXT, "
                + Event.KEY_START_TIME + " INTEGER, "
                + Event.KEY_END_TIME + " INTEGER, "
                + Event.KEY_NOTICED_TIME + " INTEGER, "
                + Event.KEY_NOTICE_MESSAGE + " TEXT, "
                + Event.KEY_STATE + " TEXT); ";

        private static final String DATABASE_CREATE_PAYMENT = "CREATE TABLE " + Payment.TABLE_NAME
                + " ("
                + Payment.KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + Payment.KEY_NAME + " TEXT, "
                + Payment.KEY_TIME + " INTEGER, "
                + Payment.KEY_COST + " DOUBLE, "
                + Payment.KEY_PARENT_EVENT + " INTEGER REFERENCES " + Event.TABLE_NAME + "("
                + Event.KEY_ID + ")); ";

        private static final String DATABASE_CREATE_MEMBER = "CREATE TABLE " + Member.TABLE_NAME
                + " ("
                + Member.KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + Member.KEY_CONTACT_ID + " INTEGER, "
                + Member.KEY_CONTACT_LOOKUP_ID + " TEXT, "
                + Member.KEY_PHOTO_ID + " INTEGER, "
                + Member.KEY_NAME + " TEXT, "
                + Member.KEY_ADDRESS + " TEXT, "
                + Member.KEY_PAYMENT_STATE + " TEXT, "
                + Member.KEY_PAID_TIME + " INTEGER, "
                + Member.KEY_ALLOC_REASON + " TEXT, "
                + Member.KEY_ALLOC_PERCENTAGE + " INTEGER, "
                + Member.KEY_PAYMENT_CONFIRM_MESSAGE + " TEXT, "
                + Member.KEY_PARENT_PAYMENT + " INTEGER REFERENCES " + Payment.TABLE_NAME + "("
                + Payment.KEY_ID + "), "
                + Member.KEY_PARENT_EVENT + " INTEGER REFERENCES " + Event.TABLE_NAME + "("
                + Event.KEY_ID + ")); ";

        public SapphireDBHelper(Context context, String name, CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE_EVENT);
            db.execSQL(DATABASE_CREATE_PAYMENT);
            db.execSQL(DATABASE_CREATE_MEMBER);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrade database from " + oldVersion + " to " + newVersion
                    + ". will destroy all data.");
            db.execSQL("DROP TABLE IF EXISTS " + Event.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + Payment.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + Member.TABLE_NAME);
            onCreate(db);
        }
    }
}
