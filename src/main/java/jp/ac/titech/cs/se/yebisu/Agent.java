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
package jp.ac.titech.cs.se.yebisu;

import java.io.File;
import java.lang.instrument.Instrumentation;

/**
 * JDK 1.5 以降で導入された java.lang.instrument パッケージを利用し、アプリケーションの
 * 動的解析を行うためのエージェントのサンプル実装です。
 */
public class Agent {

	/** The version identifier for this agent */
	public static final String VERSION = "0.8.0"; //$NON-NLS-1$

	public static final String CONSUMER_KEY = "yebisu.consumer"; //$NON-NLS-1$

	/** デバッグメッセージの表示フラグ */
	public static boolean verbose;

	/** エージェントからのイベント通知を処理するリスナ */
	public static Consumer consumer;

	/**
	 * エージェントの実行を開始します。
	 * 
	 * このメソッドからエージェントの実行が開始することは Jar アーカイブのマニフェストファイル
	 * (META-INF/MANIFEST.MF) 内にある Premain-Class エントリで指定されています。
	 * 
	 * @param agentArgs エージェント引数
	 * @param inst バイトコードを変換するためのサービス
	 * @throws Exception
	 */
	public static void premain(String agentArgs, Instrumentation inst)
			throws Exception {
		/* バナーメッセージを表示 */
		error("YEBISU - YEt another Bytecode Instrumentation agent for Software Understanding. Version " + VERSION); //$NON-NLS-1$
		error("Copyright (c) 2010-2012 Saeki Lab. at Tokyo Institute of Technology. All Rights Reserved."); //$NON-NLS-1$
		
		/* JVMシャットダウン時のクリーンアップ処理を登録 */
        Runtime.getRuntime().addShutdownHook(new Thread() {
        	public void run() { notifyJVMShutDown(); }
        });

		/* プロパティファイルを解析し、バイトコード変換モジュールとエージェントを登録 */
		String propPath = agentArgs;
		if (propPath == null) {
			propPath = "./" + Settings.PROPERTY_FILE_NAME; //$NON-NLS-1$
		}
		Settings settings = new Settings(new File(propPath)); //$NON-NLS-1$
		verbose = settings.getBoolean("yebisu.verbose"); //$NON-NLS-1$
		consumer = createConsumer(settings);
		inst.addTransformer(new Brewer(settings));

		if (inst.isRetransformClassesSupported()) {
			/*
			 * すでにロード済みのクラスは ClassFileTransformer にチェックされないので
			 * 明示的にバイトコードの再変換を指示する
			 */
			for (Class<?> clazz : inst.getAllLoadedClasses()) {
				try {
					/* プリミティブ型や配列クラスは再定義できないのでスキップ */
					if (inst.isModifiableClass(clazz)) {
						inst.retransformClasses(clazz);
					}
				} catch (Exception e) {
					error("Brewing failed: " + clazz.getCanonicalName());
				}
			}
		}
	}

	private static Consumer createConsumer(Settings config) throws Exception {
		Consumer consumer = null;
		String className = config.getString(CONSUMER_KEY);
		Object object = Class.forName(className).newInstance();
		if (object instanceof Consumer) {
			consumer = (Consumer) object;
			consumer.initialize(config);
		}
		return consumer;
	}

	public static void notifyClassLoad(long classId, String className) {
		consumer.onClassLoad(classId, className);
	}

	public static void notifyMethodInstrument(long classId, long methodId,
			String methodName) {
		consumer.onMethodInstrument(classId, methodId, methodName);
	}

	public static void notifyMethodEntry(long methodId, Object context,
			Object[] arguments) {
		consumer.onMethodEntry(methodId, context, arguments);
	}

	public static void notifyMethodExit(long methodId, Object returnValue) {
		consumer.onMethodExit(methodId, returnValue);
	}

	public static void notifyJVMShutDown() {
		consumer.onJVMShutDown();
	}

	/**
	 * 標準エラー出力にデバッグメッセージを出力します。
	 * 
	 * このメソッドはデバッグメッセージの制御フラグ (yebisu.verbose プロパティ) が true の
	 * 場合にのみ動作し、false の場合には何も行いません。
	 * 
	 * @param message デバッグメッセージ
	 */
	public static void debug(String message) {
		if (verbose) {
			System.err.println(message);
		}
	}

	/**
	 * 標準エラー出力にエラーメッセージを出力します。
	 * 
	 * @param message デバッグメッセージ
	 */
	public static void error(String message) {
		System.err.println(message);
	}

}
