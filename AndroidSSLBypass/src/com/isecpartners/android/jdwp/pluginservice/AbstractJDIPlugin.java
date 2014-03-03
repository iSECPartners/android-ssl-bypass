package com.isecpartners.android.jdwp.pluginservice;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.isecpartners.android.jdwp.LocationNotFoundException;
import com.isecpartners.android.jdwp.ReferenceTypeNotFoundException;
import com.isecpartners.android.jdwp.VirtualMachineEventManager;
import com.isecpartners.android.jdwp.common.Message;
import com.isecpartners.android.jdwp.common.QueueAgent;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.Event;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.MethodEntryRequest;
import com.sun.jdi.request.MethodExitRequest;
import com.sun.jdi.request.StepRequest;

public abstract class AbstractJDIPlugin extends QueueAgent implements JDIPlugin {
	private final static org.apache.log4j.Logger LOGGER = Logger
			.getLogger(AbstractJDIPlugin.class.getName());

	protected static final String DEFAULT_JAVA_PLUGIN_DIR = "plugins";

	protected Event currentEvent = null;
	protected ArrayList<EventRequest> eventRequestList = new ArrayList<EventRequest>();
	protected boolean mDone = false;

	protected String name = null;

	protected Properties properties = new Properties();

	protected String propsPath = null;

	protected VirtualMachineEventManager vmem = null;

	protected String basePath = null;

	public String getBasePath() {
		return basePath;
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	private File propsFile = null;

	public AbstractJDIPlugin(String name){
		this.name = name;
	}
	
	public void output(String message){
		LOGGER.info(message);
		try {
			this.sendMessage(new Message(Message.Type.OUTPUT,message));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void createBreakpointRequest(String locationString)
			throws LocationNotFoundException {

		BreakpointRequest req = this.vmem.createBreakpointRequest(
				locationString, this);
		this.eventRequestList.add(req);
	}

	public void createClassPrepareRequest(String classFilter)
			throws LocationNotFoundException, ReferenceTypeNotFoundException {
		ClassPrepareRequest req = this.vmem.createClassPrepareRequest(
				classFilter, this);
		this.eventRequestList.add(req);
	}

	public void createMethodEntryRequest(String classFilter)
			throws LocationNotFoundException, ReferenceTypeNotFoundException {
		MethodEntryRequest req = this.vmem.createMethodEntryRequest(
				classFilter, this);
		this.eventRequestList.add(req);
	}

	public void createMethodExitRequest(String classFilter)
			throws LocationNotFoundException, ReferenceTypeNotFoundException {
		MethodExitRequest req = this.vmem.createMethodExitRequest(classFilter,
				this);
		this.eventRequestList.add(req);
	}

	public void createStepRequest(ThreadReference tr, int depth, int type)
			throws LocationNotFoundException {
		StepRequest req = this.vmem.createStepRequest(tr, depth, type, this);
		this.eventRequestList.add(req);
	}

	public Event getCurrentEvent() {
		return this.currentEvent;
	}

	@Override
	public String getPluginName() {
		return this.name;
	}

	@Override
	public abstract void handleEvent(Event event);

	@Override
	public void init(VirtualMachineEventManager vmem, String path)
			throws LocationNotFoundException, FileNotFoundException,
			IOException {
		this.vmem = vmem;
		this.basePath = path;
		this.propsPath = path + File.separator + this.getPluginName() + ".prop";
		
		URL pathURL = ClassLoader.getSystemResource(this.propsPath);
		if(pathURL != null) {
		     this.propsFile = new File(pathURL.getPath());
		     FileInputStream fis;
				try {
					fis = new FileInputStream(this.propsFile);
					this.properties.load(fis);
				} catch (FileNotFoundException e) {
					LOGGER.error("could not load properties file, may cause problems for plugins that require it!");
					LOGGER.error(e.toString());
				} catch (IOException e) {
					LOGGER.error("could not load properties file, may cause problems for plugins that require it!");
					LOGGER.error(e.toString());
				}
				
		} else {
			LOGGER.error("could not load properties file:" + this.propsPath);
		}
		this.setupEvents();
	}

	public void resumeEventSet() {
		this.vmem.resumeEventSet();
	}

	public void setCurrentEvent(Event currentEvent) {
		this.currentEvent = currentEvent;
	}

	@Override
	public abstract void setupEvents();

	@Override
	public void tearDownEvents() {
		for (EventRequest ereq : this.eventRequestList) {
			this.vmem.deleteEventRequest(ereq);
		}
	}

	@Override
	public String toString() {
		return this.name;
	}

}
