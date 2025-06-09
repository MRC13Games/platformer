package gamelogic.enemies;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import gameengine.PhysicsObject;
import gameengine.hitbox.RectHitbox;
import gamelogic.GameResources;
import gamelogic.level.Level;

public class Potion extends PhysicsObject{

	private BufferedImage image;
	
	public Potion(float x, float y, Level level) {
		super(x, y,(int)(level.getLevelData().getTileSize()*1.5), (int)(level.getLevelData().getTileSize()*1.5), level);
		this.hitbox = new RectHitbox(this, 10, 10, width - 10, height - 10);
		this.image = GameResources.potion;
	}
	
	@Override
	public void update(float tslf) {
   	 //super.update(tslf);
	}
	
	@Override
	public void draw(Graphics g) {
		g.drawImage(image, (int)position.x, (int)position.y, width, height, null);
		
		hitbox.draw(g);
	}
	
}