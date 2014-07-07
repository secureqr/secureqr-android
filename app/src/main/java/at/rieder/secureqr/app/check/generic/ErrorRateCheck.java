package at.rieder.secureqr.app.check.generic;

import android.content.Context;
import android.util.Log;

import com.google.zxing.ResultMetadataType;
import com.google.zxing.qrcode.SecureQRMetadataPatch;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import at.rieder.secureqr.app.R;
import at.rieder.secureqr.app.check.Check;
import at.rieder.secureqr.app.check.CheckCallback;
import at.rieder.secureqr.app.helper.HelperUtils;
import at.rieder.secureqr.app.model.CheckResult;
import at.rieder.secureqr.app.model.Content;

/**
 * Created by Thomas on 25.03.14.
 */
public class ErrorRateCheck implements Check {

    private Content content;
    private CheckCallback callback;

    private static final String TAG = ErrorRateCheck.class.getSimpleName();

    private Map<String, Double> errorCorrectionThreshholds;
    private Map<String, Integer> errorLimitMap;

    {
        // if more than ~2/3 of the code words are corrupt, it's suspicious
        this.errorCorrectionThreshholds = new HashMap<String, Double>();
        this.errorCorrectionThreshholds.put("L", 7.0);
        this.errorCorrectionThreshholds.put("M", 12.5);
        this.errorCorrectionThreshholds.put("Q", 22.5);
        this.errorCorrectionThreshholds.put("H", 27.5);

        this.errorLimitMap = new HashMap<String, Integer>();
        this.errorLimitMap.put("L", 7);
        this.errorLimitMap.put("M", 15);
        this.errorLimitMap.put("Q", 25);
        this.errorLimitMap.put("H", 30);
    }


    public ErrorRateCheck(Content content) {
        this.content = content;
    }

    @Override
    public boolean doesVerify() {
        return true;
    }

    @Override
    public void addCallback(CheckCallback callback) {
        this.callback = callback;
    }

    @Override
    public String getPrettyName() {
        return "Error Rate";
    }

    @Override
    public void run() {
        Context context = HelperUtils.getContext();

        if (content == null || content.getMetadata() == null) {
            callback.notify(new CheckResult(false, context.getString(R.string.msg_error_couldnt_calculate), this));
            return;
        }

        Integer numberOfErrors = com.google.zxing.qrcode.SecureQRMetadataPatch.getNumberOfErrors();
        Integer numberOfCodeWords = com.google.zxing.qrcode.SecureQRMetadataPatch.getNumberOfCodeWords();
        String errorCorrectionLevel = content.getMetadata().get(ResultMetadataType.ERROR_CORRECTION_LEVEL).toString();
        Double acceptableErrorRate = this.errorCorrectionThreshholds.get(errorCorrectionLevel);
        Integer errorLimit = this.errorLimitMap.get(errorCorrectionLevel);

        if (numberOfCodeWords > 0 && acceptableErrorRate != null
                && errorCorrectionLevel != null && errorLimit != null) {

            if (numberOfErrors <= 0) {
                callback.notify(new CheckResult(true,
                        context.getString(R.string.msg_error_no_errors),
                        this
                ));
                return;
            }

            Double errorRateInPercent = ((double) numberOfErrors / (double) numberOfCodeWords) * 100;

            Log.d(TAG, "the number of errors is: " + numberOfErrors);
            Log.d(TAG, "the number of code words is: " + numberOfCodeWords);
            Log.d(TAG, "the error correction level is: " + errorCorrectionLevel);
            Log.d(TAG, "the error rate is: " + errorRateInPercent + "%");

            DecimalFormat df = new DecimalFormat("##.##");

            if (errorRateInPercent > acceptableErrorRate) {
                callback.notify(new CheckResult(false, context.getString(R.string.msg_error_tampered)
                        + " " + df.format(errorRateInPercent) + "% (" + context.getString(R.string.msg_error_limit)
                        + ": " + errorLimit + "%)", this));
            } else {
                callback.notify(new CheckResult(true,
                        context.getString(R.string.msg_error_within_norm) + ": " + df.format(errorRateInPercent)
                                + "% (" + context.getString(R.string.msg_error_limit) +
                                ": " + errorLimit + "%)",
                        this
                ));
            }

            // reset error count after scan
            SecureQRMetadataPatch.setNumberOfErrors(0);

        } else {
            callback.notify(new CheckResult(false, context.getString(R.string.msg_error_couldnt_calculate), this));
        }
    }
}
