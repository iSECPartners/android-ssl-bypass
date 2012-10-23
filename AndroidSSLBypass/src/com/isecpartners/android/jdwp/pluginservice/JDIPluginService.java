package com.isecpartners.android.jdwp.pluginservice;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.ServiceLoader;

import org.apache.log4j.Logger;

import com.isecpartners.android.jdwp.LocationNotFoundException;
import com.isecpartners.android.jdwp.VirtualMachineEventManager;

public class JDIPluginService extends AbstractPluginService {
	private final static org.apache.log4j.Logger LOGGER = Logger
			.getLogger(JDIPluginService.class.getName());

	private static JDIPluginService pluginService;

	public static JDIPluginService getInstance(File dir) {
		if (JDIPluginService.pluginService == null) {
			JDIPluginService.pluginService = new JDIPluginService(dir);
		}
		return JDIPluginService.pluginService;
	}

	private File path = null;

	private ServiceLoader<JDIPlugin> serviceLoader;

	private JDIPluginService(File dir) {
		super(dir);
		this.serviceLoader = ServiceLoader.load(JDIPlugin.class);
	}

	@Override
	public Iterator<JDIPlugin> getPlugins() {
		return this.serviceLoader.iterator();
	}
}
