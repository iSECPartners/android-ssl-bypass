package com.isecpartners.android.jdwp.pluginservice;

import java.io.File;
import java.util.Iterator;
import java.util.ServiceLoader;

import org.apache.log4j.Logger;

public class JDIPluginService extends AbstractPluginService {
	private final static org.apache.log4j.Logger LOGGER = Logger
			.getLogger(JDIPluginService.class.getName());
	private static JDIPluginService pluginService = null;
	private ServiceLoader<JDIPlugin> serviceLoader;
	//private Iterator<JDIPlugin> plugins;
	
	public static JDIPluginService getInstance(File dir) {
		/*not really working as singleton because not reloading when load plugins is called again ...
		 * really whole architecture should be reworked
		 * 
		 * if (JDIPluginService.pluginService == null) {
		 */
		JDIPluginService.pluginService = new JDIPluginService(dir);
		return JDIPluginService.pluginService;
	}

	private JDIPluginService(File dir) {
		super(dir);
		this.serviceLoader = ServiceLoader.load(JDIPlugin.class);
		LOGGER.debug("got serviceLoader: " + this.serviceLoader);
	}

	@Override
	public Iterator<JDIPlugin> getPlugins() {
		this.serviceLoader.reload();
		return this.serviceLoader.iterator();
	}
}