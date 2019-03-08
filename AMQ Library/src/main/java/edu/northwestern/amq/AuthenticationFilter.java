package edu.northwestern.amq;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;


@Secured
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter, ContainerResponseFilter {

	private static final String API_KEY_NAME = "x-api-key";
	private static final String APPLICATION_NAME = "YourApp";
	private static final String REQUEST_ID_KEY_NAME = "x-request-id";
	private static String API_KEY_VALUE = "YourAPIKey";

	protected static final Logger logger = LoggerFactory.getLogger(APPLICATION_NAME);


	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		// Get the Authorization header from the request
		String apiKey = requestContext.getHeaderString(API_KEY_NAME);

		String requestId = requestContext.getHeaderString(REQUEST_ID_KEY_NAME);
		MDC.put("RequestID", requestId);

		try {

			// Validate the token
			if (apiKey == null || apiKey.trim().length() == 0 || !apiKey.equals(API_KEY_VALUE)) {
				abortWithUnauthorized(requestContext, "No " + API_KEY_NAME + " value found");
			}
		}
		catch (Exception e) {
			abortWithUnauthorized(requestContext, e.getMessage());
		}
	}

	private void abortWithUnauthorized(ContainerRequestContext requestContext, String errorMessage) {
		// Abort the filter chain with a 401 status code response
		requestContext.abortWith(Response.status(Status.UNAUTHORIZED).entity(new ErrorMessage(errorMessage, Status.UNAUTHORIZED.getStatusCode())).type(MediaType.APPLICATION_JSON).build());
	}

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
		MDC.remove("RequestID");
		responseContext.getHeaders().add("X-Powered-By", APPLICATION_NAME);
	}

}