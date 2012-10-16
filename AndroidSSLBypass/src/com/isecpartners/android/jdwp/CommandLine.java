package com.isecpartners.android.jdwp;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.EnhancedPatternLayout;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;

import asg.cliche.Command;
import asg.cliche.Param;
import asg.cliche.ShellFactory;

import com.isecpartners.android.jdwp.common.Message;
import com.isecpartners.android.jdwp.common.QueueAgent;
import com.isecpartners.android.jdwp.pluginservice.JDIPlugin;
import com.isecpartners.android.jdwp.pluginservice.JDIPluginServiceFactory;
import com.isecpartners.android.jdwp.pluginservice.PluginService;

public class CommandLine extends QueueAgent {

	private static Logger LOGGER = Logger
			.getLogger(CommandLine.class.getName());

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

	private Control ctrl;
	private Iterator<JDIPlugin> vmHandlers;
	private PluginService pluginService;

	@Command(name = "attach", abbrev = "a", description = "Attach to JDWP process")
	public String attach(
			@Param(name = "host", description = "target virtual machine host") String host,
			@Param(name = "port", description = "target virtual machine jdwp listening port (see 'adb jdwp' and use DDMS to discover port)") String port) {

		CommandLine.LOGGER.info("attaching to: " + host + " : " + port);
		if (this.ctrl != null && this.ctrl.isConnected()) {
			return "could not connect, already attached to VM";
		}
		this.ctrl = new Control(host, port, this.vmHandlers);
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
		this.ctrl = new Control(port, this.vmHandlers);
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
				// TODO Auto-generated catch block
				e.printStackTrace();
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
			this.vmHandlers = this.pluginService.getPlugins();
			LOGGER.info("got plugins: ");
			while (this.vmHandlers.hasNext()) {
				JDIPlugin handler = this.vmHandlers.next();
				LOGGER.info(handler.getName());
				sb.append(handler.getName() + "\n");
			}
		} catch (IOException e1) {
			LOGGER.error("could not load plugins due to IO exception: " + e1);
		}
		return sb.toString();
	}

	@Command(name = "init-plugin")
	public String initializePlugin(
			@Param(name = "plugin name", description = "name of plugin to load") String pluginName) {
		if (this.ctrl != null && this.ctrl.isConnected() && this.pluginService != null) {
			try {
				this.pluginService.initPlugin(this.ctrl.getVMEM(), pluginName);
				return "succesfully initialized plugin: " + pluginName;
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
	
	@Command(name = "test", abbrev = "t")
	public void test(String port) {
		this.attach(port);
		this.loadPlugins("plugins");
		this.initializePlugin("com.isecpartners.android.jdwp.plugin.SSLBypassJDIPlugin");
	}
	
	
}
