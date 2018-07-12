package knf.kuma.downloadservice;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v4.provider.DocumentFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import knf.kuma.commons.FileUtil;
import knf.kuma.commons.PatternUtil;
import xdroid.toaster.Toaster;

public class FileAccessHelper {
    public static final int SD_REQUEST = 51247;
    public static FileAccessHelper INSTANCE;
    private Context context;

    private FileAccessHelper(Context context) {
        this.context = context;
    }

    public static void init(Context context) {
        FileAccessHelper.INSTANCE = new FileAccessHelper(context);
    }

    public static void openTreeChooser(Fragment fragment) {
        try {
            fragment.startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), SD_REQUEST);
        } catch (Exception e) {
            Toaster.toast("Error al buscar SD");
        }
    }

    public static void openTreeChooser(android.app.Fragment fragment) {
        try {
            fragment.startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), SD_REQUEST);
        } catch (Exception e) {
            Toaster.toast("Error al buscar SD");
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static boolean isSDCardRoot(Uri uri) {
        return isExternalStorageDocument(uri) && isRootUri(uri) && !isInternalStorage(uri);
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private static boolean isRootUri(Uri uri) {
        String docId = DocumentsContract.getTreeDocumentId(uri);
        return docId.endsWith(":");
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private static boolean isInternalStorage(Uri uri) {
        return isExternalStorageDocument(uri) && DocumentsContract.getTreeDocumentId(uri).contains("primary");
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public File getFile(String file_name){
        try {
            if (PreferenceManager.getDefaultSharedPreferences(context).getString("download_type", "0").equals("0")) {
                return new File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name);
            } else {
                return new File(FileUtil.getFullPathFromTreeUri(getTreeUri(),context),"UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new File(Environment.getDataDirectory(),"test.txt");
        }
    }

    public File getDownloadsDirectory(String file_name){
        try {
            if (PreferenceManager.getDefaultSharedPreferences(context).getString("download_type", "0").equals("0")) {
                return new File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + file_name);
            } else {
                return new File(FileUtil.getFullPathFromTreeUri(getTreeUri(),context),"UKIKU/downloads/" + file_name);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Environment.getDataDirectory();
        }
    }

    public File getDownloadsDirectory(){
        try {
            if (PreferenceManager.getDefaultSharedPreferences(context).getString("download_type", "0").equals("0")) {
                return new File(Environment.getExternalStorageDirectory(), "UKIKU/downloads");
            } else {
                return new File(FileUtil.getFullPathFromTreeUri(getTreeUri(),context),"UKIKU/downloads");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Environment.getDataDirectory();
        }
    }

    public void delete(final String file_name){
        try {
            AsyncTask.execute(() -> {
                if (PreferenceManager.getDefaultSharedPreferences(context).getString("download_type", "0").equals("0")) {
                    File file = new File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name);
                    file.delete();
                    File dir = file.getParentFile();
                    if (dir.listFiles() == null || dir.listFiles().length == 0)
                        dir.delete();
                } else {
                    try {
                        DocumentFile documentFile = DocumentFile.fromTreeUri(context, getTreeUri());
                        if (documentFile != null && documentFile.exists()) {
                            DocumentFile file = find(documentFile, "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name);
                            file.delete();
                            DocumentFile dir = file.getParentFile();
                            if (dir != null && dir.listFiles().length == 0)
                                dir.delete();
                        }
                    } catch (Exception e) {
                        //e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    public OutputStream getOutputStream(String file_name){
        try {
            if (PreferenceManager.getDefaultSharedPreferences(context).getString("download_type", "0").equals("0")) {
                File file = new File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name));
                if (!file.exists())
                    file.mkdirs();
                file = new File(file, file_name);
                if (!file.exists())
                    file.createNewFile();
                return new FileOutputStream(new File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name));
            } else {
                return context.getContentResolver().openOutputStream(find(DocumentFile.fromTreeUri(context, getTreeUri()), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name).getUri(),"rw");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public InputStream getInputStream(String file_name) {
        try {
            if (PreferenceManager.getDefaultSharedPreferences(context).getString("download_type", "0").equals("0")) {
                File file = new File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name));
                if (!file.exists())
                    file.mkdirs();
                file = new File(file, file_name);
                if (!file.exists())
                    file.createNewFile();
                return new FileInputStream(new File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name));
            } else {
                return context.getContentResolver().openInputStream(find(DocumentFile.fromTreeUri(context, getTreeUri()), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name).getUri());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean existFile(String file_name) {
        try {
            if (PreferenceManager.getDefaultSharedPreferences(context).getString("download_type", "0").equals("0")) {
                return new File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name).exists();
            } else {
                DocumentFile documentFile = DocumentFile.fromTreeUri(context, getTreeUri());
                if (documentFile != null && documentFile.exists()) {
                    find(documentFile, "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name);
                }
                return new File(FileUtil.getFullPathFromTreeUri(getTreeUri(),context),"UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name).exists();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean canDownload(Fragment fragment) {
        if (PreferenceManager.getDefaultSharedPreferences(context).getString("download_type", "0").equals("0")) {
            return true;
        } else {
            try {
                DocumentFile documentFile = DocumentFile.fromTreeUri(context, getTreeUri());
                if (documentFile != null && documentFile.exists()) {
                    return true;
                } else {
                    openTreeChooser(fragment);
                    return false;
                }
            } catch (IllegalArgumentException e) {
                openTreeChooser(fragment);
                return false;
            }
        }
    }

    public boolean canDownload(android.app.Fragment fragment) {
        if (PreferenceManager.getDefaultSharedPreferences(context).getString("download_type", "0").equals("0")) {
            return true;
        } else {
            try {
                DocumentFile documentFile = DocumentFile.fromTreeUri(context, getTreeUri());
                if (documentFile != null && documentFile.exists()) {
                    return true;
                } else {
                    openTreeChooser(fragment);
                    return false;
                }
            } catch (IllegalArgumentException e) {
                openTreeChooser(fragment);
                return false;
            }
        }
    }

    public boolean canDownload(android.app.Fragment fragment, String value) {
        if (value.equals("0")) {
            return true;
        } else {
            try {
                DocumentFile documentFile = DocumentFile.fromTreeUri(context, getTreeUri());
                if (documentFile != null && documentFile.exists()) {
                    return true;
                } else {
                    openTreeChooser(fragment);
                    return false;
                }
            } catch (IllegalArgumentException e) {
                openTreeChooser(fragment);
                return false;
            }
        }
    }

    public Uri getDataUri(String file_name) {
        try {
            if (PreferenceManager.getDefaultSharedPreferences(context).getString("download_type", "0").equals("0")) {
                return FileProvider.getUriForFile(context,"knf.kuma.fileprovider",new File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name));
            } else {
                DocumentFile documentFile = DocumentFile.fromTreeUri(context, getTreeUri());
                if (documentFile != null && documentFile.exists()) {
                    DocumentFile root = find(documentFile, "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name);
                    return root.getUri();
                }
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private DocumentFile find(DocumentFile root, String path) throws Exception {
        return find(root, path, true);
    }

    private DocumentFile find(DocumentFile root, String path, boolean create) throws Exception {
        for (String name : path.split("/")) {
            DocumentFile file = root.findFile(name);
            if (file == null || !file.exists()) {
                if (create)
                    if (name.endsWith(".mp4")) {
                        root.createFile("video/mp4", name);
                    } else {
                        root.createDirectory(name);
                    }
                root = root.findFile(name);
            } else {
                root = file;
            }
        }
        return root;
    }

    public boolean isUriValid(Uri uri) {
        if (isSDCardRoot(uri)) {
            context.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            PreferenceManager.getDefaultSharedPreferences(context).edit().putString("tree_uri", uri.toString()).apply();
            return true;
        } else {
            return false;
        }
    }

    @Nullable
    private Uri getTreeUri() {
        try {
            return Uri.parse(PreferenceManager.getDefaultSharedPreferences(context).getString("tree_uri", ""));
        } catch (Exception e) {
            return null;
        }
    }
}
