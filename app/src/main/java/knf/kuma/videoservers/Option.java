package knf.kuma.videoservers;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class Option implements Parcelable {
    public String server;
    public String name;
    public String url;

    public Option(String server, String name, String url) {
        this.server = server;
        this.name = name;
        this.url = url;
    }

    public static final Creator<Option> CREATOR = new Creator<Option>() {
        @Override
        public Option createFromParcel(Parcel in) {
            return new Option(in);
        }

        @Override
        public Option[] newArray(int size) {
            return new Option[size];
        }
    };

    public static List<String> getNames(List<Option> options) {
        List<String> names = new ArrayList<>();
        for (Option option : options)
            names.add(option.name);
        return names;
    }

    public static List<String> getLinks(List<Option> options) {
        List<String> links = new ArrayList<>();
        for (Option option : options)
            links.add(option.name);
        return links;
    }

    protected Option(Parcel in) {
        server = in.readString();
        name = in.readString();
        url = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(server);
        dest.writeString(name);
        dest.writeString(url);
    }
}
