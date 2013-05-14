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
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.speech.RecognizerIntent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ControllerView extends Activity {
	
	private final static int RECOGNITION_RESULT = 1;
	
	//Private Object declaration
	private static Context mContext;
	private static ImageButton dpadButton;
	private static ImageButton selectButton;
	private static ImageButton startButton;
	private static ImageButton menuButton;
	private static ImageButton micButton;
	private static ListView menuList;
	private static MessageHandler mHandler;
	private static RelativeLayout controllerView;
	private static RelativeLayout menuView;
	private static TextView xText;
	private static TextView yText;
	private static TextView zText;
	private static TextView lightText;
	private static TextView connectionText;
	
	//Private primitives declaration
	private boolean switchActivities;
	private boolean isPaused;
	private boolean setDPadPos;
	private int firstQuadrant;
	private int secondQuadrant;
	
	//Public Object declaration
	public static ImageButton aButton;
	public static ImageButton bButton;
	public static ImageButton xButton;
	public static ImageButton yButton;
	
	//public primitives declaration
	public static boolean upButtonPressed;
	public static boolean downButtonPressed;
	public static boolean leftButtonPressed;
	public static boolean rightButtonPressed;
		
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_controller_view);
		
		//Initialize the Context and MessageHandler
		mContext = this;
		mHandler = new MessageHandler();
		
		setupView();

		//Start SensorService and pass it the MessageHandler
		startService(new Intent(mContext, SensorService.class));
		SensorService.setHandler(mHandler);
		
		//Start SocketService and pass it the MessageHandler and View
		startService(new Intent(mContext, SocketService.class));
		SocketService.setHandler(mHandler);
		
		//Check if SocketService is connected and set the connection label accordingly
		if (SocketService.isConnected) {
			connectionText.setTextColor(mContext.getResources().getColor(R.color.connection_green));
			connectionText.setText(R.string.connected);
		} else {
			//SocketService.openIpDialog(mContext);
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
					Toast.makeText(mContext, "Sorry, didn't understand:\n\"" + command + "\"", Toast.LENGTH_SHORT).show();
				}
			}
			break;
		}
	}
	
	@Override public void onWindowFocusChanged(boolean hasFocus) {
		//When the view is done being created, calculate the coords of the DPad
		if (!setDPadPos) {
			int height = dpadButton.getHeight();
			
			firstQuadrant = (int) (height * .3);
			secondQuadrant = (int) (height * .7);
			
			setDPadPos = true;
		}
		
	}
	
	private void setupView() {
		//Method to initialize Buttons and Touch Listeners
		
		//Create an OnTouchListener to play sound and vibrate when a button is pressed
		final AudioManager mAudioManager = (AudioManager) getSystemService(Service.AUDIO_SERVICE);
		final Vibrator v = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
		OnTouchListener touch = new OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent e) {
				if (e.getAction()==MotionEvent.ACTION_DOWN) {
					
					//Vibrate for 15 milliseconds
					v.vibrate(15);
					
					//Play default button click sound
					mAudioManager.playSoundEffect(SoundEffectConstants.CLICK);
				}
				return false;
			}
		};
				
		//Initialize Layouts
		controllerView = (RelativeLayout) findViewById(R.id.controller_view);
		menuView = (RelativeLayout) findViewById(R.id.menu_dropdown);
		
		//Initialize TextViews
		connectionText = (TextView) findViewById(R.id.connection_text);
		xText = (TextView) findViewById(R.id.x_text);
		yText = (TextView) findViewById(R.id.y_text);
		zText = (TextView) findViewById(R.id.z_text);
		lightText = (TextView) findViewById(R.id.light_text);
		
		//Initialize Buttons
		dpadButton = (ImageButton) findViewById(R.id.dpad_button);
		aButton = (ImageButton) findViewById(R.id.a_button);
		bButton = (ImageButton) findViewById(R.id.b_button);
		xButton = (ImageButton) findViewById(R.id.x_button);
		yButton = (ImageButton) findViewById(R.id.y_button);
		selectButton = (ImageButton) findViewById(R.id.select_button);
		startButton = (ImageButton) findViewById(R.id.start_button);
		menuButton = (ImageButton) findViewById(R.id.menu_button);
		micButton = (ImageButton) findViewById(R.id.mic_button);
		
		//Set Button OnTouchListeners
		aButton.setOnTouchListener(touch);
		bButton.setOnTouchListener(touch);
		xButton.setOnTouchListener(touch);
		yButton.setOnTouchListener(touch);
		
		//Directional Pad Touch Listener
		dpadButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				
				int touchX = (int) event.getX();
				int touchY = (int) event.getY();
				
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					checkTouchEvent(touchX, touchY);
					v.vibrate(15);
					mAudioManager.playSoundEffect(SoundEffectConstants.CLICK);
					break;
				case MotionEvent.ACTION_MOVE:
					checkTouchEvent(touchX, touchY);
					break;
				case MotionEvent.ACTION_UP:
					dpadButton.setBackgroundResource(R.drawable.dpad);
					upButtonPressed = false;
					downButtonPressed = false;
					leftButtonPressed = false;
					rightButtonPressed = false;
					break;
				}
				return false;
			}
			
		});
		
		//Select Button OnClickListener
		selectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SocketService.setGesture("Select");
			}
		});
		
		//Microphone Button OnClickListener
		micButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
				intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, 
						getClass().getPackage().getName());
				intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "[broadcast|send] (name of broadcast)\n[update|change] (name of sensor)");
				intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, 
						RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
				
				switchActivities = true;
				startActivityForResult(intent, 1);
				
			}
		});
		
		//Start Button OnClickListener
		startButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SocketService.setGesture("Start");
			}
		});
		
		//Connection Label OnClickListener
		connectionText.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				switchActivities = true;
				startActivityForResult(new Intent(mContext, IpDialog.class), 0);
			}
		});
		
		//Menu Button OnClickListener
		menuButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (menuView.getVisibility() == RelativeLayout.VISIBLE) {
					//If the Menu Layout is visible, hide it
					menuView.setVisibility(RelativeLayout.INVISIBLE);
				} else {
					//If the Menu Layout is not visible, show it and set
					//OnClickListener on the entire View
					menuView.setVisibility(RelativeLayout.VISIBLE);
					controllerView.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							//If the View is clicked, hide the Menu and remove
							//the View's OnClickListener
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
			public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
				menuView.setVisibility(RelativeLayout.INVISIBLE);
				switch (pos) {
				case 0:
					//Switch to the GestureView
					startActivity(new Intent(mContext, GestureView.class));
		        	switchActivities = true;
		        	finish();
					break;
				case 1:
					//Open the connection dialog
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
		
	}
	
	private void checkTouchEvent(int x, int y) {
		//Method to determine where the DPad is being pressed
		
		if (y < firstQuadrant) {
			upButtonPressed = true;
			downButtonPressed = false;
		} else if (y < secondQuadrant) {
			upButtonPressed = false;
			downButtonPressed = false;
		} else {
			upButtonPressed = false;
			downButtonPressed = true;
		}
		
		if (x < firstQuadrant) {
			leftButtonPressed = true;
			rightButtonPressed = false;
		} else if (x < secondQuadrant) {
			leftButtonPressed = false;
			rightButtonPressed = false;
		} else {
			leftButtonPressed = false;
			rightButtonPressed = true;
		}		
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
			startService(new Intent(mContext, SensorService.class));
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
