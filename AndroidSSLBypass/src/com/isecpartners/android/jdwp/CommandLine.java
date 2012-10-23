package com.isecpartners.android.jdwp;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.EnhancedPatternLayout;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;

import asg.cliche.Command;
import asg.cliche.Param;
import asg.cliche.ShellFactory;

import com.android.ddmlib.Client;
import com.android.ddmlib.IDevice;
import com.isecpartners.android.jdwp.common.Message;
import com.isecpartners.android.jdwp.common.QueueAgent;
import com.isecpartners.android.jdwp.pluginservice.JDIPlugin;
import com.isecpartners.android.jdwp.pluginservice.JDIPluginServiceFactory;
import com.isecpartners.android.jdwp.pluginservice.JythonPluginServiceFactory;
import com.isecpartners.android.jdwp.pluginservice.PluginService;

public class CommandLine extends QueueAgent {
	
	private static final String INDENT = "\t\t\t\t";
	private ADBInterface adb;
	private static Logger LOGGER = Logger
			.getLogger(CommandLine.class.getName());

	private Control ctrl;
	private Iterator<JDIPlugin> vmHandlers;
	private PluginService pluginService;
	private IDevice currentDevice;
	private PluginService jythonPluginService;
	private Iterator<JDIPlugin> jythonHandlers;
	private ArrayList<JDIPlugin> handlerPlugins = new ArrayList<JDIPlugin>();

	/*
	 * TODO eventually should make use of ADB optional
	 */
	public CommandLine(){
		this.adb = new ADBInterface();		
	}

	public static void main(String args[]) {
		Layout layout = new EnhancedPatternLayout(
				"%r [%t] %p %c %M- %m%n");

		ConsoleAppender c = new org.apache.log4j.ConsoleAppender();
		c.setWriter(new PrintWriter(System.out));
		c.setLayout(layout);

		Logger.getRootLogger().addAppender(c);

		try {
			FileAppender f = new org.apache.log4j.FileAppender(layout,
					"logs.txt");
			Logger.getRootLogger().addAppender(f);
		} catch (IOException e1) {
			LOGGER.error("could not get file appender: " + e1);
		}

		try {
			ShellFactory.createConsoleShell("cmd", "", new CommandLine())
					.commandLoop();
		} catch (IOException e) {
			LOGGER.error("main exited with exception: " + e);
		}
	}
	
	
	
	
	@Command(name = "list-devices", abbrev = "ld", description = "List available devices")
	public String listDevices(){
		StringBuilder sb = new StringBuilder("Devices:\n");
		IDevice[] devs = this.adb.getDevices();
		for(IDevice d : devs){
			sb.append(INDENT + d.getSerialNumber() + " : " + d.getName() + "\n");
		}
		return sb.toString();
	}
	
	@Command(name = "select-device", abbrev = "sd", description = "Select from available devices")
	public String selectDevice(@Param(name = "deviceid", description = "device id") String devID){
		StringBuilder sb = new StringBuilder("Selected Device:\n");
		IDevice[] devs = this.adb.getDevices();
		for(IDevice d : devs){
			if(d.getSerialNumber().equals(devID)){
				this.currentDevice = d;
				this.adb.setCurrentDevice(d);
				break;
			}
		}
		sb.append(INDENT + this.adb.getCurrentDevice());
		return sb.toString();
	}
	
	@Command(name = "list-clients", abbrev = "lsc", description = "List clients on current device")
	public String listClients(){
		StringBuilder sb = new StringBuilder("Clients on: " + this.currentDevice + "\n");
		
		Client[] clients = this.currentDevice.getClients();
		for(Client c : clients){
			sb.append(INDENT + c.getClientData().getClientDescription() + " : " + c.getDebuggerListenPort() + "\n");
		}
		return sb.toString();
	}

	@Command(name = "attach", abbrev = "a", description = "Attach to JDWP process")
	public String attach(
			@Param(name = "host", description = "target virtual machine host") String host,
			@Param(name = "port", description = "target virtual machine jdwp listening port (see 'list-devices and list-clients' or use DDMS to discover port)") String port) {

		CommandLine.LOGGER.info("attaching to: " + host + " : " + port);
		if (this.ctrl != null && this.ctrl.isConnected()) {
			return "could not connect, already attached to VM";
		}
		this.ctrl = new Control(host, port, this.handlerPlugins);
		this.setQueueAgentListener(ctrl);
		this.ctrl.start();
		return "attach completed";
	}

	@Command(name = "attach", abbrev = "a", description = "Attach to JDWP process")
	public String attach(
			@Param(name = "port", description = "target virtual machine jdwp listening port (see 'adb jdwp' and use DDMS to discover port)") String port) {

		CommandLine.LOGGER.info("connecting - localhost:" + port);
		if (this.ctrl != null && this.ctrl.isConnected()) {
			return "could not connect, already attached to VM";
		}
		this.ctrl = new Control(port, this.handlerPlugins);
		this.setQueueAgentListener(ctrl);
		this.ctrl.start();
		return "attach completed";
	}

	@Command(name = "detach", abbrev = "d", description = "Detach from JDWP process")
	public String detach() {
		if (this.ctrl != null && this.ctrl.isConnected()) {

			try {
				this.sendMessage(new Message(Message.Type.STOP, "detach called"));
			} catch (InterruptedException e) {
				LOGGER.error("could not send STOP message");
			}
			return "detach completed";
		} else {
			return "could not detach, not connected";
		}
	}

	@Command(name = "load-plugins", abbrev = "lp")
	public String loadPlugins(String pluginsPath) {
		LOGGER.info("attempting to load plugins from: " + pluginsPath);
		StringBuilder sb = new StringBuilder("loadedPlugins:\n");
		try {
			this.pluginService = JDIPluginServiceFactory
					.createPluginService(pluginsPath);
			this.jythonPluginService = JythonPluginServiceFactory.createPluginService(pluginsPath);
			this.vmHandlers = this.pluginService.getPlugins();
			this.jythonHandlers = this.jythonPluginService.getPlugins();
			
			LOGGER.info("got plugins: ");
			while (this.vmHandlers.hasNext()) {
				JDIPlugin handler = this.vmHandlers.next();
				this.handlerPlugins.add(handler);
				LOGGER.info(handler.getName());
				sb.append(handler.getName() + "\n");
			}
			
			while (this.jythonHandlers.hasNext()) {
				JDIPlugin handler = this.jythonHandlers.next();
				this.handlerPlugins.add(handler);
				LOGGER.info(handler.getName());
				sb.append(handler.getName() + "\n");
			}
			
		} catch (IOException e1) {
			LOGGER.error("could not load plugins due to IO exception: " + e1);
		}
		return sb.toString();
	}

	
	@Command(name = "init-plugin", abbrev = "ip")
	public String initializePlugin(
			@Param(name = "plugin name", description = "name of plugin to load") String pluginName) {
		if (this.ctrl != null && this.ctrl.isConnected() && this.pluginService != null) {
			try {
				this.pluginService.initPlugin(this.ctrl.getVMEM(), pluginName);
				this.jythonPluginService.initPlugin(this.ctrl.getVMEM(), pluginName);
				return "attempted to init plugin: " + pluginName;
			} catch (NoVMSessionException e) {
				
			}
		}
		return "could not initialize plugin: " + pluginName + " no active virtual machine session";
	}

	@Command(name = "list-plugins", abbrev = "lsp")
	public String listPlugins() {
		StringBuilder plugins = new StringBuilder("plugins:\n");
		while (this.vmHandlers != null && this.vmHandlers.hasNext()) {
			plugins.append(this.vmHandlers.next().getName() + "\n");
		}
		return plugins.toString();
	}

	@Command(name = "quit", abbrev = "q")
	public void quit() {
		CommandLine.LOGGER.info("quit called");
		System.exit(0);
	}
	
}
