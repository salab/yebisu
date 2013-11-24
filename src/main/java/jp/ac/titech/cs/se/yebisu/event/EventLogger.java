/*
 * Copyright (c) 2010-2012 Saeki Lab. at Tokyo Institute of Technology.
 * All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jp.ac.titech.cs.se.yebisu.event;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.MessageFormat;

import jp.ac.titech.cs.se.yebisu.Consumer;
import jp.ac.titech.cs.se.yebisu.Settings;

public class EventLogger implements Consumer {
	
	public static final String FILE_PATTERN_KEY = "yebisu.event.csv.file.pattern"; //$NON-NLS-1$

	private PrintWriter out;

	public void initialize(Settings settings) throws Exception {
		File logFile = new File(MessageFormat.format(
				settings.getString(FILE_PATTERN_KEY),
				System.currentTimeMillis()));

		File logDir = logFile.getParentFile();
		if (!logDir.exists()) {
			logDir.mkdirs();
		}

		out = new PrintWriter(new BufferedWriter(new FileWriter(logFile)));
	}

	public void onClassLoad(long classId, String className) {
		log("ClassLoad", String.valueOf(classId), '"' + className + '"');
	}

	public void onMethodInstrument(long classId, long methodId,
			String methodName) {
		log("MethodInstrument", String.valueOf(classId),
				String.valueOf(methodId), '"' + methodName + '"');
	}

	public void onMethodEntry(long methodId, Object context, Object[] arguments) {
		log("MethodEntry", String.valueOf(methodId));
	}

	public void onMethodExit(long methodId, Object returnValue) {
		log("MethodExit", String.valueOf(methodId));
	}

	public void onJVMShutDown() {
		log("JVMShutdown");
		out.close();
	}

	private void log(String eventType, String... messages) {
		StringBuffer buf = new StringBuffer()
				.append(System.currentTimeMillis()).append(',')
				.append(Thread.currentThread().getId()).append(',')
				.append(eventType);
		for (String message : messages) {
			buf.append(',').append(message);
		}
		out.println(buf.toString());
	}

}
