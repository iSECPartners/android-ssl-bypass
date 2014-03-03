package com.isecpartners.android.jdwp.pluginservice;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.python.util.PythonInterpreter;

public class JythonPluginService extends AbstractPluginService {
	private final static org.apache.log4j.Logger LOGGER = Logger
			.getLogger(JythonPluginService.class.getName());

	private static JythonPluginService pluginService = null;
	private Iterator<JDIPlugin> plugins = null;

	private JythonPluginService(File dir) {
		super(dir);
	}

	public static JythonPluginService getInstance(File dir) {
		//not really working as singleton because not reloading when load plugins is called again ...
		JythonPluginService.pluginService = new JythonPluginService(dir);
		return JythonPluginService.pluginService;
	}

	public Object getJythonObject(String interfaceName,
			String pathToJythonModule) {

		Object javaInt = null;
		PythonInterpreter interpreter = new PythonInterpreter();
		interpreter.execfile(pathToJythonModule);

		int start = pathToJythonModule.lastIndexOf(File.separator) + 1;
		int end = pathToJythonModule.lastIndexOf(".");
		String tempName = pathToJythonModule.substring(start, end);
		LOGGER.info("tempname: " + tempName);
		String javaClassName = tempName;
		String instanceName = tempName.toLowerCase();
		String objectDef = "=" + javaClassName + "()";
		interpreter.exec(instanceName + objectDef);
		try {
			Class JavaInterface = Class.forName(interfaceName);
			javaInt = interpreter.get(instanceName).__tojava__(JavaInterface);
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace(); // Add logging here
		}

		return javaInt;
	}

	@Override
	public Iterator<JDIPlugin> getPlugins() {
		ArrayList<JDIPlugin> pluginsArray = new ArrayList<JDIPlugin>();
		for (File f : this.pluginsDir.listFiles()) {
			if (f.getAbsolutePath().endsWith(".py")) {
				JDIPlugin plugin = (JDIPlugin) this.getJythonObject(
						JDIPlugin.class.getName(), f.getAbsolutePath());
				pluginsArray.add(plugin);
			}
		}
		return this.plugins = pluginsArray.iterator();
	}
}
