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

    * ADB location can also be set in the defaults.prop file in the root dir

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

* Modify the properties in build.properties corresponding to locations of ddmlib 

    * ddmlib is found in the Android SDK at: android-sdk\tools\lib\ddmlib.jar

* Ensure that tools.jar can be found by setting JAVA_HOME to point to the JDK root directory

    * tools.jar is found in the JDK lib dir, for example: C:/Program Files (x86)/Java/jdk1.7.0_05/lib/tools.jar

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


Custom Jython Plugin Overview
=====================

This tool is rather poorly named "android-ssl-bypass" in that bypassing SSL is far from all it does. It was initally presented at conference where bypassing ssl was its main purpose. However, it was created to be an extensible debugging tool that can be used for a variety of debugging tasks. It might be more aptly named something like "android-debug-shell", and probably will be changed to that at some point. This aims to provide a basic guide for creating your own simple debugging plugins for the tool using Jython. The plugins can be written in Java as well, but Jython is the easiest method for extensibility.

The power of Jython is that it lets us easily use and Java classes in the classpath. So we can import the Java Class AbstractJDIPlugin and create a Python class which extends it in order to create a plugin that can be loaded by the tool. In the future when the APIs are more solid there will be real documentation, but for now there is only source code :). Some jargon:

* Event (http://docs.oracle.com/javase/1.5.0/docs/guide/jpda/jdi/com/sun/jdi/event/Event.html)
    
    * Interface which represents an event request to the virtual machine. An event can be one of the following:
    AccessWatchpointEvent, BreakpointEvent, ClassPrepareEvent, ClassUnloadEvent, ExceptionEvent, LocatableEvent, MethodEntryEvent, MethodExitEvent, ModificationWatchpointEvent, StepEvent, ThreadDeathEvent, ThreadStartEvent, VMDeathEvent, VMDisconnectEvent, VMStartEvent, WatchpointEvent

* EventRequest (http://docs.oracle.com/javase/1.5.0/docs/guide/jpda/jdi/com/sun/jdi/request/EventRequest.html)

    * Respresents a request for notification for an event


So first we import some Java classes from the tool that we will need to use:

    from com.isecpartners.android.jdwp.pluginservice import AbstractJDIPlugin
    from com.isecpartners.android.jdwp import DalvikUtils
    import com.sun.jdi.event.Event

We can now create a class which extends the AbstractJDIPlugin using the following:

    class TestJythonPlugin(AbstractJDIPlugin):

        def __init__(self):
            AbstractJDIPlugin.__init__(self,"TestJythonPlugin")
            self.output("Python: initalized TestJythonPlugin")

A plugin must implement the abstract methods setupEvents() and handleEvent(Event). In the setupEvents method, the plugins creates and registers EventRequest objects using the convience methods provided by the Abstract JDIPlugin API, such as createBreakpointRequest(String locationString). 

    def setupEvents(self):
        self.output("Python: setupEvents")
        self.createBreakpointRequest("android.util.Log.i")
        self.createBreakpointRequest("android.util.Log.d")
        self.createBreakpointRequest("android.util.Log.v")
        self.createBreakpointRequest("android.util.Log.e")
        self.createBreakpointRequest("android.util.Log.w")

Now in the handleEvents(Event e) method we handle the events we just registered for. The tool works in a last in wins manner as far as two plugins trying to register for the same event (overall this feature is still in heavy dev so prepare for some bugs here when trying to load multiple plugins at once). Now we can do stuff with the Event object which is received by the function. In this case we know we are dealing with a BreakpointEvent (http://docs.oracle.com/javase/1.5.0/docs/guide/jpda/jdi/com/sun/jdi/event/BreakpointEvent.html) because that is all we registered. We can use the methods of the event to extract the current thread, frame, location, and method. From that we can obtain the local variables and much more.

    def handleEvent(self, event):
        vm = event.virtualMachine();
        thread = event.thread()
        fr0 = thread.frames()[0]
        location = fr0.location()
        method = location.method()
        name = method.name() 
        dalvikUtils = DalvikUtils(vm,thread)
        args = method.variables()

        self.output("="*20) 
        self.output("EVENT: \n\t%s\n" % ( event.toString()))
        vals = []
        self.output("VARIABLES:\n")
        for arg in args:
            val = fr0.getValue(arg)
            self.output("\t%s = %s\n" % (arg,val))
            vals.append(val)

        self.output("="*20)

Then we can choose to resume the event set which causes all threads to resume and the application continues execution.

        self.resumeEventSet()   

The plugin can then be used by attaching to the process, loading the plugins from the directory in which the Jython plugin is located, and initalizing the plugin. The following is an example debugging session from the example plugin in plugins/TestJythonPlugin:

        ====================
         Welcome to ANDROID DEBUG SHELL
        Type ?list for list of commands
        ====================

        ads>> ld
        Devices:
                emulator-5554 : nexus7 [emulator-5554]

        ads>> sd emulator-5554
        Selected Device:
                emulator-5554
        ads>> apps
        Clients on: emulator-5554
                com.android.quicksearchbox : 8600
                com.android.browser : 8601
                com.android.exchange : 8602
                com.android.mms : 8603
                com.android.deskclock : 8604
                com.android.providers.calendar : 8605
                com.android.calendar : 8606
                com.android.contacts : 8607
                system_process : 8608
                com.android.email : 8609
                android.process.acore : 8610
                com.android.phone : 8611
                com.android.systemui : 8612
                com.android.launcher : 8613
                com.android.settings : 8614
                com.android.inputmethod.latin : 8615
                android.process.media : 8616

        ads>> a 8601
        successfully attached to localhost:8601
        ads>> lp plugins
        attempting to load plugins from: plugins ....
        loaded Java plugins:
                com.isecpartners.android.jdwp.plugin.SSLBypassJDIPlugin
                com.isecpartners.android.jdwp.plugin.JythonConsoleJDIPlugin
                com.isecpartners.android.jdwp.plugin.TraceMethodsJDIPlugin
                com.isecpartners.android.jdwp.plugin.TestJDIPlugin
        loaded Jython plugins:
                TestJythonPlugin

        ads>> ip TestJythonPlugin
        Python: setupEvents
        attempting to initalize plugin: TestJythonPlugin
        plugin initialized: TestJythonPlugin
        ads>> ====================
        EVENT:
                BreakpointEvent@android.util.Log:159 in thread <1> main

        VARIABLES:

                tag in android.util.Log.i(java.lang.String, java.lang.String)@android.util.Log:159 = "Choreographer"

                msg in android.util.Log.i(java.lang.String, java.lang.String)@android.util.Log:159 = "Skipped 103 frames!  The application may
        be doing too much work on its main thread."

        ====================
        ====================
        EVENT:
                BreakpointEvent@android.util.Log:159 in thread <1> main

        VARIABLES:

                tag in android.util.Log.i(java.lang.String, java.lang.String)@android.util.Log:159 = "Choreographer"

                msg in android.util.Log.i(java.lang.String, java.lang.String)@android.util.Log:159 = "Skipped 110 frames!  The application may
        be doing too much work on its main thread."


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
