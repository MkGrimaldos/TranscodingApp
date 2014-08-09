package com.thesis.transcodingapp;

import java.io.File;

import com.thesis.offloadinglibrary.Util;

import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

public class MainActivity extends Activity {

	long beginning = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}
	
	public void transcode(View view) throws Exception{
		
		beginning = System.nanoTime();
		
		RadioButton device = (RadioButton) findViewById(R.id.deviceRadioButton);
		RadioButton server = (RadioButton) findViewById(R.id.serverRadioButton);
		RadioButton auto = (RadioButton) findViewById(R.id.autoRadioButton);
		TextView info = (TextView) findViewById(R.id.infoTextView);
		
		info.setText("Starting!");
		
		if (device.isChecked()){
			info.setText("Transcoding on Device!");
			new TranscodingTask().execute(Environment.getExternalStorageDirectory() + "/source.mp4", 
					Environment.getExternalStorageDirectory() + "/target.avi");
		} else if (server.isChecked()){
			info.setText("Transcoding on Server!"); //Sending petition, bla bla bla
			// The server already has the file
			new Util().receiveFile(beginning);
			scanFile(new File(Environment.getExternalStorageDirectory() + "/target.avi").getAbsolutePath());
			//System.out.println("Time elapsed: " + (System.nanoTime() - beginning) + "ns");
			//Sending the file
			
		} else if(auto.isChecked()) {
			// Coming soon
		}
	}
	
	private class TranscodingTask extends AsyncTask<String, Integer, Integer>{

		@Override
		protected Integer doInBackground(String... params) {
			
			//
			System.gc();
			Videokit vk = new Videokit();
			
			String input = new File(params[0]).getAbsolutePath();//file.getAbsolutePath(); 
			Log.i("Test", "Let's set input to " + input);
			String output = new File(params[1]).getAbsolutePath();
			Log.i("Test", "Let's set output to " + output);
			
			vk.run(new String[]{
					"ffmpeg",
					"-y",
					"-i",
					input,
					output
			});
			//
			
			scanFile(new File(params[1]).getAbsolutePath());
			
			vk = null;
			System.gc();
			
			return 1;//resul.toString();
		}
		
		protected void onPostExecute(Integer res) {
			TextView info = (TextView) findViewById(R.id.infoTextView);
			info.setText("Complete!");

			System.out.println("Time elapsed: " + (System.nanoTime() - beginning) + "ns");
		}
		
	}
	
	private void scanFile(String path) {

        MediaScannerConnection.scanFile(MainActivity.this,
                new String[] { path }, null,
                new MediaScannerConnection.OnScanCompletedListener() {

                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("TAG", "Finished scanning " + path);
                    }
                });
    }
}
