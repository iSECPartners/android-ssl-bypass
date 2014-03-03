import os
from com.isecpartners.android.jdwp.pluginservice import AbstractJDIPlugin
from com.isecpartners.android.jdwp import DalvikUtils
import com.sun.jdi.event.Event
from java.lang import IllegalArgumentException
from com.sun.jdi import AbsentInformationException
from com.isecpartners.android.jdwp import LocationNotFoundException
from com.sun.jdi import ObjectReference
import sys
sys.path.append(r'/Users/justine/tools/android-sdk-macosx/platforms/android-19/android.jar')
from android.content import Intent

class TracerPlugin(AbstractJDIPlugin):

    def __init__(self):
        AbstractJDIPlugin.__init__(self,"TracerPlugin")
        self.output("Python: initalized TestJythonPlugin")

    def setupEvents(self):
        self.output("Python: setupEvents")
        #fpath = self.properties.getProperty("filters_path")
        fd = open(r"%s/filters" % self.getBasePath())
        lines = fd.readlines()
        msg_success = "SUCCESSS:\n"
        msg_fail = "FAILURES:\n"
        for l in lines:
            l = l.strip()
            #method_names = [r".sendBroadcast(android.content.Intent)"]
            #for m in method_names:
                #   meth = "%s%s" % (l ,m)
                #  self.output(meth)
            try:
                #self.createBreakpointRequest(l)
                self.createMethodEntryRequest(l)
                msg_success +=  ("%s\n" % l)
            except LocationNotFoundException:
                msg_fail += ("Location not found: %s\n" % l)
            #self.createMethodEntryRequest(l)
            #self.createMethodExitRequest(l)
            self.output("%s%s" % (msg_success, msg_fail))

    def handleEvent(self, event):
        vm = event.virtualMachine();
        thread = event.thread()
        frames = thread.frames()
        fr0 = frames[0]
        location = fr0.location()
        method = location.method()
        name = method.name() 
        dalvikUtils = DalvikUtils(vm,thread)

        msg = "%s\n" % ("="*20)	
        msg += "EVENT: \n\t%s --- %s\n\n" % ( event.toString(), name)
        msg += "FRAME COUNT: %d" % len(frames)
        
        count = 0
        for f in frames:
            if(count <= 2):
                count += 1
                currObj = f.thisObject()            
                if currObj != None:
                    msg += "\tFRAME OBJECT: %s\n" % currObj.toString()
        msg += "="*20
        self.output(msg)
        self.resumeEventSet()