The code in this directory contains the core of the debugger. 
These classes implement command and control between the debugger 
and the corresponding remote virtual machine.

* All threads communicate via messages retrieved from a 
  java.util.concurrent.BLockingQueue. 
	
* The Control class is responsible for attaching to the VirtualMachine class 
  and creating a VirtualMachineSession thread. 
	
* Once a connection message is received by a VirtualMachineSession, it creates 
  a JDIPluginService and a VirtualMachineEventManager:

 * The VirtualMachineEventManager class is responsible for dispatching events 
   received in the virtual machine's event queue.

	
 * JDIPluginService finds classes implementing the JDIPlugin interface on the 
   provided path.

 * Plugins can override their setupEvents method to request events 
   (BreakpointRequest, StepRequest, etc.) to occur in the attached VM
	
 * When a plugin requests an event it is automatically registered as the 
   handler for that event
	
  * Currently last one wins
				
NOTE: The plugin architecture is fairly rudimentary and certain important things such as cleanup are not yet implemented.

## KNOWN ISSUES

	
