package edu.fau.compasscar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class CarUI extends Activity implements SensorEventListener {
	public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	public static final int SUCCESS_CONNECT = 0;
	public static final int MESSAGE_READ = 1;
	private TextView nameofDevice;
	private Button led1;
	private Button led2;
	private Handler mHandler;
	private ConnectedThread connected;
	
	//variable to get information from a sensor
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private TextView xAccel;
	private TextView yAccel;
	//////////////////////////////
	private String flag;
	private String received = "";


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.car_ui);
	    
	    //setting up the handler
	    mHandler = new Handler(){
	    	@Override
	    	public void handleMessage(Message msg) {
	    		// TODO Auto-generated method stub
	    		super.handleMessage(msg);
	    		switch (msg.what) {
	    		
				case SUCCESS_CONNECT:
					Toast.makeText(getApplicationContext(), "Connected to test", Toast.LENGTH_LONG).show();
					BluetoothSocket socket = (BluetoothSocket)msg.obj;
					ConnectedThread connected = new ConnectedThread(socket);
					connected.start();
					break;
	    		
	    		case MESSAGE_READ:
	    			byte[] readBuf = (byte[])msg.obj;
	    			flag = new String(readBuf, 0, msg.arg1);
	    			Toast.makeText(getApplicationContext(), received, Toast.LENGTH_LONG).show();
	    			break;
	    		}
	    		
	    	}
	    };
	    
	    
	    
	}
	
	private void initSensor() {
		// TODO Auto-generated method stub
		xAccel= (TextView)findViewById(R.id.xAccel);
		yAccel = (TextView)findViewById(R.id.yAccel);
		mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
		//initializing to the accelerometer
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		//registering the listener
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		
		
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
	}
	
	//getting the data from the bluetooth here
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		//get intent from the other activity
		Intent intent = getIntent();
		BluetoothDevice device = (BluetoothDevice)intent.getParcelableExtra("device");
		
		//method to initialize all the data
		initialize(device); 
		
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		connected.cancel();
	}

	private void initialize(BluetoothDevice device) {
		// TODO Auto-generated method stub
		//connect to the textView
		nameofDevice = (TextView)findViewById(R.id.DeviceName);
		nameofDevice.setText(device.getName());
		
		led1 = (Button)findViewById(R.id.led1);
		led1.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				//byte[] b = {0x0040}; 
				int b = -10;
				connected.write(b);
				Toast.makeText(getApplicationContext(), "LED1 clicked", 0).show();
			}
		});
		led2 = (Button)findViewById(R.id.led2);
		
		ConnectThread connect = new ConnectThread(device);
		connect.run();
		//Initializing sensor
	    initSensor();
	}
	
	private class ConnectThread extends Thread{
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;
		
		//constructor for the connection thread
		public ConnectThread(BluetoothDevice device){
			//use a temporary socket because true socket is final
			BluetoothSocket tmp = null;
			mmDevice = device;
			
			//getting a bluetooth socket to connect to the given bluetooth device
			try{
				tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
			}
			catch(IOException e){
				e.printStackTrace();
				Toast.makeText(getApplicationContext(), "could not open bluetooth socket", 0).show();
				finish();
			}
			//setting the socket
			mmSocket = tmp;
		}
		
		public void run(){
			
			//attempting to connect to the device through the socket
			try{
				mmSocket.connect();
				connected = new ConnectedThread(mmSocket);
				
			}
			catch(IOException connectException){
				//unable to connect so close the socket
				connectException.printStackTrace();
				Toast.makeText(getApplicationContext(), "could not connect the socket", Toast.LENGTH_LONG).show();
				try{
					mmSocket.close();
				}
				catch(IOException closeException){
					closeException.printStackTrace();
					finish();
				}				
			}

			//once we get the connection, at this point is where we can move to another interface
			//***************************************************************************//
			//***************************************************************************//
			//***************************************************************************//
			mHandler.sendMessage(mHandler.obtainMessage(SUCCESS_CONNECT, mmSocket));
		}
	}
	
    private class ConnectedThread extends Thread{
    	private final BluetoothSocket mmSocket;
    	private final InputStream mmInStream;
    	private final OutputStream mmOutStream;
    	
    	//constructor for the connectedthread class
    	public ConnectedThread(BluetoothSocket socket){
    		mmSocket = socket;
    		InputStream tmpIn = null;
    		OutputStream tmpOut = null;
    		
    		try{
    			tmpIn = socket.getInputStream();
    			tmpOut = socket.getOutputStream();
    		}
    		catch(IOException e){
    			e.printStackTrace();
    			finish();
    		}
    		
    		mmInStream = tmpIn;
    		mmOutStream = tmpOut;
    	}
    	
    	public void run(){
    		byte[] buffer = new byte[1024]; //this is the buffer for the stream between the devices
    		int bytes; //bytes that will be returned from read
    		
    		//keep listening to the input stream until an exception is thrown
    		while(true){
    			try{
    				
    				bytes = mmInStream.read(buffer);
    				mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
    			}
    			catch(IOException e){
    				e.printStackTrace();
    				finish();
    				break;
    			}
    		}
    	}
    	
    
    	public void write(byte[] bytes){
    		try{
    			mmOutStream.write(bytes);
    		}
    		catch(IOException e){
    			e.printStackTrace();
    			finish();
    		}
    	}
    	
    	public void write(int bytes){
    		try{
    			mmOutStream.write(bytes);
    		}
    		catch(IOException e){
    			e.printStackTrace();
    			finish();
    		}
    	}
    	
    	public void cancel(){
    		try{
    			mmSocket.close();
    		}
    		catch(IOException ew){
    			ew.printStackTrace();
    			finish();
    		}
    	}
    }

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		xAccel.setText("X: " + event.values[0]);
		yAccel.setText("Y: " + event.values[1]);
		int x = (int)event.values[0];
		int y = (int)event.values[1];
	
		
		//brake base case
		if(x > -1 && x < 1 && y >= 0 && y < 1){
				connected.write(0);
		}
			
		
		//go forward
		else if(x >= -5 && x <= -1 && y >= 0 && y < 1){
			//normalize in values of 10
				x = x * (-10);
				connected.write(x);
			
		}
		
		//go reverse, will send the actual values
		else if(x < 6 && x > 1 && y >= 0 && y < 1){
				connected.write(x);
		}
		
		//go left
		else if(y >= -5 && y <= -1 && (x < 6 && x > 1 || x >= -5 && x <= -1 ||x > -1 && x < 1)){
			//normalize in values of 20
			y = y * (-50);
			connected.write(y);
		}
		//go right
		else if(y < 6 && y > 1 && (x < 6 && x > 1 || x >= -5 && x <= -1 ||x > -1 && x < 1)){
			//normalize to values of 25
			y = y * 250;
			connected.write(y);
		} 	
	
		
		
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

}
