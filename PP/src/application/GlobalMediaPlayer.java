package application;

import javafx.collections.ObservableList;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

/**
 * GlobalMediaPlayer is a MediaPlayer imported from javaFX this is used as a
 * single instance using Singleton Pattern I learnt in Software Engineering 2
 * It supports features like play/pause, next/previous, frequently played & track selection
 **/
public class GlobalMediaPlayer implements Serializable 
{
    private static MediaPlayer mediaPlayer;
    private static Media media;
    
    private static String songName;
    private static int songIndex;
    private static ObservableList<Song> songs;
    
    private static boolean isPlaying = false;

    private static Map<Song, Integer> playCounts = new HashMap<>();// initialises a map with the song then the amount of times it's played
    private static final String PLAY_COUNTS_FILE = "play_counts.ser";//for frequently played songs list to stay persistant


    //Imports the songs from SceneController.java
    public static void loadSongs(ObservableList<Song> songData) 
    {
    	songs = songData;
    }
    
    //Singleton instance of the controller for a specific song (Switching scenes)
    public static MediaPlayer getInstance(int songNumber) 
    {
    	songIndex = songNumber;
        if (mediaPlayer == null) 
        {
            media = songs.get(songNumber).getMedia();//
            mediaPlayer = new MediaPlayer(media);
        } 
        else 
        {
//            mediaPlayer.stop();
//            mediaPlayer.dispose();
//            Media media = new Media(songs.get(songNumber).toURI().toString());
 //           mediaPlayer = new MediaPlayer(media);
        }
        return mediaPlayer;
    }
    
    public static void setNewSong(File songFile) 
         {
        media = new Media(songFile.toURI().toString());
        if (mediaPlayer != null) 
        {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
        mediaPlayer = new MediaPlayer(media);
        
    }   
         
         
    /**---------------------------------------Operations for Media Player-------------------------------------------**/
    /**-------------------------------------------All Using Singleton-----------------------------------------------**/

    public static void play() 
    {
        if (mediaPlayer != null) 
        {
            mediaPlayer.play();
            isPlaying=true;
        }
    }
    public static void pause() 
    {
        System.out.println("pause clicked");
		if (mediaPlayer != null) 
		{
            mediaPlayer.pause();
            isPlaying=false;
        }
    }		
    public static void stop() 
    {
        if (mediaPlayer != null) 
        {
            mediaPlayer.stop();
            isPlaying=false;
        }
    }

    


   
    /**---------------------------------------Getters for Media Player-------------------------------------------**/
	//All methods for UI updates
    public static Duration getCurrentTime() 
	{
		return mediaPlayer.getCurrentTime();
	}
	
	public static Media getMedia() 
	{
		return media;
	}
	
	public static boolean getPlaying() 
	{
		return isPlaying;
	}
	
	public static String getSongName() 
	{
		return songs.get(songIndex).getTitle();
	}
	
	public static int getSongIndex() 
	{
		return songIndex;
	}
	public static Song getSong(int songNumber) 
	{
		return songs.get(songNumber);
				}
	

	
	
    /**---------------------------------------Frequently Played Songs-------------------------------------------**/

    // Method to increment play count of a song,then update map
    public static void incrementPlayCount(Song song) 
    {
        int count = playCounts.getOrDefault(song, 0);//retrieves current playcount for the song - default 0 if song isn't in map
        playCounts.put(song, count + 1);//Update Map
    }

    //Method to remove song from the playcount map
    public static void removeSongFromPlayCount(Song song) 
    {
        playCounts.remove(song);
    }

    /**
     * Retrieves a list of the top N frequently played songs, sorted by play counts in 
     * descending order. This is utilised to feature songs played often by the user.
     * 
     * @param N     number of top songs to retrieve.
     * @return      list of the top N frequently played songs.
     */
    public static List<Song> getTopFrequentlyPlayedSongs(int N) 
    {
        List<Song> sortedSongs = new ArrayList<>(playCounts.keySet());// new list of songs in playcounts map
        sortedSongs.sort((s1, s2) -> playCounts.get(s2) - playCounts.get(s1));//sort in decending order of playcounts

        // Return the top N songs
        return sortedSongs.subList(0, Math.min(N, sortedSongs.size()));
    }

    // Method to save playCounts map to a serialised file
    public static void saveFrequentlyPlayedList() 
    {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(PLAY_COUNTS_FILE))) 
        {
            oos.writeObject(playCounts);//Serialise playcounts map into play_counts.ser
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads the play count data from a file into the application. This method is called at the start of the
     * application to load up the previous most played songs from your last run into this new session. This 
     * way the data stays persistent.
     */
    @SuppressWarnings("unchecked")
	public static void loadFrequentlyPlayedList() 
    {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(PLAY_COUNTS_FILE))) 
        {
            playCounts = (Map<Song, Integer>) ois.readObject();//deserialise playcount map from file

        //Iteration over all the entries in the map:
            Iterator<Map.Entry<Song, Integer>> iterator = playCounts.entrySet().iterator();
            while (iterator.hasNext()) 
            {
                Map.Entry<Song, Integer> entry = iterator.next();
                Song song = entry.getKey();

                // Check if the song is still exists
                if (!song.isValid()) 
                {
                    // If the song is not valid, remove it from the playCounts map
                    iterator.remove();
                }
            }

        } 
        catch (IOException | ClassNotFoundException e) 
        {
            // If the file doesn't exist or couldn't be read, initialise an empty map
            playCounts = new HashMap<>();
        }
    }

}