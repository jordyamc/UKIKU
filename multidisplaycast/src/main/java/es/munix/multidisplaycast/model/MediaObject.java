package es.munix.multidisplaycast.model;

/**
 * Created by munix on 3/11/16.
 */

public class MediaObject {

    private String title;
    private String subtitle;
    private String image;
    private String mime;
    private String url;
    private Boolean isSeekable = true;
    private int currentVolume;
    private Boolean canChangeVolume = true;
    private Boolean canFastForwart = false;

    public MediaObject( String title, String subtitle, String image, String mime, String url ) {
        this.title = title;
        this.subtitle = subtitle;
        this.image = image;
        this.mime = mime;
        this.url = url;
    }

    public Boolean getCanFastForwart() {
        return canFastForwart;
    }

    public void setCanFastForwart( Boolean canFastForwart ) {
        this.canFastForwart = canFastForwart;
    }


    public int getCurrentVolume() {
        return currentVolume;
    }

    public void setCurrentVolume( int currentVolume ) {
        this.currentVolume = currentVolume;
    }

    public Boolean getCanChangeVolume() {
        return canChangeVolume;
    }

    public void setCanChangeVolume( Boolean canChangeVolume ) {
        this.canChangeVolume = canChangeVolume;
    }

    public Boolean getIsSeekable() {
        return isSeekable;
    }

    public void setIsSeekable( Boolean seekable ) {
        isSeekable = seekable;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getImage() {
        return image;
    }

    public String getMime() {
        return mime;
    }

    public String getUrl() {
        return url;
    }
}
