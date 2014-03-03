package com.isecpartners.android.jdwp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.isecpartners.android.jdwp.common.Message;
import com.isecpartners.android.jdwp.common.QueueAgent;
import com.isecpartners.android.jdwp.connection.AbstractConnection;
import com.isecpartners.android.jdwp.connection.DefaultConnectionFactory;
import com.isecpartners.android.jdwp.connection.NoAttachingConnectorException;
import com.isecpartners.android.jdwp.pluginservice.JDIPlugin;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.VMStartException;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.request.EventRequestManager;

public class VirtualMachineSession extends QueueAgent {
	private final static org.apache.log4j.Logger LOGGER = Logger
			.getLogger(VirtualMachineSession.class.getName());
	private DefaultConnectionFactory defaultConnectionFactory = new DefaultConnectionFactory();

	private AbstractConnection dvmConnection = null;

	private String host = null;

	private String port = null;

	private VirtualMachine vm = null;

	private VirtualMachineEventManager vmem;

	ArrayList<JDIPlugin> vmHandlers = null;

	private DalvikUtils vmUtils = null;

	public VirtualMachineSession(String host, String port, ArrayList<JDIPlugin> vmHandlers)
			throws NoAttachingConnectorException {
		this.setName("vm session");
		this.host = host;
		this.port = port;
		this.vmHandlers = vmHandlers;
	}

	public void connect(String port) throws NoAttachingConnectorException,
			VMDisconnectedException, IllegalConnectorArgumentsException,
			IOException, VMStartException {
		VirtualMachineSession.LOGGER
				.debug("VirtualMachineSession connect called");

		this.dvmConnection = this.defaultConnectionFactory.createSocket(
				this.host, port);
		this.setQueueAgentListener(this.dvmConnection);
		this.dvmConnection.connect();
	}

	public void disconnect() {
		this.dvmConnection.disconnect();
	}

	public List<ReferenceType> getClasses() {
		return this.vm.allClasses();
	}

	public EventQueue getEventQueue() {
		return this.vm.eventQueue();
	}

	public EventRequestManager getEventRequestManager() {
		return this.vm.eventRequestManager();
	}

	public List<ThreadReference> getThreads() {
		return this.vm.allThreads();
	}

	public ArrayList<JDIPlugin> getVMEventHandlers() {
		return this.vmHandlers;
	}

	public VirtualMachineEventManager getVMEventManager() {
		return this.vmem;
	}

	public DalvikUtils getVMUtils() {
		return this.vmUtils;
	}
	
	
	private void handleConnectionEvent(Object newValue) {
		VirtualMachineSession.LOGGER
				.debug("handleConnectionEvent: CONNECTED = " + (newValue != null ? newValue : "null"));
		// this.vm = (VirtualMachine) newValue;
		this.vm = this.dvmConnection.getVM();
		VirtualMachineSession.LOGGER.info("connected to vm: " + (this.vm != null ? this.vm : "null"));
		this.vmUtils = new DalvikUtils(this.vm, 0);
		//this.vmUtils.suspendAllThreads();
		//TODO hoping this helps with time ...
		this.vmem = new VirtualMachineEventManager(this.vm);
		this.setQueueAgentListener(this.vmem);
		this.vmem.start();
		//this.vmUtils.resumeAllThreads();
		try {
			this.sendMessage(new Message(Message.Type.SESSION_STARTED, this.vm));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	

	@Override
	public void run() {
		boolean done = false;
		VirtualMachineSession.LOGGER.info("VirtualMachineSession starting");
		try {

			while (!done) {
				Message msg = this.getMessage();
				Object obj = msg.getObject();
				switch (msg.getType()) {

				case CONNECT:
					LOGGER.info("connecting to: " + this.port);
					this.connect(this.port);
					break;

				case CONNECTED:
					VirtualMachineSession.LOGGER
							.info("vm successfully connected, session starting ...");
					this.handleConnectionEvent(obj);
					break;

				case DISCONNECT:
					VirtualMachineSession.LOGGER.info("got disconnect event");
					this.disconnect();
					break;

				case DISCONNECTED:
					VirtualMachineSession.LOGGER.info("got disconnected event");
					this.sendMessage(msg);
					done = true;
					break;
					
				case OUTPUT:
					LOGGER.info(msg.getObject());
					this.sendMessage(msg);
					break;
					
				case STOP:
					done = true;
					break;

				default:
					VirtualMachineSession.LOGGER.info("got message:"
							+ msg.getType().name());
					break;
				}
			}

		} catch (NoAttachingConnectorException e) {
			VirtualMachineSession.LOGGER
					.error("NoAttachingConnectorException: exiting");

		} catch (VMDisconnectedException e) {
			VirtualMachineSession.LOGGER.error("VMDisconnectedException: "
					+ e.getMessage() + " - exiting");

		} catch (IllegalConnectorArgumentsException e) {
			VirtualMachineSession.LOGGER
					.error("IllegalConnectorArgumentsException: "
							+ e.getMessage() + " - exiting");

		} catch (IOException e) {
			VirtualMachineSession.LOGGER.error("IOException: " + e.getMessage()
					+ " - exiting");

		} catch (VMStartException e) {
			VirtualMachineSession.LOGGER.error("VMStartException: "
					+ e.getMessage() + " - exiting");

		} catch (InterruptedException e) {
			VirtualMachineSession.LOGGER.error("InterruptedException: "
					+ e.getMessage() + " - exiting");
		}
	}

	public ArrayList<JDIPlugin> getPlguins() {
		return this.vmHandlers;
	}
}
