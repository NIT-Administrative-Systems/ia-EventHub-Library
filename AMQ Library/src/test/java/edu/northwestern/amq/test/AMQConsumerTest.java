package edu.northwestern.amq.test;

import org.junit.Assert;
import org.junit.Test;

import edu.northwestern.amq.AMQConsumer;
import edu.northwestern.amq.AcknowledgeResult;
import edu.northwestern.amq.MessageResult;


public class AMQConsumerTest {

	private AMQConsumer amqConsumer = null;
	private static String topic = System.getProperty("edu.northwestern.topic");
	private static String apiKey = System.getProperty("edu.northwestern.apikey");
	private static String env = System.getProperty("edu.northwestern.env");

	private void createConsumer() {
		if(amqConsumer == null) {
			amqConsumer = AMQConsumer.ConsumerBuilder
					.create()
					.setEnv(env)
					.setTopic(topic)
					.setAPIKey(apiKey)
					.build();
		}
	}

	@Test
	public void getMessage() throws Exception {
		createConsumer();

		//Test getting a message
		MessageResult messageResult = amqConsumer.getMessage();
		Assert.assertNotNull("MessageResult should not be null", messageResult);

		//Test acknowledging the message
		AcknowledgeResult ackResult = amqConsumer.acknowledgeMessage();

		Assert.assertTrue("Message was not Acknowledged", ackResult.isSuccess());
	}
	
	@Test(expected = RuntimeException.class)
	public void deleteMessage() throws Exception {
		createConsumer();

		amqConsumer.acknowledgeMessage();
	}

	@Test(expected = IllegalArgumentException.class)
	public void noAPIKey() {
		AMQConsumer.ConsumerBuilder
			.create()
			.setEnv(env)
			.setTopic(topic)
			.build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void noEnv() {
		AMQConsumer.ConsumerBuilder
			.create()
			.setAPIKey(apiKey)
			.setTopic(topic)
			.build();
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void noTopic() {
		AMQConsumer.ConsumerBuilder
			.create()
			.setEnv(env)
			.setAPIKey(apiKey)
			.build();
	}
}