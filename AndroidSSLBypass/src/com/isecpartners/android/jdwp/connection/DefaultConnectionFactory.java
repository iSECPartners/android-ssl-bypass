package com.isecpartners.android.jdwp.connection;

import java.util.Map;

import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;

public class DefaultConnectionFactory {

	// TODO not implemented
	public AbstractConnection createListening(String transport, String host,
			String address) throws NoListeningConnectorException {
		return null;
	}

	public AbstractConnection createSocket(String hostname, String port)
			throws NoAttachingConnectorException {
		AttachingConnector connector = DVMConnectionProvider
				.getAttachingConnector("dt_socket");
		if (connector == null) {
			throw new NoAttachingConnectorException("no socket connectors");
		}
		// Set the connector arguments.
		Map<String, ? extends Connector.Argument> args = connector
				.defaultArguments();
		if ((hostname != null) && (hostname.length() > 0)) {
			args.get("hostname").setValue(hostname);
		}
		args.get("port").setValue(port);

		// Create the actual connection.
		return this.setTimeout(connector, args);
	}

	private AbstractConnection setTimeout(Connector connector,
			Map<String, ? extends Connector.Argument> args) {

		int timeout = 3000;
		args.get("timeout").setValue(String.valueOf(timeout));

		// Create the actual connection.
		return new AttachingConnection(connector, args);
	}

}