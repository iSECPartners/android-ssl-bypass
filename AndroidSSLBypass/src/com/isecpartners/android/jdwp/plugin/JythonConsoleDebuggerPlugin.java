package com.isecpartners.android.jdwp.plugin;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

import com.isecpartners.android.jdwp.LocationNotFoundException;
import com.isecpartners.android.jdwp.pluginservice.AbstractJDIPlugin;
import com.sun.jdi.event.Event;

public class JythonConsoleDebuggerPlugin extends AbstractJDIPlugin {
	private final static org.apache.log4j.Logger LOGGER = Logger
			.getLogger(JythonConsoleDebuggerPlugin.class.getName());

	private static final String BREAKPOINT_LOCATION = "breakpoint.location";

	private String breakpointLocation = null;
	
	
	public JythonConsoleDebuggerPlugin() throws FileNotFoundException, IOException {
		super(JythonConsoleDebuggerPlugin.class.getName());
	}

	public void consoleQuit(Event event) {
		LOGGER.info("console quit called!");
		this.resumeEventSet();
	}

	@Override
	public void setupEvents() {
		this.breakpointLocation  = this.properties.getProperty(JythonConsoleDebuggerPlugin.BREAKPOINT_LOCATION);
		assert this.breakpointLocation != null;
		try {
			
			this.createBreakpointRequest(this.breakpointLocation);
		} catch (LocationNotFoundException e) {
			LOGGER.error("could not set breakpoint on location: " + this.breakpointLocation);
		}
	}
	
	@Override
	public void handleEvent(Event event) {
		LOGGER.info("handling event: " + event);
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

		//TODO make an event wrapper object for easier console use?
		pyi.set("event", event);
		pyi.set("plugin", this);
		main.__call__(pyi.getLocals());
	}
}
