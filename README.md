android-ssl-bypass
==================

This is an Android debugging tool that can be used for bypassing SSL, even when certificate pinning is implemented, 
as well as other debugging tasks. The tool runs as an interactive console.

The tool is based on a scriptable JDWP debugger using the JDI APIs (http://docs.oracle.com/javase/1.5.0/docs/guide/jpda/jdi/). 
The architecture of the tool is plugin based in order to be able to load debugging code at runtime. Plugins can 
currently be written in Java or Jython. These debugger plugins can create breakpoint events and register 
themselves as handlers for those breakpoints. Currently the last plugin registering an event wins as the handler for that event.

The SSLBypassJDIPlugin found in the 'plugins' directory implements bypassing SSL for certain implementations of SSL and 
certificate pinning. The process for bypassing SSL checks is simply to set breakpoints on certain functions and replace 
the TrustManager (http://developer.android.com/reference/javax/net/ssl/TrustManager.html) in use with one that trusts all certificates.
The HostNameVerifier (http://developer.android.com/reference/javax/net/ssl/HostnameVerifier.html) is also replaced with
the ALLOW_ALL_HOSTNAME_VERIFIER.

The hope is that the tool can be used as a starting point for further development. It is very much a work in progress, and will be frequently updated in the near future.

A very beta version was presented at BlackHat USA 2012: http://media.blackhat.com/bh-us-12/Turbo/Diquet/BH_US_12_Diqut_Osborne_Mobile_Certificate_Pinning_Slides.pdf

Requirements
==================

* Android API 14+

* Android SDK platform tools

* Java

* A debuggable app to test:

    * App with "debuggable" flag in Application Manifest

    * Any app on emulator
    
    * Any app on device with ro.debuggable=1 or ro.secure=0 (check "adb shell getprop ro.debugabble")

* Only tested on Windows 7 and Ubuntu 10.04

* If testing with included test app and test server, python and twisted (http://twistedmatrix.com/trac/)

Basic Usage
==================

    * Currently it is best to just run the binary from the AndroidSSLBypass root directory. Eventually 
    this will be fixed, but for now this is the only supported/tested usage.

    * To run SSLBypassJDIPlugin first install the included helper app AndroidSSLBypassHelperApp

        adb install AndroidSSLBypassHelperApp.apk

    * Start a debugging session, passing the path to ADB (optional but provides access to list device and client commands):

        java -jar asb.jar --adb-location "c:\Program Files (x86)\Android\android-sdk\platform-tools\adb.exe"

        Type ?list for a list of commands

    * List devices:

        ads>> ld
        Devices:
        	emulator-5554 : droid16 [emulator-5554]

    * Select a device:

        ads>> sd emulator-5554
        Selected Device:
        	emulator-5554

    * List clients:

        ads>> apps
        Clients on: emulator-5554
        	com.google.process.gapps : 8600
                com.android.systemui : 8601
                com.android.email : 8602
                com.android.calendar : 8603
                com.google.android.apps.maps:LocationFriendService : 8604
                com.android.providers.calendar : 8605
                com.google.android.apps.maps : 8606
                com.android.contacts : 8607
                com.android.exchange : 8608
                com.google.android.apps.maps:FriendService : 8609
                com.android.deskclock : 8610
                com.android.launcher : 8611
                com.android.inputmethod.latin : 8612
                com.android.phone : 8613
                com.android.mms : 8614
                android.process.media : 8615
                com.isec.ssltest : 8616
                com.android.settings : 8617
                system_process : 8618
                android.process.acore : 8619

    * Attach to client:
        
        ads>> a 8616

    * Load plugins:

        ads>> lp plugins
	attempting to load plugins from: plugins .... 
	loaded Java plugins: 
		com.isecpartners.android.jdwp.plugin.SSLBypassJDIPlugin
		com.isecpartners.android.jdwp.plugin.JythonConsoleJDIPlugin
		com.isecpartners.android.jdwp.plugin.TraceMethodsJDIPlugin
		com.isecpartners.android.jdwp.plugin.TestJDIPlugin
	loaded Jython plugins: 
		TestJythonPlugin

    * Initialize plugin:
    
    	ads>> ip com.isecpartners.android.jdwp.plugin.SSLBypassJDIPlugin
	
	ads>> ip TestJythonPlugin


After the plugin has been successfully initialized, do the action in the app that causes an SSL connection to be made. Breakpoints should be hit and handled via the initialized plugins.

The TestJythonPlugin is a good example of how to write a custom plugin using Jython
SSLBypassJDIPlugin, AndroidSSLBypassHelperApp, SSLTestApp, twistedsslserver.py
==================

This is the debugger plugin that implements bypassing SSL checks. This will only work for apps which implement 
certificate pinning or SSL in a particular way. It is left as an exercise for the user to implement a better
plugin which will bypass *all* methods of doing SSL in Android. This plugin depends on loading source from an 
external location into the app process space. Therefore, this plugin requires that a helper app also be installed:
AndroidSSLBypassHelperApp. This plugin can be tested with the corresponding test app SSLTestApp.

twistdsslserver.py is used in correspondence with the SSLTestApp - twisted (http://twistedmatrix.com/trac/) must be installed to run

Known Issues
==================

The way that the SSL bypass plugin works is to load code from a location external to the app via reflection and
the use of DexClassLoader (http://developer.android.com/reference/dalvik/system/DexClassLoader.html). The use of
reflection causes a strange issue that has not yet been resolved. The debugger will throw and exception when
an object created from such a reflected class is being set as the value of a local variable. 

The basic mantra for this tool: If it does not work the first time, try stopping the debugger and running again
without closing the app.


Building
==================

* Modify the properties in build.properties corresponding to locations of ddmlib and tools.jar on your filesystem

    * ddmlib is found in the Android SDK at: android-sdk\tools\lib\ddmlib.jar

    * tools.jar is found in the JDK libs, for example: C:/Program Files (x86)/Java/jdk1.7.0_05/lib/tools.jar

* Run the ant build file: build.xml

Testing
===================

    * Run tests using the emulator

    * Install AndroidSSLBypassHelperApp

    * Install SSLTestApp

    * Setup proxy

    * Right now SSLTestApp points to the host as seen from the emulator (10.0.2.2)

        * This is hardcoded for now, a better test app will be included in the future
        
    * Follow the basic usage instructions for running SSLBypassJDIPlugin

Writing new plugins
===================

*TODO

FAQ
==================

Q: Why is it so slow the first time I run the plugin?

A: This is due to caching that happens during the first run of the debugger. This is a known issue for 
which a solution is being worked on. 

Q: Why does the SSLBYpassJDIPlugin not work for my app?

A: Because it has not been implemented yet! Check out the info on writing your own plugin to bypass SSL for your particular app.

Q: Why do I get a ClassCastException when running the SSLBypassJDIPlugin?

A: This is the result of the way the plugin uses reflection to obtain a new TrustManager and is a known issue. 
    
    The workaround:
        
        * The first time plugin is run on the app the exception is hit
        
        * Stop the debugger but do not close the app
            
            * If the app hangs after the debugger is closed check the following:
               
                * Do you have internet acces and proxy running?

                * If testing with included test app do you have the test server and proxy running?

        * Start the debugger again and run the plugin - this time no exception should be thrown
