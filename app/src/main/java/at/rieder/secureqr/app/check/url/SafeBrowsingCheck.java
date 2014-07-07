package at.rieder.secureqr.app.check.url;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;

import at.rieder.secureqr.app.R;
import at.rieder.secureqr.app.helper.HelperUtils;
import at.rieder.secureqr.app.managers.URLResolver;
import at.rieder.secureqr.app.model.CheckResult;
import at.rieder.secureqr.app.model.Content;

/**
 * Created by Thomas on 23.03.14.
 */
public class SafeBrowsingCheck extends URLCheck {

    private static final String HOSTNAME = "sb-ssl.google.com";
    private static final String CONTEXT_PATH = "/safebrowsing/api/lookup";
    private static final String APIKEY = "ABQIAAAAyaoLBeXR131f7VP3Fo7HrRSbaKnkI0qqHEZtNDp3JkxasopW3A";
    private static final String PROTOCOL_VERSION = "3.0";

    private static final String TAG = SafeBrowsingCheck.class.getSimpleName();

    public SafeBrowsingCheck(Content content) {
        super(content);
    }

    @Override
    public String getPrettyName() {
        return "Google Safe Browsing";
    }

    @Override
    public void run() {
        Context context = HelperUtils.getContext();

        if (context == null) {
            Log.w(TAG, "warning: context is null");
        }

        try {
            int responseCode = this.getSafeBrowsingResult(content.getURL());
            int responseCode2 = this.getSafeBrowsingResult(URLResolver.resolveRedirectsOfUrl(content.getURL()));

            if (responseCode != responseCode2) {
                if (responseCode2 != 204 && responseCode != 200) {
                    responseCode = responseCode2;
                }
            }

            switch (responseCode) {
                case 200:
                    callback.notify(new CheckResult(
                            false,
                            context.getString(R.string.msg_safe_browsing_listed),
                            this));
                    break;
                case 204:
                    callback.notify(new CheckResult(
                            true,
                            context.getString(R.string.msg_safe_browsing_unlisted),
                            this));
                    break;
                case 400:
                    callback.notify(new CheckResult(
                            false,
                            context.getString(R.string.msg_safe_browsing_invalid_url),
                            this));
                    break;
                case 401:
                    callback.notify(new CheckResult(
                            false,
                            context.getString(R.string.msg_safe_browsing_unavailable),
                            this));
                    break;
                case 503:
                    callback.notify(new CheckResult(
                            false,
                            context.getString(R.string.msg_safe_browsing_unavailable),
                            this));
                    break;
                default:
                    Log.i(TAG, "got an unexpected response code: " + responseCode);
                    callback.notify(new CheckResult(
                            false,
                            context.getString(R.string.msg_safe_browsing_unavailable),
                            this));
            }

        } catch (URISyntaxException e) {
            callback.notify(new CheckResult(false, context.getString(R.string.msg_safe_browsing_unavailable), this));
            Log.i(TAG, "invalid url");
            e.printStackTrace();
        } catch (IOException e) {
            callback.notify(new CheckResult(false, context.getString(R.string.msg_safe_browsing_unavailable), this));
            Log.i(TAG, "error sending request");
            e.printStackTrace();
        } catch (Exception e) {
            // ugly
            callback.notify(new CheckResult(false, context.getString(R.string.msg_safe_browsing_unavailable), this));
            Log.i(TAG, "something else failed horribly");
            e.printStackTrace();
        }
    }

    private int getSafeBrowsingResult(String address) throws IOException, URISyntaxException {
        StringBuilder requestArguments = new StringBuilder();

        requestArguments.append(URLEncoder.encode("client", "UTF-8"));
        requestArguments.append("=");
        requestArguments.append(URLEncoder.encode("secureqrreader", "UTF-8"));
        requestArguments.append("&");

        requestArguments.append(URLEncoder.encode("apikey", "UTF-8"));
        requestArguments.append("=");
        requestArguments.append(URLEncoder.encode(APIKEY, "UTF-8"));
        requestArguments.append("&");

        requestArguments.append(URLEncoder.encode("appver", "UTF-8"));
        requestArguments.append("=");
        requestArguments.append(URLEncoder.encode(HelperUtils.getAppVersion(), "UTF-8"));
        requestArguments.append("&");

        requestArguments.append(URLEncoder.encode("pver", "UTF-8"));
        requestArguments.append("=");
        requestArguments.append(URLEncoder.encode(PROTOCOL_VERSION, "UTF-8"));
        requestArguments.append("&");

        requestArguments.append(URLEncoder.encode("url", "UTF-8"));
        requestArguments.append("=");
        requestArguments.append(URLEncoder.encode(address, "UTF-8"));
        requestArguments.append("&");

        URI uri = new URI("https", null, HOSTNAME, 443, CONTEXT_PATH, requestArguments.toString(), null);
        Log.d(TAG, "The uri is: " + uri.toString());

        URL url = uri.toURL();

        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setRequestMethod("GET");

        int responseCode = httpURLConnection.getResponseCode();
        Log.d(TAG, "Sending GET request");
        Log.d(TAG, "Response code: " + responseCode);

        BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
        String line;
        StringBuffer sb = new StringBuffer();

        while ((line = in.readLine()) != null) {
            sb.append(line);
        }
        in.close();

        Log.d(TAG, "The response body is: " + sb.toString() == null ? "empty" : sb.toString());

        return responseCode;
    }
}
