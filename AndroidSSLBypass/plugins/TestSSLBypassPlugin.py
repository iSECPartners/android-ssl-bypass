from com.isecpartners.android.jdwp.pluginservice import AbstractSSLBypassPlugin
from com.isecpartners.android.jdwp import DalvikUtils
import com.sun.jdi.event.Event

class TestSSLBypassPlugin(AbstractSSLBypassPlugin):

    def __init__(self):
        AbstractSSLBypassPlugin.__init__(self,"TestSSLBypassPlugin")
        self.output("Python: initalized TestSSLBypassPlugin")

    def setupEvents(self):
	self.createBreakpointRequest(str(AbstractSSLBypassPlugin.HTTPS_URL_CONN_INIT));
	self.createBreakpointRequest(str(AbstractSSLBypassPlugin.SET_SSL_SOCKET_FACTORY));
	self.createBreakpointRequest(str(AbstractSSLBypassPlugin.SET_HOST_NAME_VERIFIER));
	self.createBreakpointRequest(str(AbstractSSLBypassPlugin.SET_DEFAULT_HOST_NAME_VERIFIER));
	self.createBreakpointRequest(str(AbstractSSLBypassPlugin.CREATE_CONNECTION));

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
        self.resumeEventSet()
