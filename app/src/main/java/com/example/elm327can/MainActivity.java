package com.example.elm327can;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.elm327can.obd.OBDManager;
import com.example.elm327can.obd.ObdCommand;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    public static final int PERMISSION_REQUEST_CODE = 12367;
    public static OBDManager obdManager=null;
    private DataAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        obdManager = new OBDManager(obdSensorListener);
        CheckPermissionsOrRun();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isFinishing()){
            obdManager.StopWork();
            WriteSettings(this);
        }
    }

    private void showContent(){
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        adapter=new DataAdapter();
        RecyclerView recyclerView=findViewById(R.id.recyclerView);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(llm);
        recyclerView.setAdapter(adapter);
        ReadSettings(this);
        obdManager.StartWork();
    }

    private void CheckPermissionsOrRun() {
        ArrayList<String> strArNeedPermission = new ArrayList<String>();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            strArNeedPermission.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED)
            strArNeedPermission.add(Manifest.permission.BLUETOOTH);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED)
            strArNeedPermission.add(Manifest.permission.BLUETOOTH_ADMIN);
        if (strArNeedPermission.size() > 0) {
            //спрашиваем пермишен у пользователя
            requestPermission(strArNeedPermission);
        } else {
            showContent();
        }
    }

    public void requestPermission(ArrayList<String> strArNeedPermission) {
        String[] strArList = new String[strArNeedPermission.size()];
        strArNeedPermission.toArray(strArList);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this, strArList, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0) {
            boolean bWrite = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            boolean bBT = ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED;
            boolean bBTAdmin = ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED;
            for (int n = 0; n < permissions.length; n++) {
                if (grantResults[n] == PackageManager.PERMISSION_GRANTED) {
                    if (permissions[n].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                        bWrite = true;
                    if (permissions[n].equals(Manifest.permission.BLUETOOTH))
                        bBT = true;
                    if (permissions[n].equals(Manifest.permission.BLUETOOTH_ADMIN))
                        bBTAdmin = true;
                }
            }
            if (bWrite && bBT && bBTAdmin) {
                showContent();
            } else {
                AlertDialog.Builder cBuilder = new AlertDialog.Builder(this);
                cBuilder.setTitle(R.string.app_name);
                cBuilder.setMessage("Не все нужные разрешения даны программе. Программа завершается");
                //cBuilder.setCancelable(false);
                cBuilder.create().show();
                cBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                    }
                });
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Resources cRes = getResources();
        switch (item.getItemId()) {
            case R.id.menuSettings:
                startActivity(new Intent(this, OBDSettingsActivity.class));
                return true;
        }
        return false;
    }

    private void ReadSettings(Context context){
        SharedPreferences pref=context.getSharedPreferences("common", Context.MODE_PRIVATE);
        obdManager.m_nConnectionType=pref.getInt("OBD type", obdManager.m_nConnectionType);
        obdManager.m_strBTDevice=pref.getString("OBD device", obdManager.m_strBTDevice);
        obdManager.m_strIP=pref.getString("OBD IP", obdManager.m_strIP);
        obdManager.m_nPort=pref.getInt("OBD port", obdManager.m_nPort);
        ObdCommand.m_nReadDelay=pref.getInt("OBD read delay", ObdCommand.m_nReadDelay);
    }

    private void WriteSettings(Context context){
        SharedPreferences.Editor pref=context.getSharedPreferences("common", Context.MODE_PRIVATE).edit();
        pref.putInt("OBD type", obdManager.m_nConnectionType);
        pref.putString("OBD device", obdManager.m_strBTDevice);
        pref.putString("OBD IP", obdManager.m_strIP);
        pref.putInt("OBD port", obdManager.m_nPort);
        pref.putInt("OBD read delay", ObdCommand.m_nReadDelay);
        pref.commit();
    }

    private OBDManager.OBDSensorListener obdSensorListener=new OBDManager.OBDSensorListener() {
        @Override
        public void onErrorConnection() {
            makeToast("Error connection");
        }

        @Override
        public void onErrorOBDInit() {
            makeToast("Error OBD init");
        }

        @Override
        public void onOneReadTickEnd(String data) {
            adapter.setItemValue(String.valueOf(System.currentTimeMillis()), data);
        }

        @Override
        public void onConnectionLost() {
            makeToast("Connection lost");
        }

        @Override
        public void onOBDConnected() {
            makeToast("OBD connected");
        }

        private void makeToast(final String text){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this,text, Toast.LENGTH_SHORT);
                }
            });
        }
    };
}