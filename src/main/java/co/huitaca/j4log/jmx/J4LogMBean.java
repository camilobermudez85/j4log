package co.huitaca.j4log.jmx;

import java.util.Map;

import co.huitaca.j4log.LogLevel;

public interface J4LogMBean {

	public int countLoggers();

	public Map<String, LogLevel> getLoggers();

	public Map<String, LogLevel> getLoggers(int offset, int size);

	public int countLoggersLike(String like);

	public Map<String, LogLevel> getLoggersLike(String like);

	public Map<String, LogLevel> getLoggersLike(String like, int offset, int page);

	public void setLevel(String logger, LogLevel level);

	public LogLevel getLevel(String logger);

}
