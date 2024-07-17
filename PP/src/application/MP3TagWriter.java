package application;

import com.mpatric.mp3agic.ID3v24Tag;

import java.io.File;
import java.io.IOException;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.NotSupportedException;

public class MP3TagWriter
{
    public static void setTitle(Song song, String newTitle) throws NotSupportedException, IOException {
        try 
        {
            Mp3File mp3file = new Mp3File(song.getFilePath());
            ID3v2 tag = getOrCreateId3v2Tag(mp3file);
            tag.setTitle(newTitle);
            saveMp3File(mp3file, song.getFilePath(), "temp_title.mp3");  // Using a temporary file for saving
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setArtist(Song song, String newArtist) throws NotSupportedException, IOException {
        try 
        {
            Mp3File mp3file = new Mp3File(song.getFilePath());
            ID3v2 tag = getOrCreateId3v2Tag(mp3file);
            tag.setArtist(newArtist);
            saveMp3File(mp3file, song.getFilePath(), "temp_artist.mp3");  // Using a temporary file for saving
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setAlbum(Song song, String newAlbum) throws NotSupportedException, IOException {
        try 
        {
            Mp3File mp3file = new Mp3File(song.getFilePath());
            ID3v2 tag = getOrCreateId3v2Tag(mp3file);
            tag.setAlbum(newAlbum);
            saveMp3File(mp3file, song.getFilePath(), "temp_album.mp3");  // Using a temporary file for saving
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setYear(Song song, String newYear) throws NotSupportedException, IOException {
        try 
        {
            Mp3File mp3file = new Mp3File(song.getFilePath());
            ID3v2 tag = getOrCreateId3v2Tag(mp3file);
            tag.setYear(newYear);
            saveMp3File(mp3file, song.getFilePath(), "temp_year.mp3");  // Using a temporary file for saving
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static ID3v2 getOrCreateId3v2Tag(Mp3File mp3file) {
        ID3v2 tag = mp3file.getId3v2Tag();
        if (tag == null) {
            tag = new ID3v24Tag();
            mp3file.setId3v2Tag(tag);
        }
        return tag;
    }

    private static void saveMp3File(Mp3File mp3file, String originalPath, String tempPath) throws NotSupportedException, IOException {
        try {
            mp3file.save(tempPath);
            replaceOriginalFile(originalPath, tempPath);
        } catch (Exception e) {
            e.printStackTrace();
            throw new NotSupportedException("Could not save the MP3 file with updated tags: " + e.getMessage());
        }
    }

    private static void replaceOriginalFile(String originalPath, String tempPath) throws IOException {
        File originalFile = new File(originalPath);
        File tempFile = new File(tempPath);
        
        if (originalFile.delete()) 
        {
            if (!tempFile.renameTo(originalFile)) 
            {
                throw new IOException("Failed to rename " + tempPath + " to " + originalPath);
            }
        } 
        else 
        {
            throw new IOException("Failed to delete the original file: " + originalPath);
        }
    }
}
