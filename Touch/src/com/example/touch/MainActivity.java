package com.example.touch;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

public class MainActivity extends Activity
{
	public UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); 

	public static String MULTIMETER = "00:1A:7D:16:46:C5";

	public static String TAG = "TOUCH";

	// Constants that indicate the current connection state
	private static final int STATE_NONE = 0; // we're doing nothing
	private static final int STATE_LISTEN = 1; // now listening for incoming
	// connections
	private static final int STATE_CONNECTING = 2; // now initiating an outgoing
	// connection
	private static final int STATE_CONNECTED = 3; // now connected to a remote
	// device
	private int state = STATE_NONE;

	private float minPressure = 0;
	private float maxPressure = 0;
	public static float pressure = 0; //THE PRESSURE

	//ui interaction stuff
	public static final int MESSAGE_READ = 1;

	//----bluetooth stuff----
	private BluetoothDevice device;
	private BluetoothAdapter BA;
	private ConnectThread connectThread;
	private ConnectedThread connectedThread;
	private AcceptThread acceptThread;
	private Timer timer;
	TimerTask task = null;
	private Parser mParser;

	
	/**
	 * set layout content activity_main.xml for drawing on custom view DrawView.java
	 * check bluetooth support and make sure it is enabled
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{	
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		setContentView(R.layout.activity_main);

		BA = BluetoothAdapter.getDefaultAdapter();
		if (BA == null)
			Log.v(TAG,"Device does not support Bluetooth");

		mParser = new Parser();

		if (BA.isEnabled())
			device = getFromAdapter();
		else
			Toast.makeText(getApplicationContext(),"Please make sure Bluetooth is turned on and the Device is paired",Toast.LENGTH_LONG).show();
	}
	
	/**
	 * method for calculating and receiving pressure over bluetooth
	 * DrawView access this methode
	 * @return result: normalized pressure from bluetooth smartmeter in kOhm
	 */
	private float calculatePressure(float p)
	{
		minPressure = Math.min(minPressure, p);
		maxPressure = Math.max(maxPressure, p);
		Log.v("minmax",String.valueOf(minPressure)+":"+String.valueOf(maxPressure));
		if (maxPressure != minPressure)
			return Math.max(Math.min(((p-minPressure)/(maxPressure-minPressure)),1),0);
		else
			return 0.0f;
	}
	
	//--------------------------------------------//
	//----bluetooth related code starting here----//
	//--------------------------------------------//
	
	private void connect() {

		// Cancel any thread attempting to make a connection
		if (state == STATE_CONNECTING) {
			if (connectThread != null) {
				connectThread.cancel();
				connectThread = null;
			}
		}

		// Cancel any thread currently running a connection
		if (connectedThread != null) {
			connectedThread.cancel();
			connectedThread = null;
		}

		// Start the thread to connect with the given device	
		if (device != null)
		{
			connectThread = new ConnectThread(device);
			connectThread.start();

			setState(STATE_CONNECTING);
		}
	}

	private void connected(BluetoothSocket socket,
			BluetoothDevice device) {
		// Cancel the thread that completed the connection
		if (connectThread != null) {
			connectThread.cancel();
			connectThread = null;
		}

		// Cancel any thread currently running a connection
		if (connectedThread != null) {
			connectedThread.cancel();
			connectedThread = null;
		}

		// Cancel the accept thread because we only want to connect to one
		// device
		if (acceptThread != null) {
			acceptThread.cancel();
			acceptThread = null;
		}

		// Start the thread to manage the connection and perform transmissions
		connectedThread = new ConnectedThread(socket);
		connectedThread.start();

		setState(STATE_CONNECTED);
	}
	
	@Override
	protected void onResume() {
		super.onResume();

		// Only if the state is STATE_NONE, do we know that we haven't
		// started already
		if (state == STATE_NONE) {
			// Start the Bluetooth chat services
			start();
		}
	}

	private void start() 
	{
		// Cancel any thread attempting to make a connection
		if (connectThread != null) {
			connectThread.cancel();
			connectThread = null;
		}

		// Cancel any thread currently running a connection
		if (connectedThread != null) {
			connectedThread.cancel();
			connectedThread = null;
		}

		// Start the thread to listen on a BluetoothServerSocket
		//        if (acceptThread == null) {
		//            acceptThread = new AcceptThread();
		//            acceptThread.start();
		//        }

		setState(STATE_LISTEN);

		//        try {
		//			Thread.sleep(5000);
		//		} catch (InterruptedException e) {
		//			Log.e("Interrupted", "Interrupted");
		//		}
		//        
		connect();

		if (timer == null) {
			timer = new Timer();
		}

		task = new CommandTask(mHandler);
		timer.scheduleAtFixedRate(task, 0, 10);
	}

	private void setState(int newState)
	{
		state = newState;
	}

	private BluetoothDevice getFromAdapter()
	{
		Set<BluetoothDevice> pairedDevices;
		pairedDevices = BA.getBondedDevices();

		if (pairedDevices == null)
			Log.v(TAG,"UH OH");

		if (pairedDevices.size() > 0)
		{
			for(BluetoothDevice bt : pairedDevices)
				if (bt.getAddress().toString().equals(MULTIMETER))
				{
					Log.v(TAG, bt.getAddress().toString());
					return bt;
				}
		}
		Log.v(TAG,"Device was not found.");
		return null;
	}

	/**
	 * Thread that sets up Communication
	 */
	private class ConnectThread extends Thread
	{
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;
		public ConnectThread(BluetoothDevice device)
		{
			BluetoothSocket tmp = null;
			mmDevice = device;
			try
			{
				tmp = device.createRfcommSocketToServiceRecord(mUUID);
			} catch (IOException e)
			{
				Log.v(TAG,"Unable to create RFCommSocket.");
			}
			mmSocket = tmp;
		}
		public void run()
		{
			BA.cancelDiscovery();
			try
			{
				mmSocket.connect();
			} catch (IOException connectException)
			{
				Log.v(TAG,"Unable to connect to socket.");
				try
				{
					mmSocket.close();
				} catch (IOException closeException)
				{
					Log.v(TAG,"Unable to close socket.");
				}

				this.start();
				return;
			}
			Log.v(TAG, "Starting a connection..");
			// Reset the ConnectThread because we're done
			synchronized (this) {
				connectThread = null;
			}

			// Start the connected thread
			connected(mmSocket, mmDevice);
		}

		public void cancel()
		{
			try
			{
				mmSocket.close();
			} catch (IOException e)
			{
				Log.v(TAG,"Unable to close socket.");
			}
		}
	};

	/**
	 * Thread that handles Communication stream
	 */
	private class ConnectedThread extends Thread
	{
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;
		public ConnectedThread(BluetoothSocket socket)
		{
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;
			try
			{
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e("stream","get failed");
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}
		public void run()
		{
			byte[] buffer = new byte[1024];
			int begin = 0;
			int bytes = 0;
			while (true)
			{
				try
				{
					//Thread.sleep(200);

					//Log.v("stream","test");
					bytes = mmInStream.read(buffer,0,buffer.length); //scheint hier zu "locken" weil nix vom gerät zurückkommt..
					//Log.v("stream","wtf");

					byte[] tempData = Utility.cutData(buffer, bytes);

					processData(tempData);

					mHandler.obtainMessage(MESSAGE_READ, tempData.length, -1, tempData).sendToTarget();
					/*
					bytes += mmInStream.read(buffer, bytes, buffer.length - bytes);
					for(int i = begin; i < bytes; i++)
					{
						if(buffer[i] == "#".getBytes()[0])
						{
							mHandler.obtainMessage(1, begin, i, buffer).sendToTarget();
							begin = i + 1;
							if(i == bytes - 1)
							{
								bytes = 0;
								begin = 0;
							}
						}
					}*/

				} catch (IOException e)
				{
					Log.v(TAG,"error " + e.getMessage());
					//break;
				}
			}
		}

		public void write(byte[] bytes)
		{
			try
			{
				mmOutStream.write(bytes);
			} catch (IOException e) { }
		}
		public void cancel()
		{
			try {
				mmSocket.close();
			} catch (IOException e) { }
		}
	}

	/**
	 * This thread runs while listening for incoming connections. It behaves
	 * like a server-side client. It runs until a connection is accepted (or
	 * until cancelled).
	 */
	private class AcceptThread extends Thread {
		// The local server socket
		private final BluetoothServerSocket mmServerSocket;

		public AcceptThread() {
			BluetoothServerSocket tmp = null;

			// Create a new listening server socket
			try {
				tmp = BA.listenUsingRfcommWithServiceRecord("SmartMeterActivity", mUUID);
			} catch (IOException e) {
				Log.e(TAG, "listen() failed", e);
			}
			mmServerSocket = tmp;
		}

		public void run() {
			BluetoothSocket socket = null;

			// Listen to the server socket if we're not connected
			while (state != STATE_CONNECTED) {
				try {
					// This is a blocking call and will only return on a
					// successful connection or an exception
					socket = mmServerSocket.accept();
				} catch (IOException e) {
					Log.e(TAG, "accept() failed", e);
					break;
				}

				// If a connection was accepted
				if (socket != null) {
					synchronized (this) {
						switch (state) {
						case STATE_LISTEN:
						case STATE_CONNECTING:
							// Situation normal. Start the connected thread.
							connected(socket, socket.getRemoteDevice());
							break;
						case STATE_NONE:
						case STATE_CONNECTED:
							// Either not ready or already connected. Terminate
							// new socket.
							try {
								socket.close();
							} catch (IOException e) {
								Log.e(TAG, "Could not close unwanted socket", e);
							}
							break;
						}
					}
				}
			}
		}

		public void cancel() {
			try {
				mmServerSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of server failed", e);
			}
		}
	}

	private class CommandTask extends TimerTask {

		Handler mHandler;

		public CommandTask(Handler mHandler) {
			this.mHandler = mHandler;
		}

		@Override
		public void run() {
			byte[] cmd = mParser.encoder("".getBytes(), mParser.START_BIT,
					mParser.STOP_BIT, mParser.resistor);
			MainActivity.this.write(cmd);
		}
	}

	Handler mHandler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			byte[] writeBuf = (byte[]) msg.obj;
			int begin = (int)msg.arg1;
			int end = (int)msg.arg2;
			switch(msg.what)
			{
			case MESSAGE_READ:
				byte[] readBuf = (byte[]) msg.obj;
				// int tempInt2 = 0;
				displayData(readBuf);
				break;
			}
		}
	};

	private void processData(byte[] data) 
	{
		mParser.add(data);
		if (mParser.isAvailable()) {
			byte[] temp = mParser.getSaveData();
			displayData(temp);
		} else {
			//Utility.logging(TAG, "data is inavailable");
		}
	}

	private void displayData(byte[] data) {

		if (data.length < 6) {
			return;
		}
		int key = data[1] & 0xff;
		int units = data[5] & 0xff;
		int HighReal = (data[2] << 8) & 0xff00;
		int lowReal = data[3] & 0xff;
		int less = data[4] & 0xff;

		String sign = "";
		String unit = "";
		switch (units) {
		/*
        case 1:
            sign = "+";
            unit = "mV";
            break;
        case 2:
            sign = "+";
            unit = "V";
            break;
        case 129:
            sign = "-";
            unit = "mV";
            break;
        case 130:
            sign = "-";
            unit = "V";
            break;

        case 4:
            sign = "+";
            unit = "A";
            break;
        case 132:
            sign = "-";
            unit = "A";
            break;

        case 3:
            sign = "+";
            unit = "mA";
            break;
        case 131:
            sign = "-";
            unit = "mA";
            break;
		*/
        case 5:
            sign = "";
            unit = "Ohm";
            break;
		case 6:
			sign = "";
			unit = "kOhm";
			break;
			/*
        case 7:
            sign = "";
            unit = "mOhm";
            break;
			 */
		}

		int combineData = HighReal + lowReal;
		String all = sign + combineData + "." + getDecade(less) + " " + unit;

		if (units == 5)//Ohm
    	{
    		pressure = 1 - calculatePressure(1/1000 * Float.valueOf(combineData + "." + getDecade(less)));
    	}
        if (units == 6) //kOhm
        {
        	pressure = 1 - calculatePressure(Float.valueOf(combineData + "." + getDecade(less)));
            	
        	//Log.i("Result", all);
            	
        }

	}

	private String getDecade(int original) {
		String back = String.valueOf(original);

		if (0 < original && original < 10) {
			back = "0" + back;
		}
		if (100 <= original) {
			back = String.valueOf(original / 10);
		}
		return back;
	}
	
	//-----------------------------------------------------------//
	//----testing stuff code under this comment not relevant----//
	//-----------------------------------------------------------//
	public void toggleBluetooth()
	{
		//just a test 
		//final TextView mTextView = (TextView) findViewById(R.id.tvOhm);
		//mTextView.setText("Some Text");

		if (BA != null)
		{
			if (!BA.isEnabled())
			{
				if (BA.enable())
					Toast.makeText(getApplicationContext(),"Turned bluetooth on",Toast.LENGTH_LONG).show();
			} else
			{
				if (BA.disable())
					Toast.makeText(getApplicationContext(),"Turned bluetooth off",Toast.LENGTH_LONG).show();
			}
		} else
		{
			Toast.makeText(getApplicationContext(),"Unable to detect bluetooth adapter!",Toast.LENGTH_LONG).show();
		}
	}

	public void write(byte[] out) {
		// Create temporary object
		ConnectedThread r;
		// Synchronize a copy of the ConnectedThread
		synchronized (this) {
			if (state != STATE_CONNECTED)
				return;
			r = connectedThread;
		}
		// Perform the write unsynchronized
		r.write(out);
	}

	public void pair()
	{
		Toast.makeText(getApplicationContext(),"Please pair the device manually.",Toast.LENGTH_LONG).show();
		/*
		device = getFromAdapter(BA);

		if (device == null)
		{
			BluetoothDevice d = BA.getRemoteDevice(MULTIMETER);
		    Intent intent = new Intent("android.bluetooth.device.action.PAIRING_REQUEST");
		    intent.putExtra("android.bluetooth.device.extra.DEVICE", d);
		    intent.putExtra("android.bluetooth.device.extra.PAIRING_VARIANT", 0);
		    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		    startActivity(intent);
		    Log.v(TAG,"Started pairing intent.");

		} else
			Toast.makeText(getApplicationContext(),"Device is already paired!",Toast.LENGTH_LONG).show();
		 */
	}

	public void close()
	{
		//connectThread.cancel();
		if (connectedThread != null)
			connectedThread.interrupt();
		System.exit(0);
	}

	public void visible()
	{
		Intent getVisible = new Intent(BluetoothAdapter.
				ACTION_REQUEST_DISCOVERABLE);
		startActivityForResult(getVisible, 0);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		//Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		//Handle action bar item clicks here. The action bar will
		//automatically handle clicks on the Home/Up button, so long
		//as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		switch (id)
		{
		case R.id.toggle_bluetooth:
			toggleBluetooth();
			break;
		case R.id.get_visible:
			visible();
			break;
		case R.id.pair_multimeter:
			pair();
			break;
		case R.id.close:
			close();
			break;
		}
		return super.onOptionsItemSelected(item);
	}
}
