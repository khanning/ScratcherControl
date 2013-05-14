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

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

public class SocketService extends Service {
		
	private static final String LOG_TAG = "ScratcherControl";
	
	//Declare constants
	public final static int CONNECTING 			= 0;
	public final static int CONNECTION_FAILED 	= 1;
	public final static int CONNECTION_SUCCESS 	= 2;
	public final static int CONNECTION_REFUSED  = 3;
	public final static int DISCONNECTED 		= 4;
	public final static int SENSOR_UPDATE 		= 5;
	
	public final static int CONTROLLER_VIEW 	= 0;
	public final static int GESTURE_VIEW 		= 1;
	
	//Frequency to send commands to Scratch in milliseconds
	private final static int COMMAND_FREQ		= 50;

	//Private Object declaration
	private static DataOutputStream mOutputStream;
	private static Handler mHandler;
	private static ScheduledExecutorService mScheduledExecutorService;
	private static Socket mSocket;
	private static String gestureAction;
	private static String ipAddress;
	private static String voiceCommand;
	
	//Public primitive declaration
	public static boolean isConnected;
		
	public static void setHandler(Handler handler) {
		//Method to get the current View's MessageHandler
		mHandler = handler;
	}
	
	public static void setGesture(String gesture) {
		gestureAction = gesture;
	}
	
	public static void setVoiceCommand(String command) {
		voiceCommand = command;
	}
	
	public static void connectTo(String ip) {
		ipAddress = ip;
		startSocket();
	}

	private static void startSocket() {
		//Method to start a Socket connection with Scratch
		
		//Notify activity that we are starting connection
		mHandler.obtainMessage(CONNECTING).sendToTarget();
		
		//Start socket connection in a separate thread
		Thread thread = new Thread(new Runnable() {
			public void run() {
				
				try {
					
					//Setup Socket
					mSocket = new Socket();
					mSocket.setKeepAlive(true);
					mSocket.setSoTimeout(7000);
					mSocket.setSendBufferSize(10000);
					SocketAddress mSocketAddress = new InetSocketAddress(ipAddress, 42001);
					
					//Attempt Socket connection
					mSocket.connect(mSocketAddress, 7000);
					
					//Initialize output stream to send commands
					mOutputStream = new DataOutputStream(mSocket.getOutputStream());
					isConnected = true;
					
				} catch (ConnectException e) {
					Log.e(LOG_TAG, "Connection Refused");
					e.printStackTrace();
					mHandler.obtainMessage(CONNECTION_REFUSED).sendToTarget();
					isConnected = false;
				} catch (IOException e) {
					Log.e(LOG_TAG, "Connection Failed");
					e.printStackTrace();
					mHandler.obtainMessage(CONNECTION_FAILED).sendToTarget();
					isConnected = false;
				}
				
				//If connection succeeded
				if (isConnected) {
					
					//Create array to initialize broadcasts
					String[] broadcasts = {"flick_up", "flick_down", "flick_left", "flick_right", 
							"tap", "double_tap", "long_press", "scroll_up", "scroll_down", "scroll_left", 
							"scroll_right", "Start", "Select"};
					
					//Send each broadcast to Scratch, pausing between each command
					for (int i=0; i<broadcasts.length; i++) {
						sendCommand("broadcast " + broadcasts[i]);
						sleep(COMMAND_FREQ);
					}
					
					startSending();
					
					//Notify activity that the connection was successful
					mHandler.obtainMessage(CONNECTION_SUCCESS).sendToTarget();					
					Log.i(LOG_TAG, "Initialized");
					
				}
				
			}
			
			private void sleep(int time) {
				//Make the thread sleep for x milliseconds to avoid overlapping commands
				SystemClock.sleep(time);
			}
			
		});
		thread.start();
		
	}
	
	public static void closeSocket() {
		//Method to stop the Socket connection with Scratch
		if (isConnected) {
			try {
				Log.i(LOG_TAG, "Closing socket");
				stopSending();
				mOutputStream.close();
				mSocket.close();
				isConnected = false;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public static void startSending() {
		//Method to start sending commands to Scratch
		
		//Start a Thread that will run once ever x milliseconds
		mScheduledExecutorService = Executors.newScheduledThreadPool(5);
		mScheduledExecutorService.scheduleAtFixedRate(new Runnable() {
			public void run() {
													
				if (isConnected) {
					
					//Check the for gestures
					if (gestureAction!=null) {
						sendCommand("broadcast " + gestureAction);
						gestureAction = null;
					} else if (voiceCommand!=null) {
						sendCommand(voiceCommand);
						voiceCommand = null;
					} else {
					
						//Create a single command to update values
						String command = "sensor-update";
						command += " accelerometer-x " + SensorService.xVal;
						command += " accelerometer-y " + SensorService.yVal;
						command += " accelerometer-z " + SensorService.zVal;
						command += " light-level " + SensorService.lightVal;
											
						command += " button-up-pressed " + ControllerView.upButtonPressed;
						command += " button-down-pressed " + ControllerView.downButtonPressed;
						command += " button-left-pressed " + ControllerView.leftButtonPressed;
						command += " button-right-pressed " + ControllerView.rightButtonPressed;
						command += " button-a-pressed " + ControllerView.aButton.isPressed();
						command += " button-b-pressed " + ControllerView.bButton.isPressed();
						command += " button-x-pressed " + ControllerView.xButton.isPressed();
						command += " button-y-pressed " + ControllerView.yButton.isPressed();
							
						sendCommand(command);
					}

				}

			}
		}, 0, COMMAND_FREQ, TimeUnit.MILLISECONDS);
	}
	
	
	public static void stopSending() {
		//Method to stop sending commands to Scratch
		if (isConnected && !mScheduledExecutorService.isShutdown())
			mScheduledExecutorService.shutdownNow();
	}
		
	private static void sendCommand(String command) {
		//Public method to send command to Scratch
		
		if (isConnected) {
			try {
				//Convert command length to a four-byte size field
	            byte[] size = {0,0,0,0};
	            int len = command.length();
	            
	            size[0] = (byte) (len >> 24);
	            size[1] = (byte) (len >> 16);
	            size[2] = (byte) (len >> 8);
	            size[3] = (byte) (len);
	            
				//Send four-byte size field
	            for (int i=0; i<4; i++)  
	            	mOutputStream.write(size[i]);
            
	            //Send command to Scratch
	            mOutputStream.write(command.getBytes());
	            	            
			} catch (SocketException e) {
				//Socket Disconnected
				Log.i(LOG_TAG, "Send Exception: " + e.getMessage());
				mHandler.obtainMessage(DISCONNECTED).sendToTarget();
				closeSocket();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		closeSocket();
	}


}
