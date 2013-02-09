package com.isecpartners.android.jdwp.pluginservice;

import java.io.File;
import java.io.IOException;

public class JythonPluginServiceFactory {

	public static PluginService createPluginService(String pluginsPath)
			throws IOException, PluginNotFoundException {
		File path = new File(pluginsPath);
		if(!path.exists()){
			throw new PluginNotFoundException("could not create plugin service with dir: " + pluginsPath);
		}
		ClasspathUtils.addDirToClasspath(path);
		return JythonPluginService.getInstance(path);
	}
	
}