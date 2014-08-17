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
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import android.util.Log;

public class Util {
	
	final static String IP = "138.250.194.200";
	final static int PORT = 8888;

	public static void receiveFile(String path) {
		Socket socket = null;
		DataOutputStream dataOutputStream = null;
		DataInputStream dataInputStream = null;
		OutputStream output = null;

		Log.d("DebugTag", "Button clicked, still not connection...");

		try {
			Log.d("DebugTag", "Connecting...");

			socket = new Socket(IP, PORT);

			Log.d("DebugTag", "Connected");

			int bufferSize = 0;

			Log.d("DebugTag", "Receiving data...");

			bufferSize = socket.getReceiveBufferSize();
			dataInputStream = new DataInputStream(socket.getInputStream());
			String fileName = dataInputStream.readUTF();
			Log.d("DebugTag", fileName);
			output = new FileOutputStream(path + "/" + fileName);
			byte[] buffer = new byte[bufferSize];
			int count;
			while ((count = dataInputStream.read(buffer)) > 0) {
				output.write(buffer, 0, count);
			}

			Log.d("DebugTag", "Received");

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (dataOutputStream != null) {
				try {
					dataOutputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (dataInputStream != null) {
				try {
					dataInputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void sendReceiveFile(String path) {
		Socket socket = null;
		DataOutputStream dataOutputStream = null;
		DataInputStream dataInputStream = null;
		OutputStream output = null;

		Log.d("DebugTag", "Button clicked, still not connection...");

		try {
			Log.d("DebugTag", "Connecting...");

			socket = new Socket(IP, PORT);

			Log.d("DebugTag", "Connected");

			// Sending
			Log.d("DebugTag", "Sending data...");
			File source = new File(path + "/source.mp4");

			byte[] bufferSend = new byte[8192];
			BufferedInputStream bis = new BufferedInputStream(
					new FileInputStream(source));
			dataInputStream = new DataInputStream(bis);

			dataOutputStream = new DataOutputStream(socket.getOutputStream());
			dataOutputStream.writeUTF(source.getName());
			int count;
			while ((count = dataInputStream.read(bufferSend)) > 0) {
				dataOutputStream.write(bufferSend, 0, count);
			}

			Log.d("DebugTag", "Sent");

			Log.d("DebugTag", "Closing socket connection");

			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			Log.d("DebugTag", "Socket connection closed");

			// Receiving
			Log.d("DebugTag", "Connecting...");

			socket = new Socket(IP, PORT);

			Log.d("DebugTag", "Connected");

			int bufferSize = 0;

			Log.d("DebugTag", "Receiving data...");

			bufferSize = socket.getReceiveBufferSize();
			dataInputStream = new DataInputStream(socket.getInputStream());
			String fileName = dataInputStream.readUTF();
			Log.d("DebugTag", fileName);
			output = new FileOutputStream(path + "/" + fileName);
			byte[] bufferReceive = new byte[bufferSize];
			while ((count = dataInputStream.read(bufferReceive)) > 0) {
				output.write(bufferReceive, 0, count);
			}

			Log.d("DebugTag", "Received");

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (dataOutputStream != null) {
				try {
					dataOutputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (dataInputStream != null) {
				try {
					dataInputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static boolean autoDecide(String path) {
		// Time predicted depending on file size (in s/MB)
		final double estimatedDeviceTranscodingSpMBPrediction = 4.112; //3.604;
		// Predicted speedup server/device
		final double estimatedServerVideoTranscodingRatioPrediction = 0.2233; //0.2189;
		// Predicted target.avi size reduction ratio
		final double estimatedReductionTranscodeVideoSize = 0.2046; //0.2262;
		// File size in MB
		double fileSize = new File(path).length() / 1000000.0;
		Log.d("DebugTag", "File size: " + fileSize + "MB");
		// Bandwidth in MB/s
		float bandwidth = getBandwidth() / 1000.0f;
		// Predicted execution time on device
		double devicePerformancePrediction;
		// Predicted execution time on server
		double serverPerformancePrediction;

		devicePerformancePrediction = estimatedDeviceTranscodingSpMBPrediction
				* fileSize;

		serverPerformancePrediction = (fileSize / bandwidth)
				+ ((estimatedDeviceTranscodingSpMBPrediction * estimatedServerVideoTranscodingRatioPrediction) * fileSize)
				+ ((fileSize * estimatedReductionTranscodeVideoSize) / bandwidth);

		return (devicePerformancePrediction < serverPerformancePrediction);

	}

	private static float getBandwidth() {
		// Download your image
		long startTime = System.nanoTime();
		HttpGet httpRequest;
		float bandwidth = -1;

		try {
			httpRequest = new HttpGet(
					new URL(
							// "http://theamericanceo.files.wordpress.com/2014/03/dilbertgoals1.jpg")
							"http://static.businessinsider.com/image/525460146bb3f7962b2c9bc4/image.jpg")
							//"http://media.npr.org/assets/img/2013/10/18/dilbert_custom-1753db19267dacbc24468f088770ef4893deea44.jpg")
							//"http://img3.wikia.nocookie.net/__cb20090802012207/aliens/images/1/19/Dilbert03.gif")
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
}
