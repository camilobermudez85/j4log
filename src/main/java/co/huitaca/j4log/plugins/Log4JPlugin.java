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

import java.lang.reflect.InvocationTargetException;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import co.huitaca.j4log.J4LogPlugin;
import co.huitaca.j4log.LogLevel;

public class Log4JPlugin extends J4LogPlugin {

//	private static final Logger LOGGER = Logger.getLogger(Log4JPlugin.class
//			.getName());

	private static final String CONSOLE_APPENDER_LAYOUT_PATTERN = "";

	private static final String LOG4J_LOGGER = "org.apache.log4j.Logger";
	private static final String LOG4J_LOG_MANAGER = "org.apache.log4j.LogManager";
	private static final String LOG4J_LEVEL = "org.apache.log4j.Level";
	private static final String LOG4J_LOG_MANAGER_GET_CURRENT_LOGGERS = "getCurrentLoggers";
	private static final String LOG4J_LOG_MANAGER_GET_LOGGER = "getLogger";
	private static final String LOG4J_LOGGER_GET_NAME = "getName";
	private static final String LOG4J_LOGGER_GET_LEVEL = "getLevel";
	private static final String LOG4J_LOGGER_SET_LEVEL = "setLevel";
	private static final String LOG4J_LEVEL_TO_LEVEL = "toLevel";
	private static final String LOG4J_CONSOLE_APPENDER = "org.apache.log4j.ConsoleAppender";
	private static final String LOG4J_PATTERN_LAYOUT = "org.apache.log4j.PatternLayout";

	private static final Map<String, LogLevel> LOG4J_LEVELS_MAP;
	private static final Map<LogLevel, String> J4LOG_LEVELS_MAP;

	/*
	 * Transformations source code
	 */
	private static final String LOG4J_CONSOLE_APPENDER_INSTANCE_DEF = "private static "
			+ LOG4J_CONSOLE_APPENDER + " _consoleAppender;";
	private static final String LOG4J_CONSOLE_APPENDER_INSTANCE_INIT = "new "
			+ LOG4J_CONSOLE_APPENDER + " (new " + LOG4J_PATTERN_LAYOUT + " (\""
			+ CONSOLE_APPENDER_LAYOUT_PATTERN + "\"));";
	private static final String LOG4J_ADD_CONSOLE_APPENDER_SRC = "addAppender(_consoleAppender);";

	static {

		LOG4J_LEVELS_MAP = new HashMap<String, LogLevel>();
		LOG4J_LEVELS_MAP.put("ALL", LogLevel.ALL);
		LOG4J_LEVELS_MAP.put("TRACE", LogLevel.TRACE);
		LOG4J_LEVELS_MAP.put("DEBUG", LogLevel.DEBUG);
		LOG4J_LEVELS_MAP.put("INFO", LogLevel.INFO);
		LOG4J_LEVELS_MAP.put("WARN", LogLevel.WARN);
		LOG4J_LEVELS_MAP.put("ERROR", LogLevel.ERROR);
		LOG4J_LEVELS_MAP.put("FATAL", LogLevel.FATAL);
		LOG4J_LEVELS_MAP.put("OFF", LogLevel.OFF);

		J4LOG_LEVELS_MAP = new HashMap<LogLevel, String>();
		J4LOG_LEVELS_MAP.put(LogLevel.ALL, "ALL");
		J4LOG_LEVELS_MAP.put(LogLevel.TRACE, "TRACE");
		J4LOG_LEVELS_MAP.put(LogLevel.DEBUG, "DEBUG");
		J4LOG_LEVELS_MAP.put(LogLevel.INFO, "INFO");
		J4LOG_LEVELS_MAP.put(LogLevel.WARN, "WARN");
		J4LOG_LEVELS_MAP.put(LogLevel.ERROR, "ERROR");
		J4LOG_LEVELS_MAP.put(LogLevel.FATAL, "FATAL");
		J4LOG_LEVELS_MAP.put(LogLevel.OFF, "OFF");

	}

	private final Set<ClassLoader> log4jManagerClassLoaders = Collections
			.newSetFromMap(new WeakHashMap<ClassLoader, Boolean>());

	@Override
	public int countLoggers() {

		return getLoggers().size();
	}

	@Override
	public Map<String, LogLevel> getLoggers() {

		Map<String, LogLevel> allLoggers = new TreeMap<>();
		for (ClassLoader classLoader : log4jManagerClassLoaders) {
			allLoggers.putAll(getClassLoaderLoggers(classLoader));
		}
		allLoggers.putAll(getClassLoaderLoggers(getClass().getClassLoader()));

		return allLoggers;
	}

	@Override
	public int countLoggersLike(String like) {

		return filterLike(getLoggers(), like).size();
	}

	@Override
	public Map<String, LogLevel> getLoggersLike(String like) {

		return filterLike(getLoggers(), like);
	}

	@Override
	public void setLevel(String logger, LogLevel level) {

		setClassLoaderLoggerLevel(logger, level, getClass().getClassLoader());
		for (ClassLoader classLoader : log4jManagerClassLoaders) {
			setClassLoaderLoggerLevel(logger, level, classLoader);
		}
	}

	@Override
	public LogLevel getLevel(String logger) {

		LogLevel result = getClassLoaderLoggerLevel(logger, getClass()
				.getClassLoader());
		for (ClassLoader classLoader : log4jManagerClassLoaders) {
			LogLevel level = getClassLoaderLoggerLevel(logger, classLoader);
			if (level != null && result != null && !level.equals(result)) {
				// If more than one logger exists with that name and are set at
				// different levels better not to mislead the client
				return LogLevel.INDETERMINATE;
			}
			result = level == null ? result : level;
		}

		return result;
	}

	@Override
	public boolean contains(String logger) {

		return getLevel(logger) != null;
	}

	@Override
	public String[] getObservedClasses() {

		return new String[] { LOG4J_LOG_MANAGER, LOG4J_LOGGER };
	}

	@Override
	public byte[] onClassLoaded(String className, ClassLoader classLoader,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) {

		if (LOG4J_LOG_MANAGER.equals(className)) {
			System.out.println("Log4j LogManager detected in ClassLoader: "
					+ classLoader);
			log4jManagerClassLoaders.add(classLoader);
			return null;
		}

		if (LOG4J_LOGGER.equals(className)) {
			System.out.println("Transforming log4j Logger class in Classloader: "
							+ classLoader);
			return addConsoleAppenderTransformation(classLoader,
					classfileBuffer);
		}

		return null;
	}

	private byte[] addConsoleAppenderTransformation(ClassLoader classLoader,
			byte[] classfileBuffer) {

		ClassPool pool = ClassPool.getDefault();
		CtClass cl = null;

		try {

			// Add console appender field
			cl = pool.makeClass(new java.io.ByteArrayInputStream(
					classfileBuffer));
			CtField field = CtField.make(LOG4J_CONSOLE_APPENDER_INSTANCE_DEF,
					cl);
			cl.addField(field, LOG4J_CONSOLE_APPENDER_INSTANCE_INIT);

			// Add appender after every declared constructor
			CtConstructor[] constructors = cl.getDeclaredConstructors();
			for (CtConstructor constructor : constructors) {
				constructor.insertAfter(LOG4J_ADD_CONSOLE_APPENDER_SRC);
			}

			return cl.toBytecode();

		} catch (Exception e) {
			System.out.println("Error adding console appender transformation.");
		} finally {
			if (cl != null) {
				cl.detach();
			}
		}

		return null;

	}

	private Map<String, LogLevel> getClassLoaderLoggers(ClassLoader classLoader) {

		Map<String, LogLevel> loggers = new TreeMap<String, LogLevel>();
		try {

			Class<?> log4jManagerClass = Class.forName(LOG4J_LOG_MANAGER,
					false, classLoader);
			Enumeration<?> loggersEnum = (Enumeration<?>) log4jManagerClass
					.getMethod(LOG4J_LOG_MANAGER_GET_CURRENT_LOGGERS,
							(Class<?>[]) null).invoke(null, (Object[]) null);

			if (loggersEnum == null) {
				return loggers;
			}

			while (loggersEnum.hasMoreElements()) {
				Object logger = loggersEnum.nextElement();
				String name = (String) logger.getClass()
						.getMethod(LOG4J_LOGGER_GET_NAME, (Class<?>[]) null)
						.invoke(logger, (Object[]) null);
				Object levelInstance = logger.getClass()
						.getMethod(LOG4J_LOGGER_GET_LEVEL, (Class<?>[]) null)
						.invoke(logger, (Object[]) null);
				loggers.put(name, mapLevel(levelInstance));
			}

		} catch (Exception e) {
		}

		return loggers;
	}

	private LogLevel getClassLoaderLoggerLevel(String loggerName,
			ClassLoader classLoader) {

		try {

			Class<?> log4jManagerClass = Class.forName(LOG4J_LOG_MANAGER,
					false, classLoader);
			Object logger = log4jManagerClass.getMethod(
					LOG4J_LOG_MANAGER_GET_LOGGER, new Class[] { String.class })
					.invoke(null, new Object[] { loggerName });

			if (logger == null) {
				return null;
			}

			Object levelObject = logger.getClass()
					.getMethod(LOG4J_LOGGER_GET_LEVEL, (Class<?>[]) null)
					.invoke(logger, (Object[]) null);

			return mapLevel(levelObject);

		} catch (Exception e) {
			return null;
		}

	}

	private void setClassLoaderLoggerLevel(String loggerName,
			LogLevel j4logLevel, ClassLoader classLoader) {

		try {

			Class<?> log4jManagerClass = Class.forName(LOG4J_LOG_MANAGER,
					false, classLoader);
			Object logger = log4jManagerClass.getMethod(
					LOG4J_LOG_MANAGER_GET_LOGGER, new Class[] { String.class })
					.invoke(null, new Object[] { loggerName });

			if (logger == null) {
				return;
			}

			Class<?> log4jLevelClass = Class.forName(LOG4J_LEVEL, false,
					classLoader);
			Object log4jLevel = mapLevel(log4jLevelClass, j4logLevel);
			if (log4jLevel == null) {
				System.out.println("No mapping available for level "
						+ j4logLevel);
				return;
			}

			logger.getClass()
					.getMethod(LOG4J_LOGGER_SET_LEVEL,
							new Class<?>[] { log4jLevelClass })
					.invoke(logger, new Object[] { log4jLevel });

		} catch (Exception e) {
		}

	}

	private LogLevel mapLevel(Object log4jLevel) {

		return log4jLevel == null ? LogLevel.INDETERMINATE : LOG4J_LEVELS_MAP
				.get(log4jLevel.toString().trim().toUpperCase());
	}

	private Object mapLevel(Class<?> log4jLevelClass, LogLevel j4logLevel)
			throws IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {

		if (j4logLevel == null || LogLevel.INDETERMINATE.equals(j4logLevel)) {
			return null;
		}

		return log4jLevelClass.getMethod(LOG4J_LEVEL_TO_LEVEL,
				new Class<?>[] { String.class }).invoke(null,
				new Object[] { J4LOG_LEVELS_MAP.get(j4logLevel) });
	}
}