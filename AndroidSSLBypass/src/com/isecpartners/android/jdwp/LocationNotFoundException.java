package com.isecpartners.android.jdwp;

public class LocationNotFoundException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public LocationNotFoundException(String locationString) {
		super("location not found: " + locationString);
	}

}
