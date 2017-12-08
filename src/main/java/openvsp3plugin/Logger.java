/**
 * Copyright 2017 United States Government as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * 
 * All Rights Reserved.
 * 
 * The OpenVSP3Plugin platform is licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0. 
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package openvsp3plugin;

import javafx.beans.property.SimpleObjectProperty;

class Logger {
	
	static enum LogLevel {TRACE, DEBUG, INFO, WARN, FATAL, OFF};
	static SimpleObjectProperty<LogLevel> loggingLevel = new SimpleObjectProperty<>(LogLevel.OFF);
	private final String LOGGINGCLASSNAME;
	
	// initial levels should not override previously set levels
	static void initLogLevel(String level) {
		if (loggingLevel.getValue().equals(LogLevel.OFF)) setLogLevel(level);
	}
	
	// level is global so static
	static void setLogLevel(String level) {
		if (level.isEmpty()) return;
		switch (level.substring(0, 1).toUpperCase()) {
			case "T": loggingLevel.setValue(LogLevel.TRACE); break;
			case "D": loggingLevel.setValue(LogLevel.DEBUG); break;
			case "I": loggingLevel.setValue(LogLevel.INFO);  break;
			case "W": loggingLevel.setValue(LogLevel.WARN);  break;
			case "F": loggingLevel.setValue(LogLevel.FATAL); break;
			case "O": loggingLevel.setValue(LogLevel.OFF); break;
		}
	}
	
	Logger(String classname) {
		LOGGINGCLASSNAME = classname;
	}
	
	void log(String className, LogLevel level, String message) {
		if (level.ordinal() < loggingLevel.getValue().ordinal()) return; // skip if below loggingLevel
		System.out.println(String.format("%30s %5s %22s - %s", Thread.currentThread().getName(), level, className, message));
	}
	
	void trace(String message) {
		log(LOGGINGCLASSNAME, LogLevel.TRACE, message);
	}
	
	void debug(String message) {
		log(LOGGINGCLASSNAME, LogLevel.DEBUG, message);
	}
	
	void info(String message) {
		log(LOGGINGCLASSNAME, LogLevel.INFO, message);
	}
	
	void warn(String message) {
		log(LOGGINGCLASSNAME, LogLevel.WARN, message);
	}
	
	void fatal(String message) {
		log(LOGGINGCLASSNAME, LogLevel.FATAL, message);
	}
}
