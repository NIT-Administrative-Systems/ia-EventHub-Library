package edu.northwestern.amq;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MessageResult implements EventHubResult {

	private boolean hasAdditionalMessage;
	private List<Message> messages = new ArrayList<Message>();
	private int messageCount = 0;

	@JsonIgnore
	public Message getMessage() {
		if(messages != null && messages.size() == 1) {
			return messages.get(0);
		}
		else {
			throw new RuntimeException("Not Allowed.");
		}
	}
	
	@JsonIgnore
	public String getLastMessageId() {
		if(messages != null) {
			return ((Message) messages.get(messages.size() - 1)).getMessageId();
		}
		else {
			return null;
		}
	}

	@JsonProperty("messages")
	public List<Message> getMessages() {
		return messages;
	}

	public void setMessages(List<Message> messages) {
		this.messages = messages;
	}

	@JsonProperty("hasAdditionalMessage")
	public boolean hasAdditionalMessage() {
		return hasAdditionalMessage;
	}
	
	@JsonIgnore
	public boolean hasMessage() {
		if(messageCount > 0) {
			return true;
		}
		else {
			return false;
		}
	}

	public void hasAdditionalMessage(boolean hasAdditionalMessage) {
		this.hasAdditionalMessage = hasAdditionalMessage;
	}
	
	@JsonProperty("count")
	public int getMessageCount() {
		return messageCount;
	}

	public void setMessageCount(int count) {
		messageCount = count;
	}

	@JsonIgnore
	public void addMessage(Message message) {
		messages.add(message);
	}
}