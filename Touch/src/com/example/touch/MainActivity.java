package com.example.touch;

import com.example.touch.R;

import java.util.Set;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;

import android.bluetooth.*;
import android.content.Intent;

public class MainActivity extends ActionBarActivity {
	
	
	
	private Button button;
	private static final int REQUEST_ENABLE_BT = 2;
	private ArrayAdapter<String> mArrayAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
		    Log.v("Bluetooth", "Device does not support bluetooth");
		}
		else Log.v("Bluetooth", "Yiihaaa, you have bluetooth on your device!");
		
		if (!mBluetoothAdapter.isEnabled()) {
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		
		mArrayAdapter = new ArrayAdapter<String>(this, R.layout.activity_main);
		
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		// If there are paired devices
		if (pairedDevices.size() > 0) {
		    // Loop through paired devices
		    for (BluetoothDevice device : pairedDevices) {
		        // Add the name and address to an array adapter to show in a ListView
		        mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
		        Log.v("Arrays", mArrayAdapter.getItem(0));
		    }
		}
		
		
		
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		addListenerOnButton();
	}
	
	public void addListenerOnButton() {

        //Select a specific button to bundle it with the action you want
		button = (Button) findViewById(R.id.button1);

		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				Log.v("Bkl", "Button clicked");
			}

		});

	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void myfinish() {
		finish();
	}
}
