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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Patterns;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class IpDialog extends Activity {
	
	private final static String PREFS_NAME = "SocketPrefs";
	private static SharedPreferences mSharedPreferences;
	private static TextView errorText;
	private static TextView ipText;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_ip);
		
		setDialogSize();
				
		errorText = (TextView) findViewById(R.id.dialog_error);
		ipText = (TextView) findViewById(R.id.dialog_ip_text);
		
		//Check if there is a cached IP address
		mSharedPreferences = getSharedPreferences(PREFS_NAME, 0);
		if (mSharedPreferences.contains("ip"))
			ipText.setText(mSharedPreferences.getString("ip", "false"));
		
		Button connectButton = (Button) findViewById(R.id.ip_dialog_connect);
		connectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				errorText.setText("");
				
				String ip = ipText.getText().toString();
				
				if (Patterns.IP_ADDRESS.matcher(ip).matches()) {
					
					//Cache the IP address
					SharedPreferences.Editor editor = mSharedPreferences.edit();
					editor.putString("ip", ip).commit();
					
					//If the socket is connected, close it
					if (SocketService.isConnected)
						SocketService.closeSocket();
					
					SocketService.connectTo(ip);
					
					finish();
				} else {
					errorText.setText(R.string.invalid_ip);
				}
				
				
				
			}
		});
		
		Button cancelButton = (Button) findViewById(R.id.ip_dialog_cancel);
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
	}
	
	private void setDialogSize() {
		//Method to detect screen size and update dialog window size
		
		DisplayMetrics mDisplayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);
		
		int width = mDisplayMetrics.widthPixels;
		int height = mDisplayMetrics.heightPixels;
		
		double x = Math.pow(width/mDisplayMetrics.xdpi,2);
		double y = Math.pow(height/mDisplayMetrics.ydpi,2);
		double screenInches = Math.sqrt(x+y);
	    
		if (screenInches > 6 && width > 1200) {
			getWindow().setLayout(900, WindowManager.LayoutParams.WRAP_CONTENT);
		} else {
			getWindow().setLayout(5*width/7, WindowManager.LayoutParams.WRAP_CONTENT);
		}
				
	}
	
}
