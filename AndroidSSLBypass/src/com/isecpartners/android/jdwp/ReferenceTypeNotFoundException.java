package com.isecpartners.android.jdwp;

public class ReferenceTypeNotFoundException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ReferenceTypeNotFoundException(String classFilter) {
		super("class not found: " + classFilter);
	}

}
