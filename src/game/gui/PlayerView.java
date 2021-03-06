package game.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

import game.BunnyHat;
import game.BunnyHat.LevelSource;
import game.CollisionBox.Effects;
import game.Door;
import game.Player;
import game.State;
import game.elements.BadSheep;
import game.elements.BubbleGunGum;
import game.elements.BubbleGunGum.BallColor;
import game.elements.GameElement;
import game.elements.GoodSheep;
import game.level.Level;
import game.level.Level.DreamStyle;
import game.level.Level.MetaTiles;
import game.master.GameMaster;
import game.sound.Stereophone;
import processing.core.*;

public class PlayerView extends Updateable implements Observer
{
	private enum Horizontal { LEFT, RIGHT, MIDDLE }
	private enum Vertical { TOP, BOTTOM, CENTER }
	
	private int width; public int getWidth() {return width;}
	private int halfwidth;
	private int height;
	private int halfheight;
	private PApplet processing;
	
	private int viewNumber;
	private int xCoordCamera;
	private int xCoordCameraMiddle;
	private int yCoordCamera;
	private int yCoordCameraMiddle;
	
	private int playerPosition;
	
	private Player ownPlayer;
	private Player otherPlayer;
	
	private PlayerView otherPlayerView;
	private boolean ownPlayerWon = false;
	private boolean gameOver = false;

	protected boolean drawOwnPlayer = true;
	protected boolean drawOtherPlayer = false;
	
	private PGraphics buffer; 
	private PGraphics colorLayer; 
	protected int colorLayerVisibility = 0;
	private int[] colorLayerColor;
	
	protected double physicsTimeFactor = 1.0;
	
	protected double xbackup;
	protected double ybackup;
	
	protected int cameraOffsetX = 0;
	protected int cameraOffsetY = 0;
	protected double cameraOffsetFactor = 0.0;
	protected int cameraCurrentY = 0;
	
	private int levelLength;
	
	private int timeSlowingFactor;
	
	private Door ownDoor;
	private boolean doorEnabled = false;
	private boolean drawOwnDoor = false;
	private boolean shouldShowDoor = false;
	private boolean shouldBlowDoor = false;
	private boolean shouldBeCloseBy = false;
	private boolean shouldPrepareDoor = false;
	private boolean shouldPrepareSwitch = false;
	private boolean shouldExecuteSwitch = false;
	private boolean shouldFinishSwitch = false;
	private boolean shouldTunnelTwin = false;
	private boolean shouldUntunnelTwin = false;
	
	private Level level;
	private Level goodDream, badDream;
	/*private ArrayList<PImage> goodBackgroundImages, badBackgroundImages;
	private ArrayList<Double> goodBGIFactorX, goodBGIFactorY, badBGIFactorX, badBGIFactorY;
	private PImage goodForeground, badForeground;
	private double goodFGIFactorX, goodFGIFactorY, badFGIFactorX, badFGIFactorY;*/
	private LevelSource lvlSrc;
	
	// dream switch data
	private boolean switchHappening = false;
	
	
	//dream switch interaction methods
	public Level getLevel() {return level;}
	
	
	protected void initSwitchPrepare() {this.shouldPrepareSwitch = true;}
	private void switchPrepare() { 
		switchHappening = true;
		if (level.dream == DreamStyle.GOOD) {
			colorLayerColor[0] = 100;
			colorLayerColor[1] = 100;
			colorLayerColor[2] = 100;
		} else {
			colorLayerColor[0] = 135;
			colorLayerColor[1] = 206;
			colorLayerColor[2] = 250;
		}
		// getting the player y-Offset (distance above ground)
	}
	
	protected void initSwitchExecute() {this.shouldExecuteSwitch = true;}
	private void switchExecute() {
		level.removeElement(ownPlayer); // take player out
		level = (level == goodDream ? badDream : goodDream); // switch level
		level.addElement(ownPlayer); // put player in
		ownPlayer.removeCollisionGroundPath(); // in case he was standing on something - usually the case
		ownPlayer.removePushable(); // in case he was pushing something
	}
	
	protected void initSwitchFinish() {this.shouldFinishSwitch = true;}
	private void switchFinish() {
		switchHappening = false;
		level.removeAllPushBoxConstraints();
	}
	
	
	protected void initShowDoor() {
		this.initShowDoor(false);
	}
	protected void initShowDoor(boolean closeBy) {
		this.shouldBeCloseBy = closeBy;
		this.shouldShowDoor = true;
	}
	
	protected void setDoorPosition(Player p) {
		p.setPos(ownDoor.x(), ownDoor.y());
	}
	
	protected void initBlowDoor() {
		this.shouldBlowDoor = true;
	}
	
	
	// show the next best door
	private void prepareDoor() {
		boolean closeBy = this.shouldBeCloseBy;
		if (this.prepareDoor(closeBy)) {
			
			this.ownDoor.accessible = this.ownPlayer.x() < this.otherPlayer.x();
			
			this.setChanged();
			HashMap map = new HashMap();
			map.put("foundDoor", this.viewNumber);
			this.notifyObservers(map);
		}
	}
	private boolean prepareDoor(boolean closeBy) {
		
		//System.out.println("show them the doors - maxX:"+getMaximumTileX()+" minX:"+getMinimumTileX());
		// Counting y from down towards the sky
		int minimumTileX = (int)ownPlayer.x() + 1;
		int maximumTileX = getMaximumTileX() - 5 + width/2/BunnyHat.TILEDIMENSION;
		int tileSpanX = maximumTileX - minimumTileX;
		
		int minimumTileY = getMinimumTileY();
		int maximumTileY = getMaximumTileY();
		
		boolean doorFound = false;
		int doorX, doorY ;
		doorX = doorY = 0;
		double doorDistance = -1;
		for (int x = minimumTileX; x <= maximumTileX; x++)
		{
			for (int y = minimumTileY; y <= maximumTileY; y++)
			{		
				if (this.level.getMetaDataAt(x, y) == Level.MetaTiles.DOORSPAWNPOINT.index()) {
					doorFound = true;
					double pxDist = ownPlayer.x() - x;
					double pyDist = ownPlayer.y() - y;
					double distanceToPlayer = Math.sqrt(Math.pow(pxDist, 2) + Math.pow(pyDist, 2));
					boolean useNewValues = false;
					if (doorDistance == -1) {
						useNewValues = true;
					} else if (closeBy) {
						if (distanceToPlayer < doorDistance) {
							useNewValues = true;
						}
					} else {
						if (distanceToPlayer > doorDistance) {
							useNewValues = true;
						}
					}
					
					if (useNewValues) {
						doorDistance = distanceToPlayer;
						doorX = x;
						doorY = y;
					}
				}
			}
		}
		if (doorFound) {
			this.ownDoor.updatePosition(doorX, doorY);
			return true;
		}
		return false;
	}
	
	
	private void showDoor() {
		this.level.setDoorAt((int)this.ownDoor.x(), (int)this.ownDoor.y(), this.ownDoor);
		this.ownDoor.showDoor();
		this.drawOwnDoor = true;
		this.doorEnabled = true;
		Stereophone.playSound("305", "showdoors", 1000);
	}
	
	//remove all doors
	private void blowDoor() {
		this.level.removeDoorAt((int)this.ownDoor.x(), (int)this.ownDoor.y());
		this.ownDoor.blowDoor();
		this.doorEnabled = false;
		this.untunnelTwin();
		Stereophone.playSound("304", "blowdoors", 1000);
		this.ownPlayer.cannotMoveLeft = false;
		this.ownPlayer.cannotMoveRight = false;
		this.ownPlayer.tookTheDoor = false;
		//this.drawOwnDoor = false;
	}
	
	//tunnel stuff
	private void tunnelTwin() {
		ownPlayer.tookTheDoor = true;
		drawOwnPlayer = false;
		
		level.removeElement(ownPlayer);
		otherPlayerView.getLevel().addElement(ownPlayer);
		ownPlayer.setLevel(otherPlayerView.getLevel());
		
		xbackup = getPlayer().x();
		ybackup = getPlayer().y();

		otherPlayerView.setDoorPosition(ownPlayer);
		ownPlayer.removeCollisionGroundPath();
		
		ownPlayer.giveWeapon();
		
		otherPlayerView.drawOtherPlayer = true;
	}
	
	private void untunnelTwin() {
		if (!ownPlayer.tookTheDoor) return;
		
		otherPlayerView.drawOtherPlayer = false;
		
		ownPlayer.setPos(xbackup, ybackup);
		
		
		otherPlayerView.getLevel().removeElement(ownPlayer);
		level.addElement(ownPlayer);
		ownPlayer.setLevel(level);
		ownPlayer.resetBouncePartner();
		
		ownPlayer.takeWeapon();
		drawOwnPlayer = true;
		//ownPlayer.tookTheDoor = false;
		
	}
	
	protected Player getPlayer() {
		return this.ownPlayer;
	}
	
	public void setOtherPlayerView(PlayerView pv) {
		this.otherPlayerView = pv;
		//this.ownPlayer.addObserver(pv);
		this.otherPlayer = pv.getPlayer();
	}
	
	public void setOwnPlayer(Player p) {
		this.ownPlayer = p;
		//level.setPlayer(p);
		level.addElement(p);
	}
	
	public PlayerView(int width, int height, PApplet applet, int viewNumber, 
			Level goodDream, Level badDream, GameMaster gameMaster, DreamStyle style,
			LevelSource lvlSrc)
	{	
		this.buffer = applet.createGraphics(width, height, PConstants.JAVA2D);
		this.colorLayerColor = new int[3];
		colorLayerColor[0] = BunnyHat.SETTINGS.getValue("gui/colors/tintr");
		colorLayerColor[1] = BunnyHat.SETTINGS.getValue("gui/colors/tintg");
		colorLayerColor[2] = BunnyHat.SETTINGS.getValue("gui/colors/tintb");
		
		this.ownDoor = new Door(applet, 0,0,2,3);
		this.ownDoor.setCollisionEffect(Effects.NONE);
		
		
		//this.colorLayer.background(colorLayerColor, colorLayerVisibility);
		
		this.width = width;
		this.halfwidth = this.width / 2;
		this.height = height;
		this.halfheight = this.height / 2;
		
		this.processing = applet;
		//this.gameMaster = gameMaster;
		
		this.viewNumber = viewNumber;
		
		this.goodDream = goodDream;
		this.badDream = badDream;
		this.level = (style == DreamStyle.GOOD ? goodDream : badDream);
		this.levelLength = level.levelWidth * BunnyHat.TILEDIMENSION;
		
		this.lvlSrc = lvlSrc;
		
		//this.goodBackgroundImages = goodBackgrounds;
		//this.badBackgroundImages = badBackgrounds;
		
		//this.goodForeground = goodForeground;
		//this.badForeground = badForeground;
		
		// calculate those factors
		for (int i = 0; i < lvlSrc.goodBackgroundImages.size(); i++) {
			double xDistanceImage = lvlSrc.goodBackgroundImages.get(i).width-width;
			double xDistanceLevel = level.levelWidth * BunnyHat.TILEDIMENSION - width;
			double yDistanceImage = lvlSrc.goodBackgroundImages.get(i).height - height;
			double yDistanceLevel = level.levelHeight * BunnyHat.TILEDIMENSION - height;
			lvlSrc.goodBGIFactorX.add(xDistanceImage*1.0/xDistanceLevel);
			lvlSrc.goodBGIFactorY.add(yDistanceImage*1.0/yDistanceLevel);
		}
		for (int i = 0; i < lvlSrc.badBackgroundImages.size(); i++) {
			double xDistanceImage = lvlSrc.badBackgroundImages.get(i).width-width;
			double xDistanceLevel = level.levelWidth * BunnyHat.TILEDIMENSION - width;
			double yDistanceImage = lvlSrc.badBackgroundImages.get(i).height - height;
			double yDistanceLevel = level.levelHeight * BunnyHat.TILEDIMENSION - height;
			lvlSrc.badBGIFactorX.add(xDistanceImage*1.0/xDistanceLevel);
			lvlSrc.badBGIFactorY.add(yDistanceImage*1.0/yDistanceLevel);
		}
		if (lvlSrc.badForeground != null) {
			double xDistanceImage = lvlSrc.badForeground.width-width;
			double xDistanceLevel = level.levelWidth * BunnyHat.TILEDIMENSION - width;
			double yDistanceImage = lvlSrc.badForeground.height - height;
			double yDistanceLevel = level.levelHeight * BunnyHat.TILEDIMENSION - height;
			lvlSrc.badFGIFactorX=(xDistanceImage*1.0/xDistanceLevel);
			lvlSrc.badFGIFactorY=(yDistanceImage*1.0/yDistanceLevel);
		}
		if (lvlSrc.goodForeground != null) {
			double xDistanceImage = lvlSrc.goodForeground.width-width;
			double xDistanceLevel = level.levelWidth * BunnyHat.TILEDIMENSION - width;
			double yDistanceImage = lvlSrc.goodForeground.height - height;
			double yDistanceLevel = level.levelHeight * BunnyHat.TILEDIMENSION - height;
			lvlSrc.goodFGIFactorX=(xDistanceImage*1.0/xDistanceLevel);
			lvlSrc.goodFGIFactorY=(yDistanceImage*1.0/yDistanceLevel);
		}
		
		/*this.ownPlayer = new Player(processing, viewNumber, this.level);
		this.ownPlayer.addObserver(this);
		this.ownPlayer.addObserver(gameMaster);*/
		
		this.playerPosition = 0;
		
		this.xCoordCamera = 0;
		this.xCoordCameraMiddle = xCoordCamera + halfwidth;
		
		this.timeSlowingFactor = 1; // normal speed
	}
	
	
	
	public void update(int xpos, int ypos, int deltaT)
	{	
		if (this.shouldPrepareDoor) {this.prepareDoor(); this.shouldPrepareDoor = false;}
		if (this.shouldShowDoor) {this.showDoor(); this.shouldShowDoor = false;}
		if (this.shouldBlowDoor) {this.blowDoor(); this.shouldBlowDoor = false;}
		if (this.shouldPrepareSwitch) {this.switchPrepare(); this.shouldPrepareSwitch = false;}
		if (this.shouldExecuteSwitch) {this.switchExecute(); this.shouldExecuteSwitch = false;}
		if (this.shouldFinishSwitch) {this.switchFinish(); this.shouldFinishSwitch = false;}
		if (this.shouldTunnelTwin) {this.tunnelTwin(); this.shouldTunnelTwin = false;}
		if (this.shouldUntunnelTwin ) {this.untunnelTwin(); this.shouldUntunnelTwin = false;}
		
		//if (this.doorEnabled) {
		//	this.prepareDoor();
		//}
		
		buffer.beginDraw();
		if (level.dream == DreamStyle.GOOD){
			buffer.background(135,206,250);
		} else {
			buffer.background(100);
		}
		
		
		

		if (ownPlayer == null)
		{
			return;
		}
		
		//ownPlayer.isMovingSideways = false;
		
		
		
		// Draw the player
		int pxpos, pypos;
		
		if (drawOwnPlayer)
		{
			pxpos = (int) (ownPlayer.x() * BunnyHat.TILEDIMENSION);
			pypos = (int) (ownPlayer.y() * BunnyHat.TILEDIMENSION);
		}
		else
		{
			pxpos = (int) (xbackup * BunnyHat.TILEDIMENSION);
			pypos = (int) (ybackup * BunnyHat.TILEDIMENSION);
		}

		int drawpxpos;
		int drawpypos;
		
		if (pxpos < 0)
		{
			pxpos = 0;
			ownPlayer.cannotMoveLeft = true;
		}
		
		if (pxpos > level.levelWidth * BunnyHat.TILEDIMENSION)
		{
			pxpos = level.levelWidth * BunnyHat.TILEDIMENSION;
			ownPlayer.cannotMoveRight = true;
		}

		if (pypos < 0)
		{
			pypos = 0;
		}
		
		int playerHeight = ownPlayer.getCurrentTexture().height; 
		if (pypos + playerHeight > level.levelHeight * BunnyHat.TILEDIMENSION)
		{
			pypos = level.levelHeight * BunnyHat.TILEDIMENSION - playerHeight;
		}
		
		this.playerPosition = pxpos;
		pxpos += (int)(ownPlayer.collisionBoxWidth() * BunnyHat.TILEDIMENSION / 2);
		pypos += (int)(ownPlayer.collisionBoxHeight() * BunnyHat.TILEDIMENSION / 2);
		
		// calculate camera offset for door / other player
		int otherXPos, otherYPos;
		if (!drawOwnPlayer) {
		} else if (this.drawOtherPlayer) {
			//System.out.println("derandere!");
			this.cameraOffsetX = (int)(((otherPlayer.x() - ownPlayer.x())/2) * BunnyHat.TILEDIMENSION);
			this.cameraOffsetY = (int)(((otherPlayer.y() - ownPlayer.y())/2) * BunnyHat.TILEDIMENSION);
			if (cameraOffsetX > (width-ownPlayer.collisionBoxWidth()*3)/2) {
				if (otherPlayer.x() > ownPlayer.x()) {
					otherPlayer.cannotMoveRight = true;
					ownPlayer.cannotMoveLeft = true;
				} else {
					otherPlayer.cannotMoveLeft = true;
					ownPlayer.cannotMoveRight = true;
				}
			}
			if (Math.abs(cameraOffsetY) > (height-ownPlayer.collisionBoxHeight()*3)/2) {
				this.setChanged();
				this.notifyObservers("MAKEDOORSDISAPPEAR");
			}
		} else if (this.doorEnabled) {
			this.cameraOffsetX = (int)(((ownDoor.x() - ownPlayer.x())/2) * BunnyHat.TILEDIMENSION);
			this.cameraOffsetY = (int)(((ownDoor.y() - ownPlayer.y())/2) * BunnyHat.TILEDIMENSION);
			if (Math.abs(cameraOffsetX) > (width-ownPlayer.collisionBoxWidth()*3)/2) {
				if (cameraOffsetX > 0) {
					ownPlayer.cannotMoveLeft = true;
				} else {
					this.setChanged();
					this.notifyObservers("MAKEDOORSDISAPPEAR");
				}
			}
			if (Math.abs(cameraOffsetY) > (height-ownPlayer.collisionBoxHeight()*3)/2) {
				this.setChanged();
				this.notifyObservers("MAKEDOORSDISAPPEAR");
			}
		}
		
		//if (cameraOffsetFactor != 0) {
		xCoordCameraMiddle = pxpos;
		int xCoordCameraProposed = xCoordCameraMiddle - halfwidth + (int)(cameraOffsetX * cameraOffsetFactor);
		//} else {
		int minDistRight = width / 3;
		int minDistLeft = width / 3;
		if (pxpos > xCoordCamera + (width - minDistRight)) {
			xCoordCamera = pxpos - (width - minDistRight);
		} else if (pxpos < xCoordCamera + minDistLeft) {
			xCoordCamera = pxpos - minDistLeft;
		}
		//smooth transition for camera pans
		if (cameraOffsetFactor != 0) {
			int xCoordCameraProposedDiff = xCoordCameraProposed - xCoordCamera;
			xCoordCamera += xCoordCameraProposedDiff * cameraOffsetFactor;
		}
		
		//}
		
			
		yCoordCameraMiddle = pypos;
		int yCoordCameraProposed = yCoordCameraMiddle - halfheight + (int)(cameraOffsetY * cameraOffsetFactor);
		int minDistBottom = height/3;
		int minDistTop = (int)(ownPlayer.collisionBoxHeight() * BunnyHat.TILEDIMENSION);
		if (pypos > yCoordCamera + (height-minDistTop)) {
			yCoordCamera = pypos - (height-minDistTop);
		} else if (pypos < yCoordCamera + minDistBottom) {
			yCoordCamera = pypos - minDistBottom;
		}
		//smooth transition for camera pans
		if (cameraOffsetFactor != 0) {
			int yCoordCameraProposedDiff = yCoordCameraProposed - yCoordCamera;
			yCoordCamera += yCoordCameraProposedDiff * cameraOffsetFactor;
		}
		
		drawpxpos = halfwidth - (int)(cameraOffsetX * cameraOffsetFactor)
				- (int)(ownPlayer.collisionBoxWidth() * BunnyHat.TILEDIMENSION / 2);
		drawpypos = halfheight - (int)(cameraOffsetY * cameraOffsetFactor)
				- (int)(ownPlayer.collisionBoxHeight() * BunnyHat.TILEDIMENSION / 2);
		
		
		
		
		// nicer(? maybe.. well I hope so) camera y positioning
		/*if (this.cameraCurrentY == 0) this.cameraCurrentY = this.yCoordCamera;
		if (this.yCoordCamera > this.cameraCurrentY + height/4) {
			this.cameraCurrentY = this.yCoordCamera + height/4;
		} else if (this.yCoordCamera < this.cameraCurrentY - height/4) {
			this.cameraCurrentY = this.yCoordCamera - height/4;
		} else {
			this.yCoordCamera = this.cameraCurrentY;
		}*/
		
		
		if (xCoordCamera < 0)
		{
			int diff = -xCoordCamera;
			drawpxpos = drawpxpos - diff;
			xCoordCamera = 0;
			xCoordCameraMiddle = halfwidth;
		}
		
		int maxCameraPosX = level.levelWidth * BunnyHat.TILEDIMENSION - width;
		
		if (xCoordCamera > maxCameraPosX)
		{
			int diff = xCoordCamera - maxCameraPosX;
			drawpxpos = drawpxpos + diff;
			xCoordCamera = maxCameraPosX;
			xCoordCameraMiddle = xCoordCamera + halfwidth;
		}
		
		if (yCoordCamera < 0)
		{
			int diff = -yCoordCamera;
			drawpypos = drawpypos - diff;
			yCoordCamera = 0;
			yCoordCameraMiddle = halfheight;
		}
		
		int maxCameraPosY = level.levelHeight * BunnyHat.TILEDIMENSION - height;
		if (yCoordCamera > maxCameraPosY)
		{
			int diff = yCoordCamera - maxCameraPosY;
			drawpypos = (drawpypos + diff);
			yCoordCamera = maxCameraPosY;
			yCoordCameraMiddle = yCoordCamera + halfheight;
		}
		
		drawpypos = height - drawpypos;
		
		if (lvlSrc.badForeground == null && level.dream == DreamStyle.BAD
				|| lvlSrc.goodForeground == null && level.dream == DreamStyle.GOOD) {
			drawLevelGraphics(buffer, Level.Layer.BACKGROUND);
		}
		
		if (lvlSrc.badBackgroundImages.size() > 0 && level.dream == DreamStyle.BAD
				|| lvlSrc.goodBackgroundImages.size() > 0 && level.dream == DreamStyle.GOOD) {
			this.drawLevelBackground(buffer);
		}
		
		level.drawCreaturesAndObjects(xCoordCamera, yCoordCamera, 
				xCoordCamera + width, yCoordCamera + height, buffer);
		
		if (drawOwnPlayer)
		{
			//drawImage(ownPlayer.getCurrentTexture(), buffer, drawpxpos, drawpypos, Horizontal.LEFT, Vertical.BOTTOM);
		}
		
		/*if (drawOtherPlayer)
		{
			int opxpos = (int) (otherPlayer.x() * BunnyHat.TILEDIMENSION);
			int opypos = (int) (otherPlayer.y() * BunnyHat.TILEDIMENSION);
			
			if (opxpos < 0)
			{
				opxpos = 0;
				otherPlayer.cannotMoveLeft = true;
			}
			
			if (opxpos > level.levelWidth * BunnyHat.TILEDIMENSION)
			{
				opxpos = level.levelWidth * BunnyHat.TILEDIMENSION;
				otherPlayer.cannotMoveRight = true;
			}

			if (opypos < 0)
			{
				opypos = 0;
			}
			
			if (opypos + otherPlayer.getCurrentTexture().height > level.levelHeight * BunnyHat.TILEDIMENSION)
			{
				opypos = level.levelHeight * BunnyHat.TILEDIMENSION - otherPlayer.getCurrentTexture().height;
			}
			
			int xdiff = opxpos - pxpos;
			int ydiff = opypos - pypos;
			

			int drawopypos = drawpypos - ydiff - (int)(ownPlayer.collisionBoxHeight() * BunnyHat.TILEDIMENSION / 2);;
			int drawopxpos = drawpxpos + xdiff + (int)(ownPlayer.collisionBoxWidth() * BunnyHat.TILEDIMENSION / 2);;
			
			//drawImage(otherPlayer.getCurrentTexture(), buffer, drawopxpos, drawopypos, Horizontal.LEFT, Vertical.BOTTOM);	
		}*/
		
		if (lvlSrc.badForeground == null && level.dream == DreamStyle.BAD
				|| lvlSrc.goodForeground == null && level.dream == DreamStyle.GOOD) {
			drawLevelGraphics(buffer, Level.Layer.FOREGROUND);
		} 
		
		if (lvlSrc.badForeground != null && level.dream == DreamStyle.BAD
				|| lvlSrc.goodForeground != null && level.dream == DreamStyle.GOOD) {
			drawLevelForeground(buffer);
		}
		
		if (gameOver)
		{
			drawLevelEndScreen(buffer);	
		}
		// put some color on, babe!
		if (colorLayerVisibility > 0) {
			//colorLayer.beginDraw();
			//colorLayer.background(colorLayerColor, colorLayerVisibility);
			//colorLayer.endDraw();
			//buffer.image(colorLayer, 0, 0);
			buffer.noStroke();
			buffer.fill(colorLayerColor[0], colorLayerColor[1], colorLayerColor[2], colorLayerVisibility);
			buffer.rect(0, 0, width, height);
		}
		buffer.endDraw();
		processing.image(buffer, xpos, ypos);
	}
	
	private void drawLevelBackground(PGraphics graphics)
	{
		//System.out.println(yCoordCamera);
		//graphics.loadPixels();
		ArrayList<PImage> backgrounds = level.dream == DreamStyle.GOOD ? lvlSrc.goodBackgroundImages : lvlSrc.badBackgroundImages;
		for (int i = 0; i < backgrounds.size(); i++) {
			PImage image = backgrounds.get(i);
			drawLayer(graphics, image,
					(Double)(level.dream == DreamStyle.GOOD ? lvlSrc.goodBGIFactorX.get(i) : lvlSrc.badBGIFactorX.get(i)),
					(Double)(level.dream == DreamStyle.GOOD ? lvlSrc.goodBGIFactorY.get(i) : lvlSrc.badBGIFactorY.get(i)));
			//graphics.copy(image, x, y, width, height, 0, 0, width, height);
		}
		//graphics.updatePixels();
	}
	
	private void drawLevelForeground(PGraphics graphics) {
		PImage foreground = level.dream == DreamStyle.GOOD ? lvlSrc.goodForeground : lvlSrc.badForeground;
		drawLayer(graphics, foreground, 
				(Double)(level.dream == DreamStyle.GOOD ? lvlSrc.goodFGIFactorX : lvlSrc.badFGIFactorX),
				(Double)(level.dream == DreamStyle.GOOD ? lvlSrc.goodFGIFactorY : lvlSrc.badFGIFactorY));
	}
	
	
	
	private void drawLayer(PGraphics graphics, PImage image, double xFactor, double yFactor) {
		graphics.loadPixels();
		
		int x = (int)(xCoordCamera*xFactor);
		int y = (int)((level.levelHeight*BunnyHat.TILEDIMENSION - (yCoordCamera+height))*yFactor);
		//System.out.println(y);
		PImage imageArea = image.get(x, y, width, height);
		
		
		
		graphics.image(imageArea, 0, 0);
		

	}
	


	private void drawLevelEndScreen(PGraphics graphics) {
		// draw something cool!
		graphics.fill(0, 0, 0);
		graphics.text((ownPlayerWon?"WIN!!!":"LOSE :/\n\npress q -> main screen"), width / 2, height / 3, 200, 100);
	}
	
	private int getMinimumTileX() {
		int minimumTileX = xCoordCamera / BunnyHat.TILEDIMENSION;
		if (minimumTileX < 0)
		{
			minimumTileX = 0;
		}
		return minimumTileX;
	}
	private int getMaximumTileX() {
		int maximumTileX= (xCoordCamera + this.width) / BunnyHat.TILEDIMENSION;
		if (maximumTileX > level.levelWidth)
		{
			maximumTileX = level.levelWidth;
		}
		return maximumTileX;
	}
	private int getMinimumTileY() {
		int minimumTileY = yCoordCamera / BunnyHat.TILEDIMENSION;
		if (minimumTileY < 0)
		{
			minimumTileY = 0;
		}
		return minimumTileY;
	}
	private int getMaximumTileY() {
		int maximumTileY = ((yCoordCamera + this.height) / BunnyHat.TILEDIMENSION)+1;
		if (maximumTileY > level.levelHeight)
		{
			maximumTileY = level.levelHeight;
		}
		return maximumTileY;
	}
	
	
	
	// drawing the level graphics
	private void drawLevelGraphics(PGraphics graphics, Level.Layer layer)
	{	
		
		int minimumTileX = getMinimumTileX();
		int maximumTileX = getMaximumTileX();
		
		int minimumTileY = getMinimumTileY();
		int maximumTileY = getMaximumTileY();
		
		//graphics.image(level.bigPicture, -minimumTileX*BunnyHat.TILEDIMENSION- xCoordCamera, (maximumTileY-level.levelHeight)*BunnyHat.TILEDIMENSION+ yCoordCamera);
		//graphics.image(level.bigPicture, 0, 0);
		// Counting y from down towards the sky
		for (int reversey = minimumTileY; reversey <= maximumTileY; reversey++)
		{
			// Counting x from left towards right
			for (int x = minimumTileX; x <= maximumTileX; x++)
			{
				int y = level.levelHeight - reversey;
				PImage tile = level.getLevelImageAt(x, y, layer);
				
				int xcoord = x * BunnyHat.TILEDIMENSION - xCoordCamera;
				int ycoord = (height - reversey * BunnyHat.TILEDIMENSION)  + yCoordCamera;
				
				if (tile != null)
				{
					graphics.image(tile, xcoord, ycoord);
				}
				
				if (drawOwnDoor && ownDoor.x() == x && ownDoor.y() == reversey && layer == Level.Layer.BACKGROUND) {
					graphics.image(ownDoor.getCurrentTexture(), xcoord, ycoord - BunnyHat.TILEDIMENSION * 2);
				}
			}
		}
		
		/*
		// Debug below
		graphics.line(minimumTileCoord, 0, minimumTileCoord, graphics.height);
		graphics.line(maximumTileCoord, 0, maximumTileCoord, graphics.height);
		
		// Even more debug below
		int nextTile = minimumTile + 1;
		int nextTileCoord = nextTile * BunnyHat.TILEDIMENSION;
		int cameraNextTileCoord = nextTileCoord - xCoordCamera;
		
		int cameraSecondTileCoord = cameraNextTileCoord + BunnyHat.TILEDIMENSION;
		
		graphics.textFont(font);
		graphics.fill(0);
		graphics.text(minimumTile, 0, 20);

		graphics.line(cameraNextTileCoord, 0, cameraNextTileCoord, graphics.height);
		graphics.line(cameraSecondTileCoord, 0, cameraSecondTileCoord, graphics.height);
		*/
		
		//level.collisionDraw(graphics, 0, 0);
		//this.ownPlayer.collisionDraw(graphics, 0, 0);
		
	}
	
	public int getPlayerPosition()
	{
		return playerPosition;
	}
	
	public int getLevelLength()
	{
		return levelLength;
	}
	
	private void drawImage(PImage image, PGraphics graphics, int xpos, int ypos, Horizontal horizontal, Vertical vertical)
	{
		//graphics.beginDraw();
		
		int tx = xpos, ty = ypos;
		
		switch (horizontal)
		{
			case LEFT:
				tx = xpos;
				break;
			case RIGHT:
				tx = xpos - image.width;
				break;
			case MIDDLE:
				tx = xpos - image.width / 2;
				break;
		}
		
		switch (vertical)
		{
			case TOP:
				ty = ypos;
				break;
			case BOTTOM:
				ty = ypos - image.height;
				break;
			case CENTER:
				ty = ypos - image.height / 2;
				break;
		}
		
		graphics.image(image, tx, ty);
		graphics.fill(processing.color(255, 0, 0, 255));
		graphics.ellipse(xpos, ypos, 4, 4);
		
		//graphics.endDraw();
	}
	
	@Override
	public void update(Observable arg0, Object arg1)
	{
		if (arg1 instanceof HashMap) {
			HashMap map = (HashMap)arg1;
			if (map.containsKey("IFUCKINGWON") && !gameOver) {
				this.gameOver = true;
				if (((Integer)map.get("IFUCKINGWON"))==this.viewNumber) {
					this.ownPlayerWon = true;
				}
			} else if (map.containsKey("showDoors") && !gameOver) {
				if (((Integer)map.get("showDoors"))==this.viewNumber) {
					this.shouldBeCloseBy = true;
				} else {
					this.shouldBeCloseBy = false;
				}
				this.shouldShowDoor = true;
			} else if (map.containsKey("prepareDoors")) {
				if (((Integer)map.get("prepareDoors"))==this.viewNumber) {
					this.shouldBeCloseBy = true;
				} else {
					this.shouldBeCloseBy = false;
				}
				this.shouldPrepareDoor = true;
			} else if (map.containsKey("tunnelTwin")) {
				if (((Integer)map.get("tunnelTwin"))==this.viewNumber) {
					this.shouldTunnelTwin = true;
				}
			}
		} else if (arg1 instanceof String) {
			String msg = (String)arg1;
			if (msg.contentEquals("switchPrepare")) {
				this.shouldPrepareSwitch = true;
			} else if (msg.contentEquals("switchExecute")) {
				this.shouldExecuteSwitch = true;
			} else if (msg.contentEquals("switchFinish")) {
				this.shouldFinishSwitch = true;
			} else if (msg.contentEquals("blowDoors")) {
				this.shouldBeCloseBy = false;
				this.shouldBlowDoor = true;
			} else if (msg.contentEquals("untunnelTwin")) {
				this.shouldUntunnelTwin = true;
			}
		}
	}
	
	public void deleteAllTheStuff()
	{
		if (level != null)
		{
			level.deleteAllTheStuff();
			level = null;
		}
		
		if (badDream != null)
		{
			badDream.deleteAllTheStuff();
			badDream = null;
		}
		
		if (goodDream != null)
		{
			goodDream.deleteAllTheStuff();
			goodDream = null;
		}
	}
}
