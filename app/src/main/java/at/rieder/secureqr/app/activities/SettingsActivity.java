package at.rieder.secureqr.app.activities;

import android.app.Activity;
import android.os.Bundle;

/**
 * Created by Thomas on 18.03.14.
 */
public class SettingsActivity extends Activity {

    // general settings
    public static final String GENERAL_PLAY_BEEP = "preferences_beep";
    public static final String GENERAL_VIBRATE = "preferences_vibrate";
    public static final String GENERAL_COPY_CLIPBOARD = "preferences_copy_clipboard";

    // security checks
    public static final String CHECK_SAFEBROWSING = "preferences_check_safebrowsing";
    public static final String CHECK_PHISHTANK = "preferences_check_phishtank";
    public static final String CHECK_ERRORRATE = "preferences_check_errorrate";
    public static final String CHECK_METRICS_DNS_AGE = "preferences_check_dns_age";
    public static final String CHECK_METRICS_REDIRECT_COUNT = "preferences_check_redirect_count";
    public static final String CHECK_METRICS_IF_PUBLIC = "preferences_check_if_public";
    public static final String CHECK_METRICS_WEBSITE_AGE = "preferences_check_website_age";
    public static final String CHECK_SIGNATURE = "preferences_check_signature";

    // enabled formats
    public static final String FORMAT_QR = "preferences_scan_qr";
    public static final String FORMAT_AZTEC = "preferences_scan_aztec";
    public static final String FORMAT_1D_PRODUCT = "preferences_scan_1d_product";
    public static final String FORMAT_1D_INDUSTRIAL = "preferences_scan_1d_industrial";
    public static final String FORMAT_DATA_MATRIX = "preferences_scan_data_matrix";
    public static final String FORMAT_PDF417 = "preferences_scan_pdf417";

    // advanced
    public static final String ADVANCED_SIGNATURE_HOST = "preferences_signature_host";
    public static final String ADVANCED_SIGNATURE_PORT = "preferences_signature_port";
    public static final String ADVANCED_SIGNATURE_PROTOCOL = "preferences_signature_protocol";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }
}
