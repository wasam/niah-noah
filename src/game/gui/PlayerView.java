package game.gui;

import game.BunnyHat;
import game.Player;
import game.State;
import processing.core.*;

public class PlayerView extends Updateable
{
	private enum Horizontal { LEFT, RIGHT, MIDDLE }
	private enum Vertical { TOP, BOTTOM, CENTER }
	
	private int width;
	private int height;
	private PApplet processing;
	
	private PGraphics buffers[];
	private int currentBuffer;
	private static int NUMBER_OF_BUFFERS = 1;
	
	private int viewNumber;

	private Player ownPlayer;
	private Player otherPlayer;
	
	public PlayerView(int width, int height, PApplet applet, int viewNumber)
	{	
		this.width = width;
		this.height = height;
		this.processing = applet;
		
		buffers = new PGraphics[NUMBER_OF_BUFFERS];
		currentBuffer = 0;
		
		for (int i = 0; i < NUMBER_OF_BUFFERS; i++)
		{
			buffers[i] = processing.createGraphics(width, height, PConstants.P2D);
		}
		
		this.viewNumber = viewNumber;
		
		this.ownPlayer = new Player(processing);
		this.otherPlayer = null;
	}
	
	private void handleInput(State state)
	{
		boolean jumpbutton = (viewNumber == 1) ?
				(state.containsKey('w') && state.get('w')) :
				(state.containsKey('i') && state.get('i'));
		
		boolean leftbutton = (viewNumber == 1) ?
				(state.containsKey('a') && state.get('a')) :
				(state.containsKey('j') && state.get('j'));
				
		boolean rightbutton = (viewNumber == 1) ?
				(state.containsKey('d') && state.get('d')) :
				(state.containsKey('l') && state.get('l'));
				
		boolean downbutton = (viewNumber == 1) ?
				(state.containsKey('s') && state.get('s')) :
				(state.containsKey('k') && state.get('k'));

		
		if (jumpbutton && ownPlayer != null)
		{
			ownPlayer.jump();
		}
		
		if (leftbutton && ownPlayer != null)
		{
			ownPlayer.isMovingSideways = true;
			ownPlayer.moveLeft();
		}
		else if (rightbutton && ownPlayer != null)
		{
			ownPlayer.isMovingSideways = true;
			ownPlayer.moveRight();
			
		}
		
		if (downbutton && ownPlayer != null)
		{
			// Use stuff
		}
	}
	
	public void update(State state, int xpos, int ypos, int deltaT)
	{	
		// Just a shortcut :)
		PGraphics cb = buffers[currentBuffer];
		
		ownPlayer.isMovingSideways = false;
		
		handleInput(state);
		
		// Update the players physics, etc.
		ownPlayer.update(deltaT);
		
		// Draw a white background
		cb.beginDraw();
		cb.background(255);
		cb.endDraw();
		
		// Draw the player
		int pxpos = (int) (ownPlayer.xpos * BunnyHat.TILEDIMENSION);
		int pypos = BunnyHat.PLAYERVIEWHEIGHT - (int) (ownPlayer.ypos * BunnyHat.TILEDIMENSION);
		
		drawImage(ownPlayer.getCurrentTexture(), cb, pxpos, pypos, Horizontal.MIDDLE, Vertical.BOTTOM);
		
		// Draw the image to the surface
		processing.image(cb, xpos, ypos);
		
		// Swap the buffers
		currentBuffer = (currentBuffer + 1) % NUMBER_OF_BUFFERS;
	}
	
	private void drawImage(PImage image, PGraphics graphics, int xpos, int ypos, Horizontal horizontal, Vertical vertical)
	{
		graphics.beginDraw();
		
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
		
		graphics.endDraw();
	}
}
