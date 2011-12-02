package game;
import java.util.HashMap;


import game.elements.GameElement;
import game.elements.StandardCreature;
import game.graphics.Animation;
import game.gui.AmazingSwitchWitch;
import game.level.Level;
import game.sound.Stereophone;
import processing.core.*;
import util.BImage;
import util.BMath;
import util.BPoint;

public class Player extends CollisionBox
{
	private enum Facing { LEFT, RIGHT };
	private int myID;
	
	private PApplet processing;
	public double xpos, ypos, previous_xpos, previous_ypos;
	
	// jumping stuff
	private boolean isInAir;
	private boolean isJumping;
	private boolean didJump = false;
	
	public boolean isMovingSideways;

	public boolean cannotMoveLeft;
	public boolean cannotMoveRight;
	
	private double yAcceleration;
	public double ySpeed;
	public double xSpeed;
	
	private Facing facing;

	private boolean unarmed;
	
	private Animation walkAnimation;
	private Animation jumpAnimation;
	private Animation idleAnimation;
	private Animation walkAnimationGun;
	private Animation jumpAnimationGun;
	private Animation idleAnimationGun;
	
	private Level level;
	private Player myTwin; public void setTwin(Player twin) {myTwin=twin;}

	private static double GRAVITY = BunnyHat.SETTINGS.getValue("gameplay/gravity");
	private static double JUMPFORCE = BunnyHat.SETTINGS.getValue("gameplay/jumpforce");

	private static double MOVEACCEL_GROUND = BunnyHat.SETTINGS.getValue("gameplay/moveacceleration/ground");
	private static double MOVEACCEL_AIR = BunnyHat.SETTINGS.getValue("gameplay/moveacceleration/air");
	
	private static double BREAKACCEL_GROUND = BunnyHat.SETTINGS.getValue("gameplay/breakacceleration/ground");
	private static double BREAKACCEL_AIR = BunnyHat.SETTINGS.getValue("gameplay/breakacceleration/air");
	
	private static double MAXSPEED = BunnyHat.SETTINGS.getValue("gameplay/maxspeed");
	
	private static double CLAMPTOZERO = BunnyHat.SETTINGS.getValue("gameplay/clamptozero");
	
	private static int DELTAT_DIVIDENT = BunnyHat.SETTINGS.getValue("gameplay/deltatdivident");
	
	//sound stuff
	private boolean soundHitBottomPlayed = false;

	public void giveWeapon() {
		this.unarmed = false;
	}
	public void takeWeapon() {
		this.unarmed = true;
	}
	
	public void holdAnimation() {
		this.walkAnimation.holdAnimation();
		this.jumpAnimation.holdAnimation();
		this.idleAnimation.holdAnimation();
		this.walkAnimationGun.holdAnimation();
		this.jumpAnimationGun.holdAnimation();
		this.idleAnimationGun.holdAnimation();
	}
	
	public void unholdAnimation() {
		this.walkAnimation.unholdAnimation();
		this.jumpAnimation.unholdAnimation();
		this.idleAnimation.unholdAnimation();
		this.walkAnimationGun.unholdAnimation();
		this.jumpAnimationGun.unholdAnimation();
		this.idleAnimationGun.unholdAnimation();
	}
	
	public void setLevel(Level level) {
		this.level = level;
		super.setLevel(level);
	}
	
	public Player(PApplet applet, int playerNumber, Level level)
	{
		super(level.spawnX + 0.5, level.spawnY + 0.5, 2, 3);
		super.setLevel(level);
		super.setGameElement(this);
		
		this.myID = playerNumber;
		
		this.processing = applet;
		
		xSpeed = ySpeed = yAcceleration = 0.0;
		
		this.level = level;
		
		// determine our position
		this.xpos = level.spawnX + 5;// in case
		this.ypos = level.spawnY + 0.5; // there is no spawn point set
		for (int x = 0; x < level.levelWidth; x++) {
			for (int y = 0; y < level.levelHeight; y++) {
				if (level.getMetaDataAt(x, y) == Level.MetaTiles.SpawnPoint.index()) {
					this.xpos = x; // in case
					this.ypos = y; // there is a spawn point
					break;
				}
			}
		}
		
		
		System.out.println(this.xpos + ", " + this.ypos);
		
		isInAir = true;
		isJumping = true;
		isMovingSideways = false;
		cannotMoveLeft = false;

		unarmed = true;
		
		this.walkAnimation = new Animation(processing, "graphics/animations/player" + playerNumber + "run");
		this.idleAnimation = new Animation(processing, "graphics/animations/player" + playerNumber + "idle");
		this.jumpAnimation = new Animation(processing, "graphics/animations/player" + playerNumber + "jump");
		this.walkAnimationGun = new Animation(processing, "graphics/animations/player" + playerNumber + "runGun");
		this.idleAnimationGun = new Animation(processing, "graphics/animations/player" + playerNumber + "idleGun");
		this.jumpAnimationGun = new Animation(processing, "graphics/animations/player" + playerNumber + "jumpGun");
		
		this.idleAnimation.start();
		
		this.facing = Facing.RIGHT;
	}
	
	// Return the current texture (ie. specific animation sprite)
	public PImage getCurrentTexture()
	{
		PImage ret;
		int time = processing.millis();
		
		if (jumpAnimation.isRunning())
		{
			ret = unarmed?jumpAnimation.getCurrentImage(time):jumpAnimationGun.getCurrentImage(time);
		}
		else if (walkAnimation.isRunning())
		{
			ret = unarmed?walkAnimation.getCurrentImage(time):walkAnimationGun.getCurrentImage(time);
		}
		else if (idleAnimation.isRunning())
		{
			ret = unarmed?idleAnimation.getCurrentImage(time):idleAnimationGun.getCurrentImage(time);
		}
		else
		{
			idleAnimation.start();
			ret = unarmed?idleAnimation.getCurrentImage(time):idleAnimationGun.getCurrentImage(time);
		}
		
		if (facing == Facing.LEFT)
		{
			ret = BImage.mirrorAroundY(processing, ret);
		}
		
		return ret;
	}
	
	public void jump()
	{
		if (!isJumping)
		{
			ySpeed = JUMPFORCE;
		}
	}
	
	public void moveLeft()
	{
		cannotMoveRight = false;
		
		facing = Facing.LEFT;
		
		if (cannotMoveLeft)
		{
			xSpeed = 0;
			return;
		}
		
		if (isInAir)
		{
			xSpeed -= MOVEACCEL_AIR;
		}
		else
		{
			xSpeed -= MOVEACCEL_GROUND;
		}
	}
	
	public void moveRight()
	{
		cannotMoveLeft = false;
		
		facing = Facing.RIGHT;
		
		if (cannotMoveRight)
		{
			xSpeed = 0;
			return;
		}
		
		if (isInAir)
		{
			xSpeed += MOVEACCEL_AIR;
		}
		else
		{
			xSpeed += MOVEACCEL_GROUND;
		}
	}
	
	
	private void controlAnimations()
	{
		boolean hasXSpeed = Math.abs(xSpeed) > CLAMPTOZERO;
		
		if (isInAir)
		{
			idleAnimation.stop();
			walkAnimation.stop();
			idleAnimationGun.stop();
			walkAnimationGun.stop();
			
			if (jumpAnimation.isStopped())
			{
				jumpAnimation.start();
				jumpAnimationGun.start();
			}
		}
		else if (hasXSpeed || isMovingSideways)
		{
			idleAnimation.stop();
			jumpAnimation.stop();
			idleAnimationGun.stop();
			jumpAnimationGun.stop();
			
			if (walkAnimation.isStopped())
			{
				walkAnimation.start();
				walkAnimationGun.start();
			}
		}
		else
		{
			jumpAnimation.stop();
			walkAnimation.stop();
			jumpAnimationGun.stop();
			walkAnimationGun.stop();
			
			if (idleAnimation.isStopped())
			{
				idleAnimation.start();
				idleAnimationGun.start();
			}
		}
	}

	
	
	public void update(State state, int deltaT)
	{
		handleInput(state);
		
		previous_xpos = xpos;
		previous_ypos = ypos;
		double deltaFactor = deltaT / (double)DELTAT_DIVIDENT;
		
		// X
		boolean hasXSpeed = Math.abs(xSpeed) > CLAMPTOZERO;
		
		controlAnimations();
		
		if (hasXSpeed)
		{
			double xSignum = Math.signum(xSpeed);
			
			if (!isMovingSideways)
			{
				double absXSpeed = Math.abs(xSpeed);
				double breakAmount = 0;
				if (isInAir)
				{
					breakAmount = BREAKACCEL_AIR * deltaFactor;
				}
				else
				{
					breakAmount = BREAKACCEL_GROUND * deltaFactor;
				}
				if (absXSpeed > breakAmount) {
					xSpeed = (absXSpeed - breakAmount) * xSignum;
				} else {
					xSpeed = 0.0;
				}
			}
			
			xSpeed = BMath.clamp(xSpeed, 0, xSignum * MAXSPEED);
		}
		else
		{	
			xSpeed = 0;
		}
		
		xpos = xpos + xSpeed * deltaFactor;
		
		
		
		// Y
		//if (isInAir)
		//{
			yAcceleration = GRAVITY;
			//ypos = ypos + ySpeed * deltaFactor + 0.5 * yAcceleration * Math.pow(deltaFactor, 2);
			ySpeed += yAcceleration * deltaFactor;
			ypos += ySpeed * deltaFactor;// + yAcceleration;
			//System.out.println("ySpeed:"+ySpeed+" yPos:"+ypos+" deltaT:"+deltaT+" deltaFactor:"+deltaFactor);
		/*}
		else
		{
			yAcceleration = 0.0;
			ypos = ypos + ySpeed * deltaFactor;
		}*/
		
		
		
		
		
		
		
		
		if(deltaT > 84) {
			System.out.println("high deltaT: "+ deltaT);
		}
		
		//make sure, ypos and xpos did not travel to far for one frame 
		// once they travel too far, they can cross a collision box  - and we surely do not want that!
		double yDiff = ypos - previous_ypos;
		double xDiff = xpos - previous_xpos;
		double maxDistance = 0.9;
		if (Math.abs(yDiff)>=maxDistance) {
			ypos = previous_ypos + maxDistance * Math.signum(yDiff);
		}
		if (Math.abs(xDiff)>=maxDistance) {
			xpos = previous_xpos + maxDistance * Math.signum(xDiff);
		}
		

		
		if (this.isColliding(xpos - this.collisionBoxWidth()/2, ypos, xSpeed, ySpeed)) {
			xpos = this.getNewX() + this.collisionBoxWidth()/2; ypos = this.getNewY();
			xSpeed = this.getNewXSpeed(); ySpeed = this.getNewYSpeed();
			if (ySpeed == 0) {
				isInAir = isJumping = false;
				if (! this.soundHitBottomPlayed) {
					Stereophone.playSound("001", "player_hitground", 100);
					this.soundHitBottomPlayed = true;
				}
			}
			//isInAir = isJumping = this.getNewIsJumping();
			
			// any interesting collision partners?
			Object gameElement = this.getBouncePartner();
			if (gameElement instanceof FinishLine) {
				// we won!!!
				this.setChanged();
				HashMap map = new HashMap();
				map.put("IFUCKINGWON", myID);
				this.notifyObservers(map);
				Stereophone.playSound("310", "playerwon", 10000);
			} else if (gameElement instanceof StandardCreature) {
				// player hit a creature
				((StandardCreature) gameElement).contact(this);
			} else if (gameElement instanceof Door)
			{
				this.setChanged();
				HashMap map = new HashMap();
				map.put("OHDOORTAKEMEAWAY", myID);
				this.notifyObservers(map);
			}
			

		} else {
			this.soundHitBottomPlayed = false;
		}
		
		if (ySpeed != 0) isInAir = isJumping = true;
		
		
		
			
		
		
	
		// update the position of the characters collision box
		this.updatePosition(xpos-1, ypos);
		
	}
	
	
	private void handleInput(State state)
	{
		//if (switchHappening) return; // no input while a switch is happening
		
		boolean jumpbutton = (this.myID == 1) ?
				(state.containsKey('w') && state.get('w')) :
				(state.containsKey('i') && state.get('i'));
		
		boolean leftbutton = (this.myID == 1) ?
				(state.containsKey('a') && state.get('a')) :
				(state.containsKey('j') && state.get('j'));
				
		boolean rightbutton = (this.myID == 1) ?
				(state.containsKey('d') && state.get('d')) :
				(state.containsKey('l') && state.get('l'));
				
		boolean downbutton = (this.myID == 1) ?
				(state.containsKey('s') && state.get('s')) :
				(state.containsKey('k') && state.get('k'));

		// a player should always have to press jump again for another jump
		if (didJump && !jumpbutton) didJump = false;  
		// once the game is over, players can not move left / right
		//if (gameOver) leftbutton = rightbutton = false;
		
		if (jumpbutton && !didJump)
		{
			jump();
			this.didJump = true;
			if (BunnyHat.TWIN_JUMP)
			{
				myTwin.jump();
			}
		}
		
		if (leftbutton)
		{
			isMovingSideways = true;
			moveLeft();
		}
		else if (rightbutton)
		{
			isMovingSideways = true;
			moveRight();
			
		}
		
		if (downbutton)
		{
			// Use stuff
		}
	}
	

	@Override
	public void collisionDraw(PGraphics cb, int xOff, int yOff)
	{
		// TODO Auto-generated method stub
		cb.noFill();
		cb.stroke(255, 0, 0);
		//System.out.println("draw player "+xpos+":"+ypos);
		cb.rect((float)xpos*2, (float)ypos*2+yOff, (float)this.collisionBoxWidth()*2, (float)this.collisionBoxHeight()*2);
	}

}
