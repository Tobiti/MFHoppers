package net.squidstudios.mfhoppers.util;

import java.util.HashMap;
import java.util.Map;

public class MapBuilder {

    Map<String, Object> map;

    public MapBuilder(){

        map = new HashMap<>();

    }
    public MapBuilder add(String key, Object value){

        map.put(key, value);
        return this;

    }
    public Map<String, Object> getMap(){

        return map;

    }

}
