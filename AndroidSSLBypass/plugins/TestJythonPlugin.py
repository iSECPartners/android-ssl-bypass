from com.isecpartners.android.jdwp.pluginservice import AbstractJDIPlugin
import com.sun.jdi.event.Event

class TestJythonPlugin(AbstractJDIPlugin):

    def __init__(self):
        AbstractJDIPlugin.__init__(self,"TestJythonPlugin")
        self.output("Python: initalized TestJythonPlugin")

    def setupEvents(self):
        self.output("Python: setupEvents")
        self.createBreakpointRequest("javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier")
    
    def handleEvent(self, event):
        self.output(event.toString())
        self.resumeEventSet()