package edu.northwestern.amq;

import java.util.regex.Pattern;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * @author Brent Billows
 * 
 * 
 *
 */
@Path("/")
public class WebhookExample {

	protected static final Logger webhookLogger = LoggerFactory.getLogger("webhookLogger");

	/**
	 * This is an example implementation of a Webhook Listener.  It makes use of an annotation that calls a
	 * filter that handles Authentication.  It is not necessary to use an annotation you could also simply 
	 * use if..else logic in the method itself.
	 * 
	 * @param messageBody
	 * @param apikey
	 * @return {@link Response}
	 */

	//Needed for processing a HTTP Post
	@POST

	//The path this will listen on and respond to requests from
	@Path("/test/apikey")

	//The Content-Types that are allowed to be passed in
	@Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN })

	//The Content-Type of the Response Body that will be returned
	@Produces({ MediaType.APPLICATION_JSON,  MediaType.APPLICATION_XML })

	//This annotation causes the AuthenticationFilter.filter method to be called before this method is executed
	//You can check the incoming requests credentials there and if they are invalid respond with the appropriate 
	//error message and HTTP Status Code
	@Secured

	//This out method will look at the body sent in and respond with various HTTP Status code
	//based on what it finds.  It is a trivial example and is only useful for testing your configuration.
	public Response testWebhook(String messageBody, @HeaderParam("Content-Type") String contentType, @HeaderParam("Accepts") MediaType acceptFormat) {
		//Print the Message being passed in
		webhookLogger.info("Body + " + messageBody);

		//If the message contains the word "Fail" respond with a HTTP 500.  
		//This will cause Event to resend the message after a brief pause
		if (Pattern.compile(Pattern.quote("Fail"), Pattern.CASE_INSENSITIVE).matcher(messageBody).find()) {
			Response response = Response.serverError().entity(new ErrorMessage("Message Failed", 500)).build();
			return response;
		}
		//If the message contains the word "Poison" respond with a HTTP 406
		//EventHub will move this message to your DLQ (dead letter queue) and continue processing any additional messages
		else if (Pattern.compile(Pattern.quote("Poison"), Pattern.CASE_INSENSITIVE).matcher(messageBody).find()) {
			Response response = Response.notAcceptable(null).build();
			return response;
		}
		//EventHub treats all response in the 2XX range as success so the message will be remove from your queue and 
		//processing will continue normally
		else {
			//Message is removed from the queue and processing continues normally.
			return Response.ok().entity(new SimpleResponse("Success")).build();
		}
	}
}
