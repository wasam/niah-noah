package game;

import game.level.Level;

import java.awt.Polygon;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;

/**
 * All we need for a nice collision detection
 * 
 * @author samuelwalz
 *
 */
public abstract class CollisionBox
{
	public enum Effects {STOP, BOUNCE, SLOW_DOWN}
	private Effects collisionEffect = Effects.STOP;
	
	private Level collisionLevel;
	
	private Object gameElement;
	
	private CollisionBox currentCollisionPartner;
	
	private double newX, newY, newXSpeed, newYSpeed;
	
	/**
	 * Describing the path, a object can move along while being on ground
	 */
	private Line2D.Double collisionGroundPath;
	
	/**
	 * collision box for this object
	 */
	private Rectangle2D.Double cBox;
	
	public class CollisionBoxData {
		double xSpeed, ySpeed, x, y;
		
	}
	
	/**
	 * 
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 */
	public CollisionBox(double x, double y, double width, double height) {
		cBox = new Rectangle2D.Double(x, y, width, height);
		
	}
	
	public double collisionBoxWidth() {
		return this.cBox.width;
	}
	
	protected void setGameElement(Object gameElement) {
		this.gameElement = gameElement;
	}
	
	protected void setLevel(Level lvl) {
		collisionLevel = lvl;
	}
	
	public void setCollisionEffect(Effects effect) {
		this.collisionEffect = effect;
	}
	
	protected Effects getCollisionEffect() {
		return this.collisionEffect;
	}
	
	/**
	 * 
	 * @param x
	 * @param y
	 */
	protected void updatePosition(double x, double y) {
		cBox.setRect(x, y, cBox.width, cBox.height);
	}
	
	protected Rectangle2D.Double getFutureCollisionBox(double x, double y) {
		return new Rectangle2D.Double(x, y, cBox.width, cBox.height);
	}
	
	protected boolean isInTheWay(Line2D.Double line) {
		return line.intersects(cBox);
	}
	
	protected boolean isCollidingWith(CollisionBox theOtherBox) {
		return cBox.intersects(theOtherBox.cBox);
	}
	
	/**
	 * 
	 * @param x future x
	 * @param y future y
	 * @return
	 */
	protected CollisionBoxData isColliding (CollisionBoxData data) {
		CollisionBoxData newData = new CollisionBoxData();
		boolean collision = false;
		newData.x = data.x;
		newData.y = data.y;
		newData.xSpeed = data.xSpeed;
		newData.ySpeed = data.ySpeed;
		
		//determine directions
		double xDistance = data.x - cBox.x;
		double yDistance = data.y - cBox.y;
		int xDirection = (int)(xDistance / Math.abs(xDistance));
		int yDirection = (int)(yDistance / Math.abs(yDistance));
		
		//determine points
		int x0, y0, x1, y1, fx0, fy0, fx1, fy1;
		x0 = (int)(cBox.x + 0.2); x1 = (int)(cBox.x + cBox.width - 0.2);
		y0 = (int)(cBox.y + 1 +0.2); y1 = (int)(cBox.y + cBox.height -0.2);
		fx0 = (int)data.x; fy0 = (int)data.y + 1;
		fx1 = (int)(data.x + cBox.width); fy1 = (int)(data.y + cBox.height);
		
		CollisionBox collider;
		if (xDirection > 0) {
			for (int curY = y0; curY <= y1; curY++) {
				collider = collisionLevel.getBoxAt(fx1, curY);
				if (collider != null) {
					newXSpeed = 0;
					newX = fx1-this.cBox.width;
					collision = true;
					newData.xSpeed = 0;
					newData.x = fx1-this.cBox.width;
					break;
				}
			}
		} else if (xDirection < 0) {
			for (int curY = y0; curY <= y1; curY++) {
				collider = collisionLevel.getBoxAt(fx0, curY);
				if (collider != null) {
					newXSpeed = 0;
					newX = fx0+1;
					collision = true;
					newData.xSpeed = 0;
					newData.x = fx0+1;
					break;
				}
			}
		}
		if (collisionGroundPath != null) {
			double xLeftEnd = newData.x;
			double xRightEnd = newData.x + 2;
			if (collisionGroundPath.x1 > xRightEnd
					|| collisionGroundPath.x2 < xLeftEnd) {
				collisionGroundPath = null; // we fell of an edge : no path anymore
			}
		}
		
		
		if (yDirection > 0) {
			this.collisionGroundPath = null; // jump or so: we are losing connection to ground
			
			for (int curX = x0; curX <= x1; curX++) {
				collider = collisionLevel.getBoxAt(curX, fy1);
				if (collider != null) {
					// hit head
					newData.ySpeed = -0.01;
					newData.y = fy1-this.cBox.height;
					break;
				}
			}
		} else if (yDirection < 0) {
			if (collisionGroundPath != null) {
				newData.ySpeed = 0;
				newData.y = cBox.y;
				//System.out.print("onground \n");
			} else {
				//System.out.print("in air\n");
				for (int curX = x0; curX <= x1; curX++) {
					collider = collisionLevel.getBoxAt(curX, fy0);
					if (collider != null) {
						// hit feet
						if (collider.getCollisionEffect() == Effects.STOP) {
							newData.ySpeed = 0;
							newData.y = fy0;
							this.collisionGroundPath = collider.getHeadline();
						} else if (collider.getCollisionEffect() == Effects.BOUNCE) {
							newData.ySpeed = -data.ySpeed;
							newData.y = fy0;
						}
						break;
					}
				}
			}
		}
		
		
		
		return newData;
	}
	
	protected Line2D.Double getHeadline() {
		return new Line2D.Double(cBox.x, cBox.y + cBox.height, cBox.x + cBox.width, cBox.y + cBox.height);
	}
	
	/**
	 * informs about being bounced by another box
	 */
	protected abstract void bounce(Object gameElement);
	
	public abstract void collisionDraw();
	
	
}