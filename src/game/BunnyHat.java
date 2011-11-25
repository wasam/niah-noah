package game;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

import game.gui.AmazingSwitchWitch;
import game.gui.PlayerView;
import game.gui.RaceIndicator;
import game.level.Level;
import game.master.GameMaster;
import game.sound.Stereophone;
import game.control.SoundControl;
import processing.core.*;
import fullscreen.*;

@SuppressWarnings("serial")
public class BunnyHat extends PApplet implements Observer
{
	
	
	public static Settings SETTINGS = new Settings();
	
	boolean SHOW_FPS = SETTINGS.getValue("debug/fps");
	int FPS_AVERAGE_SAMPLE_SIZE = 10; // number of last measurements to take into account
	
	public static boolean TWIN_JUMP = false;
	
	public static int TILEDIMENSION = SETTINGS.getValue("gui/tiledimension");
	
	private static int RACEINDICATORHEIGHT = 2 * TILEDIMENSION;
	public static int PLAYERVIEWTILEHEIGHT = SETTINGS.getValue("gui/playerviewtileheight");
	public static int PLAYERVIEWTILEWIDTH = SETTINGS.getValue("gui/playerviewtilewidth");
	public static int PLAYERVIEWHEIGHT = PLAYERVIEWTILEHEIGHT * TILEDIMENSION;

	private static int VIEW1Y = 0;
	private static int RACEINDICATORY = PLAYERVIEWHEIGHT;
	private static int VIEW2Y = RACEINDICATORY + RACEINDICATORHEIGHT;
	
	private static int LEFT = 0;
	
	private static int WINDOWHEIGHT = RACEINDICATORHEIGHT + 2 * PLAYERVIEWHEIGHT;
	private static int WINDOWWIDTH = PLAYERVIEWTILEWIDTH * TILEDIMENSION;
	
	public PlayerView view1;
	public PlayerView view2;
	public RaceIndicator indicator;
	private SoundControl sndCtrl;
	private Stereophone sndOut;
	private GameMaster gameMaster;
	private AmazingSwitchWitch switcher;

	private State inputState;
	
	private int lastTimestamp;
	private int currentTimestamp;
	private int deltaT;
	
	// statistics
	private int lastFpsTime;
	private int fps;
	private int gameSeconds;
	private double fpsAverage;
	
	// not working FullScreen stuff
	private FullScreen fs;
	
	public void setup()
	{	
		inputState = new State();
		view1 = new PlayerView(WINDOWWIDTH, PLAYERVIEWHEIGHT, this, 1, 
				(String)SETTINGS.getValue("levels/level1/good"));
		view2 = new PlayerView(WINDOWWIDTH, PLAYERVIEWHEIGHT, this, 2,
				(String)SETTINGS.getValue("levels/level1/bad"));
		indicator = new RaceIndicator(WINDOWWIDTH, RACEINDICATORHEIGHT, this);
		sndCtrl = new SoundControl(this);
		
		
		size(WINDOWWIDTH, WINDOWHEIGHT);
		background(0);
		
		frameRate(2000);
		
		
		
		currentTimestamp = millis();
		deltaT = 0;
		lastFpsTime = 0;
		fps = 0;
		fpsAverage = 0;
		
		
		//setup & run sound input
		sndCtrl = new SoundControl(this);
		sndCtrl.addObserver(this);
		sndCtrl.startListening();
		
		//setup sound output
		sndOut = new Stereophone("sounds");
		sndOut.printSounds();
		
		//setup and run game master
		gameMaster = new GameMaster(this);
		gameMaster.startGame();
		
		// setup our special workers
		switcher = new AmazingSwitchWitch(view1, view2, this);
		switcher.wakeHer();
		
		// setup communication
		gameMaster.addObserver(switcher); // listen for level switch message
		//switcher.addObserver(view1);
		//switcher.addObserver(view2);
		
		//attempt to get a full screen mode - not working - null pointer exception 		
		/*fs = new FullScreen(this);
		if (fs.available()) {
			fs.enter();
		}*/
				
	}

	public void draw()
	{
		
		lastTimestamp = currentTimestamp;
		currentTimestamp = millis();
		
		deltaT = currentTimestamp - lastTimestamp;
		if (deltaT==0) deltaT=10;
		
		
		view1.update(inputState, LEFT, VIEW1Y, deltaT);
		view2.update(inputState, LEFT, VIEW2Y, deltaT);
		indicator.update(inputState, LEFT, RACEINDICATORY, deltaT);
		
		
		
		// Print the fps
		if ((currentTimestamp - lastFpsTime) > 1000)
		{
			gameSeconds++;
			if (SHOW_FPS)
			{
				fpsAverage += fps;
				//fpsAverage = (fpsAverage * (FPS_AVERAGE_SAMPLE_SIZE-1) + fps) / FPS_AVERAGE_SAMPLE_SIZE;
				System.out.println("FPS: " + fps + " (Average: "+fpsAverage/gameSeconds+")  " + gameSeconds +"sec.");
			}
			
			lastFpsTime = currentTimestamp;
			fps = 0;
		}
		else
		{
			fps++;
		}
	}

	public static void main(String args[])
	{
		PApplet.main(new String[]
		{ "--present", "BunnyHat" });
	}

	public void keyPressed()
	{
		if (TWIN_JUMP && (key == 'w' || key == 'i')) {
			//nothing for the moment
		} else if (TWIN_JUMP && key == ' ') {
			inputState.put('w', true);
			inputState.put('i', true);
		} else {
			inputState.put(key, true);
		}
		
		if (key == 'd')
		{
			inputState.put('a', false);
		}else if (key == 'a')
		{
			inputState.put('d', false);
		} else if (key == 'j')
		{
			inputState.put('l', false);
		} else if (key == 'l')
		{
			inputState.put('j', false);
		} else if (key == 'f')
		{
			SHOW_FPS = !SHOW_FPS;
		} else if (key == 'q') {
			exit();
		}
	}

	public void keyReleased()
	{
		if (TWIN_JUMP && (key == 'w' || key == 'i')) {
			//nothing for the moment
		} else if (TWIN_JUMP && key == ' ') {
			inputState.put('w', false);
			inputState.put('i', false);
		} else {
			inputState.put(key, false);
		}
	}

	@Override
	public void update(Observable o, Object arg)
	{
		if (arg instanceof HashMap) {
			HashMap map = (HashMap)arg;
			String detector = (String)map.get("detector");
			String pattern = (String)map.get("pattern");
			if (detector.contentEquals("HF")) {
				inputState.put('d', (pattern.contentEquals("Straight Solid")));
			} else {
				inputState.put('l', (pattern.contentEquals("Straight Solid")));
			}
		}
		
	}
}
