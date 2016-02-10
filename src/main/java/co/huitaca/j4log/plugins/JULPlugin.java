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
import java.util.logging.LogManager;

import co.huitaca.j4log.J4LogPlugin;
import co.huitaca.j4log.LogLevel;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;

public class JULPlugin extends J4LogPlugin {

    // private static final Logger LOGGER = Logger.getLogger(JULPlugin.class
    // .getName());

    private static final String JUL_LOGGER = "java.util.logging.Logger";
    private static final String JUL_LOG_MANAGER = "java.util.logging.LogManager";
    private static final String JUL_LEVEL = "java.util.logging.Level";
    private static final String JUL_LOG_MANAGER_GET_LOGGERS_NAMES = "getLoggerNames";
    private static final String JUL_LOG_MANAGER_GET_LOGGER = "getLogger";
    private static final String JUL_LOG_MANAGER_GET_LOG_MANAGER = "getLogManager";
    private static final String JUL_LOGGER_GET_LEVEL = "getLevel";
    private static final String JUL_LOGGER_SET_LEVEL = "setLevel";
    private static final String JUL_LEVEL_PARSE = "parse";
    private static final String JUL_STREAM_HANDLER = "java.util.logging.StreamHandler";
    private static final String JUL_FORMATTER = "java.util.logging.Formatter";
    private static final String JUL_CUSTOM_FORMATTER = "_J4LogJULCustomFormatter";
    private static final String JUL_CUSTOM_FORMATTER_FORMAT_IMPL;

    private static final Map<String, LogLevel> JUL_LEVELS_MAP;
    private static final Map<LogLevel, String> J4LOG_LEVELS_MAP;

    /*
     * Transformations source code
     */
    private static final String JUL_CONSOLE_HANDLER_INSTANCE_DEF;
    private static final String JUL_CONSOLE_HANDLER_INSTANCE_INIT;
    private static final String JUL_ADD_CONSOLE_HANDLER_SRC;

    static {

	JUL_LEVELS_MAP = new HashMap<String, LogLevel>();
	JUL_LEVELS_MAP.put("ALL", LogLevel.ALL);
	JUL_LEVELS_MAP.put("FINEST", LogLevel.TRACE);
	JUL_LEVELS_MAP.put("FINER", LogLevel.DEBUG);
	JUL_LEVELS_MAP.put("INFO", LogLevel.INFO);
	JUL_LEVELS_MAP.put("WARNING", LogLevel.WARN);
	JUL_LEVELS_MAP.put("SEVERE", LogLevel.ERROR);
	JUL_LEVELS_MAP.put("SEVERE", LogLevel.FATAL);
	JUL_LEVELS_MAP.put("OFF", LogLevel.OFF);

	J4LOG_LEVELS_MAP = new HashMap<LogLevel, String>();
	J4LOG_LEVELS_MAP.put(LogLevel.ALL, "ALL");
	J4LOG_LEVELS_MAP.put(LogLevel.TRACE, "FINEST");
	J4LOG_LEVELS_MAP.put(LogLevel.DEBUG, "FINER");
	J4LOG_LEVELS_MAP.put(LogLevel.INFO, "INFO");
	J4LOG_LEVELS_MAP.put(LogLevel.WARN, "WARNING");
	J4LOG_LEVELS_MAP.put(LogLevel.ERROR, "SEVERE");
	J4LOG_LEVELS_MAP.put(LogLevel.FATAL, "SEVERE");
	J4LOG_LEVELS_MAP.put(LogLevel.OFF, "OFF");
	
	JUL_CONSOLE_HANDLER_INSTANCE_DEF = "private static " + JUL_STREAM_HANDLER + " _consoleHandler;";
	JUL_ADD_CONSOLE_HANDLER_SRC = "addHandler(_consoleHandler);";
	JUL_CUSTOM_FORMATTER_FORMAT_IMPL = 
		"public String format(java.util.logging.LogRecord record){"
			+ "return record.getLevel() + \" [\" + record.getMillis() + \"] \" + record.getMessage();"
		+ "}";
	JUL_CONSOLE_HANDLER_INSTANCE_INIT = 
			"new " + JUL_STREAM_HANDLER + "(java.lang.System.out, new " + JUL_CUSTOM_FORMATTER + "());";

    }

    private final Set<ClassLoader> julManagerClassLoaders = Collections
	    .newSetFromMap(new WeakHashMap<ClassLoader, Boolean>());

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

	Map<String, String> allLoggers = new TreeMap<>();
	for (ClassLoader classLoader : julManagerClassLoaders) {
	    allLoggers.putAll(getClassLoaderLoggers(classLoader));
	}

	return allLoggers;
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

	for (ClassLoader classLoader : julManagerClassLoaders) {
	    setClassLoaderLoggerLevel(logger, level, classLoader);
	}
    }

    @Override
    public String getLevel(String logger) {

	String result = null;
	for (ClassLoader classLoader : julManagerClassLoaders) {
	    String level = getClassLoaderLoggerLevel(logger, classLoader);
	    if (level != null && result != null && !level.equals(result)) {
		// If more than one logger exists with that name and are set at
		// different levels better not to mislead the client
		return LogLevel.INDETERMINATE.name();
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

	return new String[] { JUL_LOG_MANAGER, JUL_LOGGER };
    }

    @Override
    public byte[] onClassLoaded(String className, ClassLoader classLoader, ProtectionDomain protectionDomain,
	    byte[] classfileBuffer) {

	if (JUL_LOG_MANAGER.equals(className)) {
	    System.out.println("JUL LogManager detected in ClassLoader: " + classLoader);
	    julManagerClassLoaders.add(classLoader == null ? ClassLoader.getSystemClassLoader() : classLoader);
	    return null;
	}

	if (JUL_LOGGER.equals(className)) {
	    System.out.println("Transforming JUL Logger class in Classloader: " + classLoader);
	    return addConsoleAppenderTransformation(
		    classLoader == null ? ClassLoader.getSystemClassLoader() : classLoader, protectionDomain,
		    classfileBuffer);
	}

	return null;
    }

    private byte[] addConsoleAppenderTransformation(ClassLoader classLoader, ProtectionDomain protectionDomain, byte[] classfileBuffer) {

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
//		System.out.println("Bootstrap: " + ClassLoader.getSystemClassLoader().getParent());
//		System.out.println("System: " + ClassLoader.getSystemClassLoader());
//		System.out.println(Class.forName(JUL_CUSTOM_FORMATTER, false, ClassLoader.getSystemClassLoader()));
//		System.out.println("Formatter instance: " + o);
//		System.out.println(Class.forName(JUL_CUSTOM_FORMATTER, false, ClassLoader.getSystemClassLoader().getParent()));
	    } else {
		customFormatterCtClass.toClass(classLoader, protectionDomain);
	    }
	    
//	    System.out.println("************ Loaded class: " + classLoader.loadClass(JUL_CUSTOM_FORMATTER));
	    
//	    System.out.println("addConsoleAppenderTransformation: " + c);

	    // Add console handler field
	    cl = pool.makeClass(new java.io.ByteArrayInputStream(classfileBuffer));
	    CtField field = CtField.make(JUL_CONSOLE_HANDLER_INSTANCE_DEF, cl);
	    cl.addField(field, JUL_CONSOLE_HANDLER_INSTANCE_INIT);

	    // Add handler after every declared constructor
	    CtConstructor[] constructors = cl.getDeclaredConstructors();
	    for (CtConstructor constructor : constructors) {
		constructor.insertAfter(JUL_ADD_CONSOLE_HANDLER_SRC);
	    }

//	    return cl.toBytecode();
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

    private Map<String, String> getClassLoaderLoggers(ClassLoader classLoader) {

	Map<String, String> loggers = new TreeMap<String, String>();
	try {

	    Class<?> julManagerClass = Class.forName(JUL_LOG_MANAGER, false, classLoader);
	    Object logManagerInstance = julManagerClass.getMethod(JUL_LOG_MANAGER_GET_LOG_MANAGER, (Class<?>[]) null)
		    .invoke(null, (Object[]) null);
	    Enumeration<?> loggersEnum = (Enumeration<?>) julManagerClass
		    .getMethod(JUL_LOG_MANAGER_GET_LOGGERS_NAMES, (Class<?>[]) null)
		    .invoke(logManagerInstance, (Object[]) null);

	    if (loggersEnum == null) {
		return loggers;
	    }

	    while (loggersEnum.hasMoreElements()) {

		System.out.println(loggersEnum.nextElement().toString());
		String loggerName = loggersEnum.nextElement().toString();
		Object logger = julManagerClass.getMethod(JUL_LOG_MANAGER_GET_LOGGER, new Class[] { String.class })
			.invoke(logManagerInstance, new Object[] { loggerName });
		Object levelInstance = logger.getClass().getMethod(JUL_LOGGER_GET_LEVEL, (Class<?>[]) null)
			.invoke(logger, (Object[]) null);
		loggers.put(loggerName, mapLevel(levelInstance));
	    }

	} catch (Exception e) {
	    e.printStackTrace();
	}

	return loggers;
    }

    private String getClassLoaderLoggerLevel(String loggerName, ClassLoader classLoader) {

	try {

	    Class<?> julManagerClass = Class.forName(JUL_LOG_MANAGER, false, classLoader);
	    Object logManagerInstance = julManagerClass.getMethod(JUL_LOG_MANAGER_GET_LOG_MANAGER, (Class<?>[]) null)
		    .invoke(null, (Object[]) null);
	    Object logger = julManagerClass.getMethod(JUL_LOG_MANAGER_GET_LOGGER, new Class[] { String.class })
		    .invoke(logManagerInstance, new Object[] { loggerName });

	    if (logger == null) {
		return null;
	    }

	    Object levelObject = logger.getClass().getMethod(JUL_LOGGER_GET_LEVEL, (Class<?>[]) null).invoke(logger,
		    (Object[]) null);

	    return mapLevel(levelObject);

	} catch (Exception e) {
	    return null;
	}

    }

    private void setClassLoaderLoggerLevel(String loggerName, String j4logLevel, ClassLoader classLoader) {

	try {

	    Class<?> julManagerClass = Class.forName(JUL_LOG_MANAGER, false, classLoader);
	    Object logManagerInstance = julManagerClass.getMethod(JUL_LOG_MANAGER_GET_LOG_MANAGER, (Class<?>[]) null)
		    .invoke(null, (Object[]) null);
	    Object logger = julManagerClass.getMethod(JUL_LOG_MANAGER_GET_LOGGER, new Class[] { String.class })
		    .invoke(logManagerInstance, new Object[] { loggerName });

	    if (logger == null) {
		System.out.println("No logger found: " + loggerName);
		return;
	    }

	    Class<?> julLevelClass = Class.forName(JUL_LEVEL, false, classLoader);
	    Object julLevel = mapLevel(julLevelClass, j4logLevel);
	    if (julLevel == null) {
		System.out.println("No mapping available for level " + j4logLevel);
		return;
	    }

	    logger.getClass().getMethod(JUL_LOGGER_SET_LEVEL, new Class<?>[] { julLevelClass }).invoke(logger,
		    new Object[] { julLevel });

	} catch (Exception e) {
	}

    }

    private String mapLevel(Object julLevel) {

	if (julLevel == null) {
	    return LogLevel.INDETERMINATE.name();
	} else {
	    LogLevel level = JUL_LEVELS_MAP.get(julLevel.toString().trim().toUpperCase());
	    return level == null ? null : level.name();
	}
    }

    private Object mapLevel(Class<?> julLevelClass, String j4logLevel) throws IllegalAccessException,
	    IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {

	if (j4logLevel == null || LogLevel.INDETERMINATE.name().equals(j4logLevel)) {
	    return null;
	}

	return julLevelClass.getMethod(JUL_LEVEL_PARSE, new Class<?>[] { String.class }).invoke(null,
		new Object[] { J4LOG_LEVELS_MAP.get(LogLevel.valueOf(j4logLevel)) });
    }
}
