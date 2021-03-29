package com.minewbeacon.blescan.demo;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.minew.beacon.BeaconValueIndex;
import com.minew.beacon.BluetoothState;
import com.minew.beacon.MinewBeacon;
import com.minew.beacon.MinewBeaconManager;
import com.minew.beacon.MinewBeaconManagerListener;
import com.yuliwuli.blescan.demo.R;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import static com.minewbeacon.blescan.demo.BeaconListAdapter.beacon_Major;
import static com.minewbeacon.blescan.demo.BeaconListAdapter.beacon_Minor;

public class MainActivity extends AppCompatActivity {

    static public MinewBeaconManager mMinewBeaconManager;
    private RecyclerView mRecycle;
    private BeaconListAdapter mAdapter;
    private static final int REQUEST_ENABLE_BT = 2;
    private boolean isScanning;

    private String phoneNumber;

    UserRssi comp = new UserRssi();
    private TextView mStart_scan;
    private boolean mIsRefreshing;
    private int state;

    private EditText eTClassRoom;
    private EditText eTClassNo;
    private EditText eTClassName;
    private EditText eTClassTime;
    private EditText eTStudent;
    private TextView mMessage;

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*getSupportActionBar().setIcon(R.drawable.ic_baseline_people_alt_24);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);*/

        initView();
        initManager();
        checkBluetooth();
        initListener();

        Button buttonConfirm = (Button) findViewById(R.id.confirm_button) ;
        buttonConfirm.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {

                isScanning = false;
                mStart_scan.setText("Scan");

                phoneNumber = getPhoneNumber().toString();

                eTClassRoom = (EditText) findViewById(R.id.eTClassRoom);
                eTClassNo = (EditText) findViewById(R.id.eTClassNo);
                eTClassName = (EditText) findViewById(R.id.eTClassName);
                eTClassTime = (EditText) findViewById(R.id.eTClassTime);
                eTStudent = (EditText) findViewById(R.id.eTStudent);
                mMessage = (TextView) findViewById(R.id.message);

                eTClassRoom.setText("본관 301호");
                eTClassNo.setText(beacon_Major + "/" + beacon_Minor);
                eTClassName.setText("한국사");
                eTClassTime.setText("13시");
                eTStudent.setText(phoneNumber);
                mMessage.setText("");

                Log.e("tag", "Button Click :" + beacon_Major);
                mMinewBeaconManager.stopScan();
            }
        }) ;

        Button buttonChek = (Button) findViewById(R.id.attendance_button) ;
        buttonChek.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                GetName();
                Log.e("tag", "Button Click :" + beacon_Major);
                mMinewBeaconManager.stopScan();
            }
        });
    }
    public void GetName() {
        String phoneNo = phoneNumber.replaceAll("-", "").toString();

        String[] params = new String[]{"192.168.0.8", beacon_Major, beacon_Minor, phoneNumber};
        new MyAsyncTask().execute(params);

    }
/*
    Handler ahandler = new Handler() {
        public void handlerMessage(Message msg) {
            Bundle bun = msg.getData();
            String retData = bun.getString("RetData");
            Log.e("tag", "retData :" + retData);
            mStudent.setText("출석자 : " + retData);
        }
    };
*/
    /**
     * check Bluetooth state
     */
    private void checkBluetooth() {
        BluetoothState bluetoothState = mMinewBeaconManager.checkBluetoothState();
        switch (bluetoothState) {
            case BluetoothStateNotSupported:
                Toast.makeText(this, "Not Support BLE", Toast.LENGTH_SHORT).show();
                finish();
                break;
            case BluetoothStatePowerOff:
                showBLEDialog();
                break;
            case BluetoothStatePowerOn:
                break;
        }
    }


    private void initView() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mStart_scan = (TextView) findViewById(R.id.start_scan);

        mAdapter = new BeaconListAdapter();


        mRecycle = (RecyclerView) findViewById(R.id.recyeler);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mRecycle.setLayoutManager(layoutManager);
        mAdapter = new BeaconListAdapter();
        mRecycle.setAdapter(mAdapter);
        mRecycle.addItemDecoration(new RecycleViewDivider(this, LinearLayoutManager
                .HORIZONTAL));

    }

    private void initManager() {
        mMinewBeaconManager = MinewBeaconManager.getInstance(this);
    }


    private void initListener() {
        mStart_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mMinewBeaconManager != null) {
                    BluetoothState bluetoothState = mMinewBeaconManager.checkBluetoothState();
                    switch (bluetoothState) {
                        case BluetoothStateNotSupported:
                            Toast.makeText(MainActivity.this, "Not Support BLE", Toast.LENGTH_SHORT).show();
                            finish();
                            break;
                        case BluetoothStatePowerOff:
                            showBLEDialog();
                            return;
                        case BluetoothStatePowerOn:
                            break;
                    }
                }
                if (isScanning) {
                    isScanning = false;
                    mStart_scan.setText("Scan");
                    if (mMinewBeaconManager != null) {
                        mMessage.setText("");
                        mMinewBeaconManager.stopScan();
                    }
                } else {
                    isScanning = true;
                    mStart_scan.setText("Stop");
                    try {
                        eTClassRoom.setText("");
                        eTClassNo.setText("");
                        eTClassName.setText("");
                        eTClassTime.setText("");
                        eTStudent.setText("");
                        mMessage.setText("");

                        mMinewBeaconManager.startScan();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
/*
        mRecycle.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                state = newState;
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });
*/
        mMinewBeaconManager.setDeviceManagerDelegateListener(new MinewBeaconManagerListener() {
            /**
             *   if the manager find some new beacon, it will call back this method.
             *
             *  @param minewBeacons  new beacons the manager scanned
             */
            @Override
            public void onAppearBeacons(List<MinewBeacon> minewBeacons) {

            }

            /**
             *  if a beacon didn't update data in 10 seconds, we think this beacon is out of rang, the manager will call back this method.
             *
             *  @param minewBeacons beacons out of range
             */
            @Override
            public void onDisappearBeacons(List<MinewBeacon> minewBeacons) {
                /*for (MinewBeacon minewBeacon : minewBeacons) {
                    String deviceName = minewBeacon.getBeaconValue(BeaconValueIndex.MinewBeaconValueIndex_Name).getStringValue();
                    Toast.makeText(getApplicationContext(), deviceName + "  out range", Toast.LENGTH_SHORT).show();
                }*/
            }

            /**
             *  the manager calls back this method every 1 seconds, you can get all scanned beacons.
             *
             *  @param minewBeacons all scanned beacons
             */
            @Override
            public void onRangeBeacons(final List<MinewBeacon> minewBeacons) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Collections.sort(minewBeacons, comp);
                        /*Log.e("tag", state + "");*/
                        if (state == 1 || state == 2) {
                        } else {
                            mAdapter.setItems(minewBeacons);
                        }

                    }
                });
            }

            /**
             *  the manager calls back this method when BluetoothStateChanged.
             *
             *  @param state BluetoothState
             */
            @Override
            public void onUpdateState(BluetoothState state) {
                switch (state) {
                    case BluetoothStatePowerOn:
                        Toast.makeText(getApplicationContext(), "BluetoothStatePowerOn", Toast.LENGTH_SHORT).show();
                        break;
                    case BluetoothStatePowerOff:
                        Toast.makeText(getApplicationContext(), "BluetoothStatePowerOff", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //stop scan
        if (isScanning) {
            mMinewBeaconManager.stopScan();
        }
    }

    private void showBLEDialog() {
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                break;
        }
    }

    @SuppressLint("MissingPermission")
    public String getPhoneNumber() {
        TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String phoneNumber = "";
        try {
            if (telephony.getLine1Number() != null) {
                phoneNumber = telephony.getLine1Number();
            } else {
                if (telephony.getSimSerialNumber() != null) {
                    phoneNumber = telephony.getSimSerialNumber();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (phoneNumber.startsWith("+82")) {
            phoneNumber = phoneNumber.replace("+82", "0"); // +8210xxxxyyyy 로 시작되는 번호

        }
        //phoneNumber = phoneNumber.substring(phoneNumber.length()-10,phoneNumber.length());
        //phoneNumber="0"+phoneNumber;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            phoneNumber = PhoneNumberUtils.formatNumber(phoneNumber, Locale.getDefault().getCountry());
        } else {
            phoneNumber = PhoneNumberUtils.formatNumber(phoneNumber);
        }
        return phoneNumber;
    }
/*
    private String soapData(String major, String minor){//soap 통신 메소드 날려주고 데이터 받아옮
        SoapObject request=new SoapObject(NAMESPACE, METHOD_NAME);
        SoapSerializationEnvelope envelope=new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.dotNet=true;
        envelope.setOutputSoapObject(request);
        request.addProperty("major" , major);
        request.addProperty("minor" , minor);
        HttpTransportSE androidHttpTransport=new HttpTransportSE(URL);
        //androidHttpTransport.debug = true;
        String results;

        try
        {
            androidHttpTransport.call(SOAP_ACTION, envelope);
            SoapPrimitive result = (SoapPrimitive)envelope.getResponse();
            results = xmlParsing(result.toString());
            return results;//xml파싱
        } catch(Exception e)
        {
            e.printStackTrace();
            return "";
        } //try-catch
         //결과값 출력
    };
*/
    private String xmlParsing(String data){//데이터 파싱(콤마로 구분함)
        String parsingData=null;
        try{
            XmlPullParserFactory parserCreator = XmlPullParserFactory.newInstance();
            XmlPullParser parser = parserCreator.newPullParser();
            InputStream input = new ByteArrayInputStream(data.getBytes("UTF-8"));
            parser.setInput(input, "UTF-8");

            int parserEvent = parser.getEventType();
            String tag;
            boolean inText = false;

            while (parserEvent != XmlPullParser.END_DOCUMENT ){
                switch(parserEvent){
                    case XmlPullParser.START_TAG:
                        tag = parser.getName();
                        if(tag.compareTo("NewDataSet")==0) {
                            inText=false;
                        }else if(tag.compareTo("Table")==0){
                            inText=false;
                        }else{
                            inText=true;
                        }
                        break;
                    case XmlPullParser.TEXT:
                        tag = parser.getName();
                        if(inText){
                            parsingData += parser.getText() + ","; //데이터를 구분하기위해 콤마를 추가했습니다
                        }
                        inText=false;
                        break;
                    case XmlPullParser.END_TAG:
                        inText=false;
                        break;
                }
                parserEvent = parser.next();
            }
        }catch( Exception e ){
            Log.e("dd", "Error in network call", e);
        }
        return parsingData;
    }

    class MyAsyncTask extends AsyncTask<String, Void, String>
    {
        public String SOAP_ACTION = "http://tempuri.org/AttendanceCheck";
        public String OPERATION_NAME = "AttendanceCheck";
        public String WSDL_TARGET_NAMESPACE = "http://tempuri.org/";
        public String SOAP_ADDRESS;
        private SoapObject request;
        private HttpTransportSE httpTransport;
        private SoapSerializationEnvelope envelope;
        Object response = null;

        @Override
        protected String doInBackground(String... params) {

            SOAP_ADDRESS = "http://"+params[0]+"/Attendance/WebService/WebService.asmx";
            request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);
            PropertyInfo pi_1 = new PropertyInfo();

/*          pi_1.setName("major");
            pi_1.setValue(params[1].toString());
            pi_1.setType(String.class);
 */
            request.addProperty("major", params[1].toString());
            PropertyInfo pi_2 = new PropertyInfo();
            request.addProperty("minor", params[2].toString());
            PropertyInfo pi_3 = new PropertyInfo();
            request.addProperty("phone", params[3].toString());

            envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                envelope.dotNet = true;
                envelope.setOutputSoapObject(request);
                httpTransport = new HttpTransportSE(SOAP_ADDRESS);
                try {
                    httpTransport.call(SOAP_ACTION, envelope);
                    response = envelope.getResponse();
                }
                catch(Exception exp) {
                    response = exp.getMessage();
                }
            return response.toString();
        }

        @Override
        protected void onPostExecute(final String result) {
            super.onPostExecute(result);
            try {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mMessage.setText(result);
                    }
                });
            }
            catch(Exception exp) {
                Log.d("Tag", exp.getMessage());
            }
        }
    }
}
