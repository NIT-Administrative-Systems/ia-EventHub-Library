package edu.northwestern.amq;

import java.io.StringReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.Status.Family;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This represents the actions that can be taken by a consumer against a Queue.
 * 
 * @author bab4379
 *
 */
public class AMQPublisher extends AMQClient {

	private static final int NO_MESSAGE_STATUS_CODE = Status.NO_CONTENT.getStatusCode();
	private static final boolean DEFAULT_INCLUDE_METADATA = true;
	private static final boolean DEFAULT_AUTO_ACKNOWLEDGE = false;
	private static final int DEFAULT_MAX_MESSAGES = 1;
	private static final String APIGEE_GET_URL_PATTERN = "https://northwestern-{0}.apigee.net/v1/event-hub/queue/{1}/message?includeMetaData={2}&count={3}&autoAcknowledge={4}";
	private static final String APIGEE_DELETE_URL_PATTERN = "https://northwestern-{0}.apigee.net/v1/event-hub/queue/{1}/message/{2}?fastForward={3}";

	private int maxMessages = DEFAULT_MAX_MESSAGES;
	private MediaType accept = MediaType.APPLICATION_JSON_TYPE;
	private boolean includeMetaData = DEFAULT_INCLUDE_METADATA;
	private boolean autoAcknowledge = DEFAULT_AUTO_ACKNOWLEDGE;;
	private long sleepDuration = DEFAULT_SLEEP_DURATION;
	private HttpClient httpClient = null;
	
	private String messageId = null;

	public static class ConsumerBuilder {
		
		private AMQPublisher consumer = new AMQPublisher();
		
        public static ConsumerBuilder create() {
            return new ConsumerBuilder();
        }

        public AMQPublisher build() {
        	//Verify the object was completely instantiated.
        	if(consumer.apikey == null || consumer.apikey.trim().length() == 0) {
        		throw new IllegalArgumentException("APIKey is required.");
        	}
        	
        	if(consumer.env == null) {
        		throw new IllegalArgumentException("Environment is required.");
        	}
        	
        	if(consumer.maxMessages > 400 || (consumer.maxMessages > 1 && !consumer.includeMetaData)) {
        		throw new IllegalArgumentException("Maximum number of messages to be retrieved cannot exceed 400.  If multiple messages are being requested includeMetaData must be set to true.");
        	}

        	if(consumer.topic == null || consumer.topic.trim().length() == 0) {
        		throw new IllegalArgumentException("Topic is required.");
        	}

        	//Create the HttpClient
    		// Set the timeout for this request in milliseconds.
    		//Will wait 10 seconds to get a connection from the connection manager and 6 seconds to connect to the server.
    		RequestConfig.Builder requestBuilder = RequestConfig.custom().setConnectTimeout(6 * 1000).setConnectionRequestTimeout(10 * 1000);

    		// Create the client used to connect to the service. For performance reasons this can and should be reused across your application
    		consumer.httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestBuilder.build()).build();
        	
            return consumer;
        }

        public ConsumerBuilder setAPIKey(String apiKey) {
            consumer.setAPIKey(apiKey);
            return this;
        }
        
        public ConsumerBuilder setTopic(String topic) {
            consumer.setTopic(topic);
            return this;
        }

        public ConsumerBuilder setEnv(Environment env) {
            consumer.setEnv(env);
            return this;
        }
        
        public ConsumerBuilder setEnv(String env) {
            consumer.setEnv(Environment.valueOf(env.toUpperCase()));
            return this;
        }
        
        public ConsumerBuilder setMaxAttempts(int maxAttempts) {
            consumer.setMaxAttempts(maxAttempts);
            return this;
        }

        public ConsumerBuilder setMaxMessages(int maxMessages) {
            consumer.setMaxMessages(maxMessages);
            return this;
        }
        
        /**
         * 
         * @param includeMetaData
         * @return {@link ConsumerBuilder}
         * 
         * The service supports calls indicating whether or not to return metaData but I cannot
         * think of any reason to not return it since the library will be parsing it out so there
         * is no overhead to the end-user.  I will leave the method and login in for now in case we
         * think of a reason or in case people use this code as an example for writing their own calls.
         */
        @SuppressWarnings("unused")
		private ConsumerBuilder includeMetaData(boolean includeMetaData) {
            consumer.includeMetaData(includeMetaData);
            return this;
        }
        
        public ConsumerBuilder autoAcknowledge(boolean autoAcknowledge) {
            consumer.autoAcknowledge(autoAcknowledge);
            return this;
        }

        public ConsumerBuilder setAccept(MediaType accept) {
            consumer.setAccept(accept);
            return this;
        }
	}

	/**
	 * Private construction so the only way to construct an instance of this object is via the {@link ConsumerBuilder}
	 */
	private AMQPublisher() {

	}

	private void setAccept(MediaType accept) {
		this.accept = accept;
	}
	
	private void setMaxMessages(int maxMessages) {
		this.maxMessages = maxMessages;
	}
	
	private void includeMetaData(boolean includeMetaData) {
		this.includeMetaData = includeMetaData;
	}
	
	private void autoAcknowledge(boolean autoAcknowledge) {
		this.autoAcknowledge = autoAcknowledge;
	}

	private String buildGetURL() {
    	MessageFormat mf = new MessageFormat(APIGEE_GET_URL_PATTERN);

    	return mf.format(new Object[] { env, topic, includeMetaData, maxMessages, autoAcknowledge });
	}

	private String buildDeleteURL(String messageId, boolean fastForward) {
    	MessageFormat mf = new MessageFormat(APIGEE_DELETE_URL_PATTERN);

    	return mf.format(new Object[] { env, topic, messageId, fastForward });
	}
	
	/**
	 * Calls to this will return a message from the queue.  This object can and should be reused for subsequent calls will return additional messages
	 * 
	 * @return {@link MessageResult}
	 * @throws Exception
	 */
	public MessageResult getMessage() throws InterruptedException {
		
//		HttpRequestRetryHandler
		
		
		if(!autoAcknowledge && messageId != null) {
			throw new IllegalStateException("You should Acknowledge the previous message before requesting a new one.");
		}

		// Create the POST that will be sent to the server
		HttpGet getRequest = new HttpGet(buildGetURL());

		// Set what type of data you would like to receive
		getRequest.addHeader("Accept", accept.getType());

		// Apigee API key used for authentication on Apigee
		getRequest.addHeader("apikey", apikey);

		// Exit condition for the loop. We should exit if the call is successful, or if we hit the maximum number of retries
		boolean done = false;
		int failureCount = 0;
		MessageResult messageResult = null;
		do {
			try {
				// Call the service
				HttpResponse getResponse = httpClient.execute(getRequest);
	
				// Check to make sure we received a response in the 200 Family
				if (Family.familyOf(getResponse.getStatusLine().getStatusCode()) == Response.Status.Family.SUCCESSFUL) {
					failureCount = 0;
					done = true;
	
					// Check to see if the status code is a 406 "No Messages" "No Content" code
					//If it is 406 than you have no messages.  Returning null?
					if (getResponse.getStatusLine().getStatusCode() != NO_MESSAGE_STATUS_CODE) {
						//Pull out the Response body as a String
						String responseString = EntityUtils.toString(getResponse.getEntity());
						logger.debug(responseString);
	
						//Figure out the content-type of the response body
						ContentType contentType = ContentType.get(getResponse.getEntity());
						String mimeType = contentType.getMimeType();
	
						if(includeMetaData) {
							if (MediaType.APPLICATION_XML.equalsIgnoreCase(mimeType)) {
								logger.debug("XML");
		
								StringReader xml = new StringReader(responseString);
								JAXBContext jaxbContext = JAXBContext.newInstance(MessageResult.class);
								Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
								messageResult = (MessageResult) jaxbUnmarshaller.unmarshal(xml);
							}
							else if (MediaType.APPLICATION_JSON.equalsIgnoreCase(mimeType)) {
								logger.debug("JSON");
								ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
								messageResult = mapper.readValue(responseString, MessageResult.class);
							}
						}
						else {
							messageResult = new MessageResult();
							Message message = new Message();
							message.setContentType(mimeType);
							message.setData(responseString);
	
							List<Message> messages = new ArrayList<Message>();
							messages.add(message);
	
							messageResult.setMessages(messages);
						}
						messageId = messageResult.getLastMessageId();
					}
				}
				// This will capture all the 500-level Server Error Status Codes.
				else if (Family.familyOf(getResponse.getStatusLine().getStatusCode()) == Response.Status.Family.SERVER_ERROR) {
					//Increment the failure counter.  Once we hit the retry limit we will want to break from the loop regardless
					//of whether the calls were successful or not.
					failureCount++;
	
					//Retrieve the Response Body (if any) and log the body and status code
					String responseBody = EntityUtils.toString(getResponse.getEntity());
					logger.debug("Status Code: {}, Response Body: {}", getResponse.getStatusLine().getStatusCode(), responseBody);
	
					//If we have reached the max number of retries we should log that and set the flag to true so we will break 
					if (failureCount >= maxFailures) {
						logger.debug("Too many errors, quiting.");
	
						done = true;
					}
					//Otherwise pause the application for a the SLEEP_DURATION to provide time for things to recover before trying again.
					else {
						logger.debug("Sleeping for {} milliseconds before reprocessing.", (sleepDuration * failureCount));
	
						Thread.sleep(sleepDuration * failureCount);
					}
				}
				// There was an unexpected result that should be handled in some way depending on your use case
				else {
					String responseBody = EntityUtils.toString(getResponse.getEntity());
					logger.debug("Status Code: {}, Response Body: {}", getResponse.getStatusLine().getStatusCode(), responseBody);
	
					done = true;
				}
			}
			catch(Exception e) {
				//Increment the failure counter.  Once we hit the retry limit we will want to break from the loop regardless
				//of whether the calls were successful or not.
				failureCount++;

				//Retrieve the Response Body (if any) and log the body and status code
				logger.debug("Status Code: {}, Response Body: {}", e.getMessage(), e);

				//If we have reached the max number of retries we should log that and set the flag to true so we will break 
				if (failureCount >= maxFailures) {
					logger.debug("Too many errors, quiting.");

					done = true;
				}
				//Otherwise pause the application for a the SLEEP_DURATION to provide time for things to recover before trying again.
				else {
					logger.debug("Sleeping for {} milliseconds before reprocessing.", (sleepDuration * failureCount));

					Thread.sleep(sleepDuration * failureCount);
				}
			}
		} while (!done);

		return messageResult;
	}


//	/**
//	 * Acknowledge the last message(s) returned
//	 * 
//	 * @return {@link AcknowledgeResult}
//	 * @throws Exception
//	 */
//	public AcknowledgeResult acknowledgeMessage(MessageResult messageResult, boolean fastForward) throws InterruptedException {
//
//		if(messageResult != null && messageResult.getMessages() != null && messageResult.getMessageCount() > 0) {
//			String lastMessageId = null;
//			for(Message message : messageResult.getMessages()) {
//				lastMessageId = message.getMessageId();
//			}
//			
//			return acknowledgeMessage(lastMessageId, (messageResult.getMessageCount() > 1 ? true : false));
//		}
//		else {
//			return null;
//		}
//	}
	
	/**
	 * Acknowledge the last message(s) returned
	 * 
	 * @return {@link AcknowledgeResult}
	 * @throws Exception
	 */
	public AcknowledgeResult acknowledgeMessage() throws InterruptedException, IllegalStateException {

		if(messageId != null) {
			AcknowledgeResult ackResult = acknowledgeMessage(messageId, true);
			
			if(ackResult.isSuccess()) {
				messageId = null;
			}
			return ackResult;
		}
		else {
			throw new IllegalStateException("There are no messages to acknowledge.");
		}
	}

	/**
	 * Acknowledge the last message(s) returned
	 * 
	 * @return {@link AcknowledgeResult}
	 * @throws InterruptedException
	 */
	protected AcknowledgeResult acknowledgeMessage(String messageId, boolean fastForward) throws InterruptedException {
		AcknowledgeResult ackResult = new AcknowledgeResult();

		// Create the POST that will be sent to the server
		HttpDelete deleteRequest = new HttpDelete(buildDeleteURL(messageId, fastForward));

		// Apigee API key used for authentication on Apigee
		deleteRequest.addHeader("apikey", apikey);

		// Exit condition for the loop. We should exit if the call is successful, or if we hit the maximum number of retries
		boolean done = false;
		int failureCount = 0;

		do {
			try {
				// Call the service
				HttpResponse getResponse = httpClient.execute(deleteRequest);

				//Set the status code.  If we retry for some reason this will get overwritten
				ackResult.setStatusCode(getResponse.getStatusLine().getStatusCode());

				// Check to make sure we received a response in the 200 Family
				if (Family.familyOf(getResponse.getStatusLine().getStatusCode()) == Response.Status.Family.SUCCESSFUL) {
					failureCount = 0;
					done = true;
					ackResult.setSuccess(true);
				}
				// Special status message to indicate the message is no longer there
				else if (getResponse.getStatusLine().getStatusCode() == Response.Status.GONE.getStatusCode()) {
					failureCount = 0;
					done = true;
					ackResult.setSuccess(true);
				}
				// This will capture all the 500-level Server Error Status Codes.
				else if (Family.familyOf(getResponse.getStatusLine().getStatusCode()) == Response.Status.Family.SERVER_ERROR) {
					//Increment the failure counter.  Once we hit the retry limit we will want to break from the loop regardless
					//of whether the calls were successful or not.
					failureCount++;

					//Retrieve the Response Body (if any) and log the body and status code
					String responseBody = EntityUtils.toString(getResponse.getEntity());
					logger.debug("Status Code: {}, Response Body: {}", getResponse.getStatusLine().getStatusCode(), responseBody);

					//If we have reached the max number of retries we should log that and set the flag to true so we will break 
					if (failureCount >= maxFailures) {
						logger.debug("Too many errors, quiting.");

						done = true;
						ackResult.setSuccess(false);
					}
					//Otherwise pause the application for a the SLEEP_DURATION to provide time for things to recover before trying again.
					else {
						logger.debug("Sleeping for {} milliseconds before reprocessing.", (sleepDuration * failureCount));
	
						Thread.sleep(sleepDuration * failureCount);
					}
				}
				// There was an unexpected result that should be handled in some way depending on your use case
				else {
					String responseBody = EntityUtils.toString(getResponse.getEntity());
					logger.debug("Status Code: {}, Response Body: {}", getResponse.getStatusLine().getStatusCode(), responseBody);

					done = true;
					ackResult.setSuccess(false);
				}
			}
			catch(Exception e) {
				//Increment the failure counter.  Once we hit the retry limit we will want to break from the loop regardless
				//of whether the calls were successful or not.
				failureCount++;

				//Retrieve the Response Body (if any) and log the body and status code
				logger.debug("Status Code: {}, Response Body: {}", e.getMessage(), e);

				//If we have reached the max number of retries we should log that and set the flag to true so we will break 
				if (failureCount >= maxFailures) {
					logger.debug("Too many errors, quiting.");

					done = true;
					ackResult.setSuccess(false);
				}
				//Otherwise pause the application for a the SLEEP_DURATION to provide time for things to recover before trying again.
				else {
					logger.debug("Sleeping for {} milliseconds before reprocessing.", (sleepDuration * failureCount));

					Thread.sleep(sleepDuration * failureCount);
				}
			}
		} while (!done);

		return ackResult;
	}
}