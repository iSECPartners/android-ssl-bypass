package com.isecpartners.android.jdwp.pluginservice;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.log4j.Logger;

public class ClasspathUtils {

	private static Logger LOGGER = Logger.getLogger(ClasspathUtils.class
			.getName());

	@SuppressWarnings("rawtypes")
	private static final Class[] parameters = new Class[] { URL.class };

	/**
	 * Adds the jars in the given directory to classpath
	 * 
	 * @param directory
	 * @throws IOException
	 */
	public static void addDirToClasspath(File directory) throws IOException {
		if (directory.exists()) {
			ClasspathUtils.LOGGER.info("adding directory to classpath: "
					+ directory.getAbsolutePath());
			File[] files = directory.listFiles();
			for (int i = 0; i < files.length; i++) {
				File file = files[i];
				URI uri = file.toURI();
				String path = uri.getPath();
				if (path.endsWith(".jar")) {
					ClasspathUtils.addURL(file.toURI().toURL());
				}
			}
		} else {
			ClasspathUtils.LOGGER.warn("The directory \"" + directory
					+ "\" does not exist!");
		}
	}

	/**
	 * Add URL to CLASSPATH
	 * 
	 * @param u
	 *            URL
	 * @throws IOException
	 *             IOException
	 */
	public static void addURL(URL u) throws IOException {
		ClasspathUtils.LOGGER.info("adding to classpath");
		URLClassLoader sysLoader = (URLClassLoader) ClassLoader
				.getSystemClassLoader();
		URL urls[] = sysLoader.getURLs();

		ClasspathUtils.LOGGER.info(urls.length);
		if (ClasspathUtils.isURLInClassPath(urls, u)) {
			ClasspathUtils.LOGGER.info("already in classpath: " + u);
			return;
		}

		Class<URLClassLoader> sysclass = URLClassLoader.class;
		try {
			Method method = sysclass.getDeclaredMethod("addURL",
					ClasspathUtils.parameters);
			method.setAccessible(true);
			method.invoke(sysLoader, new Object[] { u });
		} catch (Throwable t) {
			t.printStackTrace();
			throw new IOException(
					"Error, could not add URL to system classloader");
		}
	}

	public static boolean isURLInClassPath(URL[] urls, URL u) {
		for (int i = 0; i < urls.length; i++) {
			if (urls[i].toString().equalsIgnoreCase(u.toString())) {
				ClasspathUtils.LOGGER.info("URL " + u
						+ " is already in the CLASSPATH");
				return true;
			}
		}
		return false;
	}
}