package com.example.touch;

import com.example.touch.R;

import java.util.ArrayList;
import java.util.Set;

import android.support.v7.app.ActionBarActivity;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends Activity {

	// private Button On,Off,Visible,list;
	private BluetoothAdapter BA;
	private Set<BluetoothDevice>pairedDevices;
	private ListView lv;
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		//	      On = (Button)findViewById(R.id.button1);
		//	      Off = (Button)findViewById(R.id.button2);
		//	      Visible = (Button)findViewById(R.id.button3);
		//	      list = (Button)findViewById(R.id.button4);
		lv = (ListView)findViewById(R.id.listView1);
		try
		{
			BA = BluetoothAdapter.getDefaultAdapter();
		}
		catch (Exception e)
		{
			Log.v(e.getCause().toString(),e.getMessage());
		}
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
	   public void list() {
	      pairedDevices = BA.getBondedDevices();

	      ArrayList list = new ArrayList();
	      for(BluetoothDevice bt : pairedDevices)
	         list.add(bt.getName());

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

	}