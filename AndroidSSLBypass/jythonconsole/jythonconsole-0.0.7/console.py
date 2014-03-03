"""
Jython Console with Code Completion

This uses the basic Jython Interactive Interpreter.
The UI uses code from Carlos Quiroz's 'Jython Interpreter for JEdit' http://www.jedit.org
"""

from javax.swing import JFrame, JScrollPane, JTextPane, Action, KeyStroke, WindowConstants
from javax.swing.text import JTextComponent, TextAction, SimpleAttributeSet, StyleConstants
from java.awt import Color, Font, Point
from java.awt.event import  InputEvent, KeyEvent, WindowAdapter
from java.lang import System
from java.awt import Toolkit
from java.awt.datatransfer import DataFlavor

import jintrospect
from popup import Popup
from tip import Tip
from history import History

import sys
from code import InteractiveInterpreter
from org.python.util import InteractiveConsole

__author__ = "Don Coleman <dcoleman@chariotsolutions.com>"

import re
# allows multiple imports like "from java.lang import String, Properties"
_re_from_import = re.compile("from\s+\S+\s+import(\s+\S+,\s?)?")

try:
    True, False
except NameError:
    (True, False) = (1, 0)

class Console:
    PROMPT = sys.ps1
    PROCESS = sys.ps2
    BANNER = ["Jython Completion Shell", InteractiveConsole.getDefaultBanner()]
  
    include_single_underscore_methods = False
    include_double_underscore_methods = False

    def __init__(self, namespace=None):
        """
            Create a Jython Console.
            namespace is an optional and should be a dictionary or Map
        """
        self.history = History(self)

        if namespace != None:
            self.locals = namespace
        else:
            self.locals = {}

        self.buffer = [] # buffer for multi-line commands                    

        self.interp = Interpreter(self, self.locals)
        sys.stdout = StdOutRedirector(self)

        self.text_pane = JTextPane(keyTyped = self.keyTyped, keyPressed = self.keyPressed)
        self.__initKeyMap()

        self.doc = self.text_pane.document
        self.__propertiesChanged()
        self.__inittext()
        self.initialLocation = self.doc.createPosition(self.doc.length-1)

        # Don't pass frame to popups. JWindows with null owners are not focusable
        # this fixes the focus problem on Win32, but make the mouse problem worse
        self.popup = Popup(None, self.text_pane)
        self.tip = Tip(None)

        # get fontmetrics info so we can position the popup
        metrics = self.text_pane.getFontMetrics(self.text_pane.getFont())
        self.dotWidth = metrics.charWidth('.')
        self.textHeight = metrics.getHeight()

        # add some handles to our objects
        self.locals['console'] = self

    def insertText(self, text):
        """insert text at the current caret position"""
        # seems like there should be a better way to do this....
        # might be better as a method on the text component?
        caretPosition = self.text_pane.getCaretPosition()
        self.text_pane.select(caretPosition, caretPosition)
        self.text_pane.replaceSelection(text)
        self.text_pane.setCaretPosition(caretPosition + len(text))

    def getText(self):
        """get text from last line of console"""
        offsets = self.__lastLine()
        text = self.doc.getText(offsets[0], offsets[1]-offsets[0])
        return text.rstrip()

    def getDisplayPoint(self):
        """Get the point where the popup window should be displayed"""
        screenPoint = self.text_pane.getLocationOnScreen()
        caretPoint = self.text_pane.caret.getMagicCaretPosition()

        # BUG: sometimes caretPoint is None
        # To duplicate type "java.aw" and hit '.' to complete selection while popup is visible

        x = screenPoint.getX() + caretPoint.getX() + self.dotWidth
        y = screenPoint.getY() + caretPoint.getY() + self.textHeight
        return Point(int(x),int(y))

    def hide(self, event=None):
        """Hide the popup or tip window if visible"""
        if self.popup.visible:
            self.popup.hide()
        if self.tip.visible:
            self.tip.hide()

    def hideTip(self, event=None):
        self.tip.hide()
        self.insertText(')')

    def showTip(self, event=None):
        # get the display point before writing text
        # otherwise magicCaretPosition is None
        displayPoint = self.getDisplayPoint()

        if self.popup.visible:
            self.popup.hide()
        
        line = self.getText()

        self.insertText('(')
        
        (name, argspec, tip) = jintrospect.getCallTipJava(line, self.locals)

        if tip:
            self.tip.showTip(tip, displayPoint)
            
    def showPopup(self, event=None):
        """show code completion popup"""

        try:
            line = self.getText()
            list = jintrospect.getAutoCompleteList(line, self.locals, includeSingle=self.include_single_underscore_methods, includeDouble=self.include_double_underscore_methods)
            if len(list) > 0:
                self.popup.showMethodCompletionList(list, self.getDisplayPoint())

        except Exception, e:
            print >> sys.stderr, "Error getting completion list: ", e
            #traceback.print_exc(file=sys.stderr)

    def inLastLine(self, include = 1):
        """ Determines whether the cursor is in the last line """
        limits = self.__lastLine()
        caret = self.text_pane.caretPosition
        if self.text_pane.selectedText:
            caret = self.text_pane.selectionStart
        if include:
            return (caret >= limits[0] and caret <= limits[1])
        else:
            return (caret > limits[0] and caret <= limits[1])

    def enter(self, event=None):
        """ Triggered when enter is pressed """
        text = self.getText()
        self.buffer.append(text)
        source = "\n".join(self.buffer)
        more = self.interp.runsource(source)
        if more:
            self.printOnProcess()
        else:
            self.resetbuffer()
            self.printPrompt()
        self.history.append(text)

        self.hide()

    def quit(self, event=None):
        print "quitting"
        plugin = self.locals["plugin"]
        ev = self.locals["event"]
        plugin.consoleQuit(ev);        
        sys.exit()

    def resetbuffer(self):
        self.buffer = []

    def home(self, event):
        """ Triggered when HOME is pressed """
        if self.inLastLine():
            # go to end of PROMPT
            self.text_pane.caretPosition = self.__lastLine()[0]
        else:
            lines = self.doc.rootElements[0].elementCount
            for i in xrange(0,lines-1):
                offsets = (self.doc.rootElements[0].getElement(i).startOffset, \
                    self.doc.rootElements[0].getElement(i).endOffset)
                line = self.doc.getText(offsets[0], offsets[1]-offsets[0])
                if self.text_pane.caretPosition >= offsets[0] and \
                    self.text_pane.caretPosition <= offsets[1]:
                    if line.startswith(Console.PROMPT) or line.startswith(Console.PROCESS):
                        self.text_pane.caretPosition = offsets[0] + len(Console.PROMPT)
                    else:
                        self.text_pane.caretPosition = offsets[0]

    def end(self, event):
        if self.inLastLine():
            self.text_pane.caretPosition = self.__lastLine()[1] - 1

    # TODO look using text_pane replace selection like self.insertText
    def replaceRow(self, text):
        """ Replaces the last line of the textarea with text """
        offset = self.__lastLine()
        last = self.doc.getText(offset[0], offset[1]-offset[0])
        if last != "\n":
            self.doc.remove(offset[0], offset[1]-offset[0]-1)
        self.__addOutput(self.infoColor, text)
             
    def delete(self, event):
        """ Intercepts delete events only allowing it to work in the last line """
        if self.inLastLine():
            if self.text_pane.selectedText:
                self.doc.remove(self.text_pane.selectionStart, self.text_pane.selectionEnd - self.text_pane.selectionStart)
            elif self.text_pane.caretPosition < self.doc.length:
                self.doc.remove(self.text_pane.caretPosition, 1)

    def backSpaceListener(self, event=None):
        """ Don't allow backspace or left arrow to go over prompt """
        onFirstPosition = self.text_pane.getCaretPosition() <= self.__lastLine()[0]
        if onFirstPosition and not self.text_pane.selectedText:
            event.consume()
                                       
    def spaceTyped(self, event=None):
        """check we we should complete on the space key"""
        matches = _re_from_import.match(self.getText())
        if matches:
            self.showPopup()

    def killToEndLine(self, event=None):
        if self.inLastLine():
            caretPosition = self.text_pane.getCaretPosition()
            self.text_pane.setSelectionStart(caretPosition)
            self.text_pane.setSelectionEnd(self.__lastLine()[1] - 1)
            self.text_pane.cut()

    def paste(self, event=None):
        # if getText was smarter, this method would be unnecessary
        if self.inLastLine():
            clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
            clipboard.getContents(self.text_pane)
            contents = clipboard.getData(DataFlavor.stringFlavor)

            lines = contents.split("\n")
            for line in lines:
                self.insertText(line)
                if len(lines) > 1:
                    self.enter()

    def keyTyped(self, event):
        #print >> sys.stderr, "keyTyped", event.getKeyCode()
        if not self.inLastLine():
            event.consume()

    def keyPressed(self, event):
        if self.popup.visible:
            self.popup.key(event)
        #print >> sys.stderr, "keyPressed", event.getKeyCode()
        if event.keyCode == KeyEvent.VK_BACK_SPACE or event.keyCode == KeyEvent.VK_LEFT:
            self.backSpaceListener(event)

    def keyboardInterrupt(self, event=None):
        """ Raises a KeyboardInterrupt"""
        self.hide()
        self.interp.runsource("raise KeyboardInterrupt\n")
        self.resetbuffer()
        self.printPrompt()
                
    # TODO refactor me
    def write(self, text):
        self.__addOutput(self.infoColor, text)

    def printResult(self, msg):
        """ Prints the results of an operation """
        self.__addOutput(self.text_pane.foreground, "\n" + str(msg))

    def printError(self, msg): 
        self.__addOutput(self.errorColor, "\n" + str(msg))

    def printOnProcess(self):
        """ Prints the process symbol """
        self.__addOutput(self.infoColor, "\n" + Console.PROCESS)

    def printPrompt(self):
        """ Prints the prompt """
        self.__addOutput(self.infoColor, "\n" + Console.PROMPT)
        
    def __addOutput(self, color, msg):
        """ Adds the output to the text area using a given color """
        from javax.swing.text import BadLocationException
        style = SimpleAttributeSet()

        if color:
            style.addAttribute(StyleConstants.Foreground, color)

        self.doc.insertString(self.doc.length, msg, style)
        self.text_pane.caretPosition = self.doc.length

    def __propertiesChanged(self):
        """ Detects when the properties have changed """
        self.text_pane.background = Color.white #jEdit.getColorProperty("jython.bgColor")
        self.text_pane.foreground = Color.blue #jEdit.getColorProperty("jython.resultColor")
        self.infoColor = Color.black #jEdit.getColorProperty("jython.textColor")
        self.errorColor = Color.red # jEdit.getColorProperty("jython.errorColor")

        family = "Monospaced" # jEdit.getProperty("jython.font", "Monospaced")
        size = 14 #jEdit.getIntegerProperty("jython.fontsize", 14)
        style = Font.PLAIN #jEdit.getIntegerProperty("jython.fontstyle", Font.PLAIN)
        self.text_pane.setFont(Font(family,style,size))

    def __inittext(self):
        """ Inserts the initial text with the jython banner """
        self.doc.remove(0, self.doc.length)
        for line in "\n".join(Console.BANNER):
            self.__addOutput(self.infoColor, line)
        self.printPrompt()
        self.text_pane.requestFocus()

    def __initKeyMap(self):
        os_name = System.getProperty("os.name")
        if os_name.startswith("Win"):
            exit_key = KeyEvent.VK_Z
            interrupt_key = KeyEvent.VK_PAUSE # BREAK
        else:
            exit_key = KeyEvent.VK_D
            interrupt_key = KeyEvent.VK_C

        keyBindings = [
            (KeyEvent.VK_ENTER, 0, "jython.enter", self.enter),
            (KeyEvent.VK_DELETE, 0, "jython.delete", self.delete),
            (KeyEvent.VK_HOME, 0, "jython.home", self.home),
            (KeyEvent.VK_LEFT, InputEvent.META_DOWN_MASK, "jython.home", self.home),
            (KeyEvent.VK_UP, 0, "jython.up", self.history.historyUp),
            (KeyEvent.VK_DOWN, 0, "jython.down", self.history.historyDown),
            (KeyEvent.VK_PERIOD, 0, "jython.showPopup", self.showPopup),
            (KeyEvent.VK_ESCAPE, 0, "jython.hide", self.hide),

            ('(', 0, "jython.showTip", self.showTip),
            (')', 0, "jython.hideTip", self.hideTip),
            (exit_key, InputEvent.CTRL_MASK, "jython.exit", self.quit),
            (KeyEvent.VK_SPACE, InputEvent.CTRL_MASK, "jython.showPopup", self.showPopup),
            (KeyEvent.VK_SPACE, 0, "jython.space", self.spaceTyped),

            # explicitly set paste since we're overriding functionality
            (KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), "jython.paste", self.paste),

            # Mac/Emacs keystrokes
            (KeyEvent.VK_A, InputEvent.CTRL_MASK, "jython.home", self.home),
            (KeyEvent.VK_E, InputEvent.CTRL_MASK, "jython.end", self.end),
            (KeyEvent.VK_K, InputEvent.CTRL_MASK, "jython.killToEndLine", self.killToEndLine),
            (KeyEvent.VK_Y, InputEvent.CTRL_MASK, "jython.paste", self.paste),
            
            (interrupt_key, InputEvent.CTRL_MASK, "jython.keyboardInterrupt", self.keyboardInterrupt),
            ]

        keymap = JTextComponent.addKeymap("jython", self.text_pane.keymap)
        for (key, modifier, name, function) in keyBindings:
            keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(key, modifier), ActionDelegator(name, function))
        self.text_pane.keymap = keymap
        
    def __lastLine(self):
        """ Returns the char offests of the last line """
        lines = self.doc.rootElements[0].elementCount
        offsets = (self.doc.rootElements[0].getElement(lines-1).startOffset, \
                   self.doc.rootElements[0].getElement(lines-1).endOffset)
        line = self.doc.getText(offsets[0], offsets[1]-offsets[0])
        if len(line) >= 4 and (line[0:4]==Console.PROMPT or line[0:4]==Console.PROCESS):
            return (offsets[0] + len(Console.PROMPT), offsets[1])
        return offsets


class ActionDelegator(TextAction):
    """
        Class action delegator encapsulates a TextAction delegating the action
        event to a simple function
    """
    def __init__(self, name, delegate):
        TextAction.__init__(self, name)
        self.delegate = delegate

    def actionPerformed(self, event):
        if isinstance(self.delegate, Action):
            self.delegate.actionPerformed(event)
        else:
            self.delegate(event)

class Interpreter(InteractiveInterpreter):
    def __init__(self, console, locals):
        InteractiveInterpreter.__init__(self, locals)
        self.console = console
        
    def write(self, data):
        # send all output to the textpane
        # KLUDGE remove trailing linefeed
        self.console.printError(data[:-1])
        
# redirect stdout to the textpane
class StdOutRedirector:
    def __init__(self, console):
        self.console = console
        
    def write(self, data):
        #print >> sys.stderr, ">>%s<<" % data
        if data != '\n':
            # This is a sucky hack.  Fix printResult
            self.console.printResult(data)

class JythonFrame(JFrame):
    def __init__(self):
        self.title = "Jython - test"
        self.size = (600, 400)      
        try:
            #JUSTINE
            self.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE)
        except:
            # assume jdk < 1.4
            self.addWindowListener(KillListener())
            #JUSTINE
            self.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE)

class KillListener(WindowAdapter):
    """
    Handle EXIT_ON_CLOSE for jdk < 1.4
    Thanks to James Richards for this method
    """
    def windowClosed(self, evt):
        import java.lang.System as System
        System.exit(0)
        
def main(namespace=None):
    frame = JythonFrame()
    console = Console(namespace)
    frame.getContentPane().add(JScrollPane(console.text_pane))
    frame.visible = True
    frame.windowClosing = lambda x: console.locals["plugin"].consoleQuit(console.locals["event"])

if __name__ == "__main__":
    main()
    