package com.isecpartners.android.jdwp.pluginservice;

import java.io.File;
import java.util.Iterator;
import java.util.ServiceLoader;

import org.apache.log4j.Logger;

public class JDIPluginService extends AbstractPluginService {
	private static JDIPluginService pluginService;
	private ServiceLoader<JDIPlugin> serviceLoader;
	
	public static JDIPluginService getInstance(File dir) {
		if (JDIPluginService.pluginService == null) {
			JDIPluginService.pluginService = new JDIPluginService(dir);
		}
		return JDIPluginService.pluginService;
	}

	private JDIPluginService(File dir) {
		super(dir);
		this.serviceLoader = ServiceLoader.load(JDIPlugin.class);
	}

	@Override
	public Iterator<JDIPlugin> getPlugins() {
		return this.serviceLoader.iterator();
	}
}
