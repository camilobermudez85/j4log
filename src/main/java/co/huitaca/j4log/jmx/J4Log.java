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
	public Map<String, LogLevel> getLoggers() {

		Map<String, LogLevel> allLoggers = new TreeMap<>();
		for (J4LogPlugin plugin : PluginManager.getPlugins()) {

			for (Entry<String, LogLevel> loggerEntry : plugin.getLoggers().entrySet()) {

				String name = loggerEntry.getKey();
				LogLevel level = loggerEntry.getValue();
				if (allLoggers.containsKey(name) && !allLoggers.get(name).equals(level)) {
					allLoggers.put(name, LogLevel.INDETERMINATE);
				} else if (!allLoggers.containsKey(name)) {
					allLoggers.put(name, level);
				}
			}
		}

		return allLoggers;
	}

	@Override
	public Map<String, LogLevel> getLoggers(int offset, int size) {

		return paginate(getLoggers(), offset, size);
	}

	@Override
	public int countLoggersLike(String like) {

		return getLoggersLike(like).size();
	}

	@Override
	public Map<String, LogLevel> getLoggersLike(String like) {

		Map<String, LogLevel> allLoggers = new TreeMap<>();
		for (J4LogPlugin plugin : PluginManager.getPlugins()) {

			for (Entry<String, LogLevel> loggerEntry : plugin.getLoggersLike(like).entrySet()) {

				String name = loggerEntry.getKey();
				LogLevel level = loggerEntry.getValue();
				if (allLoggers.containsKey(name) && !allLoggers.get(name).equals(level)) {
					allLoggers.put(name, LogLevel.INDETERMINATE);
				} else if (!allLoggers.containsKey(name)) {
					allLoggers.put(name, level);
				}
			}
		}

		return allLoggers;
	}

	@Override
	public Map<String, LogLevel> getLoggersLike(String like, int offset, int size) {

		return paginate(getLoggersLike(like), offset, size);
	}

	@Override
	public void setLevel(String logger, LogLevel level) {

		for (J4LogPlugin plugin : PluginManager.getPlugins()) {
			if (plugin.contains(logger)) {
				plugin.setLevel(logger, level);
			}
		}
	}

	@Override
	public LogLevel getLevel(String logger) {

		LogLevel result = null;
		for (J4LogPlugin plugin : PluginManager.getPlugins()) {
			if (plugin.contains(logger)) {
				LogLevel level = plugin.getLevel(logger);
				if (result != null && !result.equals(level)) {
					return LogLevel.INDETERMINATE;
				}
				result = level;
			}
		}

		return result;
	}

	private Map<String, LogLevel> paginate(Map<String, LogLevel> loggers, int offset, int size) {

		int i = 0;
		Map<String, LogLevel> allLoggers = getLoggers();
		Map<String, LogLevel> result = new TreeMap<>();
		for (Entry<String, LogLevel> loggerEntry : allLoggers.entrySet()) {
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
