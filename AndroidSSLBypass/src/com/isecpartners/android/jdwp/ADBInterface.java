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
	
	public ADBInterface(String adbLocation){
		AndroidDebugBridge.init(true);
		AndroidDebugBridge.createBridge(adbLocation, true);
		AndroidDebugBridge.addDebugBridgeChangeListener(this);
		this.startDeviceListener();
	}
	
	public IDevice[] getDevices(){
		this.devices = AndroidDebugBridge.getBridge().getDevices();
		return this.devices;
	}
	
	public Client[] getClients(){
		if(this.currentDevice != null){
			this.clients = this.currentDevice.getClients();
		}
		return this.clients;
	}

	@Override
	public void clientChanged(Client arg0, int arg1) {
		LOGGER.info("clientChanged: " + arg0);
	}

	@Override
	public void deviceChanged(IDevice arg0, int arg1) {
		LOGGER.info("deviceChanged: " + arg0);
		this.devices = AndroidDebugBridge.getBridge().getDevices();
	}

	@Override
	public void deviceConnected(IDevice arg0) {
		LOGGER.info("deviceConnected: " + arg0);
		this.devices = AndroidDebugBridge.getBridge().getDevices();
	}

	@Override
	public void deviceDisconnected(IDevice arg0) {
		LOGGER.info("deviceDisconnected: " + arg0);
	}

	public void setCurrentDevice(IDevice d) {
		for(IDevice dev: this.getDevices()){
			if(d.getName().equals(dev.getName())){
				this.currentDevice = dev;
				this.startClientListener();
				break;
			}
		}
	}

	public IDevice getCurrentDevice() {
		return this.currentDevice;
	}

	@Override
	public void bridgeChanged(AndroidDebugBridge arg0) {
		LOGGER.info("bridgeChanged: " + arg0);
		
	}
	
	private void startDeviceListener() {
		AndroidDebugBridge.addDeviceChangeListener(this);
		if (AndroidDebugBridge.getBridge().hasInitialDeviceList()) {
			this.devices = AndroidDebugBridge.getBridge().getDevices();
		}
	}

	private void startClientListener() {
		AndroidDebugBridge.addClientChangeListener(this);
		if (this.currentDevice != null) {
			this.clients = this.currentDevice.getClients();
		}

	}

}
