Rudimentary modular architecture based on: 

http://solitarygeek.com/java/a-simple-pluggable-java-applicationly

Classes in the "plugins" dir are automatically loaded at runtime and their 
setupEvents() method is called. When an event which they registered for is 
hit, the handleEvent method is called.

* Hopefully people will write other plugins based on this template

* Goal is to move to Jython or something quicker than Java
