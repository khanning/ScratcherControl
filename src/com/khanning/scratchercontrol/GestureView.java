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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class GestureView extends Activity implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

	private static final int RECOGNITION_RESULT = 1;
	
	//Private Object declaration
	private static Context mContext;
	private static ImageButton menuButton;
	private static ImageButton micButton;
	private static ListView menuList;
	private static RelativeLayout controllerView;
	private static RelativeLayout menuView;
	private static MessageHandler mHandler;
	private static TextView xText;
	private static TextView yText;
	private static TextView zText;
	private static TextView lightText;
	private static TextView connectionText;
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
		
		switch (requestCode) {
		case RECOGNITION_RESULT:
			//Handle Voice Command
			if (data != null) {
								
				VoiceCommand voice = new VoiceCommand(data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS));
				String command = voice.getCommand();
				
				if (voice.isValid()) {
					Toast.makeText(mContext, command, Toast.LENGTH_SHORT).show();
					SocketService.setVoiceCommand(command);
				} else {
					Toast.makeText(mContext, "Sorry, didn't understand:\n" + command, Toast.LENGTH_SHORT).show();
				}
			}
			break;
		}
	}
	
	private void setupView() {
		//Method to initialize Buttons and set Listeners
		
		//Initialize Layouts
		controllerView = (RelativeLayout) findViewById(R.id.gesture_view);
		menuView = (RelativeLayout) findViewById(R.id.menu_dropdown);
		
		//Initialize TextViews
		xText = (TextView) findViewById(R.id.x_text);
		yText = (TextView) findViewById(R.id.y_text);
		zText = (TextView) findViewById(R.id.z_text);
		lightText = (TextView) findViewById(R.id.light_text);
		connectionText = (TextView) findViewById(R.id.connection_text);
		
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
		
		//Microphone Button OnClickListener
		micButton = (ImageButton) findViewById(R.id.mic_button);
		micButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
				intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, 
						getClass().getPackage().getName());
				intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "\"broadcast jump\",\"send move\"");
				intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, 
						RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
				
				switchActivities = true;
				startActivityForResult(intent, 1);
				
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
