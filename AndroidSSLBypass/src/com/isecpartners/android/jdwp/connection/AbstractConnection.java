package com.isecpartners.android.jdwp.connection;

import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;

import com.isecpartners.android.jdwp.common.Message;
import com.isecpartners.android.jdwp.common.QueueAgent;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.ListeningConnector;
import com.sun.jdi.connect.VMStartException;

public abstract class AbstractConnection
		/* implements PropertyChangeListener */extends QueueAgent {
	private final static org.apache.log4j.Logger LOGGER = Logger
			.getLogger(AbstractConnection.class.getName());

	/** Connector. */
	private Connector connector;

	/** Connector arguments. */
	private Map<String, ? extends Connector.Argument> connectorArgs;
	/** Debuggee VM. */
	private VirtualMachine debuggeeVM;
	/** True if this is a remote connection. */
	private boolean isRemoteConnection;
	protected final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(
			this);

	/**
	 * Constructs a new JvmConnection with the given connector and arguments.
	 * 
	 * @param connector
	 *            connector.
	 * @param args
	 *            connector arguments.
	 */
	public AbstractConnection(Connector connector,
			Map<String, ? extends Connector.Argument> args) {
		this.connector = connector;
		this.connectorArgs = args;
		if ((connector instanceof AttachingConnector)
				|| (connector instanceof ListeningConnector)) {
			this.isRemoteConnection = true;
		}
	}

	public abstract void connect() throws IllegalConnectorArgumentsException,
			IOException, VMDisconnectedException, VMStartException;

	public abstract void disconnect();

	// TODO
	public String getAddress() {
		String name = this.getConnectorArg("name");
		if (name != null) {
			return name;
		}
		String port = this.getConnectorArg("port");
		if (port != null) {
			String hostname = this.getConnectorArg("hostname");
			if ((hostname == null) || (hostname.length() == 0)) {
				hostname = "localhost";
			}
			return hostname + ':' + port;
		}
		String main = this.getConnectorArg("main");
		if (main != null) {
			return "Launched!";
		}
		return "";
	}

	/**
	 * Returns the JDI connector associated with this connection instance.
	 * 
	 * @return a connector.
	 */
	protected Connector getConnector() {
		return this.connector;
	}

	/**
	 * Returns the named connector argument value as a String.
	 * 
	 * @param name
	 *            name of argument to retrieve.
	 * @return named argument value, or null if not available.
	 */
	protected String getConnectorArg(String name) {
		if (this.connectorArgs != null) {
			Connector.Argument arg = this.connectorArgs.get(name);
			if (arg != null) {
				return arg.value();
			}
		}
		return null;
	}

	/**
	 * Returns the connector arguments for this connection.
	 * 
	 * @return an argument map.
	 */
	protected Map<String, ? extends Connector.Argument> getConnectorArgs() {
		return this.connectorArgs;
	}

	public VirtualMachine getVM() {
		return this.debuggeeVM;
	}

	public boolean isConnected() {
		if (this.debuggeeVM != null) {
			try {
				return this.debuggeeVM.topLevelThreadGroups() != null;
			} catch (VMDisconnectedException vmde) {
				try {
					this.sendMessage(new Message(Message.Type.DISCONNECTED,
							"vm disconnection exception"));
				} catch (InterruptedException e) {
					AbstractConnection.LOGGER
							.error("thread interrupted while sending message: "
									+ e);
				}
			}
		}
		return false;
	}

	public boolean isRemote() {
		return this.isRemoteConnection;
	}

	protected void setVM(VirtualMachine vm) {
		this.debuggeeVM = vm;
	}

}
