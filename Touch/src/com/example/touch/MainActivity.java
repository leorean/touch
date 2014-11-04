package com.example.touch;

import com.example.touch.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.Toast;
import java.util.UUID;

public class MainActivity extends Activity {

	//BT MULTIMETER
	//00:1A:7D:16:46:C5

	public UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); 
	//UUID.randomUUID();

	public static String MULTIMETER = "00:1A:7D:16:46:C5";

	private BluetoothDevice device;
	private InputStream is;
	private OutputStream os;

	// private Button On,Off,Visible,list;
	private BluetoothAdapter BA;
	private Set<BluetoothDevice>pairedDevices;
	private ListView lv;
	private int request_code_for_enabling_bt; //If >= 0, this code will be returned in onActivityResult() when the activity exits.

	private Thread thread;

	private ConnectThread mConnectThread;

	//this method is called in our thread
	public void cycle()
	{
		if (BA != null)
			BA.enable();

		//wait for bluetooth to be enabled, then do:
		if (BA.isEnabled())
		{
			BroadcastReceiver btReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {

					String action = intent.getAction();

					if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
						BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
						if (device.getBondState() == BluetoothDevice.BOND_BONDED)
						{
							// CONNECT
						}
					} else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
						BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
						// Discover new device
					}
				}
			};

			IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
			intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
			this.registerReceiver(btReceiver, intentFilter);


			Set<BluetoothDevice> pairedDevices = BA.getBondedDevices();

			for(BluetoothDevice bt : pairedDevices)
				if (bt.getAddress().equals(MULTIMETER))
					device = bt;

			if (device == null)
				Log.v("Error", "Device not found!");
			else
				Log.v("Device found", device.getAddress().toString());

			mConnectThread = new ConnectThread(device);
			mConnectThread.start();
			
			/*
			BluetoothSocket socket = null;

			try {
				socket = device.createRfcommSocketToServiceRecord(mUUID);
				Log.v("SOCKET",socket.toString());

				socket.connect();

				is = socket.getInputStream();
				os = socket.getOutputStream();


			} catch (IOException e) {
				Log.v("OOPS", e.getMessage());
				thread.interrupt();
			}
		 	*/
			//interrupt the thread once we have set up everything
			thread.interrupt();
		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		lv = (ListView)findViewById(R.id.listView1);

		try
		{
			BA = BluetoothAdapter.getDefaultAdapter();

			thread = new Thread(
					new Runnable()
					{ 
						public Runnable getInstance()
						{
							return this;
						}

						@Override
						public void run() {
							try
							{
								while (true)
								{
									cycle();
									Thread.sleep(200);
								}
							} catch (InterruptedException e)
							{
								Log.v("Thread", "Thread interrupted.");
								Thread.currentThread().interrupt();
							}
						}
					});

			thread.start();

			//TODO : HIER WEITER
			/*
			if (BA != null)
			{
				BA.enable();
				if (BA.isEnabled())
					Toast.makeText(getApplicationContext(),"Enabled",Toast.LENGTH_LONG).show();
				else
					Toast.makeText(getApplicationContext(),"Not turned on :(",Toast.LENGTH_LONG).show();
			} else
				Toast.makeText(getApplicationContext(),"FUCK.",Toast.LENGTH_LONG).show();
			 */
			/*
			BroadcastReceiver btReceiver = new BroadcastReceiver() {
			    @Override
			    public void onReceive(Context context, Intent intent) {

			    	String action = intent.getAction();

			        if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
			            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			            if (device.getBondState() == BluetoothDevice.BOND_BONDED)
			            {
			                // CONNECT
			            }
			        } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
			            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			            // Discover new device
			        }
			    }
			};

	    	IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
	    	intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
	    	this.registerReceiver(btReceiver, intentFilter);


			if (!BA.isEnabled()) {
			      Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE); 
			      startActivityForResult(enableBtIntent, request_code_for_enabling_bt);
			}

			//bond device


			Set<BluetoothDevice> pairedDevices = BA.getBondedDevices();

			for(BluetoothDevice bt : pairedDevices)
				if (bt.getAddress().equals(MULTIMETER))
					device = bt;

			if (device == null)
				Log.v("Error", "Device not found!");
			else
				Log.v("Device found", device.getAddress().toString());

			BluetoothSocket socket = device.createRfcommSocketToServiceRecord(mUUID);

			Log.v("SOCKET",socket.toString());
			socket.connect();

			is = socket.getInputStream();
			os = socket.getOutputStream();
			 */
		}
		catch (Exception e)
		{
			Log.v(e.getCause().toString(),e.getMessage());
		}

		final Button button = (Button) findViewById(R.id.button1);
		button.setOnClickListener(
				new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						thread.interrupt();
						System.exit(0);
					}
				}
				);
	}

	public void on()
	{
		try
		{
			if (!BA.isEnabled())
			{
				Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(turnOn, 0);
				Toast.makeText(getApplicationContext(),"Turned on",Toast.LENGTH_LONG).show();
			}
			else
			{
				Toast.makeText(getApplicationContext(),"Already on",
						Toast.LENGTH_LONG).show();
			}
		} catch (Exception e)
		{
			Log.v("ERROR", "Unable to turn on bluetooth.");
		}
	}

	//AB HIER CODE FORMATIEREN
	public void list()
	{
		pairedDevices = BA.getBondedDevices();
		ArrayList list = new ArrayList();

		for(BluetoothDevice bt : pairedDevices)
		{
			list.add(bt.getAddress());
			if (bt.getAddress() == MULTIMETER)
			{
				device = bt;
			}
		}		
		if (device == null)
		{
			Log.v("Error", "Device not found!");
		}

		Toast.makeText(getApplicationContext(),"Showing Paired Devices",
				Toast.LENGTH_SHORT).show();
		final ArrayAdapter adapter = new ArrayAdapter (this,android.R.layout.simple_list_item_1, list);
		lv.setAdapter(adapter);
	}
	public void off() {
		BA.disable();
		Toast.makeText(getApplicationContext(),"Turned off" ,
				Toast.LENGTH_LONG).show();
	}
	public void visible() {
		Intent getVisible = new Intent(BluetoothAdapter.
				ACTION_REQUEST_DISCOVERABLE);
		startActivityForResult(getVisible, 0);

	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		//Handle action bar item clicks here. The action bar will
		//automatically handle clicks on the Home/Up button, so long
		//as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		switch (id) {
		case R.id.turn_on:
			on();
			break;
		case R.id.get_visible:
			visible();
			break;
		case R.id.list_devices:
			list();
			break;
		case R.id.turn_off:
			off();
			break;
		}
		return super.onOptionsItemSelected(item);
	}


	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;
		public ConnectThread(BluetoothDevice device) {
			BluetoothSocket tmp = null;
			mmDevice = device;
			try {
				tmp = device.createRfcommSocketToServiceRecord(mUUID);
			} catch (IOException e) {Log.v("ConnectThread","Unable to create RFCommSocket.");}
			mmSocket = tmp;
		}
		public void run() {
			BA.cancelDiscovery();
			try {
				mmSocket.connect();
			} catch (IOException connectException) {
				try {
					mmSocket.close();
				} catch (IOException closeException) {Log.v("ConnectThread","Unable to connect socket.");}
				return;
			}
		}
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {Log.v("ConnectThread","Unable to close socket.");
			}
		}
	}
}