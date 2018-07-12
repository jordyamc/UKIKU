package knf.kuma.videoservers;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class VideoServer implements Parcelable {
    public String name;
    public List<Option> options = new ArrayList<>();

    public VideoServer(String name) {
        this.name = name;
    }

    public VideoServer(String name, Option option) {
        this.name = name;
        addOption(option);
    }

    public static final Creator<VideoServer> CREATOR = new Creator<VideoServer>() {
        @Override
        public VideoServer createFromParcel(Parcel in) {
            return new VideoServer(in);
        }

        @Override
        public VideoServer[] newArray(int size) {
            return new VideoServer[size];
        }
    };

    public static List<VideoServer> filter(List<VideoServer> videoServers) {
        List<String> names = new ArrayList<>();
        List<VideoServer> filtered = new ArrayList<>();
        for (VideoServer videoServer : videoServers) {
            if (!names.contains(videoServer.name)) {
                names.add(videoServer.name);
                filtered.add(videoServer);
            }
        }
        return filtered;
    }

    public static List<String> getNames(List<VideoServer> videoServers) {
        List<String> names = new ArrayList<>();
        for (VideoServer videoServer : videoServers) {
            names.add(videoServer.name);
        }
        return names;
    }

    public static int findPosition(List<VideoServer> videoServers, String name) {
        int i = 0;
        for (VideoServer videoServer : videoServers) {
            if (videoServer.name.equals(name))
                return i;
            i++;
        }
        return 0;
    }

    public static boolean existServer(List<VideoServer> videoServers, int position) {
        String name = Names.getDownloadServers()[position - 1];
        for (VideoServer videoServer : videoServers) {
            if (videoServer.name.equals(name))
                return true;
        }
        return false;
    }

    public static VideoServer findServer(List<VideoServer> videoServers, int position) {
        String name = Names.getDownloadServers()[position - 1];
        return videoServers.get(findPosition(videoServers, name));
    }

    public void addOption(Option option) {
        options.add(option);
    }

    public Option getOption() {
        return options.get(0);
    }

    public boolean haveOptions() {
        return options.size() > 1;
    }

    public static class Sorter implements Comparator<VideoServer> {
        @Override
        public int compare(VideoServer videoServer, VideoServer t1) {
            return videoServer.name.compareToIgnoreCase(t1.name);
        }
    }

    protected VideoServer(Parcel in) {
        name = in.readString();
        options = in.createTypedArrayList(Option.CREATOR);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeTypedList(options);
    }

    public static class Names {
        public static final String IZANAGI = "Izanagi";
        public static final String MINHATECA = "Minhateca";
        public static final String YOTTA = "Yotta";
        public static final String HYPERION = "Hyperion";
        public static final String OKRU = "Okru";
        public static final String CLUP = "Clup";
        public static final String FIRE = "Fire";
        public static final String MANGO = "Mango";
        public static final String RV = "RV";
        public static final String MP4UPLOAD = "Mp4Upload";
        public static final String YOURUPLOAD = "YourUpload";
        public static final String ZIPPYSHARE = "Zippyshare";
        public static final String _4SYNC = "4Sync";
        public static final String MEGA = "Mega";
        public static final String ANIMEFLV = "Animeflv";
        public static final String MARU = "Maru";

        public static String[] getDownloadServers() {
            return new String[]{
                    IZANAGI,
                    HYPERION,
                    OKRU,
                    FIRE,
                    RV,
                    YOURUPLOAD,
                    ZIPPYSHARE,
                    MEGA,
                    MP4UPLOAD
            };
        }
    }
}
