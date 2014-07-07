package at.rieder.secureqr.app.check.url;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import at.rieder.secureqr.app.R;
import at.rieder.secureqr.app.helper.HelperUtils;
import at.rieder.secureqr.app.managers.URLResolver;
import at.rieder.secureqr.app.model.CheckResult;
import at.rieder.secureqr.app.model.Content;

/**
 * Created by Thomas on 25.03.14.
 */
public class SearchEngineCheck extends URLCheck {

    private static final String TAG = SearchEngineCheck.class.getSimpleName();

    public SearchEngineCheck(Content content) {
        super(content);
    }

    @Override
    public String getPrettyName() {
        return "Search Engine Filtering";
    }

    @Override
    public void run() {
        try {
            URL url = new URL(URLResolver.resolveRedirectsOfUrl(content.getURL()));
            String robots = getRobotsTxt(url);

            Log.d(TAG, "the robots.txt is: " + robots);

            BufferedReader in = new BufferedReader(new StringReader(robots));

            String line = in.readLine();
            String identifier = "";
            boolean found = false;

            while (line != null && !found) {
                if (line.startsWith("User-agent:")) {
                    identifier = line.substring("User-agent:".length()).trim();
                    Log.d(TAG, "the new identifier is: " + identifier);
                } else if (line.startsWith("Disallow:")) {
                    String target = line.substring("Disallow:".length()).trim();
                    Log.d(TAG, "the target is: " + target);

                    if ("/".equals(target) && "*".equals(identifier)) {
                        Log.d(TAG, "all search engines are blocked");
                        found = true;
                        callback.notify(new CheckResult(false, HelperUtils.getContext().getString(R.string.msg_is_public_no), this));
                    }
                }
                line = in.readLine();
            }
            in.close();

            if (!found) {
                callback.notify(new CheckResult(true, HelperUtils.getContext().getString(R.string.msg_if_public_yes), this));
            }

        } catch (MalformedURLException e) {
            Log.w(TAG, "malformed url");
            callback.notify(new CheckResult(false, HelperUtils.getContext().getString(R.string.msg_if_public_error_contact), this));
            e.printStackTrace();
        } catch (IOException e) {
            Log.w(TAG, "error getting robots txt");
            callback.notify(new CheckResult(true, HelperUtils.getContext().getString(R.string.msg_if_public_yes), this));
            e.printStackTrace();
        } catch (URISyntaxException e) {
            Log.w(TAG, "malformed url");
            callback.notify(new CheckResult(false, HelperUtils.getContext().getString(R.string.msg_if_public_error_contact), this));
            e.printStackTrace();
        }

    }

    private static String getRobotsTxt(URL queryUrl) throws URISyntaxException, IOException {
        URI uri = new URI(queryUrl.getProtocol(), null, queryUrl.getHost(), queryUrl.getPort(), "/robots.txt", null, null);
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
            sb.append('\n');
        }
        in.close();

        return sb.toString();
    }
}
