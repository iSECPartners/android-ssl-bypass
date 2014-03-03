"""Extend introspect.py for Java based Jython classes."""

from org.python.core import PyReflectedFunction
from java.lang import Class, Object
from java.lang.reflect import Modifier
from introspect import *
from sets import Set
import string
import re
import types

__author__ = "Don Coleman <dcoleman@chariotsolutions.com>"

_re_import_package = re.compile('import\s+(.+)\.') # import package
# TODO need to check for a trailing '.'  example: "from java import lang." don't autocomplete on trailing '.'
_re_from_package_import = re.compile('from\s+(\w+(?:\.\w+)*)\.?(?:\s*import\s*)?') # from package import class 

def completePackageName(target):
    """ Get a package object given the full name."""      
    targetComponents = target.split('.')
    base = targetComponents[0]
    baseModule = __import__(base, globals(), locals())
    module = baseModule    

    for component in targetComponents[1:]:
        module = getattr(module, component)

    list = dir(module)
    list.remove('__name__')
    list.append('*')
    return list
  
def getPackageName(command):    
        
    match = _re_import_package.match(command)
    if not match:
        #try the other re
        match = _re_from_package_import.match(command)
            
    return match.groups()[0]

def getAutoCompleteList(command='', locals=None, includeMagic=1, includeSingle=1, includeDouble=1):
    """
    Return list of auto-completion options for command.
    The list of options will be based on the locals namespace.
    """

    # Temp KLUDGE here rather than in console.py
    command += "."

    attributes = []
    # Get the proper chunk of code from the command.
    root = getRoot(command, terminator='.')
    
    # check to see if the user is attempting to import a package
    # this may need to adjust this so that it doesn't pollute the namespace
    if command.startswith('import ') or command.startswith('from '):
        target = getPackageName(command)
        return completePackageName(target)
    
    try:
        if locals is not None:
            object = eval(root, locals)
        else:
            object = eval(root)
    except:
        return attributes
    
    if ispython(object):  # use existing code
        attributes = getAttributeNames(object, includeMagic, includeSingle, includeDouble)
    else:
        if inspect.isclass(object):
            attributes = staticMethodNames(object)
            attributes.extend(staticFieldNames(object))
        else:
            attributes = list(instanceMethodNames(object.__class__))
                    
    return attributes

def instanceMethodNames(clazz):
    """return a Set of instance method name for a Class"""

    method_names = Set()
    declared_methods = Class.getDeclaredMethods(clazz)
    for method in declared_methods:
        modifiers = method.getModifiers()
        if not Modifier.isStatic(modifiers) and Modifier.isPublic(modifiers):
            name = method.name            
            method_names.add(name)
            if name.startswith("get") and len(name) > 3 and len(method.getParameterTypes()) == 0:
                property_name = name[3].lower() + name[4:]
                method_names.add(property_name)                
                                      
    for eachBase in clazz.__bases__:
        if not ispython(eachBase):
            method_names = method_names | instanceMethodNames(eachBase)

    return method_names

def staticMethodNames(clazz):
    """return a list of static method name for a class"""

    static_methods = {}
    declared_methods = Class.getDeclaredMethods(clazz)
    for method in declared_methods:
        if Modifier.isStatic(method.getModifiers()) and Modifier.isPublic(method.getModifiers()):
            static_methods[method.name] = method
    methods = static_methods.keys()
    
    for eachBase in clazz.__bases__:
        # with Jython 2.5 type is a base of Object, which puts asName in the list        
        # will be a problem for real Java objects that extend Python objects
        # see similar "fixes" in instanceMethodNames and staticFieldNames
        if not ispython(eachBase):
            methods.extend(staticMethodNames(eachBase)) 
    
    return methods
    
def staticFieldNames(clazz):
    """return a list of static field names for class"""

    static_fields = {}
    declared_fields = Class.getDeclaredFields(clazz)
    for field in declared_fields:
        if Modifier.isStatic(field.getModifiers()) and Modifier.isPublic(field.getModifiers()):
            static_fields[field.name] = field
    fields = static_fields.keys()   
    
    for eachBase in clazz.__bases__:
        if not ispython(eachBase):
            fields.extend(staticFieldNames(eachBase)) 

    return fields        

def getCallTipJava(command='', locals=None):
    """For a command, return a tuple of object name, argspec, tip text.

    The call tip information will be based on the locals namespace."""

    calltip = ('', '', '')  # object name, argspec, tip text.

    # Get the proper chunk of code from the command.
    root = getRoot(command, terminator='(')

    try:
        if locals is not None:
            object = eval(root, locals)
        else:
            object = eval(root)
    except:
        return calltip

    if ispython(object):
        # Patrick's code handles python code
        # TODO fix in future because getCallTip runs eval() again
        return getCallTip(command, locals)

    name = ''
    try:
        name = object.__name__
    except AttributeError:
        pass
    
    tipList = []
    argspec = '' # not using argspec for Java
    
    if inspect.isclass(object):
        # get the constructor(s)
        # TODO consider getting modifiers since jython can access private methods
        constructors = object.getConstructors()
        for constructor in constructors:
            paramList = []
            paramTypes = constructor.getParameterTypes()
            # paramTypes is an array of classes, we need Strings
            # TODO consider list comprehension
            for param in paramTypes:
                paramList.append(param.__name__)
            paramString = string.join(paramList,', ')
            tip = "%s(%s)" % (constructor.name, paramString)
            tipList.append(tip)
             
    elif inspect.ismethod(object) or isinstance(object, PyReflectedFunction):
        method = object
        try:
            object = method.im_class
        except: # PyReflectedFunction
            object = method.argslist[0].declaringClass

        # java allows overloading so we may have more than one method
        methodArray = object.getMethods()

        for eachMethod in methodArray:
            if eachMethod.name == method.__name__:
                paramList = []
                for eachParam in eachMethod.parameterTypes:
                    paramList.append(eachParam.__name__)
                 
                paramString = string.join(paramList,', ')

                # create a python style string a la PyCrust
                # we're showing the parameter type rather than the parameter name, since that's all I can get
                # we need to show multiple methods for overloading
                # do we want to show the method visibility?  how about exceptions?
                # note: name, return type and exceptions same for EVERY overload method
                
                tip = "%s(%s) -> %s" % (eachMethod.name, paramString, "unkown_return_type")                    
                tipList.append(tip)

    tip_text = beautify(string.join(tipList,"\n"))
    calltip = (name, argspec, tip_text)        
    return calltip

def beautify(tip_text):
    "Make the call tip text prettier"
    tip_text = tip_text.replace("java.lang.", "")
    if "[" in tip_text:
        tip_text = tip_text.replace("[B", "byte[]")
        tip_text = tip_text.replace("[S", "short[]")
        tip_text = tip_text.replace("[I", "int[]")
        tip_text = tip_text.replace("[J", "long[]")
        tip_text = tip_text.replace("[F", "float[]")
        tip_text = tip_text.replace("[D", "double[]")
        tip_text = tip_text.replace("[Z", "boolean[]")
        tip_text = tip_text.replace("[C", "char[]")
    return tip_text
    
def ispython21(object):
    """
    Figure out if this is Python code or Java Code

    """
    pyclass = 0
    pycode = 0
    pyinstance = 0

    if inspect.isclass(object):
        try:
            object.__doc__
            pyclass = 1
        except AttributeError:
            pyclass = 0

    elif inspect.ismethod(object):
        try:
            object.__dict__
            pycode = 1
        except AttributeError:
            pycode = 0
    else: # I guess an instance of an object falls here
        try:
            object.__dict__
            pyinstance = 1
        except AttributeError:
            pyinstance = 0

    #    print "object", object, "pyclass", pyclass, "pycode", pycode, "returning", pyclass | pycode

    return pyclass | pycode | pyinstance


def ispython22(object):
    """
    Return true if object is Python code.    
    """

    object_type = type(object)

    if object_type.__name__.startswith("java") or isinstance(object, PyReflectedFunction):
        python = False

    elif object_type is types.MethodType:
        # both Java and Python methods return MethodType
        try:
            object.__dict__
            python = True
        except AttributeError:
            python = False
    else:
        # assume everything else is python
        python = True

    return python

def ispython25(object):
    """
    Return true if object is Python code.    
    """

    if isinstance(object, Class):
        python = False
    elif isinstance(object, Object):
        python = False
    elif isinstance(object, PyReflectedFunction):
        python = False
    elif type(object) == types.MethodType and not ispython(object.im_class):
        python = False
    else:
        # assume everything else is python
        python = True       
    
    return python

# Dynamically assign the version of ispython
# To deal with differences between Jython 2.1, 2.2 and 2.5
if sys.version == '2.1':
    ispython = ispython21
elif sys.version.startswith('2.5'):
    ispython = ispython25
else:
    ispython = ispython22
    
def debug(name, value=None):
    if value == None:
        print >> sys.stderr, name
    else:
        print >> sys.stderr, "%s = %s" % (name, value)
