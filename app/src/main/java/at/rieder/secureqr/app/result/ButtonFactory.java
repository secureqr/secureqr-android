package at.rieder.secureqr.app.result;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Button;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import at.rieder.secureqr.app.R;
import at.rieder.secureqr.app.helper.HelperUtils;
import at.rieder.secureqr.app.managers.URLResolver;
import at.rieder.secureqr.app.model.Content;

/**
 * Created by Thomas on 25.03.14.
 */
public class ButtonFactory {

    private static String DEFAULT_GOOGLE_SEARCH_URL = "https://www.google.at/#q=";
    private static Integer OPEN_BUTTON_ID = R.id.button_open;

    public static Intent buildOpenButton(Content data) {
        Intent intent = null;

        try {
            Button openButton = (Button) HelperUtils.getScanQRActivity().findViewById(OPEN_BUTTON_ID);
            Context context = HelperUtils.getContext();

            Content content = data;

            switch (content.getContentType()) {
                case URL:
                    intent = new Intent(android.content.Intent.ACTION_VIEW,
                            Uri.parse(HelperUtils.getUrlWithoutHash(content.getURL())));
                    openButton.setText(context.getString(R.string.button_open));
                    break;
                case TEXT:
                    intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse(DEFAULT_GOOGLE_SEARCH_URL + URLEncoder.encode(content.toString(), "UTF-8")));
                    openButton.setText(context.getString(R.string.msg_search_web));
                    break;
            }
        } catch (UnsupportedEncodingException e) {
            return null;
        }
        return intent;
    }
}
