package ir.elsadesign.fakegps;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.Style;

import java.util.ArrayList;
import java.util.List;

public class MapTile {
    private SharedPreferences prefs;
    public static final String MapBoxAccessToken = "pk.eyJ1IjoiaGthcmFtaTE4MTEiLCJhIjoiY2ppdDhrdm41MjBuaDNxbWZiaWV5ZGVqMiJ9.MSioDPEf9Kth_eU55RogWQ";

    public MapTile(Context context){
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }
    public void setMapTile(String mapTile){
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("map_tile",mapTile);
        editor.apply();
    }

    public void setTileSource(String tileSource){
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("tile_source",tileSource);
        editor.apply();
    }


    public String getMapTile(){
        return prefs.getString("map_tile","mapbox");
    }

    public String getMapTileGLStyle(){
        return prefs.getString("tile_source",Style.MAPBOX_STREETS);
    }
}