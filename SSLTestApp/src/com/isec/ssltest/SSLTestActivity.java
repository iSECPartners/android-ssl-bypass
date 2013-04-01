package com.isec.ssltest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.http.conn.ssl.SSLSocketFactory;

import dalvik.system.DexClassLoader;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class SSLTestActivity extends Activity implements OnClickListener{

	private static final String TAG = SSLTestActivity.class.getName();
	private Button mButton = null;
	private TextView mTextView = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mButton = (Button) findViewById(R.id.button1);
        mTextView = (TextView) this.findViewById(R.id.text1);
        mButton.setOnClickListener(this); 
        //TODO this is set exactly why? maybe because we are connecting ssl with IP instead of hostname? that would be neat
    	StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX);
		StrictMode.setVmPolicy(StrictMode.VmPolicy.LAX);
	}
	
	private InputStream testHttpsUrlPinned() throws IOException, NoSuchAlgorithmException, CertificateException, KeyManagementException, KeyStoreException {
		URL url = new URL("https://10.0.2.2");
		Context context = this.getApplicationContext();
		AssetManager assetManager = context.getAssets();
		
		  InputStream keyStoreInputStream = assetManager.open("tentwo.store");
		  KeyStore trustStore = KeyStore.getInstance("BKS");

		  trustStore.load(keyStoreInputStream, "password".toCharArray());

		  TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
		  tmf.init(trustStore);
		  
		  SSLContext sslContext = SSLContext.getInstance("TLS");
		  sslContext.init(null, tmf.getTrustManagers(), null);
		  
		  //HttpsURLConnection.setDefaultHostnameVerifier(SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);
		  //for now for testing
		  HttpsURLConnection.setDefaultHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		  HttpsURLConnection urlConnection = (HttpsURLConnection)url.openConnection();
		  urlConnection.setSSLSocketFactory(sslContext.getSocketFactory());
		  Log.i(TAG,"hostnameverifier is: " + urlConnection.getHostnameVerifier().getClass().getName());

		  return urlConnection.getInputStream();
	}

	public void onClick(View arg0) {
		try {
			InputStream is = this.testHttpsUrlPinned();
			Log.i(TAG, "made SSL connection!");
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line = null;
			while( (line = br.readLine()) != null){
				mTextView.append(line);
				Log.i(TAG,line);
			}
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
