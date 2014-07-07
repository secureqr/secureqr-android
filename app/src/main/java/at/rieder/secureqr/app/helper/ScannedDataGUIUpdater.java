package at.rieder.secureqr.app.helper;

import android.os.AsyncTask;
import android.widget.TextView;

import at.rieder.secureqr.app.R;
import at.rieder.secureqr.app.managers.URLResolver;
import at.rieder.secureqr.app.model.Content;
import at.rieder.secureqr.app.model.ContentType;

/**
 * Created by thomasrieder on 10.04.14.
 */
public class ScannedDataGUIUpdater extends AsyncTask<Content, Object, Object> {

    private static final int RESULT_TEXT_AREA = R.id.scanned_data;

    @Override
    protected Object doInBackground(final Content... contents) {

        HelperUtils.getScanQRActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Content content = contents[0];

                if (content != null && content.getContentType() == ContentType.URL) {
                    TextView scannedData = (TextView) HelperUtils.getScanQRActivity().findViewById(RESULT_TEXT_AREA);

                    try {
                        /* since no network activity is allowed on the main-threadm we want to give the security checks
                           time to start resolving the url - there are no problems afterwards because the methods ARE
                           synchronized on *this* (which means as long as we don't query it *first*, it will be all right ) */
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                         // yolo
                    }

                    if(HelperUtils.isDeviceOnline() && URLResolver.hasCachedUrl(contents[0].getURL())) {
                        scannedData.setText(HelperUtils.getUrlWithoutHash(URLResolver.getCachedUrl(contents[0].getURL())));
                    } else {
                        scannedData.setText(HelperUtils.getUrlWithoutHash(contents[0].getURL()));
                    }
                }
            }
        });

        return null;
    }
}
