package com.isecpartners.android.jdwp.pluginservice;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.ServiceLoader;

import org.apache.log4j.Logger;

import com.isecpartners.android.jdwp.LocationNotFoundException;
import com.isecpartners.android.jdwp.VirtualMachineEventManager;

public abstract class AbstractPluginService implements PluginService {
	private final static org.apache.log4j.Logger LOGGER = Logger
			.getLogger(AbstractPluginService.class.getName());
	protected File pluginsDir = null;

	protected AbstractPluginService(File dir) {
		if (!dir.exists()) {
			throw new IllegalArgumentException();
		}
		this.pluginsDir  = dir;
	}

	public abstract Iterator<JDIPlugin> getPlugins();

	@Override
	public void initPlugins(VirtualMachineEventManager vmem) throws PluginNotFoundException{
		Iterator<JDIPlugin> iterator = this.getPlugins();
		if (!iterator.hasNext()) {
			LOGGER.info("no plugins were found!");
		}
		while (iterator.hasNext()) {
			JDIPlugin plugin = iterator.next();
			this.pluginInit(vmem, plugin);
		}
	}

	@Override
	public void initPlugin(VirtualMachineEventManager vmem, String pluginName) throws PluginNotFoundException {
		Iterator<JDIPlugin> iterator = this.getPlugins();
		if (!iterator.hasNext()) {
			LOGGER.info("no plugins were found!");
		}
		while (iterator.hasNext()) {
			JDIPlugin plugin = iterator.next();
			if (plugin.getPluginName().equals(pluginName)) {
				this.pluginInit(vmem, plugin);
			}
		}
	}
	
	private void pluginInit(VirtualMachineEventManager vmem, JDIPlugin plugin) throws PluginNotFoundException{
		LOGGER.info("initializing the plugin " + plugin.getPluginName());	
		try {
			vmem.setQueueAgentListener(plugin);
			plugin.init(vmem, this.pluginsDir.getAbsolutePath());
		} catch (LocationNotFoundException e) {
			throw new PluginNotFoundException("could not find location referenced by plugin: "
					+ e);
		} catch (FileNotFoundException e) {
			throw new PluginNotFoundException("plugin directory not found: " + e);
		} catch (IOException e) {
			throw new PluginNotFoundException("IO Exception reading plugin dir: " + e);
		}
	}

}
