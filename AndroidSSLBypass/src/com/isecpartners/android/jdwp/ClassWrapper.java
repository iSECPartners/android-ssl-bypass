package com.isecpartners.android.jdwp;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassObjectReference;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;

public class ClassWrapper {
	private final static Logger LOGGER = Logger
			.getLogger(ClassWrapper.class.getName());
	private ClassObjectReference cor;
	private ReferenceType reflectedType;
	private ReferenceType referenceType;
	private ThreadReference thread;
	private ObjectReference instance;

	public ClassWrapper(ClassObjectReference cor, ThreadReference tr) {
		this.setCor(cor);
		this.setReflectedType(cor.reflectedType());
		this.setReferenceType(cor.referenceType());
		this.thread = tr;
	}

	public ObjectReference getInstance(){
		if(this.instance == null){
			this.instance = this.newInstance();
		}
		return this.instance;
	}
	/**
	 * @return the cor
	 */
	public ClassObjectReference getCor() {
		return cor;
	}

	/**
	 * @param cor
	 *            the cor to set
	 */
	public void setCor(ClassObjectReference cor) {
		this.cor = cor;
	}

	/**
	 * @return the reflectedType
	 */
	public ReferenceType getReflectedType() {
		return reflectedType;
	}

	/**
	 * @param reflectedType
	 *            the reflectedType to set
	 */
	public void setReflectedType(ReferenceType reflectedType) {
		this.reflectedType = reflectedType;
	}

	/**
	 * @return the referenceType
	 */
	public ReferenceType getReferenceType() {
		return referenceType;
	}

	/**
	 * @param referenceType
	 *            the referenceType to set
	 */
	public void setReferenceType(ReferenceType referenceType) {
		this.referenceType = referenceType;
	}

	/*
	 * Create a new instance of a class using the debugger
	 */
	public ObjectReference newInstance() {
		try {
			Method newInst = this.referenceType.methodsByName("newInstance")
					.get(0);
			// ClassType classType = (ClassType)this.referenceType;
			// LOGGER.info("got classtype: " + classType.name());
			ClassType classType = (ClassType) this.reflectedType;
			ClassWrapper.LOGGER.info("got classtype: " + classType.name());
			return (ObjectReference) this.cor.invokeMethod(this.thread,
					newInst, Constants.NOARGS, 1);
		} catch (java.lang.IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		return null;
	}

	public Value invokeMethodOnInstance(String methodName,List<? extends Value> args ) throws InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException, InvocationException{
		ReferenceType type = this.referenceType;
		List<Method> meths = type.methodsByName(methodName);
		Method method = null;
		this.getInstance();
		if (meths.isEmpty()) {
			LOGGER.info("no methods found");
			type = this.reflectedType;
			meths = type.methodsByName(methodName);
		} else if (meths.size() == 1) {
			method = meths.get(0);
		} else {
			for (Method m : meths) {
				List<String> argNamesToMatch = this.getArgumentTypeNames(args);
				List<String> argNames = m.argumentTypeNames();
				if ((argNamesToMatch == null && argNames == null)
						|| (argNamesToMatch.equals(argNames))) {
					method = m;
					break;
				}
			}
		}
		if (method == null) {
			LOGGER.warn("no matching methods found for " + methodName);
			return null;
		}
		return this.instance.invokeMethod(this.thread, method, args, 1);
	}
	public Value invokeMethodOnType(ReferenceType type, String methodName,
			List<? extends Value> args) throws InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException, InvocationException {
		List<Method> meths = type.methodsByName(methodName);
		Method method = null;
		if (meths.isEmpty()) {
			LOGGER.info("no methods found");
			return null;
		} else if (meths.size() == 1) {
			method = meths.get(0);
		} else {
			for (Method m : meths) {
				List<String> argNamesToMatch = this.getArgumentTypeNames(args);
				List<String> argNames = m.argumentTypeNames();
				if ((argNamesToMatch == null && argNames == null)
						|| (argNamesToMatch.equals(argNames))) {
					method = m;
					break;
				}
			}
		}
		if (method == null) {
			LOGGER.warn("no matching methods found for " + methodName);
			return null;
		}
		return ((ClassType) type).invokeMethod(this.thread, method, args, 2);
	}

	private List<String> getArgumentTypeNames(List<? extends Value> args) {
		List<String> argNames = new ArrayList<String>();
		for (Value arg : args) {
			argNames.add(arg.type().name());
		}
		return argNames;
	}

	public Field getField(String fieldName) {
		Field field = this.cor.reflectedType().fieldByName(fieldName);
		if (field == null) {
			field = this.cor.referenceType().fieldByName(fieldName);
		}
		return field;
	}

	public Value getFieldValue(Field allowAll) {
		Value val = this.cor.reflectedType().getValue(allowAll);
		if (val == null) {
			val = this.cor.referenceType().getValue(allowAll);
		}
		return val;
	}

}
