package com.maxidelo.webapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Async class that handles the download of the APK and additional files needed for the games.
 */
public class ApkDownloaderInstallerAsync<T> extends AsyncTask<T, Integer, String> {

    private final Log log = new com.maxidelo.webapp.Log(ApkDownloaderInstallerAsync.class.getName());

    // ----------------------------------------------------------------------
    // Constants
    // ----------------------------------------------------------------------

    private final String DOWNLOAD_DIRECTORY_PATH = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();

    private static final String OBB_DIRECTORY_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/obb/";

    private ProgressDialog dialog;

    private final Activity activity;

    // ----------------------------------------------------------------------
    // Fields
    // ----------------------------------------------------------------------

    private boolean useDialog;

    // ----------------------------------------------------------------------
    // Configuration methods
    // ----------------------------------------------------------------------

    public void setUseDialog(boolean useDialog) {
        this.useDialog = useDialog;
    }

    // ----------------------------------------------------------------------
    // Constructor
    // ----------------------------------------------------------------------

    public ApkDownloaderInstallerAsync(Activity activityParam) {
        activity = activityParam;
        if (useDialog) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dialog = new ProgressDialog(activity);
                }
            });
        }
    }

    // ----------------------------------------------------------------------
    // Overriden AsyncTask methods
    // ----------------------------------------------------------------------

    /**
     * Method that creates the progress dialog for the download.
     */
    @Override
    protected void onPreExecute() {
        if (useDialog) {
            dialog = new ProgressDialog(activity);
            dialog.setProgress(0);
            dialog.setMessage("Downloading application ...");
            dialog.setIndeterminate(false);
            dialog.setMax(100);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setProgressNumberFormat("%1d KB/%2d KB");
        }
        super.onPreExecute();

    }

    protected List<InstallationFile> getInstallationFiles(T... items) {
        List<InstallationFile> installationFiles = new ArrayList<>();

        boolean hasApkFile = false;

        for (T item : items) {
            if (item instanceof String) {
                String url = (String) item;
                InstallationFile installationFile = new InstallationFile(url);
                if (InstallationFileType.APK.equals(installationFile.getType())) {
                    hasApkFile = true;
                }
                installationFiles.add(installationFile);
            } else {
                // TODO not supported type
            }
        }

        if (!hasApkFile) {
            // TODO no puede insalar sin un APK
        }

        return installationFiles;
    }

    /**
     * This method is the execute of the AsyncTask, it will download the files
     * in the specified directories. It will treat the APK and OBB/PATCH in different ways.
     *
     * @param items - String with all the parameters provided by the activity.
     * @return String
     */
    protected String doInBackground(T... items) {
        List<InstallationFile> installationFiles = getInstallationFiles(items);

        String apkNameToDelete = null;

        for (InstallationFile installationFile : installationFiles) {
            try {
                if (downloadFile(installationFile) && InstallationFileType.APK.equals(installationFile.getType())) {
                    apkNameToDelete = installationFile.getName();
                    log.d(apkNameToDelete + " <--APK NAME");
                    installDeleteAPK(installationFile.getName());
                    if (useDialog) {
                        dialog.dismiss();
                    }
                }
            } catch (Exception e) {
                log.e("Download error! " + e.getMessage());
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
                // Setting Dialog Title
                alertDialog.setTitle("Wifi Connection lost!");
                // Setting Dialog Message
                alertDialog.setMessage("Please try again later...");
                // Setting Icon to Dialog
                // alertDialog.setIcon(R.drawable.ic_launcher);        // Setting Positive "Yes" Button
                alertDialog.setPositiveButton("Exit",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                android.os.Process.killProcess(android.os.Process.myPid());
                                System.exit(1);
                            }
                        });
            }
        }
        return apkNameToDelete;
    }

    private boolean downloadFile(InstallationFile installationFile) throws IOException {
        String downloadFolder;
        String downloadUrl;
        String fileName;

        //if its an APK it goes to another folder and has a different logic hence the IF
        if (InstallationFileType.APK.equals(installationFile.getType())) {
            downloadFolder = DOWNLOAD_DIRECTORY_PATH;
            downloadUrl = installationFile.getUrl();
            fileName = installationFile.getName();
        } else {
            downloadFolder = OBB_DIRECTORY_PATH + "/" + installationFile.getName();
            downloadUrl = installationFile.getUrl();
            fileName = installationFile.getName();
        }

        URL url = new URL(downloadUrl);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("GET");
        c.connect();

        if (useDialog) {
            int lenghtOfFile = c.getContentLength();
            int totalInMegas = lenghtOfFile / 1024;
            dialog.setMax(totalInMegas);
        }

        // String DOWNLOAD_DIRECTORY_PATH = "/mnt/sdcard/Download/";
        File file = new File(downloadFolder);
        file.mkdirs();
        File outputFile = new File(file, fileName);
        if (outputFile.exists()) {
            outputFile.delete();
        }
        FileOutputStream fos = new FileOutputStream(outputFile);

        if (c.getResponseCode() != HttpURLConnection.HTTP_OK) {
            log.d("http response code is " + c.getResponseCode());
            fos.close();
            return false;
        }

        InputStream is = c.getInputStream();

        byte[] buffer = new byte[1024];
        int count = 0;
        int total = 0;
        while ((count = is.read(buffer)) != -1) {
            fos.write(buffer, 0, count);
            total += count;
            publishProgress(total);
        }
        fos.close();
        is.close();
        return true;
    }


    /**
     * While the download progresses the dialog progress
     * is updated in real time.
     *
     * @param values
     */
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        if (useDialog) {
            Integer total = values[0] / 1024;
            dialog.setIndeterminate(false);
            dialog.setProgress(total);
            dialog.show();
        }
    }

    static final int START_APK_DELETION = 1;  // The request code

    /**
     * Method that installs the APK, and deletes it afterwards.
     */
    protected void installDeleteAPK(String apkToInstall) {
        File fileToInstall = new File(DOWNLOAD_DIRECTORY_PATH, apkToInstall);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(fileToInstall),
                "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivityForResult(intent, START_APK_DELETION);
    }

    public class InstallationFile {

        // ----------------------------------------------------------------------
        // Constants
        // ----------------------------------------------------------------------

        private static final String APK = "APK";

        private static final String OBB = "OBB";

        private static final String PATCH = "PATCH";

        // ----------------------------------------------------------------------
        // Fields
        // ----------------------------------------------------------------------

        private String name;

        private String url;

        private InstallationFileType type;

        // ----------------------------------------------------------------------
        // Constructor
        // ----------------------------------------------------------------------

        public InstallationFile(String url) {
            if (StringUtils.endsWith(url, InstallationFileType.APK.getExtension())) {
                this.type = InstallationFileType.APK;
            } else {
                this.type = InstallationFileType.OBB;
            }

            this.url = url;
            this.name = StringUtils.substringAfterLast(url, "/");
        }

        // ----------------------------------------------------------------------
        // Getter and Setters
        // ----------------------------------------------------------------------

        public String getName() {
            return name;
        }

        public String getUrl() {
            return url;
        }

        public InstallationFileType getType() {
            return type;
        }
    }

}

