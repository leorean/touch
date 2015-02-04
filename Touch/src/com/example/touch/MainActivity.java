package com.example.touch;

import com.example.touch.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import android.support.v7.app.ActionBarActivity;
import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.UUID;

public class MainActivity extends Activity
{
	
	//BT MULTIMETER
	//00:1A:7D:16:46:C5
	
	public UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); 
	
	public static String MULTIMETER = "00:1A:7D:16:46:C5";
	
	public static String TAG = "TOUCH";
	
	private ListView lv;
	
	private BluetoothDevice device;
	private InputStream is;
	private OutputStream os;
	
	// private Button On,Off,Visible,list;
	private BluetoothAdapter BA;
	
	private ConnectThread connectThread;
	private ConnectedThread connectedThread;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//----touchscreen stuff
		
		//TODO: hier weiter
		
		View v = (View) findViewById(R.id.content);
		v.setOnTouchListener(
				new OnTouchListener() {
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						if(event.getAction() == MotionEvent.ACTION_MOVE){
							int x =(int)  event.getRawX();
							int y =(int)  event.getRawY();
							Log.v("motion",String.valueOf(x) + ":" + String.valueOf(y));
						}      
						return true;
					}
				});
		
		final TextView tvOhm = (TextView) findViewById(R.id.tvOhm);
		tvOhm.setText("initialising...");
		
		//----bluetooth stuff
		
		BA = BluetoothAdapter.getDefaultAdapter();
		if (BA == null)
			Log.v(TAG,"Device does not support Bluetooth");
	
		if (BA.isEnabled())
			device = getFromAdapter();
		else
			Toast.makeText(getApplicationContext(),"Please make sure Bluetooth is turned on and the Device is paired",Toast.LENGTH_LONG).show();
		
		if (device != null)
		{
			connectThread = new ConnectThread(device);
			connectThread.start();
		}
		
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
				try
				{
					mmSocket.close();
				} catch (IOException closeException)
				{
					Log.v(TAG,"Unable to connect socket.");
				}
				return;
			}
			Log.v(TAG, "Starting a connection..");
			connectedThread = new ConnectedThread(mmSocket);
			connectedThread.start();
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
			} catch (IOException e) { }
			
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
					Thread.sleep(200);
					//TODO: rausfinden warum das nicht geht...
					Log.v("stream","test");
					bytes = mmInStream.read(buffer,0,buffer.length);
					Log.v("stream","wtf");
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
				} catch (InterruptedException e)
				{
					Log.v(TAG,"interrupted");
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
				case 1:
					String writeMessage = new String(writeBuf);
					writeMessage = writeMessage.substring(begin, end);
				break;
			}
		}
	};
	
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
