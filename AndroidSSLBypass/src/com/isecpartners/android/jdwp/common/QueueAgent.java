package com.isecpartners.android.jdwp.common;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

public abstract class QueueAgent extends Thread implements QueueAgentInterface{

	private final static org.apache.log4j.Logger LOGGER = Logger
			.getLogger(QueueAgent.class.getName());

	protected BlockingQueue<Message> messageIN;

	protected BlockingQueue<Message> messageOUT;

	public QueueAgent() {
		this.messageIN = new LinkedBlockingQueue<Message>();
		this.messageOUT = new LinkedBlockingQueue<Message>();
	}

	public void setQueueAgentListener(QueueAgentInterface qap) {
		qap.setMessageIN(this.messageOUT);
		qap.setMessageOUT(this.messageIN);
	}

	public Message getMessage() throws InterruptedException {
		Message msg = this.messageIN.take();
		QueueAgent.LOGGER.info("received msg: " + msg.getType().name());
		return msg;
	}

	public void sendMessage(Message msg) throws InterruptedException {
		QueueAgent.LOGGER.info("sending msg: " + msg.getType().name());
		this.messageOUT.put(msg);
	}

	public void setMessageIN(BlockingQueue<Message> out) {
		this.messageIN = out;
	}

	public void setMessageOUT(BlockingQueue<Message> in) {
		this.messageOUT = in;
	}

}
