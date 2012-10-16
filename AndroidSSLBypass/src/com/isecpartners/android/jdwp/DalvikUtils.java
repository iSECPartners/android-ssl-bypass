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
	private static final String DEX_CLASS_LOADER_CLASS = "dalvik.system.DexClassLoader";
	private static final String LOAD_CLASS_METHOD_NAME = "loadClass";
	public static ArrayList<Value> NOARGS = new ArrayList<Value>();
	private ThreadReference currentThread = null;
	private EventRequestManager eventRequestManager = null;
	private String name = null;
	private VirtualMachine vm = null;

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
	}
	
	public DalvikUtils(VirtualMachine vm, ThreadReference thread) {
		this.vm = vm;
		this.name = this.vm.name();
		this.currentThread = thread;
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

	public ClassLoaderReference getBootClassLoader(ThreadReference tr)
			throws InvalidTypeException, ClassNotLoadedException,
			IncompatibleThreadStateException, InvocationException {
		Value scl = this.getSystemClassLoader(tr);
		ObjectReference sclObj = (ObjectReference) scl;
		ReferenceType sclRef = sclObj.referenceType();
		Method getParent = sclRef.methodsByName("getParent").get(0);
		return (ClassLoaderReference) sclObj.invokeMethod(tr, getParent,
				new ArrayList<Value>(), 0);
	}

	public ClassType loadDexClassLoader(){
		ClassType dexLoader = this
				.findClassType(DalvikUtils.DEX_CLASS_LOADER_CLASS);
		if (dexLoader != null) {
			DalvikUtils.LOGGER.info("DexClassLoader already loaded!");
		} else {
			DalvikUtils.LOGGER
					.info("DexClassLoader not loaded, loading via reflection");
			ClassObjectReference dexLoaderClassObj;
			try {
				dexLoaderClassObj = (ClassObjectReference) this
						.loadClassReflection(this.currentThread, DalvikUtils.DEX_CLASS_LOADER_CLASS);
				// Note the use of reflectedType() vs referenceType()
				dexLoader = (ClassType) dexLoaderClassObj.reflectedType();
				dexLoader = this
						.findClassType(DalvikUtils.DEX_CLASS_LOADER_CLASS);
				LOGGER.info("got dexLoader: " + dexLoader.name());
			} catch (InvalidTypeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotLoadedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IncompatibleThreadStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return dexLoader;
	}
	
	public Value getDexClassLoader(ThreadReference tr, String dexPath,
			String optPath, String libPath, Value parentLoader)
			throws InvalidTypeException, ClassNotLoadedException,
			IncompatibleThreadStateException, InvocationException,
			NoLoadClassMethodException {

		ClassType dexCLType = this.loadDexClassLoader();

		StringReference pathRef = this.createString(dexPath);
		StringReference optRef = this.createString(optPath);
		StringReference libRef = this.createString(libPath);

		List<Value> vals = new ArrayList<Value>();
		vals.add(pathRef);
		vals.add(optRef);
		vals.add(libRef);
		vals.add(parentLoader);

		Method init = dexCLType.methodsByName("<init>").get(0);
		Value dexLoaderObject = dexCLType.newInstance(tr, init, vals, 0);
		DalvikUtils.LOGGER.info("got DexClassLoader Object: "
				+ dexLoaderObject.type().name());
		return dexLoaderObject;
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

	public Value loadBaseClass(ThreadReference tr, String className)
			throws InvalidTypeException, ClassNotLoadedException,
			IncompatibleThreadStateException, InvocationException,
			NoLoadClassMethodException {
		ClassLoaderReference bcl = (ClassLoaderReference) this
				.getBootClassLoader(tr);
		return this.loadClass(tr, className, bcl);
	}

	/*
	 * Another one that takes a really really long time on first run
	 */
	public Value loadClassReflection(ThreadReference tr, String className)
			throws InvalidTypeException, ClassNotLoadedException,
			IncompatibleThreadStateException, InvocationException {
		DalvikUtils.LOGGER
				.info("attempting to load class via reflection (Class.forName): "
						+ className);
		StringReference clsName = this.createString(className);
		ArrayList<Value> args = new ArrayList<Value>();
		args.add(clsName);
		ClassWrapper clWrap = this.getClassWrapper("java.lang.Class");
		Value reflectedType = clWrap.invokeMethodOnType(
				clWrap.getReferenceType(), "forName", args);
		LOGGER.info(reflectedType.getClass().getName());
		return reflectedType;

	}

	public Value loadClass(ThreadReference tr, String className, Value loader)
			throws InvalidTypeException, ClassNotLoadedException,
			IncompatibleThreadStateException, InvocationException,
			NoLoadClassMethodException {
		ReferenceType refType = null;
		StringReference clsName = this.createString(className);
		ArrayList<Value> args = new ArrayList<Value>();
		args.add(clsName);
		LOGGER.info(loader.type().name());
		try {
			ObjectReference objRef = (ObjectReference) loader;
			ArrayList<String> argTypes = new ArrayList<String>();
			argTypes.add("java.lang.String");
			Method loadClass = this.findMethodInClass(refType,
					DalvikUtils.LOAD_CLASS_METHOD_NAME, argTypes);
			if (loadClass == null) {
				throw new NoLoadClassMethodException();
			}
			return objRef.invokeMethod(tr, loadClass, args, 0);
		} catch (Exception e) {
			LOGGER.error(e);
		}
		return null;
	}

	public Value loadClass(ThreadReference tr, String className,
			ClassWrapper loader) throws InvalidTypeException,
			ClassNotLoadedException, IncompatibleThreadStateException,
			InvocationException, NoLoadClassMethodException {
		ReferenceType refType = null;
		try {
			ObjectReference newInst = loader.newInstance();
			StringReference clsName = this.createString(className);
			ArrayList<Value> args = new ArrayList<Value>();
			args.add(clsName);
			loader.invokeMethodOnType(newInst.referenceType(), "loadClass",
					args);
		} catch (Exception e) {
			LOGGER.error(e);
		}
		return null;
	}

	public ClassWrapper loadClass(String className, ClassWrapper loader)
			throws InvalidTypeException, ClassNotLoadedException,
			IncompatibleThreadStateException, InvocationException,
			NoLoadClassMethodException {
		Value cl = this.loadClass(this.currentThread, className, loader);
		if (cl == null) {
			LOGGER.error("could not load class: " + className);
			return null;
		}
		ReferenceType t = (ReferenceType) cl.type();
		return new ClassWrapper(t.classObject(), this.currentThread);
	}

	public ClassWrapper loadExternalClassFromAPK(String dexPath, String dexOpt,
			String libPath, String className, String mainActivityClass)
			throws InvalidTypeException, ClassNotLoadedException,
			IncompatibleThreadStateException, InvocationException,
			DexClassLoaderNotFoundException, NoLoadClassMethodException {

		// first check if class is already loaded
		ClassType clsType = this.findClassType(className);
		if (clsType != null) {
			LOGGER.info("class already loaded");
			return new ClassWrapper(clsType.classObject(), this.currentThread);

		} else {
			DalvikUtils.LOGGER
					.info("class not loaded, attempting to load class from external APK using DexClassLoader: "
							+ className);

			ClassLoaderReference parentLoader = this
					.getBootClassLoader(this.currentThread);

			ObjectReference dexLoader = (ObjectReference) this
					.getDexClassLoader(this.currentThread, dexPath, dexOpt,
							libPath, parentLoader);
			if (dexLoader != null) {
				DalvikUtils.LOGGER.info("got DexClassLoader instance: "
						+ dexLoader.type().name() + " for path: " + dexPath
						+ " with parentLoader: " + parentLoader.type().name());

				StringReference clsName = this.createString(className);
				ArrayList<Value> args = new ArrayList<Value>();
				args.add(clsName);
				Value result = dexLoader.invokeMethod(this.currentThread,
						dexLoader.referenceType().methodsByName("loadClass")
								.get(0), args, 0);
				// TODO fix this mess
				if (result != null) {
					if (result instanceof ClassObjectReference) {
						LOGGER.info("result is ClassObjectReference");
						ClassObjectReference resultObj = (ClassObjectReference) result;
						LOGGER.info("loaded class: "
								+ resultObj.reflectedType().name());
						return new ClassWrapper(resultObj, this.currentThread);
					} else if (result instanceof ObjectReference) {
						LOGGER.info("result is ObjectReference");
						return new ClassWrapper(((ObjectReference) result)
								.referenceType().classObject(),
								this.currentThread);
					} else if (result instanceof ReferenceType) {
						LOGGER.info("result is ReferenceType");
						return new ClassWrapper(
								((ReferenceType) result).classObject(),
								this.currentThread);
					} else if (result instanceof ClassType) {
						LOGGER.info("result is ReferenceType");
						return new ClassWrapper(
								((ClassType) result).classObject(),
								this.currentThread);
					}

				}
			} else {
				LOGGER.error("could not get DexClassLoader");
				return null;
			}
		}
		return null;
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

	public ClassWrapper getLoadClassWrapperFromBootLoader(String className)
			throws InvalidTypeException, ClassNotLoadedException,
			IncompatibleThreadStateException, InvocationException,
			NoLoadClassMethodException {
		ClassWrapper loader = this.getClassWrapper(className);
		return this.loadClass(className, loader);
	}

	public Value getFieldValue(String className, String fieldName) {
		ClassWrapper classWrapper = this.getClassWrapper(className);
		Field field = classWrapper.getField(fieldName);
		Value fieldValue = classWrapper.getFieldValue(field);
		return fieldValue;
	}

}
