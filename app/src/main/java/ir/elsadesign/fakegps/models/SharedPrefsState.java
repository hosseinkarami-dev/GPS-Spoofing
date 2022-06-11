package ir.elsadesign.fakegps.models;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefsState {
    public int     time_interval;
    public int     fixed_count;

    public SharedPrefsState(Context context) {
        SharedPreferences sharedPreferences = SharedPrefs.getSharedPreferences(context);

        time_interval            = SharedPrefs.getTimeInterval(sharedPreferences, context);
        fixed_count              = SharedPrefs.getFixedCount(sharedPreferences, context);
    }

    private boolean is_equal(double a, double b, double threshold) {
        return (Math.abs(a - b) < threshold);
    }

}
