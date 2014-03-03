package com.isecpartners.android.jdwp.connection;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;

import com.isecpartners.android.jdwp.common.Message;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.Connector.Argument;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.VMStartException;

public class AttachingConnection extends AbstractConnection {
	private final static org.apache.log4j.Logger LOGGER = Logger
			.getLogger(AttachingConnection.class.getName());

	public AttachingConnection(Connector connector,
			Map<String, ? extends Argument> args) {
		super(connector, args);
		AttachingConnection.LOGGER.info("creating AttachingConnection");
		AttachingConnection.LOGGER.info(args);
	}
	
	@Override
	public void connect() throws IllegalConnectorArgumentsException,
			IOException, VMDisconnectedException, VMStartException {
		AttachingConnection.LOGGER.info("AttachingConnection connect() called");
		AttachingConnector conn = (AttachingConnector) this.getConnector();
		VirtualMachine vm = conn.attach(this.getConnectorArgs());
		this.setVM(vm);
		AttachingConnection.LOGGER.info("connected!");

		try {
			this.sendMessage(new Message(Message.Type.CONNECTED, vm.description()));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void disconnect() {
		AttachingConnection.LOGGER.info("disconnecting: TODO not implemented");
		this.getVM().dispose();
	}

}