package at.rieder.secureqr.app.model;

import android.util.Base64;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.ResultMetadataType;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import at.rieder.secureqr.app.helper.HelperUtils;

/**
 * Created by Thomas on 18.03.14.
 */
public class Content implements Serializable {

    private String URL;
    private String text;

    private ContentType contentType;

    private Map<ResultMetadataType, Object> metadata;
    private BarcodeFormat barcodeFormat;

    public ContentType getContentType() {
        return contentType;
    }

    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public Map<ResultMetadataType, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<ResultMetadataType, Object> metadata) {
        this.metadata = metadata;
    }

    public BarcodeFormat getBarcodeFormat() {
        return barcodeFormat;
    }

    public void setBarcodeFormat(BarcodeFormat barcodeFormat) {
        this.barcodeFormat = barcodeFormat;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public String toString() {
        switch (contentType) {
            case URL:
                return getURLWithoutHash(URL);
            case TEXT:
                return text;
            default:
                return "";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Content content = (Content) o;

        if (URL != null ? !URL.equals(content.URL) : content.URL != null) return false;
        if (barcodeFormat != content.barcodeFormat) return false;
        if (contentType != content.contentType) return false;
        if (metadata != null ? !metadata.equals(content.metadata) : content.metadata != null)
            return false;
        if (text != null ? !text.equals(content.text) : content.text != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = URL != null ? URL.hashCode() : 0;
        result = 31 * result + (text != null ? text.hashCode() : 0);
        result = 31 * result + (contentType != null ? contentType.hashCode() : 0);
        result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
        result = 31 * result + (barcodeFormat != null ? barcodeFormat.hashCode() : 0);
        return result;
    }

    private String getURLWithoutHash(String url) {
        return HelperUtils.getUrlWithoutHash(url);
    }
}
