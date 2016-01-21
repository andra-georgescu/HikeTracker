package com.andra.hiketracker.service;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.andra.hiketracker.activity.PhotosActivity;
import com.andra.hiketracker.R;
import com.andra.hiketracker.network.VolleySingleton;
import com.andra.hiketracker.aidl.ILocationService;
import com.andra.hiketracker.aidl.IPhotosActivity;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Random;
import java.util.Vector;

/**
 * This is a service used to receive location updates in the background every 100m.
 * Once it gets a valid update, it uses the new location to request images from the Panoramio api,
 * which get sent to the currently registered client for the UI updates. In case there is no client
 * registered at the moment, it caches the image urls for batch updates later on.
 */
public class BackgroundLocationListenerService extends Service implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    // The client that is currently registered to receive image url updates
    private IPhotosActivity mClient;
    // This is the AIDL that gets sent to the client activity for further communication
    private final Binder mBinder = new LocationBinder();

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private List<String> mStoredPhotoUrls = new Vector<>();
    private JsonObjectRequest mCurrentPanoramioRequest;

    @Override
    public void onCreate() {
        super.onCreate();

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API).build();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // once a client binds to this service, it starts listening for location updates
        mGoogleApiClient.connect();
        return mBinder;
    }

    @Override
    public void onDestroy() {
        // don't leak the api client, this would leave the GPS on and drain the battery
        mGoogleApiClient.disconnect();
        cancelCurrentRequestIfRunning();
        super.onDestroy();
    }

    @Override
    public void onConnected(Bundle bundle) {
        createLocationRequest();
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000); // we get a new location every 10s or so
        mLocationRequest.setSmallestDisplacement(100); // but only if we traveled 100m
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onLocationChanged(Location location) {
        // once we have a new location, get a photo to mark it
        requestPhotosFromPanoramio(location.getLongitude(), location.getLatitude());
    }

    private void requestPhotosFromPanoramio(double longitude, double latitude) {
        String parametrizedUrl = getResources().getString(R.string.panoramio_request_parametrized_url);

        // we need quite a large range for Panoramio to actually return some photos,
        // so the images will no be exactly on the spot
        String minX = String.valueOf(longitude - 0.001);
        String minY = String.valueOf(latitude - 0.001);
        String maxX = String.valueOf(longitude + 0.001);
        String maxY = String.valueOf(latitude + 0.001);
        String url = String.format(parametrizedUrl, minX, minY, maxX, maxY);

        mCurrentPanoramioRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        // our request was successful and now we have a JSON response to play with
                        parsePhotosResponse(response);
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(BackgroundLocationListenerService.class.getSimpleName(), "There was an error trying to get images from Panoramio", error);
                    }
                });

        // If a request takes longer than 30s, cancel it and retry up to 3 times
        mCurrentPanoramioRequest.setRetryPolicy(new DefaultRetryPolicy(30000, 3, 3));
        VolleySingleton.getInstance(this).addToRequestQueue(mCurrentPanoramioRequest);
    }

    private void cancelCurrentRequestIfRunning() {
        if (mCurrentPanoramioRequest != null) {
            mCurrentPanoramioRequest.cancel();
        }
    }

    // out of up to 100 images, pick one at random and send the update to the client
    private void parsePhotosResponse(JSONObject response) {
        try {
            JSONArray photos = response.getJSONArray("photos");
            if (photos.length() == 0) {
                return;
            }

            Random rand = new Random();
            JSONObject photo = photos.getJSONObject(rand.nextInt(photos.length()));

            String photoUrl = photo.getString("photo_file_url");

            mStoredPhotoUrls.add(0, photoUrl);
            if (mClient != null) {
                mClient.addPhotoUrl(photoUrl);
            }
        } catch (JSONException e) {
            Log.e(PhotosActivity.class.getSimpleName(), "Error parsing photos JSON", e);
        }
    }

    public class LocationBinder extends Binder implements ILocationService {

        /**
         * Use this if you want to receive an image url once every 100m or so
         *
         * @param callback the AIDL needed for this service to send the image urls
         */
        public void registerClient(IPhotosActivity callback) {
            mClient = callback;
        }

        /**
         * Use this if you want to stop receiving updates
         */
        public void unregisterClient() {
            mClient = null;
        }

        /**
         * Use this method to receive all the photos that have been retrieved so far
         *
         * @return a List of image urls
         */
        public List<String> getAllUrls() {
            return mStoredPhotoUrls;
        }
    }
}
