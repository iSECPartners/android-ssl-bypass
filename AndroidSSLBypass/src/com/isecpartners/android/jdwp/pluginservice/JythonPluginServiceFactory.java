package com.isecpartners.android.jdwp.pluginservice;

import java.io.File;
import java.io.IOException;

public class JythonPluginServiceFactory {

	public static PluginService createPluginService(String pluginsPath)
			throws IOException {
		File path = new File(pluginsPath);

		ClasspathUtils.addDirToClasspath(path);
		return JythonPluginService.getInstance(path);
	}
	
}