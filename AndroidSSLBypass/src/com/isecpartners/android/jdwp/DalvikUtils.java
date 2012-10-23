package com.isecpartners.android.jdwp;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.isecpartners.android.jdwp.plugin.SSLBypassJDIPlugin;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.ByteValue;
import com.sun.jdi.CharValue;
import com.sun.jdi.ClassLoaderReference;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassObjectReference;
import com.sun.jdi.ClassType;
import com.sun.jdi.DoubleValue;
import com.sun.jdi.Field;
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

public class DalvikUtils extends Thread{
	private final static org.apache.log4j.Logger LOGGER = Logger
			.getLogger(DalvikUtils.class.getName());
	private static final String NEW_INSTANCE_METHOD_NAME = "newInstance";
	private static final String LOAD_CLASS_METHOD_NAME = "loadClass";
	public static ArrayList<Value> NOARGS = new ArrayList<Value>();
	private ThreadReference currentThread = null;
	private EventRequestManager eventRequestManager = null;
	private String name = null;
	private VirtualMachine vm = null;
	private DalvikUtils vmUtils;
	private ClassLoaderUtils classLoaderUtils;

	public DalvikUtils(VirtualMachine vm, int threadIndex) {
		this.vm = vm;
		this.name = this.vm.name();

		// TODO dont know if this should be defaulted or exception thrown
		if ((threadIndex < 0) || (threadIndex >= this.vm.allThreads().size())) {
			threadIndex = 0;
			DalvikUtils.LOGGER
					.warn("out of bounds condition with given argument value : "
							+ threadIndex + " using default value of 0");
		}
		this.currentThread = this.vm.allThreads().get(threadIndex);
		this.eventRequestManager = this.vm.eventRequestManager();
	}

	public DalvikUtils(VirtualMachine virtualMachine, ThreadReference thread) {
		this.vm = virtualMachine;
		this.currentThread = thread;
		this.name = this.vm.name();
	}

	public ClassLoaderUtils getClassLoaderUtils() throws InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException, InvocationException{
		this.classLoaderUtils = new ClassLoaderUtils(this);
		return this.classLoaderUtils;
	}
	
	public ThreadReference getCurrentThread() {
		return this.currentThread;
	}

	public BooleanValue createBool(boolean toCreate) {
		BooleanValue boolVal = this.vm.mirrorOf(toCreate);
		return boolVal;
	}

	public ByteValue createByte(byte toCreate) {
		ByteValue byteVal = this.vm.mirrorOf(toCreate);
		return byteVal;
	}

	public CharValue createChar(char toCreate) {
		CharValue charVal = this.vm.mirrorOf(toCreate);
		return charVal;
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

	public ClassPrepareRequest createClassPrepareRequest(String classFilter) {
		ClassPrepareRequest cpr = this.eventRequestManager
				.createClassPrepareRequest();
		cpr.setSuspendPolicy(EventRequest.SUSPEND_ALL);
		cpr.addClassFilter(classFilter);
		cpr.enable();
		return cpr;
	}

	public BreakpointRequest createBreakpointRequest(Location loc) {
		BreakpointRequest bpr = this.eventRequestManager
				.createBreakpointRequest(loc);
		// this could be SUSPEND_EVENT_THREAD
		bpr.setSuspendPolicy(EventRequest.SUSPEND_ALL);
		bpr.enable();
		return bpr;
	}

	public List<BreakpointRequest> getBreakpoints() {
		return this.eventRequestManager.breakpointRequests();
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
		this.vm.allClasses();
		return this.vm.classesByName(name);
	}

	public ClassType findClassType(String name) {
		List<ReferenceType> cls = this.findClasses(name);
		ReferenceType cl = null;
		if (!cls.isEmpty()) {
			if (cls.size() > 1) {
				DalvikUtils.LOGGER
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

	public EventRequestManager getEventRequestManager() {
		this.eventRequestManager = this.vm.eventRequestManager();
		return this.eventRequestManager;
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

	/*
	 * THis function appears to take an exorbitant amount of time why why why
	 */
	public boolean setLocalVariableValue(int i, String name, Value sf)
			throws IncompatibleThreadStateException, InvalidTypeException,
			ClassNotLoadedException, AbsentInformationException {
		StackFrame frame = this.currentThread.frames().get(i);
		LocalVariable var = frame.visibleVariableByName(name);
		LOGGER.info("got var: " + var.typeName());
		try {
			frame.setValue(var, sf);
			LOGGER.info("success setting new variable value");
			return true;
		} catch (java.lang.ClassCastException e) {
			/*
			 * KNOWN ISSUE: when checking type compatibility the debugger
			 * requests the ClassLoader of the type of the variable. When an
			 * object is loaded via reflection (using the current method) this
			 * will return an ObjectReference. The debugger is expecting a
			 * ClassLoaderReference and apparently the ObjectReference cannot be
			 * cast to a ClassLoaderReference.
			 */
			LOGGER.info("ClassCastException due to type assignments with classes loaded via reflection, work around is to load class again");
		}
		return false;
	}

	public Value getSystemClassLoader(ThreadReference tr)
			throws InvalidTypeException, ClassNotLoadedException,
			IncompatibleThreadStateException, InvocationException {
		DalvikUtils.LOGGER
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

	// TODO this is not sufficient only does method names not line locations
	public Location resolveLocation(String location) {
		DalvikUtils.LOGGER.warn("line locations not yet implemented!");
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

	public void setVm(VirtualMachine vm) {
		this.vm = vm;
	}

	public void suspendAllThreads() {
		this.vm.suspend();
	}

	public ClassWrapper getClassWrapper(String clasName) {
		ClassType clsType = this.findClassType(clasName);
		return new ClassWrapper(clsType.classObject(), this.currentThread);
	}

	public Value getFieldValue(String className, String fieldName) {
		ClassWrapper classWrapper = this.getClassWrapper(className);
		Field field = classWrapper.getField(fieldName);
		Value fieldValue = classWrapper.getFieldValue(field);
		return fieldValue;
	}

}
