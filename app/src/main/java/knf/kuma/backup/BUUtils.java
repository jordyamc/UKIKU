package knf.kuma.backup;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.android.Auth;
import com.dropbox.core.http.OkHttp3Requestor;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.SearchMatch;
import com.dropbox.core.v2.files.WriteMode;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import knf.kuma.backup.objects.BackupObject;
import knf.kuma.database.CacheDB;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.pojos.FavoriteObject;
import knf.kuma.pojos.RecordObject;
import knf.kuma.pojos.SeeingObject;
import xdroid.toaster.Toaster;

@SuppressLint("StaticFieldLeak")
public class BUUtils {
    public static final int LOGIN_CODE = 56478;
    private static Activity activity;
    private static LoginInterface loginInterface;
    private static DriveResourceClient DRC;
    private static DbxClientV2 DBC;

    public static void init(Activity activity, boolean startclient) {
        init(activity, (LoginInterface) activity, startclient);
    }

    public static void init(Activity activity, LoginInterface lInterface, boolean startclient) {
        BUUtils.activity = activity;
        loginInterface = lInterface;
        if (startclient)
            startClient(getType(), true);
    }

    public static boolean isLogedIn() {
        return DRC != null || DBC != null;
    }

    public static void setDriveClient() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(activity);
        if (account != null)
            DRC = Drive.getDriveResourceClient(activity, account);
        loginInterface.onLogin();
    }

    public static void setDropBoxClient(String token) {
        if (token != null) {
            PreferenceManager.getDefaultSharedPreferences(activity).edit().putString("db_token", token).apply();
            DbxRequestConfig requestConfig = DbxRequestConfig.newBuilder("dropbox_app")
                    .withHttpRequestor(new OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient()))
                    .build();
            DBC = new DbxClientV2(requestConfig, token);
        }
        loginInterface.onLogin();
    }

    private static String getDBToken() {
        return PreferenceManager.getDefaultSharedPreferences(activity).getString("db_token", null);
    }

    private static void clearDBToken() {
        PreferenceManager.getDefaultSharedPreferences(activity).edit().putString("db_token", null).apply();
    }

    private static void clearGoogleAccount() {
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(Drive.SCOPE_APPFOLDER)
                        .build();
        GoogleSignInClient client = GoogleSignIn.getClient(activity, signInOptions);
        client.signOut();
    }

    public static void startClient(BUType type, boolean fromInit) {
        switch (type) {
            case DRIVE:
                if (!fromInit) {
                    GoogleSignInOptions signInOptions =
                            new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestScopes(Drive.SCOPE_APPFOLDER)
                                    .build();
                    activity.startActivityForResult(GoogleSignIn.getClient(activity, signInOptions).getSignInIntent(), LOGIN_CODE);
                } else {
                    setDriveClient();
                }
                break;
            case DROPBOX:
                if (fromInit && getDBToken() != null) {
                    setDropBoxClient(getDBToken());
                } else {
                    Auth.startOAuth2Authentication(activity, "qtjow4hsk06vt19");
                }
                break;
        }
    }

    public static void logOut() {
        DRC = null;
        DBC = null;
        switch (getType(activity)) {
            case DROPBOX:
                clearDBToken();
                break;
            case DRIVE:
                clearGoogleAccount();
                break;
        }
        setType(BUType.LOCAL);
    }

    public static BUType getType() {
        return getType(activity);
    }

    public static void setType(BUType type) {
        PreferenceManager.getDefaultSharedPreferences(activity).edit().putInt("backup_type", type.value).apply();
    }

    public static BUType getType(Context context) {
        switch (PreferenceManager.getDefaultSharedPreferences(context).getInt("backup_type", -1)) {
            default:
            case -1:
                return BUType.LOCAL;
            case 0:
                return BUType.DRIVE;
            case 1:
                return BUType.DROPBOX;
        }
    }

    public static boolean isConnected(Context context) {
        return getType(context) != BUType.LOCAL;
    }

    public static void search(final String id, final SearchInterface searchInterface) {
        switch (getType()) {
            case DRIVE:
                searchDrive(id, searchInterface);
                break;
            case DROPBOX:
                searchDropbox(id, searchInterface);
                break;
        }
    }

    public static void backup(final String id, final BackupInterface backupInterface) {
        switch (getType()) {
            case DRIVE:
                backupDrive(id, backupInterface);
                break;
            case DROPBOX:
                backupDropbox(id, backupInterface);
                break;
        }
    }

    private static void searchDropbox(final String id, final SearchInterface searchInterface) {
        AsyncTask.execute(() -> {
            try {
                List<SearchMatch> list = DBC.files().search("", id).getMatches();
                if (list.size() > 0) {
                    DbxDownloader<FileMetadata> downloader = DBC.files().download("/" + id);
                    searchInterface.onResponse((BackupObject) new Gson().fromJson(new InputStreamReader(downloader.getInputStream()), getType(id)));
                    downloader.close();
                } else {
                    searchInterface.onResponse(null);
                }
            } catch (Exception e) {
                e.printStackTrace();
                searchInterface.onResponse(null);
            }
        });
    }

    private static void searchDrive(final String id, final SearchInterface searchInterface) {
        AsyncTask.execute(() -> {
            try {
                final Task<DriveFolder> appFolderTask = DRC.getAppFolder();
                appFolderTask.continueWithTask(task -> {
                    DriveFolder appfolder = appFolderTask.getResult();
                    Query query = new Query.Builder()
                            .addFilter(Filters.contains(SearchableField.TITLE, id))
                            .build();
                    return DRC.queryChildren(appfolder, query);
                }).continueWithTask(task -> {
                    MetadataBuffer metadata = task.getResult();
                    if (metadata.getCount() > 0) {
                        DriveFile driveFile = metadata.get(0).getDriveId().asDriveFile();
                        metadata.release();
                        return DRC.openFile(driveFile, DriveFile.MODE_READ_ONLY);
                    } else {
                        metadata.release();
                        return null;
                    }
                }).addOnSuccessListener(activity, driveContents -> {
                    try {
                        searchInterface.onResponse(new Gson().fromJson(new InputStreamReader(driveContents.getInputStream()), getType(id)));
                    } catch (Exception e) {
                        e.printStackTrace();
                        searchInterface.onResponse(null);
                    }
                }).addOnFailureListener(activity, e -> {
                    e.printStackTrace();
                    searchInterface.onResponse(null);
                });
            } catch (Exception e) {
                Crashlytics.logException(e);
                searchInterface.onResponse(null);
            }
        });
    }

    private static void backupDropbox(final String id, final BackupInterface backupInterface) {
        final MaterialDialog dialog = new MaterialDialog.Builder(activity)
                .content("Respaldando...")
                .progress(true, 0)
                .cancelable(false)
                .build();
        dialog.show();
        AsyncTask.execute(() -> {
            try {
                BackupObject backupObject = new BackupObject(getList(id));
                DBC.files().uploadBuilder("/" + id)
                        .withMute(true)
                        .withMode(WriteMode.OVERWRITE)
                        .uploadAndFinish(new ByteArrayInputStream(new Gson().toJson(backupObject, getType(id)).getBytes(StandardCharsets.UTF_8)));
                backupInterface.onResponse(backupObject);
            } catch (Exception e) {
                e.printStackTrace();
                backupInterface.onResponse(null);
            }
            closeDialog(dialog);
        });
    }

    private static void backupDrive(final String id, final BackupInterface backupInterface) {
        final MaterialDialog dialog = new MaterialDialog.Builder(activity)
                .content("Respaldando...")
                .progress(true, 0)
                .cancelable(false)
                .build();
        dialog.show();
        AsyncTask.execute(() -> {
            final Task<DriveFolder> appFolderTask = DRC.getAppFolder();
            final Task<DriveContents> driveContents = DRC.createContents();
            final BackupObject backupObject = new BackupObject(getList(id));
            Tasks.whenAll(appFolderTask, driveContents)
                    .continueWithTask(task -> {
                        Query query = new Query.Builder()
                                .addFilter(Filters.contains(SearchableField.TITLE, id))
                                .build();
                        return DRC.queryChildren(appFolderTask.getResult(), query);
                    }).continueWithTask(task -> {
                        MetadataBuffer metadata = task.getResult();
                        if (metadata.getCount() > 0)
                            DRC.delete(metadata.get(0).getDriveId().asDriveResource());
                        metadata.release();
                        DriveContents contents = driveContents.getResult();
                        OutputStream outputStream = contents.getOutputStream();

                        try (Writer writer = new OutputStreamWriter(outputStream)) {
                            writer.write(new Gson().toJson(backupObject, getType(id)));
                        }
                        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                .setTitle(id)
                                .setMimeType("application/json")
                                .setStarred(true)
                                .build();

                        return DRC.createFile(appFolderTask.getResult(), changeSet, contents);
            }).addOnSuccessListener(activity, driveFile -> {
                        closeDialog(dialog);
                        backupInterface.onResponse(backupObject);
            }).addOnFailureListener(activity, e -> {
                        closeDialog(dialog);
                        backupInterface.onResponse(null);
            });
        });
    }

    public static void restoreDialog(final String id, final BackupObject backupObject) {
        new MaterialDialog.Builder(activity)
                .content("¿Como desea restaurar?")
                .positiveText("mezclar")
                .negativeText("reemplazar")
                .onAny((dialog, which) -> restore(which == DialogAction.NEGATIVE, id, backupObject)).build().show();
    }

    private static void restore(final boolean replace, final String id, final BackupObject backupObject) {
        final MaterialDialog dialog = new MaterialDialog.Builder(activity)
                .content("Restaurando...")
                .progress(true, 0)
                .cancelable(false)
                .build();
        dialog.show();
        AsyncTask.execute(() -> {
            try {
                switch (id) {
                    case "favs":
                        if (replace)
                            CacheDB.INSTANCE.favsDAO().clear();
                        CacheDB.INSTANCE.favsDAO().addAll(backupObject.data);
                        break;
                    case "history":
                        if (replace)
                            CacheDB.INSTANCE.recordsDAO().clear();
                        CacheDB.INSTANCE.recordsDAO().addAll(backupObject.data);
                        break;
                    case "following":
                        if (replace)
                            CacheDB.INSTANCE.seeingDAO().clear();
                        CacheDB.INSTANCE.seeingDAO().addAll(backupObject.data);
                        break;
                    case "seen":
                        if (replace)
                            CacheDB.INSTANCE.chaptersDAO().clear();
                        CacheDB.INSTANCE.chaptersDAO().addAll(backupObject.data);
                        break;
                }
                Toaster.toast("Restauración completada");
            } catch (Exception e) {
                e.printStackTrace();
                Toaster.toast("Error al restaurar");
            } finally {
                closeDialog(dialog);
            }
        });
    }

    public static void silentRestoreAll() {
        if (isLogedIn()) {
            search("favs", backupObject -> {
                if (backupObject != null)
                    AsyncTask.execute(() -> {
                        CacheDB.INSTANCE.favsDAO().addAll(backupObject.data);
                        Log.e("Sync", "Favs sync");
                        //Toaster.toast("Favoritos sincronizados");
                    });
            });
            search("seen", backupObject -> {
                if (backupObject != null)
                    AsyncTask.execute(() -> {
                        CacheDB.INSTANCE.chaptersDAO().addAll(backupObject.data);
                        Log.e("Sync", "Seen sync");
                        //Toaster.toast("Capitulos vistos sincronizados");
                    });
            });
            search("history", backupObject -> {
                if (backupObject != null)
                    AsyncTask.execute(() -> {
                        CacheDB.INSTANCE.recordsDAO().addAll(backupObject.data);
                        Log.e("Sync", "History sync");
                        //Toaster.toast("Ultimos vistos sincronizados");
                    });
            });
        }
    }

    private static void closeDialog(MaterialDialog dialog) {
        try {
            if (dialog.isShowing())
                dialog.dismiss();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List getList(String id) {
        switch (id) {
            case "favs":
                return CacheDB.INSTANCE.favsDAO().getAllRaw();
            case "history":
                return CacheDB.INSTANCE.recordsDAO().getAllRaw();
            case "following":
                return CacheDB.INSTANCE.seeingDAO().getAllRaw();
            case "seen":
                return CacheDB.INSTANCE.chaptersDAO().getAll();
            default:
                return new ArrayList();
        }
    }

    private static Type getType(String id) {
        switch (id) {
            case "favs":
                return new TypeToken<BackupObject<FavoriteObject>>() {
                }.getType();
            case "history":
                return new TypeToken<BackupObject<RecordObject>>() {
                }.getType();
            case "following":
                return new TypeToken<BackupObject<SeeingObject>>() {
                }.getType();
            case "seen":
                return new TypeToken<BackupObject<AnimeObject.WebInfo.AnimeChapter>>() {
                }.getType();
            default:
                return new TypeToken<BackupObject>() {
                }.getType();
        }
    }

    public static boolean isAnimeflvInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo("knf.animeflv", 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public enum BUType {
        LOCAL(-1),
        DRIVE(0),
        DROPBOX(1);
        public int value;

        BUType(int value) {
            this.value = value;
        }
    }

    public interface LoginInterface {
        void onLogin();
    }

    public interface SearchInterface {
        void onResponse(@Nullable BackupObject backupObject);
    }

    public interface BackupInterface {
        void onResponse(@Nullable BackupObject backupObject);
    }

}
