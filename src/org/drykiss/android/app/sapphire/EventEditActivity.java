
package org.drykiss.android.app.sapphire;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.android.actionbarcompat.ActionBarActivity;

import org.drykiss.android.app.sapphire.ad.AdvertisementManager;
import org.drykiss.android.app.sapphire.data.DataManager;
import org.drykiss.android.app.sapphire.data.Event;
import org.drykiss.android.app.sapphire.data.Member;
import org.drykiss.android.app.sapphire.data.Member.PaymentState;
import org.drykiss.android.app.sapphire.util.TimeUtil;

import java.util.Calendar;

public class EventEditActivity extends ActionBarActivity {
    private static final String TAG = "Sapphire_event_edit_activity";

    private static final int DEFAULT_EVENT_TIME_LENGTH_IN_HOUR = 2;

    private Calendar mStartCalendar;
    private Calendar mEndCalendar;
    private Event mEvent;
    private int mEventPosition = -1;

    private EditText mNameEditText;
    private Spinner mStateSpinner;
    private TextView mFromDateTextView;
    private TextView mFromTimeTextView;
    private TextView mToDateTextView;
    private TextView mToTimeTextView;
    
    private View mAdView;

    private TimePickerDialog.OnTimeSetListener mEventFromTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            mStartCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            mStartCalendar.set(Calendar.MINUTE, minute);
            mFromTimeTextView.setText(TimeUtil.formatTimeToText(EventEditActivity.this,
                    mStartCalendar.getTimeInMillis(), false));
        }
    };

    private TimePickerDialog.OnTimeSetListener mEventToTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            mEndCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            mEndCalendar.set(Calendar.MINUTE, minute);
            mToTimeTextView.setText(TimeUtil.formatTimeToText(EventEditActivity.this,
                    mEndCalendar.getTimeInMillis(), false));
        }
    };

    private DatePickerDialog.OnDateSetListener mEventFromDateSetListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            mStartCalendar.set(Calendar.YEAR, year);
            mStartCalendar.set(Calendar.MONTH, monthOfYear);
            mStartCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            mFromDateTextView.setText(TimeUtil.formatTimeToText(EventEditActivity.this,
                    mStartCalendar.getTimeInMillis(), true));
        }
    };

    private DatePickerDialog.OnDateSetListener mEventToDateSetListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            mEndCalendar.set(Calendar.YEAR, year);
            mEndCalendar.set(Calendar.MONTH, monthOfYear);
            mEndCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            mToDateTextView.setText(TimeUtil.formatTimeToText(EventEditActivity.this,
                    mEndCalendar.getTimeInMillis(), true));
        }
    };

    private View.OnClickListener mTimeTextViewClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.event_edit_fromDateTextView:
                    new DatePickerDialog(EventEditActivity.this, mEventFromDateSetListener,
                            mStartCalendar.get(Calendar.YEAR), mStartCalendar.get(Calendar.MONTH),
                            mStartCalendar.get(Calendar.DAY_OF_MONTH)).show();
                    break;
                case R.id.event_edit_fromTimeTextView:
                    new TimePickerDialog(EventEditActivity.this, mEventFromTimeSetListener,
                            mStartCalendar.get(Calendar.HOUR_OF_DAY),
                            mStartCalendar.get(Calendar.MINUTE), false).show();
                    break;
                case R.id.event_edit_toDateTextView:
                    new DatePickerDialog(EventEditActivity.this, mEventToDateSetListener,
                            mEndCalendar.get(Calendar.YEAR), mEndCalendar.get(Calendar.MONTH),
                            mEndCalendar.get(Calendar.DAY_OF_MONTH)).show();
                    break;
                case R.id.event_edit_toTimeTextView:
                    new TimePickerDialog(EventEditActivity.this, mEventToTimeSetListener,
                            mEndCalendar.get(Calendar.HOUR_OF_DAY),
                            mEndCalendar.get(Calendar.MINUTE), false).show();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.event_edit);
        
        if (!DataManager.INSTANCE.isActive()) {
            DataManager.INSTANCE.setContext(getApplicationContext());
            DataManager.INSTANCE.loadDatas();
        }

        final Intent intent = getIntent();
        mEventPosition = intent.getIntExtra(EventsListActivity.EXTRA_EVENT_POSITION, -1);
        if (mEventPosition == -1) {
            mStartCalendar = Calendar.getInstance();
            mEndCalendar = Calendar.getInstance();
            mEndCalendar.add(Calendar.HOUR, DEFAULT_EVENT_TIME_LENGTH_IN_HOUR);
            mEvent = new Event(-1, "", mStartCalendar.getTimeInMillis(),
                    mEndCalendar.getTimeInMillis(), -1, "", Event.State.IDLE);
        } else {
            mEvent = DataManager.INSTANCE.getEvent(mEventPosition);
            mStartCalendar = Calendar.getInstance();
            mStartCalendar.setTimeInMillis(mEvent.getmStartTime());
            mEndCalendar = Calendar.getInstance();
            mEndCalendar.setTimeInMillis(mEvent.getmEndTime());
        }

        initView();
        
        mAdView = AdvertisementManager.getAdvertisementView(this);
        LinearLayout adLayout = (LinearLayout) findViewById(R.id.advertiseLayout);
        adLayout.addView(mAdView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AdvertisementManager.destroyAd(mAdView);
    }

    private void initView() {
        mNameEditText = (EditText) findViewById(R.id.event_edit_nameEditText);
        mNameEditText.setText(mEvent.getmName());

        mStateSpinner = (Spinner) findViewById(R.id.event_edit_stateSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.event_states, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);        
        mStateSpinner.setAdapter(adapter);
        mStateSpinner.setSelection(mEvent.getmState().ordinal());

        mFromDateTextView = (TextView) findViewById(R.id.event_edit_fromDateTextView);
        mFromDateTextView.setText(TimeUtil.formatTimeToText(this, mStartCalendar.getTimeInMillis(),
                true));
        mFromDateTextView.setOnClickListener(mTimeTextViewClickListener);

        mFromTimeTextView = (TextView) findViewById(R.id.event_edit_fromTimeTextView);
        mFromTimeTextView.setText(TimeUtil.formatTimeToText(this, mStartCalendar.getTimeInMillis(),
                false));
        mFromTimeTextView.setOnClickListener(mTimeTextViewClickListener);

        mToDateTextView = (TextView) findViewById(R.id.event_edit_toDateTextView);
        mToDateTextView.setText(TimeUtil.formatTimeToText(this, mEndCalendar.getTimeInMillis(),
                true));
        mToDateTextView.setOnClickListener(mTimeTextViewClickListener);

        mToTimeTextView = (TextView) findViewById(R.id.event_edit_toTimeTextView);
        mToTimeTextView.setText(TimeUtil.formatTimeToText(this, mEndCalendar.getTimeInMillis(),
                false));
        mToTimeTextView.setOnClickListener(mTimeTextViewClickListener);

        if (mEventPosition == -1) {
            final Button deleteButton = (Button) findViewById(R.id.delete);
            deleteButton.setEnabled(false);
        }

    }

    public void onSaveButtonClicked(View v) {
        final String name = mNameEditText.getText().toString();
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(EventEditActivity.this, R.string.warning_event_must_have_name,
                    Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        final int selectedPosition = mStateSpinner.getSelectedItemPosition();
        final Event.State state = Event.State.values()[selectedPosition];
        final long startTime = mStartCalendar.getTimeInMillis();
        final long endTime = mEndCalendar.getTimeInMillis();
        if (endTime < startTime) {
            Toast.makeText(EventEditActivity.this,
                    R.string.warning_to_time_must_earlier_than_from_time,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            final Event newEvent = (Event) mEvent.clone();
            newEvent.setmName(name);
            newEvent.setmState(state);
            newEvent.setmStartTime(startTime);
            newEvent.setmEndTime(endTime);
            if (mEventPosition == -1) {
                DataManager.INSTANCE.addEvent(newEvent);
            } else {
                DataManager.INSTANCE.updateEvent(mEventPosition, newEvent);
            }
            mEvent = null;

            // Add myself.
            // Delay this implementation to future. User can do this by add
            // himself manually.
            // addMySelf();
            finish();
        } catch (CloneNotSupportedException e) {
            Log.e(TAG, "clone failed!", e);
        }
    }

    public void onDiscardButtonClicked(View v) {
        finish();
    }

    public void onDeleteButtonClicked(View v) {
        if (mEventPosition <= 0) {
            finish();
            return;
        }
        DataManager.INSTANCE.removeEvent(mEventPosition);
    }
}
