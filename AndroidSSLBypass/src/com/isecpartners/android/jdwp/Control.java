package com.isecpartners.android.jdwp;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.isecpartners.android.jdwp.common.Message;
import com.isecpartners.android.jdwp.common.QueueAgent;
import com.isecpartners.android.jdwp.connection.NoAttachingConnectorException;
import com.isecpartners.android.jdwp.pluginservice.JDIPlugin;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.Type;
import com.sun.jdi.VirtualMachine;

public class Control extends QueueAgent {
	private final static org.apache.log4j.Logger LOGGER = Logger
			.getLogger(Control.class.getName());
	private static final String DEFAULT_HOST = "localhost";
	private String host = null;
	private String port = null;
	private VirtualMachineSession vmSession = null;
	private boolean connected = false;
	private ArrayList<JDIPlugin> vmHandlers = new ArrayList<JDIPlugin>();
	
	private VirtualMachine vm;

	public Control(String host, String port, ArrayList<JDIPlugin> vmHandlers) {
		this.host = host;
		this.port = port;
		this.vmHandlers  = vmHandlers;
	}
	
	public Control(String port, ArrayList<JDIPlugin> handlerPlugins) {
		this.host = DEFAULT_HOST;
		this.port = port;
		this.vmHandlers  = handlerPlugins;
	}

	public boolean isConnected(){
		return this.connected;
	}

	@Override
	public void run() {
		boolean done = false;
		try {
			
			this.vmSession = new VirtualMachineSession(this.host, this.port,
					this.vmHandlers);
			this.setQueueAgentListener(this.vmSession);
			
			Control.LOGGER.info("starting debugger session");
			this.vmSession.start();

			this.sendMessage(new Message(Message.Type.CONNECT, "attempting to start new vm session"));
			while (!done) {
				Message msg;
				try {
					msg = this.getMessage();

					switch (msg.getType()) {
					case SESSION_STARTED:
						Control.LOGGER
								.info("VM successfully connected, session starting ...");
						this.connected  = true;
						break;
						
					case DISCONNECTED:
						Control.LOGGER.info("VM disconected, quitting: " + msg.getObject());
						this.connected = false;
						// could also wait for it to start again?
						done = true;
						break;

					case OUTPUT:
						LOGGER.info("got message: " + msg.getObject());
						//TODO this might cause problems - not really thread safe right?
						System.out.println(msg.getObject());
						break;
						
					default:
						Control.LOGGER.info("got message:"
								+ msg.getType().name());
						break;

					}
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					done = true;
				}
			}

		} catch (NoAttachingConnectorException e) {
			Control.LOGGER
					.error("NoAttachingConnectorException: currently only supports attaching connector.");

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public ArrayList<JDIPlugin> getPlugins() throws NoVMSessionException {
		if(this.vmSession == null){
			throw new NoVMSessionException();
		}
		return this.vmSession.getPlguins();
	}

	public VirtualMachineEventManager getVMEM() throws NoVMSessionException {
		if(this.vmSession == null){
			throw new NoVMSessionException();
		}
		return this.vmSession.getVMEventManager();
	}

	public void setHandlerPlugins(ArrayList<JDIPlugin> handlerPlugins) {
		this.vmHandlers = handlerPlugins;
	}

	public ArrayList<String> listClasses(String className) {
		ArrayList<String> classes = new ArrayList<String>();
		if(this.isConnected()){
			DalvikUtils vmUtils = this.vmSession.getVMUtils();
			List<Type> types = vmUtils.searchForType(className);
			for(Type t : types){
				classes.add(t.name());
			}
		}
		return classes;
	}

	public ArrayList<String> listClassMethods(String className) {
		ArrayList<String> methods = new ArrayList<String>();
		if(this.isConnected()){
			DalvikUtils vmUtils = this.vmSession.getVMUtils();
			ReferenceType type = vmUtils.findReferenceType(className);
			for(Method m : type.allMethods()){
				methods.add(m.toString());
			}
		}
		return methods;
		}
}
