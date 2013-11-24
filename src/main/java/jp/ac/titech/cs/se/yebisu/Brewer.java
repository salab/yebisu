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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;

/**
 * java.lang.instrument.ClassFileTransformer �C���^�t�F�[�X�̃T���v�������ł��B
 * 
 * ���̃T���v�������ł� Javassist ��p���ăo�C�g�R�[�h�ϊ����s�����Ƃɂ��A�N���X�t�@�C������
 * �C�x���g�ʒm�̂��߂̃��\�b�h�Ăяo������}�����܂��B
 */
public class Brewer implements ClassFileTransformer {

	/** �N���X���ʎq��A�ԂŐ������邽�߂̃V�[�P���X */
	private Sequence classIdSequence = new Sequence();

	/** ���\�b�h���ʎq��A�ԂŐ������邽�߂̃V�[�P���X */
	private Sequence methodIdSequence = new Sequence();

	/** ���m�̃N���X���[�_�[�̏W�� */
	private Set<ClassLoader> classLoaders;
	
	/** ���\�b�h�̏��������ΏۂɊ܂߂�p�b�P�[�W���^�N���X�� (�O����v) �̃��X�g */
	private List<String> includedPrefixes;

	/** ���\�b�h�̏��������ΏۂɊ܂߂Ȃ��p�b�P�[�W���^�N���X�� (�O����v) �̃��X�g */
	private List<String> excludedPrefixes;

	public Brewer(Settings settings) {
		this.classLoaders = new HashSet<ClassLoader>();
		this.includedPrefixes = settings.getStringList("yebisu.include.prefix"); //$NON-NLS-1$
		this.excludedPrefixes = settings.getStringList("yebisu.exclude.prefix"); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * ���̃��\�b�h�� Java ���z�}�V���ɐV�����N���X�����[�h����钼�O�ɌĂяo����A�N���X�̓��e
	 * �𓮓I�ɏ���������@��^�����܂��B
	 * 
	 * @see
	 * java.lang.instrument.ClassFileTransformer#transform(java.lang.ClassLoader
	 * , java.lang.String, java.lang.Class, java.security.ProtectionDomain,
	 * byte[])
	 */
	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {
		/* �G�[�W�F���g�ɃN���X���[�h�C�x���g��ʒm���� */
		long classId = classIdSequence.next();
		className = className.replace('/', '.');
		Agent.notifyClassLoad(classId, className);

		if (shouldBeInstrumented(className)) {
			Agent.debug("Brewing: " + className);
			try {
				/* �o�C�g�z��� Javassist �ŉ�͂��ăR���p�C�����N���X (CtClass) �𐶐� */
				ClassPool pool = ClassPool.getDefault();
				if (loader != null && !classLoaders.contains(loader)) {
					classLoaders.add(loader);
					pool.appendClassPath(new LoaderClassPath(loader));
				}
				CtClass clazz = pool.makeClass(new ByteArrayInputStream(classfileBuffer));

				/* ���̃N���X�Ő錾���ꂽ�U�镑�� (�R���X�g���N�^�ƃ��\�b�h) ������������ */
				boolean touched = false;
				for (CtBehavior behavior : clazz.getDeclaredBehaviors()) {
					if (behavior.isEmpty()
							|| Modifier.isNative(behavior.getModifiers())) {
						continue;
					}

					/* �G�[�W�F���g�Ƀ��\�b�h����o�^���A���\�b�h ID ���擾 */
					long methodId = methodIdSequence.next();
					Agent.notifyMethodInstrument(classId, methodId,
							behavior.getLongName());
					touched |= instrument(methodId, behavior);
				}
				return touched ? clazz.toBytecode() : null;
			} catch (Exception e) {
				Agent.error("Brewing failed: " + className);
				e.printStackTrace();
			}
		} else {
			Agent.debug("Ignored: " + className);
		}

		/* null ��ԋp����ƃo�C�g�R�[�h�ϊ������A���̃N���X�����[�h���� */
		return null;
	}

	/**
	 * �����Ŏw�肳�ꂽ���O�����N���X���A�o�C�g�R�[�h�ϊ��̑ΏۂɂȂ邩�ǂ����𔻒肵�܂��B
	 * 
	 * @param className
	 *            �N���X�̊��S�C����
	 * @return �o�C�g�R�[�h�ϊ��̑ΏۂƂȂ�ꍇ�� true, �ΏۊO�̏ꍇ�� false
	 */
	private boolean shouldBeInstrumented(String className) {
		return isIncluded(className) && !isExcluded(className);
	}

	private boolean isIncluded(String className) {
		if (includedPrefixes == null || includedPrefixes.isEmpty()) {
			return true;
		}

		for (String prefix : includedPrefixes) {
			if (className.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	private boolean isExcluded(String className) {
		if (excludedPrefixes != null && !excludedPrefixes.isEmpty()) {
			for (String prefix : excludedPrefixes) {
				if (className.startsWith(prefix)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Javassist ��p���ăo�C�g�R�[�h��ϊ����A�R���X�g���N�^����у��\�b�h�̐擪�^������
	 * �v���[�u��}�����܂��B
	 * 
	 * �����ł́A�{�̂���������Ă���R���X�g���N�^�⃁�\�b�h�݂̂�ϊ����܂��B�C���^�t�F�[�X��
	 * ���ۃ��\�b�h�A�p�����ꂽ���\�b�h (�I�[�o�[���C�h����Ă��Ȃ�����)�A�l�C�e�B�u���\�b�h��
	 * �R���p�C���s�\��O���������邽�߃X�L�b�v���܂��B
	 * 
	 * �Q�l�T�C�g:
	 * http://java.dzone.com/articles/java-profiling-under-covers?page=0,2
	 * 
	 * <blockquote>
	 * The check for the class name is a workaround for a common issue -- a
	 * "no method body" CannotCompileException thrown by Javassist when the
	 * method is an inherited method. We'll explicitly display those exceptions
	 * in the next example of the profiler.
	 * </blockquote>
	 * 
	 * @param bytecode
	 *            �I���W�i���̃N���X�̃o�C�g�R�[�h
	 * @return �v���[�u���}�����ꂽ�N���X�̃o�C�g�R�[�h
	 * @throws IOException
	 *             ���o�͗�O�����������ꍇ
	 * @throws CannotCompileException
	 *             �v���[�u (���\�b�h�Ăяo����) �̃R���p�C����O�����������ꍇ
	 */
	private boolean instrument(long methodId, CtBehavior behavior)
			throws CannotCompileException {
		/* ���\�b�h�̐擪�� notifyMethodEntry ���\�b�h�̌Ăяo����}�� */
		boolean dollarZeroAvailable = (behavior instanceof CtMethod)
				&& !Modifier.isStatic(behavior.getModifiers());
		Object entryArgs = new Object[] { methodId,
				dollarZeroAvailable ? "$0" : "null", "$args" };
		behavior.insertBefore(methodEntryProbe.format(entryArgs));

		/* ���\�b�h�̖����� notifyMethodExit ���\�b�h�̌Ăяo����}�� */
		Object exitArgs = new Object[] { methodId, "($w)$_" };
		behavior.insertAfter(methodExitProbe.format(exitArgs), true);
		
		return true;
	}

	/** ���\�b�h�̐擪�ɒǉ������v���[�u�̃e���v���[�g */
	public static final MessageFormat methodEntryProbe = new MessageFormat(
			"jp.ac.titech.cs.se.yebisu.Agent.notifyMethodEntry({0,number,0}L, {1}, {2});"); //$NON-NLS-1$

	/** ���\�b�h�̖����ɒǉ������v���[�u�̃e���v���[�g */
	public static final MessageFormat methodExitProbe = new MessageFormat(
			"jp.ac.titech.cs.se.yebisu.Agent.notifyMethodExit({0,number,0}L, {1});"); //$NON-NLS-1$

}
