package com.isecpartners.android.jdwp;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.sun.jdi.ClassLoaderReference;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassObjectReference;
import com.sun.jdi.ClassType;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;

public class ClassLoaderUtils {
	private final static org.apache.log4j.Logger LOGGER = Logger
			.getLogger(ClassLoaderUtils.class.getName());
	
	static final String DEX_CLASS_LOADER_CLASS = "dalvik.system.DexClassLoader";
	static final String PATH_CLASS_LOADER_CLASS = "dalvik.system.PathClassLoader";
	static final String CLASS_LOADER_CLASS = "java.lang.ClassLoader";
	static final String GET_PARENT_METHOD_NAME = "getParent";
	static final String LOAD_CLASS_METHOD_NAME = "loadClass";
	private DalvikUtils vmUtils;

	private ClassType dexLoader;

	private ThreadReference currentThread;

	public ClassLoaderUtils(DalvikUtils vmUtils) throws InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException, InvocationException{
		this.vmUtils = vmUtils;
		this.currentThread = this.vmUtils.getCurrentThread();
		this.dexLoader = this.getDexClassLoaderClassType();
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

	public ClassType getDexClassLoaderClassType() throws InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException, InvocationException{
		dexLoader = (ClassType) this.vmUtils.findReferenceType(DEX_CLASS_LOADER_CLASS);
		if (dexLoader != null) {
			LOGGER.info("DexClassLoader already loaded!");
		} else {
			LOGGER
					.info("DexClassLoader not loaded, loading via reflection");
			ClassObjectReference dexLoaderClassObj;
			
				dexLoaderClassObj = (ClassObjectReference) this
						.loadClassReflection(this.currentThread, DEX_CLASS_LOADER_CLASS);
				// Note the use of reflectedType() vs referenceType()
				dexLoader = (ClassType) dexLoaderClassObj.reflectedType();
				dexLoader = (ClassType) this.vmUtils
						.findReferenceType(DEX_CLASS_LOADER_CLASS);
				LOGGER.info("got dexLoader: " + dexLoader.name());
		}
		return dexLoader;
	}
	
	public Value getDexClassLoader(ThreadReference tr, String dexPath,
			String optPath, String libPath, Value parentLoader)
			throws InvalidTypeException, ClassNotLoadedException,
			IncompatibleThreadStateException, InvocationException,
			NoLoadClassMethodException {

		ClassType dexCLType = this.getDexClassLoaderClassType();

		StringReference pathRef = this.vmUtils.createString(dexPath);
		StringReference optRef = this.vmUtils.createString(optPath);
		StringReference libRef = this.vmUtils.createString(libPath);

		List<Value> vals = new ArrayList<Value>();
		vals.add(pathRef);
		vals.add(optRef);
		vals.add(libRef);
		vals.add(parentLoader);

		Method init = dexCLType.methodsByName("<init>").get(0);
		Value dexLoaderObject = dexCLType.newInstance(tr, init, vals, 0);
		LOGGER.info("got DexClassLoader Object: "
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
	
	public Value getSystemClassLoader(ThreadReference tr)
			throws InvalidTypeException, ClassNotLoadedException,
			IncompatibleThreadStateException, InvocationException {
		LOGGER
				.info("attempting to get the system class loader (Class.getSystemClassLoader)");
		Value toreturn = null;
		ClassType cl = (ClassType) this.vmUtils.findReferenceType("java.lang.ClassLoader");
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
		LOGGER
				.info("attempting to load class via reflection (Class.forName): "
						+ className);
		StringReference clsName = this.vmUtils.createString(className);
		ArrayList<Value> args = new ArrayList<Value>();
		args.add(clsName);
		ClassWrapper clWrap = this.vmUtils.getClassWrapper("java.lang.Class");
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
		StringReference clsName = this.vmUtils.createString(className);
		ArrayList<Value> args = new ArrayList<Value>();
		args.add(clsName);
		LOGGER.info(loader.type().name());
		try {
			ObjectReference objRef = (ObjectReference) loader;
			ArrayList<String> argTypes = new ArrayList<String>();
			argTypes.add("java.lang.String");
			Method loadClass = this.vmUtils.findMethodInClass(refType,
					LOAD_CLASS_METHOD_NAME, argTypes);
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
		try {
			ObjectReference newInst = loader.newInstance();
			StringReference clsName = this.vmUtils.createString(className);
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
		ClassType clsType = (ClassType) this.vmUtils.findReferenceType(className);
		if (clsType != null) {
			LOGGER.info("class already loaded");
			return new ClassWrapper(clsType.classObject(), this.currentThread);

		} else {
			LOGGER
					.info("class not loaded, attempting to load class from external APK using DexClassLoader: "
							+ className);

			ClassLoaderReference parentLoader = this
					.getBootClassLoader(this.currentThread);

			ObjectReference dexLoader = (ObjectReference) this
					.getDexClassLoader(this.currentThread, dexPath, dexOpt,
							libPath, parentLoader);
			if (dexLoader != null) {
				LOGGER.info("got DexClassLoader instance: "
						+ dexLoader.type().name() + " for path: " + dexPath
						+ " with parentLoader: " + parentLoader.type().name());

				StringReference clsName = this.vmUtils.createString(className);
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

	
	

}
