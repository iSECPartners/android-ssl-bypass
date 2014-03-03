package com.isecpartners.android.jdwp.pluginservice;

import org.apache.log4j.Logger;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

import com.isecpartners.android.jdwp.DalvikUtils;
import com.sun.jdi.event.Event;

public abstract class AbstractJythonConsolePlugin extends AbstractJDIPlugin {

	private final static org.apache.log4j.Logger LOGGER = Logger
			.getLogger(AbstractJythonConsolePlugin.class.getName());

	public AbstractJythonConsolePlugin(String name) {
		super(name);
	}
	/*
	 * TODO this is a dirty hack which requires modifying the jython console code 
	 * (credits: http://code.google.com/p/jythonconsole/) in order to close this thread
	 * to get control back to the main commandline
	 * Probably a better way to do this
	 */
	public void consoleQuit(Event event) {
		AbstractJythonConsolePlugin.LOGGER.info("console quit called!");
		this.resumeEventSet();
	}

	@Override
	public void handleEvent(Event event) {
		AbstractJythonConsolePlugin.LOGGER.info("handling event: " + event);
		PySystemState.initialize();
		PythonInterpreter pyi = new PythonInterpreter();
		pyi.exec("import sys");
		pyi.exec("import os");
		// you can pass the python.path to java to avoid hardcoding this
		// java -Dpython.path=/path/to/jythonconsole-0.0.6 EmbedExample
		pyi.exec("con_path = os.path.abspath(r'jythonconsole/jythonconsole-0.0.7/')");
		pyi.exec("sys.path.append(con_path)");
		pyi.exec("from console import main");
		PyObject main = pyi.get("main");

		// stuff some objects into the namespace

		// TODO make an event wrapper object for easier console use?
		DalvikUtils dvmUtils = new DalvikUtils(event.virtualMachine(), 0);
		pyi.set("event", event);
		pyi.set("dvmUtils", dvmUtils);
		pyi.set("plugin", this);
		main.__call__(pyi.getLocals());
	}

	@Override
	public abstract void setupEvents();

}
