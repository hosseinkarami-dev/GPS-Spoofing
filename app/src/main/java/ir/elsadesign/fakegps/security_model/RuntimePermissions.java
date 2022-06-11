package ir.elsadesign.fakegps.security_model;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.Arrays;

import ir.elsadesign.fakegps.activities.RuntimePermissionsActivity;

public final class RuntimePermissions {
    private static final int REQUEST_CODE_PERMISSIONS = 0;

    private static final ArrayList<String> MANDATORY_PERMISSIONS = new ArrayList<>(
            Arrays.asList("android.permission.ACCESS_MOCK_LOCATION", "android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION")
    );

    public static String[] getMissingPermissions(Activity activity) {

        PackageInfo info;
        try {
            info = activity.getPackageManager().getPackageInfo(activity.getPackageName(), PackageManager.GET_PERMISSIONS);
        } catch (PackageManager.NameNotFoundException e) {
            return new String[0];
        }

        if (info.requestedPermissions == null) {
            return new String[0];
        }

        ArrayList<String> missingPermissions = new ArrayList<>();
        for (int i = 0; i < info.requestedPermissions.length; i++) {
            if ((info.requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) == 0) {
                missingPermissions.add(info.requestedPermissions[i]);
            }
        }

        return missingPermissions.toArray(new String[0]);
    }

    public static boolean isEnabled(Activity activity) {

        final String[] missingPermissions = getMissingPermissions(activity);

        if (missingPermissions.length > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activity.requestPermissions(missingPermissions, REQUEST_CODE_PERMISSIONS);
            }
            return false;
        }

        return true;
    }

    public static void onRequestPermissionsResult(RuntimePermissionsActivity activity, int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != REQUEST_CODE_PERMISSIONS)
            return;

        if (grantResults.length == 0) {
            if (permissions.length == 0) {
                activity.onPermissionsGranted();
            } else if (isEnabled(activity))
                activity.onPermissionsGranted();
        } else {
            ArrayList<String> deniedPermissions = new ArrayList<>();

            for (int i = 0; i < grantResults.length; i++) {
                if (
                        (grantResults[i] != PackageManager.PERMISSION_GRANTED) &&
                                MANDATORY_PERMISSIONS.contains(permissions[i])
                ) {
                    deniedPermissions.add(permissions[i]);
                }
            }

            if (deniedPermissions.isEmpty()) {
                activity.onPermissionsGranted();
            } else {
                activity.onPermissionsDenied(
                        deniedPermissions.toArray(new String[0])
                );
            }
        }
    }

    // =============================================================================================

    public static boolean hasMandatoryPermissions(Context context) {
        for (String permission : MANDATORY_PERMISSIONS) {
            if (context.checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

}
