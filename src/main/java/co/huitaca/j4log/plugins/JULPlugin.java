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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import co.huitaca.j4log.J4LogPlugin;
import co.huitaca.j4log.LogLevel;

public class JULPlugin extends J4LogPlugin {

    // private static final Logger LOGGER = Logger.getLogger(JULPlugin.class
    // .getName());

    private static final String JUL_LOGGER = "java.util.logging.Logger";
    private static final String JUL_LOG_MANAGER = "java.util.logging.LogManager";
    // private static final String JUL_LEVEL = "java.util.logging.Level";
    // private static final String JUL_LOG_MANAGER_GET_LOGGERS_NAMES =
    // "getLoggerNames";
    // private static final String JUL_LOG_MANAGER_GET_LOGGER = "getLogger";
    // private static final String JUL_LOG_MANAGER_GET_LOG_MANAGER =
    // "getLogManager";
    // private static final String JUL_LOGGER_GET_LEVEL = "getLevel";
    // private static final String JUL_LOGGER_SET_LEVEL = "setLevel";
    // private static final String JUL_LEVEL_PARSE = "parse";
    private static final String JUL_STREAM_HANDLER = "java.util.logging.StreamHandler";
    private static final String JUL_FORMATTER = "java.util.logging.Formatter";
    private static final String JUL_CUSTOM_FORMATTER = "_J4LogJULCustomFormatter";
    private static final String JUL_CUSTOM_FORMATTER_FORMAT_IMPL;

    protected static final Map<Level, LogLevel> JUL_LEVELS_MAP;
    protected static final Map<LogLevel, Level> J4LOG_LEVELS_MAP;

    /*
     * Transformations source code
     */
    protected static final String JUL_CONSOLE_HANDLER_INSTANCE_DEF;
    protected static final String JUL_CONSOLE_HANDLER_INSTANCE_INIT;
    protected static final String JUL_ADD_CONSOLE_HANDLER_SRC;

    static {

	JUL_LEVELS_MAP = new HashMap<Level, LogLevel>();
	JUL_LEVELS_MAP.put(Level.ALL, LogLevel.ALL);
	JUL_LEVELS_MAP.put(Level.FINEST, LogLevel.TRACE);
	JUL_LEVELS_MAP.put(Level.FINER, LogLevel.DEBUG);
	JUL_LEVELS_MAP.put(Level.INFO, LogLevel.INFO);
	JUL_LEVELS_MAP.put(Level.WARNING, LogLevel.WARN);
	JUL_LEVELS_MAP.put(Level.SEVERE, LogLevel.ERROR);
	JUL_LEVELS_MAP.put(Level.SEVERE, LogLevel.FATAL);
	JUL_LEVELS_MAP.put(Level.OFF, LogLevel.OFF);

	J4LOG_LEVELS_MAP = new HashMap<LogLevel, Level>();
	J4LOG_LEVELS_MAP.put(LogLevel.ALL, Level.ALL);
	J4LOG_LEVELS_MAP.put(LogLevel.TRACE, Level.FINEST);
	J4LOG_LEVELS_MAP.put(LogLevel.DEBUG, Level.FINER);
	J4LOG_LEVELS_MAP.put(LogLevel.INFO, Level.INFO);
	J4LOG_LEVELS_MAP.put(LogLevel.WARN, Level.WARNING);
	J4LOG_LEVELS_MAP.put(LogLevel.ERROR, Level.SEVERE);
	J4LOG_LEVELS_MAP.put(LogLevel.FATAL, Level.SEVERE);
	J4LOG_LEVELS_MAP.put(LogLevel.OFF, Level.OFF);

	JUL_CONSOLE_HANDLER_INSTANCE_DEF = "private static " + JUL_STREAM_HANDLER + " _consoleHandler;";
	JUL_ADD_CONSOLE_HANDLER_SRC = "addHandler(_consoleHandler);";
	JUL_CUSTOM_FORMATTER_FORMAT_IMPL = "public String format(java.util.logging.LogRecord record){"
		+ "return record.getLevel() + \" [\" + record.getMillis() + \"] \" + record.getMessage();" + "}";
	JUL_CONSOLE_HANDLER_INSTANCE_INIT = "new " + JUL_STREAM_HANDLER + "(java.lang.System.out, new "
		+ JUL_CUSTOM_FORMATTER + "());";

    }

    @Override
    public int countLoggers() {
	new java.util.logging.StreamHandler(java.lang.System.out, new java.util.logging.Formatter() {
	    public String format(java.util.logging.LogRecord record) {
		return record.getLevel() + " [" + record.getMillis() + "] " + record.getMessage();
	    }
	});
	return getLoggers().size();
    }

    @Override
    public Map<String, String> getLoggers() {

	Map<String, String> loggers = new TreeMap<String, String>();
	try {

	    LogManager lm = LogManager.getLogManager();
	    Enumeration<?> loggersEnum = (Enumeration<?>) lm.getLoggerNames();

	    if (loggersEnum == null) {
		return loggers;
	    }

	    while (loggersEnum.hasMoreElements()) {

		String loggerName = loggersEnum.nextElement().toString();
		Logger logger = lm.getLogger(loggerName);
		Level levelInstance = logger.getLevel();
		loggers.put(loggerName, mapLevel(levelInstance));
	    }

	} catch (Exception e) {
	    e.printStackTrace();
	}

	return loggers;
    }

    @Override
    public int countLoggersLike(String like) {

	return filterLike(getLoggers(), like).size();
    }

    @Override
    public Map<String, String> getLoggersLike(String like) {

	return filterLike(getLoggers(), like);
    }

    @Override
    public void setLevel(String logger, String level) {

	try {
	    Level levelInstance = mapLevel(level);
	    if (levelInstance == null) {
		return;
	    }
	    Logger loggerInstance = LogManager.getLogManager().getLogger(logger);
	    if (loggerInstance != null) {
		loggerInstance.setLevel(levelInstance);
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}

    }

    @Override
    public String getLevel(String logger) {

	try {
	    Logger loggerInstance = LogManager.getLogManager().getLogger(logger);
	    if (loggerInstance != null) {
		Level level = loggerInstance.getLevel();
		if (level != null) {
		    return mapLevel(level);
		}
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}

	return null;
    }

    @Override
    public boolean contains(String logger) {

	return getLevel(logger) != null;
    }

    @Override
    public String[] getObservedClasses() {

	return new String[] { JUL_LOG_MANAGER, JUL_LOGGER };
    }

    @Override
    public byte[] onClassLoaded(String className, ClassLoader classLoader, ProtectionDomain protectionDomain,
	    byte[] classfileBuffer) {

	if (JUL_LOG_MANAGER.equals(className)) {
	    // julManagerClassLoaders.add(classLoader == null ? ClassLoader
	    // .getSystemClassLoader() : classLoader);
	    // return null;
	}

	if (JUL_LOGGER.equals(className)) {
	    System.out.println("Transforming JUL Logger class in Classloader: " + classLoader);
	    // return addConsoleAppenderTransformation(
	    // classLoader == null ? ClassLoader.getSystemClassLoader() :
	    // classLoader, protectionDomain,
	    // classfileBuffer);
	}

	return null;
    }

    protected byte[] addConsoleAppenderTransformation(ClassLoader classLoader, ProtectionDomain protectionDomain,
	    byte[] classfileBuffer) {

	ClassPool pool = ClassPool.getDefault();
	CtClass cl = null;

	try {

	    // Create custom formatter class
	    CtClass formatterCtClass = pool.getCtClass(JUL_FORMATTER);
	    CtClass customFormatterCtClass = pool.makeClass(JUL_CUSTOM_FORMATTER, formatterCtClass);
	    customFormatterCtClass
		    .addMethod(CtNewMethod.make(JUL_CUSTOM_FORMATTER_FORMAT_IMPL, customFormatterCtClass));
	    customFormatterCtClass.addConstructor(CtNewConstructor.defaultConstructor(customFormatterCtClass));
	    if (classLoader == null) {
		customFormatterCtClass.toClass(ClassLoader.getSystemClassLoader(), protectionDomain);
	    } else {
		customFormatterCtClass.toClass(classLoader, protectionDomain);
	    }

	    // Add console handler field
	    cl = pool.makeClass(new java.io.ByteArrayInputStream(classfileBuffer));
	    CtField field = CtField.make(JUL_CONSOLE_HANDLER_INSTANCE_DEF, cl);
	    cl.addField(field, JUL_CONSOLE_HANDLER_INSTANCE_INIT);

	    // Add handler after every declared constructor
	    CtConstructor[] constructors = cl.getDeclaredConstructors();
	    for (CtConstructor constructor : constructors) {
		constructor.insertAfter(JUL_ADD_CONSOLE_HANDLER_SRC);
	    }

	    // return cl.toBytecode();
	    return classfileBuffer;

	} catch (Exception e) {
	    System.out.println("Error adding console handler transformation.");
	    e.printStackTrace();
	} finally {
	    if (cl != null) {
		cl.detach();
	    }
	}

	return null;

    }

    protected String mapLevel(Level julLevel) {

	if (julLevel == null) {
	    return LogLevel.INDETERMINATE.name();
	} else {
	    LogLevel level = JUL_LEVELS_MAP.get(julLevel);
	    return level == null ? null : level.name();
	}
    }

    protected Level mapLevel(String j4logLevel) throws IllegalAccessException, IllegalArgumentException,
	    InvocationTargetException, NoSuchMethodException, SecurityException {

	return J4LOG_LEVELS_MAP.get(LogLevel.valueOf(j4logLevel));
    }

    protected Level getEffectiveLoggerLevel(Logger logger) {

	if (logger == null) {
	    return null;
	}

	Level level = logger.getLevel();
	if (level != null) {
	    return level;
	}

	return getEffectiveLoggerLevel(logger.getParent());
    }
}
