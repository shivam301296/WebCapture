package com.tree.rh.webcapture;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.URLUtil;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.PermissionListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    EditText urlEt;
    WebView webView;
    ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            WebView.enableSlowWholeDocumentDraw();
        }
        setContentView(R.layout.activity_main);

        urlEt= findViewById(R.id.urlEt);
        webView = findViewById(R.id.myWeb);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        //webSettings.setBuiltInZoomControls(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        pd= new ProgressDialog(this);
        pd.setMessage("Saving Web page as PNG Image");
        pd.setCancelable(false);

        webView.loadUrl("https://www.google.com/");

        askPermi();

        if(!isNetworkConnected(this)){
            display("Network not available");
        }

    }//onCreateEND

    public void askPermi() {
        AndPermission.with(this)
                .requestCode(100)
                .permission(Manifest.permission.WRITE_EXTERNAL_STORAGE
                ,Manifest.permission.ACCESS_NETWORK_STATE)
                .start();
    }

    public Bitmap screenshot(WebView webView) {
        webView.measure(View.MeasureSpec.makeMeasureSpec(
                View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        webView.layout(0, 0, webView.getMeasuredWidth(), webView.getMeasuredHeight());
        webView.setDrawingCacheEnabled(true);
        webView.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(webView.getMeasuredWidth(),
                webView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        int iHeight = bitmap.getHeight();
        canvas.drawBitmap(bitmap, 0, iHeight, paint);
        webView.draw(canvas);
        //display("Bitmap Created");
        return bitmap;
    }


    public void saveBitmap(Bitmap bitmap, String name) {
        if(bitmap==null){
            pd.dismiss();
            return;
        }
        String path = Environment.getExternalStorageDirectory().toString() + File.separator + "WebCaptures";
        FileOutputStream out = null;
        File file = null;
        Date dt = new Date();
        try {
            file = new File(path);
            file.mkdirs();
            file = new File(path, name + dt.getTime() + ".jpg");
            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
            display("Saved Successfully");
            refreshGallery(file);
        } catch (Exception e) {
            e.printStackTrace();
            //display("Error\n" + e);
            pd.dismiss();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void refreshGallery(File f){
        MediaScannerConnection.scanFile(this,
                new String[] { f.toString() }, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        display("Gallery Updated");
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
        openImg(f);
        pd.dismiss();
    }

    public void openImg(final File f){
        Snackbar snackbar = Snackbar
                .make(findViewById(R.id.rootLl), "Image Saved Successfully", Snackbar.LENGTH_LONG)
                .setAction("View", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.fromFile(f), "image/*");
                        startActivity(intent);
                    }
                });
        snackbar.show();
    }



    public void capture(View view) {
        new CaptureWeb().execute();
    }

    class CaptureWeb extends AsyncTask<Void,Void,Void>{
        String title="";
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            saveBitmap(screenshot(webView), webView.getTitle());
            //pd.dismiss();
        }
    }

    public void go(View view) {
        String rawUrl= urlEt.getText().toString();
        String url="http://www.google.com/search?q=";
        if(rawUrl.contains(" ")){
            rawUrl= rawUrl.replace(" ","+");
            url=url+rawUrl;
        }else {
            url= URLUtil.guessUrl(rawUrl);
        }
        display(url);
        webView.loadUrl(url);
    }


    @Override
    public void onBackPressed() {
        if(webView.canGoBack()){
            webView.goBack();
        }else {
            super.onBackPressed();
        }
    }

    static public boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }


    public void display(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

}//classEND
