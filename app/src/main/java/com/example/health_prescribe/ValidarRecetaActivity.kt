package com.example.health_prescribe

import SecuGen.FDxSDKPro.JSGFPLib
import SecuGen.FDxSDKPro.SGAutoOnEventNotifier
import SecuGen.FDxSDKPro.SGDeviceInfoParam
import SecuGen.FDxSDKPro.SGFDxDeviceName
import SecuGen.FDxSDKPro.SGFDxErrorCode
import SecuGen.FDxSDKPro.SGFDxSecurityLevel
import SecuGen.FDxSDKPro.SGFDxTemplateFormat
import SecuGen.FDxSDKPro.SGFingerInfo
import SecuGen.FDxSDKPro.SGImpressionType
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer


class ValidarRecetaActivity : AppCompatActivity() {
    private val TAG = "SecuGen USB"
    private val IMAGE_CAPTURE_TIMEOUT_MS = 10000
    private val IMAGE_CAPTURE_QUALITY = 50
    var select_p = JSONObject()
    private var mButtonRegisterAutoOn: Button? = null
    private var mButtonMatchAutoOn: Button? = null

    private var mEditLog: EditText? = null
    private var mTextViewResult: TextView? = null
    private var mCheckBoxMatched: CheckBox? = null

    //    private android.widget.ToggleButton mToggleButtonCaptureModeN;
    private var mPermissionIntent: PendingIntent? = null

    private var mImageViewRegister: ImageView? = null
    private var mImageViewVerify: ImageView? = null
    private var mRegisterImage: ByteArray? = null
    private var tomar_valor = ByteArray(0)
    private val tomar_valor_dos = ByteArray(0)
    private var mVerifyImage: ByteArray? = null
    private var mRegisterTemplate: ByteArray? = null
    private var mVerifyTemplate: ByteArray? = null
    private var mMaxTemplateSize: IntArray? = null
    private var mImageWidth = 0
    private var mImageHeight = 0
    private var mImageDPI = 0
    private var grayBuffer: IntArray? = null
    private var grayBitmap: Bitmap? = null
    private var filter: IntentFilter? = null //2014-04-11

    private var autoOn: SGAutoOnEventNotifier? = null
    private var mAutoOnEnabled = false
    private var bSecuGenDeviceOpened = false
    private var sgfplib: JSGFPLib? = null
    private var usbPermissionRequested = false

    //    private Switch mSwitchAutoOn;
    private var mNumFakeThresholds: IntArray? = null
    private var mDefaultFakeThreshold: IntArray? = null
    private var mFakeEngineReady: BooleanArray? = null
    private var bRegisterAutoOnMode = false
    private var bVerifyAutoOnMode = false
    private var bFingerprintRegistered = false
    private var mFakeDetectionLevel = 1
    private var mensaje: TextView? = null

    private fun debugMessage(message: String) {
        mEditLog!!.append(message)
        mEditLog!!.invalidate() //TODO trying to get Edit log to update after each line written
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////
    //This broadcast receiver is necessary to get user permissions to access the attached USB device
    private val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
    private val mUsbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            //Log.d(TAG,"Enter mUsbReceiver.onReceive()");
            if (ACTION_USB_PERMISSION == action) {
                synchronized(this) {
                    val device =
                        intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            //DEBUG Log.d(TAG, "Vendor ID : " + device.getVendorId() + "\n");
                            //DEBUG Log.d(TAG, "Product ID: " + device.getProductId() + "\n");
                            debugMessage(
                                """
                                USB BroadcastReceiver VID : ${device.vendorId}
                                
                                """.trimIndent()
                            )
                            debugMessage(
                                """
                                USB BroadcastReceiver PID: ${device.productId}
                                
                                """.trimIndent()
                            )
                        } else Log.e(
                            "mUsbReceiver.onReceive() Device is null" ,
                            "mUsbReceiver.onReceive() Device is null"
                        )
                    } else Log.e(
                        "mUsbReceiver.onReceive() Device is null",
                        "mUsbReceiver.onReceive() permission denied for device $device"
                    )
                }
            }
        }
    }


    var fingerDetectedHandler: Handler = object : Handler() {
        // @Override
        override fun handleMessage(msg: Message) {
            //RegisterFingerPrint();
            if (bRegisterAutoOnMode) {
                bRegisterAutoOnMode = false
                RegisterFingerPrint()
            } else if (bVerifyAutoOnMode) {
                bVerifyAutoOnMode = false
                VerifyFingerPrint()
            }
        }
    }


    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_validar_receta)

        mButtonRegisterAutoOn = findViewById<View>(R.id.buttonRegisterAutoOn) as Button
        mButtonRegisterAutoOn!!.setOnClickListener({})
        mButtonMatchAutoOn = findViewById<View>(R.id.buttonVerifyAutoOn) as Button
        mButtonMatchAutoOn!!.setOnClickListener({})
        mEditLog = findViewById<View>(R.id.editLog) as EditText
        mTextViewResult = findViewById<View>(R.id.textViewResult) as TextView
       // mCheckBoxMatched = findViewById<View>(R.id.checkBoxMatched) as CheckBox
        mImageViewRegister = findViewById<View>(R.id.imageViewRegister) as ImageView
        mImageViewVerify = findViewById<View>(R.id.imageViewVerify) as ImageView
        mNumFakeThresholds = IntArray(1)
        mDefaultFakeThreshold = IntArray(1)
        mFakeEngineReady = BooleanArray(1)
        // mensaje = findViewById<View>(R.id.textViewResultdos) as TextView
        // perform seek bar change listener event used for getting the progress value
        // perform seek bar change listener event used for getting the progress value
        grayBuffer =
            IntArray(JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES * JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES)
        for (i in grayBuffer!!.indices) grayBuffer!![i] = Color.GRAY
        grayBitmap = Bitmap.createBitmap(
            JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES,
            JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES,
            Bitmap.Config.ARGB_8888
        )
        grayBitmap!!.setPixels(
            grayBuffer as IntArray,
            0,
            JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES,
            0,
            0,
            JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES,
            JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES
        )

        val sintbuffer =
            IntArray(JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES / 2 * (JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES / 2))
        for (i in sintbuffer.indices) sintbuffer[i] = Color.GRAY
        val sb = Bitmap.createBitmap(
            JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES / 2,
            JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES / 2,
            Bitmap.Config.ARGB_8888
        )
        sb.setPixels(
            sintbuffer,
            0,
            JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES / 2,
            0,
            0,
            JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES / 2,
            JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES / 2
        )
        mImageViewRegister!!.setImageBitmap(grayBitmap)
        mImageViewVerify!!.setImageBitmap(grayBitmap)
        mMaxTemplateSize = IntArray(1)
        //USB Permissions
        //USB Permissions
        mPermissionIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE
        )
        filter = IntentFilter(ACTION_USB_PERMISSION)
        sgfplib = JSGFPLib(this, getSystemService(USB_SERVICE) as UsbManager)
        bSecuGenDeviceOpened = false
        usbPermissionRequested = false
        debugMessage("Starting Activity\n")
        debugMessage(
            """
                JSGFPLib version: ${sgfplib!!.GetJSGFPLibVersion()}
                
                """.trimIndent()
        )
        mAutoOnEnabled = false
       // autoOn = SGAutoOnEventNotifier(sgfplib, this)
        autoOn!!.start()



    }


    override fun onPause() {

        debugMessage("Enter onPause()\n")
        if (bSecuGenDeviceOpened) {
            autoOn!!.stop()
            sgfplib!!.CloseDevice()
            bSecuGenDeviceOpened = false
        }
        unregisterReceiver(mUsbReceiver)
        mRegisterImage = null
        //mVerifyImage = null;
        mRegisterTemplate = null
        mVerifyTemplate = null
        mImageViewRegister!!.setImageBitmap(grayBitmap)
        mImageViewVerify!!.setImageBitmap(grayBitmap)
        super.onPause()

        super.onPause()
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////

    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////
    override fun onResume() {
        super.onResume()
        registerReceiver(mUsbReceiver, filter)
        var error = sgfplib!!.Init(SGFDxDeviceName.SG_DEV_AUTO)
        if (error != SGFDxErrorCode.SGFDX_ERROR_NONE) {
            val dlgAlert = AlertDialog.Builder(this)
            if (error == SGFDxErrorCode.SGFDX_ERROR_DEVICE_NOT_FOUND) dlgAlert.setMessage("The attached fingerprint device is not supported on Android") else dlgAlert.setMessage(
                "Fingerprint device initialization failed!"
            )
            dlgAlert.setTitle("SecuGen Fingerprint SDK")
            dlgAlert.setPositiveButton("OK",
                DialogInterface.OnClickListener { dialog, whichButton ->
                    finish()
                    return@OnClickListener
                }
            )
            dlgAlert.setCancelable(false)
            dlgAlert.create().show()
        } else {
            val usbDevice = sgfplib!!.GetUsbDevice()
            if (usbDevice == null) {
                val dlgAlert = AlertDialog.Builder(this)
                dlgAlert.setMessage("SecuGen fingerprint sensor not found!")
                dlgAlert.setTitle("SecuGen Fingerprint SDK")
                dlgAlert.setPositiveButton("OK",
                    DialogInterface.OnClickListener { dialog, whichButton ->
                        finish()
                        return@OnClickListener
                    }
                )
                dlgAlert.setCancelable(false)
                dlgAlert.create().show()
            } else {
                var hasPermission = sgfplib!!.GetUsbManager().hasPermission(usbDevice)
                if (!hasPermission) {
                    if (!usbPermissionRequested) {
                        debugMessage("Requesting USB Permission\n")
                        //Log.d(TAG, "Call GetUsbManager().requestPermission()");
                        usbPermissionRequested = true
                        sgfplib!!.GetUsbManager().requestPermission(usbDevice, mPermissionIntent)
                    } else {
                        //wait up to 20 seconds for the system to grant USB permission
                        hasPermission = sgfplib!!.GetUsbManager().hasPermission(usbDevice)
                        debugMessage("Waiting for USB Permission\n")
                        var i = 0
                        while (hasPermission == false && i <= 40) {
                            ++i
                            hasPermission = sgfplib!!.GetUsbManager().hasPermission(usbDevice)
                            try {
                                Thread.sleep(500)
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            }
                            //Log.d(TAG, "Waited " + i*50 + " milliseconds for USB permission");
                        }
                    }
                }
                if (hasPermission) {
                    debugMessage("Opening SecuGen Device\n")
                    error = sgfplib!!.OpenDevice(0)
                    debugMessage("OpenDevice() ret: $error\n")
                    if (error == SGFDxErrorCode.SGFDX_ERROR_NONE) {
                        bSecuGenDeviceOpened = true
                        val deviceInfo = SGDeviceInfoParam()
                        error = sgfplib!!.GetDeviceInfo(deviceInfo)
                        debugMessage("GetDeviceInfo() ret: $error\n")
                        mImageWidth = deviceInfo.imageWidth
                        mImageHeight = deviceInfo.imageHeight
                        mImageDPI = deviceInfo.imageDPI
                        debugMessage("Image width: $mImageWidth\n")
                        debugMessage("Image height: $mImageHeight\n")
                        debugMessage("Image resolution: $mImageDPI\n")
                        debugMessage(
                            """
                            Serial Number: {kotlin.String(deviceInfo.deviceSN())}
                            
                            """.trimIndent()
                        )
                        error = sgfplib!!.FakeDetectionCheckEngineStatus(mFakeEngineReady)
                        debugMessage(
                            """
                            Ret[$error] Fake Engine Ready: ${mFakeEngineReady?.get(0)}
                            
                            """.trimIndent()
                        )
                        if (mFakeEngineReady?.get(0) == true) {
                            error = sgfplib!!.FakeDetectionGetNumberOfThresholds(mNumFakeThresholds)
                            debugMessage(
                                """
                                Ret[$error] Fake Thresholds: ${mNumFakeThresholds?.get(0)}
                                
                                """.trimIndent()
                            )
                            if (error != SGFDxErrorCode.SGFDX_ERROR_NONE) mNumFakeThresholds?.set(0,
                                1
                            ) //0=Off, 1=TouchChip
                            error =
                                sgfplib!!.FakeDetectionGetDefaultThreshold(mDefaultFakeThreshold)
                            debugMessage(
                                """
                                Ret[$error] Default Fake Threshold: ${mDefaultFakeThreshold?.get(0)}
                                
                                """.trimIndent()
                            )
                            //mFakeDetectionLevel = mDefaultFakeThreshold?.get(0) ?:
                            val thresholdValue = DoubleArray(1)
                            error = sgfplib!!.FakeDetectionGetThresholdValue(thresholdValue)
                            debugMessage(
                                """
                                Ret[$error] Fake Threshold Value: ${thresholdValue[0]}
                                
                                """.trimIndent()
                            )
                        } else {
                            mNumFakeThresholds?.set(0, 1) //0=Off, 1=Touch Chip
                            mDefaultFakeThreshold?.set(0, 1) //Touch Chip Enabled
                        }
                        sgfplib!!.SetTemplateFormat(SGFDxTemplateFormat.TEMPLATE_FORMAT_ISO19794)
                        sgfplib!!.GetMaxTemplateSize(mMaxTemplateSize)
                        debugMessage(
                            """
                            TEMPLATE_FORMAT_ISO19794 SIZE: ${mMaxTemplateSize!![0]}
                            
                            """.trimIndent()
                        )
                        mRegisterTemplate = ByteArray(mMaxTemplateSize!![0])
                        mVerifyTemplate = ByteArray(mMaxTemplateSize!![0])
                    } else {
                        debugMessage("Waiting for USB Permission\n")
                    }
                }
            }
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////
    override fun onDestroy() {

        sgfplib!!.CloseDevice()
        mRegisterImage = null
        //mVerifyImage = null;
        mRegisterTemplate = null
        mVerifyTemplate = null
        sgfplib!!.Close()
        super.onDestroy()

    }

    //Converts image to grayscale (NEW)
    fun toGrayscale(mImageBuffer: ByteArray): Bitmap? {
        val Bits = ByteArray(mImageBuffer.size * 4)
        for (i in mImageBuffer.indices) {
            Bits[i * 4 + 2] = mImageBuffer[i]
            Bits[i * 4 + 1] = Bits[i * 4 + 2]
            Bits[i * 4] = Bits[i * 4 + 1] // Invert the source bits
            Bits[i * 4 + 3] = -1 // 0xff, that's the alpha.
        }
        val bmpGrayscale = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.ARGB_8888)
        bmpGrayscale.copyPixelsFromBuffer(ByteBuffer.wrap(Bits))
        return bmpGrayscale
    }

    //Converts image to binary (OLD)
    fun toBinary(bmpOriginal: Bitmap): Bitmap? {
        val width: Int
        val height: Int
        height = bmpOriginal.height
        width = bmpOriginal.width
        val bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val c = Canvas(bmpGrayscale)
        val paint = Paint()
        val cm = ColorMatrix()
        cm.setSaturation(0f)
        val f = ColorMatrixColorFilter(cm)
        paint.colorFilter = f
        c.drawBitmap(bmpOriginal, 0f, 0f, paint)
        return bmpGrayscale
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////
    fun SGFingerPresentCallback() {
        autoOn!!.stop()
        fingerDetectedHandler.sendMessage(Message())
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////


    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////
    fun RegisterFingerPrint() {
        var dwTimeStart: Long = 0
        var dwTimeEnd: Long = 0
        var dwTimeElapsed: Long = 0
        if (mRegisterImage != null) mRegisterImage = null
        mRegisterImage = ByteArray(mImageWidth * mImageHeight)
        bFingerprintRegistered = false
        mCheckBoxMatched!!.isChecked = false
        dwTimeStart = System.currentTimeMillis()
        var result = sgfplib!!.GetImageEx(
            mRegisterImage,
            IMAGE_CAPTURE_TIMEOUT_MS.toLong(),
            IMAGE_CAPTURE_QUALITY.toLong()
        )
        dwTimeEnd = System.currentTimeMillis()
        dwTimeElapsed = dwTimeEnd - dwTimeStart
        dwTimeStart = System.currentTimeMillis()
        result = sgfplib!!.SetTemplateFormat(SGFDxTemplateFormat.TEMPLATE_FORMAT_ISO19794)
        dwTimeEnd = System.currentTimeMillis()
        dwTimeElapsed = dwTimeEnd - dwTimeStart
        val quality1 = IntArray(1)
        result = sgfplib!!.GetImageQuality(
            mImageWidth.toLong(),
            mImageHeight.toLong(),
            mRegisterImage,
            quality1
        )
        var fpInfo: SGFingerInfo? = SGFingerInfo()
        fpInfo!!.FingerNumber = 1
        fpInfo.ImageQuality = quality1[0]
        fpInfo.ImpressionType = SGImpressionType.SG_IMPTYPE_LP
        fpInfo.ViewNumber = 1
        for (i in mRegisterTemplate?.indices!!) mRegisterTemplate?.set(i, 0)
        dwTimeStart = System.currentTimeMillis()
        result = sgfplib!!.CreateTemplate(fpInfo, mRegisterImage, mRegisterTemplate)
        tomar_valor = mRegisterTemplate!!
        dwTimeEnd = System.currentTimeMillis()
        dwTimeElapsed = dwTimeEnd - dwTimeStart
        debugMessage(
            """CreateTemplate() ret:$result [${dwTimeElapsed}ms]
"""
        )
        mImageViewRegister!!.setImageBitmap(toGrayscale(mRegisterImage!!))
        if (result == SGFDxErrorCode.SGFDX_ERROR_NONE) {
            bFingerprintRegistered = true
            val size = IntArray(1)
            result = sgfplib!!.GetTemplateSize(mRegisterTemplate, size)
            debugMessage(
                """GetTemplateSize() ret:$result size [${size[0]}]
"""
            )

            //mTextViewResult.setText("Fingerprint registered");
            // mTextViewResult!!.text = kotlin.String(mRegisterImage.toString().toByteArray())
        } else {
            mTextViewResult!!.text = "Fingerprint not registered"
        }

        //mRegisterImage = null;
        fpInfo = null
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////
    fun VerifyFingerPrint() {
        val b_ima = mImageViewRegister!!.drawable as BitmapDrawable
        val imagen_Conm = b_ima.bitmap
        val BYo = ByteArrayOutputStream()
        imagen_Conm.compress(Bitmap.CompressFormat.PNG, 90, BYo)
        val byteArray = BYo.toByteArray()
        mTextViewResult!!.text = "verificar"
        var dwTimeStart: Long = 0
        var dwTimeEnd: Long = 0
        var dwTimeElapsed: Long = 0
        /*if (!bFingerprintRegistered) {
			mTextViewResult.setText("Please Register a finger");
			sgfplib.SetLedOn(false);
			return;
		}*/
        //	if (mVerifyImage != null)
        //		mVerifyImage = null;
        mVerifyImage = ByteArray(mImageWidth * mImageHeight)
        dwTimeStart = System.currentTimeMillis()
         var result = sgfplib!!.GetImageEx(
            mVerifyImage,
            IMAGE_CAPTURE_TIMEOUT_MS.toLong(), IMAGE_CAPTURE_QUALITY.toLong()
           )
        dwTimeEnd = System.currentTimeMillis()
        dwTimeElapsed = dwTimeEnd - dwTimeStart
        debugMessage(
            """GetImageEx() ret:$result [${dwTimeElapsed}ms]
"""
        )
        dwTimeStart = System.currentTimeMillis()
        result = sgfplib!!.SetTemplateFormat(SGFDxTemplateFormat.TEMPLATE_FORMAT_ISO19794)
        dwTimeEnd = System.currentTimeMillis()
        dwTimeElapsed = dwTimeEnd - dwTimeStart
        val quality = IntArray(1)
        result = sgfplib!!.GetImageQuality(
            mImageWidth.toLong(),
            mImageHeight.toLong(),
            mVerifyImage,
            quality
        )
        var fpInfo: SGFingerInfo? = SGFingerInfo()
        fpInfo!!.FingerNumber = 1
        fpInfo.ImageQuality = quality[0]
        fpInfo.ImpressionType = SGImpressionType.SG_IMPTYPE_LP
        fpInfo.ViewNumber = 1
        for (i in mVerifyTemplate?.indices!!) mVerifyTemplate!![i] = 0
        dwTimeStart = System.currentTimeMillis()
        result = sgfplib!!.CreateTemplate(fpInfo, mVerifyImage, mVerifyTemplate)
        dwTimeEnd = System.currentTimeMillis()
        dwTimeElapsed = dwTimeEnd - dwTimeStart
        debugMessage(
            """CreateTemplate() ret:$result [${dwTimeElapsed}ms]
"""
        )
        mImageViewVerify!!.setImageBitmap(toGrayscale(mVerifyImage!!))
        if (result == SGFDxErrorCode.SGFDX_ERROR_NONE) {
            val size = IntArray(1)
            result = sgfplib!!.GetTemplateSize(mVerifyTemplate, size)
            var matched: BooleanArray? = BooleanArray(1)
            dwTimeStart = System.currentTimeMillis()
            mensaje!!.text = mVerifyTemplate.toString()
            result = sgfplib!!.MatchTemplate(
                tomar_valor,
                mVerifyTemplate,
                SGFDxSecurityLevel.SL_NORMAL,
                matched
            )
            dwTimeEnd = System.currentTimeMillis()
            dwTimeElapsed = dwTimeEnd - dwTimeStart
            debugMessage(
                """MatchTemplate() ret:$result [${dwTimeElapsed}ms]
"""
            )
            if (matched!![0]) {
                mTextViewResult!!.text = "Coincidencia de huellas dactilares!\n"
                Toast.makeText(this@ValidarRecetaActivity, "Verificado", Toast.LENGTH_SHORT).show()
            } else {
                mTextViewResult!!.text = "Fingerprint not matched!"
                Toast.makeText(this@ValidarRecetaActivity, "nada", Toast.LENGTH_SHORT).show()
            }
            matched = null
        } else mTextViewResult!!.text = "Fingerprint template extraction failed."
        mVerifyImage = null
        fpInfo = null
    }


    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////
    fun onClick(v: View) {
        val dwTimeStart: Long = 0
        val dwTimeEnd: Long = 0
        val dwTimeElapsed: Long = 0
        if (v === mButtonRegisterAutoOn) {
            debugMessage("Clicked REGISTER WITH AUTO ON\n")
            bRegisterAutoOnMode = true
            mAutoOnEnabled = true
            mTextViewResult!!.text = "Auto On enabled for registration"
            autoOn!!.start() //Enable Auto On
        }
        if (v === mButtonMatchAutoOn) {
            //DEBUG Log.d(TAG, "Clicked VERIFY");
            debugMessage("Clicked VERIFY WITH AUTO ON\n")
            bVerifyAutoOnMode = true
            mAutoOnEnabled = true
            mTextViewResult!!.text = "Auto On enabled for verification"
            autoOn!!.start() //Enable Auto On
        }
    }



}