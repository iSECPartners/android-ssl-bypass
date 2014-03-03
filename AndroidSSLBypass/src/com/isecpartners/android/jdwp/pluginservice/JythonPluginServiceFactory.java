package com.isecpartners.android.jdwp.pluginservice;

import java.io.File;
import java.io.IOException;

public class JythonPluginServiceFactory {

	public static PluginService createPluginService(String pluginsPath)
			throws IOException, PluginNotFoundException {
		File path = new File(pluginsPath);
		return createPluginService(path);
	}

	public static PluginService createPluginService(File path) throws PluginNotFoundException, IOException {
		if(!path.exists()){
			throw new PluginNotFoundException("could not create plugin service with dir: " + path);
		}
		
		ClasspathUtils.addDirToClasspath(path);
		return JythonPluginService.getInstance(path);
	}
	
}