package fr.noconsulting;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONException;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.util.Log;

public class UsbHid extends CordovaPlugin {
    private final String TAG = UsbHid.class.getSimpleName();

    private USBThreadDataReceiver usbThreadDataReceiver;

    private UsbManager manager;
    private UsbDeviceConnection connection;
    private UsbEndpoint endPointRead;
    private UsbDevice device;
    private PendingIntent mPermissionIntent;

    private CallbackContext readCallback;

    private int packetSize;

    private Byte[] bytes;
    private static int TIMEOUT = 0;
    private boolean forceClaim = true;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        JSONObject arg_object = args.optJSONObject(0);

        if (action.equals("enumerateDevices")){
            this.enumerateDevices(callbackContext);
            return true;
        } else if (action.equals("requestPermission")) {
            this.requestPermission(arg_object, callbackContext);
            return true;
        } else if (action.equals("open")) {
            this.open(arg_object, callbackContext);
            return true;
        } else if (action.equals("stop")) {
            this.stop(arg_object, callbackContext);
            return true;
        } else if (action.equals("registerReadCallback")) {
            this.registerReadCallback(callbackContext);
            return true;
        }

        return false;
    }

    private void enumerateDevices(CallbackContext callbackContext) {
        UsbManager manager = (UsbManager) cordova.getActivity().getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        JSONArray result = new JSONArray();
        for (UsbDevice usbDevice : deviceList.values()) {
            JSONObject obj = new JSONObject();
            addProperty(obj, "name", usbDevice.getDeviceName());
            addProperty(obj, "vendor", usbDevice.getVendorId());
            addProperty(obj, "product", usbDevice.getProductId());
            addProperty(obj, "serial", usbDevice.getSerialNumber());
            result.put(obj);
        }
        callbackContext.success(result);
    }

    private void requestPermission(final JSONObject opts, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                manager = (UsbManager) cordova.getActivity().getSystemService(Context.USB_SERVICE);
                try {
                    device = manager.getDeviceList().get(opts.get("name"));
                    mPermissionIntent = PendingIntent.getBroadcast(cordova.getActivity(), 0, new Intent(UsbBroadcastReceiver.USB_PERMISSION), 0);
                    IntentFilter filter = new IntentFilter(UsbBroadcastReceiver.USB_PERMISSION);
                    UsbBroadcastReceiver usbReceiver = new UsbBroadcastReceiver(callbackContext, cordova.getActivity());
                    cordova.getActivity().registerReceiver(usbReceiver, filter);
                    // finally ask for the permissionje gagneje sa
                    manager.requestPermission(device, mPermissionIntent);
                } catch (JSONException e) {
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void open(final JSONObject opts, final CallbackContext callbackContext) {
        //callbackContext.success("connected !");
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                UsbDeviceConnection c = manager.openDevice(device);
                if (c != null) {
                    // get first port and open it
                    if (connection != null) {
                        //throw new IOException("Already open");
                    }
                    connection = c;
                    UsbInterface intf = device.getInterface(0);
                    UsbEndpoint endpoint = intf.getEndpoint(0);
                    connection.claimInterface(intf, forceClaim);
                    try {
                        if (UsbConstants.USB_DIR_IN == intf.getEndpoint(0).getDirection()) {
                            endPointRead = intf.getEndpoint(0);
                            //packetSize = 8;
                            packetSize = endPointRead.getMaxPacketSize();
                        }
                    } catch (Exception e) {
                        Log.e("endPointWrite", "Device have no endPointRead", e);
                    }
                    usbThreadDataReceiver = new USBThreadDataReceiver();
                    usbThreadDataReceiver.start();
                    callbackContext.success("Serial port opened!");
                }
            }
        });
    }

    private void stop(final JSONObject opts, final CallbackContext callbackContext) {
        usbThreadDataReceiver.stopThis();
    }

    private void registerReadCallback(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                readCallback = callbackContext;
                JSONObject returnObj = new JSONObject();
                addProperty(returnObj, "test", "true");
                // Keep the callback
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, returnObj);
                pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);
            }
        });
    }

    /***************************/

    private void addProperty(JSONObject obj, String key, Object value) {
        try {
            obj.put(key, value);
        }
        catch (JSONException e){}
    }

    private void updateReceivedData(byte[] data) {
        if( readCallback != null ) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, data);
            result.setKeepCallback(true);
            readCallback.sendPluginResult(result);
        }
    }

    /***************************/

    private class USBThreadDataReceiver extends Thread {

        private volatile boolean isStopped;

        public USBThreadDataReceiver() {
        }

        @Override
        public void run() {
            try {
                if (connection != null && endPointRead != null) {
                    while (!isStopped) {
                        final byte[] buffer = new byte[packetSize];
                        final int status = connection.bulkTransfer(endPointRead, buffer, packetSize, 100);
                        updateReceivedData(buffer);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in receive thread", e);
            }
        }

        public void stopThis() {
            isStopped = true;
        }

        public String decodeUtf8(byte[] src) {
            return new String(src, UTF8_CHARSET);
        }
        private final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    }
}
