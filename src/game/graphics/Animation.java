package game.graphics;

import java.util.Observable;
import java.util.Observer;

import game.BunnyHat;
import processing.core.PApplet;
import processing.core.PImage;
import util.BImage;

public class Animation
{	
	private PApplet processing;
	private PImage[] sprites;
	private int millisPerFrame;
	private int numberOfFrames;
	
	private boolean isRunning;
	private int startTime;
	
	private boolean holdAnimations;
	private int timeOnStop;
	private boolean randomFrameOffset;
	private int frameOffset= 0;
	
	private boolean loop;
	
	
	public Animation(PApplet p, String settingskey)
	{
		this.setupAnimation(p, settingskey);
	}
	
	private void setupAnimation(PApplet p, String settingskey) {
		int fps = BunnyHat.SETTINGS.getValue(settingskey + "/fps");
		
		this.processing = p;
		this.millisPerFrame = (int)(1000 / fps * BunnyHat.physicsTimeFactor);
		
		this.sprites = BunnyHat.ANIMATION_IMAGES.getSprites(settingskey);
		this.numberOfFrames = sprites.length;
		
		this.isRunning = false;
		this.holdAnimations = false;
		this.randomFrameOffset = false;
	}
	
	public boolean isRunning()
	{
		return isRunning;
	}
	
	public boolean isStopped()
	{
		return !isRunning;
	}
	
	public void start()
	{
		this.start(true, false);
	}
	
	public void start(boolean loop, boolean randomFrameOffset) {
		this.loop = loop;
		this.randomFrameOffset = randomFrameOffset;
		this.isRunning = true;
		this.startTime = processing.millis();
		this.frameOffset = (int)(Math.random()*this.sprites.length);
		//System.out.println(frameOffset);
	}
	
	public void stop()
	{
		this.isRunning = false;
	}
	
	public PImage getCurrentImage(int time)
	{
		if (!isRunning)
		{
			this.start();
			//System.out.println("I am not running!");
			//return null;
		}
		
		if (holdAnimations) time = this.timeOnStop;
		
		int diff = time - startTime;
		
		int frame = ((diff / millisPerFrame)+(this.randomFrameOffset?this.frameOffset:0)) % numberOfFrames;
		if (!loop) {
			
			frame = (diff / millisPerFrame);
			if (frame >= numberOfFrames)
			{
				frame = numberOfFrames -1;
				if (!this.randomFrameOffset) this.isRunning = false;
			}
		} 
		
		if (frame < 0) frame = 0;
		if (frame < numberOfFrames)
		{
			return sprites[frame];
		}
		else
		{
			System.out.println("Frame: " + frame + ". Number of frames: " + numberOfFrames);
			System.out.println("Diff: " + diff + ". Millis per frame: " + millisPerFrame);
			return null;
		}
	}

	
	public void holdAnimation()
	{
		this.timeOnStop = this.processing.millis();
		this.holdAnimations = true;
	}
	
	public void unholdAnimation() {
		this.holdAnimations = false; 
	}
	
	public void deleteAllTheStuff()
	{
		if (sprites != null)
		{
			for (PImage sprite : sprites)
			{
				sprite.delete();
			}
			sprites = null;
		}
		
		processing = null;
	}
}
