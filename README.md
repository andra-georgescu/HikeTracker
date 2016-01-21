# HikeTracker
Simple application which uses a service for receiving location updates from the Google Play Services api and uses these updates to retrieve, every 100m or so, an image from the Panoramio api. These images are being displayed in a RecyclerView with a LinearLayoutManager in inverse chronological order. Since it only receives location updates every 100 meters, the app is quite light on the battery life. Can be used during a walk, to track the places you've been to.

