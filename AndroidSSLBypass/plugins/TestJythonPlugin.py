from com.isecpartners.android.jdwp.pluginservice import AbstractJDIPlugin
import com.sun.jdi.event.Event

class TestJythonPlugin(AbstractJDIPlugin):

    def __init__(self):
        AbstractJDIPlugin.__init__(self,"TestJythonPlugin")
        print "initalized TestJythonPlugin\n"

    def setupEvents(self):
        print "setupEvents\n"
    
    def handleEvent(self, event):
	   print event + "\n"