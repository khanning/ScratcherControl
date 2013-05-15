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

public class VoiceCommand {
	
	private ArrayList<String> command;
	private String type = "";
	private String variable = "";
	private String value = "";
	private String output = "";
	private boolean isValid = true;
	
	public VoiceCommand(ArrayList<String> input) {
		command = input;
		parseCommand();
	}
	
	private void parseCommand() {
		
		//Find the type of command
		for (int i = 0; i < command.size(); i++) {
			
			//If we haven't already found the trigger
			if (type.equals("")) {
				
				String[] words = command.get(i).split(" ");
				if (words[0].equals("broadcast") || words[0].equals("send"))
					type = "broadcast";
				else if (words[0].equals("update") || words[0].equals("change"))
					type = "sensor-update";
				else
					isValid = false;
				
			}
		}
		
		if (isValid) {
			
			String[] words = command.get(0).split(" ");
			
			if (words.length < 5) {
				variable = "\"";
				if (type.equals("broadcast") && words.length > 1) {
					
					for (int n = 1; n < words.length; n++) {
						variable += words[n];
						if (n < words.length - 1)
							variable += " ";
					}
					variable += "\"";
					
					
					output = type + " " + variable;
					
				} else if (type.equals("sensor-update") && words.length > 2) {
					
					for (int n = 1; n < words.length - 1; n++) {
						variable += words[n];
						if (n < words.length - 2)
							variable += " ";
					}
					variable += "\"";							
					
					//Check the value
					String lastWord = getInt(words[words.length - 1]);
					if (lastWord != null)
						value = lastWord;
					else
						isValid = false;
					
					output = type + " " + variable  + " " + value;
					
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
			output = command.get(0);
		}
		
	}

	public boolean isValid() {
		return isValid;
	}
	
	public String getCommand() {
		return output;
	}
	
	private String getInt(String input) {
		try {
			int number = Integer.parseInt(input);
			return Integer.toString(number);
		} catch (NumberFormatException e) {
			return null;
		}
	}

}
