import unittest
import introspect

class IntrospectTestCase(unittest.TestCase):

    def setUp(self):
        pass

    def testGetRoot(self):
        "figure out how getRoot behaves"
        import string
        root = introspect.getRoot("string.", ".")
        self.assertEquals("string", root)

        root = introspect.getRoot("string.join", "(")
        self.assertEquals("string.join", root)

if __name__ == '__main__':
    unittest.main()
