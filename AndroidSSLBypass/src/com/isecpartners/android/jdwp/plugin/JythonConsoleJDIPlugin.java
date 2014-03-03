 package com.isecpartners.android.jdwp.plugin;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

import com.isecpartners.android.jdwp.LocationNotFoundException;
import com.isecpartners.android.jdwp.pluginservice.AbstractJDIPlugin;
import com.isecpartners.android.jdwp.pluginservice.AbstractJythonConsolePlugin;
import com.sun.jdi.event.Event;

public class JythonConsoleJDIPlugin extends AbstractJythonConsolePlugin {
	private static final String BREAKPOINT_LOCATION = "breakpoint.location";

	private final static org.apache.log4j.Logger LOGGER = Logger
			.getLogger(JythonConsoleJDIPlugin.class.getName());

	private String breakpointLocation = null;

	public JythonConsoleJDIPlugin() throws FileNotFoundException,
			IOException {
		super(JythonConsoleJDIPlugin.class.getName());
	}

	@Override
	public void setupEvents() {
		this.breakpointLocation = this.properties
				.getProperty(JythonConsoleJDIPlugin.BREAKPOINT_LOCATION);
		assert this.breakpointLocation != null;
		try {

			this.createBreakpointRequest(this.breakpointLocation);
			LOGGER.info("success setting up events!");
		} catch (LocationNotFoundException e) {
			JythonConsoleJDIPlugin.LOGGER
					.error("could not set breakpoint on location: "
							+ this.breakpointLocation);
		}
	}
	
}
