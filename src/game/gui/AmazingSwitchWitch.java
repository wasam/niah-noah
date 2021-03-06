package game.gui;

import game.BunnyHat;
import game.control.PatternDetector;
import game.level.Level;
import game.master.GameMaster;
import game.sound.Stereophone;
import game.util.Animator;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

import processing.core.PApplet;

public class AmazingSwitchWitch extends Observable implements Observer, Runnable
{
	
	//Thread control
	private Thread ourThread;
	private static boolean awake = false;
	
	private final static int SWITCH_DURATION = 1600;
	private final static int DOOR_CAMERA_MOVE_DURATION = 500;
	
	private static PlayerView playerView1, playerView2;
	
	private static boolean shouldSwitchDreams = false;
	private static boolean shouldSwitchPlayerBack = false;
	private static boolean shouldPrepareDoors = false;
	private static int doorPrepareAttempt = 0;
	private static boolean shouldSpawnDoors = false;
	private static int doorSpawnDarling = -1;
	
	private BunnyHat bunnyHat;
	
	private int tintColor;
	
	private int playerSwitched = -1;
	
	private boolean foundDoor1, foundDoor2;
	
	// animate camera position
	private class MoveCamera extends Animator {
		private PlayerView pv;
		
		
		public MoveCamera(int from, int to, int stepSize, int timeSpan, PlayerView pv)
		{
			super(from, to, stepSize, timeSpan);
			this.pv = pv;
			super.begin();
		}

		@Override
		protected void applyValue(int value)
		{
			pv.cameraOffsetX = value;
		}
		
	}
	
	// affect camera offset factor
	private class ChangeCameraOffsetFactor extends Animator {
		private PlayerView pv;
		private int maxValue;
		
		public ChangeCameraOffsetFactor(int from, int to, int stepSize, int timeSpan, PlayerView pv)
		{
			super(from, to, stepSize, timeSpan);
			this.pv = pv;
			super.begin();
			this.maxValue = from < to ? to : from;
		}
		
		@Override
		protected void applyValue(int value)
		{
			pv.cameraOffsetFactor = 1.0 * value / maxValue;
		}
	}
	
	//shake camera
	private class ShakeCameraX extends Animator {
		private PlayerView pv;

		public ShakeCameraX(int from, int to, int stepSize, int timeSpan, PlayerView pv)
		{
			super(from, to, stepSize, timeSpan, true);
			this.pv = pv;
			super.begin();
		}

		@Override
		protected void applyValue(int value)
		{
			this.pv.cameraOffsetX = value;
		}
		
	}
	
	private class ShakeCameraY extends Animator {
		private PlayerView pv;

		public ShakeCameraY(int from, int to, int stepSize, int timeSpan, PlayerView pv)
		{
			super(from, to, stepSize, timeSpan, true);
			this.pv = pv;
			super.begin();
		}

		@Override
		protected void applyValue(int value)
		{
			this.pv.cameraOffsetY = value;
		}
		
	}
	
	private class ShakeCamera extends Animator {
		Animator shakeX, shakeY;

		public ShakeCamera(PlayerView pv)
		{
			super(0, 10, 1000);
			shakeX = new ShakeCameraX(0, 55, 2, 100, pv);
			shakeY = new ShakeCameraY(0, 51, 2, 119, pv);
			super.begin();
		}

		@Override
		protected void applyValue(int value)
		{
			if (value == 10) {
				shakeX.finishLoop();
				shakeY.finishLoop();
			}
		}
		
	}
	
	// make a nice switch transition
	private class SwitchTransition extends Animator {
		private PlayerView pv1, pv2;
		private BunnyHat bunnyHat;

		public SwitchTransition(int from, int to, int stepSize, int timeSpan, 
				PlayerView pv1, PlayerView pv2, BunnyHat bunnyHat)
		{
			super(from, to, stepSize, timeSpan);
			this.pv1 = pv1; this.pv2 = pv2;
			this.bunnyHat = bunnyHat;
			super.begin();
		}

		@Override
		protected void applyValue(int value)
		{
			pv1.colorLayerVisibility = pv2.colorLayerVisibility = value;
			if (value == 0) {
				bunnyHat.physicsTimeFactor = bunnyHat.physicsTimeFactor = 1.0;
			} else if (value > 0 && value < 100) {
				bunnyHat.physicsTimeFactor = bunnyHat.physicsTimeFactor = 0.5;
			} else if (value >= 100 && value < 200) {
				bunnyHat.physicsTimeFactor = bunnyHat.physicsTimeFactor = 0.1;
			} else {
				bunnyHat.physicsTimeFactor = bunnyHat.physicsTimeFactor = 0.0;
			}
		}
		
	}
	
	
	public AmazingSwitchWitch(PlayerView pv1, PlayerView pv2, BunnyHat papplet) {
		playerView1 = pv1;
		playerView2 = pv2;
		bunnyHat = papplet;
		foundDoor1 = foundDoor2 = false;
	}
	
	
	
	private void tunnelTwin(int number) {
		this.setChanged();
		HashMap map = new HashMap();
		map.put("tunnelTwin", number);
		this.notifyObservers(map);
		
		playerSwitched = number;
		
		
		
	}
	
	private void untunnelTwin() {
		System.out.println("untunneling twin"+playerSwitched);
		this.setChanged();
		this.notifyObservers("untunnelTwin");
		
		playerSwitched = -1;
	}
	
	
	
	public void prepareDoors(int number) {
		this.setChanged();
		HashMap map = new HashMap();
		map.put("prepareDoors", number);
		this.notifyObservers(map);
	}
	
	public void spawnDoors(int number) {
		int cameraMoveDuration = 500;
		PlayerView pvDarling = number == 1 ? playerView1 : playerView2;
		PlayerView pvVictim = pvDarling == playerView1 ? playerView2 : playerView1;
		
		
		this.setChanged();
		HashMap map = new HashMap();
		map.put("showDoors", number);
		this.notifyObservers(map);
		
		new ChangeCameraOffsetFactor(0, 100, 2, cameraMoveDuration, pvVictim);
		System.out.println("doortime");
		
		
		
	}
	
	public void blowDoors() {
		PlayerView pvSource = (doorSpawnDarling == 1 ? playerView1 : playerView2);
		PlayerView pvTarget = (pvSource == playerView1 ? playerView2 : playerView1);
		
		
		new ChangeCameraOffsetFactor(100, 0, 2, DOOR_CAMERA_MOVE_DURATION, pvTarget);
		
		
		this.setChanged();
		this.notifyObservers("blowDoors");
	}

	public void switchDreams() {
		
		this.setChanged();
		this.notifyObservers("switchPrepare");
		

		new SwitchTransition(0, 255, 3, SWITCH_DURATION/2, playerView1, playerView2, bunnyHat);
		
		try
		{
			Thread.currentThread().sleep(SWITCH_DURATION/2);
		}
		catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		this.setChanged();
		this.notifyObservers("switchExecute");
		
		
		new SwitchTransition(255, 0, 2, SWITCH_DURATION/2, playerView1, playerView2, bunnyHat);
		try
		{
			Thread.currentThread().sleep(SWITCH_DURATION/2);
		}
		catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		this.hasChanged();
		this.notifyObservers("startAnimations");
		
		this.setChanged();
		this.notifyObservers("switchFinish");
		
	}
	
	public void wakeHer() {
		awake = true;
		ourThread = new Thread(this);
		ourThread.start();
	}
	
	public void makeHerSleep() {
		awake = false;
	}
	
	/**
	 * doing the actual switch
	 */
	@Override
	public void run()
	{
		
		while (awake) {
			try
			{
				if (shouldSwitchDreams) {switchDreams(); shouldSwitchDreams = false;}
				if (shouldSwitchPlayerBack) {
					this.blowDoors();
					Thread.currentThread().sleep(100);
					this.untunnelTwin();
					shouldSwitchPlayerBack = false;
				}
				if (shouldSpawnDoors) {
					if (foundDoor1 && foundDoor2) {
						spawnDoors(doorSpawnDarling); 
						shouldSpawnDoors = false;
						this.doorPrepareAttempt = 0;
						foundDoor1 = foundDoor2 = false;
					} else {
						if (this.doorPrepareAttempt > 13) {
							shouldSpawnDoors = false;
							this.doorPrepareAttempt = 0;
							foundDoor1 = foundDoor2 = false;
						} else {
							this.doorPrepareAttempt++;
							this.prepareDoors(doorSpawnDarling);
							foundDoor1 = foundDoor2 = false;
						}
					}
				}
				
				Thread.currentThread().sleep(200);
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}


	@Override
	public void update(Observable o, Object arg)
	{
		if (o instanceof GameMaster && arg instanceof GameMaster.MSG) {
			switch((GameMaster.MSG)arg) {
				case SWITCH_DREAMS:
					shouldSwitchDreams = true;
					break;
				case DOORS_SPAWN_STOP:
					shouldSwitchPlayerBack = true;
					break;
				case DOORS_SPAWN_START_PLAYER_1:
					doorSpawnDarling = 1;
					shouldSpawnDoors = true;
					break;
				case DOORS_SPAWN_START_PLAYER_2:
					doorSpawnDarling = 2;
					shouldSpawnDoors = true;
					break;
				case SWITCH_PLAYER_1:
					if (this.playerSwitched == -1) this.tunnelTwin(1);
					break;
				case SWITCH_PLAYER_2:
					if (this.playerSwitched == -1) this.tunnelTwin(2);
					break;
			}
		} else if (arg instanceof HashMap) {
			HashMap map = (HashMap)arg;
			if (map.containsKey("foundDoor")) {
				int number = (Integer)map.get("foundDoor");
				switch (number) {
					case 1:
						this.foundDoor1 = true;
						break;
					case 2:
						this.foundDoor2 = true;
						break;
				}
			} 
		}
	}

}
