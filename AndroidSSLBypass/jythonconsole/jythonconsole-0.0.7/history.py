"""
 history.py - Handles the History of the jython console
 Copyright (C) 2001 Carlos Quiroz

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
"""

from java.lang import System, Runtime
from java.lang import Runnable, Thread

class History(Runnable):
    """
    Command line history
    """
    
    default_history_file = System.getProperty("user.home") + '/.jythonconsole.history'
    MAX_SIZE = 200

    def __init__(self, console, history_file=default_history_file):
        Runtime.getRuntime().addShutdownHook(Thread(self))        

        self.history_file = history_file
        self.history = []
        self.loadHistory()            
          
        self.console = console
        self.index = len(self.history) - 1
        self.last = ""

    def append(self, line):
        if line == None or line == '\n' or len(line) == 0:
            return

        if line != self.last: # avoids duplicates
            self.last = line
            self.history.append(line)
            
        self.index = len(self.history) - 1

    def historyUp(self, event=None):
        if len(self.history) > 0 and self.console.inLastLine():
            self.console.replaceRow(self.history[self.index])
            self.index = max(self.index - 1, 0)

    def historyDown(self, event=None):
        if len(self.history) > 0 and self.console.inLastLine():
            if self.index == len(self.history) - 1:
                self.console.replaceRow("")
            else:
                self.index += 1
                self.console.replaceRow(self.history[self.index])

    def loadHistory(self):
        try:
            f = open(self.history_file)
            for line in f.readlines():
                self.history.append(line[:-1])
            f.close()
        except:
            pass
        
    def saveHistory(self):
        f = open(self.history_file, 'w')
        for item in self.history[-self.MAX_SIZE:]:
            f.write("%s\n" % item)
        f.flush()
        f.close()
        
    def run(self):
        self.saveHistory()
