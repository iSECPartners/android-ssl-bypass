/**
 * 
 */
package com.isecpartners.android.jdwp.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.isecpartners.android.jdwp.LocationNotFoundException;
import com.isecpartners.android.jdwp.ReferenceTypeNotFoundException;
import com.isecpartners.android.jdwp.pluginservice.AbstractJDIPlugin;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.MethodEntryEvent;

/**
 * @author nml
 * 
 */
public class TraceMethodsJDIPlugin extends AbstractJDIPlugin {

	private final static org.apache.log4j.Logger LOGGER = Logger
			.getLogger(TraceMethodsJDIPlugin.class.getName());
	private static final String FILTERS_FILENAME_KEY = "filters.filename";
	private ArrayList<String> filters = new ArrayList<String>();
	private String filtersFileName = null;

	public TraceMethodsJDIPlugin() throws FileNotFoundException, IOException {
		super(TraceMethodsJDIPlugin.class.getName());
	}

	@Override
	public void handleEvent(Event event) {
		StringBuilder out = new StringBuilder("");
		if (event instanceof MethodEntryEvent) {
			MethodEntryEvent meEvent = (MethodEntryEvent) event;
			ThreadReference tr = meEvent.thread();
			StackFrame fr;
			try {
				fr = tr.frame(0);
				Location loc = fr.location();
				Method method = loc.method();
				out.append("\n===============\n" + method.toString() + " " + method.name()
						+ "\n===============\n");
				out.append("local variables:\n");
				List<LocalVariable> visVars = fr.visibleVariables();
				if (visVars != null && !visVars.isEmpty()) {
					Map<LocalVariable, Value> vars = fr.getValues(visVars);
					for (LocalVariable key : vars.keySet()) {
						out.append("\t" + key + " : " + vars.get(key) + "\n");
					}
				}
				out.append("\n =============== \n");
			} catch (IncompatibleThreadStateException e) {
				out.append("could not get visible variables due to IncompatibleThreadStateException");
				LOGGER.error(e);
			} catch (AbsentInformationException e) {
				out.append("could not get visible variables due to AbsentInformationException");
				//TODO should probably print full stack trace
				LOGGER.error(e);
			}

		} else {
			out.append("unexpected event type for handler: " + event);
		}
		
		LOGGER.info(out.toString());
		this.output(out.toString());
		this.resumeEventSet();
	}

	@Override
	public void setupEvents() {
		String filename = this.properties.getProperty(FILTERS_FILENAME_KEY,
				"filters");
		this.filtersFileName = this.basePath + File.separator + filename;
		LOGGER.info(this.filtersFileName);
		try {
			this.filters = this.readLines(this.filtersFileName);
			for (String f : filters) {
				this.createMethodEntryRequest(f);
			}
		} catch (IOException e) {
			LOGGER.info("could not get filters filename");
		} catch (LocationNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ReferenceTypeNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public ArrayList<String> readLines(String filename) throws IOException {
		FileReader fileReader = new FileReader(filename);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		ArrayList<String> lines = new ArrayList<String>();
		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			lines.add(line);
		}
		bufferedReader.close();
		return lines;
	}

}