package co.huitaca.j4log.plugins;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import co.huitaca.j4log.J4LogPlugin;
import co.huitaca.j4log.LogLevel;

public class LogBackPlugin extends J4LogPlugin{

	private static final String LOG4J_LOG_MANAGER = "org.apache.log4j.LogManager";
	private static final String LOG4J_LOG_MANAGER_GET_CURRENT_LOGGERS = "getCurrentLoggers";
	private static final String LOG4J_LOGGER_GET_NAME = "getName";
	private static final String LOG4J_LOGGER_GET_LEVEL = "getLevel";
	private static final String LOGBACK_LOGGER_CONTEXT = "ch.qos.logback.classic.LoggerContext";

	private static final Map<String, LogLevel> LOGBACK_LEVELS;

	static {

		LOGBACK_LEVELS = new HashMap<String, LogLevel>();
		LOGBACK_LEVELS.put("ALL", LogLevel.ALL);
		LOGBACK_LEVELS.put("DEBUG", LogLevel.DEBUG);
		LOGBACK_LEVELS.put("ERROR", LogLevel.ERROR);
		LOGBACK_LEVELS.put("FATAL", LogLevel.FATAL);
		LOGBACK_LEVELS.put("INFO", LogLevel.INFO);
		LOGBACK_LEVELS.put("OFF", LogLevel.OFF);
		LOGBACK_LEVELS.put("TRACE", LogLevel.TRACE);
		LOGBACK_LEVELS.put("WARN", LogLevel.WARN);
		LOGBACK_LEVELS.put("ALL", LogLevel.ALL);

	}

	private final Set<ClassLoader> log4jManagerClassLoaders = Collections
			.newSetFromMap(new WeakHashMap<ClassLoader, Boolean>());

	private final Set<ClassLoader> logbackContextClassLoaders = Collections
			.newSetFromMap(new WeakHashMap<ClassLoader, Boolean>());

	@Override
	public int countLoggers() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Map<String, LogLevel> getLoggers() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int countLoggersLike(String like) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Map<String, LogLevel> getLoggersLike(String like) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setLevel(String logger, LogLevel level) {
		// TODO Auto-generated method stub

	}

	@Override
	public LogLevel getLevel(String logger) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean contains(String logger) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String[] notifyOnClassLoading() {

		return new String[] { LOG4J_LOG_MANAGER, LOGBACK_LOGGER_CONTEXT };
	}

	@Override
	public void classLoaded(String className, ClassLoader classLoader) {

		if (LOG4J_LOG_MANAGER.equals(className)) {
			log4jManagerClassLoaders.add(classLoader);
		}

		if (LOGBACK_LOGGER_CONTEXT.equals(className)) {
			logbackContextClassLoaders.add(classLoader);
		}

	}

	private Map<String, LogLevel> getClassLoaderLoggers(ClassLoader classLoader)
			throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, ClassNotFoundException {

		Class<?> log4jManagerClass = Class.forName(LOG4J_LOG_MANAGER, false, classLoader);
		Map<String, LogLevel> loggers = new HashMap<String, LogLevel>();
		Enumeration<?> loggersEnum = (Enumeration<?>) log4jManagerClass
				.getMethod(LOG4J_LOG_MANAGER_GET_CURRENT_LOGGERS, (Class<?>[]) null).invoke(null, (Object[]) null);

		if (loggers != null) {
			while (loggersEnum.hasMoreElements()) {
				Object logger = loggersEnum.nextElement();
				String name = (String) logger.getClass().getMethod(LOG4J_LOGGER_GET_NAME, (Class<?>[]) null)
						.invoke(logger, (Object[]) null);
				Object levelObject = logger.getClass().getMethod(LOG4J_LOGGER_GET_LEVEL, (Class<?>[]) null)
						.invoke(logger, (Object[]) null);
				loggers.put(name, mapLevel(levelObject));
			}
		}

		return loggers;
	}

	private LogLevel mapLevel(Object log4jLevel) {

		return log4jLevel == null ? LogLevel.INDETERMINATE
				: LOGBACK_LEVELS.get(log4jLevel.toString().trim().toUpperCase());
	}
}
