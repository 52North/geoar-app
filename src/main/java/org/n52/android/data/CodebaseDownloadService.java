package org.n52.android.data;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.ResultReceiver;

public class CodebaseDownloadService extends IntentService{

	public static final int PROGRESS_CB_UPDATE = 48484;
	
	public CodebaseDownloadService() {
		super("CodebaseDownloadService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String downloadURL = intent.getStringExtra("url");
		ResultReceiver receiver = (ResultReceiver) intent.getParcelableExtra("resultReceiver");
		try{
			URL url = new URL(downloadURL);
			URLConnection connection = url.openConnection();
			connection.connect();
			 
			int downloadSize = connection.getContentLength();
			
			InputStream input = new BufferedInputStream(url.openStream());
			OutputStream output = new FileOutputStream(Environment.getExternalStorageDirectory() + "/GeoAR/" + "wurst.apk");
			
			byte data[] = new byte[1024];
			long totalLength = 0;
			int in;
			// download the file
			while((in = input.read(data)) != -1){
				totalLength += in;
				Bundle args = new Bundle();
				args.putInt("progress_update", (int)(totalLength * 100 / downloadSize));
				receiver.send(PROGRESS_CB_UPDATE, args);
				output.write(data, 0 , in);
			}
			
			output.flush();
			output.close(); 
			input.close();
		} catch (IOException e) {
            e.printStackTrace();
        }
		
		Bundle args = new Bundle();
		args.putInt("progress_update", 100);
		receiver.send(PROGRESS_CB_UPDATE, args);
	}
	

}
