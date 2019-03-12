package edu.northwestern.amq;

import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

/**
 * This represents the actions that can be taken by a consumer against a Queue.
 * 
 * @author bab4379
 *
 */
public class AMQPublisher extends AMQClient {

	private static final String APIGEE_WRITE_TO_TOPIC_URL_PATTERN = "https://northwestern-{0}.apigee.net/v1/event-hub/topic/{1}";

	private MediaType contentType = null;
	private HttpClient httpClient = null;
	
	public static class PublisherBuilder {
		
		private AMQPublisher publisher = new AMQPublisher();
		
        public static PublisherBuilder create() {
            return new PublisherBuilder();
        }

        public AMQPublisher build() {
        	//Verify the object was completely instantiated.
        	if(publisher.apikey == null || publisher.apikey.trim().length() == 0) {
        		throw new IllegalArgumentException("APIKey is required.");
        	}
        	
        	if(publisher.env == null) {
        		throw new IllegalArgumentException("Environment is required.");
        	}
        	
        	if(publisher.topic == null || publisher.topic.trim().length() == 0) {
        		throw new IllegalArgumentException("Topic is required.");
        	}

        	if(publisher.contentType == null) {
        		throw new IllegalArgumentException("ContentType is required.");
        	}

        	//Create the HttpClient
    		// Set the timeout for this request in milliseconds.
    		//Will wait 10 seconds to get a connection from the connection manager and 6 seconds to connect to the server.
    		RequestConfig.Builder requestBuilder = RequestConfig.custom().setConnectTimeout(6 * 1000).setConnectionRequestTimeout(10 * 1000);

    		// Create the client used to connect to the service. For performance reasons this can and should be reused across your application
    		publisher.httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestBuilder.build()).build();
        	
            return publisher;
        }

        public PublisherBuilder setAPIKey(String apiKey) {
            publisher.setAPIKey(apiKey);
            return this;
        }
        
        public PublisherBuilder setTopic(String topic) {
            publisher.setTopic(topic);
            return this;
        }

        public PublisherBuilder setEnv(Environment env) {
            publisher.setEnv(env);
            return this;
        }
        
        public PublisherBuilder setEnv(String env) {
            publisher.setEnv(Environment.valueOf(env.toUpperCase()));
            return this;
        }
        
        public PublisherBuilder setMaxAttempts(int maxAttempts) {
            publisher.setMaxAttempts(maxAttempts);
            return this;
        }

        public PublisherBuilder setContentType(MediaType contentType) {
            publisher.setContentType(contentType);
            return this;
        }
	}

	/**
	 * Private construction so the only way to construct an instance of this object is via the {@link PublisherBuilder}
	 */
	private AMQPublisher() {

	}

	private void setContentType(MediaType contentType) {
		this.contentType = contentType;
	}
	
	private String buildWriteURL() {
    	MessageFormat mf = new MessageFormat(APIGEE_WRITE_TO_TOPIC_URL_PATTERN);

    	return mf.format(new Object[] { env, topic, });
	}

	public WriteResult writeToTopic(String message) throws InterruptedException, IllegalStateException, UnsupportedEncodingException {
		
		if(message != null && message.trim().length() > 0) {
			WriteResult writeResult = new WriteResult();

			// Create the POST that will be sent to the server
			HttpPost postRequest = new HttpPost(buildWriteURL());
			StringEntity input = new StringEntity(message);
			postRequest.setEntity(input);
	
			// Apigee API key used for authentication on Apigee
			postRequest.addHeader("apikey", apikey);
			postRequest.addHeader("Content-Type", contentType.toString());
	
			// Exit condition for the loop. We should exit if the call is successful, or if we hit the maximum number of retries
			boolean done = false;
			int failureCount = 0;
	
			do {
				try {
					// Call the service
					HttpResponse postResponse = httpClient.execute(postRequest);
	
					//Set the status code.  If we retry for some reason this will get overwritten
					writeResult.setStatusCode(postResponse.getStatusLine().getStatusCode());
	
					// Check to make sure we received a response in the 200 Family
					if (Family.familyOf(postResponse.getStatusLine().getStatusCode()) == Response.Status.Family.SUCCESSFUL) {
						failureCount = 0;
						done = true;
						writeResult.setSuccess(true);

						Header messageIdHeader = postResponse.getFirstHeader(MESSAGE_ID_HEADER_NAME);
						
						if(messageIdHeader != null) {
							writeResult.setMessageId(messageIdHeader.getValue());
						}
					}
					// This will capture all the 500-level Server Error Status Codes.
					else if (Family.familyOf(postResponse.getStatusLine().getStatusCode()) == Response.Status.Family.SERVER_ERROR) {
						//Increment the failure counter.  Once we hit the retry limit we will want to break from the loop regardless
						//of whether the calls were successful or not.
						failureCount++;
	
						//Retrieve the Response Body (if any) and log the body and status code
						String responseBody = EntityUtils.toString(postResponse.getEntity());
						logger.debug("Status Code: {}, Response Body: {}", postResponse.getStatusLine().getStatusCode(), responseBody);
	
						//If we have reached the max number of retries we should log that and set the flag to true so we will break 
						if (failureCount >= maxFailures) {
							logger.debug("Too many errors, quiting.");
	
							done = true;
							writeResult.setSuccess(false);
						}
						//Otherwise pause the application for a the SLEEP_DURATION to provide time for things to recover before trying again.
						else {
							logger.debug("Sleeping for {} milliseconds before reprocessing.", (sleepDuration * failureCount));

							Thread.sleep(sleepDuration * failureCount);
						}
					}
					// There was an unexpected result that should be handled in some way depending on your use case
					else {
						String responseBody = EntityUtils.toString(postResponse.getEntity());
						logger.debug("Status Code: {}, Response Body: {}", postResponse.getStatusLine().getStatusCode(), responseBody);
	
						done = true;
						writeResult.setSuccess(false);
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
						writeResult.setSuccess(false);
					}
					//Otherwise pause the application for a the SLEEP_DURATION to provide time for things to recover before trying again.
					else {
						logger.debug("Sleeping for {} milliseconds before reprocessing.", (sleepDuration * failureCount));
	
						Thread.sleep(sleepDuration * failureCount);
					}
				}
			} while (!done);
	
			return writeResult;
		}
		else {
			throw new IllegalStateException("Message cannot be null or blank.");
		}
	}

}