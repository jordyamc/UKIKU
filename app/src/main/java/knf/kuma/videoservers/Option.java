package knf.kuma.videoservers;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Option implements Parcelable {
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
    public String server;
    public String name;
    public String url;
    public Headers headers;

    /**
     * Crea una opcion de descarga
     *
     * @param server  Nombre del servidor de donde viene la opcion {@link VideoServer.Names}
     * @param name    Nombre de la opcion, null si es una opcion unica
     * @param url     Url de la opcion
     * @param headers Header requerido por la opcion
     */
    public Option(String server, @Nullable String name, String url, @Nullable Headers headers) {
        if (url == null || url.trim().isEmpty())
            throw new IllegalStateException("Url is not valid!");
        this.server = server;
        this.name = name;
        this.url = url;
        this.headers = headers;
    }

    public Option(String server, @Nullable String name, String url) {
        if (url == null || url.trim().isEmpty())
            throw new IllegalStateException("Url is not valid!");
        this.server = server;
        this.name = name;
        this.url = url;
    }

    protected Option(Parcel in) {
        server = in.readString();
        name = in.readString();
        url = in.readString();
        headers = in.readParcelable(Headers.class.getClassLoader());
    }

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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(server);
        dest.writeString(name);
        dest.writeString(url);
        dest.writeParcelable(headers, flags);
    }
}
