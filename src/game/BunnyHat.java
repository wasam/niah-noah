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
	
	// game modes
	public static boolean TWIN_JUMP_SPACEBAR = false;
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
	
	private Player player1, player2;
	public PlayerView view1, view2, view3;
	private RaceIndicator indicator;
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
	
	// which screen we are on right now?
	public static enum Screens {GAME, MENU_MAIN, MENU_SETUP, MENU_STORY, MENU_CREDITS, INTRO}
	private static Screens currentView;
	
	//buffer stuff
	private PGraphics buffer;
	
	
	public void setup()
	{	
		currentView = Screens.INTRO;
		
		inputState = new State();
		
		sndCtrl = new SoundControl(this);
		
		
		//size(WINDOWWIDTH, WINDOWHEIGHT);
		size(1024, 768);
		background(0);
		
		frameRate(2000);
		
		//setup buffers 
		//buffer = createGraphics(this.width, this.height, PConstants.JAVA2D);
		
		//setup statistics
		currentTimestamp = millis();
		deltaT = 0;
		lastFpsTime = 0;
		fps = 0;
		fpsAverage = 0;
		
		
		//setup & run sound input
		sndCtrl = new SoundControl(this);
		sndCtrl.addObserver(this);
		
		
		//setup sound output
		sndOut = new Stereophone("sounds", this);
		sndOut.printSounds();
		
		
		
		
		
		//attempt to get a full screen mode - not working - null pointer exception 		
		/*fs = new FullScreen(this);
		if (fs.available()) {
			fs.enter();
		}*/
				
	}

	public void draw()
	{
		/*PGraphics cb = buffer;
		cb.beginDraw();
		cb.background(255);*/
		
		lastTimestamp = currentTimestamp;
		currentTimestamp = millis();
		
		deltaT = currentTimestamp - lastTimestamp;
		if (deltaT==0) deltaT=10;
		
		switch (currentView) {
		case INTRO:
			drawIntroScreen();
			break;
		case MENU_MAIN:
			drawMenuMainScreen();
			break;
		case GAME:
			player1.update(inputState, deltaT);
			player2.update(inputState, deltaT);
			view1.update(LEFT, VIEW1Y, deltaT);
			view2.update(LEFT, (height-RACEINDICATORHEIGHT)/2+RACEINDICATORHEIGHT, deltaT);
			//view3.update(LEFT + width/2, VIEW2Y, deltaT);
			indicator.update(LEFT, (height-RACEINDICATORHEIGHT)/2, deltaT);
			break;
		}
		
		/*cb.endDraw();
		image(cb, 0, 0);*/
		
		// Print the fps
		if ((currentTimestamp - lastFpsTime) > 1000)
		{
			gameSeconds++;
			fpsAverage += fps;
			if (SHOW_FPS)
			{
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

	private void drawMenuMainScreen()
	{
		// TODO Auto-generated method stub
		background(255);
		fill(0);
		text("press 1 for level 1 and 2 for level 2 .) \n and q to quit this game..", width/2, height/2);
	}

	private void drawIntroScreen()
	{
		// TODO Auto-generated method stub
		background(255);
		fill(0);
		text("d(^_^)b amaaazing intro screen - press space to go on!", width/2, height/2);
	}

	public static void main(String args[])
	{
		PApplet.main(new String[]
		{ "--present", "BunnyHat" });
	}

	public void keyPressed()
	{
		switch (this.currentView) {
		case GAME:
			handleKeyGame();
			break;
		case MENU_MAIN:
			handleKeyMenuMain();
			break;
		case INTRO:
			handleKeyIntro();
			break;
		}
	}
	
	private void handleKeyGame() {
		if (TWIN_JUMP_SPACEBAR && (key == 'w' || key == 'i'))
		{
			//nothing for the moment
		}
		else if (TWIN_JUMP_SPACEBAR && key == ' ')
		{
			inputState.put('w', true);
			inputState.put('i', true);
		}
		else {
			inputState.put(key, true);
		}
		
		if (key == 'd')
		{
			inputState.put('a', false);
		}
		else if (key == 'a')
		{
			inputState.put('d', false);
		}
		else if (key == 'j')
		{
			inputState.put('l', false);
		}
		else if (key == 'l')
		{
			inputState.put('j', false);
		}
		else if (key == 'f')
		{
			SHOW_FPS = !SHOW_FPS;
		}
		else if (key == 'q') {
			gameMaster.stopGame();
			currentView = Screens.MENU_MAIN;
		} else if (key == 'n')
		{
			switcher.swapPlayer1();
		}
		else if (key == 'm')
		{
			switcher.resetPlayer1();
		}
		else if (key == '1')
		{
			switcher.setupDoors(1);
		}
		else if (key == '2') 
		{
			switcher.setupDoors(2);
		}
		else if (key == '3')
		{
			switcher.blowDoors();
		}
		
	}
	
	private void handleKeyMenuMain() {
		switch (key) {
			case '1':
				setupGame(1);
				currentView = Screens.GAME;
				break;
			case '2':
				setupGame(2);
				currentView = Screens.GAME;
				break;
			case 'q':
				exit();
				break;
				
		}
	}
	
	private void handleKeyIntro() {
		if (key == ' ') {
			currentView = Screens.MENU_MAIN;
		}
	}

	public void keyReleased()
	{
		switch (this.currentView) {
		case GAME:
			if (TWIN_JUMP_SPACEBAR && (key == 'w' || key == 'i')) {
				//nothing for the moment
			} else if (TWIN_JUMP_SPACEBAR && key == ' ') {
				inputState.put('w', false);
				inputState.put('i', false);
			} else {
				inputState.put(key, false);
			}
			break;
		case MENU_MAIN:
			break;
		}
	}
	
	private void setupGame(int level) {
		//setup game master
		gameMaster = new GameMaster(this);
		
		view1 = new PlayerView(width, (height - RACEINDICATORHEIGHT)/2, this, 1, 
				(String)SETTINGS.getValue("levels/level"+level+"/good"), gameMaster);
		view2 = new PlayerView(width, (height - RACEINDICATORHEIGHT)/2, this, 2,
				(String)SETTINGS.getValue("levels/level"+level+"/bad"), gameMaster);
		/*view3 = new PlayerView(width/2, PLAYERVIEWHEIGHT, this, 2,
				(String)SETTINGS.getValue("levels/level"+level+"/bad"), gameMaster);*/
		player1 = new Player(this, 1, view1.getLevel());
		player2 = new Player(this, 2, view2.getLevel());
		player1.setTwin(player2);
		player2.setTwin(player1);
		view1.setOwnPlayer(player1);
		view2.setOwnPlayer(player2);
		//view3.setOwnPlayer(player2);
		view1.setOtherPlayerView(view2);
		view2.setOtherPlayerView(view1);
		gameMaster.setTwins(player1, player2);
		
		
		indicator = new RaceIndicator(width, RACEINDICATORHEIGHT, this);

		sndCtrl.startListening();


		// setup our special workers
		switcher = new AmazingSwitchWitch(view1, view2, this);
		switcher.wakeHer();

		// setup communication
		gameMaster.addObserver(switcher); // listen for level switch message
		player1.addObserver(view1);
		player2.addObserver(view2);
		player1.addObserver(gameMaster);
		player2.addObserver(gameMaster);
		
		//switcher.addObserver(view1);
		//switcher.addObserver(view2);

		//setup and run game master
		gameMaster.startGame();
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
