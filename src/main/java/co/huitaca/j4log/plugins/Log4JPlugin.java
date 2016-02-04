package co.huitaca.j4log.plugins;

import java.util.Map;

import co.huitaca.j4log.J4LogPlugin;
import co.huitaca.j4log.LogLevel;

public class Log4JPlugin extends J4LogPlugin {

	private static final String LOG4J_LOG_MANAGER = "org.apache.log4j.LogManager";
	private static final String LOGBACK_LOGGER_CONTEXT = "ch.qos.logback.classic.LoggerContext";

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

	}

}
