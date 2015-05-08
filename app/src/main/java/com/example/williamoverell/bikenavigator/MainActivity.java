package com.example.williamoverell.bikenavigator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class MainActivity extends Activity implements CvCameraViewListener2{

    protected static final String TAG = null;
    private CameraBridgeViewBase mOpenCvCameraView;

    LanePreprocessor lanePreprocessor;

    TextView instructionsTextView;

    Mat rgbaFrame;
    Mat blobFrame;
    Mat edgeFrame;

    boolean processingSwitch;
    boolean showColorBlobsSwitch;

    int request_Code = 1;

    int img_width;
    int img_height;
    int img_skyLine;

    int view_angle;
    int vertical_view_angle;

    Rect processSection;

    SharedPreferences.OnSharedPreferenceChangeListener listener;

    @Override
    public void onCameraViewStarted(int width, int height) {
        img_width = width;
        img_height = height;
        img_skyLine = 0; // default value
        vertical_view_angle = 50; // for Galaxy S3 approx vertical view angle of the rear-facing camera is 50 degrees

        // Toast.makeText(getApplicationContext(), "Running", Toast.LENGTH_LONG).show();
        // set the sky line
        calculateSkyLine(view_angle, vertical_view_angle, img_height);

        rgbaFrame = new Mat(height, width, CvType.CV_8UC4);
        blobFrame = new Mat(height, width, CvType.CV_8UC4);
        edgeFrame = new Mat(height, width, CvType.CV_8UC4);

        //processSection = new Rect(new Point(0, img_skyLine), new Point(width, height));

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

        // Draw the sky line
        Core.line(rgbaFrame, new Point(0, img_skyLine), new Point(img_width, img_skyLine), new Scalar(255, 0, 255, 0), 5);

        Mat toProcess = rgbaFrame.submat(processSection);
        if(processingSwitch) {
            lanePreprocessor.processImage(toProcess,blobFrame);
            if(showColorBlobsSwitch)
                return blobFrame;
            lanePreprocessor.findLaneLine(toProcess, toProcess, blobFrame);
        }
            // update the instructions text view
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                instructionsTextView.setText(lanePreprocessor.getInstruction());
            }
        });
            return rgbaFrame;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.layout_activity_main);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.OpenCvView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        instructionsTextView = (TextView) findViewById(R.id.instructionTextView);

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

        startActivityForResult(new Intent("com.example.williamoverell.bikenavigator.SetupActivity"), request_Code);

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(requestCode == request_Code){
            if(resultCode == RESULT_OK){
                // set the view angle from the returned camera angle
                view_angle = Integer.parseInt(data.getData().toString());
                // Toast.makeText(getApplicationContext(), Integer.toString(view_angle), Toast.LENGTH_LONG).show();
            }
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

    public void calculateSkyLine(int viewangle, int vertangle, int imgheight) {
        // Calculate the sky line from camera view_angle, vertical_view_angle, and image height

        // First, if the view angle is larger than half the vertangle then we need to process the entire image
        if(viewangle > vertangle/2)
            this.img_skyLine = 0;
        else {
            //  Otherwise we need to calculate what percentage of the vertangle is above 0 deg
            // Then this percentage of the imgheight from the top of the frame is sky

            // So first find what degree from 0 the top of the vertical view angle is
            int highest_View_Angle = vertangle/2 - viewangle;
            // Now what percentage is this out of the vertangle
            double percentOfScreen = (double)highest_View_Angle / (double)vertangle;
            //Toast.makeText(getApplicationContext(), Double.toString(percentOfScreen), Toast.LENGTH_LONG).show();
            // Then set the skyline to this percent from the top of the image
            this.img_skyLine = (int) Math.round(percentOfScreen*imgheight);
        }
    }
}
