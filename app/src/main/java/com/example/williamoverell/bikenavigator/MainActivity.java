package com.example.williamoverell.bikenavigator;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.util.Log;
import android.widget.Toast;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class MainActivity extends Activity implements CvCameraViewListener2, SensorEventListener2{

    protected static final String TAG = null;
    private CameraBridgeViewBase mOpenCvCameraView;

    LanePreprocessor lanePreprocessor;

    Mat rgbaFrame;
    Mat blobFrame;
    Mat edgeFrame;

    boolean processingSwitch;
    boolean showColorBlobsSwitch;

    SharedPreferences.OnSharedPreferenceChangeListener listener;

    @Override
    public void onCameraViewStarted(int width, int height) {
        rgbaFrame = new Mat(height, width, CvType.CV_8UC4);
        blobFrame = new Mat(height, width, CvType.CV_8UC4);
        edgeFrame = new Mat(height, width, CvType.CV_8UC4);

        lanePreprocessor = new LanePreprocessor();
    }

    @Override
    public void onCameraViewStopped() {
        rgbaFrame.release();
        blobFrame.release();
        edgeFrame.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        rgbaFrame = inputFrame.rgba();
        if(processingSwitch) {
            lanePreprocessor.processImage(rgbaFrame,blobFrame);
            if(showColorBlobsSwitch)
                return blobFrame;
            lanePreprocessor.findLaneLine(rgbaFrame, rgbaFrame, blobFrame);
        }
            return rgbaFrame;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.layout_activity_main);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.OpenCvView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        processingSwitch = true;

        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(this);

        listener =
                new SharedPreferences.OnSharedPreferenceChangeListener() {
                    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                        if(key.equals("huelowPref"))
                        {
                            lanePreprocessor.setHuelow(Integer.parseInt(prefs.getString(key,"0")));
                        }
                        if(key.equals("huehighPref"))
                        {
                            lanePreprocessor.setHuehigh(Integer.parseInt(prefs.getString(key,"0")));
                        }
                        if(key.equals("satlowPref"))
                        {
                            lanePreprocessor.setSatlow(Integer.parseInt(prefs.getString(key,"0")));
                        }
                        if(key.equals("sathighPref"))
                        {
                            lanePreprocessor.setSathigh(Integer.parseInt(prefs.getString(key,"0")));
                        }
                        if(key.equals("vallowPref"))
                        {
                            lanePreprocessor.setVallow(Integer.parseInt(prefs.getString(key,"0")));
                        }
                        if(key.equals("valhighPref"))
                        {
                            lanePreprocessor.setValhigh(Integer.parseInt(prefs.getString(key,"0")));
                        }
                        if(key.equals("erosionsize"))
                        {
                            lanePreprocessor.setErosionSize(Integer.parseInt(prefs.getString(key,"0")));
                        }
                        if(key.equals("dilationsize"))
                        {
                            lanePreprocessor.setDilationSize(Integer.parseInt(prefs.getString(key,"0")));
                        }
                        if(key.equals("opencheckbox"))
                        {
                            lanePreprocessor.setPerformOpenSwitch(prefs.getBoolean(key,false));
                        }
                        if(key.equals("blobprocessingcheckbox"))
                        {
                            processingSwitch = prefs.getBoolean(key, false);
                        }
                        if(key.equals("colorthreshcheckboxPref"))
                        {
                            showColorBlobsSwitch = prefs.getBoolean(key, false);
                        }
                    }
                };
        prefs.registerOnSharedPreferenceChangeListener(listener);

        Log.i(TAG, "Trying to load OpenCV library");
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "Cannot connect to OpenCV Manager");
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new Preferences())
                    .addToBackStack(null)
                    .commit();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
        Log.i(TAG, "Trying to load OpenCV library");
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "Cannot connect to OpenCV Manager");
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    public void onFlushCompleted(Sensor sensor) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        /*
        This method will update the region of the frame to process
        as the pitch of the camera changes.  If the camera is pointing more towards the ground
        then more of the frame is processed, if pointing more towards the sky then less of the
        frame is processed.  The idea being that we do not want to look for lane lines in the sky!
         */
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        /*
        If accuracy changes
         */
    }
}
