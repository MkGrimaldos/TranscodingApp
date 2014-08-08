package com.thesis.transcodingapp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

import android.os.Environment;
import android.util.Log;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}
	
	public void transcode(View view) throws Exception{
		/*
		final String path = Environment.getExternalStorageDirectory() + "DCIM/Camera/"; //android.os.Environment.DIRECTORY_DCIM + "/Camera/";
		File source = new File(path + "source.mp4");
		File target = new File(path + "target.avi");
		*/
		
		Videokit vk = new Videokit();
		/*
		File images = new File(Environment.getExternalStorageDirectory(), "fun");
		images.mkdirs();
		for (int i=0; i<10; i++) {
			String filename = String.format("snap%04d.jpg", i);
			File dest = new File(images, filename);
			Log.i("Test", "Adding image at " + dest.getAbsolutePath());
			InputStream is = getInstrumentation().getContext().getAssets().open("image.jpg");
			BufferedOutputStream o = null;
			try {
				byte[] buff = new byte[10000];
				int read = -1;
				o = new BufferedOutputStream(new FileOutputStream(dest), 10000);
				while ((read = is.read(buff)) > -1) { 
					o.write(buff, 0, read);
				}
			} finally {
				is.close();
				if (o != null) o.close();  
				
			}
		}
		*/
		
		//videokit.initialise();
		
		//File file = new File(images.getAbsolutePath(), "snap0000.jpg");
		//assertTrue("File exist", file.exists());
		
		String input = new File(Environment.getExternalStorageDirectory(), "source.mp4").getAbsolutePath();//file.getAbsolutePath(); 
		Log.i("Test", "Let's set input to " + input);
//		videokit.setInputFile(input); 
		//deleteFile(Environment.getExternalStorageDirectory().getAbsolutePath(), "dest.avi");
		new File(Environment.getExternalStorageDirectory(), "dest.avi").delete();
		String output = new File(Environment.getExternalStorageDirectory(), "dest.avi").getAbsolutePath();
		Log.i("Test", "Let's set output to " + output);
//		videokit.setOutputFile(output); 
		
		vk.run(new String[]{
				"ffmpeg",
				"-i",
				input,
				output
		});
		
//		videokit.setSize(640,480); 
//		videokit.setFrameRate(5);
//		
//		videokit.encode();
	}
	
	private void deleteFile(String inputPath, String inputFile) {
	    try {
	        // delete the original file
	        new File(inputPath, inputFile).delete();  


	    }
	    /*
	   catch (FileNotFoundException fnfe1) {
	        Log.e("tag", fnfe1.getMessage());
	    }
	    */
	    catch (Exception e) {
	        Log.e("tag", e.getMessage());
	    }
	}
}
