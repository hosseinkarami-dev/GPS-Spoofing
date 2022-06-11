package ir.elsadesign.fakegps.service.looper;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;

import ir.elsadesign.fakegps.models.LocPoint;

public class LocationThread extends HandlerThread {
    private final Context mContext;
    private LocationThreadManager mLocationThreadManager;
    private int mTimeInterval;
    private Handler mHandler;

    public LocationThread(Context context, LocationThreadManager locationThreadManager, int timeInterval) {
        super("LocationThread", Process.THREAD_PRIORITY_MORE_FAVORABLE);

        mContext = context;
        mLocationThreadManager = locationThreadManager;
        mTimeInterval = timeInterval;
    }

    @Override
    public synchronized void start() {
        super.start();

        mHandler = new Handler(getLooper());
        mHandler.post(mUpdateLocation);
    }

    public void startThread() {
        MockLocationProviderManager.startMockingLocation(mContext);

        start();
    }

    public void stopThread() {
        MockLocationProviderManager.stopMockingLocation();

        mHandler.removeCallbacksAndMessages(null);
        try {
            quit();
            interrupt();
        }
        catch (Exception ignored) {}
        mLocationThreadManager = null;
    }

    public void updateTimeInterval(int timeInterval) {
        mTimeInterval = timeInterval;
    }

    Runnable mUpdateLocation = new Runnable() {
        @Override
        public void run() {
            LocPoint locPoint = mLocationThreadManager.getUpdateLocPoint();
            if (locPoint != null) {
                MockLocationProviderManager.exec(locPoint.getLatitude(), locPoint.getLongitude());
            }
            if (mLocationThreadManager.shouldContinue()) {
                mHandler.postDelayed(mUpdateLocation, mTimeInterval);
            }
        }
    };

    public Handler getHandler() {
        return mHandler;
    }
}
