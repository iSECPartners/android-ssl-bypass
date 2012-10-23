package com.isecpartners.android.jdwp.pluginservice;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.isecpartners.android.jdwp.LocationNotFoundException;
import com.isecpartners.android.jdwp.ReferenceTypeNotFoundException;
import com.isecpartners.android.jdwp.VirtualMachineEventManager;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.Event;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.MethodEntryRequest;
import com.sun.jdi.request.MethodExitRequest;
import com.sun.jdi.request.StepRequest;

public interface JDIPlugin {

	String getName();

	void init(VirtualMachineEventManager vmem, String propertiesPath)
			throws LocationNotFoundException, FileNotFoundException,
			IOException;

	void setupEvents();
	
	void handleEvent(Event event);

	void tearDownEvents();
	
	public void createBreakpointRequest(String locationString)
			throws LocationNotFoundException ;

	public void createClassPrepareRequest(String classFilter)
			throws LocationNotFoundException, ReferenceTypeNotFoundException;

	public void createMethodEntryRequest(String classFilter)
			throws LocationNotFoundException, ReferenceTypeNotFoundException;

	public void createMethodExitRequest(String classFilter)
			throws LocationNotFoundException, ReferenceTypeNotFoundException;
	
	public void createStepRequest(ThreadReference tr, int depth, int type)
			throws LocationNotFoundException;

	public Event getCurrentEvent();
}
