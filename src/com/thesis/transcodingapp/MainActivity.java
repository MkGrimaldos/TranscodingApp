package com.thesis.transcodingapp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
					Environment.getExternalStorageDirectory() + "/target.avi");
		} else if (server.isChecked()) {
			info.setText("Transcoding on Server!");
			switch (OPTION) {
			case 1:
				// The server already has the file
				new ReceiveTask().execute(Environment
						.getExternalStorageDirectory().getAbsolutePath());
				break;
			case 2:
				// The device needs to send the file to the server
				new SendReceiveTask().execute(Environment
						.getExternalStorageDirectory().getAbsolutePath());
				break;
			default:
				break;
			}

		} else if (auto.isChecked()) {
			new AutoDecideTask().execute(Environment
					.getExternalStorageDirectory().getAbsolutePath());
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
			Util.receiveFile(params[0]);

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

			// System.out.println("This should appear at the very end.");

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
			Util.sendReceiveFile(params[0]);

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
			if (Util.autoDecide(params[0])) {
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
						Environment.getExternalStorageDirectory()
								+ "/source.mp4",
						Environment.getExternalStorageDirectory()
								+ "/target.avi");
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
