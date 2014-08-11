package com.thesis.transcodingapp;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Calendar;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
//import com.thesis.offloadinglibrary.Util;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

public class MainActivity extends Activity {

	// If OPTION == 1 we assume file is on Server
	// If OPTION == 2 we assume file will be sent by the Client
	// Check Server application for consistency purposes
	private static final int OPTION = 2;

	static long beginning = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		new File(Environment.getExternalStorageDirectory() + "/battery.txt")
				.delete();
		BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
			File battery = new File(Environment.getExternalStorageDirectory()
					+ "/battery.txt");
			BufferedWriter out;
			Calendar cal;
			int scale = -1;
			int level = -1;
			int voltage = -1;
			int temp = -1;

			@Override
			public void onReceive(Context context, Intent intent) {

				try {
					out = new BufferedWriter(new FileWriter(battery, true));
					cal = Calendar.getInstance();

					level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
					scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
					temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,
							-1);
					voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE,
							-1);
					Log.e("BatteryManager", "level is " + level + "/" + scale
							+ ", temp is " + temp + ", voltage is " + voltage);
					out.write(cal.get(Calendar.HOUR_OF_DAY) + ":"
							+ cal.get(Calendar.MINUTE) + ":"
							+ cal.get(Calendar.SECOND) + "."
							+ cal.get(Calendar.MILLISECOND) + "\tlevel "
							+ level + "/" + scale + "\ttemp " + temp
							+ "\tvoltage " + voltage + "\n");

				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						scanFile(Environment.getExternalStorageDirectory()
								+ "/battery.txt");
						out.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		};
		IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		registerReceiver(batteryReceiver, filter);
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
			new AutoDecideTask().execute(
					Environment.getExternalStorageDirectory() + "/source.mp4",
					Environment.getExternalStorageDirectory() + "/target.avi");
		}
	}

	private class TranscodingTask extends AsyncTask<String, Void, Integer> {

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

			long end = System.nanoTime();

			System.out.println("Time elapsed: " + (end - beginning) + "ns");

			File battery = new File(Environment.getExternalStorageDirectory()
					+ "/battery.txt");
			try {
				BufferedWriter out = new BufferedWriter(new FileWriter(battery,
						true));
				out.write("Time elapsed: " + (end - beginning) + "ns\n");
				scanFile(Environment.getExternalStorageDirectory()
						+ "/battery.txt");
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

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
					//System.out.println(count);
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

			long end = System.nanoTime();

			System.out.println("Time elapsed: " + (end - beginning) + "ns");

			File battery = new File(Environment.getExternalStorageDirectory()
					+ "/battery.txt");
			try {
				BufferedWriter out = new BufferedWriter(new FileWriter(battery,
						true));
				out.write("Time elapsed: " + (end - beginning) + "ns\n");
				scanFile(Environment.getExternalStorageDirectory()
						+ "/battery.txt");
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

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

			long end = System.nanoTime();

			System.out.println("Time elapsed: " + (end - beginning) + "ns");

			File battery = new File(Environment.getExternalStorageDirectory()
					+ "/battery.txt");
			try {
				BufferedWriter out = new BufferedWriter(new FileWriter(battery,
						true));
				out.write("Time elapsed: " + (end - beginning) + "ns\n");
				scanFile(Environment.getExternalStorageDirectory()
						+ "/battery.txt");
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			Log.d("DebugTag", "This should appear at the very end.");

			dialog.dismiss();
		}

	}

	private class AutoDecideTask extends AsyncTask<String, Void, Integer> {

		private final ProgressDialog dialog = new ProgressDialog(
				MainActivity.this);

		protected void onPreExecute() {
			this.dialog.setMessage("Processing...");
			this.dialog.show();
		}

		@Override
		protected Integer doInBackground(String... params) {
			// Time predicted depending on file size (in s/MB)
			final double estimatedDeviceTranscodingSpMBPrediction = 3.604;
			// Predicted speedup server/device
			final double estimatedServerVideoTranscodingRatioPrediction = 0.2189;
			// Predicted target.avi size reduction ratio
			final double estimatedReductionTranscodeVideoSize = 0.2262;
			// File size in MB
			double fileSize = new File(params[0]).length()/1000000.0;
			Log.d("DebugTag", "File size: " + fileSize);
			// Bandwidth in KB/s
			float bandwidth = getBandwidth()/1000.0f;
			// Predicted execution time on device
			double devicePerformancePrediction;
			// Predicted execution time on server
			double serverPerformancePrediction;

			devicePerformancePrediction = estimatedDeviceTranscodingSpMBPrediction
					* fileSize;

			serverPerformancePrediction = (fileSize / bandwidth)
					+ ((estimatedDeviceTranscodingSpMBPrediction * estimatedServerVideoTranscodingRatioPrediction) * fileSize)
					+ ((fileSize * estimatedReductionTranscodeVideoSize) / bandwidth);
			
			if(devicePerformancePrediction < serverPerformancePrediction) {
				return 1;
			} else {
				return 2;
			}
		}

		protected void onPostExecute(Integer res) {
			TextView info = (TextView) findViewById(R.id.infoTextView);

			dialog.dismiss();
			
			if (res == 1) {
				info.setText("Transcoding on Device!");
				new TranscodingTask().execute(
						Environment.getExternalStorageDirectory() + "/source.mp4",
						Environment.getExternalStorageDirectory() + "/target.avi");
			} else if (res == 2) {
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
			}
		}

	}

	private float getBandwidth() {
		// Download your image
		long startTime = System.nanoTime();
		HttpGet httpRequest;
		float bandwidth = -1;

		try {
			httpRequest = new HttpGet(
					new URL(
							"http://img3.wikia.nocookie.net/__cb20090802012207/aliens/images/1/19/Dilbert03.gif")
							.toURI());

			HttpClient httpClient = new DefaultHttpClient();
			HttpResponse response;
			response = (HttpResponse) httpClient.execute(httpRequest);
			long endTime = System.nanoTime();

			HttpEntity entity = response.getEntity();
			BufferedHttpEntity bufHttpEntity;
			bufHttpEntity = new BufferedHttpEntity(entity);

			// You can re-check the size of your file
			/* final */long contentLength = bufHttpEntity.getContentLength();

			Log.d("DebugTag", "Size of the file: " + contentLength / 1000.0f
					+ "KB");

			// Log
			Log.d("DebugTag", "[BENCHMARK] Dowload time: "
					+ (endTime - startTime) + "ns");

			// Bandwidth : size(KB)/time(s)
			bandwidth = ((float) contentLength / (endTime - startTime)) * 1000000.0f;

			Log.d("DebugTag", "Bandwidth: " + bandwidth + "KB/s");

		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		return bandwidth;
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
