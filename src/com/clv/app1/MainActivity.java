package com.clv.app1;

import java.io.IOException;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

public class MainActivity extends Activity {
	public static final String DEVICE_NAME = "device_name";
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_STATE_CHANGE = 1;

	public static final int MESSAGE_TOAST = 5;
	public static final int MESSAGE_WRITE = 3;
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;
	public static final String TOAST = "toast";
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	
	// Objeto para la App1
	private App1 mApp1 = null;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Name of the connected device
	private String mConnectedDeviceName = null;
	// Array adapter for the conversation thread
	private ArrayAdapter<String> mConversationArrayAdapter;
	private ListView mConversationView;
	// The Handler that gets information back from the BluetoothChatService
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:

				switch (msg.arg1) {
				case App1.STATE_CONNECTED:
					mTitle.setText(R.string.title_connected_to);
					mTitle.append(mConnectedDeviceName);
					//mConversationArrayAdapter.clear();
					break;
				case App1.STATE_CONNECTING:
					mTitle.setText(R.string.title_connecting);
					break;
				case App1.STATE_LISTEN:
				case App1.STATE_NONE:
					mTitle.setText(R.string.title_not_connected);
					break;
				}
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
				mConversationArrayAdapter.add(mConnectedDeviceName + ":  "
						+ readMessage);
				break;
			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(),
						"Connected to " + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};
	private EditText mOutEditText;
	// String buffer for outgoing messages
	private StringBuffer mOutStringBuffer;
	private Button mSendButton0;
	private Button mSendButton1;
	
	// Defino variables locales al Activity
	// Layout Views
	private TextView mTitle;

	// The action listener for the EditText widget, to listen for the return key
	private TextView.OnEditorActionListener mWriteListener = new TextView.OnEditorActionListener() {
		@Override
		public boolean onEditorAction(TextView view, int actionId,
				KeyEvent event) {
			// If the action is a key-up event on the return key, send the
			// message
			if (actionId == EditorInfo.IME_NULL
					&& event.getAction() == KeyEvent.ACTION_UP) {
				String message = view.getText().toString();
				sendMessage(message);
			}

			return true;
		}
	};

	private void ensureDiscoverable() {

		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				// Get the device MAC address
				String address = data.getExtras().getString(
						DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// Get the BLuetoothDevice object
				BluetoothDevice device = mBluetoothAdapter
						.getRemoteDevice(address);
				// Attempt to connect to the device
				mConversationArrayAdapter.add("Conectando..."+address);
				mApp1.connect(device);
			}
			break;
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a chat session
				setupApp1();
			} else {
				// User did not enable Bluetooth or an error occured

				Toast.makeText(this, R.string.bt_not_enabled_leaving,
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set up the window layout
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.main);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.custom_title);

		// Set up the custom title
		mTitle = (TextView) findViewById(R.id.title_left_text);
		mTitle.setText(R.string.app_name);
		mTitle = (TextView) findViewById(R.id.title_right_text);
		mConversationArrayAdapter = new ArrayAdapter<String>(this,
				R.layout.message);
		mConversationView = (ListView) findViewById(R.id.in);
		mConversationView.setAdapter(mConversationArrayAdapter);
		
		mConversationArrayAdapter.add("Recuperando adaptador BT...");

		// Adaptador bluetooth.
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// Si es NULL, Bluetooth no soportado.
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth no soportado", Toast.LENGTH_LONG)
					.show();
			finish();
			return;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);
		return true;
	}

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
	}
	@Override
	public synchronized void onResume() {
		super.onResume();

		// Performing this check in onResume() covers the case in which BT was
		// not enabled during onStart(), so we were paused to enable it...
		// onResume() will be called when ACTION_REQUEST_ENABLE activity
		// returns.
		if (mApp1 != null) {
			// Only if the state is STATE_NONE, do we know that we haven't
			// started already
			if (mApp1.getState() == App1.STATE_NONE) {
				// Start the Bluetooth chat services
				mApp1.start();
			}
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		mConversationArrayAdapter.add("Chequeo Estado BT...");

		// Si BT no está activado, solicito que se active.
		// ....
		if (!mBluetoothAdapter.isEnabled()) {
			mConversationArrayAdapter.add("BT no activo. Solicito activación...");
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			// Sino, activo programa...
		} else {
			mConversationArrayAdapter.add("BT activo...");
			if (mApp1 == null)
				setupApp1();
		}
	}

	private void sendMessage(String message) {
		mConversationArrayAdapter.add("Envia mensaje...");
		// Check that we're actually connected before trying anything
		if (mApp1.getState() != App1.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
					.show();
			return;
		}

		// Check that there's actually something to send
		if (message.length() > 0) {
			// Get the message bytes and tell the BluetoothChatService to write
			byte[] send = message.getBytes();
			mApp1.write(send);

			// Reset out string buffer to zero and clear the edit text field
			//mOutStringBuffer.setLength(0);
			//mOutEditText.setText(mOutStringBuffer);
		}
	}
	
	private void sendData(String message) {
		mConversationArrayAdapter.add("Envia dato 1...");
		// Check that we're actually connected before trying anything
		if (mApp1.getState() != App1.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
					.show();
			return;
		}

		// Check that there's actually something to send
		if (message.length() > 0) {
			// Get the message bytes and tell the BluetoothChatService to write
			mApp1.sendData(message);
			//byte[] send = message.getBytes();
			//mApp1.write(send);
			//mApp1.sendData(send);
					
			// Reset out string buffer to zero and clear the edit text field
			mOutStringBuffer.setLength(0);
			mOutEditText.setText(mOutStringBuffer);
		}
	}

	private void setupApp1() {

		// Initialize the array adapter for the conversation thread
		// mConversationArrayAdapter = new ArrayAdapter<String>(this,
		// 		R.layout.message);
		// mConversationView = (ListView) findViewById(R.id.in);
		// mConversationView.setAdapter(mConversationArrayAdapter);

		// Initialize the compose field with a listener for the return key
		//mOutEditText = (EditText) findViewById(R.id.edit_text_out);
		//mOutEditText.setOnEditorActionListener(mWriteListener);

		// Initialize the send button with a listener that for click events
		mSendButton1 = (Button) findViewById(R.id.button_send1);
		mSendButton1.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Send a message using content of the edit text widget
				//TextView view = (TextView) findViewById(R.id.editText1);
				String message = "1"; //view.getText().toString();
				//message = "1";
							
				//mApp1.sendData("1");
				sendMessage(message);
				//mConversationArrayAdapter.add("Enviando datos...");
				//sendData("1"); 
				//Toast msg = Toast.makeText(getBaseContext(),
			    //        "Has Enviado un 1", Toast.LENGTH_SHORT);
			    //msg.show();
			}
		});
		mSendButton0 = (Button) findViewById(R.id.button_send0);
		mSendButton0.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Send a message using content of the edit text widget
				//TextView view = (TextView) findViewById(R.id.editText1); 
				String message = "0";  // view.getText().toString();
				
				//message = "0";
				
				//mApp1.sendData("1");
				sendMessage(message);
				//mConversationArrayAdapter.add("Enviando datos...");
				//sendData("1"); 
				//Toast msg = Toast.makeText(getBaseContext(),
			    //        "Has Enviado un 1", Toast.LENGTH_SHORT);
			    //msg.show();
			}
		});

		// Initialize the BluetoothChatService to perform bluetooth connections
		mApp1 = new App1(this, mHandler);

		// Initialize the buffer for outgoing messages
		mOutStringBuffer = new StringBuffer("");
		
	}
	
	public void showTexto(String s) {
		mConversationArrayAdapter.add(s);		
	}
	
	
}