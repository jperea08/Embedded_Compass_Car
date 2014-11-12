package edu.fau.compasscar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import android.annotation.SuppressLint;
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
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;


@SuppressLint("HandlerLeak")
public class MainActivity extends ActionBarActivity implements OnItemClickListener, OnClickListener{
	
	public static final int MESSAGE_READ = 1;
	protected static final int SUCCESS_CONNECT = 0;
	//local bluetooth adaptr
	private BluetoothAdapter mBluetoothAdapter;
	//set to hold the paired devices
	private Set<BluetoothDevice> pairedDevices;
	//arrayadapter to display the devices
	private ArrayAdapter<String> listAdapter;
	//variable to connect to the listview
	private ListView listView;
	//intent filter to listen to the appropriate signals from the BlueTooth
	private IntentFilter filter;
	//Broadcast receiver to begin to search for devices
	private BroadcastReceiver receiver;
	//button to enable discovery
	private Button discoButton;
	//handler to handle data from bluetooth connection
	public Handler mHandler;
	
	//coding the behavior of the handler anonmyously/////// 
	
	/////////end of the handler////////////////////////
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		mHandler = new Handler(){
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				switch(msg.what){
				case SUCCESS_CONNECT:	//when we get the successful message when we connect to a device
					// do something, like make a new UI
					mBluetoothAdapter.cancelDiscovery();
					BluetoothDevice device = (BluetoothDevice)msg.obj;
					Intent intent = new Intent(MainActivity.this, CarUI.class);
					intent.putExtra("device", device);
					startActivity(intent);
					Toast.makeText(getApplicationContext(), "New Activity Started", Toast.LENGTH_SHORT).show();
					
					//must figure out what to pass to the other activity
					
					break;
				}
			}
		};
		
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//getting local bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		//if there is no adapter, then finish it
		if(mBluetoothAdapter == null){
			Toast.makeText(getApplicationContext(), "No Bluetooth on device", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
	}
	
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		
		//if BlueTooth adapter is not enabled, then enable it
		if(!mBluetoothAdapter.isEnabled()){
			//calling the turnOnBT method to make the intent
			turnOnBt();
		}
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		//initialize everything
		initialize();	
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		unregisterReceiver(receiver);
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		
	}

	private void initialize() {
		// TODO Auto-generated method stub
		listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,0);
		listView = (ListView)findViewById(R.id.listView1);
		listView.setAdapter(listAdapter);
		listView.setOnItemClickListener(this);
		//setting up the discover button
		discoButton = (Button)findViewById(R.id.button1);
		discoButton.setOnClickListener(this);
		
		//get paired devices
		getPairedDevices();
		//setting up the broadcast receiver
		receiver = new BroadcastReceiver(){

			@Override
			public void onReceive(Context context, Intent intent) {
				// TODO Auto-generated method stub
				String action = intent.getAction();
				//when discovery finds a device
				if(BluetoothDevice.ACTION_FOUND.equals(action)){
					//get the bluetooth object from the intent
					BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					//adding any discovered devices to the listview
					//checking if the discovered devices are already in the listAdapter
					if(!pairedDevices.contains(device))
						listAdapter.add(device.getName() + "\n" + device.getAddress());
				}
			}
			
		};
		//setting up the filter to register the broadcast receiver
		filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(receiver, filter);
	}

	/**
	 * method to retrieve the paired devices
	 */
	private void getPairedDevices() {
		// TODO Auto-generated method stub
		//getting all the paired devices
		pairedDevices = mBluetoothAdapter.getBondedDevices();
		if(pairedDevices.size() > 0){
			for(BluetoothDevice device : pairedDevices){
				//add the name to the listView
				listAdapter.add(device.getName() +"(PAIRED)\n" + device.getAddress());
			}
		}
		
	}

	private void turnOnBt() {
		// TODO Auto-generated method stub
		Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	    startActivityForResult(enableBtIntent, 1);
	}

	@Override
	//in this method we will establish a connection between the devices
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		//canceling discoverying because it is costly
		if(mBluetoothAdapter.isDiscovering())
			mBluetoothAdapter.cancelDiscovery();
		
		if(listAdapter.getItem(position).contains("PAIRED")){
			String address = listAdapter.getItem(position);
			//for loop to get the address of the paired device
			for(int i = 0; i < address.length(); i++){
				if(address.charAt(i) == ':'){
					i -= 2;
					address = address.substring(i);
					
					break;
				}
			}
			//Toast.makeText(getApplicationContext(), address, 0).show();
			
			BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
			
			//send bluetooth info to the handler
			mHandler.sendMessage(mHandler.obtainMessage(SUCCESS_CONNECT, device));
			

		}
		else{
			Toast.makeText(getApplicationContext(), "This device is not paired", 0).show();
			
		}
			
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if(mBluetoothAdapter.isDiscovering()){
			mBluetoothAdapter.cancelDiscovery();
			mBluetoothAdapter.startDiscovery();
		}
		else
			mBluetoothAdapter.startDiscovery();
	}
}

