package com.example.myapplication2;

import androidx.appcompat.app.AppCompatActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.*;
import java.io.IOException;
import java.lang.ref.WeakReference;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.*;


public class MainActivity extends AppCompatActivity {

    private static final int LOAD_SUCCESS = 101;
    private static final int LOAD_SUCCESS2 = 102;
    private ProgressDialog progressDialog;
    private TextView textviewJSONText1;
    private TextView textviewJSONText2;

    String saml_res;
    String access_token;
    String SecPin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        // 버튼을 눌러 saml데이터 요청
        Button buttonRequestJSON = (Button) findViewById(R.id.button_main_requestjson);
        textviewJSONText1 = (TextView) findViewById(R.id.textview_main_jsontext);
        textviewJSONText1.setMovementMethod(new ScrollingMovementMethod());

        buttonRequestJSON.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setMessage("Please wait.....");
                progressDialog.show();

                getJSON(); // 버튼을 눌러 JSON데이터 요청(saml)
            }
        });

        // 버튼을 눌러 accesstoken 요청
        Button buttonRequestJSON2 = (Button) findViewById(R.id.button_main_requestjson2);
        textviewJSONText2 = (TextView) findViewById(R.id.textview_main_jsontext2);
        textviewJSONText2.setMovementMethod(new ScrollingMovementMethod());

        buttonRequestJSON2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setMessage("Please wait.....");
                progressDialog.show();

                getJSON_ACCESSTOKEN(); // 버튼을 눌러 JSON데이터 요청(saml)
            }
        });




        //자산현황 조회하기
        Button buttongetasset = (Button) findViewById(R.id.get_asset);
        buttongetasset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //PinNUM입력받기
                EditText et = (EditText)findViewById(R.id.SetPinNumber);
                SecPin = et.getText().toString();
                System.out.println(SecPin);

//                Bundle bundle = new Bundle();
//                bundle.putString("access_token",access_token);
//                bundle.putString("SecPin",SecPin);

                //Token, PinNum 전송
                Intent intent = new Intent(MainActivity.this, SubActivity.class);
                intent.putExtra("access_token",access_token);
                intent.putExtra("SecPin",SecPin);
                startActivity(intent);

            }
        });

    }

    private final MyHandler mHandler = new MyHandler(this);

    private static class MyHandler extends Handler {

        private final WeakReference<MainActivity> weakReference;

        public MyHandler(MainActivity mainactivity) {
            weakReference = new WeakReference<MainActivity>(mainactivity);
        }

        @Override
        public void handleMessage(Message msg) {

            MainActivity mainactivity = weakReference.get();

            if (mainactivity != null) {
                // 로딩 팝업
                String jsonString;
                switch (msg.what) {
                    case LOAD_SUCCESS:
                        mainactivity.progressDialog.dismiss();

                        jsonString = (String) msg.obj;

                        mainactivity.textviewJSONText1.setText(jsonString);
                        break;
                    case LOAD_SUCCESS2:
                        mainactivity.progressDialog.dismiss();

                        jsonString = (String) msg.obj;

                        mainactivity.textviewJSONText2.setText(jsonString);
                        break;
                }
            }
        }
    }

    public void getJSON() {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String result = null;

                try {
                    OkHttpClient client = new OkHttpClient().newBuilder()
                            .build();
                    MediaType mediaType = MediaType.parse("application/json");
                    RequestBody body = RequestBody.create(mediaType, "{\n\t\"dataHeader\" : {\n\t\t\"udId\" : \"UDID\",\n\t\t\"subChannel\" : \"subChannel\",\n\t\t\"deviceModel\" : \"Android\",\n\t\t\"deviceOs\" : \"Android\",\n\t\t\"carrier\" : \"KT\",\n\t\t\"connectionType\" : \"..\",\n\t\t\"appName\" : \"..\",\n\t\t\"appVersion\" : \"..\",\n\t\t\"hsKey\" : \"body\",\n\t\t\"scrNo\" : \"0000\"\n\t},\n\t\"dataBody\" : {\n\t\t\"type\" : \"4\",\n\t\t\"ciNo\" : \"yOV2MgaR+OoKU8mSzZn4LTiV8nYEKgHTFOmYI+z120IhHBzXuZmOglpa6YnPfpRjSDvhE4Uk9xAdpkisqrS7ig==\",\n\t\t\"clientId\" : \"l7xx1b65f79ae0d642248d5980fae64820c7\",\n\t\t\"loginType\" : \"2\"\n\t}\n}");
                    Request request = new Request.Builder()
                            .url("https://oapidev.kbsec.com:8443/v1.0/OAuth/saml/assertion")
                            .method("POST", body)
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Cookie", "WMONID=RntUzVTI98e; JSESSIONID=5ab51eYXskYTeM48BDB5ygcEki05atRUbV1y9ypax4fhg41fVu0dZhNtY6JleUg0.amV1c19kb21haW4va2JvcGVuYXBp")
                            .build();
                    Response response = client.newCall(request).execute();

                    result = response.body().string();

                    // samlAssertion값 파싱 후 저장
                    JSONObject jObject = new JSONObject(result);
                    String temp = jObject.getString("dataBody");
                    jObject = new JSONObject(temp);
                    saml_res = jObject.getString("samlAssertion");
                    System.out.println(result);

                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }

                Message message = mHandler.obtainMessage(LOAD_SUCCESS, saml_res);
                mHandler.sendMessage(message);
            }
        });

        try {
            thread.start();
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    public void getJSON_ACCESSTOKEN() {

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
                    json_dataHeader.put("udId","UDID");
                    json_dataHeader.put("subChannel" ,"subChannel");
                    json_dataHeader.put("deviceModel" ,"Android");
                    json_dataHeader.put("deviceOs", "Android");
                    json_dataHeader.put("carrier" ,"KT");
                    json_dataHeader.put("connectionType" , "..");
                    json_dataHeader.put("appName" , "KBSEC_APP");
                    json_dataHeader.put("appVersion" , "..");
                    json_dataHeader.put("scrNo" , "0000");

                    // databody 세팅
                    json_dataBody.put("samlAssertion", ""+saml_res+"="); //saml 입력
                    json_dataBody.put("clientId" , "l7xx6c19b49d527d467b97c1c4dab5fdf0e3");
                    json_dataBody.put("clientSecret" , "363eecba4a4d41b5a29fc45628430610");
                    json_dataBody.put("grantType" ,"urn:ietf:params:oauth:grant-type:saml2-bearer");
                    json_dataBody.put("scope" ,"public security");

                    jsonBody.put("dataHeader", json_dataHeader);
                    jsonBody.put("dataBody", json_dataBody);
                    System.out.println(jsonBody);
                    RequestBody body = RequestBody.create(mediaType,jsonBody.toString());
                    Request request = new Request.Builder()
                            .url("https://oapidev.kbsec.com:8443/v1.0/OAuth/token/access")
                            .method("POST", body)
                            .addHeader("Content-Type", "application/json")
                            .build();
                    Response response = client.newCall(request).execute();

                    result = response.body().string();

                    // accesstoken값 파싱 후 저장
                    JSONObject jObject = new JSONObject(result);
                    String temp = jObject.getString("dataBody");
                    jObject = new JSONObject(temp);
                    access_token = jObject.getString("access_token");
                    System.out.println(result);

                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
                Message message = mHandler.obtainMessage(LOAD_SUCCESS2, access_token);
                mHandler.sendMessage(message);
            }
        });
        thread.start();
    }
}

