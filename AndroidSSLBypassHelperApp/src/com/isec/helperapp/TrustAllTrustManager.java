package com.isec.helperapp;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import org.apache.http.conn.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import android.util.Log;

public class TrustAllTrustManager {
	
	protected static final String TAG = TrustAllTrustManager.class.getName();
	public static TrustManager[] trustAllCerts = new X509TrustManager[] { new X509TrustManager() {
		
		public void checkClientTrusted(
				java.security.cert.X509Certificate[] certs, String authType) {
			Log.i(TAG,"checkClientTrusted - always pass");
		}

		public void checkServerTrusted(
				java.security.cert.X509Certificate[] certs, String authType) {
			Log.i(TAG,"checkServerTrusted - always pass");
		}

		public X509Certificate[] getAcceptedIssuers() {
			Log.i(TAG,"getAcceptedIssuers");
			return null;
		}
	} };
	
	
	public static TrustManager[] getTrustManagers(){
		Log.i(TAG,"getting trust all trust manager");
		return trustAllCerts;
	}
	
	public static javax.net.ssl.SSLSocketFactory getSSLSocketFactory() throws NoSuchAlgorithmException, KeyManagementException{
		SSLContext inst = SSLContext.getInstance("SSL");
		inst.init(null, getTrustManagers(), null);
		return inst.getSocketFactory();
	}

	public static SSLContext getSSLContextInst() throws NoSuchAlgorithmException, KeyManagementException{
		SSLContext inst = SSLContext.getInstance("SSL");
		inst.init(null, getTrustManagers(), null);
		return inst;
	}
}
