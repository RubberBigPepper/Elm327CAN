package com.example.elm327can.obd;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Set;
import java.util.UUID;

public class OBDManager {
    public static String TAG = "OBD";

    private OBDCommandListener m_cOBD = null;
    private OBDSensorListener m_cListener = null;
    private final int TIMEOUT=3000;

    public interface OBDSensorListener {
        //состояния
		void onErrorConnection();//неправильное соединение

        void onErrorOBDInit();//устройство не OBD

        void onOneReadTickEnd(String data);//завершен один цикл чтения данных из ЭБУ

        void onConnectionLost();//при обрыве соединения с OBD

        void onOBDConnected();//при соединении с OBD
    }

    public int m_nConnectionType = 0;//тип подключения, 0 - Bluetooth, 1- WiFi
    public String m_strIP = "192.168.1.10";//адрес устройства для работы
    public int m_nPort = 35000;//порт для работы
    public String m_strBTDevice = "";//девайс для работы
    public String m_strVIN = "";

    private boolean m_bStarted = false;

    public OBDManager(@NonNull OBDSensorListener cListener) {
        m_cListener = cListener;
    }

    /**
     * запуск работы обмена данными с OBD
     */
    public void StartWork() {
        StopWork();
        Log.e(TAG, "Start work");
        m_cOBD = new OBDCommandListener();
        m_cOBD.start();
    }

    /**
     * конец работы, закрытие потока обмена данными
     */
    public void StopWork() {
        Log.e(TAG, "Stop work");
        if (m_cOBD != null) {
            try {
                OBDCommandListener cOBD = m_cOBD;
                //	if(m_cOBD.isAlive())
                {
                    cOBD.SetStop();
                    //cOBD.join();
                }
            } catch (Exception ex) {
            }
            m_cOBD = null;
        }
    }

    public boolean IsStarted() {
        return m_cOBD != null;
    }

    /**
     * MAC адрес устройства BlueTooth для работы
     */
    public String getBTDevice() {
        return m_strBTDevice;
    }

    /**
     * инициализация данных
     */
    public void SetBTDevice(String strDevice) {
        Log.e(TAG, "Set BT device to " + strDevice);
        m_strBTDevice = strDevice;
    }

    private class OBDCommandListener extends Thread {
        private volatile boolean m_bStop = false;
        private Object m_oMutex = null;
        private final Socket m_cSocket = null;

        public void SetStop() {
            if (m_oMutex != null) {
                synchronized (m_oMutex) {
                    m_bStop = true;
                    try {
                        m_oMutex.notify();
                    } catch (Exception ex) {
                    }
                }
            } else
                m_bStop = true;
        }

        private BluetoothSocket InitSocket(String strDevice) {
            Log.e(TAG, "Initializing socket, BT address=" + strDevice);
            BluetoothSocket socket = null;
            try {
                BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
                BluetoothDevice device = btAdapter.getRemoteDevice(strDevice);
                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                btAdapter.cancelDiscovery();
                socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
                socket.connect();
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    socket.close();
                } catch (Exception ex) {
                }
                socket = null;
            }
            return socket;
        }

        private BluetoothSocket InitBTSocket() {
            if (m_strBTDevice.trim().length() != 0) {
                try {//если устройство выбрано, подключаемся к нему
                    BluetoothSocket cSocket = InitSocket(m_strBTDevice);
                    if (InitOBD(cSocket.getInputStream(), cSocket.getOutputStream()))
                        return cSocket;
                    else
                        cSocket.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return null;
            } else {//иначе переберем все устройства блютус и попробуем к ним подключиться
                try {
                    BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
                    Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
                    if (pairedDevices.size() > 0) {
                        for (BluetoothDevice device : pairedDevices) {
                            BluetoothSocket socket = InitSocket(device.getAddress());
                            if (socket != null && InitOBD(socket.getInputStream(), socket.getOutputStream()))
                                return socket;
                            else {
                                try {
                                    socket.close();
                                } catch (Exception ex1) {
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                }
            }
            return null;
        }

        private boolean InitOBD(InputStream cIn, OutputStream cOut) {
            try {
                Log.e(TAG, "Initializing OBD");
                //DebugLog.WriteDebug(m_cContext, "Initializing OBD");
                new ObdRawCommand("ATZ").run(cIn, cOut);//reset  ELM327 device
                new ObdRawCommand("ATD").run(cIn, cOut);//set to default
                new ObdRawCommand("ATE0").run(cIn, cOut);//echo OFF
                new ObdRawCommand("ATSP6").run(cIn, cOut);//Set CAN protocol SAE J1939 CAN (29 bit ID, 250* kbaud)
                new ObdRawCommand("ATS0").run(cIn, cOut);//Set spaces OFF
                new ObdRawCommand("ATL0").run(cIn, cOut);//Set linefeed OFF
                new ObdRawCommand("ATAL").run(cIn, cOut);//Set long messages
                new ObdRawCommand("ATPBC001").run(cIn, cOut);//Helps to start monitoring mode for some ELM327 devices
                new ObdRawCommand("ATH1").run(cIn, cOut);//Set header ON
                new ObdRawCommand("ATMA").run(cIn, cOut);//start monitor all messages
                Log.e(TAG, "Initializing OBD success");
                //DebugLog.WriteDebug(m_cContext, "Initializing OBD success");
                return true;
            } catch (Exception ex) {
                Log.e(TAG, "Initializing OBD error " + ex.getMessage());
                //DebugLog.WriteDebug(m_cContext, "Initializing OBD error "+ex.getMessage());
                ex.printStackTrace();
                return false;
            }
        }

        @Override
        public void run() {
            Log.e(TAG, "OBD read thread created");
            //DebugLog.WriteDebug(m_cContext, "OBD read thread created");
            m_oMutex = new Object();
            while (!m_bStop) {
                if (mainLoop())//поток завершился
                    break;
                {//Иначе подождем и продолжим снова
                    synchronized (m_oMutex) {
                        if (m_bStop)//сигнал завершения
                            break;
                        try {
                            //	DebugLog.WriteDebug(m_cContext, "Mutex waiting");
                            m_oMutex.wait(TIMEOUT);
                            //Thread.sleep(1000);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
            endLoop();
        }

        private boolean ReadVIN(InputStream cIn, OutputStream cOut) {
            try {
                ObdRawCommand cRaw = new ObdRawCommand("0902");
                cRaw.run(cIn, cOut);
                String strRes = cRaw.getResult().trim().replace(" ", "");
                String[] strArRows = strRes.split("\r");
                StringBuilder cBuilder = new StringBuilder();
                boolean b4902Found = false;
                for (int n = 0; n < strArRows.length; n++) {
                    String strRow = strArRows[n];
                    int nIndex = strRow.indexOf(':');
                    if (nIndex >= 0)
                        strRow = strRow.substring(nIndex + 1);//берем все после двоеточия
                    if (!b4902Found/*n==0*/){//первая строка-особая ищем ответ
                        nIndex = strRow.indexOf("4902");
                        if (nIndex >= 0) {
                            b4902Found = true;
                            nIndex = Math.min(nIndex + 6, strRow.length() - 1);
                            strRow = strRow.substring(nIndex);
                        }
                    }
                    int begin = 0;
                    int end = 2;
                    //boolean bAppend=false;
                    while (end <= strRow.length()) {
                        try {
                            int nChar = Integer.decode("0x" + strRow.substring(begin, end));
                            if (nChar > 32 && nChar < 128) {
                                cBuilder.append((char) nChar);
                            }
                        } catch (Exception ex1) {
                        }
                        begin = end;
                        end += 2;
                    }
                }
                if (cBuilder.length() > 8) {
                    m_strVIN = cBuilder.toString();
                    return true;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return false;
        }

		private class _PreparedSocketStream{//вспомогательный класс, будет хранить сокет и его стримы
        	BluetoothSocket socketBT = null;
			Socket cSocketIP = null;
			InputStream cInStream=null;
			OutputStream cOutStream=null;

			public _PreparedSocketStream(BluetoothSocket socketBT){
				this.socketBT=socketBT;
				if (socketBT != null) {
					try {
						cInStream=socketBT.getInputStream();
						cOutStream=socketBT.getOutputStream();
					} catch (Exception ex) {
					}
				}
			}

			public _PreparedSocketStream(Socket socketIP){
				this.cSocketIP=socketIP;
				if (cSocketIP != null) {
					try {
						cInStream=cSocketIP.getInputStream();
						cOutStream=cSocketIP.getOutputStream();
					} catch (Exception ex) {
					}
				}
			}

			private void CloseStreams(){
				try{
					cInStream.close();
				}
				catch (Exception ex){}
				try{
					cOutStream.close();
				}
				catch (Exception ex){}
			}

			public void CleanUp(){
				CloseStreams();
				try {
					Log.e(TAG, "Closing socket");
					if (socketBT != null)
						socketBT.close();
					if (cSocketIP != null)
						cSocketIP.close();
				} catch (Exception ex) {
					Log.e(TAG, "Closing socket error " + ex.getMessage());
					ex.printStackTrace();
				}
			}
		}

        private _PreparedSocketStream prepareMainLoop() {
			_PreparedSocketStream preparedSocketStream = null;
			if (m_nConnectionType == 0 && m_cSocket == null) {
				//Log.e(TAG,"Init BT socket");
				preparedSocketStream = new _PreparedSocketStream(InitBTSocket());
			}
			if (m_nConnectionType == 1 || m_cSocket != null) {//IP
				//	Log.e(TAG,"Init IP socket");
				Socket socket = null;
				if (m_cSocket == null) {
					try {
						socket = new Socket(m_strIP, m_nPort);
					} catch (IOException e) {
						e.printStackTrace();
						if (m_cListener != null)
							m_cListener.onErrorConnection();
						return null;
					}
				} else
					socket = m_cSocket;
				if (!socket.isConnected() || socket.isClosed()) {
					if (m_cListener != null)
						m_cListener.onErrorConnection();
					//		Log.e(TAG,"Stopped mainLoop by socket connection error");
					try {
						socket.close();
					} catch (Exception ex2) {
					}
					return null;
				}
				preparedSocketStream = new _PreparedSocketStream(socket);
			}
			if (!InitOBD(preparedSocketStream.cInStream, preparedSocketStream.cOutStream)) {
				preparedSocketStream.CleanUp();
				if (m_cListener != null)
					m_cListener.onErrorOBDInit();
				return null;
			}
			return preparedSocketStream;
		}

		private boolean mainLoop(){
			_PreparedSocketStream preparedSocketStream=prepareMainLoop();
			if (preparedSocketStream==null)
				return false;
			int nErrorReadAttempt = 0;//счетчик непрочитанных данных
            ObdRawCommand atmaCmd=new ObdRawCommand("ATMA");//читаем все
			while (true) {
				boolean bStop=false;
				if (m_oMutex != null) {
					synchronized (m_oMutex) {
						bStop = m_bStop;
					}
				}
				if (bStop)
					break;
				//Log.e(TAG, "Reading OBD sensor data");
				if (!m_bStarted) {
					m_bStarted = true;
					if (m_cListener != null)
						m_cListener.onOBDConnected();
				}
				if (atmaCmd.run(preparedSocketStream.cInStream, preparedSocketStream.cOutStream)) {
                    if (m_cListener!=null)
				        m_cListener.onOneReadTickEnd(atmaCmd.getFormattedResult());
                    nErrorReadAttempt = 0;//сбрасываем счетчик ошибок чтения
                }
				else {
					nErrorReadAttempt++;
					if (nErrorReadAttempt > 10)//10 раз подряд не прочитали ничего-вываливаемся с ошибкой чтения
						break;//ничего не прочитано, выходим
				}
			}
			m_bStarted = false;
			preparedSocketStream.CleanUp();
			return m_bStop;
		}

        private void endLoop() {
            if (this == m_cOBD) {
                m_cOBD = null;
            }
        }

    }

}
