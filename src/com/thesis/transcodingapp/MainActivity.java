package com.thesis.transcodingapp;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

//import com.thesis.offloadinglibrary.Util;

import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

public class MainActivity extends Activity {

	// If OPTION == 1 we assume file is on Server
	// If OPTION == 2 we assume file will be sent by the Client
	// Check Server application for consistency purposes
	private static final int OPTION = 2;

	long beginning = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	public void transcode(View view) throws Exception {

		beginning = System.nanoTime();

		RadioButton device = (RadioButton) findViewById(R.id.deviceRadioButton);
		RadioButton server = (RadioButton) findViewById(R.id.serverRadioButton);
		RadioButton auto = (RadioButton) findViewById(R.id.autoRadioButton);
		TextView info = (TextView) findViewById(R.id.infoTextView);

		info.setText("Starting!");

		if (device.isChecked()) {
			info.setText("Transcoding on Device!");

			new TranscodingTask().execute(
					Environment.getExternalStorageDirectory() + "/source.mp4",
					Environment.getExternalStorageDirectory() + "/target.avi");// .get();
		} else if (server.isChecked()) {
			info.setText("Transcoding on Server!"); // Sending petition, etc
			switch (OPTION) {
			case 1:
				// The server already has the file
				new ReceiveTask().execute(Environment
						.getExternalStorageDirectory().getAbsolutePath());// Long.toString(beginning));
				// new Util().receiveFile(beginning);
				break;
			case 2:
				// The device needs to send the file to the server
				new SendReceiveTask().execute(Environment
						.getExternalStorageDirectory().getAbsolutePath());
				// new Util().sendReceiveFile(beginning);
				break;
			default:
				break;
			}

		} else if (auto.isChecked()) {
			// Coming soon
		}
	}

	private class TranscodingTask extends AsyncTask<String, Integer, Integer> {

		private final ProgressDialog dialog = new ProgressDialog(
				MainActivity.this);

		protected void onPreExecute() {
			this.dialog.setMessage("Processing...");
			this.dialog.show();
		}

		@Override
		protected Integer doInBackground(String... params) {

			//
			System.gc();
			Videokit vk = new Videokit();

			String input = new File(params[0]).getAbsolutePath();
			Log.i("Test", "Let's set input to " + input);
			String output = new File(params[1]).getAbsolutePath();
			Log.i("Test", "Let's set output to " + output);

			vk.run(new String[] { "ffmpeg", "-y", "-i", input, output });

			vk = null;
			System.gc();

			return 1;// resul.toString();
		}

		protected void onPostExecute(Integer res) {
			scanFile(Environment.getExternalStorageDirectory() + "/target.avi");

			TextView info = (TextView) findViewById(R.id.infoTextView);
			info.setText("Complete!");

			System.out.println("Time elapsed: "
					+ (System.nanoTime() - beginning) + "ns");

			Log.d("DebugTag", "This should appear at the very end.");

			dialog.dismiss();
		}

	}

	private class ReceiveTask extends AsyncTask<String, Void, Integer> {
		private final ProgressDialog dialog = new ProgressDialog(
				MainActivity.this);

		protected void onPreExecute() {
			this.dialog.setMessage("Processing...");
			this.dialog.show();
		}

		@Override
		protected Integer doInBackground(String... params) {
			// new Util().receiveFile(params[0]);

			Socket socket = null;
			DataOutputStream dataOutputStream = null;
			DataInputStream dataInputStream = null;
			OutputStream output = null;

			// Log.d("DebugTag", "Button clicked, still not connection...");

			try {
				// Log.d("DebugTag", "Connecting...");

				socket = new Socket("138.250.194.200", 8888);

				// Log.d("DebugTag", "Connected");

				int bufferSize = 0;

				// Log.d("DebugTag", "Receiving data...");

				bufferSize = socket.getReceiveBufferSize();
				dataInputStream = new DataInputStream(socket.getInputStream());
				String fileName = dataInputStream.readUTF();
				// Log.d("DebugTag", fileName);
				output = new FileOutputStream(
				/* Environment.getExternalStorageDirectory() */params[0] + "/"
						+ fileName);
				byte[] buffer = new byte[bufferSize];
				int count;
				while ((count = dataInputStream.read(buffer)) > 0) {
					System.out.println(count);
					output.write(buffer, 0, count);
				}

				// Log.d("DebugTag", "Received");

			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if (socket != null) {
					try {
						socket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				if (dataOutputStream != null) {
					try {
						dataOutputStream.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				if (dataInputStream != null) {
					try {
						dataInputStream.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				if (output != null) {
					try {
						output.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

			return 1;// resul.toString();
		}

		protected void onPostExecute(Integer res) {
			scanFile(Environment.getExternalStorageDirectory() + "/target.avi");

			TextView info = (TextView) findViewById(R.id.infoTextView);
			info.setText("Complete!");

			System.out.println("Time elapsed: "
					+ (System.nanoTime() - beginning) + "ns");

			System.out.println("This should appear at the very end.");

			dialog.dismiss();
		}

	}

	private class SendReceiveTask extends AsyncTask<String, Void, Integer> {
		private final ProgressDialog dialog = new ProgressDialog(
				MainActivity.this);

		protected void onPreExecute() {
			this.dialog.setMessage("Processing...");
			this.dialog.show();
		}

		@Override
		protected Integer doInBackground(String... params) {
			// new Util().sendReceiveFile(params[0]);

			Socket socket = null;
			DataOutputStream dataOutputStream = null;
			DataInputStream dataInputStream = null;
			OutputStream output = null;

			Log.d("DebugTag", "Button clicked, still not connection...");

			try {
				Log.d("DebugTag", "Connecting...");

				socket = new Socket("138.250.194.200", 8888);

				Log.d("DebugTag", "Connected");

				// Sending
				Log.d("DebugTag", "Sending data...");
				File source = new File(
						Environment.getExternalStorageDirectory()
								+ "/source.mp4");

				byte[] bufferSend = new byte[8192];
				BufferedInputStream bis = new BufferedInputStream(
						new FileInputStream(source));
				dataInputStream = new DataInputStream(bis);

				dataOutputStream = new DataOutputStream(
						socket.getOutputStream());
				dataOutputStream.writeUTF(source.getName());
				int count;
				while ((count = dataInputStream.read(bufferSend)) > 0) {
					// System.out.println(count);
					dataOutputStream.write(bufferSend, 0, count);
				}

				Log.d("DebugTag", "Sent");

				Log.d("DebugTag", "Closing socket connection");

				try {
					socket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				Log.d("DebugTag", "Socket connection closed");

				// Receiving

				Log.d("DebugTag", "Connecting...");

				socket = new Socket("138.250.194.200", 8888);

				Log.d("DebugTag", "Connected");

				int bufferSize = 0;

				Log.d("DebugTag", "Receiving data...");

				bufferSize = socket.getReceiveBufferSize();
				dataInputStream = new DataInputStream(socket.getInputStream());
				String fileName = dataInputStream.readUTF();
				Log.d("DebugTag", fileName);
				output = new FileOutputStream(
						Environment.getExternalStorageDirectory() + "/"
								+ fileName);
				byte[] bufferReceive = new byte[bufferSize];
				// int count;
				while ((count = dataInputStream.read(bufferReceive)) > 0) {
					output.write(bufferReceive, 0, count);
				}

				Log.d("DebugTag", "Received");

			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if (socket != null) {
					try {
						socket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				if (dataOutputStream != null) {
					try {
						dataOutputStream.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				if (dataInputStream != null) {
					try {
						dataInputStream.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				if (output != null) {
					try {
						output.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

			return 1;// resul.toString();
		}

		protected void onPostExecute(Integer res) {
			scanFile(Environment.getExternalStorageDirectory() + "/target.avi");

			TextView info = (TextView) findViewById(R.id.infoTextView);
			info.setText("Complete!");

			System.out.println("Time elapsed: "
					+ (System.nanoTime() - beginning) + "ns");

			Log.d("DebugTag", "This should appear at the very end.");

			dialog.dismiss();
		}

	}

	private void scanFile(String path) {
		/*
		 * TextView info = (TextView) findViewById(R.id.infoTextView);
		 * info.setText("Complete!");
		 */

		MediaScannerConnection.scanFile(MainActivity.this,
				new String[] { path }, null,
				new MediaScannerConnection.OnScanCompletedListener() {

					public void onScanCompleted(String path, Uri uri) {
						Log.i("TAG", "Finished scanning " + path);
					}
				});
	}
}
