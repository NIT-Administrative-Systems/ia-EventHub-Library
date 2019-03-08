package edu.northwestern.amq;

public class AcknowledgeResult implements EventHubResult {


	private boolean success;
	private int statusCode;

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
}