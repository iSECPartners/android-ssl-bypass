import unittest
import jintrospect

class Issue17TestCase(unittest.TestCase):
    """http://code.google.com/p/jythonconsole/issues/detail?id=17"""
    
    def setUp(self):
        pass
                
    def testStaticCompletion(self):
        from java.util import Calendar
        list = jintrospect.getAutoCompleteList("Calendar", locals())
        self.assert_(len(list) > 0)
        self.assert_(list.index("getInstance") > -1)
        
    def testStaticCallTip(self):
        # Call Tip was failing because Calendar.getInstance 
        # returns PyReflectedMethod which was not being handled properly
        from java.util import Calendar
        command = "Calendar.getInstance";
        tip = jintrospect.getCallTipJava(command, locals())
        # tip should be something like 
        #   getInstance(java.util.TimeZone, java.util.Locale) -> java.util.Calendar
        #   getInstance(java.util.TimeZone) -> java.util.Calendar
        #   getInstance(java.util.Locale) -> java.util.Calendar
        #   getInstance() -> java.util.Calendar'
        self.assert_(tip[2] != '')
        self.assert_(tip[2].index("getInstance") > -1)
        self.assert_(tip[2].index("TimeZone") > -1)
      
    def testIsPython(self):        
        from java.util import Calendar        
        self.failIf(jintrospect.ispython(Calendar.getInstance), "Calendar.getInstance is not Python")
      
if __name__ == '__main__':
    unittest.main()
