package at.rieder.secureqr.app.check.url;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import at.rieder.secureqr.app.R;
import at.rieder.secureqr.app.helper.HelperUtils;
import at.rieder.secureqr.app.managers.URLResolver;
import at.rieder.secureqr.app.model.CheckResult;
import at.rieder.secureqr.app.model.Content;

/**
 * Created by Thomas on 25.03.14.
 */
public class PhishtankCheck extends URLCheck {

    private static final String TAG = PhishtankCheck.class.getSimpleName();
    private static final String BASE_URL = "http://checkurl.phishtank.com/checkurl/";
    private static final String RESPONSE_TYPE = "json";


    public PhishtankCheck(Content content) {
        super(content);
    }

    @Override
    public String getPrettyName() {
        return "Phishtank";
    }

    @Override
    public void run() {
        Context context = HelperUtils.getContext();

        if (context == null) {
            Log.w(TAG, "warning: context is null");
        }

        try {
            boolean result1 = this.getInDatabase(this.getPhishtankResponse(HelperUtils.getUrlWithoutHash(content.getURL())));
            boolean result2 = this.getInDatabase(this.getPhishtankResponse(URLResolver.resolveRedirectsOfUrl(content.getURL())));

            boolean inDatabase = result1 || result2;

            if (inDatabase) {
                callback.notify(new CheckResult(false, context.getString(R.string.msg_phishtank_listed), this));
            } else {
                callback.notify(new CheckResult(true, context.getString(R.string.msg_phishtank_unlisted), this));
            }

        } catch (IOException e) {
            Log.i(TAG, "error communicating with the phishtank server");
            callback.notify(new CheckResult(false, context.getString(R.string.msg_phishtank_unavailable), this));
            e.printStackTrace();
        } catch (JSONException e) {
            Log.i(TAG, "error parsing json response");
            callback.notify(new CheckResult(false, context.getString(R.string.msg_phishtank_unavailable), this));
            e.printStackTrace();
        }
    }

    private boolean getInDatabase(JSONObject jsonObject) throws JSONException {
        if (jsonObject != null) {
            JSONObject meta = jsonObject.getJSONObject("meta");

            if (meta != null) {
                String status = meta.getString("status");

                if ("success".equals(status)) {
                    JSONObject result = jsonObject.getJSONObject("results");

                    if (result != null) {
                        return result.getBoolean("in_database");
                    }
                }
            }
        }
        return false;
    }

    private JSONObject getPhishtankResponse(String address) throws IOException, JSONException {
        URL url = new URL(BASE_URL);

        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setDoOutput(true);

        // build parameters
        StringBuilder parameters = new StringBuilder();
        parameters.append("format=");
        parameters.append(RESPONSE_TYPE);
        parameters.append("&url=");
        parameters.append(URLEncoder.encode(address, "UTF-8"));

        Log.d(TAG, "the resolved url is : " + address);
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

        return new JSONObject(response.toString());
    }
}
