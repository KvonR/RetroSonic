package application;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.chart.Chart;
import javafx.scene.control.*;

import java.io.*;
import java.net.URL;
import java.util.*;

import com.mpatric.mp3agic.NotSupportedException;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

//Controls the functionality of the FXML Files Initializable used for FXML UI initialization
public class SceneController implements Initializable{
	
	private Stage stage;
	private Scene scene;
	private Parent root;
	private String songNamestr;
	@FXML
	private ImageView songArt;
	@FXML
	private Pane pane;
	@FXML
	private Label songLabel;
	@FXML
	private Label artistLabel;
	@FXML
	private Label albumLabel;
	
	@FXML
	private Button playButton, previousButton,nextButton;

	@FXML
	ImageView playPauseImageMain;
	@FXML
	ImageView playPauseImagePlaylist;
	@FXML
	ImageView playPauseImage;
	
	@FXML
	private ProgressBar songProgressBar;
	@FXML
	private FlowPane playlists_container;
	

	private File directory;
	private File[] files;
	private ArrayList<File> songs;

	private int songNumber;
	private Timer timer;
	private TimerTask task;
	@FXML
	private Label songLength;
	@FXML
	private Label timeElapsed;
	
	@FXML
	private TableView<Song> tableView;
	@FXML
	private TableView<Song> tableView2;
	@FXML
	private Label selectedPL;
	@FXML
	private TableColumn<Song, String> songTable;
	@FXML
	private TableColumn<Song, String> artistTable;
	@FXML
	private TableColumn<Song, String> albumTable;
	@FXML
	private TableColumn<Song, String> yearTable;
	@FXML
	private TableColumn<Song, String> actionTC;//add to playlist
	@FXML
	private TableColumn<Song, String> actionTC2;//remove from playlist

	@FXML
	private VBox freqPlayedSongs;
	private ObservableList<Song> songData = FXCollections.observableArrayList();
	private ObservableList<Song> playListData = FXCollections.observableArrayList();



	private static Map<String, List<Song>> playlists;
	private static Song playingSong;
	
	//Used to show which scene is selected to choose what to populate
	private static boolean fromLibrary = false;
	private static boolean fromPlaylists = false;
//This method is run every time the scene is switched
	@Override
    public void initialize(URL location, ResourceBundle resources) 
	{
		//Playlists loaded from a serialised file, or if the file doesn't exist makes a new one
		Map<String, List<Song>> p = loadPlaylists();
		playlists = p != null ? p : new HashMap<>();
		validatePlayLists();//Remove deleted songs

		System.out.print(songNumber);		
		//Import songData if it's empty (first run)
		
		if(songData.isEmpty()) 
		{
			loadSongs();
			for (File file : songs) 
			{
				Song song = new Song(file);
				if(song.isValid()) {
					songData.add(song);
				}
			}
			GlobalMediaPlayer.loadSongs(songData);
		}

//Choosing which part of UI to populate:		
		if(!fromLibrary && !fromPlaylists) 
		{
			populateFrequentlyPlayedSongs();
		}

		if(fromPlaylists) 
		{
			populatePlaylistButtons(this);
		}

		if(!songs.isEmpty())
		{
            GlobalMediaPlayer.getInstance(songNumber);
            //songLabel.setText(songs.get(GlobalMediaPlayer.getSongIndex()).getName());
        }
        System.out.println(GlobalMediaPlayer.getSongIndex());//TEST
		songProgressBar.setStyle("-fx-accent: #397DFF;");//progress bar
		updateSongDetails();

	}

	//Index of song found by iterating over list of songData
	private int getNumberOf(Song song) 
	{
		for (int i = 0; i < songs.size(); i++) 
		{
			if(songData.get(i).equals(song)) 
			{
				return i;
			}
		}
		return -1;//Song not found
	}
	
	//Load songs from the "music" directory
	public ArrayList<File> loadSongs()
	{
    	songs = new ArrayList<File>();
		directory = new File("music");	
		files = directory.listFiles();
		
		//Add all files to the list of songs
		if(files != null) 
		{
			for(File file : files) 
			{
				songs.add(file);
			}
		}
		return songs;
    }
	
	/**----------------------------------------------------------------------------------**/
	/**----------------------------------Progress Bar------------------------------------**/
	/**----------------------------------------------------------------------------------**/
//Method for updating the progress bar on the song
    public void beginTimer() {
        if(timer == null) 
        {
			timer = new Timer();
		}
        System.out.println("Test");
        task = new TimerTask() {
            public void run() {
                Platform.runLater(() -> {//Updates UI on the JavaFX application thread
                        double current = GlobalMediaPlayer.getCurrentTime().toSeconds();
                        double end = GlobalMediaPlayer.getMedia().getDuration().toSeconds();  
                        String formattedCurrent = String.format("%d:%02d", (int) current / 60, (int) current % 60);
                        String formattedEnd = String.format("%d:%02d", (int) end / 60, (int) end % 60);
                        //Update UI:
                        songLength.setText(formattedEnd);
                        timeElapsed.setText(formattedCurrent);
                        songProgressBar.setProgress(current/end);
                        
                        //System.out.println("TEST"+current/end);
                    	if(current/end == 1) { //Song is finished when current and end = 1
        					cancelTimer();
        					nextMedia();
        					beginTimer();
        				}
                    }
                );
            }
        };
        timer.scheduleAtFixedRate(task, 0, 1000);        // Schedule the task to run every second
    }
    
//Cancel the timer:
    public void cancelTimer() 
    {
        if (timer != null) {
            timer.cancel();
            timer.purge(); 
            timer = null;
        }
    }
	/**----------------------------------------------------------------------------------**/
	/**----------------------------------Music Buttons-----------------------------------**/
	/**----------------------------------------------------------------------------------**/
//Play & Pause Button:
	public void playMedia() 
	{
		System.out.println("play button clicked " + fromLibrary);
		
		
		if (GlobalMediaPlayer.getPlaying()) //If media player is playing 
		{//Update Icons:
			if (fromLibrary) 
			{
				playPauseImage.setImage(new Image(getClass().getResourceAsStream("/main/icons/playbutton.png")));
			} 
			else if (fromPlaylists) 
			{
				playPauseImagePlaylist.setImage(new Image(getClass().getResourceAsStream("/main/icons/playbutton.png")));
			} 
			else 
			{
				playPauseImageMain.setImage(new Image(getClass().getResourceAsStream("/main/icons/playbutton.png")));
			}
			GlobalMediaPlayer.pause();//If playing n button clicked, pause
		} 
		else //Not playing
		{//Update Icons:
			if (fromLibrary) 
			{
				playPauseImage.setImage(new Image(getClass().getResourceAsStream("/main/icons/pause.png")));
			} 
			else if (fromPlaylists) 
			{
				playPauseImagePlaylist.setImage(new Image(getClass().getResourceAsStream("/main/icons/pause.png")));
			} 
			else 
			{
				playPauseImageMain.setImage(new Image(getClass().getResourceAsStream("/main/icons/pause.png")));
			}
			beginTimer();

			if (GlobalMediaPlayer.getMedia() != null) 
			{
				GlobalMediaPlayer.play();
			} 
			else 
			{
				updateMediaPlayer();//Both methods will play when button clicked
			}
		}
	}

//Previous Song Button:
    public void previousMedia() 
    {
		System.out.println("songN in previousMedia " + songNumber);
		
		if(fromPlaylists) 
		{
			int index = tableView2.getSelectionModel().getSelectedIndex();//Current index
			if (index > 0) //Check if it's first song
			{
				index--;
			} 
			else //If it's first song, loop back round 
			{
				index = playListData.size() - 1;
			}
			songNumber = getSongNumberOf(playListData.get(index));//Update songNumber
			tableView2.getSelectionModel().select(index);//select in UI

		} 
		else 
		{
			if (songNumber > 0) //Check first song 	
			{
				songNumber--;
			} 
			else //if it is loop back
			{
				songNumber = songs.size() - 1;
			}

		}

        updateMediaPlayer();//plays new song
    }
    
//Next Song Button:
    public void nextMedia() //Same context as the previousMedia
    {
		if(fromPlaylists) 
		{
			int index = tableView2.getSelectionModel().getSelectedIndex();
			if (index < playListData.size() - 1) 
			{
				index++;
			} else 
			{
				index = 0;
			}
			tableView2.getSelectionModel().select(index);

			songNumber = getSongNumberOf(playListData.get(index));
		} 
		else 
		{
			if (songNumber < songs.size() - 1) 
			{
				songNumber++;
			} 
			else 
			{
				songNumber = 0;
			}
		}

        updateMediaPlayer();
    }
    
    
    
//    private void updateSceneMedia() {
//        GlobalMediaPlayer.setNewSong(songs.get(songNumber));
//        songLabel.setText(songs.get(songNumber).getName());
//    }
    
//Sets song then plays:    
    private void updateMediaPlayer() 
    {
        GlobalMediaPlayer.setNewSong(songs.get(songNumber));//Song set to current songNumber
		GlobalMediaPlayer.incrementPlayCount(songData.get(songNumber));//For frequently played count
		GlobalMediaPlayer.saveFrequentlyPlayedList();//Save list

		System.out.println("fromLibrary = " + fromLibrary);//Test
		
		//Update icon in scene.
		if (fromLibrary) 
		{
			playPauseImage.setImage(new Image(getClass().getResourceAsStream("/main/icons/pause.png")));
		} 
		else if(fromPlaylists && playPauseImagePlaylist != null) 
		{
			playPauseImagePlaylist.setImage(new Image(getClass().getResourceAsStream("/main/icons/pause.png")));
		} 
		else if(playPauseImageMain != null)
		{
			playPauseImageMain.setImage(new Image(getClass().getResourceAsStream("/main/icons/pause.png")));
		}
		updateSongDetails();
		GlobalMediaPlayer.play();//Play set song
    }
    
	/**----------------------------------------------------------------------------------**/
	/**------------------------------Upload Music Button---------------------------------**/
	/**----------------------------------------------------------------------------------**/
    //Method to allow music to be uploaded, directories imported in file by file
	@FXML
	private void uploadMusicAction(ActionEvent event) 
	{
	    FileChooser fileChooser = new FileChooser();
	    
	    // Set extension filter for .mp3 files and allow directories
	    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("MP3 files (*.mp3)", "*.mp3"));
	    fileChooser.setTitle("Select Music Files or Directory");
	    
	    List<File> selectedFiles = fileChooser.showOpenMultipleDialog(null); // Enable multiple selection

	    if (selectedFiles != null) //If file selected
	    {
	        for (File file : selectedFiles) //Every file in selected files
	        {
	            if (file.isDirectory()) //If Directory
	            {
	                File[] filesInDir = file.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp3"));//Extract every file MP3 to filesInDir
	                if (filesInDir != null) //If not empty
	                {
	                    for (File mp3File : filesInDir) //Every MP3 File in fileInDir
	                    {
	                        copyFileToMusicFolder(mp3File);//Copy them over to music folder
	                    }
	                }
	            } 
	            else if (file.getName().toLowerCase().endsWith(".mp3")) //Selected is one MP3
	            {
	                copyFileToMusicFolder(file);//Copy over to music folder
	            }
	        }
	    }
	}

	private void copyFileToMusicFolder(File file) 
	{
	    File dest = new File("music/" + file.getName());
	    copyFile(file, dest);
	    if (dest.exists()) //Tests
	    {
	        System.out.println("Uploaded: " + dest.getAbsolutePath());
	    }  
	    else 
	    {
	        System.out.println("Failed to upload: " + file.getAbsolutePath());
	    }
	}

	private void copyFile(File source, File dest) 
	{
	    try (FileInputStream is = new FileInputStream(source); //Both streams close at same time
	    	FileOutputStream os = new FileOutputStream(dest)) 
	    {
	        byte[] buffer = new byte[1024];
	        int length;
	        
	        while ((length = is.read(buffer)) > 0) //Length = exact number of bytes to dest file
	        {
	            os.write(buffer, 0, length);
	        }
	    } 
	    catch (IOException e) //Can't copy
	    {
	        e.printStackTrace();
	    }
	}
	
	/**----------------------------------------------------------------------------------**/
	/**-----------------------------Switch Scenes Buttons--------------------------------**/  
	/**----------------------------------------------------------------------------------**/
//Switch to Main.FXML
	public void switchToMain(ActionEvent event) throws IOException
	{
		fromPlaylists = false;
		fromLibrary = false;
		
		System.out.println("switchToMain");
		
		Parent root = FXMLLoader.load(getClass().getResource("Main.fxml"));//Load main.fxml
		stage = (Stage)((Node)event.getSource()).getScene().getWindow();//Current Stage from button click
		scene = new Scene(root);//New scene with root loaded from Main.FXML
		stage.setScene(scene);//Set new scene on current stage
		stage.show();//Show the main view

	}
//Switch to Library.FXML
	public void switchToLibrary(ActionEvent event) throws IOException 
	{
		fromLibrary = true;//Context for initialisation
		fromPlaylists = false;

		System.out.println("switchToLibrary");

		FXMLLoader loader = new FXMLLoader(getClass().getResource("Library.fxml"));
		Parent root = loader.load();
		SceneController sceneController = loader.getController();
		//Setup Table View Cells:
		sceneController.getSongTable().setCellValueFactory(new PropertyValueFactory<>("title"));
		sceneController.getArtistTable().setCellValueFactory(new PropertyValueFactory<>("artist"));
		sceneController.getAlbumTable().setCellValueFactory(new PropertyValueFactory<>("album"));
		sceneController.getYearTable().setCellValueFactory(new PropertyValueFactory<>("year"));

		// Cell for adding to the playlist
		sceneController.getActionTC().setCellFactory(new Callback<TableColumn<Song, String>, TableCell<Song, String>>() 
		{
			@Override
			public TableCell<Song, String> call(TableColumn<Song, String> param) 
			{
				return new TableCell<Song, String>() 
				{
					final Button btn = new Button("+");
					{
						btn.setStyle("-fx-background-color: green; -fx-text-fill: white; -fx-font-size: 20px; -fx-padding: 0 10 0 10; -fx-margin: 0;");
						btn.setOnAction(event -> {
							Song song = getTableView().getItems().get(getIndex());//Get index of song to add to playlist
							System.out.println("Button clicked for song: " + song.getTitle());
							showAddToPlaylistDialog(song);
						});
					}
					@Override
					public void updateItem(String item, boolean empty) {
						super.updateItem(item, empty);
						if (empty) 
						{
							setGraphic(null);
							setText(null);
						} 
						else 
						{
							setGraphic(btn);
							setText(null);
						}
					}
				};
			}
		});
		//Update table view with songData
		songData = sceneController.songData;
		sceneController.getTableView().setItems(songData);
		
		if (!songData.isEmpty()) {
			Song playingSong = songData.get(songNumber);
			sceneController.getTableView().getSelectionModel().select(playingSong);
		}
		
		System.out.println(songNumber);

		//Set and show new  scene:
		stage = (Stage)((Node)event.getSource()).getScene().getWindow();
		scene = new Scene(root);
		stage.setScene(scene);
		stage.show();		
	}
	
//Switch To Playlist:
	public void switchToPlaylist(ActionEvent event)throws IOException 
	{
		fromPlaylists = true;
		fromLibrary = false;
		
		FXMLLoader loader = new FXMLLoader(getClass().getResource("Playlist.fxml"));
		Parent root = loader.load();
		SceneController sceneController = loader.getController();
		//Table
		sceneController.getSongTable().setCellValueFactory(new PropertyValueFactory<>("title"));
		sceneController.getArtistTable().setCellValueFactory(new PropertyValueFactory<>("artist"));
		sceneController.getAlbumTable().setCellValueFactory(new PropertyValueFactory<>("album"));
		sceneController.getYearTable().setCellValueFactory(new PropertyValueFactory<>("year"));
	// Two Action Cells Remove & Add:
		//Add
		sceneController.getActionTC().setCellFactory(new Callback<TableColumn<Song, String>, TableCell<Song, String>>() {
			@Override
			public TableCell<Song, String> call(TableColumn<Song, String> param) {
				return new TableCell<Song, String>() {
					final Button btn = new Button("+");

					{
						btn.setStyle("-fx-background-color: green; -fx-text-fill: white; -fx-font-size: 20px; -fx-padding: 0 10 0 10; -fx-margin: 0;");
						btn.setOnAction(event -> {
							Song song = getTableView().getItems().get(getIndex());
							System.out.println("Button clicked for song: " + song.getTitle());
							showAddToPlaylistDialog(song);

						});
					}

					@Override
					public void updateItem(String item, boolean empty) {
						super.updateItem(item, empty);
						if (empty) {
							setGraphic(null);
							setText(null);
						} else {
							setGraphic(btn);
							setText(null);
						}
					}
				};
			}
		});
		//Remove
		sceneController.getActionTC2().setCellFactory(new Callback<TableColumn<Song, String>, TableCell<Song, String>>() {
			@Override
			public TableCell<Song, String> call(TableColumn<Song, String> param) {
				return new TableCell<Song, String>() {
					final Button btn = new Button("-");

					{
						btn.setStyle("-fx-background-color: red; -fx-text-fill: white; -fx-font-size: 20px; -fx-padding: 0 10 0 10; -fx-margin: 0;");
						btn.setOnAction(event -> {
							Song song = getTableView().getItems().get(getIndex());
							try {
								deleteFromPlayList(song, sceneController, event);
							} catch (IOException e) {
								throw new RuntimeException(e);
							}
						});
					}

					@Override
					public void updateItem(String item, boolean empty) {
						super.updateItem(item, empty);
						if (empty) {
							setGraphic(null);
							setText(null);
						} else {
							setGraphic(btn);
							setText(null);
						}
					}
				};
			}
		});

		//load UI
		stage = (Stage)((Node)event.getSource()).getScene().getWindow();
		scene = new Scene(root);
		stage.setScene(scene);
		stage.show();		
	}

//Add to Playlist Window
	private void showAddToPlaylistDialog(Song song) 
	{
		Stage dialogStage = new Stage();
		dialogStage.initModality(Modality.APPLICATION_MODAL);//Block interaction with main window until selected
		dialogStage.setTitle("Add Song to Playlist");

		ComboBox<String> playlistComboBox = new ComboBox<>();

		ObservableList<String> playlistNames = FXCollections.observableArrayList(playlists.keySet());
		playlistComboBox.setItems(playlistNames);//Existing playlists

		TextField newPlaylistTextField = new TextField();//User Input
		newPlaylistTextField.setPromptText("Enter new playlist name");
		
		Button addButton = new Button("Add");
		addButton.setOnAction(event -> {
			String selectedPlaylist = playlistComboBox.getSelectionModel().getSelectedItem();
			String newPlaylist = newPlaylistTextField.getText().trim();

			if (selectedPlaylist != null || !newPlaylist.isEmpty()) 
			{
				String playlistName = newPlaylist.isEmpty() ? selectedPlaylist : newPlaylist;
				System.out.println("Adding song to playlist: " + playlistName);

				// Get the playlist from the map or create a new one if it doesn't exist
				List<Song> playlist = playlists.getOrDefault(playlistName, new ArrayList<>());
				// Add the song to the playlist
				playlist.add(song);
				// Update the map with the modified playlist
				playlists.put(playlistName, playlist);

				// Save then close:
				savePlaylists();
				dialogStage.close();
			}
		});

	    //UI components in a VBox:
		VBox vBox = new VBox(10);
		vBox.setPadding(new Insets(10));

		Label playlistLabel = new Label("Select Playlist:");
		HBox playlistBox = new HBox(10);
		playlistBox.getChildren().addAll(playlistLabel, playlistComboBox);

		vBox.getChildren().addAll(playlistBox, newPlaylistTextField, addButton);

		Scene dialogScene = new Scene(vBox);
		dialogStage.setWidth(300);
		dialogStage.setScene(dialogScene);
		dialogStage.showAndWait();
	}

	
	private void deleteFromPlayList(Song song, SceneController sceneController, ActionEvent event) throws IOException {
		String plName = sceneController.getSelectedPL().getText();//Current playlist name
		TableView<Song> tv = sceneController.getTableView2();// Table view for songs in the pl
		List<Song> pl = playlists.get(plName);//List of songs in the pl
		if(pl.size() == 1) //If playlist = 1 song
		{
			playlists.remove(plName);//Delete this playlist
			GlobalMediaPlayer.pause();
			//Find the button and delete it from the UI:
			Node node = null;
			for (Node child : sceneController.getPlaylists_container().getChildren()) 
			{
				if(((Button) child).getText() == plName) 
				{
					node = child;
					break;
				}
			}
			sceneController.getPlaylists_container().getChildren().remove(node);
			//Update Changes and Switch to another existing playlist
			savePlaylists();
			switchToPlaylist(event);
			GlobalMediaPlayer.pause();

		} 
		else //If playlist contains more than one song just remove the song
		{
			pl.remove(song);
			playlists.put(plName, pl);
		}
		savePlaylists();
		System.out.println(song.getTitle() + " is deleted from " + plName);
//Update table view:
		if(tv.getSelectionModel().getSelectedItem() != null && tv.getSelectionModel().getSelectedItem().equals(song))   // Check if currently selected item in TableView is the song that was deleted.
		{
			int index = tv.getSelectionModel().getSelectedIndex();//index of removed song
			// Determine new index. If the removed song was not the last one, move to the next song; 
		    // otherwise, wrap around and select the first song in the playlist.
			if (index < pl.size() - 1) 
			{
				index++;
			} else 
			{
				index = 0;
			}
			tv.getSelectionModel().select(index);//Select song in tableview
			
			pl = playlists.get(plName);
			
			songNumber = getSongNumberOf(pl.get(index));//Update songNumber to reflect newly selected song 
			updateMediaPlayer();//Play new song
		}
		
		// Update the TableView's items to reflect the current state of the playlist.
        if (pl != null) 
        {
          // Check if playlist exists (it should not be null after deletion unless it was the only playlist and got removed)
            playListData.setAll(pl);// Update playlistData that backs the TableView with the modified playlist.
            tv.setItems(playListData);// Set the modified list of songs as the new item list for the TableView.
        }

	}
	
//The serialisable file "playlists.ser" is deserialised and contains playlist data, persistently
	public Map<String, List<Song>> loadPlaylists() 
	{
		Map<String, List<Song>> playlists = null;
		try (FileInputStream fileIn = new FileInputStream("playlists.ser");
			 ObjectInputStream objectIn = new ObjectInputStream(fileIn)) 
		{
			playlists = (Map<String, List<Song>>) objectIn.readObject();
		} 
		catch (IOException | ClassNotFoundException e) {
			//e.printStackTrace();
			System.out.println("playlists file is not found");
		}
		return playlists;
	}
	
//Takes playlist data serialises it into playlists.ser so that it is saved for next run
	private void savePlaylists() 
	{
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("playlists.ser"))) 
		{
			oos.writeObject(playlists);
			System.out.println("Playlists saved successfully.");
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			System.out.println("Error saving playlists: " + e.getMessage());
		}
	}

//If song is double clicked start playing
	public void doubleClickOnTableView(MouseEvent event) 
	{
		if(event.getClickCount() == 2) 
		{
			System.out.println("clicked twice");
			songNumber = tableView != null ? tableView.getSelectionModel().getSelectedIndex() : 
				getSongNumberOf(tableView2.getSelectionModel().getSelectedItem());
			updateMediaPlayer();
		}
	}
	
//Get songNumber 
	private int getSongNumberOf(Song song) 
	{
		for (int i = 0; i < songData.size(); i++) 
		{
			if(songData.get(i).equals(song)) 
			{
				return i;
			}
		}

		return -1;
	}


	/**----------------------------------------------------------------------------------**/
	/**-----------------------------Update Scenes Buttons--------------------------------**/
	/**----------------------------------------------------------------------------------**/
//Update song details
	public void updateSongDetails() 
	{
	    Media media = GlobalMediaPlayer.getMedia();
	    if (media != null) 
	    {
	        Song song = GlobalMediaPlayer.getSong(songNumber);
	        
	        songLabel.setText(song.getTitle());
	        artistLabel.setText(song.getArtist());
	        albumLabel.setText(song.getAlbum());
	      
	        //Update time and bar
			songProgressBar.setStyle("-fx-accent: #397DFF;");
	        beginTimer();
			Image songimage = song.getAlbumArt();
			
			if (songimage != null) 
			{
	           songArt.setImage(songimage); // Update album art.
	        } 
			else 
			{
	            // Set default image if there's no album art.
	            songArt.setImage(new Image(getClass().getResourceAsStream("/main/icons/Logo.png")));
	        }
	        System.out.println("Testa");
	    }
	}

//Table Getters:
	public TableView<Song> getTableView() {
		return tableView;
	}
	public TableView<Song> getTableView2() {
		return tableView2;
	}
	public TableColumn<Song, String> getSongTable() {
		return songTable;
	}
	public TableColumn<Song, String> getArtistTable() {
		return artistTable;
	}
	public TableColumn<Song, String> getAlbumTable() {
		return albumTable;
	}
	public TableColumn<Song, String> getYearTable() {
		return yearTable;
	}
	public TableColumn<Song, String> getActionTC() {
		return actionTC;
	}
	public TableColumn<Song, String> getActionTC2() {
		return actionTC2;
	}
	public VBox getFreqPlayedSongs() {
		return freqPlayedSongs;
	}
	public FlowPane getPlaylists_container() {
		return playlists_container;
	}

	public Label getSelectedPL() {
		return selectedPL;
	}

	private void populatePlaylistButtons(SceneController sceneController) 
	{
		getPlaylists_container().getChildren().clear();

		if (playlists != null) //If playlists exist
		{
			for (String playlistName : playlists.keySet()) //for each playlist name
			{
				Button playlistButton = new Button(playlistName);//Create new button with playlist
	            playlistButton.setStyle("-fx-background-color:  #397DFF; -fx-text-fill: white;");
				playlistButton.setOnAction(event -> {
					// Selecting the playlist
					System.out.println("Selected playlist: " + playlistName);
					viewSongsOf(sceneController, playlists.get(playlistName));
					selectedPL.setText(playlistName);
				});
	            
				playlists_container.getChildren().add(playlistButton);// Add the newly created button to UI
			}
			// Automatically start the first playlist button if exists
			if(!playlists_container.getChildren().isEmpty()) 
			{
				Button firstButtonPlayList = (Button) playlists_container.getChildren().get(0);

				if(firstButtonPlayList != null) 
				{
					firstButtonPlayList.fire();
				}
			}

		}
	}
	
//Display songs within a playlist
	private void viewSongsOf(SceneController sceneController, List<Song> songs) 
	{
		if(!songs.isEmpty()) //If playlist isn't empty
		{
			playListData.setAll(songs);//set pldata to selected pl
			sceneController.getTableView2().setItems(playListData);
			sceneController.getTableView2().getSelectionModel().selectFirst();//Select first song of a playlist
			songNumber = getSongNumberOf(tableView2.getSelectionModel().getSelectedItem());//Assign it's index
			updateMediaPlayer();
			updateSongDetails();
			//GlobalMediaPlayer.pause();//Player always plays after update so pause
		} 
		else 
		{
			playlists.clear();
			sceneController.getTableView2().setItems(playListData);
			GlobalMediaPlayer.pause();
		}
	}

//Remove songs that no longer exist in playlist
	public void validatePlayLists() 
	{
		Iterator<Map.Entry<String, List<Song>>> iterator = playlists.entrySet().iterator();//to go through each pl in map
		while (iterator.hasNext()) 
		{
			Map.Entry<String, List<Song>> entry = iterator.next();
			List<Song> songs = entry.getValue();
			List<Song> validSongs = new ArrayList<>();

			// Iterate over songs in the playlist and check if they are valid
			for (Song song : songs) 
			{
				if (song.isValid()) //if Song is valid
				{
					validSongs.add(song);//Add to list of valid songs
				} 
				else 
				{
					GlobalMediaPlayer.removeSongFromPlayCount(song);//Remove song if it doesn't exist
				}
			}

			if (validSongs.isEmpty()) 
			{
				iterator.remove();//If no valid songs remove the playlist 
			} 
			else 
			{
				playlists.put(entry.getKey(), validSongs);// Update the playlist with only valid songs
			}
		}
	}

//Populating the frequently played songs list:
	private void populateFrequentlyPlayedSongs() 
	{
		System.out.println("populateFrequentlyPlayedSongs one");//TEST
		
		//GlobalMediaPlayer.loadSongs(songData);//load songs into GlobalMediaPlayer
		GlobalMediaPlayer.loadFrequentlyPlayedList();//load the playcount data
		
		List<Song> topPlayedList = GlobalMediaPlayer.getTopFrequentlyPlayedSongs(8);//Returns list of frequently played songs
		
		
		System.out.println("topPlayedList: ");//TEST
		System.out.println("freqPlayedSongs = " + freqPlayedSongs);//TEST
		
		// Iterate over the list and update the UI to display the songs.
		for (int i = 0; i < topPlayedList.size(); i++) 
		{//Bob the builder
			Song song = topPlayedList.get(i);

			HBox hBox = (HBox) freqPlayedSongs.getChildren().get(i);//Hbox index for the song
			hBox.setStyle("-fx-background-color: #397DFF;"); //same as colour as upload music button
			
			//Updates art,song title,artist & song duration within each hbox
			ImageView imageLabel = (ImageView) hBox.getChildren().get(0);
			imageLabel.setImage(song.getAlbumArt());

			Label titleLabel = (Label) hBox.getChildren().get(1);
			titleLabel.setStyle("-fx-text-fill: white;");
			titleLabel.setText(song.getTitle());

			Label artistLabel = (Label) hBox.getChildren().get(2);
			artistLabel.setStyle("-fx-text-fill: white;");
			artistLabel.setText(song.getArtist());

			Label durationLabel = (Label) hBox.getChildren().get(3);
			durationLabel.setStyle("-fx-text-fill: white;");
			durationLabel.setText(song.getDuration());


			//Playbutton function next to each song so they can be played
			ImageView playBtn = (ImageView) hBox.getChildren().get(4);
			int finalI = i;
			playBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					songNumber = getNumberOf(topPlayedList.get(finalI));
					updateMediaPlayer();//Plays song selected
					updateSongDetails();
				}
			});

		}

	}
	@FXML
	private Button settingsButton;
//
	@FXML
	private void showEditSongMetadataDialog() {
	    Song selectedSong = GlobalMediaPlayer.getSong(songNumber);
	    
	    if (selectedSong == null) {
	        // Handle case where no song is selected
	        System.out.println("No song selected.");
	        return;
	    }

	    Stage dialogStage = new Stage();
	    dialogStage.initModality(Modality.APPLICATION_MODAL);
	    dialogStage.setTitle("Edit Song Metadata");

	    GridPane grid = new GridPane();
	    grid.setHgap(10);
	    grid.setVgap(10);
	    grid.setPadding(new Insets(20, 150, 10, 10));

	    TextField titleField = new TextField(selectedSong.getTitle());
	    TextField artistField = new TextField(selectedSong.getArtist());
	    TextField albumField = new TextField(selectedSong.getAlbum());
	    TextField yearField = new TextField(selectedSong.getYear());

	    grid.add(new Label("Title:"), 0, 0);
	    grid.add(titleField, 3, 0);
	    grid.add(new Label("Artist:"), 0, 1);
	    grid.add(artistField, 3, 1);
	    grid.add(new Label("Album:"), 0, 2);
	    grid.add(albumField, 3, 2);
	    grid.add(new Label("Year:"), 0, 3);
	    grid.add(yearField, 3, 3);

	    Button doneButton = new Button("Done");
	    doneButton.setOnAction(event -> {
	        // Apply changes and close dialog
	        try {
				MP3TagWriter.setTitle(selectedSong, titleField.getText());
			} catch (NotSupportedException | IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.println("Song contains Obselete Frames, Cannot be Saved!");
			}
	        try {
				MP3TagWriter.setArtist(selectedSong, artistField.getText());
			} catch (NotSupportedException | IOException e) {
				// TODO Auto-generated catch block
				System.out.println("Song contains Obselete Frames, Cannot be Saved!");
			}
	        try {
				MP3TagWriter.setAlbum(selectedSong, albumField.getText());
			} catch (NotSupportedException | IOException e) {
				// TODO Auto-generated catch block
				System.out.println("Song contains Obselete Frames, Cannot be Saved!");
			}
	        try {
				MP3TagWriter.setYear(selectedSong, yearField.getText());
			} catch (NotSupportedException | IOException e) {
				// TODO Auto-generated catch block
				System.out.println("Song contains Obselete Frames, Cannot be Saved!");
			}
	        dialogStage.close();
	    });

	    grid.add(doneButton, 1, 4);

	    Scene dialogScene = new Scene(grid, 500, 250);
	    dialogStage.setScene(dialogScene);
	    dialogStage.showAndWait();
	}
}

