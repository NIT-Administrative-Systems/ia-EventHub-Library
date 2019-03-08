package edu.northwestern.amq.test;

import javax.ws.rs.core.MediaType;

import org.junit.Assert;
import org.junit.Test;

import edu.northwestern.amq.AMQPublisher;
import edu.northwestern.amq.WriteResult;


public class AMQPublisherTest {

	private AMQPublisher amqPublisher = null;
	private static final String testMessage = "{ \"name\" : \"Brent\", \"message\" : \"Hello\" }";
	
	private static String topic = System.getProperty("edu.northwestern.topic");
	private static String apiKey = System.getProperty("edu.northwestern.apikey");
	private static String env = System.getProperty("edu.northwestern.env");
	
	private void createPublisher() {
		if(amqPublisher == null) {
			amqPublisher = AMQPublisher.PublisherBuilder
					.create()
					.setEnv(env)
					.setTopic(topic)
					.setAPIKey(apiKey)
					.setContentType(MediaType.APPLICATION_JSON_TYPE)
					.build();
		}
	}

	@Test
	public void putMessage() {
		try {
			createPublisher();

			WriteResult writeResult = amqPublisher.writeToTopic(testMessage);
			
			Assert.assertNotNull("WriteResult should not be null", writeResult);
			Assert.assertTrue("WriteResult should be successful: " + writeResult.getStatusCode(), writeResult.isSuccess());

			String messageId = writeResult.getMessageId();
			Assert.assertNotNull("Message ID should not be null", messageId);
			Assert.assertNotSame("Message ID should not be empty", messageId.trim(), "");
		}
		catch(Exception e) {
			System.out.println("Error");
			e.printStackTrace(System.out);
			Assert.fail(e.getMessage());
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void noContentType() {
		AMQPublisher.PublisherBuilder
			.create()
			.setEnv(env)
			.setTopic(topic)
			.setAPIKey(apiKey)
			.build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void noEnvironment() {
		AMQPublisher.PublisherBuilder
			.create()
			.setContentType(MediaType.APPLICATION_JSON_TYPE)
			.setTopic(topic)
			.setAPIKey(apiKey)
			.build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void noAPIKey() {
		AMQPublisher.PublisherBuilder
			.create()
			.setEnv(env)
			.setTopic(topic)
			.setContentType(MediaType.APPLICATION_JSON_TYPE)
			.build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void noTopicName() {
		AMQPublisher.PublisherBuilder
			.create()
			.setEnv(env)
			.setContentType(MediaType.APPLICATION_JSON_TYPE)
			.setAPIKey(apiKey)
			.build();
	}
}