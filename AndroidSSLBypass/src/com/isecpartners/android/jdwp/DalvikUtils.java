package com.isecpartners.android.jdwp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.ByteValue;
import com.sun.jdi.CharValue;
import com.sun.jdi.ClassNotLoadedException;
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
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.MethodEntryRequest;
import com.sun.jdi.request.MethodExitRequest;
import com.sun.jdi.request.StepRequest;

public class DalvikUtils extends Thread {
	private final static org.apache.log4j.Logger LOGGER = Logger
			.getLogger(DalvikUtils.class.getName());
	public static ArrayList<Value> NOARGS = new ArrayList<Value>();
	private ThreadReference currentThread = null;
	private EventRequestManager eventRequestManager = null;
	private VirtualMachine vm = null;
	private ClassLoaderUtils classLoaderUtils;
	private String name = null;

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

	public BreakpointRequest createBreakpointRequest(Location loc){
		BreakpointRequest bpr = this.eventRequestManager
				.createBreakpointRequest(loc);
		// this could be SUSPEND_EVENT_THREAD
		bpr.setSuspendPolicy(EventRequest.SUSPEND_ALL);
		bpr.enable();
		return bpr;
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

	public List<BreakpointRequest> getBreakpoints() {
		return this.eventRequestManager.breakpointRequests();
	}

	public ClassLoaderUtils getClassLoaderUtils() throws InvalidTypeException,
			ClassNotLoadedException, IncompatibleThreadStateException,
			InvocationException {
		this.classLoaderUtils = new ClassLoaderUtils(this);
		return this.classLoaderUtils;
	}

	public ThreadReference getCurrentThread() {
		return this.currentThread;
	}

	private List<ReferenceType> findClasses(String name) {
		this.vm.allClasses();
		return this.vm.classesByName(name);
	}

	public ReferenceType findReferenceType(String name) {
		List<ReferenceType> cls = this.findClasses(name);
		ReferenceType cl = null;
		if (!cls.isEmpty()) {
			if (cls.size() > 1) {
				DalvikUtils.LOGGER
						.warn("found more than one class; solution not implemented, taking the first");
			}
			cl = cls.get(0);
		}
		return cl;
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

	// TODO should create an EventWrapper class or something instead of this
	// something weird bout execution time, thread is resuming w method entry reqs
	public HashMap<String, String> getVarStringFromFrame(
			ThreadReference currentThread, int indx)
			throws IncompatibleThreadStateException,
			AbsentInformationException, InvalidTypeException,
			ClassNotLoadedException, InvocationException {
		HashMap<String, String> varsMap = new HashMap<String, String>();
		if (currentThread.isSuspended()) {
			StackFrame frame = currentThread.frames().get(indx);
			List<LocalVariable> visVars = frame.visibleVariables();
			for (LocalVariable var : visVars) {
				Value val = frame.getValue(var);
				String valStr = "NOVALUE";
				if (val instanceof ObjectReference) {
					ObjectReference valObj = (ObjectReference) val;
					Method toString = this.findMethodInClass(
							valObj.referenceType(), "toString",
							new ArrayList<String>());
					Value objStr = valObj.invokeMethod(currentThread, toString,
							NOARGS, 0);
					valStr = objStr.toString();
				} else {
					valStr = val.toString();
				}
				varsMap.put(var.name(), valStr);
			}
		} else {
			LOGGER.error("thread not suspended");
		}
		return varsMap;
	}

	public List<LocalVariable> getVisibleVars(ThreadReference tr, int idx)
			throws IllegalArgumentException, IncompatibleThreadStateException,
			AbsentInformationException {
		StackFrame frame = tr.frames().get(idx);
		if (frame != null) {
			return frame.visibleVariables();
		}
		return null;
	}

	public Value getLocalVariableValue(ThreadReference tr, int frameIdx,
			String localName) throws AbsentInformationException,
			IncompatibleThreadStateException {
		StackFrame fr = tr.frame(frameIdx);
		Value ret = null;
		LocalVariable var = fr.visibleVariableByName(localName);
		if (var != null) {
			ret = fr.getValue(var);
		}
		return ret;
	}

	public Value getLocalVariableValue(StackFrame fr, String localName)
			throws AbsentInformationException, IncompatibleThreadStateException {
		Value ret = null;
		LocalVariable var = fr.visibleVariableByName(localName);
		if (var != null) {
			ret = fr.getValue(var);
		}
		return ret;
	}

	public StringReference getLocalVariableValueAsString(StackFrame fr,
			String localName) throws AbsentInformationException,
			IncompatibleThreadStateException {
		Value val = null;
		StringReference ret = null;
		LocalVariable var = fr.visibleVariableByName(localName);
		if (var != null) {
			val = fr.getValue(var);
			if (val instanceof StringReference) {
				ret = (StringReference) fr.getValue(var);
			} else {
				LOGGER.warn("getLocalVariableValueAsString called with non-String Object: "
						+ var.getClass().getName());
			}
		} else {
			LOGGER.warn("LocalVariable with name: " + localName
					+ " was not found");
		}
		return ret;
	}

	/*
	 * This function appears to take an exorbitant amount of time why why why
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
		ClassType cl = (ClassType) this
				.findReferenceType("java.lang.ClassLoader");
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

	public ClassWrapper getClassWrapper(String clasName) {
		ClassType clsType = (ClassType) this.findReferenceType(clasName);
		return new ClassWrapper(clsType.classObject(), this.currentThread);
	}

	public Value getFieldValue(String className, String fieldName) {
		ClassWrapper classWrapper = this.getClassWrapper(className);
		Field field = classWrapper.getField(fieldName);
		Value fieldValue = classWrapper.getFieldValue(field);
		return fieldValue;
	}

	// TODO this is not sufficient only does method names not line locations
	// jsut buggy needs to be fixed
	public Location resolveLocation(String location) {
		DalvikUtils.LOGGER.warn("line locations not yet implemented!");
		location = location.trim();
		location = location.replaceAll("\\s", "");
		Location loc = null;
		int parenStart = location.indexOf("(");
		if(parenStart == -1){
			parenStart = location.length()-1;
		}
		String subLoc = location.substring(0, parenStart);
		int endIdx = subLoc.lastIndexOf(".");
		if (endIdx != -1) {
			String className = location.substring(0, endIdx);
			String method = location.substring(endIdx+1);
			String methodName = method.substring(0, method.indexOf("("));
			LOGGER.debug("classname: " + className);
			LOGGER.debug("method:" + method);
			LOGGER.debug("methodName: " + methodName);
			ReferenceType cr = this.findReferenceType(className);
			if (cr != null) {
				for (Method m : cr.methodsByName(methodName)) {
					// TODO need to think on this comparison ...
					String mString = m.toString().trim();
					mString = mString.replaceAll("\\s", "");
					if (mString.contains(method)) {
						loc = m.location();
						
						if( loc == null){
							LOGGER.debug("found location but null, method is in abstract class?");
						} else {
							LOGGER.info("found location: " + loc);
							break;
						}
					} else {
						LOGGER.debug("locations do not match: " + mString + " != " + location);
					}
				}
			} else {
				LOGGER.debug("could not find class: " + className);
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

}
