package game.gui;

import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

import game.BunnyHat;
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

	private GameMaster gameMaster;
	
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
	
	private int levelLength;
	
	private int timeSlowingFactor;
	
	private Door ownDoor;
	private boolean drawOwnDoor = false;
	private boolean shouldShowDoor = false;
	private boolean shouldBlowDoor = false;
	private boolean shouldBeCloseBy = false;
	
	
	private Level level;
	private Level goodDream, badDream;
	
	// dream switch data
	private boolean switchHappening = false;
	
	
	//dream switch interaction methods
	public Level getLevel() {return level;}
	
	
	
	public void switchPrepare() { 
		switchHappening = true;
		ownPlayer.holdAnimation();
		// getting the player y-Offset (distance above ground)
	}
	
	public void switchExecute() {
		level.removeElement(ownPlayer);
		level = (level == goodDream ? badDream : goodDream);
		level.addElement(ownPlayer);
		ownPlayer.removeCollisionGroundPath();
	}
	
	public void switchFinish() {
		ownPlayer.unholdAnimation();
		switchHappening = false;
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
	protected void showDoor(boolean closeBy) {
		//System.out.println("show them the doors - maxX:"+getMaximumTileX()+" minX:"+getMinimumTileX());
		// Counting y from down towards the sky
		int minimumTileX = (int)ownPlayer.x() + 1;
		int maximumTileX = getMaximumTileX();
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
				if (this.level.getMetaDataAt(x, y) == Level.MetaTiles.DoorSpawnPoint.index()) {
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
			//System.out.println(actualX+":"+y);
			this.level.setDoorAt(doorX, doorY, this.ownDoor);
			this.ownDoor.showDoor();
			this.drawOwnDoor = true;
		}
	}
	
	//remove all doors
	protected void blowDoor() {
		this.level.removeDoorAt((int)this.ownDoor.x(), (int)this.ownDoor.y());
		this.ownDoor.blowDoor();
		//this.drawOwnDoor = false;
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
			Level goodDream, Level badDream, GameMaster gameMaster, DreamStyle style)
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
		this.gameMaster = gameMaster;
		
		this.viewNumber = viewNumber;
		
		this.goodDream = goodDream;
		this.badDream = badDream;
		this.level = (style == DreamStyle.GOOD ? goodDream : badDream);
		this.levelLength = level.levelWidth * BunnyHat.TILEDIMENSION;
		
		
		/*this.ownPlayer = new Player(processing, viewNumber, this.level);
		this.ownPlayer.addObserver(this);
		this.ownPlayer.addObserver(gameMaster);*/
		
		this.playerPosition = 0;
		
		this.xCoordCamera = 0;
		this.xCoordCameraMiddle = xCoordCamera + halfwidth;
		
		this.timeSlowingFactor = 1; // normal speed
	}
	
	public void insertGameElements() {
		for (int x = 0; x < level.levelWidth; x++) {
			for (int y = 0; y < level.levelHeight; y++) {
				//if (this.level.getMetaDataAt(x, y) > 1) {System.out.println(this.level.getMetaDataAt(x, y));}
				if (this.level.getMetaDataAt(x, y) == Level.MetaTiles.Sheep.index() && level.dream == DreamStyle.GOOD) {
					GoodSheep goodSheep = new GoodSheep(x, y, 3, 3, processing);
					BadSheep badSheep = new BadSheep(x, y, 3, 3, processing, goodSheep);
					
					this.otherPlayerView.level.addElement(badSheep);
					this.level.addElement(goodSheep);
				}
			}
		}
	}
	
	
	
	public void update(int xpos, int ypos, int deltaT)
	{	
		if (this.shouldShowDoor) {this.showDoor(this.shouldBeCloseBy); this.shouldShowDoor = false;}
		if (this.shouldBlowDoor) {this.blowDoor(); this.shouldBlowDoor = false;}
		
		
		buffer.beginDraw();
		buffer.background(255);
		

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
		
		if (pypos + ownPlayer.getCurrentTexture().height > level.levelHeight * BunnyHat.TILEDIMENSION)
		{
			pypos = level.levelHeight * BunnyHat.TILEDIMENSION - ownPlayer.getCurrentTexture().height;
		}
		
		this.playerPosition = pxpos;
		
		// calculate camera offset for door / other player
		int otherXPos, otherYPos;
		if (this.drawOtherPlayer) {
			this.cameraOffsetX = (int)(((otherPlayer.x() - ownPlayer.x())/2) * BunnyHat.TILEDIMENSION);
			this.cameraOffsetY = (int)(((otherPlayer.y() - ownPlayer.y())/2) * BunnyHat.TILEDIMENSION);
		} else if (this.drawOwnDoor) {
			this.cameraOffsetX = (int)(((ownDoor.x() - ownPlayer.x())/2) * BunnyHat.TILEDIMENSION);
			this.cameraOffsetY = (int)(((ownDoor.y() - ownPlayer.y())/2) * BunnyHat.TILEDIMENSION);
		}
		
		// Place the player in the middle
		xCoordCameraMiddle = pxpos;
		xCoordCamera = xCoordCameraMiddle - halfwidth + (int)(cameraOffsetX * cameraOffsetFactor);
		yCoordCameraMiddle = pypos;
		yCoordCamera = yCoordCameraMiddle - halfheight + (int)(cameraOffsetY * cameraOffsetFactor);
		
		drawpxpos = halfwidth - (int)(cameraOffsetX * cameraOffsetFactor);
		drawpypos = halfheight - (int)(cameraOffsetY * cameraOffsetFactor);
		
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
		
		drawLevelGraphics(buffer, Level.Layer.BACKGROUND);
		
		level.drawCreaturesAndObjects(xCoordCamera, yCoordCamera, 
				xCoordCamera + width, yCoordCamera + height, buffer);
		
		if (drawOwnPlayer)
		{
			drawImage(ownPlayer.getCurrentTexture(), buffer, drawpxpos, drawpypos, Horizontal.LEFT, Vertical.BOTTOM);
		}
		
		if (drawOtherPlayer)
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
			

			int drawopypos = drawpypos - ydiff;
			int drawopxpos = drawpxpos + xdiff;
			
			drawImage(otherPlayer.getCurrentTexture(), buffer, drawopxpos, drawopypos, Horizontal.LEFT, Vertical.BOTTOM);	
		}
		
		drawLevelGraphics(buffer, Level.Layer.FOREGROUND);
		
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
	
	private void drawObjectsAndCreatures() {
		
	}
	
	// drawing the level graphics
	private void drawLevelGraphics(PGraphics graphics, Level.Layer layer)
	{	
		int minimumTileX = getMinimumTileX();
		int maximumTileX = getMaximumTileX();
		
		int minimumTileY = getMinimumTileY();
		int maximumTileY = getMaximumTileY();
		
		
		
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
			}
		}
	}
}
