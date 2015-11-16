package xyz.ordering.unshuffle;

import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.media.ImageReader;

import java.text.Format;

public class StartupActivity extends AppCompatActivity {
    private int mScreenDensity;
    private MediaProjectionManager mProjectionManager;
    private int mDisplayWidth;
    private int mDisplayHeight;
    private boolean mScreenSharing;

    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private Surface mSurface;
    private ImageReader mReader = null;

    private static final int MAX_NUM_IMAGES = 4;
    private static final int PERMISSION_CODE = 9000;
    private static final String TAG = "UnShuffle";

    // my hack
//    private final Resolution resolution = new Resolution(640,360);

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;

        // i just choose a convenient format here, might cause problems(?)
        // SEE http://stackoverflow.com/questions/26673127/android-imagereader-acquirelatestimage-returns-invalid-jpg
        mReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels,
                    ImageFormat.FLEX_RGB_888, MAX_NUM_IMAGES);


//        mSurfaceView = (SurfaceView) findViewById(R.id.surface);
        mSurface = mReader.getSurface();

//        ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();
//        lp.height = 640;
//        lp.width = 360;
//        mSurfaceView.setLayoutParams(lp);

        mProjectionManager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareScreen();
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
            }
        });
        startService(new Intent(getApplicationContext(), FloatService.class));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != PERMISSION_CODE) {
            Log.e(TAG, "Unknown request code: " + requestCode);
            return;
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this,
                    "User denied screen sharing permission", Toast.LENGTH_SHORT).show();
            return;
        }
        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mVirtualDisplay = createVirtualDisplay();
    }

    private void stopScreenSharing() {
        mScreenSharing = false;
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
    }

    private void resizeVirtualDisplay() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.resize(mDisplayWidth, mDisplayHeight, mScreenDensity);
    }

    private VirtualDisplay createVirtualDisplay() {
        return mMediaProjection.createVirtualDisplay("ScreenSharingDemo",
                mDisplayWidth, mDisplayHeight, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mSurface, null /*Callbacks*/, null /*Handler*/);
    }


    private void shareScreen() {
        mScreenSharing = true;
        if (mSurface == null) {
            return;
        }
        if (mMediaProjection == null) {
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(),
                    PERMISSION_CODE);
            return;
        }
        mVirtualDisplay = createVirtualDisplay();
    }

    // CAMERA STUFF

    private void prepareImageReader(Size sz, int format) throws Exception {
        int width = sz.getWidth();
        int height = sz.getHeight();
        mReader = ImageReader.newInstance(width, height, format, MAX_NUM_IMAGES);
        mListener  = new SimpleImageListener();
        mReader.setOnImageAvailableListener(mListener, mHandler);
        if (VERBOSE) Log.v(TAG, "Preparing ImageReader size " + sz.toString());
    }

//    private CaptureRequest prepareCaptureRequest(int format) throws Exception {
//        List<Surface> outputSurfaces = new ArrayList<Surface>(1);
//        Surface surface = mReader.getSurface();
//        assertNotNull("Fail to get surface from ImageReader", surface);
//        outputSurfaces.add(surface);
//        mCamera.configureOutputs(outputSurfaces);
//        mCameraListener.waitForState(STATE_BUSY, CAMERA_BUSY_TIMEOUT_MS);
//        mCameraListener.waitForState(STATE_IDLE, CAMERA_IDLE_TIMEOUT_MS);
//        CaptureRequest.Builder captureBuilder =
//                mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
//        assertNotNull("Fail to get captureRequest", captureBuilder);
//        captureBuilder.addTarget(mReader.getSurface());
//        return captureBuilder.build();
//    }

    private class SimpleImageListener implements ImageReader.OnImageAvailableListener {
        private int mPendingImages = 0;
        private final Object mImageSyncObject = new Object();
        @Override
        public void onImageAvailable(ImageReader reader) {
            if (VERBOSE) Log.v(TAG, "new image available");
            synchronized (mImageSyncObject) {
                mPendingImages++;
                mImageSyncObject.notifyAll();
            }
        }
        public boolean isImagePending() {
            synchronized (mImageSyncObject) {
                return (mPendingImages > 0);
            }
        }
        public void waitForImage() {
            final int TIMEOUT_MS = 5000;
            synchronized (mImageSyncObject) {
                while (mPendingImages == 0) {
                    try {
                        if (VERBOSE)
                            Log.d(TAG, "waiting for next image");
                        mImageSyncObject.wait(TIMEOUT_MS);
                        if (mPendingImages == 0) {
                            fail("wait for next image timed out");
                        }
                    } catch (InterruptedException ie) {
                        throw new RuntimeException(ie);
                    }
                }
                mPendingImages--;
            }
        }
    }

    // /STUFFS



    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            mMediaProjection = null;
            stopScreenSharing();
        }
    }

    /*
    private class SurfaceCallbacks implements SurfaceHolder.Callback {
        @Override
        public void
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            mDisplayWidth = width;
            mDisplayHeight = height;
            resizeVirtualDisplay();
        }
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            mSurface = holder.getSurface();
            if (mScreenSharing) {
                shareScreen();
            }
        }
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (!mScreenSharing) {
                stopScreenSharing();
            }
        }
    }


    private static class Resolution {
        int x;
        int y;
        public Resolution(int x, int y) {
            this.x = x;
            this.y = y;
        }
        @Override
        public String toString() {
            return x + "x" + y;
        }
    }
    */
}


