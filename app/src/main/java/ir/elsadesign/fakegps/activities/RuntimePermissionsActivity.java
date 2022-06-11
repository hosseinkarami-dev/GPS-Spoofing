package ir.elsadesign.fakegps.activities;

public abstract class RuntimePermissionsActivity extends BaseActivity {
    public abstract void onPermissionsGranted();
    public abstract void onPermissionsDenied(String[] permissions);
}
