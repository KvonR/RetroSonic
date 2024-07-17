package application;

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

import javafx.scene.image.Image;
import javafx.scene.media.Media;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class Song implements Serializable 
{
    private String title;
    private String artist;
    private String album;
    private String year;
    private String duration;
    private File file;
    private transient Image albumArt;//stop it getting serialised into song data
    
    // Constructor
    public Song(File file) 
    {
        // Initialize with default values
        this.title = "";
        this.artist = "";
        this.album = "";
        this.year = "";
        this.file = file;
        
        extractMetadata();
        extractAlbumArt();
    }
    
    private void extractMetadata() {
        try {
            Mp3File mp3file = new Mp3File(this.file.getPath());
            String name = file.getName();//Incase file has no title
            name = name.substring(0, name.length() - 4);//remove .mp3
            
            if (mp3file.hasId3v2Tag()) {
                ID3v2 id3v2tag = mp3file.getId3v2Tag();
                title = id3v2tag.getTitle() != null ? id3v2tag.getTitle() : name;
                artist = id3v2tag.getArtist() != null ? id3v2tag.getArtist() : "Unknown Artist";
                album = id3v2tag.getAlbum() != null ? id3v2tag.getAlbum() : "Unknown Album";
                year = id3v2tag.getYear() != null ? id3v2tag.getYear() : "N/A";
                // ID3v2 might also contain more info such as comments, genre, etc. This is for future work: extracting more metadata values like those mentioned previous
            } else if (mp3file.hasId3v1Tag()) {
                // Some older files might only have ID3v1 tags
                ID3v1 id3v1tag = mp3file.getId3v1Tag();
                title = id3v1tag.getTitle() != null ? id3v1tag.getTitle() : name;
                artist = id3v1tag.getArtist() != null ? id3v1tag.getArtist() : "Unknown Artist";
                album = id3v1tag.getAlbum() != null ? id3v1tag.getAlbum() : "Unknown Album";
                year = id3v1tag.getYear() != null ? id3v1tag.getYear() : "N/A";
            }

            // Extract duration
            long durationSeconds = mp3file.getLengthInSeconds();
            duration = String.format("%02d:%02d", durationSeconds / 60, durationSeconds % 60);
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
            //Handle Error - Potential with other default values or console log in future work
        }
    }
    
    
    private void extractAlbumArt() 
    {
        try 
        {
            Mp3File mp3file = new Mp3File(this.file.getPath());
            if (mp3file.hasId3v2Tag()) 
            {
                ID3v2 id3v2tag = mp3file.getId3v2Tag();
                byte[] imageData = id3v2tag.getAlbumImage();
                if (imageData != null) {
                    this.albumArt = new Image(new ByteArrayInputStream(imageData));
                }
                else 
                {
        			this.albumArt = new Image(getClass().getResourceAsStream("/main/icons/Logo.png"));
                }
            }
        } 
        catch (IOException | UnsupportedTagException | InvalidDataException e) 
        {       	
            e.printStackTrace();
            //Handle exception - can possibly system log the warning .
        }
    }

// Getters and setters:
    public Image getAlbumArt() {
        return albumArt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public boolean isValid() {
        if (file != null) {
            return file.exists();
        }
        return false;
    }
    
    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }
    public Media getMedia() {
            return new Media(new File(getFilePath()).toURI().toString());
        }
    
	public String getFilePath() {
		return file.getPath();
	}
	 
    @Override
    public String toString() {
        return "Song{" +
                "title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                ", album='" + album + '\'' +
                ", year='" + year + '\'' +
                '}';
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Song song = (Song) o;
        return Objects.equals(title, song.title) && Objects.equals(artist, song.artist);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, artist);
    }

}