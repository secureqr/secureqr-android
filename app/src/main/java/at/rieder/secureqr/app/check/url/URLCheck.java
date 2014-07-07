package at.rieder.secureqr.app.check.url;

import at.rieder.secureqr.app.check.Check;
import at.rieder.secureqr.app.check.CheckCallback;
import at.rieder.secureqr.app.helper.HelperUtils;
import at.rieder.secureqr.app.model.Content;
import at.rieder.secureqr.app.model.ContentType;

/**
 * Created by Thomas on 25.03.14.
 */
public abstract class URLCheck implements Check {

    protected Content content;
    protected CheckCallback callback;

    public URLCheck(Content content) {
        this.content = content;
    }

    @Override
    public boolean doesVerify() {
        if (content == null || content.getContentType() != ContentType.URL || !HelperUtils.isDeviceOnline()) {
            return false;
        }

        return true;
    }

    @Override
    public void addCallback(CheckCallback callback) {
        this.callback = callback;
    }

    @Override
    public abstract String getPrettyName();

    @Override
    public abstract void run();
}
