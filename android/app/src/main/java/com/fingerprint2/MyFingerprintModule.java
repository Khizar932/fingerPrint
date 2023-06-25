package com.fingerprint2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.facebook.react.bridge.CatalystInstance;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import SecuGen.FDxSDKPro.JSGFPLib;
import SecuGen.FDxSDKPro.SGDeviceInfoParam;
import SecuGen.FDxSDKPro.SGFDxDeviceName;
import SecuGen.FDxSDKPro.SGFDxErrorCode;
import SecuGen.FDxSDKPro.SGFDxTemplateFormat;

public class MyFingerprintModule extends ReactContextBaseJavaModule {
    private JSGFPLib sgFplib;
    private ReactApplicationContext reactContext;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static final int REQUEST_USB_PERMISSION = 1;

    public MyFingerprintModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        registerUSBReceiver();
    }

    private void registerUSBReceiver() {
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        reactContext.registerReceiver(mUsbReceiver, filter);
    }

    @ReactMethod
    @SuppressLint("SuspiciousIndentation")
    public void captureFingerprint() {
        if (hasUSBPermission()) {
            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit("FingerprintCaptured", "inside");
            sgFplib = new JSGFPLib(reactContext, (UsbManager) reactContext.getSystemService(Context.USB_SERVICE));
            long error = sgFplib.Init(SGFDxDeviceName.SG_DEV_AUTO);
            if (error == SGFDxErrorCode.SGFDX_ERROR_NONE) {
                error = sgFplib.OpenDevice(0);
                if (error == SGFDxErrorCode.SGFDX_ERROR_NONE) {
                    sgFplib.SetTemplateFormat(SGFDxTemplateFormat.TEMPLATE_FORMAT_ANSI378);
                    SGDeviceInfoParam deviceInfo = new SGDeviceInfoParam();
                    error = sgFplib.GetDeviceInfo(deviceInfo);
                    if (error == SGFDxErrorCode.SGFDX_ERROR_NONE) {
                        int imageWidth = deviceInfo.imageWidth;
                        int imageHeight = deviceInfo.imageHeight;
                        byte[] fingerprintImageData = new byte[imageWidth * imageHeight];
                        error = sgFplib.GetImage(fingerprintImageData);
                        if (error == SGFDxErrorCode.SGFDX_ERROR_NONE) {
                            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                                    .emit("FingerprintCaptured", fingerprintImageData);
                        }
                    }
                }
                sgFplib.CloseDevice();
            }
            sgFplib.Close();
        } else {
            requestUSBPermission();
            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit("FingerprintCaptured", "outside");
        }
    }
    private boolean hasUSBPermission() {
        UsbManager usbManager = (UsbManager) reactContext.getSystemService(Context.USB_SERVICE);
        if (usbManager != null && usbManager.getDeviceList().values().size() > 0) {
            UsbDevice usbDevice = usbManager.getDeviceList().values().iterator().next();
            if (usbManager.hasPermission(usbDevice)) {
                return true;
            } else {
                ActivityCompat.requestPermissions(reactContext.getCurrentActivity(), new String[]{Manifest.permission_group.USB}, REQUEST_USB_PERMISSION);
            }
        }
        return false;
    }
    private void requestUSBPermission() {
        UsbManager usbManager = (UsbManager) reactContext.getSystemService(Context.USB_SERVICE);
        PendingIntent permissionIntent = PendingIntent.getBroadcast(reactContext, REQUEST_USB_PERMISSION, new Intent(ACTION_USB_PERMISSION), 0);
        UsbDevice usbDevice = usbManager.getDeviceList().values().iterator().next();
        usbManager.requestPermission(usbDevice, permissionIntent);
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
//                            Log.d(TAG, "Vendor ID: " + device.getVendorId());
//                            Log.d(TAG, "Product ID: " + device.getProductId());
                        } else {
//                            Log.e(TAG, "mUsbReceiver.onReceive() Device is null");
                        }
                    } else {
//                        Log.e(TAG, "mUsbReceiver.onReceive() permission denied for device " + device);
                    }
                }
            }
        }
    };

    @NonNull
    @Override
    public String getName() {
        return "MyFingerprintModule";
    }
}
