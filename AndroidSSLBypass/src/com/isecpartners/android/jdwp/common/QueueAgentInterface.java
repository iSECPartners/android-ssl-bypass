package com.isecpartners.android.jdwp.common;

import java.util.concurrent.BlockingQueue;

public interface QueueAgentInterface {
	
	public void setQueueAgentListener(QueueAgentInterface  qap);
	
	public Message getMessage() throws InterruptedException;
	
	public void sendMessage(Message msg) throws InterruptedException;
	
	public void setMessageIN(BlockingQueue<Message> out);
	
	public void setMessageOUT(BlockingQueue<Message> in);

}
