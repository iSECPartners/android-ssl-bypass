import unittest
import tempfile
from  history import History

try:
    True, False
except NameError:
    (True, False) = (1, 0)

class MockConsole:
    def replaceRow(self, text):
        self.text = text

    def inLastLine(self):
        return True

class HistoryTestCase(unittest.TestCase):

    def setUp(self):
        self.console = MockConsole()

    def tearDown(self):
        self.console = None

    def testHistoryUp(self):
        h = History(self.console, tempfile.mktemp())
        h.append("one")
        h.append("two")
        h.append("three")

        h.historyUp()
        self.assertEquals("three", self.console.text)

        h.historyUp()
        self.assertEquals("two", self.console.text)

        h.historyUp()
        self.assertEquals("one", self.console.text)

        # history doesn't roll, just stops at the last item
        h.historyUp()
        self.assertEquals("one", self.console.text)

    def testHistoryDown(self):
        h = History(self.console,tempfile.mktemp())
        h.append("one")
        h.append("two")
        h.append("three")

        h.historyUp()
        h.historyUp()
        
        h.historyUp()
        self.assertEquals("one", self.console.text)

        h.historyDown()
        self.assertEquals("two", self.console.text)

        h.historyDown()
        self.assertEquals("three", self.console.text)

        h.historyDown()
        self.assertEquals("", self.console.text)

        # History doesn't wrap
        h.historyDown()
        self.assertEquals("", self.console.text)

    def testSkipDuplicates(self):
       h = History(self.console, tempfile.mktemp())
       h.append("one")
       h.append("one")
       h.append("two")
       h.append("two")
       h.append("three")
       h.append("three")

       h.historyUp()
       self.assertEquals("three", self.console.text)

       h.historyUp()
       self.assertEquals("two", self.console.text)

       h.historyUp()
       self.assertEquals("one", self.console.text)

    def testSkipEmpty(self):
        h = History(self.console, tempfile.mktemp())
        size = len(h.history)
        h.append("")
        self.assert_(len(h.history) == size)

        h.append("\n")
        self.assert_(len(h.history) == size)

        h.append(None)
        self.assert_(len(h.history) == size)

    def testLoadHistory(self):
        file = tempfile.mktemp()
        f = open(file, "w")
        f.write("red\n")
        f.write("yellow\n")
        f.write("blue\n")
        f.flush()
        f.close()

        h = History(self.console, file)
        h.historyUp()
        self.assertEquals("blue", self.console.text)

        h.historyUp()
        self.assertEquals("yellow", self.console.text)

        h.historyUp()
        self.assertEquals("red", self.console.text)

    def testSaveHistory(self):
        file = tempfile.mktemp()

        h = History(self.console, file)
        h.append("a")
        h.append("b")
        h.append("c")
        h.saveHistory()

        f = open(file)
        self.assertEquals("a", f.readline()[:-1])
        self.assertEquals("b", f.readline()[:-1])
        self.assertEquals("c", f.readline()[:-1])

if __name__ == '__main__':
    unittest.main()
