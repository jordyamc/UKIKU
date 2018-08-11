package knf.kuma.commons;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;
import android.util.Pair;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Locale;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import knf.kuma.download.FileAccessHelper;

public final class FileUtil {

    private static final String PRIMARY_VOLUME_NAME = "primary";
    static String TAG = "TAG";

    @Nullable
    public static String getFullPathFromTreeUri(@Nullable final Uri treeUri, Context con) {
        if (treeUri == null) {
            return null;
        }
        String volumePath = FileUtil.getVolumePath(FileUtil.getVolumeIdFromTreeUri(treeUri), con);
        if (volumePath == null) {
            return File.separator;
        }
        if (volumePath.endsWith(File.separator)) {
            volumePath = volumePath.substring(0, volumePath.length() - 1);
        }

        String documentPath = FileUtil.getDocumentPathFromTreeUri(treeUri);
        if (documentPath.endsWith(File.separator)) {
            documentPath = documentPath.substring(0, documentPath.length() - 1);
        }

        if (documentPath.length() > 0) {
            if (documentPath.startsWith(File.separator)) {
                return volumePath + documentPath;
            } else {
                return volumePath + File.separator + documentPath;
            }
        } else {
            return volumePath;
        }
    }


    private static String getVolumePath(final String volumeId, Context con) {
        try {
            StorageManager mStorageManager =
                    (StorageManager) con.getSystemService(Context.STORAGE_SERVICE);

            Class<?> storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");

            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getUuid = storageVolumeClazz.getMethod("getUuid");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isPrimary = storageVolumeClazz.getMethod("isPrimary");
            Object result = getVolumeList.invoke(mStorageManager);

            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String uuid = (String) getUuid.invoke(storageVolumeElement);
                Boolean primary = (Boolean) isPrimary.invoke(storageVolumeElement);

                // primary volume?
                if (primary && PRIMARY_VOLUME_NAME.equals(volumeId)) {
                    return (String) getPath.invoke(storageVolumeElement);
                }

                // other volumes?
                if (uuid != null) {
                    if (uuid.equals(volumeId)) {
                        return (String) getPath.invoke(storageVolumeElement);
                    }
                }
            }

            // not found.
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static String getVolumeIdFromTreeUri(final Uri treeUri) {
        final String docId = DocumentsContract.getTreeDocumentId(treeUri);
        final String[] split = docId.split(":");

        if (split.length > 0) {
            return split[0];
        } else {
            return null;
        }
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static String getDocumentPathFromTreeUri(final Uri treeUri) {
        final String docId = DocumentsContract.getTreeDocumentId(treeUri);
        final String[] split = docId.split(":");
        if ((split.length >= 2) && (split[1] != null)) {
            return split[1];
        } else {
            return File.separator;
        }
    }

    public static HashSet<String> getExternalMounts() {

        final HashSet<String> out = new HashSet<>();
        String reg = "(?i).*vold.*(vfat|ntfs|exfat|fat32|ext3|ext4).*rw.*";
        StringBuilder s = new StringBuilder();
        try {
            final Process process = new ProcessBuilder().command("mount").redirectErrorStream(true).start();
            process.waitFor();
            final InputStream is = process.getInputStream();
            final byte[] buffer = new byte[1024];
            while (is.read(buffer) != -1) {
                s.append(new String(buffer));
            }
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        final String[] lines = s.toString().split("\n");
        for (String line : lines) {
            if (!line.toLowerCase(Locale.US).contains("asec")) {
                if (line.matches(reg)) {
                    String[] parts = line.split(" ");
                    for (String part : parts) {
                        if (part.startsWith("/")) {
                            if (!part.toLowerCase(Locale.US).contains("vold")) {
                                out.add(part);
                            }
                        }
                    }
                }
            }
        }
        return out;
    }

    public static LiveData<Pair<Integer, Boolean>> moveFile(ContentResolver resolver, Uri uri, OutputStream outputStream) {
        final MutableLiveData<Pair<Integer, Boolean>> liveData = new MutableLiveData<>();
        AsyncTask.execute(() -> {
            try {
                InputStream inputStream = resolver.openInputStream(uri);
                long total = inputStream.available();
                byte[] buffer = new byte[128 * 1024];
                int read;
                long current = 0;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                    current += read;
                    int prog = (int) ((current * 100) / total);
                    new Handler(Looper.getMainLooper()).post(() -> liveData.setValue(new Pair<>(prog, false)));
                }
                inputStream.close();
                outputStream.flush();
                outputStream.close();
                try {
                    DocumentsContract.deleteDocument(resolver, uri);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                new Handler(Looper.getMainLooper()).post(() -> liveData.setValue(new Pair<>(100, true)));
            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> liveData.setValue(new Pair<>(-1, true)));
            }
        });
        return liveData;
    }

    public static void moveFile(String file_name, MoveCallback callback) {
        AsyncTask.execute(() -> {
            try {
                InputStream inputStream = FileAccessHelper.INSTANCE.getTmpInputStream(file_name);
                OutputStream outputStream = FileAccessHelper.INSTANCE.getOutputStream(file_name);
                long total = inputStream.available();
                byte[] buffer = new byte[128 * 1024];
                int read;
                long current = 0;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                    current += read;
                    int prog = (int) ((current * 100) / total);
                    callback.onProgress(new Pair<>(prog, false));
                }
                inputStream.close();
                outputStream.flush();
                outputStream.close();
                try {
                    File file = FileAccessHelper.INSTANCE.getTmpFile(file_name);
                    file.delete();
                    if (file.getParentFile().list().length == 0)
                        file.getParentFile().delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                callback.onProgress(new Pair<>(100, true));
            } catch (Exception e) {
                e.printStackTrace();
                callback.onProgress(new Pair<>(-1, true));
            }
        });
    }

    public interface MoveCallback {
        void onProgress(Pair<Integer, Boolean> pair);
    }
}
