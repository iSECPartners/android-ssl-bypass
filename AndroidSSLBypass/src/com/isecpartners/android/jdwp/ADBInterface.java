package com.isecpartners.android.jdwp;

import org.apache.log4j.Logger;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.AndroidDebugBridge.IClientChangeListener;
import com.android.ddmlib.AndroidDebugBridge.IDebugBridgeChangeListener;
import com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener;
import com.android.ddmlib.Client;
import com.android.ddmlib.IDevice;

public class ADBInterface implements IDeviceChangeListener, IClientChangeListener, IDebugBridgeChangeListener {
	private final static org.apache.log4j.Logger LOGGER = Logger
			.getLogger(ADBInterface.class.getName());
	private IDevice[] devices;
	private IDevice currentDevice;
	private Client[] clients;
	private static ADBInterface sAdbInterface;
	
	public ADBInterface(){
		AndroidDebugBridge.init(true);
	}
	
	public static ADBInterface getInstance(){
		if(ADBInterface.sAdbInterface != null){
			return ADBInterface.sAdbInterface;
		}
		ADBInterface.sAdbInterface = new ADBInterface();
		return ADBInterface.sAdbInterface;
	}
	
	public void createBridge(String adbLocation){
		AndroidDebugBridge.createBridge(adbLocation, true);
		startDeviceListener();
		AndroidDebugBridge.addDebugBridgeChangeListener(sAdbInterface);
	}
	
	public IDevice[] getDevices(){
		devices = AndroidDebugBridge.getBridge().getDevices();
		return devices;
	}
	
	public Client[] getClients(){
		if(currentDevice != null){
			clients = currentDevice.getClients();
		}
		return clients;
	}

	@Override
	public void clientChanged(Client arg0, int arg1) {
		LOGGER.info("clientChanged: " + arg0);
	}

	@Override
	public void deviceChanged(IDevice arg0, int arg1) {
		LOGGER.info("deviceChanged: " + arg0);
		devices = AndroidDebugBridge.getBridge().getDevices();
	}

	@Override
	public void deviceConnected(IDevice arg0) {
		LOGGER.info("deviceConnected: " + arg0);
		devices = AndroidDebugBridge.getBridge().getDevices();
	}

	@Override
	public void deviceDisconnected(IDevice arg0) {
		LOGGER.info("deviceDisconnected: " + arg0);
	}

	public void setCurrentDevice(IDevice d) {
		for(IDevice dev: this.getDevices()){
			if(d.getName().equals(dev.getName())){
				currentDevice = dev;
				startClientListener();
				break;
			}
		}
	}

	public IDevice getCurrentDevice() {
		return currentDevice;
	}

	@Override
	public void bridgeChanged(AndroidDebugBridge arg0) {
		LOGGER.info("bridgeChanged: " + arg0);
		//TODO what should happn here?
		
	}
	
	private void startDeviceListener() {
		AndroidDebugBridge.addDeviceChangeListener(sAdbInterface);
		if (AndroidDebugBridge.getBridge().hasInitialDeviceList()) {
			devices = AndroidDebugBridge.getBridge().getDevices();
		}
	}

	private void startClientListener() {
		AndroidDebugBridge.addClientChangeListener(sAdbInterface);
		if (currentDevice != null) {
			clients = currentDevice.getClients();
		}

	}

}
