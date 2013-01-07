package com.isecpartners.android.jdwp.common;

public class Message {

	public static enum Type {
		CONNECT, CONNECTED, DISCONNECT, DISCONNECTED, EVENT, EVENT_HANDLED, STOP, LOAD_PLUGINS, INITIALIZE_PLUGIN, OUTPUT, SESSION_STARTED
	}

	private Type name = null;
	private Object object = null;

	public Message(Type name, Object obj) {
		this.name = name;
		this.object = obj;
	}

	public Object getObject() {
		return this.object;
	}

	public Type getType() {
		return this.name;
	}

	public void setName(Type name) {
		this.name = name;
	}

	public void setObject(Object object) {
		this.object = object;
	}

}
