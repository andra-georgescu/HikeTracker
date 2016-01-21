package com.andra.hiketracker.aidl;

import java.util.List;

public interface ILocationService {

    void registerClient(IPhotosActivity callback);

    void unregisterClient();

    List<String> getAllUrls();

}
