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
		return createPluginService(dir);
	}

	public static PluginService createPluginService(File pPath) throws PluginNotFoundException, IOException{
		if(!pPath.exists()){
			throw new PluginNotFoundException("could not create plugin service with dir: " + pPath);
		}
		LOGGER.info("creating JDIPluginService for path: " + pPath.getAbsolutePath());
		ClasspathUtils.addDirToClasspath(pPath);
		return JDIPluginService.getInstance(pPath);
	}
	
}