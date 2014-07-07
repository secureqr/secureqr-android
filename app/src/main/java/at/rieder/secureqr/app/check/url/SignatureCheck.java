package at.rieder.secureqr.app.check.url;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import at.rieder.secureqr.app.R;
import at.rieder.secureqr.app.activities.SettingsActivity;
import at.rieder.secureqr.app.helper.HelperUtils;
import at.rieder.secureqr.app.managers.URLResolver;
import at.rieder.secureqr.app.model.CheckResult;
import at.rieder.secureqr.app.model.Content;

/**
 * Created by thomasrieder on 09.04.14.
 */
public class SignatureCheck extends URLCheck {

    private static final String TAG = SignatureCheck.class.getSimpleName();

    private static final String DEFAULT_HOSTNAME = "qr.rieder.io";
    private static final String DEFAULT_PROTOCOL = "https";
    private static final Integer DEFAULT_PORT = 443;

    private static final String CONTEXT_PATH = "/verify";
    private static final Integer CONNECT_TIMEOUT = 5000;

    private String hostname;
    private Integer port;
    private String protocol;


    public SignatureCheck(Content content) {
        super(content);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(HelperUtils.getScanQRActivity());
        hostname = prefs.getString(SettingsActivity.ADVANCED_SIGNATURE_HOST, DEFAULT_HOSTNAME);
        protocol = prefs.getString(SettingsActivity.ADVANCED_SIGNATURE_PROTOCOL, DEFAULT_PROTOCOL);
        port = Integer.parseInt(prefs.getString(SettingsActivity.ADVANCED_SIGNATURE_PORT, String.valueOf(DEFAULT_PORT)));

        if(! (port > 0) || ! (port <= 65535)) {
            port = DEFAULT_PORT;
        }

        if ("Secure HTTP".equals(protocol)) {
            protocol = "https";
        } else if ("HTTP".equals(protocol)) {
            protocol = "http";
        } else {
            protocol = DEFAULT_PROTOCOL;
        }
    }

    private String getSha256AsciiArmored(String data) {
        try {
            Log.i(TAG, "calculating the checksum of the url: \"" + data + "\"");

            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.reset();

            byte[] stringBytes = messageDigest.digest(data.getBytes("UTF-8"));
            StringBuffer stringBuffer = new StringBuffer();

            stringBuffer.append(Base64.encodeToString(stringBytes, Base64.URL_SAFE));

            Log.d(TAG, "return the ascii armored hash: " + stringBuffer.toString());

            return stringBuffer.toString().trim();

        } catch (UnsupportedEncodingException e) {
            Log.d(TAG, "error calculating hash from string: invalid encoding");
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            Log.d(TAG, "error calculation hash from string: couldn't find algorithm");
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public String getPrettyName() {
        return "QR Signature Check";
    }

    @Override
    public void run() {
        Log.d(TAG, "signature check is run");

        String hash = HelperUtils.getChecksumFromUrl(content.getURL());

        if(hash == null) {
            Log.i(TAG, "no hash found inside url");
            callback.notify(new CheckResult(true, HelperUtils.getContext().getString(R.string.msg_signature_not_found), this));
            return;
        }

        Log.d(TAG, "extracted the hash: " + hash);


        String urlWithoutFragment = HelperUtils.getUrlWithoutHash(content.getURL());
        String resolvedUrl = URLResolver.resolveSingleRedirectOfUrl(urlWithoutFragment);
        String urlHash = getSha256AsciiArmored(resolvedUrl);
        String urlHashWithoutTrailingSlash = getSha256AsciiArmored(resolvedUrl.substring(0, resolvedUrl.length() - 1));

        if (urlHash == null || urlHashWithoutTrailingSlash == null) {
            Log.i(TAG, "couldnt calculate the hash of the url");
            callback.notify(new CheckResult(false, HelperUtils.getContext().getString(R.string.msg_signature_mismatch), this));
            return;
        }

        if (urlHash.equals(hash) || urlHashWithoutTrailingSlash.equals(hash)) {
            Log.i(TAG, "the hash matches");
        } else {
            Log.w(TAG, "the hash doesnt match");
            callback.notify(new CheckResult(false, HelperUtils.getContext().getString(R.string.msg_signature_mismatch), this));
            return;
        }

        if (verifyWithServer(resolvedUrl, hash)
                || verifyWithServer(resolvedUrl.substring(0, resolvedUrl.length() - 1), hash)) {
            callback.notify(new CheckResult(true, HelperUtils.getContext().getString(R.string.msg_signature_ok) + hostname, this));
        } else {
            callback.notify(new CheckResult(false, HelperUtils.getContext().getString(R.string.msg_signature_server_fail), this));
        }
    }

    private boolean verifyWithServer(String url, String calculatedHash) {
        try {
            URI uri = new URI(protocol, null, hostname, port, CONTEXT_PATH, null, null);
            URL serverUrl = uri.toURL();

            HttpURLConnection httpURLConnection = (HttpURLConnection) serverUrl.openConnection();
            httpURLConnection.setConnectTimeout(CONNECT_TIMEOUT);
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setDoOutput(true);

            // build parameters
            StringBuilder parameters = new StringBuilder();
            parameters.append("hash=");
            parameters.append(URLEncoder.encode(calculatedHash, "UTF-8"));
            parameters.append("&url=");
            parameters.append(URLEncoder.encode(url, "UTF-8"));

            Log.d(TAG, "the parameters are: " + parameters);

            // send form data
            DataOutputStream os = new DataOutputStream(httpURLConnection.getOutputStream());
            os.writeBytes(parameters.toString());
            os.flush();
            os.close();

            // read response
            BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            Log.d(TAG, "the response is: " + response.toString());

            JSONObject jsonResponse = new JSONObject(response.toString());

            Boolean successful = jsonResponse.getBoolean("successful");

            if (successful != null && successful) {
                Date date = new Date(Long.parseLong(jsonResponse.getString("date")));

                if (date != null) {
                    Log.i(TAG, "the qr code was first created on: " + date.toString());
                    return true;
                }
            }

        } catch (URISyntaxException e) {
            Log.d(TAG, "got an invalid server url");
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "error communiting with signature server");
            e.printStackTrace();
        } catch (JSONException e) {
            Log.d(TAG, "error parsing signature server response");
            e.printStackTrace();
        }

        return false;
    }
}
