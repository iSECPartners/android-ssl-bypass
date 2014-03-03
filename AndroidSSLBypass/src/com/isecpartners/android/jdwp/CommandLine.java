package com.isecpartners.android.jdwp;

import static org.kohsuke.args4j.ExampleMode.ALL;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.EnhancedPatternLayout;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import asg.cliche.Command;
import asg.cliche.Param;
import asg.cliche.ShellFactory;

import com.android.ddmlib.Client;
import com.android.ddmlib.IDevice;
import com.isecpartners.android.jdwp.common.Message;
import com.isecpartners.android.jdwp.common.QueueAgent;
import com.isecpartners.android.jdwp.pluginservice.JDIPlugin;
import com.isecpartners.android.jdwp.pluginservice.JDIPluginService;
import com.isecpartners.android.jdwp.pluginservice.JDIPluginServiceFactory;
import com.isecpartners.android.jdwp.pluginservice.JythonPluginService;
import com.isecpartners.android.jdwp.pluginservice.JythonPluginServiceFactory;
import com.isecpartners.android.jdwp.pluginservice.PluginNotFoundException;
import com.sun.jdi.ClassType;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.EventRequest;

public class CommandLine extends QueueAgent {

	private static final String INDENT = "\t";
	private ADBInterface adb;
	private static Logger LOGGER = Logger
			.getLogger(CommandLine.class.getName());

	private Control ctrl;
	private Iterator<JDIPlugin> vmHandlers;
	private JDIPluginService pluginService;
	private IDevice currentDevice;
	private JythonPluginService jythonPluginService;
	private Iterator<JDIPlugin> jythonHandlers;
	private ArrayList<JDIPlugin> handlerPlugins = new ArrayList<JDIPlugin>();
	private File propsFile;
	protected Properties defaultProperties = new Properties();

	@Argument
	private List<String> arguments = new ArrayList<String>();

	@Option(name = "--adb-location", usage = "set the location of adb e.g. C:\\Program Files (x86)\\Android\\android-sdk\\platform-tools\\adb.exe", metaVar = "ADB_LOCATION")
	private String adbLocation = null;

	@Option(name = "--help", usage = "display usage")
	private boolean usage = false;

	@Option(name = "--config", usage = "use specified configuration file")
	private String propsPath = "defaults.prop";

	@Option(name = "--plugins-path", usage = "set path to load plugins from")
	private String pluginsPath = "plugins";

	private boolean connected = false;

	public void run() {
		boolean done = false;
		LOGGER.debug("starting cmdline thread");
		while (done == false) {
			LOGGER.info("entering loop");
			Message msg;
			try {
				msg = this.getMessage();
				LOGGER.info(msg);
				switch (msg.getType()) {
				case SESSION_STARTED:
					this.output("sucessfully attached");
					this.connected = true;
					break;

				case DISCONNECTED:
					LOGGER.info("VM disconected, quitting: " + msg.getObject());
					this.connected = false;
					// could also wait for it to start again?
					done = true;
					break;

				case OUTPUT:
					LOGGER.info("OUTPUT recieved: " + msg.getObject());
					this.output((String) msg.getObject());
					break;

				default:
					LOGGER.info("got message:" + msg.getType().name());
					break;

				}
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				done = true;
			}
		}
		LOGGER.info("exiting loop");

	}

	private void output(String string) {
		System.out.println(string);
	}

	public void doMain(String[] args) throws IOException {
		CmdLineParser parser = new CmdLineParser(this);
		parser.setUsageWidth(80);

		this.propsFile = new File(this.propsPath);
		if (this.propsFile.isFile()) {
			this.defaultProperties.load(new FileInputStream(this.propsPath));
			this.adbLocation = this.defaultProperties
					.getProperty(Constants.ADB_LOCATION_PROP);
		}

		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {

			System.err.println(e.getMessage());
			return;
		}

		if (usage) {

			System.err.println("Android Debug Shell Usage:\n\n");
			System.err.println("java -jar ads.jar [options...]\n");

			parser.printUsage(System.err);
			System.err.println();

			System.err.println("  Example: java -jar ads.jar"
					+ parser.printExample(ALL) + "\n");
			System.exit(0);
		}

		if (adbLocation != null) {
			System.out.println("Starting debugger with ADB location: "
					+ adbLocation + ".....\n");
			try {
				this.adb = ADBInterface.getInstance();
				this.adb.createBridge(adbLocation);
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

		// Logger.getRootLogger().addAppender(c);

		try {
			FileAppender f = new org.apache.log4j.FileAppender(layout,
					"logs.txt");
			f.setLayout(layout);
			Logger.getRootLogger().addAppender(f);
		} catch (IOException e1) {
			LOGGER.error("could not get file appender: " + e1);
		}

		CommandLine cmdLine = new CommandLine();
		try {
			cmdLine.doMain(args);
			cmdLine.start();
			ShellFactory
					.createConsoleShell(
							"ads>",
							"\n====================\n Welcome to ANDROID DEBUG SHELL\nType ?list for list of commands\n====================\n",
							cmdLine).commandLoop();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Sets the path to the adb executable which is used for obtaining
	 * information such as running processes and their listening JDWP ports.
	 * 
	 * @param pathToADB
	 *            Local path to the adb executable on host system.
	 * @return Result of command output.
	 */
	@Command(name = "set-adb-loc", abbrev = "adb", description = "Set the location of adb for access to list-devices command")
	public String setADBLocation(
			@Param(name = "pathToADB", description = "Set the location of the adb executable to enable list devices command") String pathToADB) {
		StringBuilder res = new StringBuilder();
		File adb = new File(pathToADB);
		if (adb.isFile()) {
			this.adbLocation = pathToADB;
			try {
				this.adb = ADBInterface.getInstance();
				this.adb.createBridge(adbLocation);
				res.append("success setting ADB location to: "
						+ this.adbLocation);
			} catch (Exception e) {
				LOGGER.error("caught exception trying to start ADB interface: "
						+ e);
				this.adb = null;
				res.append("Caught exception trying to start ADB interface: "
						+ e
						+ "\nList devices and clients commands will not work.\n");
			}

		} else {
			res.append("could not set ADB location to: " + pathToADB
					+ "; file not found");
		}
		return res.toString();
	}

	/**
	 * List the available devices as returned by the "adb devices" command
	 * 
	 * @return Command result
	 */
	@Command(name = "list-devices", abbrev = "ld", description = "List available devices to debug")
	public String listDevices() {
		if (this.adb == null) {
			LOGGER.error("could not start ADB interface");
			return "could not start ADB interface... do you have another instance of ADB running (DDMS/eclipse with ADT)?";
		}
		StringBuilder sb = new StringBuilder("Devices:\n");
		IDevice[] devs = this.adb.getDevices();
		for (IDevice d : devs) {
			if(this.currentDevice == null){
				this.selectDevice(d);
			}
			
			sb.append(INDENT + d.getSerialNumber() + " : " + d.getName());
			if(d.getSerialNumber().equals(this.currentDevice.getSerialNumber())){
				sb.append(" -- CURRENT");
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	/**
	 * Select the device by its id as reported by the command "adb devices"
	 * 
	 * @param devID
	 * @return Command result
	 */
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
				this.selectDevice(d);
				break;
			}
		}
		sb.append(INDENT + this.currentDevice);
		return sb.toString();
	}

	private void selectDevice(IDevice iDevice) {
		this.currentDevice = iDevice;
		this.adb.setCurrentDevice(iDevice);
	}

	/**
	 * List the apps, or remote debugging clients, available for attach on the
	 * device
	 * 
	 * @return
	 */
	@Command(name = "list-apps", abbrev = "apps", description = "List apps (remote debugging clients) on current device")
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

	/**
	 * Attach to the listening jdwp port for the given process (app).
	 * 
	 * @param host
	 *            The address of the target device (localhost for emulator)
	 * @param port
	 *            The target listening jdwp port
	 * @return Command result
	 */
	@Command(name = "attach", abbrev = "a", description = "Attach to listening JDWP port of application on device")
	public String attach(
			@Param(name = "host", description = "target virtual machine host") String host,
			@Param(name = "port", description = "target virtual machine jdwp listening port (see 'list-devices and list-clients' or use DDMS to discover port)") String port) {

		CommandLine.LOGGER.info("attaching to: " + host + " : " + port);
		if (this.ctrl != null && this.ctrl.isConnected()) {
			return "could not connect, already attached to VM";
		}
		this.ctrl = new Control(host, port, this.handlerPlugins);
		// this is not working probably due to conflict with the
		// VirtualMachineSession consuming the ctrl out queue?
		// this.ctrl.setQueueAgentListener(this);
		this.ctrl.start();
		// TODO this is a bad hack for now, need to add a timeout here
		while (!this.ctrl.isConnected()) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		this.connected = true;
		return "successfully attached to " + host + ":" + port;
	}

	/**
	 * Attach to the listening jdwp port for the given process (app).
	 * 
	 * @param port
	 *            The target listening jdwp port
	 * @return Command result
	 */
	@Command(name = "attach", abbrev = "a", description = "Attach to listening JDWP port of application on device")
	public String attach(
			@Param(name = "port", description = "target virtual machine jdwp listening port (see 'list-devices and list-clients' or use DDMS to discover port)") String port) {

		CommandLine.LOGGER.info("attaching to localhost:" + port);
		if (this.ctrl != null && this.ctrl.isConnected()) {
			return "could not connect, already attached to VM";
		}
		this.ctrl = new Control(port, this.handlerPlugins);
		// this is not working probably due to conflict with the
		// VirtualMachineSession consuming the ctrl out queue?
		// this.ctrl.setQueueAgentListener(this);
		this.ctrl.start();
		// TODO this is a bad hack for now, need to add a timeout here
		while (!this.ctrl.isConnected()) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		this.connected = true;
		return "successfully attached to localhost:" + port;
	}

	/**
	 * Detach from the debugged application.
	 * 
	 * @return
	 */
	@Command(name = "detach", abbrev = "d", description = "Detach from JDWP process")
	public String detach() {
		// TODO this does not work
		if (this.ctrl != null && this.ctrl.isConnected()) {

			try {
				this.sendMessage(new Message(Message.Type.STOP, "detach called"));
			} catch (InterruptedException e) {
				LOGGER.error("could not send STOP message");
			}
			return "detach not implemented";
		} else {
			return "could not detach, not connected";
		}
	}

	/**
	 * Load plugins (custom classes extending the AbstractJDIlPLugin class) from
	 * the specified folder. Python plugins can simply be a .py file, Java
	 * plugins should be compiled into a jarfile.
	 * 
	 * @param pluginsPath
	 *            Path the folder which contains plugins to load.
	 * @return
	 */
	@Command(name = "load-plugins", abbrev = "lp")
	public String loadPlugins(
			@Param(name = "pluginsPath", description = "Path the folder which contains plugins (custom classes extending the AbstractJDIlPLugin class) to load.") String pluginsPath) {
		LOGGER.info("attempting to load plugins from: " + pluginsPath);
		StringBuilder sb = new StringBuilder(
				"attempting to load plugins from: " + pluginsPath + " .... \n");
		if (!this.connected) {
			return "not attached, can't load plugins";
		}
		try {
			File pPath = new File(pluginsPath);
			if (!pPath.exists()) {
				return "could not find plugins path: " + pluginsPath;
			}
			this.pluginService = (JDIPluginService) JDIPluginServiceFactory
					.createPluginService(pPath);
			this.jythonPluginService = (JythonPluginService) JythonPluginServiceFactory
					.createPluginService(pPath);
			this.vmHandlers = this.pluginService.getPlugins();
			LOGGER.info(this.vmHandlers.hasNext());
			this.jythonHandlers = this.jythonPluginService.getPlugins();

			sb.append("loaded Java plugins: \n");
			while (this.vmHandlers.hasNext()) {

				JDIPlugin handler = this.vmHandlers.next();
				this.handlerPlugins.add(handler);
				String name = handler.getPluginName();
				LOGGER.info("got java plugin: " + name);
				sb.append(INDENT + name + "\n");
			}
			sb.append("loaded Jython plugins: \n");
			while (this.jythonHandlers.hasNext()) {
				JDIPlugin handler = this.jythonHandlers.next();
				this.handlerPlugins.add(handler);
				String name = handler.getPluginName();
				LOGGER.info("got jython plugin: " + name);
				sb.append(INDENT + name + "\n");
			}

			this.ctrl.setHandlerPlugins(this.handlerPlugins);

		} catch (IOException e1) {
			LOGGER.error("could not load plugins due to IO exception: " + e1);
			sb.append("ERROR: could not load plugins due to IO exception");
		} catch (PluginNotFoundException e) {
			LOGGER.error("plugin not found");
			sb.append("ERROR: could not load plugins due to PluginNotFoundException: "
					+ e.getMessage());
		}
		return sb.toString();
	}

	public String unloadPlugins(String pluginDirToUnload) {
		return "not implemented";
	}

	/**
	 * Initialize a specific plugin from those available (see list-plugins).
	 * 
	 * @param pluginName
	 * @return
	 */
	@Command(name = "init-plugin", abbrev = "ip", description = "Initialize a specific plugin from those available (see list-plugins).")
	public String initializePlugin(
			@Param(name = "plugin name", description = "name of plugin to initialize") String pluginName) {
		StringBuilder sb = new StringBuilder("attempting to initalize plugin: "
				+ pluginName + "\n");
		LOGGER.debug("ctrl: " + this.ctrl + " , pluginName: " + pluginName);
		try {
			this.pluginService.initPlugin(this.ctrl.getVMEM(), pluginName);
			this.jythonPluginService
					.initPlugin(this.ctrl.getVMEM(), pluginName);
			LOGGER.info("attempted to init plugin: " + pluginName);
			sb.append("attempted to init plugin: " + pluginName);
		} catch (NoVMSessionException e) {
			LOGGER.error("No virtual machine session");
			sb.append("ERROR: no virtual machine session");
		} catch (PluginNotFoundException e) {
			sb.append("plugin not found: " + e.getMessage());
		}
		return sb.toString();
	}

	/**
	 * Load and init specified plugin.
	 * 
	 * @return
	 */
	@Command(name = "load-init-plugin", abbrev = "lip", description = "Load and init the specified plugin")
	public String loadAndInitPlugin(
			@Param(name = "plugin name", description = "name of plugin to initialize") String pluginName,
			@Param(name = "pluginsPath", description = "Path the folder which contains plugins (custom classes extending the AbstractJDIlPLugin class) to load.") String pluginsPath) {
		String msg = this.loadPlugins(pluginsPath);
		msg += this.initializePlugin(pluginName);
		return "attempted to laod and init plugin: " + msg;
	}

	/**
	 * List the available loaded plugins.
	 * 
	 * @return
	 */
	@Command(name = "list-plugins", abbrev = "lsp", description = "List the available loaded plugins.")
	public String listPlugins() {
		StringBuilder plugins = new StringBuilder("loaded Java plugins:\n\n");
		while (this.vmHandlers != null && this.vmHandlers.hasNext()) {
			plugins.append(this.vmHandlers.next().getPluginName() + "\n");
		}

		plugins.append("\nloaded Jython plugins: \n\n");
		while (this.jythonHandlers != null && this.jythonHandlers.hasNext()) {
			plugins.append(this.jythonHandlers.next().getPluginName() + "\n");
		}
		return plugins.toString();
	}

	@Command(name = "list-vm-events", abbrev = "events", description = "List all the currently set VM events (breakpoints, etc.)")
	public String listVMEvents() {
		StringBuilder sb = new StringBuilder();
		sb.append("Currently set VirtualMachine events: \n\n");
		if (this.ctrl.isConnected()) {
			try {
				VirtualMachineEventManager vmem = this.ctrl.getVMEM();
				for (EventRequest er : vmem.getVmEvents().keySet()) {
					JDIPlugin handler = vmem.getVmEvents().get(er);
					sb.append(INDENT + "event request: " + er + ", handler: "
							+ handler.getClass().getName() + "\n\n");
				}
			} catch (NoVMSessionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	/*
	 * List classes
	 */
	@Command(name = "list-classes", abbrev = "lsc", description = "list all loaded classes")
	public String listClasses() {
		StringBuilder sb = new StringBuilder();
		sb.append("CLASSES:\n");
		if (this.ctrl.isConnected()) {
			VirtualMachineEventManager vmem;
			try {
				vmem = this.ctrl.getVMEM();
				DalvikUtils dalvikUtils = vmem.getDalvikUtils(0);
				List<ReferenceType> classes = dalvikUtils.getAllClasses();
				for (ReferenceType c : classes) {
					sb.append(INDENT + c.name() + "\n");
				}
				return sb.toString();
			} catch (NoVMSessionException e) {
				return "could not get dalivk vm is not connected";
			}
		} else {
			return "could not list classes, not connected";
		}
	}

	/*
	 * List methods of class
	 */
	@Command(name = "list-class-methods", abbrev = "lscm", description = "list all class methods")
	public String listClasses(@Param(name = "classname") String className) {
		StringBuilder sb = new StringBuilder();

		if (this.ctrl.isConnected()) {

			ArrayList<String> methods = this.ctrl.listClassMethods(className);
			for (String m : methods) {
				sb.append(INDENT + m + "\n");
			}
			return sb.toString();
		} else {
			return "could not list class methods, not connected";
		}
	}

	/*
	 * Find classes
	 */
	@Command(name = "find-classes", abbrev = "fc", description = "find all loaded classes matching pattern")
	public String findClasses(@Param(name = "filter") String filter) {
		StringBuilder sb = new StringBuilder();
		sb.append("CLASSES:\n");
		if (this.ctrl.isConnected()) {

			ArrayList<String> classes = this.ctrl.listClasses(filter);
			for (String clazz : classes) {
				sb.append(INDENT + clazz + "\n");
			}
			return sb.toString();
		} else {
			return "could not list classes, not connected";
		}
	}

	/*
	 * Quit the debugging session
	 */
	@Command(name = "quit", abbrev = "q", description = "quit the debugging session")
	public void quit() {
		CommandLine.LOGGER.info("quit called");
		System.exit(0);
	}

}
