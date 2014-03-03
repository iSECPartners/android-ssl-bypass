import sys
import unittest
import introspect

class IntrospectTestCase(unittest.TestCase):

    def setUp(self):
        try:
            sys.ps2
        except AttributeError:
            sys.ps2 = '... '

    def testUnboundMethod(self):
        import string
        method = 'string.index('
        (name, argspec, tip) = introspect.getCallTip(method, locals())

        self.assertEquals('index', name)
        self.assertEquals('s, *args', argspec)
        self.assertEquals('index(s, *args)\n\nindex(s, sub [,start [,end]]) -> int\n\nLike find but raises ValueError when the substring is not found.', tip)

    def testBuiltinFunction(self):
        """Builtin types don't work, like they do in PyCrust.  This is because they have a null __doc__ string in Jython."""
        method = 'len('
        
        (name, argspec, tip) = introspect.getCallTip(method, locals())
        
        self.assertEquals('len', name)
        
        if not sys.platform.startswith('java'):
            # next line worked with Python 2.2, fails with Python 2.3 and 2.5 on OS X, probably need newer introspect, inspect or dis
            # self.assertEquals('len(object) -> integer', argspec)
            self.assertEquals('', argspec)
            self.assertEquals('len(object) -> integer\n\nReturn the number of items of a sequence or mapping.', tip)


if __name__ == '__main__':
    unittest.main()
