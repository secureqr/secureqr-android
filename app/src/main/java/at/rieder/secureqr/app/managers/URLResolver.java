package at.rieder.secureqr.app.managers;

import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Thomas on 23.03.14.
 */
public class URLResolver {

    private static final String TAG = URLResolver.class.getSimpleName();
    private static Integer MAX_DEPTH = 5;

    private static Integer lastQueryDepth = Integer.MAX_VALUE;

    private static final Map<String, String> urlCache;
    private static final List<Integer> redirectResponseCodes;

    static {
        urlCache = new HashMap<String, String>();
        redirectResponseCodes = new ArrayList<Integer>();
        redirectResponseCodes.add(301);
        redirectResponseCodes.add(302);
        redirectResponseCodes.add(303);
        redirectResponseCodes.add(307);
    }

    private URLResolver() {

    }

    public synchronized static String resolveSingleRedirectOfUrl(String address) {
        Integer oldMax = MAX_DEPTH;
        MAX_DEPTH = 1;
        String ret = resolveRedirectsOfUrl(address, 0);
        MAX_DEPTH = oldMax;
        return ret;
    }

    public synchronized static String resolveRedirectsOfUrl(String address) {
        return resolveRedirectsOfUrl(address, 0);
    }

    private synchronized static String resolveRedirectsOfUrl(String address, Integer level) {
        Log.d(TAG, "attemping to resolve the url: " + address);

        if (MAX_DEPTH.equals(level)) {
            Log.w(TAG, "warning: the url resolver reached the maximum resolval depth");
            return address;
        }

        if (urlCache.containsKey(address)) {
            Log.d(TAG, "using url cache");
            if (urlCache.get(address) == null) {
                return address;
            }

            return resolveRedirectsOfUrl(urlCache.get(address), level + 1);
        }

        URL url = null;
        try {
            url = new URL(address);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.connect();

            String expandedURL = null;
            connection.getInputStream().close();

            Log.d(TAG, "the response code is: " + connection.getResponseCode());

            if (connection.getHeaderField("Location") != null && redirectResponseCodes.contains(connection.getResponseCode())) {
                expandedURL = connection.getHeaderField("Location");

                // check if new url is valid
                new URL(expandedURL);
            } else {
                urlCache.put(address, null);
                lastQueryDepth = level;
                return address;
            }

            if (expandedURL.equals(address)) {
                lastQueryDepth = level;
                return address;
            } else {
                Log.d(TAG, "adding the following to the url cache: " + address + " => " + expandedURL);
                urlCache.put(address, expandedURL);
                return resolveRedirectsOfUrl(expandedURL, level + 1);
            }

        } catch (IOException e) {
            Log.d(TAG, "the url resolver got an invalid url: " + address);
            e.printStackTrace();
            lastQueryDepth = level;
            return address;
        }
    }

    public static synchronized boolean hasCachedUrl(String address) {
        return urlCache.get(address) != null;
    }

    public static synchronized String getCachedUrl (String address) {
        String resolvedAddress = urlCache.get(address);

        if (urlCache.get(resolvedAddress) != null) {
            return getCachedUrl(resolvedAddress);
        } else {
            return resolvedAddress;
        }
    }

    public static Integer getLastQueryDepth() {
        return lastQueryDepth;
    }
}
