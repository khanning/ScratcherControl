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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class GestureView extends Activity implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {
	
	private final static String MIC_PREFS = "MicPrefs";
	
	//Private Object declaration
	private static Context mContext;
	private static ImageButton menuButton;
	private static LinearLayout micView;
	private static ListView menuList;
	private static ImageView micImage;
	private static RelativeLayout controllerView;
	private static RelativeLayout menuView;
	private static MessageHandler mHandler;
	private static SharedPreferences mSharedPreferences;
	private static SpeechCommand mSpeechCommand;
	private static TextView xText;
	private static TextView yText;
	private static TextView zText;
	private static TextView lightText;
	private static TextView connectionText;
	private static TextView micText;
	private GestureDetectorCompat mGesture;
	
	//Private primitive declaration
	private boolean switchActivities;
	private boolean isPaused;	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gesture_view);
		
		//Initialize the Context and MessageHandler
		mContext = this;
		mHandler = new MessageHandler();
		
		setupView();
		
		//Get Shared Preferences
		mSharedPreferences = getSharedPreferences(MIC_PREFS, 0);
		
		//Create a speech recognizer
		mSpeechCommand = new SpeechCommand(mContext, mHandler);
		
		//Start SensorService and pass it the MessageHandler
		startService(new Intent(this, SensorService.class));
		SensorService.setHandler(mHandler);
		
		//Start SocketService and pass it the MessageHandler and View
		startService(new Intent(this, SocketService.class));
		SocketService.setHandler(mHandler);
		
		//Initializes GestureDetector
		mGesture = new GestureDetectorCompat(this,this);
		
		//Check if SocketService is connected and set the connection label accordingly
		if (SocketService.isConnected) {
			connectionText.setTextColor(mContext.getResources().getColor(R.color.connection_green));
			connectionText.setText(R.string.connected);
		} else {
			switchActivities = true;
			startActivityForResult(new Intent(mContext, IpDialog.class), 0);
			connectionText.setTextColor(mContext.getResources().getColor(R.color.connection_red));
			connectionText.setText(R.string.not_connected);
		}
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
		switchActivities = false;
	}
	
	private void setupView() {
		//Method to initialize Buttons and set Listeners
		
		//Initialize Layouts
		controllerView = (RelativeLayout) findViewById(R.id.gesture_view);
		menuView = (RelativeLayout) findViewById(R.id.menu_dropdown);
		micView = (LinearLayout) findViewById(R.id.microphone);
		
		//Initialize ImageViews
		micImage = (ImageView) findViewById(R.id.mic_image);
		
		//Initialize TextViews
		xText = (TextView) findViewById(R.id.x_text);
		yText = (TextView) findViewById(R.id.y_text);
		zText = (TextView) findViewById(R.id.z_text);
		lightText = (TextView) findViewById(R.id.light_text);
		connectionText = (TextView) findViewById(R.id.connection_text);
		micText = (TextView) findViewById(R.id.mic_text);
		
		//Connection Label OnClickListener 
		connectionText.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				switchActivities = true;
				startActivityForResult(new Intent(mContext, IpDialog.class), 0);
			}
		});
		
		//Menu Button OnClickListener
		menuButton = (ImageButton) findViewById(R.id.menu_button);
		menuButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (menuView.getVisibility() == RelativeLayout.VISIBLE) {
					menuView.setVisibility(RelativeLayout.INVISIBLE);
				} else {
					menuView.setVisibility(RelativeLayout.VISIBLE);
					menuView.bringToFront();
					controllerView.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							menuView.setVisibility(RelativeLayout.INVISIBLE);
							controllerView.setOnClickListener(null);
						}
					});
				}
			}
		});
		
		//Create a custom Menu so the style can be universal for all android versions
		menuList = (ListView) findViewById(R.id.connection_menu);
		menuList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position,
					long id) {
				menuView.setVisibility(RelativeLayout.INVISIBLE);
				switch (position) {
				case 0:
					//Switch to the ControllerView
					startActivity(new Intent(mContext, ControllerView.class));
		        	switchActivities = true;
		        	finish();
					break;
				case 1:
					//Open the Connection Dialog
					switchActivities = true;
					startActivityForResult(new Intent(mContext, IpDialog.class), 0);
					break;
				case 2:
					//Exit
					switchActivities = false;
		        	finish();
					break;
				}
			}
		});
		
		//Microphone View OnClickListener
				micView.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						
						if (mSharedPreferences.getBoolean("voice_helper", false)) {
							mSpeechCommand.listen();
						} else {
							AlertDialog.Builder voiceHelper = new AlertDialog.Builder(mContext);
							voiceHelper.setTitle(getResources().getString(R.string.voice_helper_title));
							voiceHelper.setMessage(getResources().getString(R.string.voice_helper_text));
							voiceHelper.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									SharedPreferences.Editor editor = mSharedPreferences.edit();
									editor.putBoolean("voice_helper", true).commit();
									mSpeechCommand.listen();
								}
							});
							voiceHelper.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									
								}
							});
							
							voiceHelper.create().show();
							
						}				
					}
				});
		
	}

    public static class MessageHandler extends Handler {
    	//Handler to receive messages from SocketService and SensorService
    	
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case SocketService.CONNECTING:
				connectionText.setTextColor(mContext.getResources().getColor(R.color.connection_orange));
				connectionText.setText(R.string.connecting);
				break;
			case SocketService.CONNECTION_SUCCESS:
				connectionText.setTextColor(mContext.getResources().getColor(R.color.connection_green));
				connectionText.setText(R.string.connected);
				break;
			case SocketService.CONNECTION_FAILED:
				connectionText.setTextColor(mContext.getResources().getColor(R.color.connection_red));
				connectionText.setText(R.string.not_connected);
				Toast.makeText(mContext, "Connection Failed", Toast.LENGTH_SHORT).show();
				break;
			case SocketService.CONNECTION_REFUSED:
				connectionText.setTextColor(mContext.getResources().getColor(R.color.connection_red));
				connectionText.setText(R.string.not_connected);
				Toast.makeText(mContext, "Connection Refused", Toast.LENGTH_SHORT).show();
				break;
			case SocketService.DISCONNECTED:
				connectionText.setTextColor(mContext.getResources().getColor(R.color.connection_red));
				connectionText.setText(R.string.not_connected);
				break;
			case SocketService.SENSOR_UPDATE:
				String[] sensor = msg.obj.toString().split(",");
				xText.setText(sensor[0]);
				yText.setText(sensor[1]);
				zText.setText(sensor[2]);
				lightText.setText(sensor[3]);
				break;
			case SocketService.VOICE_INIT:
				micText.setTextColor(mContext.getResources().getColor(R.color.sensor_bg));
				micText.setText(mContext.getResources().getString(R.string.mic_initializing));
				micImage.setBackgroundResource(R.drawable.ic_menu_mic_black);
				break;
			case SocketService.VOICE_LISTEN:
				micText.setTextColor(mContext.getResources().getColor(R.color.mic_green));
				micText.setText(mContext.getResources().getString(R.string.mic_listen));
				micImage.setBackgroundResource(R.drawable.ic_menu_mic_green);
				break;
			case SocketService.VOICE_ANALYZING:
				micText.setTextColor(mContext.getResources().getColor(R.color.mic_blue));
				micText.setText(mContext.getResources().getString(R.string.mic_analyzing));
				micImage.setBackgroundResource(R.drawable.ic_menu_mic_blue);
				break;
			case SocketService.VOICE_RESULTS:
				String command = msg.obj.toString();
				Toast.makeText(mContext, command, Toast.LENGTH_SHORT).show();
				SocketService.setVoiceCommand(command);
				micText.setTextColor(mContext.getResources().getColor(R.color.sensor_bg_light));
				micText.setText(mContext.getResources().getString(R.string.mic_default));
				micImage.setBackgroundResource(R.drawable.ic_menu_mic);
				break;
			case SocketService.VOICE_CANCEL:
				micText.setTextColor(mContext.getResources().getColor(R.color.sensor_bg_light));
				micText.setText(mContext.getResources().getString(R.string.mic_default));
				micImage.setBackgroundResource(R.drawable.ic_menu_mic);
				Toast.makeText(mContext, mContext.getResources().getString(R.string.bad_voice_command), Toast.LENGTH_SHORT).show();
				break;
			}
		}
	};
	
	@Override
	public boolean onTouchEvent(MotionEvent e) {
		this.mGesture.onTouchEvent(e);
		return super.onTouchEvent(e);
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		//Receive a Fling action and determine the direction
		if (Math.abs(velocityX) > Math.abs(velocityY)) {
			if (velocityX > 0)
				SocketService.setGesture("flick_right");
			else
				SocketService.setGesture("flick_left");
		} else {
			if (velocityY > 0)
				SocketService.setGesture("flick_down");
			else
				SocketService.setGesture("flick_up");
		}
		
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		//Receive a Long Press action
		SocketService.setGesture("long_press");
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		//Receive a scroll action and determine the direction

		if (Math.abs(distanceX) > Math.abs(distanceY)) {
			if (distanceX > 0)
				SocketService.setGesture("scroll_left");
			else
				SocketService.setGesture("scroll_right");
		} else {
			if (distanceY > 0)
				SocketService.setGesture("scroll_up");
			else
				SocketService.setGesture("scroll_down");
		}
			
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		//Receive a Single Tap
		SocketService.setGesture("tap");
		return false;
	}
	
	@Override
	public boolean onDoubleTap(MotionEvent e) {
		//Receive a Double Tap
		SocketService.setGesture("double_tap");
		return false;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		return false;
	}
	
	@Override
	public void onPause() {
		super.onPause();
		if (!switchActivities) {
			//If the app is being paused, stop the SensorService and
			//stop sending SocketService updates
			stopService(new Intent(this, SensorService.class));
			SocketService.stopSending();
			isPaused = true;
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (isPaused) {
			//If resuming from paused, start the SensorService and
			//start sending SocketService updates
			startService(new Intent(this, SensorService.class));
			SocketService.startSending();
			isPaused = false;
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (!switchActivities) {
			//If the app is being closed, stop both services
			stopService(new Intent(this, SocketService.class));
			stopService(new Intent(this, SensorService.class));
		}
	}
	
}
