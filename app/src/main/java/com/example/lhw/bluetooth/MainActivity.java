package com.example.lhw.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Message;
//import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final int RECV_VIEW = 0;
    public static final int NOTICE_VIEW = 1;

    private BluetoothAdapter bluetoothAdapter = null;

    private ConnectThread connectThread = null;
    private ConnectedThread connectedThread = null;

    private TextView noticeView = null;
    private Button turnOnOff = null;
    private TextView recvView = null;
    private Button clearRecvView = null;
    private EditText sendText = null;
    private Button send = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 获取BluetoothAdapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth
            return;
        }

        int REQUEST_ENABLE_BT = 1;
        if (!bluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BT);
            noticeView.setText("开启蓝牙成功");
            //Toast.makeText(this, "开启蓝牙成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "蓝牙已开启", Toast.LENGTH_SHORT).show();
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "蓝牙未开启", Toast.LENGTH_SHORT).show();
        }
        // 查询配对设备 建立连接，只能连接第一个配对的设备
        List<String> devices = new ArrayList<String>();
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : bondedDevices) {
            connectThread = new ConnectThread(device);
            connectThread.start();
            Toast.makeText(this, "连接成功", Toast.LENGTH_SHORT).show();
            break;
        }

        // 注册监听事件
        noticeView = (TextView) findViewById(R.id.notice_view);
        turnOnOff = (Button) findViewById(R.id.turn_on_off);
        recvView = (TextView) findViewById(R.id.recv_view);
        clearRecvView = (Button) findViewById(R.id.clear_recv_view);
        sendText = (EditText) findViewById(R.id.send_text);
        send = (Button) findViewById(R.id.send);

        turnOnOff.setOnClickListener(this);
        clearRecvView.setOnClickListener(this);
        send.setOnClickListener(this);

        if (!bluetoothAdapter.isEnabled()) {
            noticeView.setText("蓝牙未开启");
        }
        else {
            noticeView.setText("蓝牙已开启");
        }
        noticeView.setBackgroundColor(Color.GRAY);
    }

    private boolean isOn = false;
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.turn_on_off: // 发送'0'或者'1'都可以
                if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                    Toast.makeText(this, "蓝牙未开启", Toast.LENGTH_SHORT).show();
                    break;
                }
                if (connectedThread == null) {
                    Toast.makeText(this, "未连接设备", Toast.LENGTH_SHORT).show();
                    break;
                }

                String turn_string = "1@#";
                connectedThread.write(turn_string.getBytes());
                if (isOn == false) {
                    isOn = true; // 打开了
                    turnOnOff.setText("OFF");

                }
                else {
                    isOn = false; // 关闭了
                    turnOnOff.setText("ON");

                }
                break;

            case R.id.clear_recv_view: // 清空接收框
                recvView.setText("");
                break;

            case R.id.send: // 发送数据，默认以"@#"结尾
                if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                    Toast.makeText(this, "蓝牙未开启", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (connectedThread == null) {
                    Toast.makeText(this, "未连接设备", Toast.LENGTH_SHORT).show();
                    break;
                }
                String inputText = sendText.getText().toString() + "@#"; // 发送给单片机数据以"@#结尾"，这样单片机知道一条数据发送结束
                //Toast.makeText(MainActivity.this, inputText, Toast.LENGTH_SHORT).show();
                connectedThread.write(inputText.getBytes());
                break;

            default:
                break;
        }
    }//主界面上的几个辣鸡按钮而已

    private android.os.Handler handler = new android.os.Handler() {
        public void handleMessage(Message msg) {
            Bundle bundle = null;
            switch (msg.what) {
                case RECV_VIEW:
                    if (isOn == false) {
                        isOn = true;
                        turnOnOff.setText("OFF");
                    }
                    bundle = msg.getData();
                    String recv = bundle.getString("recv");
                    recvView.append(recv + "\n");

                    if (recv.isEmpty() || recv.contains(" ") || recv.contains("#")) {
                        break;
                    }
                    int num = Integer.valueOf(recv) / 2; // 0-60s
                    break;

                case NOTICE_VIEW:
                    bundle = msg.getData();
                    String notice = bundle.getString("notice");
                    noticeView.setText(notice);
                    break;

                default:
                    break;
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.start_bluetooth) {
            if (bluetoothAdapter != null) {
                // 开启蓝牙
                int REQUEST_ENABLE_BT = 1;
                if (!bluetoothAdapter.isEnabled()) {
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent, REQUEST_ENABLE_BT);
                    noticeView.setText("开启蓝牙成功");
                    //Toast.makeText(this, "开启蓝牙成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "蓝牙已开启", Toast.LENGTH_SHORT).show();
                }
            }

            return true;
        }
        else if (id == R.id.show_devices) {
            if (bluetoothAdapter != null) {
                if (!bluetoothAdapter.isEnabled()) {
                    Toast.makeText(this, "蓝牙未开启", Toast.LENGTH_SHORT).show();
                    return true;
                }

                // 查询配对设备
                List<String> devices = new ArrayList<String>();
                Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
                for (BluetoothDevice device : bondedDevices) {
                    devices.add(device.getName() + "-" + device.getAddress());
                }
                StringBuilder text = new StringBuilder();
                for (String device : devices) {
                    text.append(device + "\n");
                }
                Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        else if (id == R.id.find_devices) {
            Toast.makeText(this, "该功能暂时不可用", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.connect_devices) {
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                Toast.makeText(this, "蓝牙未开启", Toast.LENGTH_SHORT).show();
                return true;
            }

            // 查询配对设备 建立连接，只能连接第一个配对的设备
            List<String> devices = new ArrayList<String>();
            Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : bondedDevices) {
                connectThread = new ConnectThread(device);
                connectThread.start();
                Toast.makeText(this, "连接成功", Toast.LENGTH_SHORT).show();
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private class ConnectThread extends Thread {
        private final String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        public ConnectThread(BluetoothDevice device) {
            this.device = device;
            BluetoothSocket tmp = null;

            try {
                tmp = device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.socket = tmp;
        }

        public void run() {
            bluetoothAdapter.cancelDiscovery();
            try {
                socket.connect();
                connectedThread = new ConnectedThread(socket);
                connectedThread.start();
            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException ee) {
                    ee.printStackTrace();
                }
                return;
            }
            //manageConnectedSocket(socket);
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 客户端与服务器建立连接成功后，用ConnectedThread收发数据
    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;
            InputStream input = null;
            OutputStream output = null;

            try {
                input = socket.getInputStream();
                output = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.inputStream = input;
            this.outputStream = output;
        }

        public void run() {
            StringBuilder recvText = new StringBuilder();
            byte[] buff = new byte[1024];
            int bytes;

            Bundle tmpBundle = new Bundle();
            Message tmpMessage = new Message();
            tmpBundle.putString("notice", "连接成功");
            tmpMessage.what = NOTICE_VIEW;
            tmpMessage.setData(tmpBundle);
            handler.sendMessage(tmpMessage);
            while (true) {
                try {
                    bytes = inputStream.read(buff);
                    String str = new String(buff, "ISO-8859-1");
                    str = str.substring(0, bytes);

                    // 收到数据，单片机发送上来的数据以"#"结束，这样手机知道一条数据发送结束
                    //Log.e("read", str);
                 /*   if (!str.endsWith("#")) {
                        recvText.append(str);
                        continue;
                    }*/
                    recvText.append(str.substring(0, str.length() - 1)); // 去除'#'

                    Bundle bundle = new Bundle();
                    Message message = new Message();

                    bundle.putString("recv", recvText.toString());
                    message.what = RECV_VIEW;
                    message.setData(bundle);
                    handler.sendMessage(message);
                    recvText.replace(0, recvText.length(), "");
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}


