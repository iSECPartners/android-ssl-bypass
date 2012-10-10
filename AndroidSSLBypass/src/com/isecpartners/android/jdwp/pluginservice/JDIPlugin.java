package com.isecpartners.android.jdwp.pluginservice;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.isecpartners.android.jdwp.LocationNotFoundException;
import com.isecpartners.android.jdwp.ReferenceTypeNotFoundException;
import com.isecpartners.android.jdwp.VirtualMachineEventManager;
import com.sun.jdi.event.Event;

public interface JDIPlugin {

	String getName();

	void init(VirtualMachineEventManager vmem, String path)
			throws LocationNotFoundException, FileNotFoundException,
			IOException;

	void setupEvents() throws LocationNotFoundException, ReferenceTypeNotFoundException;
	
	void handleEvent(Event event);

	void tearDownEvents();

}
