package com.biometricscan.principal;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;



import com.biometricscan.principal.models.LoginResponse;
import com.biometricscan.principal.utils.APIService;
import com.biometricscan.principal.utils.PlaySound;
import com.biometricscan.principal.utils.VariablesToConnect;
import com.futronictech.ScanAPIHelper.UsbDeviceDataExchangeImpl;
import com.futronictech.ftrWsqAndroidHelper;
import com.google.gson.JsonObject;
import com.integratedbiometrics.ibscanultimate.IBScan;
import com.integratedbiometrics.ibscanultimate.IBScanDevice;
import com.integratedbiometrics.ibscanultimate.IBScanDeviceListener;
import com.integratedbiometrics.ibscanultimate.IBScanException;
import com.integratedbiometrics.ibscanultimate.IBScanListener;
import com.kaopiz.kprogresshud.KProgressHUD;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.futronictech.ScanAPIHelper.*;

/**
 * Code created by Carlos Cruz Ordoñez
 * ccruz@freewayconsulting.com
 */

public class MainActivity extends Activity  {

    /**
     * Called when the activity is first created.
     */
    private Button btnActivateScan;



    public static final int MESSAGE_SHOW_MSG = 1;
    public static final int MESSAGE_SHOW_SCANNER_INFO = 2;
    public static final int MESSAGE_SHOW_IMAGE = 3;
    public static final int MESSAGE_ERROR = 4;
    public static byte[] mImageFP = null;
    public static int mImageWidth = 0;
    public static int mImageHeight = 0;
    private static Bitmap mBitmapFP = null;


    private static final int PERMISSION_ALL = 101;

    String[] PERMISSIONS = {
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.ACCESS_NETWORK_STATE,
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.RECORD_AUDIO
    };


    private String idAccount;


    private KProgressHUD saveProgress;


    /*
     * Enum representing the application state.  The application will move between states based on
     * callbacks from the IBScan library.  There are two methods associated with each state.  The
     * "transition" method is call by the app to move to a new state and sends a message to a
     * handler.  The "handleTransition" method is called by that handler to effect the transition.
     */
    private static enum AppState {
        NO_SCANNER_ATTACHED,
        SCANNER_ATTACHED,
        REFRESH,
        INITIALIZING,
        SAVE_IMAGE,
        CLOSING,
        STARTING_CAPTURE,
        CAPTURING,
        STOPPING_CAPTURE,
        IMAGE_CAPTURED,
        COMMUNICATION_BREAK;
    }

    /* *********************************************************************************************
     * PRIVATE FIELDS (UI COMPONENTS)
     ******************************************************************************************** */
    private Button connect;
    private Button refresh;
    private TextView m_txtDeviceCount;
    private TextView m_txtStatus;
    private TextView m_txtDesciption;
    private TextView m_txtFrameTime;
    private static ImageView m_imagePreviewImage;
    private TextView[] m_txtFingerQuality = new TextView[FINGER_QUALITIES_COUNT];
    private Spinner m_spinnerCaptureType;
    private Button m_startCaptureBtn;
    private Button m_stopCaptureBtn;
    private Button m_save_ImageBtn;
    private Button m_closeScannerBtn;
    private Button m_refreshBtn;
    private Bitmap m_BitmapImage;
    private Bitmap m_BitmapKojakRollImage;


    /* *********************************************************************************************
     * PRIVATE CONSTANTS
     ******************************************************************************************** */

    /* The tag used for Android log messages from this app. */
    private static final String TAG = "Simple Scan";

    /* The default value of the status TextView. */
    private static final String STATUS_DEFAULT = "";

    /* The default value of the frame time TextView. */
    private static final String FRAME_TIME_DEFAULT = "n/a";

    /* The value of AppData.captureType when the capture type has never been set. */
    private static final int CAPTURE_TYPE_INVALID = -1;

    /* The number of finger qualities set in the preview image. */
    private static final int FINGER_QUALITIES_COUNT = 4;

    /* The description in the device description TextView when no device is attached. */
    private static final String NO_DEVICE_DESCRIPTION_STRING = "(no scanner)";

    /* The background color of the device description TextView when no device is attached. */
    private static final int NO_DEVICE_DESCRIPTION_COLOR = Color.RED;


    /* The background color of the preview image ImageView. */
    private static final int PREVIEW_IMAGE_BACKGROUND = Color.LTGRAY;

    /* The background color of a finger quality TextView when the finger is not present. */
    private static final int FINGER_QUALITY_NOT_PRESENT_COLOR = Color.LTGRAY;


    public static boolean mUsbHostMode = true;
    private List<String> mDevNames = new ArrayList<String>();
    private FPScan mFPScan = null;
    private ArrayAdapter<String> mDevNameAdapter = null;
    private UsbDeviceDataExchangeImpl usb_host_ctx = null;
    List<UsbDeviceDataExchangeImpl.FtrUsbDevice> mUsbHostDevs = null;
    private Spinner mDevInstaces;
    private CheckBox mCheckUsbHostMode;
    public static boolean mStop = false;
    public static Object mSyncObj= new Object();
    public static boolean mLFD = false;
    public static boolean mInvertImage = false;
    public static boolean mFrame = true;
    private static int[] mPixels = null;
    private static Canvas mCanvas = null;
    private static Paint mPaint = null;
    private static File mDir;
    private String mFileName ="HUELLA.wsq";





    /*
     * This class wraps the data saved by the app for configuration changes.
     */
    private class AppData {
        /* The state of the application. */
        public AppState state = AppState.NO_SCANNER_ATTACHED;

        /* The type of capture currently selected. */
        public int captureType = CAPTURE_TYPE_INVALID;

        /* The current contents of the status TextView. */
        public String status = STATUS_DEFAULT;

        /* The current contents of the frame time TextView. */
        public String frameTime = FRAME_TIME_DEFAULT;

        /* The current image displayed in the image preview ImageView. */
        public Bitmap imageBitmap = null;

        /* The current background colors of the finger quality TextViews. */
        public int[] fingerQualityColors = new int[]
                {FINGER_QUALITY_NOT_PRESENT_COLOR, FINGER_QUALITY_NOT_PRESENT_COLOR,
                        FINGER_QUALITY_NOT_PRESENT_COLOR, FINGER_QUALITY_NOT_PRESENT_COLOR};


        /* Indicates whether the image preview ImageView can be long-clicked. */
        public boolean imagePreviewImageClickable = false;

        /* The current contents of the device description TextView. */
        public String description = NO_DEVICE_DESCRIPTION_STRING;

        /* The current background color of the device description TextView. */
        public int descriptionColor = NO_DEVICE_DESCRIPTION_COLOR;

        /* The current device count displayed in the device count TextView. */
        public int deviceCount = 0;

    }


    private AppData m_savedData = new AppData();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }
        /*if (getIntent().getData() != null) {
            idAccount = getIntent().getData().getQueryParameter("idAccount");
        } else {
            onBackPressed();
            System.exit(0);
        }*/
        initUIFields();
        mDevNameAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, mDevNames);
        this.mDevInstaces.setAdapter(mDevNameAdapter);
        usb_host_ctx = new UsbDeviceDataExchangeImpl(this, mHandler);
        EnumDevices();

        populateUI();

        if(mUsbHostDevs.size() > 0){
            transitionToRefresh();
        } else{
            this.showToastOnUiThread("Sin escaner",2000);
        }

    }
    @Override
    protected void onResume() {
        super.onResume();
        EnumDevices();
        /*if (getIntent().getData() != null) {
            idAccount = getIntent().getData().getQueryParameter("idAccount");
        } else {
            onBackPressed();
            System.exit(0);
        }*/
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_ALL: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
   }
                return;
            }
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void initUIFields() {


        this.btnActivateScan = (Button) findViewById(R.id.btnActivateScan);
        this.btnActivateScan.setOnClickListener(this.accion);


        this.m_txtDeviceCount = (TextView) findViewById(R.id.device_count);
        this.m_txtStatus = (TextView) findViewById(R.id.status);
        this.m_txtDesciption = (TextView) findViewById(R.id.description);

        /* Hard-coded for four finger qualities. */
        this.m_txtFingerQuality[0] = (TextView) findViewById(R.id.scan_states_color1);
        this.m_txtFingerQuality[1] = (TextView) findViewById(R.id.scan_states_color2);
        this.m_txtFingerQuality[2] = (TextView) findViewById(R.id.scan_states_color3);
        this.m_txtFingerQuality[3] = (TextView) findViewById(R.id.scan_states_color4);

        this.m_txtFrameTime = (TextView) findViewById(R.id.frame_time);


        this.m_imagePreviewImage = (ImageView) findViewById(R.id.preview_image);
        this.m_imagePreviewImage.setBackgroundColor(PREVIEW_IMAGE_BACKGROUND);
        this.m_imagePreviewImage.setOnLongClickListener(this.m_imagePreviewImageLongClickListener);

        this.m_stopCaptureBtn = (Button) findViewById(R.id.stop_capture_btn);
        this.m_stopCaptureBtn.setOnClickListener(this.m_stopCaptureBtnClickListener);

        this.m_startCaptureBtn = (Button) findViewById(R.id.start_capture_btn);
        this.m_startCaptureBtn.setOnClickListener(this.m_startCaptureBtnClickListener);

        this.m_save_ImageBtn = (Button) findViewById(R.id.save_Image);
        this.m_save_ImageBtn.setOnClickListener(this.m_saveImgBtnClickListener);

        this.m_closeScannerBtn = (Button) findViewById(R.id.close_scanner_btn);
        this.m_closeScannerBtn.setOnClickListener(this.m_closeScannerBtnClickListener);

        this.m_refreshBtn = (Button) findViewById(R.id.refresh_btn);

        this.connect = (Button) findViewById(R.id.connect);
        this.connect.setOnClickListener(this.connectListener);

        this.refresh = (Button) findViewById(R.id.refresh);
        this.refresh.setOnClickListener(this.refreshListener);

        this.mDevInstaces = (Spinner) findViewById(R.id.spinnerDevs);
        this.mCheckUsbHostMode = (CheckBox) findViewById(R.id.cbUsbHostMode);

        this.m_spinnerCaptureType = (Spinner) findViewById(R.id.capture_type);

        this.m_spinnerCaptureType.setOnItemSelectedListener(this.m_captureTypeItemSelectedListener);

        this.mFrame = true;
        this.mUsbHostMode = true;
        this.mLFD = this.mInvertImage = false;
        this.m_save_ImageBtn.setEnabled(false);
        m_startCaptureBtn.setEnabled(false);
        m_stopCaptureBtn.setEnabled(false);
        m_closeScannerBtn.setEnabled(false);
        connect.setVisibility(View.INVISIBLE);

    }


    /*
     * Populate UI with data from old orientation.
     */
    private void populateUI() {

        setDescription(this.m_savedData.description, this.m_savedData.descriptionColor);

        if (this.m_savedData.status != null) {
            this.m_txtStatus.setText(this.m_savedData.status);
        }
        if (this.m_savedData.frameTime != null) {
            this.m_txtFrameTime.setText(this.m_savedData.frameTime);
        }
        if (this.m_savedData.imageBitmap != null) {
            this.m_imagePreviewImage.setImageBitmap(this.m_savedData.imageBitmap);
        }

        for (int i = 0; i < FINGER_QUALITIES_COUNT; i++) {
            this.m_txtFingerQuality[i].setBackgroundColor(this.m_savedData.fingerQualityColors[i]);
        }

        if (this.m_savedData.captureType != CAPTURE_TYPE_INVALID) {
            this.m_spinnerCaptureType.setSelection(this.m_savedData.captureType);
        }

        if (m_BitmapImage != null) {
            m_BitmapImage.isRecycled();
        }

        if (m_BitmapKojakRollImage != null) {
            m_BitmapKojakRollImage.isRecycled();
        }

    }

    private void EnumDevices() {

        mDevNames.clear();
        if(this.mCheckUsbHostMode.isChecked()) {
            mUsbHostDevs = usb_host_ctx.GetInterfaces();
            if (mUsbHostDevs.size() > 0) {
                int devIndex = 1;
                for (UsbDeviceDataExchangeImpl.FtrUsbDevice dev : mUsbHostDevs) {

                    String sn = dev.getSerialNumber() != "" ? dev.getSerialNumber() : dev.getDeviceName();
                    mDevNames.add(String.format("%d: %s", devIndex, sn.length() > 0 ? sn : "<no id>"));
                    devIndex++;
                }
                mDevInstaces.setSelection(0);
            }


            if (mUsbHostDevs.size() > 0) {
                mDevInstaces.setSelection(0);
            }
        } else {
            byte[] interfaceStatus = new byte[128];
            com.futronictech.ScanAPIHelper.Scanner enumScaner = new com.futronictech.ScanAPIHelper.Scanner();
            if( enumScaner.GetInterfaces(interfaceStatus) ) {
                boolean notEmpty = false;
                int devIndex = 0;
                for (byte status : interfaceStatus ) {
                    if( status == com.futronictech.ScanAPIHelper.Scanner.FTRSCAN_INTERFACE_STATUS_CONNECTED ) {

                        StringBuffer devNameBuffer = new StringBuffer();
                        String devName = "<empty>";
                        com.futronictech.ScanAPIHelper.Scanner scanner = new com.futronictech.ScanAPIHelper.Scanner();

                        if( scanner.GetDeviceNameOnInterface(devIndex,devNameBuffer) ) {
                            devName = devNameBuffer.toString();
                        }

                        mDevNames.add(String.format("%d: %s",  devIndex + 1, devName));
                        devIndex++;
                        notEmpty = true;
                    }
                }

                if (notEmpty) {
                    mDevInstaces.setSelection(0);
                }
            }
        }

    }

    public static void InitFingerPictureParameters(int wight, int height)
    {
        mImageWidth = wight;
        mImageHeight = height;

        mImageFP = new byte[MainActivity.mImageWidth*MainActivity.mImageHeight];
        mPixels = new int[MainActivity.mImageWidth*MainActivity.mImageHeight];

        mBitmapFP = Bitmap.createBitmap(wight, height, Bitmap.Config.RGB_565);

        mCanvas = new Canvas(mBitmapFP);
        mPaint = new Paint();

        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        mPaint.setColorFilter(f);
    }

    /* *********************************************************************************************
     * EVENT HANDLERS
     ******************************************************************************************** */

    /*
    Refresh scann and image
     */

    private View.OnClickListener refreshListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            /* Sanity check.  Make sure we are in a proper state.*/

            m_savedData.imageBitmap = null;
            m_imagePreviewImage.setImageBitmap(m_savedData.imageBitmap);
            transitionToRefresh();

        }
    };

    /*
     * Connect to SF
     */

    private View.OnClickListener connectListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {


            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    saveProgress = KProgressHUD.create(MainActivity.this)
                            .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                            .setLabel(getString(R.string.espera_momento))
                            .setDetailsLabel(getString(R.string.enviando))
                            .setCancellable(false)
                            .setAnimationSpeed(2)
                            .setDimAmount(0.5f)
                            .show();
                }
            });


            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(VariablesToConnect.baseURL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            APIService apiService = retrofit.create(APIService.class);

            Call<LoginResponse> callToken = apiService.getToken("password", VariablesToConnect.clientId, VariablesToConnect.client_secret, VariablesToConnect.username, VariablesToConnect.password);

            callToken.enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, final Response<LoginResponse> response) {

                    if (response.code() == 200) {
                        Log.d("RESPONSE: ", "" + response.body().toString());

                        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(new Interceptor() {
                            @Override
                            public okhttp3.Response intercept(Chain chain) throws IOException {
                                Request newRequest = chain.request().newBuilder()
                                        .addHeader("Authorization", "OAuth " + response.body().getAccess_token())
                                        .build();
                                return chain.proceed(newRequest);
                            }
                        }).build();

                        Retrofit retrofit = new Retrofit.Builder()
                                .client(client)
                                .baseUrl(String.format("%s/services/apexrest/", VariablesToConnect.baseURL))
                                .addConverterFactory(GsonConverterFactory.create())
                                .build();

                        APIService serviceApi = retrofit.create(APIService.class);

                        String wsqBase64 = convierteBase64();

                        JsonObject json = new JsonObject();
                        json.addProperty("idAccount", idAccount);
                        json.addProperty("fingerprint", wsqBase64);

                        Log.d("JSONFingerPrint", json.toString());
                        Call<ResponseBody> callSendFinger = serviceApi.sendFingerprinSF(json);

                        callSendFinger.enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                Log.d("Response Code", "" + response.code());


                                saveProgress.dismiss();
                                onBackPressed();
                                System.exit(0);
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {

                            }
                        });

                    }
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable throwable) {
                    Log.d("Error", "" + throwable.toString());
                }
            });

        }
    };

    private View.OnClickListener accion = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

        }
    };

    /*
     * Handle click on "Start capture" button.
     */
    private View.OnClickListener m_startCaptureBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {

            /* Transition to capturing state. */
            transitionToStartingCapture();
        }
    };

    /*
     * Handle click on "Stop capture" button.
     */
    private View.OnClickListener m_stopCaptureBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            transitionToStoppingCapture();
        }
    };

    /*
     * Handle long clicks on the image view.
     */
    private View.OnLongClickListener m_imagePreviewImageLongClickListener = new View.OnLongClickListener() {
        /*
         * When the image view is long-clicked, show a popup menu.
         */
        @Override
        public boolean onLongClick(final View v) {
            final PopupMenu popup = new PopupMenu(MainActivity.this, MainActivity.this.m_txtDesciption);
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                /*
                 * Handle click on a menu item.
                 */
                @Override
                public boolean onMenuItemClick(final MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.email_image:
                            return (true);
                        default:
                            return (false);
                    }
                }

            });

            final MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.scanimage_menu, popup.getMenu());
            popup.show();

            return (true);
        }
    };

    public String convierteBase64() {
        try {
            File imageFile = new File(Environment.getExternalStorageDirectory() + "/Pictures/SCANNER/HUELLA.wsq");
            File listFiles = new File(Environment.getExternalStorageDirectory() + "/Pictures");
            for (File f : listFiles.listFiles()) {
                Log.d("ERROR WSQ", "ARCHIVO: " + f.getName());
                imageFile = f;
            }


            String fileToBase64 = getStringFile(imageFile);
            imageFile.delete();
            return fileToBase64;
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("ERROR WQS", "MIRA: " + e.getMessage());
        }
        Log.d("ERROR WQS", "UNERROR");
        return null;
    }

    public String getStringFile(File f) {
        InputStream inputStream = null;
        String encodedFile = "", lastVal;
        try {
            inputStream = new FileInputStream(f.getAbsolutePath());

            byte[] buffer = new byte[10240];//specify the size to allow
            int bytesRead;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Base64OutputStream output64 = new Base64OutputStream(output, Base64.NO_WRAP);

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output64.write(buffer, 0, bytesRead);
            }


            output64.close();


            encodedFile = output.toString();

        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        lastVal = encodedFile;
        return lastVal;
    }

    /*
     * Handle click on "Open" button.
     */
    private View.OnClickListener m_saveImgBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            /* Transition to initializing state. */
            transitionToSave();
        }
    };

    /*
     * Handle click on "Close" button.
     */
    private View.OnClickListener m_closeScannerBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            transitionToClosing();
        }
    };

    /*
     * Handle click on the spinner that determine the scan type.
     */
    private AdapterView.OnItemSelectedListener m_captureTypeItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(final AdapterView<?> parent, final View view, final int pos,
                                   final long id) {
            /* Save capture type for screen orientation change. */
            MainActivity.this.m_savedData.captureType = pos;
        }

        @Override
        public void onNothingSelected(final AdapterView<?> parent) {
            MainActivity.this.m_savedData.captureType = CAPTURE_TYPE_INVALID;
        }
    };



    private void showToastOnUiThread(final String message, final int duration) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(getApplicationContext(), message, duration);
                toast.show();
            }
        });
    }


    private void transitionToRefresh() {
        final Message msg = this.m_scanHandler.obtainMessage(AppState.REFRESH.ordinal());
        this.m_scanHandler.sendMessage(msg);
    }

    private void transitionToSave() {
        final Message msg = this.m_scanHandler.obtainMessage(AppState.SAVE_IMAGE.ordinal());
        this.m_scanHandler.sendMessage(msg);
    }


    private void transitionToClosing() {
        final Message msg = this.m_scanHandler.obtainMessage(AppState.CLOSING.ordinal());
        this.m_scanHandler.sendMessage(msg);
    }

    private void transitionToStartingCapture() {
        final Message msg = this.m_scanHandler.obtainMessage(AppState.STARTING_CAPTURE.ordinal());
        this.m_scanHandler.sendMessage(msg);
    }


    private void transitionToStoppingCapture() {
        final Message msg = this.m_scanHandler.obtainMessage(AppState.STOPPING_CAPTURE.ordinal());
        this.m_scanHandler.sendMessage(msg);
    }



    /*
     * A handler to process state transition messages.
     */
    private Handler m_scanHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(final Message msg) {
            final AppState nextState = AppState.values()[msg.what];

            switch (nextState) {

                case REFRESH:
                    handleTransitionToRefresh();
                    break;

                case SAVE_IMAGE: {
                    handleTransitionToSave();
                    break;
                }

                case CLOSING:
                    handleTransitionToClosing();
                    break;

                case STARTING_CAPTURE:
                    handleTransitionToStartingCapture();
                    break;


                case STOPPING_CAPTURE:
                    handleTransitionToStoppingCapture();
                    break;

                case COMMUNICATION_BREAK:
                    handleTransitionToCommunicationBreak();
                    break;
            }

            return (false);
        }
    });

    /*
     * Set status in status field.  Save value for orientation change.
     */
    private void setStatus(final String s) {
        this.m_savedData.status = s;

        /* Make sure this occurs on the UI thread. */
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MainActivity.this.m_txtStatus.setText(s);
            }
        });
    }

    /*
     * Set frame time in frame time field.  Save value for orientation change.
     */
    private void setFrameTime(final String s) {
        this.m_savedData.frameTime = s;

        /* Make sure this occurs on the UI thread. */
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MainActivity.this.m_txtFrameTime.setText(s);
            }
        });
    }

    /*
     * Handle transition to communication break.
     */
    private void handleTransitionToCommunicationBreak() {

        /* Move to this state. */
        this.m_savedData.state = AppState.COMMUNICATION_BREAK;

        /* Setup UI for this state. */
        EnableControls(false,false,false,false);
        setStatus("comm break");
        setFrameTime(FRAME_TIME_DEFAULT);

        /* Transition to closing, then to refresh. */
        transitionToClosing();
    }

    /*
     * Set description of header in finger print image box.
     */
    private void setDescription(final String description, final int color) {
        this.m_savedData.description = description;
        this.m_savedData.descriptionColor = color;
        /* Make sure this occurs on the UI thread. */
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MainActivity.this.m_txtDesciption.setText(description);
                MainActivity.this.m_txtDesciption.setBackgroundColor(color);
            }
        });
    }

    /*
     * Handle transition to refresh state.
     */
    private void handleTransitionToRefresh() {

                this.m_savedData.state = AppState.REFRESH;
                /* Setup UI for state. */
                setStatus("refreshing");
                setFrameTime(FRAME_TIME_DEFAULT);
                if(mDevNames.size() > 0) {
                    setDescription("Escaner detectado: " + mDevNames.get(0), Color.WHITE);
                    EnableControls(true,false,true,false);
                }
                setCaptureTypes(new String[0], 0);
    }


    /*
     * Handle transition to stopping capture state.
     */
    private void handleTransitionToStoppingCapture() {

        /* Move to this state. */
        this.m_savedData.state = AppState.STOPPING_CAPTURE;

        /* Setup UI for state. */
        setStatus("stopping");
        setFrameTime(FRAME_TIME_DEFAULT);
        mStop = true;
        if( mFPScan != null )
        {
            mFPScan.stop();
            mFPScan = null;

        }
        if(mUsbHostMode)
        {
            usb_host_ctx.CloseDevice();
        }
        transitionToRefresh();
    }



    /*
     * Handle transition to starting capture state.
     */
    private void handleTransitionToStartingCapture() {

        /* Move to this state. */
        this.m_savedData.state = AppState.STARTING_CAPTURE;
        EnableControls(false,false,false,true);
        mUsbHostMode = true;
        setDescription("Starting capture",Color.GREEN);
        if( mFPScan != null )
        {
            mStop = true;
            mFPScan.stop();
        }
        mStop = false;
        if(mUsbHostMode) {
            int targetInstance = 0;

            if (mUsbHostDevs.size() > 0) {
                targetInstance = mUsbHostDevs.get(0).getInstance();
            }

            if (usb_host_ctx.OpenDevice(targetInstance, true)) {
                    StartScan();
            }
        }else
        {
                StartScan();
        }

    }
    private void StartScan()
    {
        mFPScan = new FPScan(usb_host_ctx, mHandler, mDevInstaces.getSelectedItemPosition());
        mFPScan.start();
    }

    private void EnableControls(boolean startCapture,boolean saveImage, boolean closeScanner, boolean stop)
    {

        MainActivity.this.m_startCaptureBtn.setEnabled(startCapture);
        MainActivity.this.m_startCaptureBtn.setClickable(startCapture);

        MainActivity.this.m_stopCaptureBtn.setEnabled(stop);
        MainActivity.this.m_stopCaptureBtn.setClickable(stop);

        MainActivity.this.m_closeScannerBtn.setEnabled(closeScanner);
        MainActivity.this.m_closeScannerBtn.setClickable(closeScanner);




        if (m_savedData.imageBitmap != null)
            connect.setVisibility(View.VISIBLE);
        else
            connect.setVisibility(View.GONE);
        if(mImageFP != null){
            this.m_save_ImageBtn.setEnabled(true);
            this.m_save_ImageBtn.setClickable(true);
        }

    }


    /*
     * Handle transition to closing state.
     */
    private void handleTransitionToClosing() {

        /* Move to this state. */
        this.m_savedData.state = AppState.CLOSING;

        /* Setup the UI for state. */
        this.showToastOnUiThread("Cerrando escáner",2000);
        this.exitApp(this);
    }

    /*
     * Handle transition to starting capture state.
     */
    private void handleTransitionToSave() {

        /* Move to this state. */
        this.m_savedData.state = AppState.SAVE_IMAGE;

        /* Setup the UI for state. */
        EnableControls(false,true,true,false);
        setStatus("initialized");
        if( mImageFP != null && isImageFolder()) {
            SaveImageByFileFormat();
        }
        transitionToRefresh();
        }




    /*
     * Set capture types.
     */
    private void setCaptureTypes(final String[] captureTypes, final int selectIndex) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(MainActivity.this,
                        android.R.layout.simple_spinner_item, captureTypes);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                MainActivity.this.m_spinnerCaptureType.setAdapter(adapter);
                MainActivity.this.m_spinnerCaptureType.setSelection(selectIndex);
            }
        });
    }

    /*
     * Exit application.
     */
    private static void exitApp(Activity ac) {
        ac.moveTaskToBack(true);
        ac.finish();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private void SaveImageByFileFormat()
    {
            Scanner devScan = new Scanner();
            boolean bRet = false;
        String extraString =  mDir.getAbsolutePath() + "/"+ mFileName;
        if( mUsbHostMode )
            {
                int targetInstance = 0;
                if(mUsbHostDevs.size() > 0) {
                    targetInstance = mUsbHostDevs.get(0).getInstance();
                }
                if( usb_host_ctx.OpenDevice(targetInstance, true) )
                    bRet = devScan.OpenDeviceOnInterfaceUsbHost(usb_host_ctx);
            }
            else
                bRet = devScan.OpenDevice();
            if( !bRet )
            {
                return;
            }
            byte[] wsqImg = new byte[mImageWidth*mImageHeight];
            long hDevice = devScan.GetDeviceHandle();
            ftrWsqAndroidHelper wsqHelper = new ftrWsqAndroidHelper();
            if( wsqHelper.ConvertRawToWsq(hDevice, mImageWidth, mImageHeight, 2.25f, mImageFP, wsqImg) )
            {
                File file = new File(extraString);
                try {
                    FileOutputStream out = new FileOutputStream(file);
                    out.write(wsqImg, 0, wsqHelper.mWSQ_size);	// save the wsq_size bytes data to file
                    showToastOnUiThread("Imagen guardada como 'HUELLA.WSQ' ",2000);
                    this.m_savedData.imageBitmap =mBitmapFP;
                    out.close();
                } catch (Exception e) {
                    showToastOnUiThread("Exception : " + e.getMessage(),2000);
                }
            }
            else
                showToastOnUiThread("Error al convertir la imagen ",2000);
            if( mUsbHostMode )
                devScan.CloseDeviceUsbHost();
            else
                devScan.CloseDevice();
            return;
    }


    // The Handler that gets information back from the FPScan
    private static final Handler mHandler = new Handler() {
        @Override
        public  void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_SHOW_MSG:
                    String showMsg = (String) msg.obj;

                    break;
                case MESSAGE_SHOW_SCANNER_INFO:
                    String showInfo = (String) msg.obj;

                    break;
                case MESSAGE_SHOW_IMAGE:
                    ShowBitmap();
                    break;
                case MESSAGE_ERROR:
                    //mFPScan = null;

                    break;
            }
        }
    };

    private static void ShowBitmap() {
        int[] pixels = new int[mImageWidth * mImageHeight];
        for (int i = 0; i < mImageWidth * mImageHeight; i++)
            pixels[i] = mImageFP[i];
        Bitmap emptyBmp = Bitmap.createBitmap(pixels, mImageWidth, mImageHeight, Bitmap.Config.RGB_565);

        int width, height;
        height = emptyBmp.getHeight();
        width = emptyBmp.getWidth();

        mBitmapFP = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(mBitmapFP);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(emptyBmp, 0, 0, paint);
        m_imagePreviewImage.setImageBitmap(mBitmapFP);
    }


    public boolean isImageFolder()
    {
        File extStorageDirectory = Environment.getExternalStorageDirectory();
        mDir = new File(extStorageDirectory, "Pictures/SCANNER");
        if( mDir.exists() ) {
            if( !mDir.isDirectory() )
            {
                return false;
            }
        } else {
            try {
                mDir.mkdirs();
            }
            catch( SecurityException e )
            {
                return false;
            }
        }
        return true;
    }
}
