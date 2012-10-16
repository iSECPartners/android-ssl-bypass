package com.isecpartners.android.jdwp.pluginservice;

import java.util.Iterator;

import com.isecpartners.android.jdwp.VirtualMachineEventManager;

public interface PluginService {
	Iterator<JDIPlugin> getPlugins();

	void initPlugins(VirtualMachineEventManager vmem);

	void initPlugin(VirtualMachineEventManager vmem, String pluginName);
}