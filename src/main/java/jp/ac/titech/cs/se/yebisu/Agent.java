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
 * JDK 1.5 �ȍ~�œ������ꂽ java.lang.instrument �p�b�P�[�W�𗘗p���A�A�v���P�[�V������
 * ���I��͂��s�����߂̃G�[�W�F���g�̃T���v�������ł��B
 */
public class Agent {

	/** The version identifier for this agent */
	public static final String VERSION = "0.8.0"; //$NON-NLS-1$

	public static final String CONSUMER_KEY = "yebisu.consumer"; //$NON-NLS-1$

	/** �f�o�b�O���b�Z�[�W�̕\���t���O */
	public static boolean verbose;

	/** �G�[�W�F���g����̃C�x���g�ʒm���������郊�X�i */
	public static Consumer consumer;

	/**
	 * �G�[�W�F���g�̎��s���J�n���܂��B
	 * 
	 * ���̃��\�b�h����G�[�W�F���g�̎��s���J�n���邱�Ƃ� Jar �A�[�J�C�u�̃}�j�t�F�X�g�t�@�C��
	 * (META-INF/MANIFEST.MF) ���ɂ��� Premain-Class �G���g���Ŏw�肳��Ă��܂��B
	 * 
	 * @param agentArgs �G�[�W�F���g����
	 * @param inst �o�C�g�R�[�h��ϊ����邽�߂̃T�[�r�X
	 * @throws Exception
	 */
	public static void premain(String agentArgs, Instrumentation inst)
			throws Exception {
		/* �o�i�[���b�Z�[�W��\�� */
		error("YEBISU - YEt another Bytecode Instrumentation agent for Software Understanding. Version " + VERSION); //$NON-NLS-1$
		error("Copyright (c) 2010-2012 Saeki Lab. at Tokyo Institute of Technology. All Rights Reserved."); //$NON-NLS-1$
		
		/* JVM�V���b�g�_�E�����̃N���[���A�b�v������o�^ */
        Runtime.getRuntime().addShutdownHook(new Thread() {
        	public void run() { notifyJVMShutDown(); }
        });

		/* �v���p�e�B�t�@�C������͂��A�o�C�g�R�[�h�ϊ����W���[���ƃG�[�W�F���g��o�^ */
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
			 * ���łɃ��[�h�ς݂̃N���X�� ClassFileTransformer �Ƀ`�F�b�N����Ȃ��̂�
			 * �����I�Ƀo�C�g�R�[�h�̍ĕϊ����w������
			 */
			for (Class<?> clazz : inst.getAllLoadedClasses()) {
				try {
					/* �v���~�e�B�u�^��z��N���X�͍Ē�`�ł��Ȃ��̂ŃX�L�b�v */
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
	 * �W���G���[�o�͂Ƀf�o�b�O���b�Z�[�W���o�͂��܂��B
	 * 
	 * ���̃��\�b�h�̓f�o�b�O���b�Z�[�W�̐���t���O (yebisu.verbose �v���p�e�B) �� true ��
	 * �ꍇ�ɂ̂ݓ��삵�Afalse �̏ꍇ�ɂ͉����s���܂���B
	 * 
	 * @param message �f�o�b�O���b�Z�[�W
	 */
	public static void debug(String message) {
		if (verbose) {
			System.err.println(message);
		}
	}

	/**
	 * �W���G���[�o�͂ɃG���[���b�Z�[�W���o�͂��܂��B
	 * 
	 * @param message �f�o�b�O���b�Z�[�W
	 */
	public static void error(String message) {
		System.err.println(message);
	}

}
