package knf.kuma.download;

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
    public static boolean NOMEDIA_CREATING = false;
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

    public File getFile(String file_name) {
        try {
            if (PreferenceManager.getDefaultSharedPreferences(context).getString("download_type", "0").equals("0")) {
                return new File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name);
            } else {
                return new File(FileUtil.getFullPathFromTreeUri(getTreeUri(), context), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new File(Environment.getDataDirectory(), "test.txt");
        }
    }

    public File getTmpFile(String file_name) {
        return new File(FileUtil.getFullPathFromTreeUri(getTreeUri(), context), "Android/data/knf.kuma/files/downloads/" + PatternUtil.getNameFromFile(file_name));
    }

    public File getFileCreate(String file_name) {
        try {
            if (PreferenceManager.getDefaultSharedPreferences(context).getString("download_type", "0").equals("0")) {
                File file = new File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name));
                if (!file.exists())
                    file.mkdirs();
                file = new File(file, file_name);
                if (!file.exists())
                    file.createNewFile();
                return file;
            } else {
                File file = new File(FileUtil.getFullPathFromTreeUri(getTreeUri(), context), "Android/data/knf.kuma/files/downloads/" + PatternUtil.getNameFromFile(file_name));
                if (!file.exists())
                    file.mkdirs();
                file = new File(file, file_name);
                if (!file.exists())
                    file.createNewFile();
                return file;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    boolean isTempFile(String file) {
        try {
            String path = FileUtil.getFullPathFromTreeUri(getTreeUri(), context);
            if (path == null) return false;
            return file.contains(path);
        } catch (Exception e) {
            return false;
        }
    }

    public void checkNoMedia(boolean noMediaNeeded) {
        NOMEDIA_CREATING = true;
        AsyncTask.execute(() -> {
            try {
                File file = new File(Environment.getExternalStorageDirectory(), "UKIKU/downloads");
                if (!file.exists())
                    file.mkdirs();
                File root = new File(file, ".nomedia");
                if (noMediaNeeded && !root.exists())
                    root.createNewFile();
                else if (!noMediaNeeded && root.exists())
                    root.delete();
                File[] list = file.listFiles(File::isDirectory);
                if (list != null && list.length > 0)
                    for (File current: list) {
                        File inside = new File(current, ".nomedia");
                        if (noMediaNeeded && !inside.exists())
                            inside.createNewFile();
                        else if (!noMediaNeeded && inside.exists())
                            inside.delete();
                    }
                if (getTreeUri() != null) {
                    DocumentFile documentRoot = find(DocumentFile.fromTreeUri(context, getTreeUri()), "UKIKU/downloads");
                    DocumentFile nomediaRoot = documentRoot.findFile(".nomedia");
                    if (noMediaNeeded && (nomediaRoot == null || !nomediaRoot.exists()))
                        documentRoot.createFile("application/nomedia", ".nomedia");
                    else if (!noMediaNeeded && (nomediaRoot != null && nomediaRoot.exists()))
                        nomediaRoot.delete();
                    DocumentFile[] documentList = documentRoot.listFiles();
                    if (documentList.length > 0)
                        for (DocumentFile dFile: documentList) {
                            if (dFile.isDirectory()) {
                                DocumentFile inside = dFile.findFile(".nomedia");
                                if (noMediaNeeded && (inside == null || !inside.exists()))
                                    dFile.createFile("application/nomedia", ".nomedia");
                                else if (!noMediaNeeded && (inside != null && inside.exists()))
                                    inside.delete();
                            }
                        }
                }
                Toaster.toast("Archivos nomedia " + (noMediaNeeded ? "creados" : "eliminados"));
                NOMEDIA_CREATING = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public File getDownloadsDirectory(String file_name) {
        try {
            if (PreferenceManager.getDefaultSharedPreferences(context).getString("download_type", "0").equals("0")) {
                return new File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + file_name);
            } else {
                return new File(FileUtil.getFullPathFromTreeUri(getTreeUri(), context), "UKIKU/downloads/" + file_name);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Environment.getDataDirectory();
        }
    }

    public File getDownloadsDirectory() {
        try {
            if (PreferenceManager.getDefaultSharedPreferences(context).getString("download_type", "0").equals("0")) {
                return new File(Environment.getExternalStorageDirectory(), "UKIKU/downloads");
            } else {
                return new File(FileUtil.getFullPathFromTreeUri(getTreeUri(), context), "UKIKU/downloads");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Environment.getDataDirectory();
        }
    }

    public void delete(String file_name) {
        delete(file_name, null);
    }

    public void delete(final String file_name, DeleteListener listener) {
        try {
            AsyncTask.execute(() -> {
                if (PreferenceManager.getDefaultSharedPreferences(context).getString("download_type", "0").equals("0")) {
                    File file = new File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name);
                    file.delete();
                    File dir = file.getParentFile();
                    if (dir.listFiles() == null || dir.listFiles().length == 0)
                        dir.delete();
                    if (listener != null)
                        listener.onDelete();
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
                        if (listener != null)
                            listener.onDelete();
                    } catch (Exception e) {
                        //e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    public OutputStream getOutputStream(String file_name) {
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
                return context.getContentResolver().openOutputStream(find(DocumentFile.fromTreeUri(context, getTreeUri()), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name).getUri(), "rw");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public FileOutputStream getFileOutputStream(String file_name) {
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
                return new FileOutputStream(context.getContentResolver().openFileDescriptor(find(DocumentFile.fromTreeUri(context, getTreeUri()), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name).getUri(), "rw").getFileDescriptor());
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

    public InputStream getTmpInputStream(String file_name) {
        try {
            return new FileInputStream(new File(FileUtil.getFullPathFromTreeUri(getTreeUri(), context), "Android/data/knf.kuma/files/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name));
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
                return new File(FileUtil.getFullPathFromTreeUri(getTreeUri(), context), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name).exists();
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
                return FileProvider.getUriForFile(context, "knf.kuma.fileprovider", new File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name));
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
        for (String name: path.split("/")) {
            DocumentFile file = root.findFile(name);
            if (file == null || !file.exists()) {
                if (create)
                    if (name.endsWith(".mp4")) {
                        root.createFile("video/mp4", name);
                    } else if (name.endsWith(".nomedia")) {
                        root.createFile("application/nomedia", name);
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

    public interface DeleteListener {
        void onDelete();
    }
}
