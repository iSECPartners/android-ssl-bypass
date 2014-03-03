These are the JDIPlugin classes which are loaded at runtime and used to 
request VirtualMachine events and register themselves as handlers for 
them. These plugins must implement the JDIPlugin interface. 

* SSLBypassJDIPlugin

 * Bypass SSL checks for certain methods of SSL certificate pinning
	
  * javax.net.HttpsURLConnection
		
  * org.apache.http.* 
	
 * JythonConsoleJDIPlugin
 
 	* Starts a Jython console (http://code.google.com/p/jythonconsole/) at specified breakpoint
 	
 	* Console has code completion! Nice to examine stuff at runtime and debug plugin code
 	
 * TraceMethodsJDIPlugin
 
 	* Traces methods of specified classes
 	
 * TestJDIPlugin
 
 	* Will eventually be used to run tests on plugin lib code
