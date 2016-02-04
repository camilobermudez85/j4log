package co.huitaca.j4log.plugins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import co.huitaca.j4log.J4LogPlugin;

public class PluginManager {

	private static List<J4LogPlugin> PLUGINS;

	static {
		PLUGINS = new ArrayList<>();
		PLUGINS.add(new Log4JPlugin());
		PLUGINS = Collections.unmodifiableList(PLUGINS);

	}

	public static List<J4LogPlugin> getPlugins() {
		return PLUGINS;
	}

}
