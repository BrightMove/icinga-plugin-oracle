package org.icinga.oracle.plugin;

/**
 * Enumeration for Nagios status codes.
 * 
 * @author Aparna Chaudhary
 */
public enum NagiosStatus {

	OK(0), WARNING(1), CRITICAL(2), UNKNOWN(3);

	private int code;

	/**
	 * Private constructor
	 * 
	 * @param code status code
	 */
	private NagiosStatus(int code) {
		this.code = code;
	}

	/**
	 * Returns status code
	 * 
	 * @return status code
	 */
	public int getCode() {
		return code;
	}

}
