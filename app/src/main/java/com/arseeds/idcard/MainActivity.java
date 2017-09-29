package com.arseeds.idcard;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    OkHttpClient mOkHttpClient;
    private static final String TAG = "MainActivity";
    private Button re_net;
    private Button re_local;
    private String url = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "idcard.jpg";
    private EditText tv_result;


    //训练数据路径，必须包含tesseract文件夹
    static final String TESSBASE_PATH = Environment.getExternalStorageDirectory() + "/";
    //识别语言英文
    static final String DEFAULT_LANGUAGE = "eng";
    private ImageView iv_number;
    private Button re_real;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initClient();
        new AssestUtils(this).init();
    }


    private void initClient() {
        mOkHttpClient = new OkHttpClient.Builder()
                .connectTimeout(1000, TimeUnit.MINUTES)
                .readTimeout(1000, TimeUnit.MINUTES)
                .writeTimeout(1000, TimeUnit.MINUTES)
                .build();
    }

    private void uploadAndRecognize(String url) {
        if (!TextUtils.isEmpty(url)) {
            File file = new File(url);
            //构造上传请求，类似web表单
            RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addPart(Headers.of("Content-Disposition", "form-data; name=\"callbackurl\""), RequestBody.create(null, "/idcard/"))
                    .addPart(Headers.of("Content-Disposition", "form-data; name=\"action\""), RequestBody.create(null, "idcard"))
                    .addPart(Headers.of("Content-Disposition", "form-data; name=\"img\"; filename=\"idcardFront_user.jpg\""), RequestBody.create(MediaType.parse("image/jpeg"), file))
                    .build();
            //进行包装，使其支持进度回调
            final Request request = new Request.Builder()
                    .header("Host", "ocr.ccyunmai.com:8080")
                    .header("Origin", "http://ocr.ccyunmai.com:8080")
                    .header("Referer", "http://ocr.ccyunmai.com:8080/idcard/")
                    .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2398.0 Safari/537.36")
                    .url("http://ocr.ccyunmai.com:8080/UploadImage.action")
                    .post(requestBody)
                    .build();
            //开始请求
            mOkHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final String result = response.body().string();
                    Document parse = Jsoup.parse(result);
                    final Elements select = parse.select("div#ocrresult");
                    Log.e(TAG, "onResponse: " + select.text());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tv_result.setText(getHtmlMsg(select.text(), "公民身份号码:", "签发机关"));
                        }
                    });

                }
            });
        }
    }

    private void localre(String url) {
        Bitmap bmp = BitmapFactory.decodeFile(url);
        final TessBaseAPI baseApi = new TessBaseAPI();
        //初始化OCR的训练数据路径与语言

        int x, y, w, h;
        x = (int) (bmp.getWidth() * 0.340);
        y = (int) (bmp.getHeight() * 0.800);
        w = (int) (bmp.getWidth() * 0.6 + 0.5f);
        h = (int) (bmp.getHeight() * 0.12 + 0.5f);
        Bitmap bit_hm = Bitmap.createBitmap(bmp, x, y, w, h);
        iv_number.setImageBitmap(bit_hm);
        baseApi.init(TESSBASE_PATH, DEFAULT_LANGUAGE);
        //设置识别模式
        baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_LINE);
        //设置要识别的图片
        baseApi.setImage(bit_hm);
        baseApi.setVariable("tessedit_char_whitelist", "0123456789Xx");
        tv_result.setText(baseApi.getUTF8Text());
        baseApi.clear();
        baseApi.end();
    }


    private void initView() {
        re_net = (Button) findViewById(R.id.re_net);
        re_local = (Button) findViewById(R.id.re_local);

        re_net.setOnClickListener(this);
        re_local.setOnClickListener(this);
        tv_result = (EditText) findViewById(R.id.tv_result);
        iv_number = (ImageView) findViewById(R.id.iv_number);
        iv_number.setOnClickListener(this);
        re_real = (Button) findViewById(R.id.re_real);
        re_real.setOnClickListener(this);
    }

    public String getHtmlMsg(String s, String startContent, String endContent) {
        String[] id = s.split(startContent);
        String[] idTrue = id[1].split(endContent);
        return idTrue[0];
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.re_net:
                uploadAndRecognize(url);
                break;
            case R.id.re_local:
                localre(url);
                break;
            case R.id.re_real:
                startActivity(new Intent(getApplicationContext(),CameraActivity.class));
                break;
        }
    }
}
