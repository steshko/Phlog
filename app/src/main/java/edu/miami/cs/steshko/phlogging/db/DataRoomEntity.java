package edu.miami.cs.steshko.phlogging.db;

import android.graphics.Bitmap;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Phlog")
public class DataRoomEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "dateTime")
    private long dateTime;

    @ColumnInfo(name = "text")
    private String text;

    @ColumnInfo(name = "photo")
    private byte[] photo;

    @ColumnInfo(name = "location")
    private String location;

    @ColumnInfo(name = "orientation")
    private String orientation;

    @ColumnInfo(name = "galleryPictures")
    private String galleryPictures;

    @ColumnInfo(name = "recording")
    private byte[] recording;

    //Constructors
    public DataRoomEntity() {
    }

    //Getters and Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getDateTime() {
        return dateTime;
    }

    public void setDateTime(long dateTime) {
        this.dateTime = dateTime;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getOrientation() {
        return orientation;
    }

    public void setOrientation(String orientation) {
        this.orientation = orientation;
    }

    public String getGalleryPictures() {
        return galleryPictures;
    }

    public void setGalleryPictures(String galleryPictures) {
        this.galleryPictures = galleryPictures;
    }

    public byte[] getRecording() {
        return recording;
    }

    public void setRecording(byte[] recording) {
        this.recording = recording;
    }
}