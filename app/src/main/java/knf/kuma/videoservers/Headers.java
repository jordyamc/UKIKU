package knf.kuma.videoservers;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class Headers implements Parcelable {
    public static final Creator<Headers> CREATOR = new Creator<Headers>() {
        @Override
        public Headers createFromParcel(Parcel in) {
            return new Headers(in);
        }

        @Override
        public Headers[] newArray(int size) {
            return new Headers[size];
        }
    };
    private List<Pair<String, String>> headers = new ArrayList<>();
    private List<Pair<String, String>> cookies = new ArrayList<>();

    public Headers() {
    }

    public Headers(Parcel parcel) {
        headers = parcel.readArrayList(null);
        cookies = parcel.readArrayList(null);
    }

    public void addHeader(String key, String value) {
        headers.add(new Pair<>(key, value));
    }

    public void addCookie(String key, String value) {
        cookies.add(new Pair<>(key, value));
    }

    public List<Pair<String, String>> getHeaders() {
        if (cookies.size() > 0)
            headers.add(new Pair<>("Cookie", getCookies()));
        return headers;
    }

    private String getCookies() {
        StringBuilder builder = new StringBuilder();
        for (Pair<String, String> pair : cookies)
            builder.append(pair.first)
                    .append('=')
                    .append(pair.second)
                    .append("; ");
        return builder.toString().trim();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeList(headers);
        parcel.writeList(cookies);
    }
}
