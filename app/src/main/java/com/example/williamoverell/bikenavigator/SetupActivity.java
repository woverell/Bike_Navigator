package com.example.williamoverell.bikenavigator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

/*
    This activity will set the camera angle for the main activity
 */
public class SetupActivity extends Activity implements SensorEventListener2{

    private SensorManager mSensorManager;
    private Sensor mGravity;
    private Sensor mGeomagnetic;

    float mR[] = new float[9];
    float mRotatedR[] = new float[9];
    float mI[] = new float[9];
    float mOrient[] = new float[3];
    float mGrav[] = new float[3];
    float mGeo[] = new float[3];
    boolean mGravSet, mGeoSet;

    int highAngle, lowAngle, currentAngle;

    TextView highAngleTextView;
    TextView currentAngleTextView;
    TextView lowAngleTextView;
    TextView azimuthTextView;
    TextView pitchTextView;
    TextView rollTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mGeomagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mGravSet = false;
        mGeoSet = false;

        azimuthTextView = (TextView) findViewById(R.id.azimuthTextView);
        pitchTextView = (TextView) findViewById(R.id.pitchTextView);
        rollTextView = (TextView) findViewById(R.id.rollTextView);
        highAngleTextView = (TextView) findViewById(R.id.angleHigh);
        currentAngleTextView = (TextView) findViewById(R.id.angleCurrent);
        lowAngleTextView = (TextView) findViewById(R.id.angleLow);

        highAngle = 30;
        lowAngle = 40;
        currentAngle=0;

        highAngleTextView.setText(String.valueOf(highAngle));
        lowAngleTextView.setText(String.valueOf(lowAngle));
    }

    @Override
    public void onResume()
    {
        super.onResume();

        mSensorManager.registerListener(this, mGravity, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mGeomagnetic, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause()
    {
        super.onPause();

        mSensorManager.unregisterListener(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_setup, menu);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onClick(View view)
    {
        Intent data = new Intent();

        data.setData(Uri.parse(Integer.toString(currentAngle)));

        setResult(RESULT_OK, data);

        finish();
    }

    @Override
    public void onFlushCompleted(Sensor sensor) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        /* TODO: When roll is in correct range go to main activity and pass the currentAngle */
        if(event.sensor == mGravity)
        {
            System.arraycopy(event.values, 0, mGrav, 0, event.values.length);
            mGravSet = true;
        }else if(event.sensor == mGeomagnetic)
        {
            System.arraycopy(event.values, 0, mGeo, 0, event.values.length);
            mGeoSet = true;
        }

        if(mGravSet && mGeoSet)
        {
            SensorManager.getRotationMatrix(mR, mI, mGrav, mGeo);
            SensorManager.remapCoordinateSystem(mR,SensorManager.AXIS_X, SensorManager.AXIS_Z, mRotatedR);
            SensorManager.getOrientation(mRotatedR, mOrient);

            float azimuth_angle = mOrient[0]; // the azimuth angle
            float pitch_angle = mOrient[1]; // the pitch angle
            float roll_angle = mOrient[2]; // the roll angle
            currentAngle = (int)Math.round(Math.toDegrees(pitch_angle));

            azimuthTextView.setText(String.valueOf(Math.round(Math.toDegrees(azimuth_angle))));
            pitchTextView.setText(String.valueOf(Math.round(Math.toDegrees(pitch_angle))));
            rollTextView.setText(String.valueOf(Math.round(Math.toDegrees(roll_angle))));
            currentAngleTextView.setText(String.valueOf(currentAngle));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
