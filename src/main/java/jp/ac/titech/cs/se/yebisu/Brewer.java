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
 * java.lang.instrument.ClassFileTransformer インタフェースのサンプル実装です。
 * 
 * このサンプル実装では Javassist を用いてバイトコード変換を行うことにより、クラスファイル内に
 * イベント通知のためのメソッド呼び出し文を挿入します。
 */
public class Brewer implements ClassFileTransformer {

	/** クラス識別子を連番で生成するためのシーケンス */
	private Sequence classIdSequence = new Sequence();

	/** メソッド識別子を連番で生成するためのシーケンス */
	private Sequence methodIdSequence = new Sequence();

	/** 既知のクラスローダーの集合 */
	private Set<ClassLoader> classLoaders;
	
	/** メソッドの書き換え対象に含めるパッケージ名／クラス名 (前方一致) のリスト */
	private List<String> includedPrefixes;

	/** メソッドの書き換え対象に含めないパッケージ名／クラス名 (前方一致) のリスト */
	private List<String> excludedPrefixes;

	public Brewer(Settings settings) {
		this.classLoaders = new HashSet<ClassLoader>();
		this.includedPrefixes = settings.getStringList("yebisu.include.prefix"); //$NON-NLS-1$
		this.excludedPrefixes = settings.getStringList("yebisu.exclude.prefix"); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * このメソッドは Java 仮想マシンに新しいクラスがロードされる直前に呼び出され、クラスの内容
	 * を動的に書き換える機会が与えられます。
	 * 
	 * @see
	 * java.lang.instrument.ClassFileTransformer#transform(java.lang.ClassLoader
	 * , java.lang.String, java.lang.Class, java.security.ProtectionDomain,
	 * byte[])
	 */
	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {
		/* エージェントにクラスロードイベントを通知する */
		long classId = classIdSequence.next();
		className = className.replace('/', '.');
		Agent.notifyClassLoad(classId, className);

		if (shouldBeInstrumented(className)) {
			Agent.debug("Brewing: " + className);
			try {
				/* バイト配列を Javassist で解析してコンパイル時クラス (CtClass) を生成 */
				ClassPool pool = ClassPool.getDefault();
				if (loader != null && !classLoaders.contains(loader)) {
					classLoaders.add(loader);
					pool.appendClassPath(new LoaderClassPath(loader));
				}
				CtClass clazz = pool.makeClass(new ByteArrayInputStream(classfileBuffer));

				/* このクラスで宣言された振る舞い (コンストラクタとメソッド) を書き換える */
				boolean touched = false;
				for (CtBehavior behavior : clazz.getDeclaredBehaviors()) {
					if (behavior.isEmpty()
							|| Modifier.isNative(behavior.getModifiers())) {
						continue;
					}

					/* エージェントにメソッド名を登録し、メソッド ID を取得 */
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

		/* null を返却するとバイトコード変換せず、元のクラスをロードする */
		return null;
	}

	/**
	 * 引数で指定された名前を持つクラスが、バイトコード変換の対象になるかどうかを判定します。
	 * 
	 * @param className
	 *            クラスの完全修飾名
	 * @return バイトコード変換の対象となる場合は true, 対象外の場合は false
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
	 * Javassist を用いてバイトコードを変換し、コンストラクタおよびメソッドの先頭／末尾に
	 * プローブを挿入します。
	 * 
	 * ここでは、本体が実装されているコンストラクタやメソッドのみを変換します。インタフェースや
	 * 抽象メソッド、継承されたメソッド (オーバーライドされていないもの)、ネイティブメソッドは
	 * コンパイル不能例外が発生するためスキップします。
	 * 
	 * 参考サイト:
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
	 *            オリジナルのクラスのバイトコード
	 * @return プローブが挿入されたクラスのバイトコード
	 * @throws IOException
	 *             入出力例外が発生した場合
	 * @throws CannotCompileException
	 *             プローブ (メソッド呼び出し文) のコンパイル例外が発生した場合
	 */
	private boolean instrument(long methodId, CtBehavior behavior)
			throws CannotCompileException {
		/* メソッドの先頭に notifyMethodEntry メソッドの呼び出しを挿入 */
		boolean dollarZeroAvailable = (behavior instanceof CtMethod)
				&& !Modifier.isStatic(behavior.getModifiers());
		Object entryArgs = new Object[] { methodId,
				dollarZeroAvailable ? "$0" : "null", "$args" };
		behavior.insertBefore(methodEntryProbe.format(entryArgs));

		/* メソッドの末尾に notifyMethodExit メソッドの呼び出しを挿入 */
		Object exitArgs = new Object[] { methodId, "($w)$_" };
		behavior.insertAfter(methodExitProbe.format(exitArgs), true);
		
		return true;
	}

	/** メソッドの先頭に追加されるプローブのテンプレート */
	public static final MessageFormat methodEntryProbe = new MessageFormat(
			"jp.ac.titech.cs.se.yebisu.Agent.notifyMethodEntry({0,number,0}L, {1}, {2});"); //$NON-NLS-1$

	/** メソッドの末尾に追加されるプローブのテンプレート */
	public static final MessageFormat methodExitProbe = new MessageFormat(
			"jp.ac.titech.cs.se.yebisu.Agent.notifyMethodExit({0,number,0}L, {1});"); //$NON-NLS-1$

}
