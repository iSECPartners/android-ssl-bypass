/**
 * 
 */
package com.isecpartners.android.jdwp.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.isecpartners.android.jdwp.ClassLoaderUtils;
import com.isecpartners.android.jdwp.ClassWrapper;
import com.isecpartners.android.jdwp.Constants;
import com.isecpartners.android.jdwp.DalvikUtils;
import com.isecpartners.android.jdwp.DexClassLoaderNotFoundException;
import com.isecpartners.android.jdwp.LocationNotFoundException;
import com.isecpartners.android.jdwp.NoLoadClassMethodException;
import com.isecpartners.android.jdwp.ReferenceTypeNotFoundException;
import com.isecpartners.android.jdwp.pluginservice.AbstractJDIPlugin;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.MethodEntryEvent;

/**
 * @author nml
 * 
 */
public class TestJDIPlugin extends AbstractJDIPlugin {

	private final static org.apache.log4j.Logger LOGGER = Logger
			.getLogger(TestJDIPlugin.class.getName());
	private static final String FILTERS_FILENAME_KEY = "filters.filename";
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
	
	private ArrayList<String> filters = new ArrayList<String>();
	private String filtersFileName = null;
	private String filename;
	private String easySSLSocketFactory;
	private String externalSourceAPK;
	private String targetAppDataPath;
	private String externalTrustManagerClass;
	private String targetAppLibPath;
	private String sslPortString;
	private int sslPort;
	private String targetMainActivity;

	public TestJDIPlugin() throws FileNotFoundException, IOException {
		super(TestJDIPlugin.class.getName());
	}

	@Override
	public void handleEvent(Event event) {
		if (event instanceof MethodEntryEvent) {
			MethodEntryEvent meEvent = (MethodEntryEvent) event;
			ThreadReference tr = meEvent.thread();
			DalvikUtils dalvikUtils = new DalvikUtils(meEvent.virtualMachine(),tr);
			StackFrame fr;
			try {
				fr = tr.frames().get(0);
				Location loc = fr.location();
				Method method = loc.method();
				LOGGER.info(method.toString());
				LOGGER.info(method.variables());
				
				ClassLoaderUtils classLoaderUtils = dalvikUtils.getClassLoaderUtils();
				ClassWrapper wrappedClass = classLoaderUtils.loadExternalClassFromAPK(this.externalSourceAPK, this.targetAppDataPath, this.targetAppLibPath, this.externalTrustManagerClass, this.targetMainActivity);
				ObjectReference newInst = wrappedClass.newInstance();
				Value result = wrappedClass.invokeMethodOnInstance("getClass", Constants.NOARGS);
				LOGGER.info("GOT RESULTS oF CALLING TOSTRING: " + result);
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

		} else {
			LOGGER.info("received unexpected event type: " + event);
		}
		this.resumeEventSet();
	}

	@Override
	public void setupEvents() {
		this.filename = this.properties.getProperty(FILTERS_FILENAME_KEY,
				"filters");
		this.easySSLSocketFactory = this.properties
				.getProperty(EXTERNAL_EASYSSLSOCKETFACTORY_CLASS);
		this.externalSourceAPK = this.properties
				.getProperty(EXTERNAL_SOURCE_APK_PATH);
		this.targetAppDataPath = this.properties
				.getProperty(TARGET_APP_DATA_PATH);
		this.externalTrustManagerClass = this.properties
				.getProperty(EXTERNAL_TRUSTMANAGER_CLASS);
		this.targetAppLibPath = this.properties
				.getProperty(TARGET_APP_LIB_PATH);
		this.sslPortString = this.properties
				.getProperty(TARGET_APP_SSL_PORT);
		this.sslPort = Integer.parseInt(this.sslPortString);
		this.filtersFileName = this.basePath + File.separator + filename;
		LOGGER.info(this.filtersFileName);
		try {
			this.filters = this.readLines(this.filtersFileName);
			for (String f : filters) {
				this.createMethodEntryRequest(f);
			}
		} catch (IOException e) {
			LOGGER.info("could not get filters filename");
		} catch (LocationNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ReferenceTypeNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public ArrayList<String> readLines(String filename) throws IOException {
		FileReader fileReader = new FileReader(filename);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		ArrayList<String> lines = new ArrayList<String>();
		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			lines.add(line);
		}
		bufferedReader.close();
		return lines;
	}

}