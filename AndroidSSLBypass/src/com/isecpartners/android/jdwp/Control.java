package com.isecpartners.android.jdwp;

import org.apache.log4j.Logger;

import com.isecpartners.android.jdwp.common.Message;
import com.isecpartners.android.jdwp.common.QueueAgent;
import com.isecpartners.android.jdwp.connection.NoAttachingConnectorException;

public class Control extends QueueAgent {
	private final static org.apache.log4j.Logger LOGGER = Logger
			.getLogger(Control.class.getName());
	private String host = null;
	private String pluginsPath = null;
	private String port = null;
	private VirtualMachineSession vmSession = null;

	public Control(String host, String port, String pluginsPath) {
		this.host = host;
		this.port = port;
		this.pluginsPath = pluginsPath;
	}

	@Override
	public void run() {
		boolean done = false;
		try {
			this.vmSession = new VirtualMachineSession(this.host, this.port,
					this.pluginsPath);
			this.vmSession.addQueueAgentListener(this);
			this.addQueueAgentListener(this.vmSession);

			Control.LOGGER.info("starting debugger session");
			this.vmSession.start();

			this.sendMessage(new Message(Message.Type.CONNECT, this));
			while (!done) {
				Message msg;
				try {
					msg = this.getMessage();

					switch (msg.getType()) {
					case CONNECTED:
						Control.LOGGER
								.info("VM successfully connected, session starting ...");
						break;

					case DISCONNECTED:
						Control.LOGGER.info("VM disconected, quitting");
						// could also wait for it to start again?
						done = true;
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

}
