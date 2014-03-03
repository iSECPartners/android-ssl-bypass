===Feature Requests===
 
  	* Ability to unload plugins on demand

	* Multiple handler support for a single event (might be tricky)

	* Support for all VM events (http://docs.oracle.com/javase/1.5.0/docs/guide/jpda/jdi/com/sun/jdi/event/package-summary.html) - currently only four most used are supported

	* Support for interaction with the commandline via Jython plugins  
	
===Random TODOs===

	* Currently not calling tearDownEvents() always for plugins to remove event requests

	* Make sure everything is platform independent and configurable

	* Add documentation!!

	* Add licensing info and thirdparty libs documentation

	* Entire plugin service architecture should probably be reworked to something better and cleaner

	* Fix all the bugs :)
