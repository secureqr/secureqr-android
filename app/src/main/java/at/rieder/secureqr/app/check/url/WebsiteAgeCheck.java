package at.rieder.secureqr.app.check.url;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import at.rieder.secureqr.app.R;
import at.rieder.secureqr.app.helper.HelperUtils;
import at.rieder.secureqr.app.managers.URLResolver;
import at.rieder.secureqr.app.model.CheckResult;
import at.rieder.secureqr.app.model.Content;

/**
 * Created by Thomas on 25.03.14.
 */
public class WebsiteAgeCheck extends URLCheck {

    private static final String TAG = WebsiteAgeCheck.class.getSimpleName();

    private static final String HOSTNAME = "archive.org";
    private static final String CONTEXT_PATH = "/wayback/available";

    // everything younger than 25 days is strange
    private static final Integer AGE_THRESHOLD = 25;

    public WebsiteAgeCheck(Content content) {
        super(content);
    }

    @Override
    public String getPrettyName() {
        return "Website Age";
    }

    @Override
    public void run() {

        try {
            JSONObject response = this.getWaybackResponse(URLResolver.resolveRedirectsOfUrl(content.getURL()));

            Log.d(TAG, "got the json response: " + response);

            if (response != null && response.getJSONObject("archived_snapshots") != null) {
                JSONObject snapshots = response.getJSONObject("archived_snapshots");
                JSONObject closest = snapshots.getJSONObject("closest");

                if (closest != null) {
                    String age = closest.getString("timestamp");

                    if (age != null) {
                        SimpleDateFormat parsedDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                        SimpleDateFormat printFormat = new SimpleDateFormat("dd MMM yyyy");

                        Date date = parsedDateFormat.parse(age);

                        Log.i(TAG, "the oldest date is: " + date);

                        // stackoverflow 4tw
                        int diffInDays = (int) ((new Date().getTime() - date.getTime()) / (1000 * 60 * 60 * 24));

                        Log.i(TAG, "the difference in days is: " + diffInDays);


                        if (diffInDays > AGE_THRESHOLD) {
                            callback.notify(new CheckResult(true, HelperUtils.getContext().getString(R.string.msg_website_age_ok) + " " + printFormat.format(date), this));
                        } else {
                            callback.notify(new CheckResult(false, HelperUtils.getContext().getString(R.string.msg_website_age_too_young) + " " + printFormat.format(date), this));
                        }

                        return;
                    }
                }
            }

            callback.notify(new CheckResult(false, HelperUtils.getContext().getString(R.string.msg_website_age_not_listed), this));

        } catch (IOException e) {
            Log.w(TAG, "error contacting server");
            callback.notify(new CheckResult(false, HelperUtils.getContext().getString(R.string.msg_website_age_error_contact), this));
            e.printStackTrace();
        } catch (URISyntaxException e) {
            Log.w(TAG, "invalid url");
            callback.notify(new CheckResult(false, HelperUtils.getContext().getString(R.string.msg_website_age_error_contact), this));
            e.printStackTrace();
        } catch (JSONException e) {
            Log.w(TAG, "invalid response");
            callback.notify(new CheckResult(false, HelperUtils.getContext().getString(R.string.msg_website_age_not_listed), this));
            e.printStackTrace();
        } catch (ParseException e) {
            Log.w(TAG, "invalid date response");
            callback.notify(new CheckResult(false, HelperUtils.getContext().getString(R.string.msg_website_age_error_contact), this));
            e.printStackTrace();
        }
    }

    private JSONObject getWaybackResponse(String address) throws IOException, URISyntaxException, JSONException {
        StringBuilder requestArguments = new StringBuilder();

        requestArguments.append(URLEncoder.encode("url", "UTF-8"));
        requestArguments.append("=");
        requestArguments.append(URLEncoder.encode(new URL(address).getHost(), "UTF-8"));
        requestArguments.append("&");

        requestArguments.append(URLEncoder.encode("timestamp", "UTF-8"));
        requestArguments.append("=");
        requestArguments.append(URLEncoder.encode("19000101", "UTF-8"));

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

        return new JSONObject(sb.toString());
    }
}
