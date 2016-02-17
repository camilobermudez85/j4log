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
import java.lang.reflect.InvocationTargetException;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import co.huitaca.j4log.J4LogPlugin;
import co.huitaca.j4log.LogLevel;

public class JULPlugin extends J4LogPlugin {

    /*
     * String.format(format, date, source, logger, level, message, thrown);
     */
    private static final String JUL_SIMPLE_FORMATTER_FORMAT_PROPERTY = "java.util.logging.SimpleFormatter.format";
    private static final String JUL_SIMPLE_FORMATTER_FORMAT = "%1$tY%1$tm%1$td-%1$tH:%1$tM:%1$tS.%1$tL %4$-7s [%3$s]: %5$s %n";

    private static final String JUL_LOGGER = "java.util.logging.Logger";
    private static final String JUL_LOGGER_LEVEL_VALUE_FIELD = "levelValue";

    private static final String JUL_LOGGER_GET_LEVEL_METHOD_NAME;
    private static final String JUL_LOGGER_GET_LEVEL_METHOD_DESC;
    private static final String JUL_LOGGER_ADD_HANDLER_METHOD_NAME;
    private static final String JUL_LOGGER_ADD_HANDLER_METHOD_DESC;
    private static final String JUL_LOGGER_ADD_HANDLER_AFTER_SRC;
    private static final String JUL_LOGGER_CONSOLE_LOGGER_FLAG_NAME;
    private static final String JUL_LOGGER_CONSOLE_LOGGER_FLAG_DEF;
    private static final String JUL_LOGGER_CONSOLE_LOGGER_FLAG_INIT;
    private static final String JUL_LOGGER_INITIAL_STATE_NAME;
    private static final String JUL_LOGGER_INITIAL_STATE_DEF;
    private static final String JUL_LOGGER_INITIAL_STATE_INIT;
    private static final String JUL_LOGGER_GET_HANDLERS_METHOD_NAME;
    private static final String JUL_LOGGER_GET_HANDLERS_METHOD_DESC;
    private static final String JUL_LOGGER_GET_HANDLERS_BEFORE_SRC;

    protected static final Map<Level, LogLevel> JUL_LEVELS_MAP;
    protected static final Map<LogLevel, Level> J4LOG_LEVELS_MAP;
    protected static final Map<Integer, Level> JUL_LOG_LEVELS_INT_MAP;

    protected static final int JAVA_VERSION_MAJOR = Integer
	    .parseInt(System.getProperty("java.version").split("\\.")[1]);

    static {

	JUL_LEVELS_MAP = new HashMap<Level, LogLevel>();
	JUL_LEVELS_MAP.put(Level.ALL, LogLevel.ALL);
	JUL_LEVELS_MAP.put(Level.FINEST, LogLevel.TRACE);
	JUL_LEVELS_MAP.put(Level.FINER, LogLevel.DEBUG);
	JUL_LEVELS_MAP.put(Level.FINE, LogLevel.DEBUG);
	JUL_LEVELS_MAP.put(Level.CONFIG, LogLevel.INFO);
	JUL_LEVELS_MAP.put(Level.INFO, LogLevel.INFO);
	JUL_LEVELS_MAP.put(Level.WARNING, LogLevel.WARN);
	JUL_LEVELS_MAP.put(Level.SEVERE, LogLevel.ERROR);
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

	JUL_LOG_LEVELS_INT_MAP = new HashMap<Integer, Level>();
	JUL_LOG_LEVELS_INT_MAP.put(Level.ALL.intValue(), Level.ALL);
	JUL_LOG_LEVELS_INT_MAP.put(Level.FINEST.intValue(), Level.FINEST);
	JUL_LOG_LEVELS_INT_MAP.put(Level.FINER.intValue(), Level.FINER);
	JUL_LOG_LEVELS_INT_MAP.put(Level.FINE.intValue(), Level.FINE);
	JUL_LOG_LEVELS_INT_MAP.put(Level.CONFIG.intValue(), Level.CONFIG);
	JUL_LOG_LEVELS_INT_MAP.put(Level.INFO.intValue(), Level.INFO);
	JUL_LOG_LEVELS_INT_MAP.put(Level.WARNING.intValue(), Level.WARNING);
	JUL_LOG_LEVELS_INT_MAP.put(Level.SEVERE.intValue(), Level.SEVERE);
	JUL_LOG_LEVELS_INT_MAP.put(Level.OFF.intValue(), Level.OFF);

	JUL_LOGGER_GET_LEVEL_METHOD_NAME = "getLevel";
	JUL_LOGGER_GET_LEVEL_METHOD_DESC = "()Ljava/util/logging/Level;";
	JUL_LOGGER_ADD_HANDLER_METHOD_NAME = "addHandler";
	JUL_LOGGER_ADD_HANDLER_METHOD_DESC = "(Ljava/util/logging/Handler;)V";
	JUL_LOGGER_ADD_HANDLER_AFTER_SRC = "{if(!\"\".equals(name)) {removeHandler($1);setUseParentHandlers(true);}}";
	JUL_LOGGER_CONSOLE_LOGGER_FLAG_NAME = "consoleLogger";
	JUL_LOGGER_CONSOLE_LOGGER_FLAG_DEF = "private volatile boolean " + JUL_LOGGER_CONSOLE_LOGGER_FLAG_NAME + ";";
	JUL_LOGGER_CONSOLE_LOGGER_FLAG_INIT = "false;";
	JUL_LOGGER_INITIAL_STATE_NAME = "initialState";
	JUL_LOGGER_INITIAL_STATE_DEF = "private " + Map.class.getName() + " " + JUL_LOGGER_INITIAL_STATE_NAME + ";";
	JUL_LOGGER_INITIAL_STATE_INIT = "new " + TreeMap.class.getName() + "();";
	JUL_LOGGER_GET_HANDLERS_METHOD_NAME = "getHandlers";
	JUL_LOGGER_GET_HANDLERS_METHOD_DESC = "()[Ljava/util/logging/Handler;";
	JUL_LOGGER_GET_HANDLERS_BEFORE_SRC = 
			"{"
			+ "if(\"\".equals(name)) {"
        		+ "if(!this." + JUL_LOGGER_CONSOLE_LOGGER_FLAG_NAME + "){"
    				+ "this." + JUL_LOGGER_CONSOLE_LOGGER_FLAG_NAME + " = true;"
            			+ StreamHandler.class.getName() + " sh = new java.util.logging.StreamHandler"
            			+ "(java.lang.System.out, new " + SimpleFormatter.class.getName() + "());"
            			+ "sh.setLevel(" + Level.class.getName() + ".ALL);"
            			+ "handlers.clear();"
            			+ "handlers.add(sh);"
//            		+ "}"
        		+ "} else if(handlers.size() > 0){((" + Handler.class.getName() + ")handlers.get(0)).flush();}"
			+ "}"
			+ "};";

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

	return new String[] { JUL_LOGGER };
    }

    @Override
    public byte[] onClassLoaded(String className, ClassLoader classLoader, ProtectionDomain protectionDomain,
	    byte[] classfileBuffer) {

	if (JUL_LOGGER.equals(className)) {
	    return addConsoleAppenderTransformation(
		    classLoader == null ? ClassLoader.getSystemClassLoader() : classLoader, protectionDomain,
		    classfileBuffer);
	}

	return null;
    }

    protected byte[] addConsoleAppenderTransformation(ClassLoader classLoader, ProtectionDomain protectionDomain,
	    byte[] classfileBuffer) {

	ClassPool pool = ClassPool.getDefault();
	CtClass cl = null;
	try {

	    CtClass loggerCtClass = pool.getCtClass(JUL_LOGGER);
	    
	    // Transform addHandler
	    CtMethod methodAddHandler = loggerCtClass.getMethod(JUL_LOGGER_ADD_HANDLER_METHOD_NAME,
		    JUL_LOGGER_ADD_HANDLER_METHOD_DESC);
	    methodAddHandler.insertAfter(JUL_LOGGER_ADD_HANDLER_AFTER_SRC);

	    // Add initialized field
	    CtField initialStateField = CtField.make(JUL_LOGGER_INITIAL_STATE_DEF, loggerCtClass);
	    loggerCtClass.addField(initialStateField, JUL_LOGGER_INITIAL_STATE_INIT);
	    StringBuilder initialStateSrc = new StringBuilder();
	    // initalState should be sorted so that the logger hierarchies in it
	    // get traversed from the top down, parents before children,
	    // otherwise children levels would get overwritten by their
	    // ancestors levels
	    for (Entry<String, LogLevel> e : new TreeMap<>(initialState).entrySet()) {
		initialStateSrc.append("this.").append(JUL_LOGGER_INITIAL_STATE_NAME)
			.append(".put(\"")
			.append(e.getKey())
			.append("\"," + Level.class.getName() + ".")
			.append(mapLevel(e.getValue().toString()))
			.append(");");
	    }
	    // Add after every constructor
	    for(CtConstructor ctor : loggerCtClass.getConstructors()) {
		ctor.insertAfter(initialStateSrc.toString());
	    }
	    
	    // Transform getLevel()
	    CtMethod methodGetLevel = loggerCtClass.getMethod(JUL_LOGGER_GET_LEVEL_METHOD_NAME,
			    JUL_LOGGER_GET_LEVEL_METHOD_DESC);
	    StringBuilder beforeGetLevelSrc = new StringBuilder
	    		("{ ")
	    .append("for(")
	    .append(Iterator.class.getName())
	    .append(" iter = ")
	    .append("this.").append(JUL_LOGGER_INITIAL_STATE_NAME).append(".entrySet().iterator(); iter.hasNext();) {")
	    .append(Entry.class.getName())
	    .append(" entry = iter.next();")
	    .append("")
	    .append(" if(name.startsWith((java.lang.String)entry.getKey())) { ")
	    .append("setLevel(")
	    .append("(").append(Level.class.getName()).append(")")
	    .append("entry.getValue());")
	    .append("iter.remove();")
	    .append("}}};");
	    methodGetLevel.insertBefore(beforeGetLevelSrc.toString());				
	    
	    // Add consoleLogger field
	    CtField consoleLoggerField = CtField.make(JUL_LOGGER_CONSOLE_LOGGER_FLAG_DEF, loggerCtClass);
	    loggerCtClass.addField(consoleLoggerField, JUL_LOGGER_CONSOLE_LOGGER_FLAG_INIT);
	    
	    // Transform getHandlers
	    System.setProperty(JUL_SIMPLE_FORMATTER_FORMAT_PROPERTY, JUL_SIMPLE_FORMATTER_FORMAT);
	    CtMethod methodGetHandlers = loggerCtClass.getMethod(JUL_LOGGER_GET_HANDLERS_METHOD_NAME,
		    JUL_LOGGER_GET_HANDLERS_METHOD_DESC);
	    methodGetHandlers.insertBefore(JUL_LOGGER_GET_HANDLERS_BEFORE_SRC);

	    return loggerCtClass.toBytecode();

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

    /**
     * Finds a logger effective level, i.e. if the logger level is
     * <code>null</code> it will introspect the field containing the real level
     * (<code>levelValue</code>).
     * 
     * @param logger
     * @return
     */
    protected Level getEffectiveLoggerLevel(Logger logger) {

	try {

	    if (logger.getLevel() != null) {
		return logger.getLevel();
	    }

	    Field LevelValueField = Logger.class.getDeclaredField(JUL_LOGGER_LEVEL_VALUE_FIELD);
	    LevelValueField.setAccessible(true);
	    Integer levelValue = (Integer) LevelValueField.get(logger);
	    return JUL_LOG_LEVELS_INT_MAP.get(levelValue);

	} catch (Exception e) {
	    e.printStackTrace();
	}

	return null;
    }

}
