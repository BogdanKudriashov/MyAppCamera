package com.bohdandroid.myappcamera;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    public static final String LOG_TAG = "myLogs";

    CameraService[] myCameras = null;
    int indexCamera = -1;

    private CameraManager mCameraManager = null;
    private Button mButtonOpenCamera;
    private TextureView mImageView;
    private TextView tvCameraTitle;
    private TextView tvCameraTitleNext;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(LOG_TAG, "Request permission!");
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                ||
                (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        )
        {
            requestPermissions(new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }

        mButtonOpenCamera =  findViewById(R.id.btn_open_cam);
        mImageView = findViewById(R.id.textureView);
        tvCameraTitle = findViewById(R.id.tvCameraTitle);
        tvCameraTitleNext = findViewById(R.id.tvCameraTitleNext);

        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try{
            // Get list of cameras from device
            myCameras = new CameraService[mCameraManager.getCameraIdList().length];

            for (String cameraID : mCameraManager.getCameraIdList()) {
                Log.i(LOG_TAG, "cameraID: " + cameraID);
                int id = Integer.parseInt(cameraID);

                // Obtaining camera characteristics
                CameraCharacteristics cc = mCameraManager.getCameraCharacteristics(cameraID);

                //  Determining which camera looks where
                int lensFaceing = cc.get(CameraCharacteristics.LENS_FACING);

                if (lensFaceing ==  CameraCharacteristics.LENS_FACING_FRONT) {
                    Log.i(LOG_TAG,"Camera with ID: " + cameraID + "  is FRONT CAMERA  ");
                }

                if (lensFaceing ==  CameraCharacteristics.LENS_FACING_BACK) {
                    Log.i(LOG_TAG,"Camera with ID: " + cameraID + " is BACK CAMERA  ");
                }

                // Create a handler for the camera
                myCameras[id] = new CameraService(mCameraManager,cameraID,lensFaceing);
            }
        }
        catch(CameraAccessException e){
            Log.e(LOG_TAG, e.getMessage());
            e.printStackTrace();
        }

        mButtonOpenCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (indexCamera <= myCameras.length -1) {

                    if (indexCamera != -1 && indexCamera < myCameras.length - 1) {
                        if (myCameras[indexCamera].isOpen()) {
                            myCameras[indexCamera].closeCamera();
                        }
                    }

                    if (indexCamera < myCameras.length - 1) {
                        indexCamera++;
                    }

                    if (indexCamera == myCameras.length - 1) {
                        mButtonOpenCamera.setVisibility(View.INVISIBLE);
                    }


                    tvCameraTitleNext.setText("myCameras.length: " + String.valueOf(myCameras.length));

                    if (indexCamera != -1 && indexCamera <= myCameras.length - 1) {
                        if (myCameras[indexCamera] != null && myCameras[indexCamera].getValueLensFacing() == CameraCharacteristics.LENS_FACING_BACK) {
                            if (!myCameras[indexCamera].isOpen()) {
                                myCameras[indexCamera].openCamera();
                                tvCameraTitle.setText("Current BACK camera index: " + String.valueOf(indexCamera) + ", number: " + String.valueOf(indexCamera + 1));
                            }
                        }
                    }

                    if (indexCamera != -1 && indexCamera <= myCameras.length - 1) {
                        if (myCameras[indexCamera] != null && myCameras[indexCamera].getValueLensFacing() == CameraCharacteristics.LENS_FACING_FRONT) {
                            if (!myCameras[indexCamera].isOpen()) {
                                myCameras[indexCamera].openCamera();
                                tvCameraTitle.setText("Current FRONT camera index: " + String.valueOf(indexCamera) + ", number: " + String.valueOf(indexCamera + 1));
                            }
                        }
                    }
                }
            }
        });
    }

    public class CameraService {

        private String mCameraID;
        private int mLensFaceing;
        private CameraDevice mCameraDevice = null;
        private CameraCaptureSession mCaptureSession;

        public CameraService(CameraManager cameraManager, String cameraID, int lensFaceing) {
            mCameraManager = cameraManager;
            mCameraID = cameraID;
            mLensFaceing = lensFaceing;
        }

        public int getValueLensFacing() {
            return mLensFaceing;
        }

        public String getValueCameraId() {
            return mCameraID;
        }

        private CameraDevice.StateCallback mCameraCallback = new CameraDevice.StateCallback() {

            @Override
            public void onOpened(CameraDevice camera) {
                mCameraDevice = camera;
                Log.i(LOG_TAG, "Open camera  with id:"+mCameraDevice.getId());
                createCameraPreviewSession();
            }

            @Override
            public void onDisconnected(CameraDevice camera) {
                mCameraDevice.close();
                Log.i(LOG_TAG, "disconnect camera  with id:"+mCameraDevice.getId());
                mCameraDevice = null;
            }

            @Override
            public void onError(CameraDevice camera, int error) {
                Log.i(LOG_TAG, "error! camera id:"+camera.getId()+" error:"+error);
            }
        };

        private void createCameraPreviewSession() {
            SurfaceTexture texture = mImageView.getSurfaceTexture();
            texture.setDefaultBufferSize(1920,1080);
            Surface surface = new Surface(texture);

            try {
                final CaptureRequest.Builder builder =
                        mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                builder.addTarget(surface);

                mCameraDevice.createCaptureSession(Arrays.asList(surface),
                        new CameraCaptureSession.StateCallback() {

                            @Override
                            public void onConfigured(CameraCaptureSession session) {
                                mCaptureSession = session;
                                try {
                                    mCaptureSession.setRepeatingRequest(builder.build(),null,null);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(CameraCaptureSession session) { }}, null );

            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        public boolean isOpen() {
            if (mCameraDevice == null) {
                return false;
            } else {
                return true;
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        public void openCamera() {
            try {
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    mCameraManager.openCamera(mCameraID,mCameraCallback,null);
                }
            } catch (CameraAccessException e) {
                Log.i(LOG_TAG,e.getMessage());
            }
        }

        public void closeCamera() {
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        finish();
        System.exit(0);
    }
}