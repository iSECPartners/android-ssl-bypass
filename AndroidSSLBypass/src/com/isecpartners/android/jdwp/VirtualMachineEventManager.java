package com.isecpartners.android.jdwp;

import java.util.HashMap;

import org.apache.log4j.Logger;

import com.isecpartners.android.jdwp.common.Message;
import com.isecpartners.android.jdwp.common.QueueAgent;
import com.isecpartners.android.jdwp.pluginservice.JDIPlugin;
import com.sun.jdi.Location;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Type;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.MethodEntryRequest;
import com.sun.jdi.request.MethodExitRequest;
import com.sun.jdi.request.StepRequest;

public class VirtualMachineEventManager extends QueueAgent {
	private final static org.apache.log4j.Logger LOGGER = Logger
			.getLogger(VirtualMachineEventManager.class.getName());
	private EventSet currentEventSet;

	private VirtualMachine vm;
	private HashMap<EventRequest, JDIPlugin> vmEvents;
	
	
	private DalvikUtils vmUtils;

	public VirtualMachineEventManager(VirtualMachine vm) {
		this.vm = vm;
		this.vmUtils = new DalvikUtils(vm,0);
		this.vmEvents = new HashMap<EventRequest, JDIPlugin>();
	}
	
	public HashMap<EventRequest, JDIPlugin> getVmEvents() {
		return vmEvents;
	}

	public void setVmEvents(HashMap<EventRequest, JDIPlugin> vmEvents) {
		this.vmEvents = vmEvents;
	}


	// TODO test what happens when two things try to create the same bp
	public BreakpointRequest createBreakpointRequest(String locationString,
			JDIPlugin vmeh) throws LocationNotFoundException {
		Location loc = this.vmUtils.resolveLocation(locationString);
		if (loc == null) {
			throw new LocationNotFoundException(locationString);
		}
		BreakpointRequest bpEvent = this.vmUtils.createBreakpointRequest(loc);
		this.vmEvents.put(bpEvent, vmeh);
		VirtualMachineEventManager.LOGGER.info("BreakpointRequest created: "
				+ bpEvent);
		return bpEvent;
	}

	public ClassPrepareRequest createClassPrepareRequest(String classFilter,
			JDIPlugin vmeh) throws LocationNotFoundException,
			ReferenceTypeNotFoundException {
		Type classtype = this.vmUtils.findReferenceType(classFilter);
		if (classtype == null) {
			throw new ReferenceTypeNotFoundException(classFilter);
		}
		ClassPrepareRequest cpr = this.vmUtils
				.createClassPrepareRequest(classFilter);
		this.vmEvents.put(cpr, vmeh);
		VirtualMachineEventManager.LOGGER.info("ClassPrepareRequest created: "
				+ cpr);
		return cpr;
	}

	public MethodEntryRequest createMethodEntryRequest(String classFilter,
			JDIPlugin vmeh) throws LocationNotFoundException,
			ReferenceTypeNotFoundException {
		Type classtype = this.vmUtils.findReferenceType(classFilter);
		if (classtype == null) {
			throw new ReferenceTypeNotFoundException(classFilter);
		}
		MethodEntryRequest mer = this.vmUtils
				.createMethodEntryRequest(classFilter);
		this.vmEvents.put(mer, vmeh);
		VirtualMachineEventManager.LOGGER.info("MethodEntryRequest created: "
				+ mer);
		return mer;

	}

	public MethodExitRequest createMethodExitRequest(String classFilter,
			JDIPlugin vmeh) throws LocationNotFoundException,
			ReferenceTypeNotFoundException {
		Type classtype = this.vmUtils.findReferenceType(classFilter);
		if (classtype == null) {
			throw new ReferenceTypeNotFoundException(classFilter);
		}
		MethodExitRequest mer = this.vmUtils
				.createMethodExitRequest(classFilter);
		this.vmEvents.put(mer, vmeh);
		VirtualMachineEventManager.LOGGER.info("MethodExitRequest created: "
				+ mer);
		return mer;
	}

	public StepRequest createStepRequest(ThreadReference tr, int depth,
			int type, JDIPlugin vmeh) throws LocationNotFoundException {
		StepRequest stepEvent = this.vmUtils.createStepRequest(tr, depth, type);
		this.vmEvents.put(stepEvent, vmeh);
		VirtualMachineEventManager.LOGGER.info("StepRequest created: "
				+ stepEvent);
		return stepEvent;
	}

	public void deleteEventRequest(EventRequest req) {
		this.vmUtils.deleteEventRequest(req);
	}

	private EventQueue getEventQueue() {
		return this.vm.eventQueue();
	}

	public void resumeEventSet() {
		this.currentEventSet.resume();
	}

	@Override
	public void run() {
		boolean done = false;
		while (!done) {
			EventQueue queue = this.getEventQueue();
			EventSet eventSet;
			try {
				eventSet = queue.remove();
				this.setCurrentEventSet(eventSet);
				EventIterator it = eventSet.eventIterator();

				if (it.hasNext()) {
					Event event = it.nextEvent();
					JDIPlugin handler = this.vmEvents.get(event.request());
					if(event instanceof com.sun.jdi.event.VMDisconnectEvent || event instanceof com.sun.jdi.event.VMDeathEvent){
						done = true;
						this.sendMessage(new Message(Message.Type.DISCONNECTED,event));
						break;
					} else {
						handler.handleEvent(event);
					}
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	private void setCurrentEventSet(EventSet eventSet) {
		this.currentEventSet = eventSet;
	}

	public void setVmUtils(DalvikUtils vmUtils) {
		this.vmUtils = vmUtils;
	}

	public DalvikUtils getDalvikUtils(int index) {
		return new DalvikUtils(this.vm,index);
	}
}
