package at.rieder.secureqr.app.activities;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import at.rieder.secureqr.app.R;
import at.rieder.secureqr.app.camera.CameraManager;


public final class ViewfinderView extends View {

    private static final String TAG = ViewfinderView.class.getSimpleName();

    private CameraManager cameraManager;
    private Integer actionBarHeight = 0;

    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        }
        Log.i(TAG, "The action bar height is: " + actionBarHeight);

    }

    public void setCameraManager(CameraManager cameraManager) {
        this.cameraManager = cameraManager;
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (cameraManager == null) {
            return;
        }

        Rect frame = cameraManager.getFramingRect();
        Rect previewFrame = cameraManager.getFramingRectInPreview();
        if (frame == null || previewFrame == null) {
            return;
        }

        canvas.drawBitmap(
                BitmapFactory.decodeResource(getContext().getResources(), R.drawable.crosshair),
                null,
                new Rect(frame.left, frame.top - actionBarHeight, frame.right, frame.bottom - actionBarHeight),
                null);
    }

}