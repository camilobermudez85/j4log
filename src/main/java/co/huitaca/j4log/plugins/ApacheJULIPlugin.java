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
package co.huitaca.j4log.plugins;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class ApacheJULIPlugin extends JULPlugin {

    private static final String LOG_MANAGER_CLASSLOADER_LOGGERS_FIELD = "classLoaderLoggers";
    private static final String APACHE_JULI_LOG_MANAGER_IMPL = "org.apache.juli.ClassLoaderLogManager";
    private static final String CLASSLOADER_LOG_INFO_LOGGERS = "loggers";

    @Override
    public Map<String, String> getLoggers() {

	Map<String, String> result = new TreeMap<String, String>();
	try {

	    Map<String, Logger> loggers = getLoggerInstances();
	    for (Entry<String, Logger> entry : loggers.entrySet()) {
		result.put(entry.getKey(), mapLevel(getEffectiveLoggerLevel(entry.getValue())));
	    }

	} catch (Exception e) {
	    e.printStackTrace();
	}

	return result;
    }

    @Override
    public void setLevel(String logger, String level) {

	try {

	    Map<String, Logger> loggers = getLoggerInstances();
	    for (Entry<String, Logger> entry : loggers.entrySet()) {
		if (entry.getKey().equals(logger)) {
		    entry.getValue().setLevel(mapLevel(level));
		    return;
		}
	    }

	} catch (Exception e) {
	    e.printStackTrace();
	}

    }

    @Override
    public String getLevel(String logger) {

	try {

	    Map<String, Logger> loggers = getLoggerInstances();
	    for (Entry<String, Logger> entry : loggers.entrySet()) {
		if (entry.getKey().equals(logger)) {
		    return mapLevel(getEffectiveLoggerLevel(entry.getValue()));
		}
	    }

	} catch (Exception e) {
	    e.printStackTrace();
	}

	return null;

    }

    private Map<String, Logger> getLoggerInstances() {

	Map<String, Logger> loggers = new HashMap<String, Logger>();
	try {

	    LogManager lm = LogManager.getLogManager();
	    Class<?> logManagerClass = lm.getClass();

	    // We're only interested in the Apache JULI implementation
	    if (!logManagerClass.getName().equals(APACHE_JULI_LOG_MANAGER_IMPL)) {
		return loggers;
	    }

	    Field f = lm.getClass().getDeclaredField(LOG_MANAGER_CLASSLOADER_LOGGERS_FIELD);
	    f.setAccessible(true);
	    Map<?, ?> classLoaderLoggers = (Map<?, ?>) f.get(lm);
	    if (classLoaderLoggers == null || classLoaderLoggers.size() == 0) {
		return loggers;
	    }

	    Field loggersField = null;
	    for (Entry<?, ?> entry : classLoaderLoggers.entrySet()) {

		// Entry values should be instances of
		// org.apache.juli.ClassLoaderLogManager$ClassLoaderLogInfo
		Object classLoaderLogInfo = entry.getValue();
		if (loggersField == null) {
		    loggersField = classLoaderLogInfo.getClass().getDeclaredField(CLASSLOADER_LOG_INFO_LOGGERS);
		    loggersField.setAccessible(true);
		}

		@SuppressWarnings("unchecked")
		Map<String, Logger> classLoaderloggers = (Map<String, Logger>) loggersField.get(classLoaderLogInfo);
		if (classLoaderLoggers != null) {
		    loggers.putAll(classLoaderloggers);
		}
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}

	return loggers;
    }

}
