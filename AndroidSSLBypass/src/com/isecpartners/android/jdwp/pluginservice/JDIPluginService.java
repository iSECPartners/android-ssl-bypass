package com.isecpartners.android.jdwp.pluginservice;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.ServiceLoader;

import org.apache.log4j.Logger;

import com.isecpartners.android.jdwp.LocationNotFoundException;
import com.isecpartners.android.jdwp.VirtualMachineEventManager;

public class JDIPluginService implements PluginService {
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
		if (dir.exists()){
			this.path = dir;
			this.serviceLoader = ServiceLoader.load(JDIPlugin.class);
		}
	}

	@Override
	public Iterator<JDIPlugin> getPlugins() {
		return this.serviceLoader.iterator();
	}

	@Override
	public void initPlugins(VirtualMachineEventManager vmem) {
		Iterator<JDIPlugin> iterator = this.getPlugins();
		if (!iterator.hasNext()) {
			JDIPluginService.LOGGER.info("no plugins were found!");
		}
		while (iterator.hasNext()) {
			JDIPlugin plugin = iterator.next();
			JDIPluginService.LOGGER.info("initializing the plugin "
					+ plugin.getName());
			try {
				plugin.init(vmem, this.path.getAbsolutePath());
			} catch (LocationNotFoundException e) {
				JDIPluginService.LOGGER
						.error("could not find location referenced by plugin: "
								+ e);
			} catch (FileNotFoundException e) {
				JDIPluginService.LOGGER.error("plugin directory not found: "
						+ e);
			} catch (IOException e) {
				JDIPluginService.LOGGER
						.error("IO Exception reading plugin dir: " + e);
			}
		}
	}

	@Override
	public void initPlugin(VirtualMachineEventManager vmem, String pluginName) {
		Iterator<JDIPlugin> iterator = this.getPlugins();
		if (!iterator.hasNext()) {
			JDIPluginService.LOGGER.info("no plugins were found!");
		}
		while (iterator.hasNext()) {
			JDIPlugin plugin = iterator.next();
			if(plugin.getName().equals(pluginName)){
				JDIPluginService.LOGGER.info("initializing the plugin "
						+ plugin.getName());
				try {
					plugin.init(vmem, this.path.getAbsolutePath());
				} catch (LocationNotFoundException e) {
					JDIPluginService.LOGGER
							.error("could not find location referenced by plugin: "
									+ e);
				} catch (FileNotFoundException e) {
					JDIPluginService.LOGGER.error("plugin directory not found: "
							+ e);
				} catch (IOException e) {
					JDIPluginService.LOGGER
							.error("IO Exception reading plugin dir: " + e);
				}
			}
		}
	}
}
