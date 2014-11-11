/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.touch;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the main Activity that displays the current chat session.
 */
public class MainActivity extends Activity {
    // Debugging
    private static final String TAG = "BluetoothChat";
    private static final boolean D = true;

    public static String MULTIMETER = "00:1A:7D:16:46:C5";
    
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Layout Views
    private TextView mTitle;
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;

    // Name of the connected device
    private String mConnectedServerName = null;
    private String mConnectedDeviceName1 = null;
    private String mConnectedDeviceName2 = null;
    private String mConnectedDeviceName3 = null;
   
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
              // Start the Bluetooth chat services
              mChatService.start();
            }
        }
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
       
        // Check if all devices connected to server
        if (mChatService.serverDevice && mChatService.getState() != BluetoothChatService.STATE_ALL_CONNECTED) {
            Toast.makeText(this, "waiting", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!mChatService.serverDevice && mChatService.getState() != BluetoothChatService.STATE_CONNECTED_TO_SERVER) {
            Toast.makeText(this, "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
       
        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }

    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
        new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            if(D) Log.i(TAG, "END onEditorAction");
            return true;
        }
    };

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
//                switch (msg.arg1) {
//                case BluetoothChatService.STATE_ALL_CONNECTED:
//                    mTitle.setText(R.string.title_all_connected);
//                    mConversationArrayAdapter.clear();
//                    break;
//                case BluetoothChatService.STATE_CONNECTED:
//                    mTitle.setText(R.string.title_connected_to);
//                    mTitle.append(mChatService.connectedDevices + " Device(s)");
//                    break;
//                case BluetoothChatService.STATE_CONNECTED_TO_SERVER:
//                    mTitle.setText(R.string.title_connected_to);
//                    mTitle.append(" Server: " + mConnectedServerName);
//                    break;                    
//                case BluetoothChatService.STATE_CONNECTING:
//                    mTitle.setText(R.string.title_connecting);
//                    break;
//                case BluetoothChatService.STATE_LISTEN:
//                case BluetoothChatService.STATE_NONE:
//                    mTitle.setText(R.string.title_not_connected);
//                    break;
//                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                int deviceID = msg.arg2;
                mConversationArrayAdapter.add("Device "+ deviceID + ": " + readMessage);
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                if (mChatService.getState() == BluetoothChatService.STATE_CONNECTED) {
                                 if (mChatService.connectedDevices == 1) {
                                        mConnectedDeviceName1 = msg.getData().getString(DEVICE_NAME);
                                   Toast.makeText(getApplicationContext(), "Connected to "
                                                  + mConnectedDeviceName1, Toast.LENGTH_SHORT).show();
                                } else if ( mChatService.connectedDevices == 2) {
                                        mConnectedDeviceName2 = msg.getData().getString(DEVICE_NAME);
                                Toast.makeText(getApplicationContext(), "Connected to "
                                               + mConnectedDeviceName2, Toast.LENGTH_SHORT).show();
                                } else if ( mChatService.connectedDevices == 3) {
                                        mConnectedDeviceName3 = msg.getData().getString(DEVICE_NAME);
                                Toast.makeText(getApplicationContext(), "Connected to "
                                               + mConnectedDeviceName3, Toast.LENGTH_SHORT).show();
                                }
                }
               
                if (mChatService.getState() == BluetoothChatService.STATE_CONNECTED_TO_SERVER) {
                                mConnectedServerName = msg.getData().getString(DEVICE_NAME);
                        Toast.makeText(getApplicationContext(), "Connected to server "
                                       + mConnectedServerName, Toast.LENGTH_SHORT).show();
                        }
               
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = MULTIMETER;//data.getExtras()
                                     //.getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mChatService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupChat();
            } else {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this,"bt not enabled", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    /*
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.scan:
            // Launch the DeviceListActivity to see devices and do scan
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
        case R.id.discoverable:
            // Ensure this device is discoverable by others
            ensureDiscoverable();
            return true;
        }
        return false;
    }*/

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

    
	public void toggleBluetooth()
	{
		
	}
	
	public void pair()
	{
	}
	
	public void close()
	{
		//connectThread.cancel();
		//mConnectedThread.interrupt();
		System.exit(0);
	}
	
	public void visible()
	{
		
	}
}

//
//package com.example.touch;
//import com.example.touch.R;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.lang.reflect.Method;
//import java.util.ArrayList;
//import java.util.Set;
//import java.util.UUID;
//
//import android.support.v7.app.ActionBarActivity;
//import android.app.Activity;
//import android.app.Application;
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothDevice;
//import android.bluetooth.BluetoothServerSocket;
//import android.bluetooth.BluetoothSocket;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Message;
//import android.provider.Settings;
//import android.util.Log;
//import android.view.Menu;
//import android.view.MenuItem;
//import android.view.View;
//import android.view.View.OnClickListener;
//import android.widget.ArrayAdapter;
//import android.widget.Button;
//import android.widget.Filter;
//import android.widget.ListView;
//import android.widget.Toast;
//import java.util.UUID;
//
//public class MainActivity extends Activity
//{
//	
//	//BT MULTIMETER
//	//00:1A:7D:16:46:C5
//	
//	public UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); 
//	public final String mNAME = "MainActivity";
//	public static String MULTIMETER = "00:1A:7D:16:46:C5";
//	
//	// Debugging
//	private static final String TAG = "TOUCH";
//    private static final boolean D = true;
//
//    // Message types sent from the BluetoothChatService Handler
//    public static final int MESSAGE_STATE_CHANGE = 1;
//    public static final int MESSAGE_READ = 2;
//    public static final int MESSAGE_WRITE = 3;
//    public static final int MESSAGE_DEVICE_NAME = 4;
//    public static final int MESSAGE_TOAST = 5;
//
//    // Key names received from the BluetoothChatService Handler
//    public static final String DEVICE_NAME = "device_name";
//    public static final String TOAST = "toast";
//
//    // Intent request codes
//    private static final int REQUEST_CONNECT_DEVICE = 1;
//    private static final int REQUEST_ENABLE_BT = 2;
//	
//	private ListView lv;
//	
//	private BluetoothDevice device;
//	private InputStream is;
//	private OutputStream os;
//	
//	// private Button On,Off,Visible,list;
//	private BluetoothAdapter BA;
//	
//	//private AcceptThread mAcceptThread;
//	//private ConnectThread mConnectThread;
//	//private ConnectedThread mConnectedThread;
//	
//    // Array adapter for the conversation thread
//    private ArrayAdapter<String> mConversationArrayAdapter;
//    // String buffer for outgoing messages
//    private StringBuffer mOutStringBuffer;
//    // Local Bluetooth adapter
//    private BluetoothAdapter mBluetoothAdapter = null;
//    // Member object for the chat services
//    private BluetoothChatService mChatService = null;
//	
//    // Name of the connected device
//    private String mConnectedServerName = null;
//    private String mConnectedDeviceName = null;
//    
//    // The Handler that gets information back from the BluetoothChatService
//    private final Handler mHandler = new Handler() {
//        
//		@Override
//        public void handleMessage(Message msg) {
//            switch (msg.what) {
//            case MESSAGE_WRITE:
//                byte[] writeBuf = (byte[]) msg.obj;
//                // construct a string from the buffer
//                String writeMessage = new String(writeBuf);
//                mConversationArrayAdapter.add("Me:  " + writeMessage);
//                break;
//            case MESSAGE_READ:
//                byte[] readBuf = (byte[]) msg.obj;
//                // construct a string from the valid bytes in the buffer
//                String readMessage = new String(readBuf, 0, msg.arg1);
//                int deviceID = msg.arg2;
//                mConversationArrayAdapter.add("Device "+ deviceID + ": " + readMessage);
//                break;
//            case MESSAGE_DEVICE_NAME:
//                // save the connected device's name
//                if (mChatService.getState() == BluetoothChatService.STATE_CONNECTED) {
//                                 if (mChatService.connectedDevices == 1) {
//                                        mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
//                                   Toast.makeText(getApplicationContext(), "Connected to "
//                                                  + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
//                                }
//                }
//               
//                if (mChatService.getState() == BluetoothChatService.STATE_CONNECTED_TO_SERVER) {
//                                mConnectedServerName = msg.getData().getString(DEVICE_NAME);
//                        Toast.makeText(getApplicationContext(), "Connected to server "
//                                       + mConnectedServerName, Toast.LENGTH_SHORT).show();
//                        }
//               
//                break;
//            case MESSAGE_TOAST:
//                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
//                               Toast.LENGTH_SHORT).show();
//                break;
//            }
//        }
//    };
//
//    
//	private void setupChat()
//	{
//        Log.d(TAG, "setupChat()");
//
//        // Initialize the BluetoothChatService to perform bluetooth connections
//        mChatService = new BluetoothChatService(this, mHandler);
//
//        // Initialize the buffer for outgoing messages
//        mOutStringBuffer = new StringBuffer("");
//    }
//
//	@Override
//	protected void onCreate(Bundle savedInstanceState)
//	{
//		super.onCreate(savedInstanceState);
//		setContentView(R.layout.activity_main);
//		
//		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        if (mBluetoothAdapter == null) {
//            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
//            finish();
//            return;
//        }
//        
//        /*
//		BA = BluetoothAdapter.getDefaultAdapter();
//		if (BA == null)
//			Log.v(TAG,"Device does not support Bluetooth");
//	
//		if (BA.isEnabled())
//			device = getFromAdapter();
//		else
//			Toast.makeText(getApplicationContext(),"Please make sure Bluetooth is turned on and the Device is paired",Toast.LENGTH_LONG).show();
//		
//		if (device != null)
//		{
//			mConnectThread = new ConnectThread(device);
//			mConnectThread.start();
//		}
//		*/
//	}
//	
//	@Override
//    public void onStart()
//	{
//        super.onStart();
//        if(D) Log.e(TAG, "++ ON START ++");
//
//        // If BT is not on, request that it be enabled.
//        // setupChat() will then be called during onActivityResult
//        if (!mBluetoothAdapter.isEnabled()) {
//            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
//        // Otherwise, setup the chat session
//        } else {
//            if (mChatService == null) setupChat();
//        }
//    }
//
//	/*
//	private BluetoothDevice getFromAdapter()
//	{
//		Set<BluetoothDevice> pairedDevices;
//		pairedDevices = BA.getBondedDevices();
//		
//		if (pairedDevices == null)
//			Log.v(TAG,"UH OH");
//		
//		if (pairedDevices.size() > 0)
//		{
//			for(BluetoothDevice bt : pairedDevices)
//				if (bt.getAddress().toString().equals(MULTIMETER))
//				{
//					Log.v(TAG, bt.getAddress().toString());
//					return bt;
//				}
//		}
//		Log.v(TAG,"Device was not found.");
//		return null;
//	}*/
//	
//	/**
//	 * Thread that sets up Communication
//	 */
//	/*
//	private class ConnectThread extends Thread
//	{
//		private final BluetoothSocket mmSocket;
//		private final BluetoothDevice mmDevice;
//		public ConnectThread(BluetoothDevice device)
//		{
//			BluetoothSocket tmp = null;
//			mmDevice = device;
//			try
//			{
//				tmp = device.createRfcommSocketToServiceRecord(mUUID);
//			} catch (IOException e)
//			{
//				Log.v(TAG,"Unable to create RFCommSocket.");
//			}
//			mmSocket = tmp;
//		}
//		public void run()
//		{
//			BA.cancelDiscovery();
//			try
//			{
//				mmSocket.connect();
//			} catch (IOException connectException)
//			{
//				try
//				{
//					mmSocket.close();
//				} catch (IOException closeException)
//				{
//					Log.v(TAG,"Unable to connect socket.");
//				}
//				return;
//			}
//			Log.v(TAG, "Starting a connection..");
//			mConnectedThread = new ConnectedThread(mmSocket);
//			mConnectedThread.start();
//		}
//		public void cancel()
//		{
//			try
//			{
//				mmSocket.close();
//			} catch (IOException e)
//			{
//				Log.v(TAG,"Unable to close socket.");
//			}
//		}
//	};*/
//	
//	/**
//	 * Thread that handles Communication stream
//	 */
//	/*
//	private class ConnectedThread extends Thread
//	{
//		private final BluetoothSocket mmSocket;
//		private final InputStream mmInStream;
//		private final OutputStream mmOutStream;
//		public ConnectedThread(BluetoothSocket socket)
//		{
//			mmSocket = socket;
//			InputStream tmpIn = null;
//			OutputStream tmpOut = null;
//			try
//			{
//				tmpIn = socket.getInputStream();
//				tmpOut = socket.getOutputStream();
//			} catch (IOException e)
//			{
//				Log.v(TAG,"Sockets not created!");
//			}
//			
//			mmInStream = tmpIn;
//			mmOutStream = tmpOut;
//		}
//		public void run()
//		{
//			byte[] buffer = new byte[1024];
//			int begin = 0;
//			int bytes = 0;
//			while (true)
//			{
//				try
//				{
//					Thread.sleep(50);
//					bytes = mmInStream.read(buffer,0,buffer.length);
//					//Log.v(TAG,String.valueOf(mmInStream.read()));
//					Log.v(TAG,String.valueOf(bytes));
//				
//					//mHandler.obtainMessage();
//					/*
//					bytes += mmInStream.read(buffer, bytes, buffer.length - bytes);
//					for(int i = begin; i < bytes; i++)
//					{
//						if(buffer[i] == "#".getBytes()[0])
//						{
//							mHandler.obtainMessage(1, begin, i, buffer).sendToTarget();
//							begin = i + 1;
//							if(i == bytes - 1)
//							{
//								bytes = 0;
//								begin = 0;
//							}
//						}
//					}
//				} catch (IOException e)
//				{
//					Log.v(TAG,"IO Error.");
//					//break;
//				} catch (InterruptedException e)
//				{
//					Log.v(TAG,"Thread Interrupted.");
//				} catch (Exception e)
//				{
//					Log.v(TAG,"Something else went wrong:" + e.getCause());
//				}
//			}
//		}
//		public void write(byte[] bytes)
//		{
//			try
//			{
//				mmOutStream.write(bytes);
//			} catch (IOException e) { }
//		}
//		public void cancel()
//		{
//			try {
//				mmSocket.close();
//			} catch (IOException e) { }
//		}
//	};*/
//	/*
//	Handler mHandler = new Handler()
//	{
//		@Override
//		public void handleMessage(Message msg)
//		{
//			byte[] writeBuf = (byte[]) msg.obj;
//			int begin = (int)msg.arg1;
//			int end = (int)msg.arg2;
//			switch(msg.what)
//			{
//				case 1:
//					String writeMessage = new String(writeBuf);
//					writeMessage = writeMessage.substring(begin, end);
//				break;
//			}
//			Log.v(TAG,String.valueOf(begin)+":"+String.valueOf(end));
//		}
//	};*/
//	
//	public void toggleBluetooth()
//	{
//		if (mBluetoothAdapter != null)
//		{
//			if (!mBluetoothAdapter.isEnabled())
//			{
//				if (mBluetoothAdapter.enable())
//					Toast.makeText(getApplicationContext(),"Turned bluetooth on",Toast.LENGTH_LONG).show();
//			} else
//			{
//				if (mBluetoothAdapter.disable())
//					Toast.makeText(getApplicationContext(),"Turned bluetooth off",Toast.LENGTH_LONG).show();
//			}
//		} else
//		{
//			Toast.makeText(getApplicationContext(),"Unable to detect bluetooth adapter!",Toast.LENGTH_LONG).show();
//		}
//	}
//	
//	public void pair()
//	{
//		Toast.makeText(getApplicationContext(),"Please pair the device manually.",Toast.LENGTH_LONG).show();
//	}
//	
//	public void close()
//	{
//		//connectThread.cancel();
//		//mConnectedThread.interrupt();
//		System.exit(0);
//	}
//	
//	public void visible()
//	{
//		Intent getVisible = new Intent(BluetoothAdapter.
//				ACTION_REQUEST_DISCOVERABLE);
//		startActivityForResult(getVisible, 0);
//	
//	}
//	
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu)
//	{
//		//Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.main, menu);
//		return true;
//	}
//	
//	@Override
//	public boolean onOptionsItemSelected(MenuItem item)
//	{
//		//Handle action bar item clicks here. The action bar will
//		//automatically handle clicks on the Home/Up button, so long
//		//as you specify a parent activity in AndroidManifest.xml.
//		int id = item.getItemId();
//		switch (id)
//		{
//			case R.id.toggle_bluetooth:
//				toggleBluetooth();
//			break;
//			case R.id.get_visible:
//				visible();
//			break;
//			case R.id.pair_multimeter:
//				pair();
//			break;
//			case R.id.close:
//				close();
//			break;
//		}
//		return super.onOptionsItemSelected(item);
//	}
//}
