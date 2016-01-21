package com.andra.hiketracker.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.andra.hiketracker.ui.PhotosAdapter;
import com.andra.hiketracker.R;
import com.andra.hiketracker.aidl.ILocationService;
import com.andra.hiketracker.aidl.IPhotosActivity;
import com.andra.hiketracker.service.BackgroundLocationListenerService;

import java.util.ArrayList;
import java.util.List;

public class PhotosActivity extends AppCompatActivity implements IPhotosActivity {

    private static final String PREF_SERVICE_STARTED = "isStarted";

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private PhotosAdapter mAdapter;
    private List<String> mPhotoUrls;
    private MenuItem mActionButton;
    private boolean mStarted = false;

    // The AIDL for communicating with the service
    // (registering/unregistering as a client, requesting all the data so far)
    private ILocationService mService = null;

    // The actual connection to the service; once it is connected we can instantiate our AIDL for
    // further requests and also register as a client of the service so that we can receive
    // real time updates; in case the service was already running for some time, get all the photos
    // up until now and display them
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mService = (ILocationService) binder;
            mService.registerClient(PhotosActivity.this);

            refreshPhotosFromService();
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    // This method does a batch update of all the photo urls we got so far and updates the UI
    private void refreshPhotosFromService() {
        mPhotoUrls.clear();
        mPhotoUrls.addAll(0, mService.getAllUrls());
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_photos);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        mRecyclerView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mPhotoUrls = new ArrayList<>();
        mAdapter = new PhotosAdapter(this, mPhotoUrls);
        mRecyclerView.setAdapter(mAdapter);

        // It can happen that, when pressing on the back button, the activity gets destroyed;
        // for such cases, we need to persist our current state and retrieve it upon recreation
        mStarted = getPreferences(MODE_PRIVATE).getBoolean(PREF_SERVICE_STARTED, false);
        if (mStarted) {
            startService(new Intent(this, BackgroundLocationListenerService.class));
            bindService(new Intent(this, BackgroundLocationListenerService.class), mServiceConnection, BIND_AUTO_CREATE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_photos, menu);
        mActionButton = menu.findItem(R.id.action_start_stop);
        // Reflect the current state of our service by alternating the action button between start/stop
        mActionButton.setTitle(getResources().getString(mStarted ? R.string.action_stop : R.string.action_start));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_start_stop) {
            mStarted = !mStarted;
            // once our state changes, reflect it in shared prefs
            getPreferences(MODE_PRIVATE).edit().putBoolean(PREF_SERVICE_STARTED, mStarted).commit();
            if (mStarted) {
                // when the user wants to start the journey, start the service for location updates
                startService(new Intent(this, BackgroundLocationListenerService.class));
                bindService(new Intent(this, BackgroundLocationListenerService.class), mServiceConnection, BIND_AUTO_CREATE);
                mActionButton.setTitle(getResources().getString(R.string.action_stop));
            } else {
                // if the user decides to stop the app, remove this client from the service so that
                // we no longer get any remaining updates and unbind from the service,
                // which should lead to it being killed
                mService.unregisterClient();
                unbindService(mServiceConnection);
                mActionButton.setTitle(getResources().getString(R.string.action_start));
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // update the UI and register as a client for the service to resume the updates
        if (mService != null) {
            refreshPhotosFromService();
            mService.registerClient(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // we don't want any unnecessary updates if the activity is not in forground
        if (mService != null) {
            mService.unregisterClient();
        }
    }

    /**
     * This method updates the UI by adding a new photo from the given url to the top of the list
     *
     * @param url the URL to retrieve the image from
     */
    @Override
    public void addPhotoUrl(String url) {
        mPhotoUrls.add(0, url);
        mAdapter.notifyDataSetChanged();
    }
}
