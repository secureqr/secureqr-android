package at.rieder.secureqr.app.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.google.zxing.Result;

import java.io.IOException;
import java.util.Collection;

import at.rieder.secureqr.app.R;
import at.rieder.secureqr.app.camera.CameraManager;
import at.rieder.secureqr.app.check.CheckRunner;
import at.rieder.secureqr.app.check.CheckRunnerFactory;
import at.rieder.secureqr.app.helper.BailOutListener;
import at.rieder.secureqr.app.helper.HelperUtils;
import at.rieder.secureqr.app.managers.ClipboardInterface;
import at.rieder.secureqr.app.managers.ComponentAccessor;
import at.rieder.secureqr.app.managers.HistoryManager;
import at.rieder.secureqr.app.model.CheckResult;
import at.rieder.secureqr.app.model.Content;
import at.rieder.secureqr.app.parse.ContentParser;
import at.rieder.secureqr.app.result.ButtonFactory;


public class ScanQRActivity extends ActionBarActivity implements SurfaceHolder.Callback {

    private static final String TAG = ScanQRActivity.class.getSimpleName();

    private CameraManager cameraManager;
    private HistoryManager historyManager;
    private ViewfinderView viewfinderView;

    private boolean surfaceExists;

    private View resultView;
    private View buttonView;
    private ScanQRActivityHandler scanQRActivityHandler;
    private Result lastResult;
    private Button reportButton;
    private boolean overlayVisible;

    private Content content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        HelperUtils.setScanQRActivity(this);

        // keep the screen on when scanning
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // set the layout
        setContentView(R.layout.scanqr);

        surfaceExists = false;
        overlayVisible = false;
        reportButton = (Button) findViewById(R.id.button_report);

        // set preferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        this.historyManager = new HistoryManager(this);
        ComponentAccessor.getInstance().addComponent(historyManager);
    }


    @Override
    protected void onResume() {
        super.onResume();

        HelperUtils.setScanQRActivity(this);

        // init camera
        this.cameraManager = new CameraManager(getApplication());
        this.cameraManager.stopPreview();

        ComponentAccessor.getInstance().addComponent(cameraManager);

        this.viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
        this.viewfinderView.setCameraManager(this.cameraManager);

        this.resultView = findViewById(R.id.result_view);
        this.buttonView = findViewById(R.id.result_button_view);

        if (!this.overlayVisible) {
            this.scanQRActivityHandler = null;
            this.lastResult = null;
            resetStatusView();
        }

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.camera_preview);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (surfaceExists) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
        }

        try {
            HelperUtils.setAppVersion(getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.i(TAG, "couldnt get app version");
            e.printStackTrace();
        }

        HelperUtils.setContext(this.getApplicationContext());

        if (isFirstTime()) {
            Log.i(TAG, "app is run the first time; showing the help dialog");
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
            alertBuilder.setMessage(HelperUtils.getContext().getString(R.string.msg_first_launch));
            alertBuilder.setCancelable(false);
            alertBuilder.setPositiveButton(HelperUtils.getContext().getString(R.string.msg_ack),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    }
            );

            AlertDialog alert = alertBuilder.create();
            alert.show();
        }
    }

    private boolean isFirstTime() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        boolean ranBefore = preferences.getBoolean("preferences_ran_before", false);
        if (!ranBefore) {
            // first time
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("preferences_ran_before", true);
            editor.commit();
        }
        return !ranBefore;
    }

    @Override
    protected void onPause() {
        if (scanQRActivityHandler != null) {
            scanQRActivityHandler.quitSynchronously();
            scanQRActivityHandler = null;
        }
        cameraManager.closeDriver();
        if (!surfaceExists) {
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.camera_preview);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ComponentAccessor.getInstance().getHistoryManager().close();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (lastResult != null) {
                    lastResult = null;
                    overlayVisible = false;
                    restartPreviewAfterDelay(0L);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_FOCUS:
            case KeyEvent.KEYCODE_CAMERA:
                // Handle these events so they don't launch the Camera app
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                cameraManager.setTorch(false);
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                cameraManager.setTorch(true);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalArgumentException("SurfaceHolder is null!");
        }

        if (cameraManager.isOpen()) {
            Log.i(TAG, "the camera manager is already open");
            return;
        }

        try {
            cameraManager.openDriver(surfaceHolder);

            if (scanQRActivityHandler == null) {
                scanQRActivityHandler = new ScanQRActivityHandler(this);
            }
        } catch (IOException e) {
            Log.w(TAG, e);
            bailOut(getString(R.string.msg_init_failed));
        }
    }

    public void restartPreviewAfterDelay(long delayMS) {
        if (scanQRActivityHandler != null) {
            scanQRActivityHandler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
        }
        resetStatusView();
    }

    private void bailOut(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage(message);
        builder.setPositiveButton(R.string.button_ok, new BailOutListener(this));
        builder.setOnCancelListener(new BailOutListener(this));
        builder.show();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "surfaceCreated() gave us a null surface!");
        }
        if (!surfaceExists) {
            surfaceExists = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceExists = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    private void resetStatusView() {
        viewfinderView.setVisibility(View.VISIBLE);
        resultView.setVisibility(View.GONE);
        buttonView.setVisibility(View.GONE);
        lastResult = null;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

        switch (item.getItemId()) {
            case R.id.action_settings:
                intent.setClassName(this, SettingsActivity.class.getName());
                startActivity(intent);
                break;
            case R.id.action_help:
                intent.setClassName(this, HelpActivity.class.getName());
                startActivity(intent);
                break;
            case R.id.action_history:
                if (ComponentAccessor.getInstance().getHistoryManager().isEmpty()) {
                    Toast.makeText(this, getString(R.string.msg_history_empty), Toast.LENGTH_LONG).show();
                } else {
                    intent.setClassName(this, HistoryActivity.class.getName());
                    startActivity(intent);
                }
                break;
            case R.id.action_clear_history:
                ComponentAccessor.getInstance().getHistoryManager().clearHistory();
                Toast.makeText(this, getString(R.string.msg_history_cleared), Toast.LENGTH_LONG).show();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public Handler getHandler() {
        return scanQRActivityHandler;
    }

    /**
     * A valid barcode has been found, so give an indication of success and show the results.
     *
     * @param rawResult The contents of the barcode.
     * @param barcode   A greyscale bitmap of the camera data which was decoded.
     */
    public void handleDecode(Result rawResult, Bitmap barcode) {
        if (!overlayVisible) {

            reportButton.setEnabled(false);

            lastResult = rawResult;

            // play beep
            AudioManager audioService = (AudioManager) getSystemService(getApplicationContext().AUDIO_SERVICE);

            if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsActivity.GENERAL_PLAY_BEEP, false)
                    && audioService.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {

                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.beep);
                mediaPlayer.setVolume(0.15F, 0.15F);
                mediaPlayer.start();
            }

            // vibrate
            if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsActivity.GENERAL_VIBRATE, false)) {
                Vibrator vibrator = (Vibrator) this.getSystemService(this.getApplicationContext().VIBRATOR_SERVICE);
                vibrator.vibrate(300L);
            }

            content = ContentParser.parseScanResult(rawResult);

            // hide viewfinder and show overlay
            viewfinderView.setVisibility(View.GONE);
            resultView.setVisibility(View.VISIBLE);
            buttonView.setVisibility(View.VISIBLE);
            overlayVisible = true;

            ButtonFactory.buildOpenButton(content);


            // start checking
            Log.i(TAG, "starting checks");
            CheckRunner checkRunner = CheckRunnerFactory.buildCheckRunner(this, content);

            if (checkRunner != null) {
                checkRunner.start();
            }

            // copy to clipboard
            if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsActivity.GENERAL_COPY_CLIPBOARD, false)) {
                ClipboardInterface.setText(content.toString(), this.getApplicationContext());
                Toast.makeText(this, getString(R.string.msg_copied_to_clipboard), Toast.LENGTH_LONG).show();
            }
        }
    }

    public void openButtonPressed(View view) {
        Log.d(TAG, "the open button was pressed");

        boolean showWarning = false;
        Collection<CheckResult> lastResults = ComponentAccessor.getInstance().getHistoryManager().getMostRecentCheckResults();

        for (CheckResult checkResult : lastResults) {
            if (! checkResult.getSuccessful()) {
                showWarning = true;
            }
        }

        if (showWarning) {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
            alertBuilder.setMessage(HelperUtils.getContext().getString(R.string.msg_open_malicious));
            alertBuilder.setCancelable(false);
            alertBuilder.setPositiveButton(HelperUtils.getContext().getString(R.string.msg_yes),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();

                            Intent intent = ButtonFactory.buildOpenButton(content);
                            if (intent != null) {
                                startActivity(intent);
                            } else {
                                Toast.makeText(HelperUtils.getScanQRActivity(), getString(R.string.msg_no_app_to_open_with), Toast.LENGTH_LONG).show();
                            }
                        }
                    }
            );
            alertBuilder.setNegativeButton(HelperUtils.getContext().getString(R.string.msg_cancel),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    }
            );

            AlertDialog alert = alertBuilder.create();
            alert.show();
        } else {
            Intent intent = ButtonFactory.buildOpenButton(content);

            if (intent != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, getString(R.string.msg_no_app_to_open_with), Toast.LENGTH_LONG).show();
            }
        }
    }

    public void reportButtonPressed(View view) {
        Log.d(TAG, "the scan report button was pressed");
        if (ComponentAccessor.getInstance().getHistoryManager().getMostRecentCheckResults().isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_no_checks_performed), Toast.LENGTH_LONG).show();
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            intent.setClassName(this, ReportActivity.class.getName());
            startActivity(intent);
        }
    }
}
