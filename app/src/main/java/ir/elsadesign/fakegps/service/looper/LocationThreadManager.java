package ir.elsadesign.fakegps.service.looper;

import android.annotation.SuppressLint;
import android.content.Context;

import ir.elsadesign.fakegps.listeners.ISharedPrefsListener;
import ir.elsadesign.fakegps.models.LocPoint;
import ir.elsadesign.fakegps.models.SharedPrefs;
import ir.elsadesign.fakegps.models.SharedPrefsState;
import ir.elsadesign.fakegps.service.LocationService;

public class LocationThreadManager implements ISharedPrefsListener {
    @SuppressLint("StaticFieldLeak")
    private static final LocationThreadManager INSTANCE = new LocationThreadManager();

    private Context mContext;
    private LocationThread mLocationThread;
    private LocPoint mCurrentLocPoint;
    private LocPoint mOriginLocPoint;
    private LocPoint mTargetLocPoint;
    private int mFlyTime;
    private int mFlyTimeIndex;

    private int mTimeInterval;
    private int mFixedCount;
    private int mFixedCountRemaining;

    private boolean mIsStarted = false;
    private boolean mIsFlyMode = false;

    private LocationThreadManager() {
        mContext = null;
    }

    public void init(Context context) {
        mContext = context;
        importSharedPrefs();
    }

    public static LocationThreadManager get() {
        return INSTANCE;
    }

    public void start(LocPoint locPoint) {
        if (mContext == null) return;
        if (locPoint == null) return;

        mCurrentLocPoint = new LocPoint(locPoint);
        if ((mLocationThread == null) || !mLocationThread.isAlive()) {
            mLocationThread = new LocationThread(mContext, this, mTimeInterval);
            mLocationThread.startThread();
        }

        mFixedCountRemaining = mFixedCount;
        mIsStarted = true;
    }

    public void stop() {
        if (mLocationThread != null) {
            mLocationThread.stopThread();
            mLocationThread = null;
        }

        mIsStarted = false;

        stopService();
    }

    private void stopService() {
        LocationService.doStop(mContext, true);
    }

    public boolean isStarted() {
        return mIsStarted;
    }

    public LocPoint getCurrentLocPoint() {
        return new LocPoint(mCurrentLocPoint);
    }

    public LocPoint getUpdateLocPoint() {
        if (!mIsFlyMode && (mFixedCountRemaining != 0)) {
            if (mFixedCountRemaining < 0) {
                return null;
            }
            if (mFixedCountRemaining == 1) {
                mFixedCountRemaining = -1;
            } else {
                mFixedCountRemaining--;
            }
        }

        if (!mIsFlyMode) {
            return new LocPoint(mCurrentLocPoint);
        }

        if (mFlyTimeIndex >= mFlyTime) {
            jumpToLocation(mTargetLocPoint);
            mFixedCountRemaining = -1;
            return new LocPoint(mCurrentLocPoint);
        } else {
            float factor = (float) mFlyTimeIndex / (float) mFlyTime;
            double lat = mOriginLocPoint.getLatitude() + (factor * (mTargetLocPoint.getLatitude() - mOriginLocPoint.getLatitude()));
            double lon = mOriginLocPoint.getLongitude() + (factor * (mTargetLocPoint.getLongitude() - mOriginLocPoint.getLongitude()));
            mFlyTimeIndex++;
            mCurrentLocPoint.setLatitude(lat);
            mCurrentLocPoint.setLongitude(lon);
            return new LocPoint(mCurrentLocPoint);
        }
    }

    public boolean tripIsEnded() {
        if(!mIsFlyMode)
            return true;

        return !shouldContinue();
    }

    public boolean shouldContinue() {
        boolean is_done = (
                (!mIsFlyMode && (mFixedCountRemaining < 0))
                        || (mIsFlyMode && (mFlyTimeIndex > mFlyTime))
        );

        if (is_done) {
            stop();
            LocPoint modifiedLoc = SharedPrefs.getTripOrigin(mContext);
            LocationService.doStart(mContext, true, modifiedLoc, null, 0, false);
        }
        return !is_done;
    }

    public void jumpToLocation(LocPoint location) {
        mIsFlyMode = false;
        mCurrentLocPoint = new LocPoint(location);
    }

    public void flyToLocation(LocPoint location, int trip_duration_seconds) {
        mOriginLocPoint = new LocPoint(mCurrentLocPoint);
        mTargetLocPoint = new LocPoint(location);
        mIsFlyMode = true;
        mFlyTimeIndex = 0;
        mFlyTime = convertFlyTime_secondsToLoopIterations(trip_duration_seconds, mTimeInterval);
    }

    public boolean isFlyMode() {
        return mIsFlyMode;
    }

    public void stopFlyMode() {
        mIsFlyMode = false;
    }

    @Override
    public void onSharedPrefsChange(short diff_fields) {
        importSharedPrefs();
    }

    private void importSharedPrefs() {
        if (mContext == null) return;

        SharedPrefsState prefsState = new SharedPrefsState(mContext);

        if (mFixedCount != prefsState.fixed_count) {
            mFixedCount = prefsState.fixed_count;

            if (mIsStarted && !mIsFlyMode) {
                mFixedCountRemaining = mFixedCount;
            }
        }

        if (mTimeInterval != prefsState.time_interval) {
            updateFlyTime(prefsState.time_interval);

            mTimeInterval = prefsState.time_interval;

            if ((mLocationThread != null) && mLocationThread.isAlive()) {
                mLocationThread.updateTimeInterval(mTimeInterval);
            }
        }
    }

    private void updateFlyTime(int new_time_interval) {
        if (!mIsStarted || !mIsFlyMode || (mFlyTimeIndex >= mFlyTime))
            return;

        int remaining_trip_duration_seconds = convertFlyTime_loopIterationsToSeconds(mFlyTime - mFlyTimeIndex, mTimeInterval);
        int remaining_trip_duration_iterations = convertFlyTime_secondsToLoopIterations(remaining_trip_duration_seconds, new_time_interval);

        mOriginLocPoint = new LocPoint(mCurrentLocPoint);
        mFlyTimeIndex = 0;
        mFlyTime = remaining_trip_duration_iterations;
    }

    private static int convertFlyTime_secondsToLoopIterations(int trip_duration_seconds, int time_interval) {
        return (int) Math.ceil((1000f / time_interval) * trip_duration_seconds);
    }

    private static int convertFlyTime_loopIterationsToSeconds(int trip_duration_iterations, int time_interval) {
        return (int) Math.ceil((time_interval / 1000f) * trip_duration_iterations);
    }
}
