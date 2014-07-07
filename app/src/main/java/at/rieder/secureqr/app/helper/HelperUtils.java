package at.rieder.secureqr.app.helper;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Base64;
import android.util.Log;

import java.net.URI;
import java.net.URISyntaxException;

import at.rieder.secureqr.app.activities.ScanQRActivity;

/**
 * Created by Thomas on 23.03.14.
 */
public class HelperUtils {

    private static final String TAG = HelperUtils.class.getSimpleName();
    private static String appVersion = "1.0";
    private static Context context;
    private static ScanQRActivity scanQRActivity;

    private HelperUtils() {

    }

    public static ScanQRActivity getScanQRActivity() {
        return scanQRActivity;
    }

    public static void setScanQRActivity(ScanQRActivity scanQRActivity) {
        HelperUtils.scanQRActivity = scanQRActivity;
    }

    public static Context getContext() {
        return context;
    }

    public static void setContext(Context context) {
        HelperUtils.context = context;
    }

    public static String getAppVersion() {
        return appVersion;
    }

    public static void setAppVersion(String appVersion) {
        HelperUtils.appVersion = appVersion;
    }

    public static String getTag() {
        return TAG;
    }

    public static boolean isDeviceOnline() {
        if (context != null) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (connectivityManager != null) {
                NetworkInfo[] networkInfos = connectivityManager.getAllNetworkInfo();

                if (networkInfos != null && networkInfos.length > 0) {
                    for (int i = 0; i < networkInfos.length; i++) {
                        if (networkInfos[i].isConnected()) {
                            Log.d(TAG, "the device is online");
                            return true;
                        }
                    }
                }
            }
        }
        Log.d(TAG, "the device is offline");
        return false;
    }

    public static String getChecksumFromUrl(String address) {
        try {
            URI url = new URI(address);
            String fragment = url.getFragment();

            if (fragment == null) {
                return null;
            }

            if (fragment.indexOf('#') != fragment.lastIndexOf('#')) {
                // there is more than one '#' we need to split it
                fragment = fragment.substring(fragment.lastIndexOf('#'));
                Log.d(TAG, "the new split fragment is: " + fragment);
            }

            byte[] hashBytes = Base64.decode(fragment, Base64.URL_SAFE);

            Log.d(TAG, "the number of bytes is: " + hashBytes.length);

            if(hashBytes.length != 256/8) {
                return null;
            }

            Log.d(TAG, "returning the fragment: \"" + fragment + "\"");

            return fragment;

        } catch (URISyntaxException e) {
            Log.d(TAG, "invalid url");
            e.printStackTrace();
        }

        return null;
    }

    public static String getUrlWithoutHash(String address) {
        String checksum = getChecksumFromUrl(address);

        if(checksum == null) {
            return address;
        } else {
            return address.substring(0, address.lastIndexOf('#'));
        }
    }
}
