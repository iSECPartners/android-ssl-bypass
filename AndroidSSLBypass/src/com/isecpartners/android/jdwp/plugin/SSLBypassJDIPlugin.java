package com.isecpartners.android.jdwp.plugin;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.isecpartners.android.jdwp.ClassLoaderUtils;
import com.isecpartners.android.jdwp.ClassWrapper;
import com.isecpartners.android.jdwp.Constants;
import com.isecpartners.android.jdwp.DalvikUtils;
import com.isecpartners.android.jdwp.DexClassLoaderNotFoundException;
import com.isecpartners.android.jdwp.LocationNotFoundException;
import com.isecpartners.android.jdwp.NoLoadClassMethodException;
import com.isecpartners.android.jdwp.pluginservice.AbstractJDIPlugin;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.request.EventRequest;

public class SSLBypassJDIPlugin extends AbstractJDIPlugin {
	public static final String CREATE_CONNECTION = "org.apache.http.impl.conn.DefaultClientConnectionOperator.createConnection";

	/* User Configurable */

	public static final String createConnection = "createConnection";
	public static final String DEFAULT_CLIENT_CONN_OP = "org.apache.http.impl.conn.DefaultClientConnectionOperator";

	public static final String EXTERNAL_DATA_CACHE_PATH = "external.data.cache.path";

	public static final String EXTERNAL_EASYSSLSOCKETFACTORY_CLASS = "external.easysslsocketfactory.class";

	public static final String EXTERNAL_SOURCE_APK_PACKAGE_NAME = "external.source.apk.package.name";

	public static final String EXTERNAL_SOURCE_APK_PATH = "external.source.apk.path";

	public static final String EXTERNAL_TRUSTMANAGER_CLASS = "external.trustmanager.class";

	public static final String HTTPS_URL_CONNECTION = "javax.net.ssl.HttpsURLConnection";
	
	public static final String TARGET_APP_DATA_PATH = "target.app.data.path";

	private static final String TARGET_APP_LIB_PATH = "target.app.lib.path";

	private static final String TARGET_APP_SSL_PORT = "target.app.ssl.port";
	
	private static final String TARGET_MAIN_ACTIVITY = "target.main.activity";

	/* User Configurable */

	public static String init = "<init>";
	public static final String HTTPS_URL_CONN_INIT = "javax.net.ssl.HttpsURLConnection.<init>";
	
	private final static org.apache.log4j.Logger LOGGER = Logger
			.getLogger(SSLBypassJDIPlugin.class.getName());
	public static final String OPEN_CONNECTION = "org.apache.http.impl.conn.DefaultClientConnectionOperator.openConnection";
	public static final String SCHEME = "org.apache.http.conn.scheme.Scheme";

	public static final String SCHEME_REGISTRY = "org.apache.http.conn.scheme.SchemeRegistry";

	public static final String SET_DEFAULT_HOST_NAME_VERIFIER = "javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier";

	public static final String SET_HOST_NAME_VERIFIER = "javax.net.ssl.HttpsURLConnection.setHostnameVerifier";

	public static final String SET_SSL_SOCKET_FACTORY = "javax.net.ssl.HttpsURLConnection.setSSLSocketFactory";

	public static final String setDefaultHostnameVerifier = "setDefaultHostnameVerifier";

	public static final String setHostnameVerifier = "setHostnameVerifier";

	public static final String setSSLSocketFactory = "setSSLSocketFactory";

	public static final String SF_VAR_NAME = "sf";

	private static final int SSL_PORT_DEFAULT = 443;

	public static final String SSL_SOCKET_FACTORY = "org.apache.http.conn.ssl.SSLSocketFactory";

	private static final String VERIFIER_VAR_NAME = "v";

	

	private String easySSLSocketFactory = null;

	private String externalSourceAPK = null;

	private String externalTrustManagerClass = null;

	private int sslPort = SSLBypassJDIPlugin.SSL_PORT_DEFAULT;

	private String sslPortString = null;

	private String targetAppDataPath = null;

	private String targetAppLibPath = null;

	private String targetMainActivity;

	public SSLBypassJDIPlugin() throws FileNotFoundException, IOException {
		super(SSLBypassJDIPlugin.class.getName());
		
	}

	private void bpEntryEvent(BreakpointEvent event) {
		ThreadReference tr = event.thread();
		DalvikUtils vmUtils = new DalvikUtils(event.virtualMachine(), event.thread());
		StackFrame fr = null;
		try {
			// TODO should be abstracted away in VMUtils
			fr = tr.frames().get(0);
			Location loc = fr.location();
			Method method = loc.method();
			String name = method.name();
			SSLBypassJDIPlugin.LOGGER.info("hit breakpont: " + name);
			
			if (name.equals(SSLBypassJDIPlugin.HTTPS_URL_CONN_INIT)) {
				this.setAllowAllHostNameVerifier(vmUtils);
				
			} else if (name.equals(SSLBypassJDIPlugin.setHostnameVerifier)
					|| name.equals(SSLBypassJDIPlugin.setDefaultHostnameVerifier)) {

				SSLBypassJDIPlugin.LOGGER
						.info("attempting to set host name verifier to allow all");
				this.replaceHostNameVerifier(vmUtils);

			} else if (name.equals(SSLBypassJDIPlugin.setSSLSocketFactory)) {

				SSLBypassJDIPlugin.LOGGER
						.info("attempting to set socket factory to trust all");
				if(this.replaceTrustManager(vmUtils)){
					LOGGER.info("success replacing TM");
				} else {
					LOGGER.info("could not replace TM");
				}

			} else if (name.equals(SSLBypassJDIPlugin.createConnection)) {
				SSLBypassJDIPlugin.LOGGER
						.info("attempting to set allow all host name verifier");
				LOGGER.info("*******NOTE: this may take a long time, especially on the first run!");
				this.setAllowAllHostNameVerifier(vmUtils);
				SSLBypassJDIPlugin.LOGGER
						.info("attempting to set scheme registry to trust all scheme");
				this.registerNewScheme(vmUtils);
			}
		} catch (IncompatibleThreadStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AbsentInformationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidTypeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotLoadedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DexClassLoaderNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoLoadClassMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		LOGGER.info("resuming event set");
		this.resumeEventSet();
	}

	public Value getAllowAllHostNameVerifier(DalvikUtils vmUtils, StackFrame fr) {
		return vmUtils.getFieldValue(SSLBypassJDIPlugin.SSL_SOCKET_FACTORY,"ALLOW_ALL_HOSTNAME_VERIFIER");
	}

	public ObjectReference getNewScheme(DalvikUtils vmUtils)
			throws InvalidTypeException, ClassNotLoadedException,
			IncompatibleThreadStateException, InvocationException,
			DexClassLoaderNotFoundException, NoLoadClassMethodException {

		ClassType scheme = (ClassType) vmUtils.findReferenceType(SSLBypassJDIPlugin.SCHEME);
		ClassType ezssl = (ClassType) vmUtils.findReferenceType(this.easySSLSocketFactory);
		ThreadReference tr = vmUtils.getCurrentThread();
		ObjectReference ezsslObj = null;
		ClassLoaderUtils classLoaderUtils = vmUtils.getClassLoaderUtils();
		if (ezssl == null) {
			SSLBypassJDIPlugin.LOGGER
					.info("loading external class with DexClassLoader");
			ezssl = (ClassType) classLoaderUtils.loadExternalClassFromAPK(this.externalSourceAPK,
					this.targetAppDataPath, this.targetAppLibPath,
					this.easySSLSocketFactory, targetMainActivity);

		}

		Method init = ezssl.methodsByName("<init>").get(0);
		ezsslObj = ezssl.newInstance(tr, init, new ArrayList<Value>(), 0);

		SSLBypassJDIPlugin.LOGGER.info("got EasySSLSocketFactory Object: "
				+ ezsslObj);
		StringReference https = vmUtils.createString("https");

		IntegerValue port = vmUtils.createInt(this.sslPort);
		List<Value> vals = new ArrayList<Value>();
		vals.add(https);
		vals.add(ezsslObj);
		vals.add(port);
		List<Method> inits = scheme.methodsByName("<init>");
		Method i = inits.get(0);
		SSLBypassJDIPlugin.LOGGER.info(i);
		return scheme.newInstance(tr, i, vals, 0);
	}

	@Override
	public void handleEvent(Event nextEvent) {
		if (nextEvent instanceof BreakpointEvent) {
			this.bpEntryEvent((BreakpointEvent) nextEvent);
		} else {
			this.resumeEventSet();
		}
	}

	

	public void registerNewScheme(DalvikUtils vmUtils) throws InvalidTypeException,
			ClassNotLoadedException, IncompatibleThreadStateException,
			InvocationException, DexClassLoaderNotFoundException,
			NoLoadClassMethodException {
		ThreadReference tr = vmUtils.getCurrentThread();
		// ClassType schemeRegistryType = (ClassType) vmUtils
		// .findRefType(SCHEME_REGISTRY);
		ObjectReference schemeObj = this.getNewScheme(vmUtils);
		StackFrame fr0 = tr.frames().get(0);
		ObjectReference obj = fr0.thisObject();
		ReferenceType objr = obj.referenceType();
		ObjectReference schemeRegO = (ObjectReference) obj.getValue(objr
				.fieldByName("schemeRegistry"));
		SSLBypassJDIPlugin.LOGGER.info(schemeRegO);
		ReferenceType schemeRegR = schemeRegO.referenceType();
		Method register = schemeRegR.methodsByName("register").get(0);
		List<Value> args = new ArrayList<Value>();
		args.add(schemeObj);
		Value result = schemeRegO.invokeMethod(tr, register, args, 0);
		SSLBypassJDIPlugin.LOGGER.info(result);
	}

	public void replaceHostNameVerifier(DalvikUtils vmUtils)
			throws IncompatibleThreadStateException,
			AbsentInformationException, InvalidTypeException,
			ClassNotLoadedException {
		ThreadReference tr = vmUtils.getCurrentThread();
		// LOGGER.info(tr.frames().get(0).thisObject());
	
		StackFrame fr = tr.frames().get(0);
		Value aahv = this.getAllowAllHostNameVerifier(vmUtils, fr);
		vmUtils.setLocalVariableValue(0, VERIFIER_VAR_NAME, aahv);
	}
	

	public boolean replaceTrustManager(DalvikUtils vmUtils)
			throws InvalidTypeException, ClassNotLoadedException,
			IncompatibleThreadStateException, InvocationException,
			AbsentInformationException, DexClassLoaderNotFoundException,
			NoLoadClassMethodException {
		ClassLoaderUtils classLoaderUtils = vmUtils.getClassLoaderUtils();
		ClassWrapper classWrapper = classLoaderUtils.loadExternalClassFromAPK(this.externalSourceAPK, this.targetAppDataPath, this.targetAppLibPath, this.externalTrustManagerClass, targetMainActivity);
		ObjectReference newInstance = classWrapper.getInstance();
		LOGGER.info("new instance type: " + newInstance.referenceType().name());
		Value sf = classWrapper.invokeMethodOnType(newInstance.referenceType(), "getSSLSocketFactory", Constants.NOARGS);
		SSLBypassJDIPlugin.LOGGER.info("attempting to set new val: " + sf);
		return vmUtils.setLocalVariableValue(0,SSLBypassJDIPlugin.SF_VAR_NAME,
				sf);
	}

	public void setAllowAllHostNameVerifier(DalvikUtils vmUtils)
			throws InvalidTypeException, ClassNotLoadedException,
			IncompatibleThreadStateException, InvocationException {
		ThreadReference tr = vmUtils.getCurrentThread();
		ClassType httpsURLConn = (ClassType) vmUtils
				.findReferenceType(SSLBypassJDIPlugin.HTTPS_URL_CONNECTION);
		List<Method> sdhvm = httpsURLConn
				.methodsByName(SSLBypassJDIPlugin.setDefaultHostnameVerifier);
		Method sdhv = sdhvm.get(0);
		StackFrame fr = tr.frames().get(0);
		Value aahv = this.getAllowAllHostNameVerifier(vmUtils, fr);
		ArrayList<Value> args = new ArrayList<Value>();
		args.add(aahv);
		SSLBypassJDIPlugin.LOGGER
				.info("attempting to call setDefaultHostnameVerifier");
		httpsURLConn.invokeMethod(tr, sdhv, args, 0);
	}

	@Override
	public void setupEvents() {
		this.easySSLSocketFactory = this.properties
				.getProperty(SSLBypassJDIPlugin.EXTERNAL_EASYSSLSOCKETFACTORY_CLASS);
		this.externalSourceAPK = this.properties
				.getProperty(SSLBypassJDIPlugin.EXTERNAL_SOURCE_APK_PATH);
		this.targetAppDataPath = this.properties
				.getProperty(SSLBypassJDIPlugin.TARGET_APP_DATA_PATH);
		this.externalTrustManagerClass = this.properties
				.getProperty(SSLBypassJDIPlugin.EXTERNAL_TRUSTMANAGER_CLASS);
		this.targetAppLibPath = this.properties
				.getProperty(SSLBypassJDIPlugin.TARGET_APP_LIB_PATH);
		this.sslPortString = this.properties
				.getProperty(SSLBypassJDIPlugin.TARGET_APP_SSL_PORT);
		this.sslPort = Integer.parseInt(this.sslPortString);

		this.targetMainActivity = this.properties.getProperty(TARGET_MAIN_ACTIVITY);
		SSLBypassJDIPlugin.LOGGER.info("source: " + this.externalSourceAPK
				+ " target : " + this.targetAppDataPath + " appLib : "
				+ this.targetAppLibPath + " sourceTM : "
				+ this.externalTrustManagerClass);

		assert (this.easySSLSocketFactory != null)
				&& (this.externalSourceAPK != null)
				&& (this.targetAppDataPath != null)
				&& (this.targetAppLibPath != null)
				&& (this.externalTrustManagerClass != null);
		try {
			this.createBreakpointRequest(SSLBypassJDIPlugin.HTTPS_URL_CONN_INIT);
			this.createBreakpointRequest(SSLBypassJDIPlugin.SET_SSL_SOCKET_FACTORY);

			this.createBreakpointRequest(SSLBypassJDIPlugin.SET_HOST_NAME_VERIFIER);

			this.createBreakpointRequest(SSLBypassJDIPlugin.SET_DEFAULT_HOST_NAME_VERIFIER);

			this.createBreakpointRequest(SSLBypassJDIPlugin.CREATE_CONNECTION);
		} catch (LocationNotFoundException e) {
			LOGGER.error("could not setup events: " + e);
		}
	}

		

	@Override
	public void tearDownEvents() {
		for (EventRequest req : this.eventRequestList) {
			this.vmem.deleteEventRequest(req);
		}
		this.eventRequestList.clear();
		this.resumeEventSet();
	}

}