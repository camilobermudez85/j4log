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

public class Log4JPlugin extends J4LogPlugin {

	private static final String LOG4J_LOG_MANAGER = "org.apache.log4j.LogManager";
	private static final String LOG4J_LOG_MANAGER_GET_CURRENT_LOGGERS = "getCurrentLoggers";
	private static final String LOG4J_LOGGER_GET_NAME = "getName";
	private static final String LOG4J_LOGGER_GET_LEVEL = "getLevel";
	private static final String LOGBACK_LOGGER_CONTEXT = "ch.qos.logback.classic.LoggerContext";

	private static final Map<String, LogLevel> LOG4J_LEVELS;

	static {

		LOG4J_LEVELS = new HashMap<String, LogLevel>();
		LOG4J_LEVELS.put("ALL", LogLevel.ALL);
		LOG4J_LEVELS.put("DEBUG", LogLevel.DEBUG);
		LOG4J_LEVELS.put("ERROR", LogLevel.ERROR);
		LOG4J_LEVELS.put("FATAL", LogLevel.FATAL);
		LOG4J_LEVELS.put("INFO", LogLevel.INFO);
		LOG4J_LEVELS.put("OFF", LogLevel.OFF);
		LOG4J_LEVELS.put("TRACE", LogLevel.TRACE);
		LOG4J_LEVELS.put("WARN", LogLevel.WARN);
		LOG4J_LEVELS.put("ALL", LogLevel.ALL);

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

	private Class<?> findClass(String name, ClassLoader classLoader) {

		try {
			return Class.forName(name, false, classLoader);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	private Map<String, LogLevel> getLog4JLoggers(Class<?> log4jManager)
			throws NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException,
			InvocationTargetException {

		Map<String, LogLevel> loggers = new HashMap<String, LogLevel>();
		Enumeration<?> loggersEnum = (Enumeration<?>) log4jManager.getMethod(
				LOG4J_LOG_MANAGER_GET_CURRENT_LOGGERS, (Class<?>[]) null)
				.invoke(null, (Object[]) null);

		if (loggers != null) {
			while (loggersEnum.hasMoreElements()) {
				Object logger = loggersEnum.nextElement();
				String name = (String) logger.getClass()
						.getMethod(LOG4J_LOGGER_GET_NAME, (Class<?>[]) null)
						.invoke(logger, (Object[]) null);
				Enum<?> level = (Enum<?>) logger.getClass()
						.getMethod(LOG4J_LOGGER_GET_LEVEL, (Class<?>[]) null)
						.invoke(logger, (Object[]) null);

			}
		}

		return null;
	}

	private LogLevel mapLevel(String log4jLevel) {

		return LOG4J_LEVELS.get(log4jLevel.trim().toUpperCase());
	}
}