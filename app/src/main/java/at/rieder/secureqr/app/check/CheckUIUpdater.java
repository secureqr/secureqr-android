package at.rieder.secureqr.app.check;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import at.rieder.secureqr.app.R;
import at.rieder.secureqr.app.activities.ScanQRActivity;
import at.rieder.secureqr.app.helper.ScannedDataGUIUpdater;
import at.rieder.secureqr.app.managers.ComponentAccessor;
import at.rieder.secureqr.app.managers.HistoryManager;
import at.rieder.secureqr.app.model.Content;

/**
 * Created by Thomas on 18.03.14.
 */
public class CheckUIUpdater {

    private static final String TAG = CheckUIUpdater.class.getSimpleName();

    private static final int PROGRESS_TEXT_VIEW_ID = R.id.current_status;
    private static final int PROGRESS_BAR_VIEW_ID = R.id.progressBar;
    private static final int PROGRESS_OVERALL_STATUS_VIEW_ID = R.id.overall_status;
    private static final int RESULT_TEXT_AREA = R.id.scanned_data;
    private static final int RESULT_VIEW_ICON = R.id.result_icon;
    private static final int REPORT_BUTTON_ID = R.id.button_report;

    private TextView statusMessage;
    private TextView overallStatusMessage;
    private TextView textContent;
    private ImageView resultIcon;
    private ProgressBar progressBar;
    private ScanQRActivity scanQRActivity;
    private Handler handler;

    private Button reportButton;

    private Drawable ok;
    private Drawable warning;
    private Drawable error;

    private Integer totalNumberOfChecks;
    private Content content;
    private CheckCallback callback;


    protected CheckUIUpdater(ScanQRActivity scanQRActivity, Integer totalNumberOfChecks, Content content) {
        this.scanQRActivity = scanQRActivity;
        this.statusMessage = (TextView) scanQRActivity.findViewById(PROGRESS_TEXT_VIEW_ID);
        this.overallStatusMessage = (TextView) scanQRActivity.findViewById(PROGRESS_OVERALL_STATUS_VIEW_ID);
        this.textContent = (TextView) scanQRActivity.findViewById(RESULT_TEXT_AREA);
        this.progressBar = (ProgressBar) scanQRActivity.findViewById(PROGRESS_BAR_VIEW_ID);
        this.resultIcon = (ImageView) scanQRActivity.findViewById(RESULT_VIEW_ICON);

        this.ok = scanQRActivity.getResources().getDrawable(R.drawable.ok);
        this.warning = scanQRActivity.getResources().getDrawable(R.drawable.warning);
        this.error = scanQRActivity.getResources().getDrawable(R.drawable.error);

        this.reportButton = (Button) scanQRActivity.findViewById(REPORT_BUTTON_ID);

        this.progressBar.setMax(totalNumberOfChecks);
        this.totalNumberOfChecks = totalNumberOfChecks;

        this.content = content;
        this.handler = new Handler();

        this.textContent.setText(content.toString());
        new ScannedDataGUIUpdater().execute(content);
    }

    protected synchronized void setProgress(final Integer numberOfChecksDone, final Integer errorCount) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                progressBar.setProgress(numberOfChecksDone);
            }
        });

        scanQRActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (totalNumberOfChecks.equals(0)) {
                    statusMessage.setVisibility(View.GONE);
                    overallStatusMessage.setText(R.string.no_checks_found);
                    resultIcon.setImageDrawable(warning);
                } else {
                    overallStatusMessage.setText(R.string.checks_pending);
                    statusMessage.setVisibility(View.VISIBLE);
                    resultIcon.setImageDrawable(warning);
                }

                if (numberOfChecksDone == 1) {
                    statusMessage.setText(numberOfChecksDone +
                            " " + getString(R.string.msg_check_out_of) +
                            " " + totalNumberOfChecks +
                            " " + getString(R.string.msg_completed));
                } else if (totalNumberOfChecks != 0) {
                    statusMessage.setText(numberOfChecksDone +
                            " " + getString(R.string.msg_checks_out_of) +
                            " " + totalNumberOfChecks +
                            " " + getString(R.string.msg_completed));
                }

                if (progressBar.getMax() == numberOfChecksDone && numberOfChecksDone != 0) {

                    // enable autolink
                    textContent.setAutoLinkMask(Linkify.ALL);
                    new ScannedDataGUIUpdater().execute(content);
                    textContent.setAutoLinkMask(Linkify.ALL);

                    statusMessage.setVisibility(View.GONE);
                    overallStatusMessage.setText(R.string.msg_all_checks_done);
                    resultIcon.setImageDrawable(ok);
                }

                if (errorCount > 0) {
                    Log.i(TAG, "error count is greater than 0");

                    // disable the autolink
                    textContent.setAutoLinkMask(0);
                    new ScannedDataGUIUpdater().execute(content);

                    resultIcon.setImageDrawable(error);
                    statusMessage.setVisibility(View.GONE);
                    overallStatusMessage.setText(R.string.errors_found);
                }

                if (progressBar.getMax() == numberOfChecksDone) {
                    Log.i(TAG, "all checks done");

                    reportButton.setEnabled(true);

                    HistoryManager historyManager = ComponentAccessor.getInstance().getHistoryManager();
                    if (historyManager != null && callback != null) {
                        historyManager.addScanResult(content, callback.getCheckResults());
                    } else {
                        historyManager.addScanResult(content, null);
                    }
                }
            }
        });
    }

    private String getString(int id) {
        return this.scanQRActivity.getResources().getString(id);
    }

    public void setCallback(CheckCallback callback) {
        this.callback = callback;
    }
}
