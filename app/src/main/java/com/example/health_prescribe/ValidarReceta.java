    package com.example.health_prescribe;

    import android.annotation.TargetApi;
    import android.app.AlertDialog;
    import android.app.PendingIntent;
    import android.content.*;
    import android.graphics.*;
    import android.graphics.drawable.BitmapDrawable;
    import android.hardware.usb.UsbDevice;
    import android.hardware.usb.UsbManager;
    import android.os.AsyncTask;
    import android.os.Build;
    import android.os.Bundle;
    import android.os.Handler;
    import android.os.Message;
    import android.util.Log;
    import android.view.View;
    import android.widget.*;

    import androidx.appcompat.app.AppCompatActivity;

    import com.example.health_prescribe.model.PacienteDetalle;

    import org.json.JSONObject;

    import java.io.ByteArrayOutputStream;
    import java.nio.ByteBuffer;

    import SecuGen.FDxSDKPro.JSGFPLib;
    import SecuGen.FDxSDKPro.SGAutoOnEventNotifier;
    import SecuGen.FDxSDKPro.SGFDxDeviceName;
    import SecuGen.FDxSDKPro.SGFDxErrorCode;
    import SecuGen.FDxSDKPro.SGFDxSecurityLevel;
    import SecuGen.FDxSDKPro.SGFDxTemplateFormat;
    import SecuGen.FDxSDKPro.SGFingerInfo;
    import SecuGen.FDxSDKPro.SGFingerPresentEvent;
    import SecuGen.FDxSDKPro.SGImpressionType;

    public class ValidarReceta extends AppCompatActivity implements View.OnClickListener, SGFingerPresentEvent {
        private static final String TAG = "ValidarReceta";

        private static final int IMAGE_CAPTURE_TIMEOUT_MS = 10000;
        private static final int IMAGE_CAPTURE_QUALITY = 50;
        JSONObject select_p = new JSONObject();
        private Button mButtonRegisterAutoOn;
        private Button mButtonMatchAutoOn;

        private EditText mEditLog;
        private TextView mTextViewResult;
        private CheckBox mCheckBoxMatched;
        //    private android.widget.ToggleButton mToggleButtonCaptureModeN;
        private PendingIntent mPermissionIntent;

        private ImageView mImageViewRegister;
        private ImageView mImageViewVerify;
        private byte[] mRegisterImage;
        byte[] tomar_valor = new byte[800];
        private byte[] tomar_valor_dos=new byte[800];
        private byte[] mVerifyImage;
        private byte[] mRegisterTemplate;
        private byte[] mVerifyTemplate;
        private int[] mMaxTemplateSize;
        private int mImageWidth;
        private int mImageHeight;
        private int mImageDPI;
        private int[] grayBuffer;
        private Bitmap grayBitmap;
        private IntentFilter filter; //2014-04-11
        private SGAutoOnEventNotifier autoOn;
        private boolean mAutoOnEnabled;
        private boolean bSecuGenDeviceOpened;
        private JSGFPLib sgfplib;
        private boolean usbPermissionRequested;
        //    private Switch mSwitchAutoOn;
        private int[] mNumFakeThresholds;
        private int[] mDefaultFakeThreshold;
        private boolean[] mFakeEngineReady;
        private boolean bRegisterAutoOnMode;
        private boolean bVerifyAutoOnMode;
        private boolean bFingerprintRegistered;
        private int mFakeDetectionLevel = 1;
        private TextView mensaje;
        private int patientId=-1;
        private int codigoReceta;
        private int farmaceuticoId;
        private byte[] patientFingerprint;
        private void debugMessage(String message) {
            this.mEditLog.append(message);
            this.mEditLog.invalidate(); //TODO trying to get Edit log to update after each line written
        }

        //This broadcast receiver is necessary to get user permissions to access the attached USB device
        private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
        private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (ACTION_USB_PERMISSION.equals(action)) {
                    synchronized (this) {
                        UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if(device != null){
                                // Permiso concedido, puedes iniciar tu Activity aquí si es necesario
                                Intent launchActivityIntent = new Intent(context, ValidarReceta.class);
                                int requestCode = 0; // El requestCode que necesites
                                PendingIntent activityPendingIntent = PendingIntent.getActivity(
                                        context,
                                        requestCode,
                                        launchActivityIntent,
                                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                                );
                                try {
                                    activityPendingIntent.send();
                                } catch (PendingIntent.CanceledException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        };



        public Handler fingerDetectedHandler = new Handler(){
            // @Override
            public void handleMessage(Message msg) {
                //RegisterFingerPrint();
                if (bRegisterAutoOnMode) {
                    bRegisterAutoOnMode = false;
                    RegisterFingerPrint();
                }
                else if (bVerifyAutoOnMode) {
                    bVerifyAutoOnMode = false;
                    VerifyFingerPrint();
                }
            }
        };


        @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
        public void onCreate(Bundle savedInstanceState) {
            Log.d(TAG, "Enter onCreate()");
            super.onCreate(savedInstanceState);


            setContentView(R.layout.layout_validar_receta);

            mButtonRegisterAutoOn = (Button)findViewById(R.id.buttonVerifyAutoOn);
            mButtonRegisterAutoOn.setOnClickListener(this);
            mButtonMatchAutoOn = (Button)findViewById(R.id.textViewResultdos);
            mButtonMatchAutoOn.setOnClickListener(this);
           // mButtonRegisterAutoOn.setVisibility(View.INVISIBLE);
            mEditLog = (EditText)findViewById(R.id.textViewResultados);
            mTextViewResult = (TextView)findViewById(R.id.textViewResult);
            mCheckBoxMatched = (CheckBox) findViewById(R.id.checkBoxMatchedValido);
            mImageViewRegister = (ImageView)findViewById(R.id.imageViewRegister);
            mImageViewVerify = (ImageView)findViewById(R.id.imageViewVerify);
            mNumFakeThresholds = new int[1];
            mDefaultFakeThreshold = new int[1];
            mFakeEngineReady =new boolean[1];
            mensaje=(TextView) findViewById(R.id.textViewResultados);
            Intent intent = getIntent();
            this.farmaceuticoId = getIntent().getIntExtra("farmaceuticoId", -1);
            this.codigoReceta = getIntent().getIntExtra("codigoReceta", -1);
            int patientId = getIntent().getIntExtra("clienteId", -1);
            PacienteDetalle paciente = DatabaseConnection.INSTANCE.getPatientByIdhUELLA(patientId);
            if (paciente != null) {
                patientFingerprint = paciente.getHuellaDactilar(); // inicializando el campo
                if (patientFingerprint != null) {
                    Bitmap fingerprintBitmap = BitmapFactory.decodeByteArray(patientFingerprint, 0, patientFingerprint.length);
                    mImageViewRegister.setImageBitmap(fingerprintBitmap);
                } else {
                    mTextViewResult.setText("El paciente no tiene una huella dactilar registrada.");
                }
            } else {
                mTextViewResult.setText("No se encontraron detalles del paciente.");
            }
            if (mImageViewRegister != null) {
                // Configurar la huella dactilar en imageViewRegister
                Bitmap fingerprintBitmap = BitmapFactory.decodeByteArray(patientFingerprint, 0, patientFingerprint.length);
                mImageViewRegister.setImageBitmap(fingerprintBitmap);
            } else {
                // mTextViewResult.setText("El paciente no tiene una huella dactilar registrada.");
            }
            // perform seek bar change listener event used for getting the progress value
            grayBuffer = new int[JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES* JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES];
            for (int i=0; i<grayBuffer.length; ++i)
                grayBuffer[i] = Color.GRAY;
            grayBitmap = Bitmap.createBitmap(JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES, JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES, Bitmap.Config.ARGB_8888);
            grayBitmap.setPixels(grayBuffer, 0, JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES, 0, 0, JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES, JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES);
            int[] sintbuffer = new int[(JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES/2)*(JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES/2)];
            for (int i=0; i<sintbuffer.length; ++i)
                sintbuffer[i] = Color.GRAY;
            Bitmap sb = Bitmap.createBitmap(JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES/2, JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES/2, Bitmap.Config.ARGB_8888);
            sb.setPixels(sintbuffer, 0, JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES/2, 0, 0, JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES/2, JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES/2);
            mImageViewRegister.setImageBitmap(grayBitmap);
            mImageViewVerify.setImageBitmap(grayBitmap);
            mMaxTemplateSize = new int[1];
            //USB Permissions
            mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_MUTABLE);
            filter = new IntentFilter(ACTION_USB_PERMISSION);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                sgfplib = new JSGFPLib(this, (UsbManager)getSystemService(Context.USB_SERVICE));
            }
            bSecuGenDeviceOpened = false;
            usbPermissionRequested = false;
            debugMessage("Starting Activity\n");
            debugMessage("JSGFPLib version: " + sgfplib.GetJSGFPLibVersion() + "\n");
            mAutoOnEnabled = false;
            autoOn = new SGAutoOnEventNotifier(sgfplib, this);
            autoOn.start();
        }
        public class GetFingerprintTask extends AsyncTask<Integer, Void, byte[]> {
            private ImageView mImageViewRegister;
            private TextView mTextViewResult;
            @Override
            protected byte[] doInBackground(Integer... params) {
                byte[] fingerprintBytes = null;

                try {   int patientId = params[0];
                    // Llama al método para obtener la huella dactilar
                    fingerprintBytes = DatabaseConnection.getFingerprintByPatientId(patientId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return fingerprintBytes;
            }

            @Override
            protected void onPostExecute(byte[] fingerprintBytes) {
                if (fingerprintBytes != null) {
                    Bitmap fingerprintBitmap = BitmapFactory.decodeByteArray(fingerprintBytes, 0, fingerprintBytes.length);
                    mImageViewRegister.setImageBitmap(fingerprintBitmap);
                    VerifyFingerPrint();
                } else {
                    mTextViewResult.setText("El paciente no tiene una huella dactilar registrada.");
                }
            }
        }



        @Override
        public void onPause() {
            Log.d(TAG, "Enter onPause()");
            debugMessage("Enter onPause()\n");
            if (bSecuGenDeviceOpened)
            {autoOn.stop();
                sgfplib.CloseDevice();
                bSecuGenDeviceOpened = false;
            }
            unregisterReceiver(mUsbReceiver);
            mRegisterImage = null;
            //mVerifyImage = null;
            mRegisterTemplate = null;
            mVerifyTemplate = null;

            mImageViewRegister.setImageBitmap(grayBitmap);
            mImageViewVerify.setImageBitmap(grayBitmap);
            super.onPause();
            Log.d(TAG, "Exit onPause()");
            super.onPause();
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
        @Override
        public void onResume(){
            super.onResume();
            registerReceiver(mUsbReceiver, filter);
            long error = sgfplib.Init( SGFDxDeviceName.SG_DEV_AUTO);
            if (error != SGFDxErrorCode.SGFDX_ERROR_NONE){
                AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
                if (error == SGFDxErrorCode.SGFDX_ERROR_DEVICE_NOT_FOUND)
                    dlgAlert.setMessage("The attached fingerprint device is not supported on Android");
                else
                    dlgAlert.setMessage("Fingerprint device initialization failed!");
                dlgAlert.setTitle("SecuGen Fingerprint SDK");
                dlgAlert.setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int whichButton){
                                finish();
                                return;
                            }
                        }
                );
                dlgAlert.setCancelable(false);
                dlgAlert.create().show();
            }
            else {
                UsbDevice usbDevice = sgfplib.GetUsbDevice();
                if (usbDevice == null){
                    AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
                    dlgAlert.setMessage("SecuGen fingerprint sensor not found!");
                    dlgAlert.setTitle("SecuGen Fingerprint SDK");
                    dlgAlert.setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int whichButton){
                                    finish();
                                    return;
                                }
                            }
                    );
                    dlgAlert.setCancelable(false);
                    dlgAlert.create().show();
                }
                else {
                    boolean hasPermission = sgfplib.GetUsbManager().hasPermission(usbDevice);
                    if (!hasPermission) {
                        if (!usbPermissionRequested)
                        {
                            debugMessage("Requesting USB Permission\n");
                            //Log.d(TAG, "Call GetUsbManager().requestPermission()");
                            usbPermissionRequested = true;
                            sgfplib.GetUsbManager().requestPermission(usbDevice, mPermissionIntent);
                        }
                        else
                        {
                            //wait up to 20 seconds for the system to grant USB permission
                            hasPermission = sgfplib.GetUsbManager().hasPermission(usbDevice);
                            debugMessage("Waiting for USB Permission\n");
                            int i=0;
                            while ((hasPermission == false) && (i <= 40))
                            {
                                ++i;
                                hasPermission = sgfplib.GetUsbManager().hasPermission(usbDevice);
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                //Log.d(TAG, "Waited " + i*50 + " milliseconds for USB permission");
                            }
                        }
                    }
                    if (hasPermission) {
                        debugMessage("Opening SecuGen Device\n");
                        error = sgfplib.OpenDevice(0);
                        debugMessage("OpenDevice() ret: " + error + "\n");
                        if (error == SGFDxErrorCode.SGFDX_ERROR_NONE)
                        {
                            bSecuGenDeviceOpened = true;
                            SecuGen.FDxSDKPro.SGDeviceInfoParam deviceInfo = new SecuGen.FDxSDKPro.SGDeviceInfoParam();
                            error = sgfplib.GetDeviceInfo(deviceInfo);
                            debugMessage("GetDeviceInfo() ret: " + error + "\n");
                            mImageWidth = deviceInfo.imageWidth;
                            mImageHeight= deviceInfo.imageHeight;
                            mImageDPI = deviceInfo.imageDPI;
                            debugMessage("Image width: " + mImageWidth + "\n");
                            debugMessage("Image height: " + mImageHeight + "\n");
                            debugMessage("Image resolution: " + mImageDPI + "\n");
                            debugMessage("Serial Number: " + new String(deviceInfo.deviceSN()) + "\n");

                            error = sgfplib.FakeDetectionCheckEngineStatus(mFakeEngineReady);
                            debugMessage("Ret[" + error + "] Fake Engine Ready: " + mFakeEngineReady[0] + "\n");
                            if (mFakeEngineReady[0]) {
                                error = sgfplib.FakeDetectionGetNumberOfThresholds(mNumFakeThresholds);
                                debugMessage("Ret[" + error + "] Fake Thresholds: " + mNumFakeThresholds[0] + "\n");
                                if (error != SGFDxErrorCode.SGFDX_ERROR_NONE)
                                    mNumFakeThresholds[0] = 1; //0=Off, 1=TouchChip

                                error = sgfplib.FakeDetectionGetDefaultThreshold(mDefaultFakeThreshold);
                                debugMessage("Ret[" + error + "] Default Fake Threshold: " + mDefaultFakeThreshold[0] + "\n");
                                mFakeDetectionLevel = mDefaultFakeThreshold[0];
                                double[] thresholdValue = new double[1];
                                error = sgfplib.FakeDetectionGetThresholdValue(thresholdValue);
                                debugMessage("Ret[" + error + "] Fake Threshold Value: " + thresholdValue[0] + "\n");
                            }
                            else {
                                mNumFakeThresholds[0] = 1;		//0=Off, 1=Touch Chip
                                mDefaultFakeThreshold[0] = 1; 	//Touch Chip Enabled

                            }

                            sgfplib.SetTemplateFormat(SGFDxTemplateFormat.TEMPLATE_FORMAT_ISO19794);
                            sgfplib.GetMaxTemplateSize(mMaxTemplateSize);
                            debugMessage("TEMPLATE_FORMAT_ISO19794 SIZE: " + mMaxTemplateSize[0] + "\n");
                            mRegisterTemplate = new byte[(int)mMaxTemplateSize[0]];
                            mVerifyTemplate = new byte[(int)mMaxTemplateSize[0]];

                        }
                        else
                        {
                            debugMessage("Waiting for USB Permission\n");
                        }
                    }

                }
            }

        }

        @Override
        public void onDestroy() {
            Log.d(TAG, "Enter onDestroy()");
            sgfplib.CloseDevice();
            mRegisterImage = null;
            //mVerifyImage = null;
            mRegisterTemplate = null;
            mVerifyTemplate = null;
            sgfplib.Close();
            super.onDestroy();
            Log.d(TAG, "Exit onDestroy()");
        }

        //Converts image to grayscale (NEW)
        public Bitmap toGrayscale(byte[] mImageBuffer)
        {
            byte[] Bits = new byte[mImageBuffer.length * 4];
            for (int i = 0; i < mImageBuffer.length; i++) {
                Bits[i * 4] = Bits[i * 4 + 1] = Bits[i * 4 + 2] = mImageBuffer[i]; // Invert the source bits
                Bits[i * 4 + 3] = -1;// 0xff, that's the alpha.
            }

            Bitmap bmpGrayscale = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.ARGB_8888);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
                bmpGrayscale.copyPixelsFromBuffer(ByteBuffer.wrap(Bits));
            }
            return bmpGrayscale;
        }

        //Converts image to binary (OLD)
        public Bitmap toBinary(Bitmap bmpOriginal)
        {
            int width, height;
            height = bmpOriginal.getHeight();
            width = bmpOriginal.getWidth();
            Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            Canvas c = new Canvas(bmpGrayscale);
            Paint paint = new Paint();
            ColorMatrix cm = new ColorMatrix();
            cm.setSaturation(0);
            ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
            paint.setColorFilter(f);
            c.drawBitmap(bmpOriginal, 0, 0, paint);
            return bmpGrayscale;
        }

        public void SGFingerPresentCallback (){
            autoOn.stop();
            fingerDetectedHandler.sendMessage(new Message());
        }

        public void RegisterFingerPrint(){
            long dwTimeStart = 0, dwTimeEnd = 0, dwTimeElapsed = 0;
            if (mRegisterImage != null)
                mRegisterImage = null;
            mRegisterImage = new byte[mImageWidth*mImageHeight];
            bFingerprintRegistered = false;
            this.mCheckBoxMatched.setChecked(false);
            dwTimeStart = System.currentTimeMillis();
            long result = sgfplib.GetImageEx(mRegisterImage, IMAGE_CAPTURE_TIMEOUT_MS,IMAGE_CAPTURE_QUALITY);

            dwTimeEnd = System.currentTimeMillis();
            dwTimeElapsed = dwTimeEnd-dwTimeStart;

            dwTimeStart = System.currentTimeMillis();
            result = sgfplib.SetTemplateFormat(SGFDxTemplateFormat.TEMPLATE_FORMAT_ISO19794);
            dwTimeEnd = System.currentTimeMillis();
            dwTimeElapsed = dwTimeEnd-dwTimeStart;
            int quality1[] = new int[1];
            result = sgfplib.GetImageQuality(mImageWidth, mImageHeight, mRegisterImage, quality1);
            SGFingerInfo fpInfo = new SGFingerInfo();
            fpInfo.FingerNumber = 1;
            fpInfo.ImageQuality = quality1[0];
            fpInfo.ImpressionType = SGImpressionType.SG_IMPTYPE_LP;
            fpInfo.ViewNumber = 1;
            for (int i=0; i< mRegisterTemplate.length; ++i)
                mRegisterTemplate[i] = 0;
            dwTimeStart = System.currentTimeMillis();
            result = sgfplib.CreateTemplate(fpInfo, mRegisterImage, mRegisterTemplate);
            tomar_valor=mRegisterTemplate;

            dwTimeEnd = System.currentTimeMillis();
            dwTimeElapsed = dwTimeEnd-dwTimeStart;
            debugMessage("CreateTemplate() ret:" + result + " [" + dwTimeElapsed + "ms]\n");
            mImageViewRegister.setImageBitmap(this.toGrayscale(mRegisterImage));
            if (result == SGFDxErrorCode.SGFDX_ERROR_NONE) {
                bFingerprintRegistered = true;

                int[] size = new int[1];
                result = sgfplib.GetTemplateSize(mRegisterTemplate, size);
                debugMessage("GetTemplateSize() ret:" + result + " size [" + size[0] + "]\n");

                //mTextViewResult.setText("Fingerprint registered");
                mTextViewResult.setText(new String(mRegisterImage.toString().getBytes()));
            }
            else{
                mTextViewResult.setText("Fingerprint not registered");
            }

            //mRegisterImage = null;
            fpInfo = null;

        }

        public void VerifyFingerPrint() {

            if (patientFingerprint  != null) {
                BitmapDrawable b_ima= (BitmapDrawable) mImageViewRegister.getDrawable();
                Bitmap imagen_Conm=b_ima.getBitmap();
                ByteArrayOutputStream BYo=new ByteArrayOutputStream();
                imagen_Conm.compress(Bitmap.CompressFormat.PNG, 90, BYo);
                byte[] byteArray = BYo.toByteArray();
                mTextViewResult.setText("verificar");
            } else {
                Log.e(TAG, "patientFingerprint is null");
                return;
            }
            long dwTimeStart = 0, dwTimeEnd = 0, dwTimeElapsed = 0;
            /*if (!bFingerprintRegistered) {
                mTextViewResult.setText("Please Register a finger");
                sgfplib.SetLedOn(false);
                return;
            }*/
            //	if (mVerifyImage != null)
            //		mVerifyImage = null;
            mVerifyImage = new byte[mImageWidth*mImageHeight];
            dwTimeStart = System.currentTimeMillis();
            long result = sgfplib.GetImageEx(mVerifyImage, IMAGE_CAPTURE_TIMEOUT_MS,IMAGE_CAPTURE_QUALITY);
            dwTimeEnd = System.currentTimeMillis();
            dwTimeElapsed = dwTimeEnd-dwTimeStart;
            debugMessage("GetImageEx() ret:" + result + " [" + dwTimeElapsed + "ms]\n");

            dwTimeStart = System.currentTimeMillis();
            result = sgfplib.SetTemplateFormat(SGFDxTemplateFormat.TEMPLATE_FORMAT_ISO19794);
            dwTimeEnd = System.currentTimeMillis();
            dwTimeElapsed = dwTimeEnd-dwTimeStart;
            int quality[] = new int[1];
            result = sgfplib.GetImageQuality(mImageWidth, mImageHeight, mVerifyImage, quality);
            SGFingerInfo fpInfo = new SGFingerInfo();
            fpInfo.FingerNumber = 1;
            fpInfo.ImageQuality = quality[0];
            fpInfo.ImpressionType = SGImpressionType.SG_IMPTYPE_LP;
            fpInfo.ViewNumber = 1;
            for (int i=0; i< mVerifyTemplate.length; ++i)
                mVerifyTemplate[i] = 0;
            dwTimeStart = System.currentTimeMillis();
            result = sgfplib.CreateTemplate(fpInfo, mVerifyImage, mVerifyTemplate);

            dwTimeEnd = System.currentTimeMillis();
            dwTimeElapsed = dwTimeEnd-dwTimeStart;
            debugMessage("CreateTemplate() ret:" + result+ " [" + dwTimeElapsed + "ms]\n");
            mImageViewVerify.setImageBitmap(this.toGrayscale(mVerifyImage));

            if (result == SGFDxErrorCode.SGFDX_ERROR_NONE) {

                int[] size = new int[1];
                result = sgfplib.GetTemplateSize(mVerifyTemplate, size);
                boolean[] matched = new boolean[1];
                dwTimeStart = System.currentTimeMillis();
                mensaje.setText(mVerifyTemplate.toString());
                result = sgfplib.MatchTemplate(tomar_valor, mVerifyTemplate, SGFDxSecurityLevel.SL_NORMAL, matched);
                dwTimeEnd = System.currentTimeMillis();
                dwTimeElapsed = dwTimeEnd - dwTimeStart;
                debugMessage("MatchTemplate() ret:" + result + " [" + dwTimeElapsed + "ms]\n");
                if (matched[0]) {
                    mTextViewResult.setText("Coincidencia de huellas dactilares!\n");
                    //Toast.makeText( ValidarReceta.this,"Verificado", Toast.LENGTH_SHORT).show();
                    Toast.makeText( ValidarReceta.this,"Coincidencia de huellas dactilares para el paciente con ID: " + patientId+" Huella: "+mRegisterTemplate, Toast.LENGTH_SHORT).show();
                   // new ValidarReceta.SaveFingerprintTask(patientId, mRegisterTemplate, ValidarReceta.this).execute();
                   // Intent intent = new Intent(ValidarReceta.this, GeneratePrescriptionActivity.class);
                    //startActivity(intent);
                    //finish();
                } else {
                    mTextViewResult.setText("Fingerprint not matched!");
                    Toast.makeText(ValidarReceta.this, "nada", Toast.LENGTH_SHORT).show();

                }
                matched = null;
            }
            else
                mTextViewResult.setText("Fingerprint template extraction failed.");
            mVerifyImage = null;
            fpInfo = null;
        }
        @Override
        public void onClick(View v) {
            long dwTimeStart = 0, dwTimeEnd = 0, dwTimeElapsed = 0;

            if (v == this.mButtonRegisterAutoOn) {
                debugMessage("Clicked REGISTER WITH AUTO ON\n");
                bRegisterAutoOnMode = true;
                mAutoOnEnabled = true;
                mTextViewResult.setText("Auto On enabled for registration");
                autoOn.start(); //Enable Auto On

            }
            if (v == this.mButtonMatchAutoOn) {
                int patientId = getIntent().getIntExtra("clienteId", -1);
                PacienteDetalle paciente = DatabaseConnection.INSTANCE.getPatientByIdhUELLA(patientId);
                if (paciente != null) {
                    patientFingerprint = paciente.getHuellaDactilar(); // inicializando el campo
                    if (patientFingerprint != null) {
                        Bitmap fingerprintBitmap = BitmapFactory.decodeByteArray(patientFingerprint, 0, patientFingerprint.length);
                        mImageViewRegister.setImageBitmap(fingerprintBitmap);
                    } else {
                        mTextViewResult.setText("El paciente no tiene una huella dactilar registrada.");
                    }
                } else {
                    mTextViewResult.setText("No se encontraron detalles del paciente.");
                }
                if (mImageViewRegister != null) {
                    // Configurar la huella dactilar en imageViewRegister
                    Bitmap fingerprintBitmap = BitmapFactory.decodeByteArray(patientFingerprint, 0, patientFingerprint.length);
                    mImageViewRegister.setImageBitmap(fingerprintBitmap);
                } else {
                    // mTextViewResult.setText("El paciente no tiene una huella dactilar registrada.");
                }
                // perform seek bar change listener event used for getting the progress value
                grayBuffer = new int[JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES* JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES];
                for (int i=0; i<grayBuffer.length; ++i)
                    grayBuffer[i] = Color.GRAY;
                grayBitmap = Bitmap.createBitmap(JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES, JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES, Bitmap.Config.ARGB_8888);
                grayBitmap.setPixels(grayBuffer, 0, JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES, 0, 0, JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES, JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES);
                int[] sintbuffer = new int[(JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES/2)*(JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES/2)];
                for (int i=0; i<sintbuffer.length; ++i)
                    sintbuffer[i] = Color.GRAY;
                Bitmap sb = Bitmap.createBitmap(JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES/2, JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES/2, Bitmap.Config.ARGB_8888);
                sb.setPixels(sintbuffer, 0, JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES/2, 0, 0, JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES/2, JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES/2);
                mImageViewRegister.setImageBitmap(grayBitmap);
                mImageViewVerify.setImageBitmap(grayBitmap);
                mMaxTemplateSize = new int[1];
                //USB Permissions
                mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_MUTABLE);
                filter = new IntentFilter(ACTION_USB_PERMISSION);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                    sgfplib = new JSGFPLib(this, (UsbManager)getSystemService(Context.USB_SERVICE));
                }
                bSecuGenDeviceOpened = false;
                usbPermissionRequested = false;
                debugMessage("Starting Activity\n");
                debugMessage("JSGFPLib version: " + sgfplib.GetJSGFPLibVersion() + "\n");
                mAutoOnEnabled = false;
                autoOn = new SGAutoOnEventNotifier(sgfplib, this);
                autoOn.start();

            }
        }


    }
