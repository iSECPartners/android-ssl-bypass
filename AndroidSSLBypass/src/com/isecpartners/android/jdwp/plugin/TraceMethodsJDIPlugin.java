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
		if (event instanceof MethodEntryEvent) {
			MethodEntryEvent meEvent = (MethodEntryEvent) event;
			ThreadReference tr = meEvent.thread();
			StackFrame fr;
			StringBuilder out = new StringBuilder("");
			try {
				fr = tr.frames().get(0);
				Location loc = fr.location();
				Method method = loc.method();
				out.append("\n =============== \n" + method.toString() + "\n=============== \n");
				out.append("local variables:\n");
				Map<LocalVariable, Value> vars = fr.getValues(fr.visibleVariables());
				for(LocalVariable key : vars.keySet()){
					out.append("\t" + key + " : " + vars.get(key) + "\n");
				}
				out.append("\n =============== \n");
				LOGGER.info(out.toString());
			} catch (IncompatibleThreadStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (AbsentInformationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			LOGGER.info("received unexpected event type: " + event);
		}
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