from com.isecpartners.android.jdwp.pluginservice import AbstractJDIPlugin
from com.isecpartners.android.jdwp import DalvikUtils
import com.sun.jdi.event.Event

class TestJythonPlugin(AbstractJDIPlugin):

    def __init__(self):
        AbstractJDIPlugin.__init__(self,"TestJythonPlugin")
        self.output("Python: initalized TestJythonPlugin")

    def setupEvents(self):
        self.output("Python: setupEvents")
        self.createBreakpointRequest("android.util.Log.i")
	self.createBreakpointRequest("android.util.Log.d")
 	self.createBreakpointRequest("android.util.Log.v")
 	self.createBreakpointRequest("android.util.Log.e")
     	self.createBreakpointRequest("android.util.Log.w")
    
    def handleEvent(self, event):
	# http://docs.oracle.com/javase/1.5.0/docs/guide/jpda/jdi/com/sun/jdi/event/BreakpointEvent.html
	vm = event.virtualMachine();
	thread = event.thread()
	fr0 = thread.frames()[0]
	location = fr0.location()
	method = location.method()
	name = method.name() 
	dalvikUtils = DalvikUtils(vm,thread)
	args = method.variables()

	
	self.output("event: %s, args: %s" % ( event.toString(), args.toString()))
        self.resumeEventSet()
