/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package at.rieder.secureqr.app.camera;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.hardware.Camera;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import at.rieder.secureqr.app.R;
import at.rieder.secureqr.app.helper.HelperUtils;

/**
 * A class which deals with reading, parsing, and setting the camera parameters which are used to
 * configure the camera hardware.
 */
final class CameraConfigurationManager {

    private static final String TAG = "CameraConfiguration";

    // This is bigger than the size of a small screen, which is still supported. The routine
    // below will still select the default (presumably 320x240) size for these. This prevents
    // accidental selection of very low resolution on some devices.
    private static final int MIN_PREVIEW_PIXELS = 480 * 320; // normal screen
    private static final float MAX_EXPOSURE_COMPENSATION = 1.5f;
    private static final float MIN_EXPOSURE_COMPENSATION = 0.0f;
    private static final double MAX_ASPECT_DISTORTION = 0.15;
    private static final int MIN_FPS = 5;

    private final Context context;
    private Point screenResolution;
    private Point cameraResolution;

    CameraConfigurationManager(Context context) {
        this.context = context;
    }

    /**
     * Reads, one time, values from the camera that are needed by the app.
     */
    void initFromCameraParameters(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        Point theScreenResolution = new Point();
        display.getSize(theScreenResolution);
        screenResolution = theScreenResolution;
        Log.i(TAG, "Screen resolution: " + screenResolution);
        cameraResolution = findBestPreviewSizeValue(parameters, screenResolution);
        Log.i(TAG, "Camera resolution: " + cameraResolution);
    }

    void setDesiredCameraParameters(Camera camera, boolean safeMode) {
        Camera.Parameters parameters = camera.getParameters();

        if (parameters == null) {
            Log.w(TAG, "Device error: no camera parameters are available. Proceeding without configuration.");
            return;
        }

        Log.i(TAG, "Initial camera parameters: " + parameters.flatten());

        if (safeMode) {
            Log.w(TAG, "In camera config safe mode -- most settings will not be honored");
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        initializeTorch(parameters, prefs, safeMode);

        setBestPreviewFPS(parameters);

        String focusMode = null;
        if (safeMode) {
            focusMode = findSettableValue(parameters.getSupportedFocusModes(),
                    Camera.Parameters.FOCUS_MODE_AUTO);
        } else {
            focusMode = findSettableValue(parameters.getSupportedFocusModes(),
                    Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE,
                    Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO,
                    Camera.Parameters.FOCUS_MODE_AUTO);
        }
        // Maybe selected auto-focus but not available, so fall through here:
        if (!safeMode && focusMode == null) {
            focusMode = findSettableValue(parameters.getSupportedFocusModes(),
                    Camera.Parameters.FOCUS_MODE_MACRO,
                    Camera.Parameters.FOCUS_MODE_EDOF);
        }
        if (focusMode != null) {
            parameters.setFocusMode(focusMode);
        }

        if (!safeMode) {
            if (parameters.isVideoStabilizationSupported()) {
                Log.i(TAG, "Enabling video stabilization...");
                parameters.setVideoStabilization(true);
            } else {
                Log.i(TAG, "This device does not support video stabilization");
            }

            //MeteringInterface.setFocusArea(parameters);
            //MeteringInterface.setMetering(parameters);

        }

        Log.d(TAG, "the surface view is size: x=" + (HelperUtils.getScanQRActivity().findViewById(R.id.camera_preview)).getWidth() + ",y=" + (HelperUtils.getScanQRActivity().findViewById(R.id.camera_preview)).getHeight());

        SurfaceView surfaceView = (SurfaceView) HelperUtils.getScanQRActivity().findViewById(R.id.camera_preview);

        int supposedX, supposedY;
        if (screenResolution.x * screenResolution.y > parameters.getPreviewSize().height * parameters.getPreviewSize().width &&
                screenResolution.x <= 1280 &&
                screenResolution.y <= 1280) {

            supposedX = screenResolution.y;
            supposedY = screenResolution.x;
            parameters.setPreviewSize(screenResolution.y, screenResolution.x);

        } else if (parameters.getPreviewSize().width <= 1280 &&
                parameters.getPreviewSize().height <= 1280 &&
                parameters.getPreviewSize().width + 50 > cameraResolution.x &&
                parameters.getPreviewSize().height + 50 > cameraResolution.y){

            supposedX = parameters.getPreviewSize().width;
            supposedY = parameters.getPreviewSize().height;
        } else {
            supposedX = cameraResolution.y;
            supposedY = cameraResolution.x;
            parameters.setPreviewSize(cameraResolution.x, cameraResolution.y);
        }

        camera.setParameters(parameters);

        Camera.Parameters afterParameters = camera.getParameters();
        Camera.Size afterSize = afterParameters.getPreviewSize();
        if (afterSize != null && (supposedX != afterSize.width || supposedY != afterSize.height)) {
            Log.w(TAG, "Camera said it supported preview size " + supposedX + 'x' + supposedY +
                    ", but after setting it, preview size is " + afterSize.width + 'x' + afterSize.height);
            cameraResolution.x = afterSize.width;
            cameraResolution.y = afterSize.height;
        } else {
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(supposedY, supposedX);
            surfaceView.setLayoutParams(layoutParams);
        }
    }

    Point getCameraResolution() {
        return cameraResolution;
    }

    Point getScreenResolution() {
        return screenResolution;
    }

    boolean getTorchState(Camera camera) {
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            if (parameters != null) {
                String flashMode = camera.getParameters().getFlashMode();
                return flashMode != null &&
                        (Camera.Parameters.FLASH_MODE_ON.equals(flashMode) ||
                                Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode));
            }
        }
        return false;
    }

    void setTorch(Camera camera, boolean newSetting) {
        Camera.Parameters parameters = camera.getParameters();
        doSetTorch(parameters, newSetting, false);
        camera.setParameters(parameters);
    }

    private void initializeTorch(Camera.Parameters parameters, SharedPreferences prefs, boolean safeMode) {
        doSetTorch(parameters, false, safeMode);
    }

    private void doSetTorch(Camera.Parameters parameters, boolean newSetting, boolean safeMode) {
        String flashMode;
        if (newSetting) {
            flashMode = findSettableValue(parameters.getSupportedFlashModes(),
                    Camera.Parameters.FLASH_MODE_TORCH,
                    Camera.Parameters.FLASH_MODE_ON);
        } else {
            flashMode = findSettableValue(parameters.getSupportedFlashModes(),
                    Camera.Parameters.FLASH_MODE_OFF);
        }
        if (flashMode != null) {
            parameters.setFlashMode(flashMode);
        }

        if (!safeMode) {
            int minExposure = parameters.getMinExposureCompensation();
            int maxExposure = parameters.getMaxExposureCompensation();
            if (minExposure != 0 || maxExposure != 0) {
                float step = parameters.getExposureCompensationStep();
                int desiredCompensation;
                if (newSetting) {
                    // Light on; set low exposure compensation
                    desiredCompensation = Math.max((int) (MIN_EXPOSURE_COMPENSATION / step), minExposure);
                } else {
                    // Light off; set high compensation
                    // seems to fail
                    // desiredCompensation = Math.min((int) (MAX_EXPOSURE_COMPENSATION / step), maxExposure);
                    desiredCompensation = Math.max((int) (MIN_EXPOSURE_COMPENSATION / step), minExposure);
                }
                Log.i(TAG, "Setting exposure compensation to " + desiredCompensation + " / " + (step * desiredCompensation));
                parameters.setExposureCompensation(desiredCompensation);
            } else {
                Log.i(TAG, "Camera does not support exposure compensation");
            }
        }
    }

    private static void setBestPreviewFPS(Camera.Parameters parameters) {
        // Required for Glass compatibility; also improves battery/CPU performance a tad
        List<int[]> supportedPreviewFpsRanges = parameters.getSupportedPreviewFpsRange();
        Log.i(TAG, "Supported FPS ranges: " + toString(supportedPreviewFpsRanges));
        if (supportedPreviewFpsRanges != null && !supportedPreviewFpsRanges.isEmpty()) {
            int[] minimumSuitableFpsRange = null;
            for (int[] fpsRange : supportedPreviewFpsRanges) {
                int fpsMax = fpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX];
                if (fpsMax >= MIN_FPS * 1000 &&
                        (minimumSuitableFpsRange == null ||
                                fpsMax > minimumSuitableFpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX])) {
                    minimumSuitableFpsRange = fpsRange;
                }
            }
            if (minimumSuitableFpsRange == null) {
                Log.i(TAG, "No suitable FPS range?");
            } else {
                int[] currentFpsRange = new int[2];
                parameters.getPreviewFpsRange(currentFpsRange);
                if (!Arrays.equals(currentFpsRange, minimumSuitableFpsRange)) {
                    Log.i(TAG, "Setting FPS range to " + Arrays.toString(minimumSuitableFpsRange));
                    parameters.setPreviewFpsRange(minimumSuitableFpsRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
                            minimumSuitableFpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
                }
            }
        }
    }

    // Actually prints the arrays properly:
    private static String toString(Collection<int[]> arrays) {
        if (arrays == null || arrays.isEmpty()) {
            return "[]";
        }
        StringBuilder buffer = new StringBuilder();
        buffer.append('[');
        Iterator<int[]> it = arrays.iterator();
        while (it.hasNext()) {
            buffer.append(Arrays.toString(it.next()));
            if (it.hasNext()) {
                buffer.append(", ");
            }
        }
        buffer.append(']');
        return buffer.toString();
    }

    private Point findBestPreviewSizeValue(Camera.Parameters parameters, Point screenResolution) {

        List<Camera.Size> rawSupportedSizes = parameters.getSupportedPreviewSizes();
        if (rawSupportedSizes == null) {
            Log.w(TAG, "Device returned no supported preview sizes; using default");
            Camera.Size defaultSize = parameters.getPreviewSize();
            return new Point(defaultSize.width, defaultSize.height);
        }

        // Sort by size, descending
        List<Camera.Size> supportedPreviewSizes = new ArrayList<Camera.Size>(rawSupportedSizes);
        Collections.sort(supportedPreviewSizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size a, Camera.Size b) {
                int aPixels = a.height * a.width;
                int bPixels = b.height * b.width;
                if (bPixels < aPixels) {
                    return -1;
                }
                if (bPixels > aPixels) {
                    return 1;
                }
                return 0;
            }
        });

        if (Log.isLoggable(TAG, Log.INFO)) {
            StringBuilder previewSizesString = new StringBuilder();
            for (Camera.Size supportedPreviewSize : supportedPreviewSizes) {
                previewSizesString.append(supportedPreviewSize.width).append('x')
                        .append(supportedPreviewSize.height).append(' ');
            }
            Log.i(TAG, "Supported preview sizes: " + previewSizesString);
        }

        double screenAspectRatio = (double) screenResolution.y / (double) screenResolution.x;

        // Remove sizes that are unsuitable
        Iterator<Camera.Size> it = supportedPreviewSizes.iterator();
        while (it.hasNext()) {
            Camera.Size supportedPreviewSize = it.next();
            int realWidth = supportedPreviewSize.width;
            int realHeight = supportedPreviewSize.height;
            if (realWidth * realHeight < MIN_PREVIEW_PIXELS) {
                it.remove();
                continue;
            }

            boolean isCandidatePortrait = realWidth < realHeight;
            int maybeFlippedWidth = isCandidatePortrait ? realHeight : realWidth;
            int maybeFlippedHeight = isCandidatePortrait ? realWidth : realHeight;
            double aspectRatio = (double) maybeFlippedWidth / (double) maybeFlippedHeight;
            double distortion = Math.abs(aspectRatio - screenAspectRatio);
            if (distortion > MAX_ASPECT_DISTORTION) {
                it.remove();
                continue;
            }

            if (maybeFlippedWidth == screenResolution.x && maybeFlippedHeight == screenResolution.y
                    && screenResolution.x <= 1280 && screenResolution.y <= 1280) {
                Point exactPoint = new Point(realWidth, realHeight);
                Log.i(TAG, "Found preview size exactly matching screen size: " + exactPoint);
                return exactPoint;
            }
        }

        // If no exact match, use largest preview size. This was not a great idea on older devices because
        // of the additional computation needed. We're likely to get here on newer Android 4+ devices, where
        // the CPU is much more powerful.
        Iterator<Camera.Size> sizeIterator = supportedPreviewSizes.iterator();
        while (sizeIterator.hasNext()) {
            Camera.Size largestPreview = sizeIterator.next();

            if (largestPreview.width <= 1280 && largestPreview.height <= 1280) {
                Point largestSize = new Point(largestPreview.width, largestPreview.height);
                Log.i(TAG, "Using largest suitable preview size: " + largestSize);
                return largestSize;
            } else {
                sizeIterator.remove();
            }
        }

        // If there is nothing at all suitable, return current preview size
        Camera.Size defaultPreview = parameters.getPreviewSize();
        Point defaultSize = new Point(defaultPreview.width, defaultPreview.height);
        Log.i(TAG, "No suitable preview sizes, using default: " + defaultSize);
        return defaultSize;
    }

    private static String findSettableValue(Collection<String> supportedValues,
                                            String... desiredValues) {
        Log.i(TAG, "Supported values: " + supportedValues);
        String result = null;
        if (supportedValues != null) {
            for (String desiredValue : desiredValues) {
                if (supportedValues.contains(desiredValue)) {
                    result = desiredValue;
                    break;
                }
            }
        }
        Log.i(TAG, "Settable value: " + result);
        return result;
    }

}
