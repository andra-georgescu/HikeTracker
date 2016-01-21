package com.andra.hiketracker.network;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * This singleton class is used to keep one persistent instance of the
 * request queue needed throughout the application
 * for making network requests
 */
public class VolleySingleton {
    private static VolleySingleton mInstance = null;
    private RequestQueue mRequestQueue;

    private VolleySingleton(Context context) {
        mRequestQueue = Volley.newRequestQueue(context);
    }

    public static VolleySingleton getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new VolleySingleton(context);
        }
        return mInstance;
    }

    public RequestQueue getRequestQueue() {
        return this.mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }
}
