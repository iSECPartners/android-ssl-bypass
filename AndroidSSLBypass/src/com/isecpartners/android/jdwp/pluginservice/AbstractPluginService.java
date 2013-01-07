package com.isecpartners.android.jdwp.pluginservice;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.ServiceLoader;

import org.apache.log4j.Logger;

import com.isecpartners.android.jdwp.LocationNotFoundException;
import com.isecpartners.android.jdwp.VirtualMachineEventManager;

public abstract class AbstractPluginService implements PluginService {
	private final static org.apache.log4j.Logger LOGGER = Logger
			.getLogger(AbstractPluginService.class.getName());
	protected File pluginsDir = new File("plugins");

	protected AbstractPluginService() {
		if(!pluginsDir.exists()){
			throw new IllegalArgumentException();
		}
	}
	
	protected AbstractPluginService(File dir) {
		if (!dir.exists()) {
			throw new IllegalArgumentException();
		}
		this.pluginsDir = dir;
	}

	public abstract Iterator<JDIPlugin> getPlugins();

	@Override
	public void initPlugins(VirtualMachineEventManager vmem) {
		Iterator<JDIPlugin> iterator = this.getPlugins();
		if (!iterator.hasNext()) {
			LOGGER.info("no plugins were found!");
		}
		while (iterator.hasNext()) {
			JDIPlugin plugin = iterator.next();
			LOGGER.info("initializing the plugin " + plugin.getPluginName());
			vmem.setQueueAgentListener(plugin);
			try {
				plugin.init(vmem, this.pluginsDir.getAbsolutePath());
			} catch (LocationNotFoundException e) {
				LOGGER.error("could not find location referenced by plugin: "
						+ e);
			} catch (FileNotFoundException e) {
				LOGGER.error("plugin directory not found: " + e);
			} catch (IOException e) {
				LOGGER.error("IO Exception reading plugin dir: " + e);
			}
		}
	}

	@Override
	public void initPlugin(VirtualMachineEventManager vmem, String pluginName) {
		Iterator<JDIPlugin> iterator = this.getPlugins();
		if (!iterator.hasNext()) {
			LOGGER.info("no plugins were found!");
		}
		while (iterator.hasNext()) {
			JDIPlugin plugin = iterator.next();
			if (plugin.getPluginName().equals(pluginName)) {
				LOGGER.info(plugin.getPluginName());
				LOGGER.info("initializing the plugin " + plugin.getPluginName());
				
				try {
					vmem.setQueueAgentListener(plugin);
					plugin.init(vmem, this.pluginsDir.getAbsolutePath());
				} catch (LocationNotFoundException e) {
					LOGGER.error("could not find location referenced by plugin: "
							+ e);
				} catch (FileNotFoundException e) {
					LOGGER.error("plugin directory not found: " + e);
				} catch (IOException e) {
					LOGGER.error("IO Exception reading plugin dir: " + e);
				}
			}
		}
	}

}
