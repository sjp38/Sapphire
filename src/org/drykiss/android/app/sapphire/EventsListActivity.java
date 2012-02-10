
package org.drykiss.android.app.sapphire;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.example.android.actionbarcompat.ActionBarActivity;

import org.drykiss.android.app.sapphire.ad.AdvertisementManager;
import org.drykiss.android.app.sapphire.data.DataManager;
import org.drykiss.android.app.sapphire.data.Event;
import org.drykiss.android.app.sapphire.util.TimeUtil;

public class EventsListActivity extends ActionBarActivity implements
        DataManager.OnDataChangedListener {
    public static final String EXTRA_EVENT_POSITION = "org.drykiss.android.app.sapphire.EXTRA_EVENT_POSITION";
    private ListView mListView;
    private EventsAdapter mAdapter;
    private View mAdView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.events_list);

        DataManager.INSTANCE.setContext(getApplicationContext());
        DataManager.INSTANCE.loadDatas();
        DataManager.INSTANCE.registerEventChangedListener(this);

        mListView = (ListView) findViewById(R.id.eventsListView);
        mAdapter = new EventsAdapter();
        mListView.setAdapter(mAdapter);

        mAdView = AdvertisementManager.getAdvertisementView(this);
        LinearLayout adLayout = (LinearLayout) findViewById(R.id.advertiseLayout);
        adLayout.addView(mAdView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AdvertisementManager.destroyAd(mAdView);

        DataManager.INSTANCE.unregisterEventChangedListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.events_list_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_action_add:
                Intent intent = new Intent(this, EventEditActivity.class);
                intent.putExtra(EXTRA_EVENT_POSITION, -1);
                startActivity(intent);
                break;
            case R.id.menu_action_setting:
                Intent settingIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingIntent);
                break;
            default:
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDataChanged() {
        mAdapter.notifyDataSetChanged();
    }

    private class EventsAdapter extends BaseAdapter {
        View.OnClickListener mRemoveButtonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final View parent = (View) v.getParent();
                final EventsViewHolder viewHolder = (EventsViewHolder) parent.getTag();
                DataManager.INSTANCE.removeEvent(viewHolder.mPosition);
            }
        };

        View.OnClickListener mEventClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EventsViewHolder viewHolder = (EventsViewHolder) v.getTag();

                Intent intent = new Intent(EventsListActivity.this, EventDetailActivity.class);
                intent.putExtra(EXTRA_EVENT_POSITION, viewHolder.mPosition);
                startActivity(intent);
            }
        };

        @Override
        public int getCount() {
            return DataManager.INSTANCE.getEventsCount();
        }

        @Override
        public Object getItem(int position) {
            return DataManager.INSTANCE.getEvent(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            EventsViewHolder viewHolder = null;
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.event_list_item, null, false);
                TextView nameTextView = (TextView) convertView.findViewById(R.id.eventNameTextView);
                TextView timeTextView = (TextView) convertView.findViewById(R.id.eventTimeTextView);
                TextView stateTextView = (TextView) convertView
                        .findViewById(R.id.eventStateTextView);
                ImageButton removeButton = (ImageButton) convertView
                        .findViewById(R.id.eventRemoveImageButton);

                removeButton.setOnClickListener(mRemoveButtonClickListener);

                viewHolder = new EventsViewHolder(nameTextView, timeTextView, stateTextView,
                        removeButton);
                convertView.setTag(viewHolder);
                convertView.setOnClickListener(mEventClickListener);
            } else {
                viewHolder = (EventsViewHolder) convertView.getTag();
            }
            viewHolder.mPosition = position;

            Event event = DataManager.INSTANCE.getEvent(position);
            viewHolder.mNameTextView.setText(event.getmName());
            viewHolder.mTimeTextView.setText(TimeUtil.formatTimeToText(EventsListActivity.this,
                    event.getmStartTime(), true));
            String[] simpleEventStates = getResources().getStringArray(R.array.event_simple_states);
            viewHolder.mStateTextView.setText(simpleEventStates[event.getmState().ordinal()]);

            return convertView;
        }

        private class EventsViewHolder {
            int mPosition;
            TextView mNameTextView;
            TextView mTimeTextView;
            TextView mStateTextView;
            ImageButton mRemoveButton;

            public EventsViewHolder(TextView mNameTextView, TextView mTimeTextView,
                    TextView mStateTextView, ImageButton mRemoveButton) {
                super();
                this.mNameTextView = mNameTextView;
                this.mTimeTextView = mTimeTextView;
                this.mStateTextView = mStateTextView;
                this.mRemoveButton = mRemoveButton;
            }
        }
    }
}
