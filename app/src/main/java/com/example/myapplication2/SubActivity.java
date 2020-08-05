package com.example.myapplication2;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;


import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Formatter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Utf8;

public class SubActivity extends AppCompatActivity {

    private static final int LOAD_SUCCESS = 101;
    private ProgressDialog progressDialog;
    private String access_token;
    private String SecPin;
    private TextView textviewsum;
    private String hskey;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.subactivity_main);

        Intent intent = getIntent();
//        Bundle bundle = intent.getExtras();
//        access_token = bundle.getString("access_toekn");
//        SecPin = bundle.getString("SecPin");
        access_token = intent.getExtras().getString("access_token");
        SecPin = intent.getExtras().getString("SecPin");

        System.out.println(access_token);
        System.out.println(SecPin);

        TextView textview;
        textview = (TextView)findViewById(R.id.test);
        textview.setText("access  :  "+access_token+"\n"+"sec  :  "+SecPin);


        //총자산평가
        Button buttongetsum = (Button) findViewById(R.id.get_sum);
        textviewsum = (TextView) findViewById(R.id.test2);
        textviewsum.setMovementMethod(new ScrollingMovementMethod());

        buttongetsum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                progressDialog = new ProgressDialog(SubActivity.this);
                progressDialog.setMessage("Please wait.....");
                progressDialog.show();

                getSum(); //총자산평가 조회
            }
        });
    }


    private final SubActivity.MyHandler mHandler = new SubActivity.MyHandler(this);

    private static class MyHandler extends Handler {

        private final WeakReference<SubActivity> weakReference;

        public MyHandler(SubActivity subactivity) {
            weakReference = new WeakReference<SubActivity>(subactivity);
        }

        @Override
        public void handleMessage(Message msg) {

            SubActivity subActivity = weakReference.get();

            if (subActivity != null) {
                // 로딩 팝업
                String jsonString;
                switch (msg.what) {
                    case LOAD_SUCCESS:
                        subActivity.progressDialog.dismiss();

                        jsonString = (String) msg.obj;

                        subActivity.textviewsum.setText(jsonString);
                        break;
                }
            }
        }
    }


    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    private static String toHexString(byte[] bytes) {

        Formatter formatter = new Formatter();

        for (byte b : bytes) {
            formatter.format("%02x", b);
        }

        return formatter.toString();

    }

    public static String toBase64String(byte[] bytes){

        byte[] byteArray = Base64.encodeBase64(bytes);
        return new String(byteArray);

    }

    public static String encryption(String data, String key) throws SignatureException, NoSuchAlgorithmException, InvalidKeyException {

        SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
        mac.init(signingKey);

        return toBase64String(mac.doFinal(data.getBytes()));

    }


    public String getHashKey(String jsonStr, String key, String charset){
        String hash = "";
        try{
            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretkey = new SecretKeySpec(key.getBytes(), "HmacSHA256");
            sha256HMAC.init(secretkey);

            //hash = Base64.encodeBase64String(sha256HMAC.doFinal(jsonStr.getBytes(charset)));
            hash = toBase64String(sha256HMAC.doFinal(jsonStr.getBytes(charset)));

        }catch (Exception e) {
        }
        return  hash;
    }







    public void getSum() {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String result = null;

                try {

                    OkHttpClient client = new OkHttpClient().newBuilder()
                            .build();
                    MediaType mediaType = MediaType.parse("application/json");
                    JSONObject jsonBody = new JSONObject();
                    JSONObject json_dataHeader = new JSONObject();
                    JSONObject json_dataBody = new JSONObject();

                    // dataHeader 세팅
                    json_dataHeader.put("carrier" ,"KT");
                    json_dataHeader.put("appVersion" , "..");
                    json_dataHeader.put("deviceOs", "Android");
                    json_dataHeader.put("appName" , "..");
                    json_dataHeader.put("subChannel" ,"subChannel");
                    json_dataHeader.put("deviceModel" ,"Android");
                    json_dataHeader.put("udId" , "UDID");
                    json_dataHeader.put("connectionType" , "..");


                    // databody 세팅
                    json_dataBody.put("secPINNumber", "H005629572991"); //saml 입력


                    jsonBody.put("dataHeader", json_dataHeader);
                    jsonBody.put("dataBody", json_dataBody);



                    System.out.println(jsonBody);
                    RequestBody body = RequestBody.create(mediaType,jsonBody.toString());

                    hskey = getHashKey(jsonBody.toString(), access_token, "UTF-8");
                    System.out.println(hskey);
                    Request request = new Request.Builder()
                            .url("https://oapidev.kbsec.com:8443/v1.0/KSA/accountInfo/totalAssetInfo")
                            .method("POST", body)
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Authorization", "Bearer "+access_token)
                            .addHeader("hsKey", hskey)
                            .build();
                    Response response = client.newCall(request).execute();


                    result = response.body().string();


                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Message message = mHandler.obtainMessage(LOAD_SUCCESS, result);
                mHandler.sendMessage(message);
            }
        });
            thread.start();

    }
}
