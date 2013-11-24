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

/**
 * �G�[�W�F���g����ʒm���󂯂郊�X�i�̃C���^�t�F�[�X�ł��B
 * 
 * ���̃C���^�t�F�[�X�����������N���X�̓��t���N�V������ʂ��ăC���X�^���X������邽�߁A��̈�����
 * ���R���X�g���N�^��񋟂���K�v������܂��B
 * 
 * @see java.lang.Class#newInstance()
 */
public interface Consumer {

	public void initialize(Settings settings) throws Exception;

	public void onClassLoad(long classId, String className);

	public void onMethodInstrument(long classId, long methodId,
			String methodName);

	public void onMethodEntry(long methodId, Object context, Object[] arguments);

	public void onMethodExit(long methodId, Object returnValue);

	public void onJVMShutDown();

}
