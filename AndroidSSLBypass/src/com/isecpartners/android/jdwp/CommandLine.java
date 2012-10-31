package com.isecpartners.android.jdwp;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.EnhancedPatternLayout;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import static org.kohsuke.args4j.ExampleMode.ALL;

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

	private static final String INDENT = "\t\t";
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

	@Argument
	private List<String> arguments = new ArrayList<String>();

	@Option(name = "--adb-location", usage = "set the location of adb e.g. C:\\Program Files (x86)\\Android\\android-sdk\\platform-tools\\adb.exe", metaVar = "ADB_LOCATION")
	private String adbLocation = null;

	@Option(name = "--help", usage = "display usage")
	private boolean usage = false;

	public void doMain(String[] args) throws IOException {
		CmdLineParser parser = new CmdLineParser(this);

		// if you have a wider console, you could increase the value;
		// here 80 is also the default
		parser.setUsageWidth(80);
		try {
			// parse the arguments.
			parser.parseArgument(args);

		} catch (CmdLineException e) {
			// if there's a problem in the command line,
			// you'll get this exception. this will report
			// an error message.
			System.err.println(e.getMessage());
			return;
		}

		if (adbLocation == null && usage) {

			System.err.println("ASB Usage:\n\n");
			System.err.println("java -jar asb.jar [options...]\n");

			// print the list of available options
			parser.printUsage(System.err);
			System.err.println();

			// print option sample. This is useful some time
			System.err.println("  Example: java -jar asb.jar"
					+ parser.printExample(ALL) + "\n");
			System.exit(0);
		}

		if (adbLocation != null) {
			System.out.println("Starting debugger with ADB location: "
					+ adbLocation + ".....\n");
			try {
				this.adb = new ADBInterface(adbLocation);
			} catch (Exception e) {
				LOGGER.error("caught exception trying to start ADB interface: "
						+ e);
				this.adb = null;
				System.out
						.println("Caught exception trying to start ADB interface: "
								+ e
								+ "\nList devices and clients commands will not work.\n");
			}
		} else {
			System.out
					.println("No ADB location passed in arguments. Starting debugger without ADB location. List devices and clients commands will not work.\n");
		}
	}

	public static void main(String args[]) {
		Layout layout = new EnhancedPatternLayout("%r [%t] %p %c %M- %m%n");

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
			CommandLine cmdLine = new CommandLine();
			cmdLine.doMain(args);
			ShellFactory
					.createConsoleShell(
							"ASB>",
							"\n====================\n Welcome to ANDROID DEBUG SHELL\nType ?list for list of commands\n====================\n",
							cmdLine).commandLoop();
		} catch (IOException e) {
			LOGGER.error("main exited with exception: " + e);
		}
	}

	@Command(name = "list-devices", abbrev = "ld", description = "List available devices")
	public String listDevices() {
		if (this.adb == null) {
			LOGGER.error("could not start ADB interface");
			return "could not start ADB interface... do you have another instance of ADB running (DDMS/eclipse with ADT)?";
		}
		StringBuilder sb = new StringBuilder("Devices:\n");
		IDevice[] devs = this.adb.getDevices();
		for (IDevice d : devs) {
			sb.append(INDENT + d.getSerialNumber() + " : " + d.getName() + "\n");
		}
		return sb.toString();
	}

	@Command(name = "select-device", abbrev = "sd", description = "Select from available devices")
	public String selectDevice(
			@Param(name = "deviceid", description = "device id") String devID) {
		if (this.adb == null) {
			LOGGER.error("could not start ADB interface");
			return "could not start ADB interface... do you have another instance of ADB running (eclipse with ADT)?";
		}
		StringBuilder sb = new StringBuilder("Selected Device:\n");
		IDevice[] devs = this.adb.getDevices();
		for (IDevice d : devs) {
			if (d.getSerialNumber().equals(devID)) {
				this.currentDevice = d;
				this.adb.setCurrentDevice(d);
				break;
			}
		}
		sb.append(INDENT + this.adb.getCurrentDevice());
		return sb.toString();
	}

	@Command(name = "list-clients", abbrev = "lsc", description = "List clients on current device")
	public String listClients() {
		if (this.adb == null) {
			LOGGER.error("could not start ADB interface");
			return "could not start ADB interface... do you have another instance of ADB running (eclipse with ADT)?";
		}

		StringBuilder sb = new StringBuilder("Clients on: "
				+ this.currentDevice + "\n");

		Client[] clients = this.currentDevice.getClients();
		for (Client c : clients) {
			sb.append(INDENT + c.getClientData().getClientDescription() + " : "
					+ c.getDebuggerListenPort() + "\n");
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
			this.jythonPluginService = JythonPluginServiceFactory
					.createPluginService(pluginsPath);
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
		if (this.ctrl != null && this.ctrl.isConnected()
				&& this.pluginService != null) {
			try {
				this.pluginService.initPlugin(this.ctrl.getVMEM(), pluginName);
				this.jythonPluginService.initPlugin(this.ctrl.getVMEM(),
						pluginName);
				return "attempted to init plugin: " + pluginName;
			} catch (NoVMSessionException e) {

			}
		}
		return "could not initialize plugin: " + pluginName
				+ " no active virtual machine session";
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
