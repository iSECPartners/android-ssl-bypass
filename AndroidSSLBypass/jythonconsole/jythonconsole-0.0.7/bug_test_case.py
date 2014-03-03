import unittest
import introspect
import jintrospect

class BugTestCase(unittest.TestCase):

    def testPythonStackOverflow(self):
        """
        jythonconsole-0.0.1 has a stack overflow when autocomplete with Jython-2.2b1
        
        import sys
        sys. <-- autocomplete starts and then the following stacktrace
        
        Traceback (most recent call last):
          File "bug_test_case.py", line 8, in testJython22b1Bug
            list = jintrospect.getAutoCompleteList("sys.", locals())
          File "/Users/don/jythonconsole/jintrospect.py", line 90, in getAutoCompleteList
            attributes = getAttributeNames(object, includeMagic, includeSingle, includeDouble)
          File "/Users/don/jythonconsole/introspect.py", line 61, in getAttributeNames
            attrdict = getAllAttributeNames(object)
          File "/Users/don/jythonconsole/introspect.py", line 125, in getAllAttributeNames
            attrdict.update(getAllAttributeNames(klass))
            ...
          File "/Users/don/jythonconsole/introspect.py", line 138, in getAllAttributeNames
            attrdict.update(getAllAttributeNames(base))
          File "/Users/don/jythonconsole/introspect.py", line 125, in getAllAttributeNames
            attrdict.update(getAllAttributeNames(klass))
          File "/Users/don/jythonconsole/introspect.py", line 138, in getAllAttributeNames
            attrdict.update(getAllAttributeNames(base))
          File "/Users/don/jythonconsole/introspect.py", line 125, in getAllAttributeNames
            attrdict.update(getAllAttributeNames(klass))
          File "/Users/don/jythonconsole/introspect.py", line 101, in getAllAttributeNames
            wakeupcall = dir(object)
        java.lang.StackOverflowError: java.lang.StackOverflowError        
        """
        dict = introspect.getAllAttributeNames("sys")
        # if the bug is happening you'll never get here
        # you'll get a stack overflow instead    
        self.assert_(len(dict) > 0)

    # method completion for python strings was failing in 0.0.2 with python2.b1
    def testAutoCompleteString(self):
        f = "foo"
        list = jintrospect.getAutoCompleteList("f", locals())
        self.assert_(len(list) > 0)
        self.assert_(list.index("startswith") > 0)

if __name__ == '__main__':
    unittest.main()
