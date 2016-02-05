/*
 * Copyright 2016 Camilo Bermúdez
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package co.huitaca.j4log;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public abstract class J4LogPlugin {

	public abstract int countLoggers();

	public abstract Map<String, LogLevel> getLoggers();

	public abstract int countLoggersLike(String like);

	public abstract Map<String, LogLevel> getLoggersLike(String like);

	public abstract void setLevel(String logger, LogLevel level);

	public abstract LogLevel getLevel(String logger);

	public abstract boolean contains(String logger);

	/**
	 * *** ONLY USED IN AGENT MODE ***
	 * 
	 * The implementation should return the names of the classes it's interested
	 * in. When any of the classes returned here gets loaded the plug-in
	 * implementation will get notified on the method
	 * {@link #classLoaded(String, ClassLoader)}.
	 * 
	 * @return a list of classes fully qualified names, e.g.
	 *         {"java.lang.List","java.lang.String"}
	 */
	public abstract String[] notifyOnClassLoading();

	/**
	 * *** ONLY USED IN AGENT MODE ***
	 * 
	 * Through this method, the implementation will get notified about the
	 * loading events of any of the classes returned on
	 * {@link #notifyOnClassLoading()}.
	 * 
	 * @param className
	 * @param classLoader
	 */
	public abstract void classLoaded(String className, ClassLoader classLoader);

	protected Map<String, LogLevel> filterLike(Map<String, LogLevel> input, String like) {

		if (like == null || "".equals(like.trim())) {
			return input;
		}

		Map<String, LogLevel> filtered = new TreeMap<>();
		for (Entry<String, LogLevel> entry : input.entrySet()) {
			if (entry.getKey().toLowerCase().contains(like.toLowerCase())) {
				filtered.put(entry.getKey(), entry.getValue());
			}
		}

		return filtered;
	}
}
