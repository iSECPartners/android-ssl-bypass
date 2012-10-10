import unittest
import jintrospect
from java.lang import String

class JIntrospectTestCase(unittest.TestCase):

    def testGetAutoCompleteList(self):
        s = String("Unit Test")
        list = jintrospect.getAutoCompleteList("s", locals())
        self.assertNotEmpty(list)
        self.assertContains(list, "contains")

    def testGetCallTipJava(self):
        s = String("Unit Test")
        tip = jintrospect.getCallTipJava("s.contains", locals())
        self.assertEquals("contains(CharSequence) -> boolean", tip[2])

    def testGetPackageName(self):
        package_name = jintrospect.getPackageName("import java.")
        self.assertEquals("java", package_name)

        package_name = jintrospect.getPackageName("from java.awt import")
        self.assertEquals("java.awt", package_name)

    def testCompletePackageName(self):
        try:
            list = jintrospect.completePackageName("bogus")
            fail("Expecting import error.")
        except ImportError:
            pass

        list = jintrospect.completePackageName("java")
        self.assertNotEmpty(list)
        self.assertContains(list, "awt")

        list = jintrospect.completePackageName("java.util")
        self.assertNotEmpty(list)
        self.assertContains(list, "ArrayList")

    def testIsPython(self):
        s = String("Java String")
        self.assert_(not jintrospect.ispython(s))

        self.assert_(jintrospect.ispython(jintrospect))

    def testIsPython22(self):
        # NOTE: This will fail with AP 2.1.  Would it fail for old version too?             
        ps = "python string"
        self.assert_(jintrospect.ispython(ps))
        d = {}
        self.assert_(jintrospect.ispython(d))
        
    def testStaticJavaMethods(self):
        """ Instances of Java classes should not show static methods as completion choices. """
        static_methods = jintrospect.getAutoCompleteList("String", {"String" : String})
        self.assert_("valueOf" in static_methods, "'valueOf' missing from static method list")
        instance_methods = jintrospect.getAutoCompleteList("s", {"s" : String("Test")})
        self.assert_("valueOf" not in instance_methods, "'valueOf' should not be in the instance method list")

    def testJavaAccessorAsProperty(self):
        instance_methods = jintrospect.getAutoCompleteList("s", {"s" : String("Test")})
        self.assert_("class" in instance_methods, "'class' property should be in the instance method list")
        
    def testJavaLangRemoved(self):
        object_name, argspec, tip_text = jintrospect.getCallTip('String', {'String' : 's'})
        self.assertDoesNotContain(tip_text, "java.lang.")
        
    def testPrimitiveArrayConversion(self):
        """[B, [C and [I should be replaced"""        
        object_name, argspec, tip_text = jintrospect.getCallTipJava('String', { 'String' : String })
        self.assertDoesNotContain(tip_text, "[B")
        self.assertDoesNotContain(tip_text, "[C")
        self.assertDoesNotContain(tip_text, "[I")
        self.assertContains(tip_text, "byte[]")
        self.assertContains(tip_text, "char[]")
        self.assertContains(tip_text, "int[]")

    # http://code.google.com/p/jythonconsole/issues/detail?id=2
    # def testMultipleStatements(self):
    #     command = "import sys; sys"
    #     list = jintrospect.getAutoCompleteList(command, locals())
    #     self.assert_(len(list) > 0)

    # note: static methods and fields are tested in static_test_case

    def assertNotEmpty(self, list):
        if list == None:
            self.fail("list is None")
        if len(list) < 1:
            self.fail("list is empty")

    def assertContains(self, list, value):
        try:
            list.index(value)
        except ValueError:
            self.fail("%s does not contain %s" % (type(list).__name__, value))

    def assertDoesNotContain(self, list, value):
        try:
            list.index(value)
            self.fail("%s should contain %s" % (type(list).__name__, value))            
        except ValueError:
            self.assert_(True)
        

if __name__ == '__main__':
    unittest.main()
