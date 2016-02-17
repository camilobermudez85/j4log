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
package co.huitaca.j4log.jmx;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import co.huitaca.j4log.J4LogPlugin;
import co.huitaca.j4log.LogLevel;
import co.huitaca.j4log.plugins.PluginManager;

public class J4Log implements J4LogMBean {

    public static final String OBJECT_NAME = "co.huitaca:type=J4Log";

    private static J4Log INSTANCE = new J4Log();

    public static J4Log getInstance() {
	return INSTANCE;
    }

    @Override
    public int countLoggers() {

	return getLoggers().size();
    }

    @Override
    public Map<String, String> getLoggers() {

	Map<String, String> allLoggers = new TreeMap<>();
	for (J4LogPlugin plugin : PluginManager.getPlugins()) {

	    for (Entry<String, String> loggerEntry : plugin.getLoggers().entrySet()) {

		String name = loggerEntry.getKey();
		String level = loggerEntry.getValue();
		if (allLoggers.containsKey(name) && !allLoggers.get(name).equals(level)) {
		    allLoggers.put(name, LogLevel.INDETERMINATE.name());
		} else if (!allLoggers.containsKey(name)) {
		    allLoggers.put(name, level);
		}
	    }
	}

	return allLoggers;
    }

    @Override
    public Map<String, String> getLoggers(int offset, int size) {

	return paginate(getLoggers(), offset, size);
    }

    @Override
    public int countLoggersLike(String like) {

	return getLoggersLike(like).size();
    }

    @Override
    public Map<String, String> getLoggersLike(String like) {

	Map<String, String> allLoggers = new TreeMap<>();
	for (J4LogPlugin plugin : PluginManager.getPlugins()) {

	    for (Entry<String, String> loggerEntry : plugin.getLoggersLike(like).entrySet()) {

		String name = loggerEntry.getKey();
		String level = loggerEntry.getValue();
		if (allLoggers.containsKey(name) && !allLoggers.get(name).equals(level)) {
		    allLoggers.put(name, LogLevel.INDETERMINATE.name());
		} else if (!allLoggers.containsKey(name)) {
		    allLoggers.put(name, level);
		}
	    }
	}

	return allLoggers;
    }

    @Override
    public Map<String, String> getLoggersLike(String like, int offset, int size) {

	return paginate(getLoggersLike(like), offset, size);
    }

    @Override
    public void setLevel(String logger, String level) {

	System.out.println("setLevel: " + level);
	for (J4LogPlugin plugin : PluginManager.getPlugins()) {
	    System.out.println("Plugin: " + plugin);
	    System.out.println("Subtree: " + plugin.getSubtree(logger));
	    for (Entry<String, String> e : plugin.getSubtree(logger).entrySet()) {
		System.out.println("logger: " + e.getKey());
		plugin.setLevel(e.getKey(), level);
	    }
	}
    }

    @Override
    public String getLevel(String logger) {

	String result = null;
	for (J4LogPlugin plugin : PluginManager.getPlugins()) {
	    if (plugin.contains(logger)) {
		String level = plugin.getLevel(logger);
		if (result != null && !result.equals(level)) {
		    return LogLevel.INDETERMINATE.name();
		}
		result = level;
	    }
	}

	return result;
    }

    private Map<String, String> paginate(Map<String, String> loggers, int offset, int size) {

	int i = 0;
	Map<String, String> allLoggers = getLoggers();
	Map<String, String> result = new TreeMap<>();
	for (Entry<String, String> loggerEntry : allLoggers.entrySet()) {
	    if (i - offset == size) {
		return result;
	    }
	    if (i >= offset) {
		result.put(loggerEntry.getKey(), loggerEntry.getValue());
	    }
	    i++;
	}
	return result;
    }

}
