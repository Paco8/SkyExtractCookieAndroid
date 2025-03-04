package paco8.skyextractcookie;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends Activity {

    private WebView mWebView;
    private String host, platform, output_file, login_url, wait_for;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        platform = getIntent().getStringExtra("platform");
        output_file = getIntent().getStringExtra("output");

        if (platform != null) {
            loadPage();
        } else {
            CharSequence[] items = {"PeacockTV", "SkyShowtime", "NowTV", "WowTV"};
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select your streaming service:")
                    .setItems(items, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            switch (item) {
                                case 0:
                                    platform = "peacocktv";
                                    host = "https://www.peacocktv.com";
                                    login_url = host + "/signin";
                                    wait_for = "localisation";
                                    break;
                                case 1:
                                    platform = "skyshowtime";
                                    host = "https://www.skyshowtime.com";
                                    login_url = host + "/signin";
                                    wait_for = "localisation";
                                    break;
                                case 2:
                                    platform = "nowtv";
                                    host = "https://www.nowtv.com";
                                    login_url = host + "/gb/sign-in";
                                    wait_for = "home";
                                    break;
                                case 3:
                                    platform = "wowtv";
                                    host = "https://www.wowtv.de";
                                    login_url = host + "/login";
                                    wait_for = "home";
                                    break;
                            }
                            loadPage();
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void loadPage() {
        output_file = output_file == null ? platform + "_login.key" : output_file;
        System.out.println("platform: " + platform);
        System.out.println("output_file: " + output_file);

        mWebView = (WebView) findViewById(R.id.webview);
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36");
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().setAcceptCookie(true);
        mWebView.setWebViewClient(new MyWebViewClient());

        mWebView.loadUrl(login_url);
    }

    private class MyWebViewClient extends WebViewClient {
        private boolean cookie_found = false;

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            if (cookie_found) return new WebResourceResponse("text/plain", "UTF-8", null);

            String url = request.getUrl().toString();
            Log.d("url", url);
            if (url.contains(wait_for)) {
                //Map<String, String> headers = request.getRequestHeaders();
                //Log.d("Headers sent", url + " " + headers.toString());

                CookieManager cookieManager = CookieManager.getInstance();
                String cookies = cookieManager.getCookie(url);
                if (cookies != null) {
                    cookie_found = true;

                    Date now = Calendar.getInstance().getTime();
                    Timestamp timestamp = new Timestamp(now.getTime());

                    String json = String.format(
                            "{\n" +
                            "  \"url\": \"%s\",\n" +
                            "  \"app_name\": \"SkyExtractCookie\",\n" +
                            "  \"version\": \"%s\",\n" +
                            "  \"app_system\": \"android\",\n" +
                            "  \"host\": \"%s\",\n" +
                            "  \"data\": \"%s\",\n" +
                            "  \"timestamp\": \"%d\"\n" +
                            "}\n", url, BuildConfig.VERSION_NAME, host, cookies, timestamp.getTime());

                    File downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    File file = new File(downloadsDirectory, output_file);
                    if (file.exists()) {
                        file.delete();
                    }

                    try {
                        FileOutputStream fos = new FileOutputStream(file, false);
                        fos.write(json.getBytes());
                        fos.close();
                        showResult("Access token saved as "+ file.getAbsolutePath());
                    } catch (IOException e) {
                        e.printStackTrace();
                        showResult("Error: " + e.getMessage());
                    }
                }
            }
            return super.shouldInterceptRequest(view, request);
        }
    }

    private void showResult(String text) {
        runOnUiThread(new Runnable() {
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage(text)
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                finish();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });
    }
}
