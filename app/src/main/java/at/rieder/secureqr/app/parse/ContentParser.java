package at.rieder.secureqr.app.parse;

import com.google.zxing.Result;

import java.net.MalformedURLException;
import java.net.URL;

import at.rieder.secureqr.app.model.Content;
import at.rieder.secureqr.app.model.ContentType;

/**
 * Created by Thomas on 18.03.14.
 */
public class ContentParser {

    public static Content parseScanResult(Result rawResult) {
        Content content = new Content();

        content.setBarcodeFormat(rawResult.getBarcodeFormat());
        content.setMetadata(rawResult.getResultMetadata());

        String text = rawResult.getText();
        if (isURL(text)) {
            content.setContentType(ContentType.URL);
            content.setURL(text);
        } else {
            content.setContentType(ContentType.TEXT);
            content.setText(text);
        }

        // TODO add additional content types

        return content;
    }

    private static boolean isURL(String text) {
        try {
            URL url = new URL(text);
            return true;
        } catch (MalformedURLException e) {
            if (text.startsWith("www.")) {
                try {
                    URL url = new URL("http://" + text);
                    // if we add "http://" it is an valid url
                    return true;
                } catch (MalformedURLException e1) {
                    return false;
                }
            } else {
                return false;
            }
        }
    }
}
