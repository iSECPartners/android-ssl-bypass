from com.isecpartners.android.jdwp.pluginservice import AbstractJythonConsolePlugin
from com.isecpartners.android.jdwp import DalvikUtils
import com.sun.jdi.event.Event

class TestJythonConsolePlugin(AbstractJythonConsolePlugin):

    def __init__(self):
        AbstractJythonConsolePlugin.__init__(self,"TestJythonConsolePlugin")
        self.output("Python: initalized TestJythonPlugin")

    def setupEvents(self):
        self.output("Python: setupEvents for android.util.Log.i,d,v,e,w")
        self.createBreakpointRequest("android.util.Log.i")
        self.createBreakpointRequest("android.util.Log.d")
    	self.createBreakpointRequest("android.util.Log.v")
    	self.createBreakpointRequest("android.util.Log.e")
    	self.createBreakpointRequest("android.util.Log.w")

