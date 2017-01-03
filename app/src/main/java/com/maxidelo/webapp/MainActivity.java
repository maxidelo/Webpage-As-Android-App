package com.maxidelo.webapp;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity implements NetworkStateReceiver.NetworkStateReceiverListener {

    private final Log log = new Log(MainActivity.class.getName());

    // ----------------------------------------------------------------------
    // Permissions to save file
    // ----------------------------------------------------------------------

    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    // ----------------------------------------------------------------------
    // Constants
    // ----------------------------------------------------------------------

    private static final String URL = "http://homeboard.me";

    private static final String EXTERNAL_APP_URL_PATTERN = "http://download.pattern.com";

    // ----------------------------------------------------------------------
    // Fields
    // ----------------------------------------------------------------------

    private WebView webView;

    private WebView mWebviewPop;

    private ViewGroup videoLayout;

    private VideoEnabledWebChromeClient webChromeClient;

    private CoordinatorLayout mContainer;

    private Context mContext;

    private NetworkStateReceiver networkStateReceiver;

    private ProgressBar progressBar;

    private TextView txtview;

    // ----------------------------------------------------------------------
    // Activity methods
    // ----------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        networkStateReceiver = new NetworkStateReceiver();
        networkStateReceiver.addListener(this);
        this.registerReceiver(networkStateReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        createView();
    }

    private void createView() {
        setContentView(R.layout.activity_main);

        initWebView();
        initWebViewSettings();

        if (Build.VERSION.SDK_INT >= 21) {
            // AppRTC requires third party cookies to work
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }

        webView.loadUrl(URL);

        mContainer = (CoordinatorLayout) findViewById(R.id.webview_frame);
        mContext = this.getApplicationContext();
        progressBar = (ProgressBar) findViewById(R.id.progressbarLoading);
        txtview = (TextView) findViewById(R.id.textviewLoading);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (webView.canGoBack()) {
                        webView.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(networkStateReceiver);
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration conf) {
        // Avoids reloading when cellphone change between landscape and portrait
        super.onConfigurationChanged(conf);
    }

    @Override
    public void onBackPressed() {
        // Notify the VideoEnabledWebChromeClient, and handle it ourselves if it doesn't handle it
        if (!webChromeClient.onBackPressed()) {
            if (webView.canGoBack()) {
                webView.goBack();
            } else {
                // Standard back button implementation (for example this could close the app)
                super.onBackPressed();
            }
        }
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * Intialices and configures the webview
     */
    private void initWebView() {
        videoLayout = (ViewGroup) findViewById(R.id.videoLayout);
        if (android.os.Build.VERSION.SDK_INT >= 16) {
            videoLayout.setBackgroundColor(Color.BLACK);
        }

        webView = (WebView) findViewById(R.id.webview);
        webView.setWebViewClient(new UriWebViewClient());

        webChromeClient = new VideoEnabledWebChromeClient(webView, videoLayout) {
            @Override
            public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, Message resultMsg) {
                mWebviewPop = new WebView(mContext);
                mWebviewPop.setVerticalScrollBarEnabled(false);
                mWebviewPop.setHorizontalScrollBarEnabled(false);
                mWebviewPop.setWebViewClient(new UriWebViewClient());
                mWebviewPop.getSettings().setJavaScriptEnabled(true);
                mWebviewPop.getSettings().setSavePassword(false);
                mWebviewPop.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                mContainer.addView(mWebviewPop);
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(mWebviewPop);
                resultMsg.sendToTarget();
                return true;
            }

            @Override
            public void onProgressChanged(WebView view, int progress) {
                if (progressBar == null) {
                    return;
                }

                if (progress < 100 && progressBar.getVisibility() == ProgressBar.GONE) {
                    progressBar.setVisibility(ProgressBar.VISIBLE);
                    txtview.setVisibility(View.VISIBLE);
                }

                progressBar.setProgress(progress);
                if (progress == 100) {
                    progressBar.setVisibility(ProgressBar.GONE);
                    txtview.setVisibility(View.GONE);
                }
            }
        };

        webChromeClient.setOnToggledFullscreen(new VideoEnabledWebChromeClient.ToggledFullscreenCallback()
        {
            @Override
            public void toggledFullscreen(boolean fullscreen)
            {
                // Your code to handle the full-screen change, for example showing and hiding the title bar. Example:
                if (fullscreen)
                {
                    WindowManager.LayoutParams attrs = getWindow().getAttributes();
                    attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
                    attrs.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                    getWindow().setAttributes(attrs);
                    if (android.os.Build.VERSION.SDK_INT >= 14)
                    {
                        //noinspection all
                        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
                    }
                }
                else
                {
                    WindowManager.LayoutParams attrs = getWindow().getAttributes();
                    attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
                    attrs.flags &= ~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                    getWindow().setAttributes(attrs);
                    if (android.os.Build.VERSION.SDK_INT >= 14)
                    {
                        //noinspection all
                        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                    }
                }

            }
        });

        webView.setWebChromeClient(webChromeClient);
    }

    /**
     * Configures the web view settings
     */
    private void initWebViewSettings() {
        webView.setInitialScale(0);
        webView.setVerticalScrollBarEnabled(false);

        // Enable JavaScript
        final WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);

        // Enable multi window support
        settings.setSupportMultipleWindows(true);

        // Set the nav dump for HTC 2.x devices (disabling for ICS, deprecated entirely for Jellybean 4.2)
        try {
            Method gingerbread_getMethod = WebSettings.class.getMethod("setNavDump", new Class[]{boolean.class});

            String manufacturer = Build.MANUFACTURER;
            log.d("WebView is running on device made by: " + manufacturer);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB &&
                    Build.MANUFACTURER.contains("HTC")) {
                gingerbread_getMethod.invoke(settings, true);
            }
        } catch (NoSuchMethodException e) {
            log.d("We are on a modern version of Android, we will deprecate HTC 2.3 devices in 2.8");
        } catch (IllegalArgumentException e) {
            log.d("Doing the NavDump failed with bad arguments");
        } catch (IllegalAccessException e) {
            log.d("This should never happen: IllegalAccessException means this isn't Android anymore");
        } catch (InvocationTargetException e) {
            log.d("This should never happen: InvocationTargetException means this isn't Android anymore.");
        }

        //We don't save any form data in the application
        settings.setSaveFormData(false);
        settings.setSavePassword(false);

        // Jellybean rightfully tried to lock this down. Too bad they didn't give us a whitelist
        // while we do this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            settings.setAllowUniversalAccessFromFileURLs(true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            settings.setMediaPlaybackRequiresUserGesture(false);
        }

        // Enable database
        String databasePath = webView.getContext().getApplicationContext().getDir("database", Context.MODE_PRIVATE).getPath();
        settings.setDatabaseEnabled(true);
        settings.setDatabasePath(databasePath);
        settings.setGeolocationDatabasePath(databasePath);

        // Enable DOM storage
        settings.setDomStorageEnabled(true);

        // Enable built-in geolocation
        settings.setGeolocationEnabled(true);

        // Enable AppCache
        settings.setAppCachePath(databasePath);
        settings.setAppCacheEnabled(true);
    }


    /**
     * Checks if the app has permission to write to device storage
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity the current activity
     */
    private void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }

    }

    /**
     * Inner class WebView
     */
    private class UriWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            log.d("Should override url loading: " + url);

            String host = Uri.parse(url).getHost();
            log.d("Host: " + host);

            if (host.equals("m.facebook.com")) {
                return false;
            } else {
                // This is my web site, so do not override; let my WebView load
                // the page
                if (mWebviewPop != null) {
                    mWebviewPop.setVisibility(View.GONE);
                    mContainer.removeView(mWebviewPop);
                    mWebviewPop = null;
                }

                if (UrlAnalizer.isApkDownload(url)) {
                    verifyStoragePermissions(MainActivity.this);
                    ApkDownloaderInstallerAsync apkDownloaderInstallerAsync = new ApkDownloaderInstallerAsync(MainActivity.this);
                    apkDownloaderInstallerAsync.setUseDialog(true);
                    apkDownloaderInstallerAsync.execute(url);
                    return true;
                } else if (url.startsWith(EXTERNAL_APP_URL_PATTERN) || !UrlAnalizer.isBrowserUrl(url) || UrlAnalizer.isDownloadeableContent(url)) {
                    try {
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        MainActivity.this.startActivity(i);
                        startActivity(i);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(MainActivity.this, R.string.application_not_installed, Toast.LENGTH_LONG)
                                .show();
                    }
                    return true;
                } else {
                    webView.loadUrl(url);
                    return true;
                }
            }
        }

        @TargetApi(android.os.Build.VERSION_CODES.M)
        @Override
        public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError rerr) {
            // Redirect to deprecated method, so you can use it in all SDK versions
            onReceivedError(view, rerr.getErrorCode(), rerr.getDescription().toString(), req.getUrl().toString());
        }

        @Override
        // Old versions call this function
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            if (errorCode == ERROR_HOST_LOOKUP) {
                networkUnavailable();
            } else if (errorCode == ERROR_CONNECT) {
                log.d("Error Connect, retrying URL: " + failingUrl);
                view.loadUrl(failingUrl);
            } else {
                log.d("Error Code: " + errorCode);
                super.onReceivedError(view, errorCode, description, failingUrl);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (url != null) {
                String cookies = CookieManager.getInstance().getCookie(url);
                log.d("All the cookies for " + Uri.parse(url).getHost() + " :" + cookies);
            }
        }
    }

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    @Override
    public void networkAvailable() {
        super.onStop();
        createView();
    }

    @Override
    public void networkUnavailable() {
        setContentView(R.layout.activity_no_internet);
    }
}
