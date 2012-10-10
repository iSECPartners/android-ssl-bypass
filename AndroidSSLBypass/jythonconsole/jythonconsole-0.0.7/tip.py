from java.awt import Color, Dimension
from javax.swing import JWindow, JTextArea, JScrollPane

__author__ = "Don Coleman <dcoleman@chariotsolutions.com>"
__cvsid__ = "$Id: tip.py,v 1.3 2003/05/01 03:43:53 dcoleman Exp $"

class Tip(JWindow):
    """
    Window which provides the user with information about the method.
    For Python, this shows arguments, and the documention
    For Java, this shows the signature(s) and return type
    """
    MAX_HEIGHT = 300
    MAX_WIDTH = 400
    
    def __init__(self, frame):
        JWindow.__init__(self, frame)
        self.textarea = JTextArea()
        # TODO put this color with all the other colors
        self.textarea.setBackground(Color(225,255,255))
        self.textarea.setEditable(0)
        self.jscrollpane = JScrollPane(self.textarea)
        self.getContentPane().add(self.jscrollpane)

    def setText(self, tip):
        self.textarea.setText(tip)
        self.textarea.setCaretPosition(0)
        #print >> sys.stderr, self.textarea.getPreferredScrollableViewportSize()
        self.setSize(self.getPreferredSize())

    def getPreferredSize(self):
        # need to add a magic amount to the size to avoid scrollbars
        # I'm sure there's a better way to do this
        MAGIC = 20
        size = self.textarea.getPreferredScrollableViewportSize()
        height = size.height + MAGIC
        width = size.width + MAGIC
        if height > Tip.MAX_HEIGHT:
            height = Tip.MAX_HEIGHT
        if width > Tip.MAX_WIDTH:
            width = Tip.MAX_WIDTH
        return Dimension(width, height)

    def showTip(self, tip, displayPoint):
        self.setLocation(displayPoint)
        self.setText(tip)
        self.show()
