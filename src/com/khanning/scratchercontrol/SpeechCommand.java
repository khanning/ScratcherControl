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

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

public class SpeechCommand {
	
	private static Context mContext;
	private static Intent recognizerIntent;
	private static Handler messageHandler;
	private static ScheduledExecutorService listenTimer;
	private static ScheduledExecutorService analyzeTimer;
	private static SpeechRecognizer mRecognizer;
	private static RecognitionListener mRecognitionListener;
	
	private boolean isValid = true;
	
	private boolean isListenerRunning;
	private boolean isAnalyzing;
	
	public SpeechCommand(Context context, Handler handler) {
		
		mContext = context;
		messageHandler = handler;
		
		recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 150);
		recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, 
				getClass().getPackage().getName());
		recognizerIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "[broadcast|send] (broadcast)\n[update|change] (sensor) (value)");
		
		mRecognitionListener = new RecognitionListener() {

			@Override
			public void onBeginningOfSpeech() {
				isListenerRunning = true;
				Log.i("ScratcherControl", "Beginning Speech");
			}

			@Override
			public void onBufferReceived(byte[] arg0) {
				
			}

			@Override
			public void onEndOfSpeech() {
				Log.i("ScratcherControl", "End of Speech");
				messageHandler.obtainMessage(SocketService.VOICE_ANALYZING).sendToTarget();
			
				analyzeTimer = Executors.newSingleThreadScheduledExecutor();
				analyzeTimer.schedule(new Runnable() {
					@Override
					public void run() {
						if (!isAnalyzing) {
							messageHandler.obtainMessage(SocketService.VOICE_CANCEL, mContext.getResources().getString(R.string.bad_voice_command)).sendToTarget();
							listenTimer.shutdownNow();
							isListenerRunning = false;
							mRecognizer.destroy();
						}
					}
					
				}, 5, TimeUnit.SECONDS);
			}

			@Override
			public void onError(int arg0) {
				
			}

			@Override
			public void onEvent(int arg0, Bundle arg1) {
				
			}

			@Override
			public void onPartialResults(Bundle arg0) {
				Log.i("ScratcherControl", "Partial");
			}

			@Override
			public void onReadyForSpeech(Bundle arg0) {
				messageHandler.obtainMessage(SocketService.VOICE_LISTEN).sendToTarget();
				
				listenTimer = Executors.newSingleThreadScheduledExecutor();
				listenTimer.schedule(new Runnable() {
					@Override
					public void run() {
						if (!isListenerRunning) {
							Log.i("ScratcherControl", "Test " + isListenerRunning);
							messageHandler.obtainMessage(SocketService.VOICE_CANCEL).sendToTarget();
							isListenerRunning = false;
							mRecognizer.destroy();
						}
					}
					
				}, 4, TimeUnit.SECONDS);
				
			}

			@Override
			public void onResults(Bundle results) {
				
				analyzeTimer.shutdownNow();
				
				ArrayList<String> voiceList = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
				String command = null;
				if (voiceList != null)
					command = parseCommand(voiceList);
				else
					command = mContext.getResources().getString(R.string.bad_voice_command);
				
				messageHandler.obtainMessage(SocketService.VOICE_RESULTS, command).sendToTarget();
								
				listenTimer.shutdownNow();
				isListenerRunning = false;
				mRecognizer.destroy();
				
			}

			@Override
			public void onRmsChanged(float arg0) {
								
			}
			
		};
		
	}
	
	public void listen() {
		if (!isListenerRunning) {
			messageHandler.obtainMessage(SocketService.VOICE_INIT).sendToTarget();
			mRecognizer = SpeechRecognizer.createSpeechRecognizer(mContext);
			mRecognizer.setRecognitionListener(mRecognitionListener);	
			mRecognizer.startListening(recognizerIntent);
		}	
	}
	
	private String parseCommand(ArrayList<String> commandList) {
		
		isValid = true;
		
		String type = "";
		String variable = "";
		String value = "";
		String output = "";
		
		//Find the type of command
		for (int i = 0; i < commandList.size(); i++) {
			
			//If we haven't already found the trigger
			if (type.equals("")) {
				
				String[] words = commandList.get(i).split(" ");
				if (words[0].equals("broadcast") || words[0].equals("send"))
					type = "broadcast";
				else if (words[0].equals("update") || words[0].equals("change"))
					type = "sensor-update";
				else
					isValid = false;
				
			}
		}
		
		if (isValid) {
			
			String[] words = commandList.get(0).split(" ");
			
			if (words.length < 5) {
				variable = "\"";
				if (type.equals("broadcast") && words.length > 1) {
					
					for (int n = 1; n < words.length; n++) {
						variable += words[n];
						if (n < words.length - 1)
							variable += " ";
					}
					variable += "\"";
										
				} else if (type.equals("sensor-update") && words.length > 2) {
					
					for (int n = 1; n < words.length - 1; n++) {
						variable += words[n];
						if (n < words.length - 2)
							variable += " ";
					}
					variable += "\"";							
					
					//Check the value
					value = words[words.length - 1];
										
				} else {
					isValid = false;
				}
				
			} else {
				isValid = false;
			}
			
		}		
		
		if (isValid) {
			if (type.equals("broadcast"))
				output = type + " " + variable;
			else if (type.equals("sensor-update"))
				output = type + " " + variable  + " " + value;
		} else {
			output = mContext.getResources().getString(R.string.bad_voice_command) + "\n\"" + commandList.get(0) + "\"";
		}
		
		return output;
		
	}
}
