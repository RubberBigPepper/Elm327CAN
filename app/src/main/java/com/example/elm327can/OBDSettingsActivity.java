package com.example.elm327can;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.example.elm327can.obd.OBDManager;
import com.example.elm327can.obd.ObdCommand;

import java.util.ArrayList;
import java.util.Set;

public class OBDSettingsActivity extends Activity implements OnItemSelectedListener
{
	private Spinner m_cComboOBDDevices;
	private Spinner m_cComboOBDType;//тип подключения, вафля или BT
	private EditText m_cEditIP;
	private EditText m_cEditPort;
	private EditText m_cEditDelay;

	private ArrayList<String> m_cArDeviceStrs = new ArrayList<String>();
    private ArrayList<BluetoothDevice> m_cArBTDevices = new ArrayList<BluetoothDevice>();
    private String m_strBTDevice="";

    private OBDManager obdManager=null;

	@Override
	protected void onCreate(Bundle savedInstanceState)
    {
		super.onCreate(savedInstanceState);
	    try
	    {
	        setContentView(R.layout.obdsettings);	        
	        m_cComboOBDDevices=(Spinner)findViewById(R.id.spinnerBTDevice);
	        m_cComboOBDType=(Spinner)findViewById(R.id.spinnerConnectionType);
	        m_cEditIP=(EditText)findViewById(R.id.editTextIP);
	    	m_cEditPort=(EditText)findViewById(R.id.editTextPort);
	        m_cEditDelay=(EditText)findViewById(R.id.editTextDelay);

	    	m_cComboOBDType.setOnItemSelectedListener(this);
			m_cComboOBDDevices.setOnItemSelectedListener(this);
			obdManager=MainActivity.obdManager;
			if (obdManager!=null) {
				m_strBTDevice = obdManager.m_strBTDevice;
				m_cEditIP.setText(obdManager.m_strIP);
				m_cEditPort.setText(String.valueOf(obdManager.m_nPort));
				m_cEditDelay.setText(String.valueOf(ObdCommand.m_nReadDelay));
			}
			
	        switch(obdManager.m_nConnectionType)
	        {
	        case 1://Wifi
	        	m_cComboOBDType.setSelection(1);
	        	break;
	        default://Bluetooth
	        	m_cComboOBDType.setSelection(0);
	        	break;
	        }
	        
	        ShowBTList();	     
	    }
        catch(Exception ex)
        {
        	ex.printStackTrace();
        }
    }
	
	@Override
    protected void onPause()
    {
		super.onPause();
		if(!isFinishing()||obdManager==null)
			return;
		int nDelay=(int)GetLongValue(m_cEditDelay);
		if(nDelay>=0)
			ObdCommand.m_nReadDelay=nDelay;
		String strIP=m_cEditIP.getText().toString().trim();
		if(!IsValidIP(strIP))
			strIP=obdManager.m_strIP;
		int nPort=(int)GetLongValue(m_cEditPort);
		if(nPort<0||nPort>65535)
			nPort=obdManager.m_nPort;
		obdManager.m_nPort=nPort;
		obdManager.m_strIP=strIP;
		obdManager.m_strBTDevice=m_strBTDevice;
		obdManager.m_nConnectionType=m_cComboOBDType.getSelectedItemPosition();
		obdManager.StartWork();
    }
	
	private long GetLongValue(EditText cEditText)
	{
		try
		{
			cEditText.endBatchEdit();
			String strText=cEditText.getText().toString();
			strText=strText.replace(',', '.');
			return Long.valueOf(strText);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			return -9999;
		}
	}
	
	public static boolean IsValidIP (String ip)
	{
	    try {
	        if ( ip == null || ip.isEmpty() ) {
	            return false;
	        }

	        String[] parts = ip.split( "\\." );
	        if ( parts.length != 4 ) {
	            return false;
	        }

	        for ( String s : parts ) {
	            int i = Integer.parseInt( s );
	            if ( (i < 0) || (i > 255) ) {
	                return false;
	            }
	        }
	        if ( ip.endsWith(".") ) {
	            return false;
	        }

	        return true;
	    } catch (NumberFormatException nfe) {
	        return false;
	    }
	}

	private void ShowBTList()
    {
		ArrayList<String> cArDeviceStrs = new ArrayList<String>();
    	ArrayList<BluetoothDevice> cArBTDevices = new ArrayList<BluetoothDevice>();
    	BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    	Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
    	int nSelIndex=-1;
    	if (pairedDevices.size() > 0)
    	{
    	    for (BluetoothDevice device : pairedDevices)
    	    {
    	    	cArDeviceStrs.add(device.getName() + " (" + device.getAddress()+")");
    	    	if(device.getAddress().equalsIgnoreCase(m_strBTDevice))
    	    		nSelIndex=cArBTDevices.size();
    	    	cArBTDevices.add(device);
    	    }
    	}    	
    	m_cArDeviceStrs=cArDeviceStrs;
    	m_cArBTDevices=cArBTDevices;
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item,m_cArDeviceStrs);
    	m_cComboOBDDevices.setAdapter(adapter);
    	if(nSelIndex>=0)
    		m_cComboOBDDevices.setSelection(nSelIndex);    	
    }

	
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
	{
		switch(parent.getId())
		{
		case R.id.spinnerBTDevice://selected new BT device
			{
				BluetoothDevice cBTDevice=m_cArBTDevices.get(position);
				m_strBTDevice=cBTDevice.getAddress();				
			}
			break;
		case R.id.spinnerConnectionType:
			{
				switch(position)
				{
				case 1:
					findViewById(R.id.linearBT).setVisibility(View.GONE);
					findViewById(R.id.linearWifi).setVisibility(View.VISIBLE);
					break;
				default:
					findViewById(R.id.linearBT).setVisibility(View.VISIBLE);
					findViewById(R.id.linearWifi).setVisibility(View.GONE);
					break;
				}
				break;
			}
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent)
	{
		
	}

}
