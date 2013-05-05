/*Copyright 2013 Kreg Hanning

This file is part of ScratcherControl.

ScratcherControl is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

ScratcherControl is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with ScratcherControl.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.khanning.scratchercontrol;

import java.util.Locale;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

public class SensorService extends Service implements SensorEventListener {
	
	private static final String LOG_TAG = "ScratcherControl";

	//Private Object declaration
	private static Handler messageHandler;
	private static SensorManager mSensorManager;
	private HandlerThread accelThread;
	private HandlerThread lightThread;
	private Handler accelHandler;
	private Handler lightHandler;
	
	//Private primitive declaration
	private int screenRotation;
	
	//Public Object declaration
	public static String xVal;
	public static String yVal;
	public static String zVal;
	public static String lightVal;

	@Override
	public void onCreate() {
		
		//Get the default screen rotation
		Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
		screenRotation = display.getRotation();		
		
		//Initialize the SensorManager
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		//Start Accelerometer Thread
		accelThread = new HandlerThread("accelThread");
		accelThread.start();
		accelHandler = new Handler(accelThread.getLooper());
		
		//Start Light Indicator Thread
		lightThread = new HandlerThread("lightThread");
		lightThread.start();
		lightHandler = new Handler(lightThread.getLooper());

		// Register Sensor Listeners
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 
										SensorManager.SENSOR_DELAY_NORMAL, accelHandler);
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT),
										SensorManager.SENSOR_DELAY_NORMAL, lightHandler);
				
	}
	
	public static void setHandler(Handler mHandler) {
		//Method to get the current View's MessageHandler
		messageHandler = mHandler;
	}
	
	@Override
	public void onSensorChanged(SensorEvent e) {
		switch (e.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			//Compare the current and default screen rotation and adjust accelerometer values
			switch (screenRotation) {
			case Surface.ROTATION_90:
				xVal = String.format(Locale.US, "%.1f", e.values[1]);
				yVal = String.format(Locale.US, "%.1f", (e.values[0] * -1));
				zVal = String.format(Locale.US, "%.1f", e.values[2]);
				break;
			case Surface.ROTATION_270:
				xVal = String.format(Locale.US, "%.1f", e.values[1] * -1);
				yVal = String.format(Locale.US, "%.1f", (e.values[0]));
				zVal = String.format(Locale.US, "%.1f", e.values[2]);
				break;
			default:
				xVal = String.format(Locale.US, "%.1f", e.values[0] * -1);
				yVal = String.format(Locale.US, "%.1f", e.values[1] * -1);
				zVal = String.format(Locale.US, "%.1f", e.values[2] * -1);
				break;
			}
			break;
		case Sensor.TYPE_LIGHT:
			//Get the current light sensor value and cast to integer
			int light = (int) e.values[0];
			lightVal = Integer.toString(light);
			break;
		}
		
		//Send the current sensor values to the current View
		messageHandler.obtainMessage(SocketService.SENSOR_UPDATE, xVal + "," + 
				yVal + "," + zVal + "," + lightVal).sendToTarget();
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override 
	public void onDestroy() {
		super.onDestroy();
		Log.i(LOG_TAG, "Unregistered Sensors");
		
		//Quit both Thread's and unregister SensorManager
		accelThread.quit();
		lightThread.quit();
		mSensorManager.unregisterListener(this);
	}
	
}
