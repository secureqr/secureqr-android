package at.rieder.secureqr.app.check.url;

import android.util.Log;

import at.rieder.secureqr.app.R;
import at.rieder.secureqr.app.helper.HelperUtils;
import at.rieder.secureqr.app.managers.URLResolver;
import at.rieder.secureqr.app.model.CheckResult;
import at.rieder.secureqr.app.model.Content;

/**
 * Created by Thomas on 25.03.14.
 */
public class RedirectCountCheck extends URLCheck {

    private static final String TAG = RedirectCountCheck.class.getSimpleName();
    private static final Integer REDIRECT_LIMIT = 5;

    public RedirectCountCheck(Content content) {
        super(content);
    }

    @Override
    public String getPrettyName() {
        return "Number of redirects";
    }

    @Override
    public void run() {

        URLResolver.resolveRedirectsOfUrl(content.getURL());

        Integer lastQueryDepth = URLResolver.getLastQueryDepth();
        Log.d(TAG, "the number of redirects is: " + lastQueryDepth);

        if (lastQueryDepth > REDIRECT_LIMIT) {
            callback.notify(new CheckResult(false, HelperUtils.getContext().getString(R.string.msg_redirect_high_number) + lastQueryDepth, this));
        } else {
            callback.notify(new CheckResult(true, HelperUtils.getContext().getString(R.string.msg_redirect_low_number) + lastQueryDepth, this));
        }
    }
}
