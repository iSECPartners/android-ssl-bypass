package com.isecpartners.android.jdwp.connection;

import java.util.List;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.ListeningConnector;

public class DVMConnectionProvider {

	public static AttachingConnector getAttachingConnector(String transport) {
		VirtualMachineManager vmm = Bootstrap.virtualMachineManager();
		List<AttachingConnector> connectors = vmm.attachingConnectors();
		for (AttachingConnector conn : connectors) {
			if (conn.transport().name().equals(transport)) {
				return conn;
			}
		}

		return null;
	}

	public static ListeningConnector getListeningConnector(String transport) {
		VirtualMachineManager vmm = Bootstrap.virtualMachineManager();
		List<ListeningConnector> connectors = vmm.listeningConnectors();
		for (ListeningConnector conn : connectors) {
			if (conn.transport().name().equals(transport)) {
				return conn;
			}
		}
		return null;
	}

}
