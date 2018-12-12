package edu.northwestern.amq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AMQClient {

	private static final int DEFAULT_MAX_FAILURE_COUNT = 10;
	protected static final long DEFAULT_SLEEP_DURATION = 1000;
	protected static final Logger logger = LoggerFactory.getLogger("logger");
	protected String apikey;
	protected String topic;
	protected Environment env;
	protected int maxFailures = DEFAULT_MAX_FAILURE_COUNT;

	public enum Environment {
	       /**
	     * 200 OK, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.2.1">HTTP/1.1 documentation</a>}.
	     */
		/**
		 * Apigee Development environment
		 */
		DEV,
		
		/**
		 * Apigee Test (synonymous with QA) environment
		 */
		TEST,
		
		/**
		 * Convenience value for people who might be more comfortable with the name QA.
		 */
		QA,
		
		/**
		 * Apigee Production environment.
		 */
		PROD;
	
		public String toString() {
			switch (this) {
			case DEV:
				return "dev";
			case TEST:
				return "test";
			case QA:
				return "test";
			case PROD:
				return "prod";
			}
	
			return null;
		}
	}

	public AMQClient() {
		super();
	}

	protected void setAPIKey(String apikey) {
		this.apikey = apikey;
	}

	protected void setTopic(String topic) {
		this.topic = topic;
	}

	protected void setEnv(Environment env) {
		this.env = env;
	}

	protected void setMaxAttempts(int maxAttempts) {
		this.maxFailures = maxAttempts;
	}

}