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
package jp.ac.titech.cs.se.yebisu.method;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import jp.ac.titech.cs.se.yebisu.Agent;
import jp.ac.titech.cs.se.yebisu.Consumer;
import jp.ac.titech.cs.se.yebisu.Settings;

public class CallGraphCollector implements Consumer {

	public static final String FILE_PATTERN_KEY = "yebisu.callgraph.csv.file.pattern"; //$NON-NLS-1$

	private File logFile;

	private Map<Long, String> methodNames;

	private Map<Long, Map<Long, Long>> dependencies;

	private Map<Long, Stack<Long>> callStacks;

	public void initialize(Settings settings) throws Exception {
		logFile = new File(MessageFormat.format(
				settings.getString(FILE_PATTERN_KEY),
				System.currentTimeMillis()));

		File logDir = logFile.getParentFile();
		if (!logDir.exists()) {
			logDir.mkdirs();
		}

		methodNames = new HashMap<Long, String>();
		dependencies = new HashMap<Long, Map<Long, Long>>();
		callStacks = new HashMap<Long, Stack<Long>>();
	}

	public void onClassLoad(long classId, String className) {
		// NOP
	}

	public void onMethodInstrument(long classId, long methodId,
			String methodName) {
		methodNames.put(methodId, methodName);
	}

	public void onMethodEntry(long methodId, Object context, Object[] arguments) {
		/*
		 * 呼び出しスタックの頂上から呼び出し元のメソッドID (callerId) を取得して
		 * callerId -> methodId の依存性を追加する
		 */
		Stack<Long> stack = getCallStack();
		long callerId = stack.isEmpty() ? 0 : stack.lastElement();

		Map<Long, Long> edge = getCallees(callerId);
		long frequency = edge.containsKey(methodId) ? edge.get(methodId) : 0;
		edge.put(methodId, frequency + 1);

		/* 呼び出しスタックに現在のメソッド ID を積む */
		stack.push(methodId);
	}

	private Stack<Long> getCallStack() {
		long threadId = Thread.currentThread().getId();

		Stack<Long> stack;
		if (callStacks.containsKey(threadId)) {
			stack = callStacks.get(threadId);
		} else {
			stack = new Stack<Long>();
			callStacks.put(threadId, stack);
		}
		return stack;
	}

	private Map<Long, Long> getCallees(long methodId) {
		Map<Long, Long> callees;
		if (dependencies.containsKey(methodId)) {
			callees = dependencies.get(methodId);
		} else {
			callees = new HashMap<Long, Long>();
			dependencies.put(methodId, callees);
		}
		return callees;
	}

	public void onMethodExit(long methodId, Object returnValue) {
		getCallStack().pop();
	}

	public void onJVMShutDown() {
		try {
			save();
		} catch (IOException e) {
			Agent.error("Caught an exception while saving call graph.");
			e.printStackTrace();
		}
	}

	private void save() throws IOException {
		PrintWriter out = null;
		try {
			out = new PrintWriter(new BufferedWriter(new FileWriter(logFile)));
			Set<Long> methodIds = new HashSet<Long>();

			for (Map.Entry<Long, Map<Long, Long>> entry : dependencies.entrySet()) {
				long callerId = entry.getKey();
				methodIds.add(callerId);

				for (Map.Entry<Long, Long> edge : entry.getValue().entrySet()) {
					long calleeId = edge.getKey();
					long frequency = edge.getValue();
					out.println("Edge," + callerId + ',' + calleeId + ',' + frequency);
					methodIds.add(calleeId);
				}
			}

			for (long methodId : methodIds) {
				String methodName = methodNames.get(methodId);
				out.println("Node," + methodId + ",\"" + methodName + '"');
			}
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

}