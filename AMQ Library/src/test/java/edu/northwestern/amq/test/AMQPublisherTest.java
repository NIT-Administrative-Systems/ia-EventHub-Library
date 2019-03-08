package edu.northwestern.amq.test;

import javax.ws.rs.core.MediaType;

import org.junit.Assert;
import org.junit.Test;

import edu.northwestern.amq.AMQPublisher;
import edu.northwestern.amq.WriteResult;


public class AMQPublisherTest {

	private AMQPublisher amqPublisher = null;
	private static final String testMessage = "{ \"name\" : \"Brent\", \"message\" : \"Hello\" }";
	
	private void createPublisher() {
		if(amqPublisher == null) {
			String topic = System.getProperty("edu.northwestern.topic");
			String apiKey = System.getProperty("edu.northwestern.apikey");
			String env = System.getProperty("edu.northwestern.env");

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
		}
		catch(Exception e) {
			System.out.println("Error");
			e.printStackTrace(System.out);
			Assert.fail(e.getMessage());
		}
	}

}