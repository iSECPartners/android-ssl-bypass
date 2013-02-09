package com.isecpartners.android.jdwp.pluginservice;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

public class JDIPluginServiceFactory {
	private final static org.apache.log4j.Logger LOGGER = Logger
			.getLogger(JDIPluginServiceFactory.class.getName());

	public static PluginService createPluginService(String pluginsPath)
			throws IOException, PluginNotFoundException {
		File dir = new File(pluginsPath);
		if(!dir.exists()){
			throw new PluginNotFoundException("could not create plugin service with dir: " + pluginsPath);
		}
		LOGGER.info("creating JDIPluginService for path: " + dir.getAbsolutePath());
		ClasspathUtils.addDirToClasspath(dir);
		return JDIPluginService.getInstance(dir);
	}
	
}