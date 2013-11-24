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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * エージェントの設定情報をプロパティファイルから読み込んで管理します。
 */
public class Settings {

	/** プロパティファイルの名前 */
	public static final String PROPERTY_FILE_NAME = "yebisu.properties"; //$NON-NLS-1$

	private Properties prop;
	
	/**
	 * JAR アーカイブに格納されたデフォルトのプロパティファイルを使用して、設定情報オブジェクト
	 * を生成します。
	 * 
	 * @throws IOException
	 *             プロパティファイルの読み込みエラーが発生した場合
	 */
	public Settings() throws IOException {
		Properties defaults = new Properties();
		InputStream defaultIs = getClass().getResourceAsStream(PROPERTY_FILE_NAME);
		defaults.load(defaultIs);
		defaultIs.close();
		this.prop = new Properties(defaults);

		InputStream classpathIs = getClass().getResourceAsStream('/' + PROPERTY_FILE_NAME);
		if (classpathIs != null) {
			prop.load(classpathIs);
			classpathIs.close();
		}
	}

	/**
	 * 引数で指定されたプロパティファイルを用いて設定情報オブジェクトを生成します。
	 * 
	 * 指定されたプロパティファイルが存在しない場合、JAR アーカイブに格納されているデフォルトの
	 * プロパティファイルを使用します。
	 * 
	 * @param propFile
	 *            プロパティファイル
	 * @throws IOException
	 *             プロパティファイルの読み込みエラーが発生した場合
	 */
	public Settings(File propFile) throws IOException {
		this();
		load(propFile);
	}

	public void load(File propFile) throws IOException {
		if (propFile != null && propFile.isFile() && propFile.canRead()) {
			FileInputStream fis = new FileInputStream(propFile);
			prop.load(fis);
			fis.close();
		}
	}

	public String getString(String key) {
		return prop.getProperty(key);
	}

	public boolean getBoolean(String key) {
		return Boolean.valueOf(prop.getProperty(key));
	}

	public List<String> getStringList(String baseKey) {
		List<String> result = new ArrayList<String>();
		for (int i = 0; true; i++) {
			String value = prop.getProperty(baseKey + '.' + i);
			if (value == null) {
				return result;
			}
			result.add(value);
		}
	}

}
