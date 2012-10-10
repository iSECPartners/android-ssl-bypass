package com.isecpartners.android.jdwp;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.ByteValue;
import com.sun.jdi.CharValue;
import com.sun.jdi.ClassLoaderReference;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassObjectReference;
import com.sun.jdi.ClassType;
import com.sun.jdi.DoubleValue;
import com.sun.jdi.FloatValue;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.LongValue;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ShortValue;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.MethodEntryRequest;
import com.sun.jdi.request.MethodExitRequest;
import com.sun.jdi.request.StepRequest;

public class VMUtils {
	private static final String DEX_CLASS_LOADER_CLASS = "dalvik.system.DexClassLoader";

	private static final String LOAD_CLASS_METHOD_NAME = "loadClass";

	private final static org.apache.log4j.Logger LOGGER = Logger
			.getLogger(VMUtils.class.getName());

	private static final String NEW_INSTANCE = "newInstance";

	public static ArrayList<Value> NOARGS = new ArrayList<Value>();
	private ThreadReference currentThread = null;
	private EventRequestManager eventRequestManager = null;
	private String name = null;
	private VirtualMachine vm = null;

	public VMUtils(VirtualMachine vm, int threadIndex) {
		this.vm = vm;
		this.name = this.vm.name();

		// TODO dont know if this should be defaulted or exception thrown
		if ((threadIndex < 0) || (threadIndex >= this.vm.allThreads().size())) {
			threadIndex = 0;
			VMUtils.LOGGER
					.warn("out of bounds condition with given argument value : "
							+ threadIndex + " using default value of 0");
		}
		this.currentThread = this.vm.allThreads().get(threadIndex);

		this.eventRequestManager = this.vm.eventRequestManager();
	}

	public BooleanValue createBool(boolean toCreate) {
		BooleanValue boolVal = this.vm.mirrorOf(toCreate);
		return boolVal;
	}

	public BreakpointRequest createBreakpointRequest(Location loc) {
		BreakpointRequest bpr = this.eventRequestManager
				.createBreakpointRequest(loc);
		// this could be SUSPEND_EVENT_THREAD
		bpr.setSuspendPolicy(EventRequest.SUSPEND_ALL);
		bpr.enable();
		return bpr;
	}

	public ByteValue createByte(byte toCreate) {
		ByteValue byteVal = this.vm.mirrorOf(toCreate);
		return byteVal;
	}

	public CharValue createChar(char toCreate) {
		CharValue charVal = this.vm.mirrorOf(toCreate);
		return charVal;
	}

	public ClassPrepareRequest createClassPrepareRequest(String classFilter) {
		ClassPrepareRequest cpr = this.eventRequestManager
				.createClassPrepareRequest();
		cpr.setSuspendPolicy(EventRequest.SUSPEND_ALL);
		cpr.addClassFilter(classFilter);
		cpr.enable();
		return cpr;
	}

	public DoubleValue createDouble(double toCreate) {
		DoubleValue doubleVal = this.vm.mirrorOf(toCreate);
		return doubleVal;
	}

	public FloatValue createFloat(float toCreate) {
		FloatValue floatVal = this.vm.mirrorOf(toCreate);
		return floatVal;
	}

	public IntegerValue createInt(int toCreate) {
		IntegerValue intVal = this.vm.mirrorOf(toCreate);
		return intVal;
	}

	public LongValue createLong(long toCreate) {
		LongValue longVal = this.vm.mirrorOf(toCreate);
		return longVal;
	}

	public MethodEntryRequest createMethodEntryRequest(String classFilter) {
		MethodEntryRequest mer = this.eventRequestManager
				.createMethodEntryRequest();
		// this could be SUSPEND_EVENT_THREAD
		mer.setSuspendPolicy(EventRequest.SUSPEND_ALL);
		mer.addClassFilter(classFilter);
		mer.enable();
		return mer;
	}

	public MethodExitRequest createMethodExitRequest(String classFilter) {
		MethodExitRequest mexr = this.eventRequestManager
				.createMethodExitRequest();
		// this could be SUSPEND_EVENT_THREAD
		mexr.setSuspendPolicy(EventRequest.SUSPEND_ALL);
		mexr.addClassFilter(classFilter);
		mexr.enable();
		return mexr;
	}

	public ShortValue createShort(short toCreate) {
		ShortValue shortVal = this.vm.mirrorOf(toCreate);
		return shortVal;
	}

	public StepRequest createStepRequest(ThreadReference tr, int depth, int type) {
		StepRequest req = this.eventRequestManager.createStepRequest(tr, depth,
				type);
		// req.addCountFilter(1); // next step only
		req.setSuspendPolicy(EventRequest.SUSPEND_ALL);
		req.enable();
		return req;
	}

	public StringReference createString(String toCreate) {
		StringReference stringRef = this.vm.mirrorOf(toCreate);
		return stringRef;
	}

	public void deleteAllBreakpoints() {
		for (BreakpointRequest req : this.eventRequestManager
				.breakpointRequests()) {
			req.disable();
		}
	}

	public void deleteAllClassPrepare() {
		this.eventRequestManager.deleteEventRequests(this.eventRequestManager
				.classPrepareRequests());
	}

	public void deleteAllMethodEntry() {
		this.eventRequestManager.deleteEventRequests(this.eventRequestManager
				.methodEntryRequests());
	}

	public void deleteAllMethodExit() {
		this.eventRequestManager.deleteEventRequests(this.eventRequestManager
				.methodExitRequests());
	}

	public void deleteAllRequests() {
		this.deleteAllBreakpoints();
		this.deleteAllMethodEntry();
		this.deleteAllMethodExit();
		this.deleteAllClassPrepare();
	}

	public void deleteAllStep() {
		this.eventRequestManager.deleteEventRequests(this.eventRequestManager
				.stepRequests());
	}

	public void deleteEventRequest(EventRequest req) {
		this.eventRequestManager.deleteEventRequest(req);
	}

	private List<ReferenceType> findClasses(String name) {
		return this.vm.classesByName(name);
	}

	public ClassType findClassType(String name) {
		List<ReferenceType> cls = this.findClasses(name);
		ReferenceType cl = null;
		if (!cls.isEmpty()) {
			if (cls.size() > 1) {
				VMUtils.LOGGER
						.warn("found more than one class; solution not implemented, taking the first");
			}
			cl = cls.get(0);
		}
		return (ClassType) cl;
	}

	public Method findMethodInClass(ReferenceType clazz, String methodName,
			List<String> argTypes) {
		Method toReturn = null;
		for (Method m : clazz.methodsByName(methodName)) {
			if (m.argumentTypeNames().equals(argTypes)) {
				toReturn = m;
				break;
			}
		}
		return toReturn;
	}

	public List<ReferenceType> getAllClasses() {
		return this.vm.allClasses();
	}

	public List<ThreadReference> getAllThreads() {
		return this.vm.allThreads();
	}

	public Value getBootClassLoader(ThreadReference tr)
			throws InvalidTypeException, ClassNotLoadedException,
			IncompatibleThreadStateException, InvocationException {
		Value scl = this.getSystemClassLoader(tr);
		ObjectReference sclObj = (ObjectReference) scl;
		ReferenceType sclRef = sclObj.referenceType();
		Method getParent = sclRef.methodsByName("getParent").get(0);
		return sclObj.invokeMethod(tr, getParent, new ArrayList<Value>(), 0);
	}

	public List<BreakpointRequest> getBreakpoints() {
		return this.eventRequestManager.breakpointRequests();
	}

	public ThreadReference getCurrentThread() {
		return this.currentThread;
	}

	public Value getDexClassLoader(ThreadReference tr, String dexPath,
			String optPath, String libPath, Value parentLoader)
			throws InvalidTypeException, ClassNotLoadedException,
			IncompatibleThreadStateException, InvocationException {

		ClassType dexLoader = this
				.findClassType(VMUtils.DEX_CLASS_LOADER_CLASS);
		if (dexLoader != null) {
			VMUtils.LOGGER.info("DexClassLoader already loaded!");
		} else {
			VMUtils.LOGGER
					.info("DexClassLoader not loaded, loading via reflection");
			ClassObjectReference dexLoaderClassObj = (ClassObjectReference) this
					.loadClassReflection(tr, VMUtils.DEX_CLASS_LOADER_CLASS);
			// Note the use of reflectedType() vs referenceType()
			dexLoader = (ClassType) dexLoaderClassObj.reflectedType();
		}

		StringReference pathRef = this.createString(dexPath);
		StringReference optRef = this.createString(optPath);
		StringReference libRef = this.createString(libPath);

		List<Value> vals = new ArrayList<Value>();
		vals.add(pathRef);
		vals.add(optRef);
		vals.add(libRef);
		vals.add(parentLoader);

		Method init = dexLoader.allMethods().get(0);
		Value dexLoaderObject = dexLoader.newInstance(tr, init, vals, 0);
		VMUtils.LOGGER.info("got DexClassLoader Object: "
				+ dexLoaderObject.type().name());
		return dexLoaderObject;
	}

	public EventRequestManager getEventRequestManager() {
		this.eventRequestManager = this.vm.eventRequestManager();
		return this.eventRequestManager;
	}

	public ObjectReference getFrameObject(ThreadReference tr, int idx)
			throws IncompatibleThreadStateException {
		StackFrame frame = tr.frames().get(idx);
		return frame.thisObject();
	}

	public Value getFrameObjectClassLoader(ThreadReference tr, int idx)
			throws InvalidTypeException, ClassNotLoadedException,
			IncompatibleThreadStateException, InvocationException {
		StackFrame fr = tr.frames().get(idx);
		ObjectReference currObj = fr.thisObject();
		ReferenceType currObjRef = currObj.referenceType();
		return currObjRef.classLoader();
	}

	public Value getFrameObjectClassLoaderParent(ThreadReference tr, int idx)
			throws InvalidTypeException, ClassNotLoadedException,
			IncompatibleThreadStateException, InvocationException {
		ClassLoaderReference objLoader = (ClassLoaderReference) this
				.getFrameObjectClassLoader(tr, idx);
		if (objLoader != null) {
			Method getParent = objLoader.referenceType()
					.methodsByName("getParent").get(0);
			return objLoader.invokeMethod(tr, getParent,
					new ArrayList<Value>(), 0);
		}
		return null;
	}

	public StackFrame getFrameZero(ThreadReference tr)
			throws IncompatibleThreadStateException {
		return tr.frames().get(0);
	}

	public List<LocalVariable> getLocals(ThreadReference tr, int idx)
			throws IllegalArgumentException, IncompatibleThreadStateException,
			AbsentInformationException {
		StackFrame frame = tr.frames().get(idx);
		if (frame != null) {
			return frame.visibleVariables();
		}
		return null;
	}

	public Value getLocalVariableValue(ThreadReference tr, StackFrame fr,
			String localName) throws AbsentInformationException,
			IncompatibleThreadStateException {
		Value ret = null;
		LocalVariable var = fr.visibleVariableByName(localName);
		if (var != null) {
			ret = fr.getValue(var);
		}
		return ret;
	}

	public String getName() {
		return this.name;
	}

	public Value getSystemClassLoader(ThreadReference tr)
			throws InvalidTypeException, ClassNotLoadedException,
			IncompatibleThreadStateException, InvocationException {
		VMUtils.LOGGER
				.info("attempting to get the system class loader (Class.getSystemClassLoader)");
		Value toreturn = null;
		ClassType cl = this.findClassType("java.lang.ClassLoader");
		if (cl != null) {
			List<Method> getSCLMS = cl.methodsByName("getSystemClassLoader");
			Method getSCL = getSCLMS.get(0);
			if (getSCL != null) {
				Value result = cl.invokeMethod(tr, getSCL,
						new ArrayList<Value>(), 0);
				toreturn = result;
			}
		}
		return toreturn;
	}

	public VirtualMachine getVm() {
		return this.vm;
	}

	public Value loadBaseClass(ThreadReference tr, String className)
			throws InvalidTypeException, ClassNotLoadedException,
			IncompatibleThreadStateException, InvocationException,
			NoLoadClassMethodException {
		ClassLoaderReference bcl = (ClassLoaderReference) this
				.getBootClassLoader(tr);
		return this.loadClass(tr, className, bcl);
	}

	// TODO dont like the duplicated code here
	public Value loadClass(ThreadReference tr, String className,
			Value classLoaderObject) throws InvalidTypeException,
			ClassNotLoadedException, IncompatibleThreadStateException,
			InvocationException, NoLoadClassMethodException {
		ReferenceType refType = null;
		StringReference clsName = this.createString(className);
		ArrayList<Value> args = new ArrayList<Value>();
		args.add(clsName);

		if (classLoaderObject instanceof ClassLoaderReference) {
			ClassLoaderReference clRef = (ClassLoaderReference) classLoaderObject;
			refType = clRef.referenceType();
			ArrayList<String> argTypes = new ArrayList<String>();
			argTypes.add("java.lang.String");
			Method loadClass = this.findMethodInClass(refType,
					VMUtils.LOAD_CLASS_METHOD_NAME, argTypes);
			if(loadClass == null){
				throw new NoLoadClassMethodException();
			}
			return clRef.invokeMethod(tr, loadClass, args, 0);

		} else if (classLoaderObject instanceof ObjectReference) {
			ObjectReference objRef = (ObjectReference) classLoaderObject;
			refType = objRef.referenceType();
			ArrayList<String> argTypes = new ArrayList<String>();
			argTypes.add("java.lang.String");
			Method loadClass = this.findMethodInClass(refType,
					VMUtils.LOAD_CLASS_METHOD_NAME, argTypes);
			if(loadClass == null){
				throw new NoLoadClassMethodException();
			}
			return objRef.invokeMethod(tr, loadClass, args, 0);
		}
		return null;
	}

	public Value loadClassReflection(ThreadReference tr, String className)
			throws InvalidTypeException, ClassNotLoadedException,
			IncompatibleThreadStateException, InvocationException {
		VMUtils.LOGGER
				.info("attempting to load class via reflection (Class.forName): "
						+ className);
		ClassType clazz = this.findClassType("java.lang.Class");
		List<String> argsTypes = new ArrayList<String>();
		argsTypes.add("java.lang.String");
		Method m = this.findMethodInClass(clazz, "forName", argsTypes);
		StringReference clsName = this.createString(className);
		ArrayList<Value> args = new ArrayList<Value>();
		args.add(clsName);
		return clazz.classObject().invokeMethod(tr, m, args, 0);
	}

	//TODO many weird bugs in this!!
	public ObjectReference newInstanceOfDexClass(ThreadReference tr,
			String dexPath, String optPath, String libPath, String className,
			Value parentLoader) throws InvalidTypeException,
			ClassNotLoadedException, IncompatibleThreadStateException,
			InvocationException, DexClassLoaderNotFoundException,
			NoLoadClassMethodException {
		VMUtils.LOGGER
				.info("attempting to load class from external apk using DexClassLoader: "
						+ className);

		ObjectReference dexLoader = (ObjectReference) this.getDexClassLoader(
				tr, dexPath, optPath, libPath, parentLoader);
		VMUtils.LOGGER.info("got DexClassLoader instance for path: " + dexPath
				+ " with parentLoader: " + parentLoader.type().name());
		Value cls = this.loadClass(tr, className, dexLoader);
		ClassObjectReference loadedClass = (ClassObjectReference) cls;

		// Note the difference between reflectedType and referenceType
		VMUtils.LOGGER.info("successfully loaded class: "
				+ loadedClass.reflectedType().name());

		// Getting a reference to the underlying Class Object so we can use
		// newInstance
		ClassType loadedClassType = (ClassType) loadedClass.referenceType();
		Method newInst = loadedClassType.methodsByName(VMUtils.NEW_INSTANCE)
				.get(0);
		return (ObjectReference) loadedClassType.invokeMethod(tr, newInst,
				VMUtils.NOARGS, 0);
	}

	// TODO this is not sufficient only does method names not line locations
	public Location resolveLocation(String location) {
		VMUtils.LOGGER.warn("line locations not yet implemented!");
		location = location.trim();
		Location loc = null;
		int endIdx = location.lastIndexOf(".");
		if (endIdx != -1) {
			String className = location.substring(0, endIdx);
			ReferenceType cr = this.findClassType(className);
			if (cr != null) {
				for (Method m : cr.allMethods()) {
					// TODO need to think on this comparison ...
					if (m.toString().contains(location)) {
						loc = m.location();
						break;
					}
				}
			}
		}
		return loc;
	}

	public void resumeAllThreads() {
		this.vm.resume();
	}

	public List<Type> searchForType(String filter) {
		List<Type> result = new ArrayList<Type>();
		for (Type ref : this.vm.allClasses()) {
			if (ref.name().contains(filter)) {
				result.add(ref);
			}
		}
		return result;
	}

	public void setCurrentThread(ThreadReference currentThread) {
		this.currentThread = currentThread;
	}

	public void setLocalVariableValue(ThreadReference tr, StackFrame fr,
			String name, Value newVal) throws IncompatibleThreadStateException,
			InvalidTypeException, ClassNotLoadedException,
			AbsentInformationException {
		LocalVariable var = fr.visibleVariableByName(name);
		fr.setValue(var, newVal);
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setVm(VirtualMachine vm) {
		this.vm = vm;
	}

	public void suspendAllThreads() {
		this.vm.suspend();
	}
}
