package mapeditor;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JFileChooser;

import gameengine.GameBase;
import gameengine.graphics.Camera;
import gameengine.input.KeyboardInputManager;
import gameengine.input.MouseInputManager;
import gameengine.loaders.Tileset;
import gameengine.maths.Vector2D;
import gamelogic.GameResources;

class MapEditor extends GameBase{

	public static final int SCREEN_WIDTH = 1280;
	public static final int SCREEN_HEIGHT = 860;

	public static Camera camera;

	private boolean isPressed;
	private Vector2D oldMousePosition; //saves position of the mouse when mouse is dragged
	private Vector2D oldCameraPosition; //saves position of the camera when mouse is dragged

	private EditorTiledMap map;

	private int screenSplit = 1000;

	public Palette paletteTiles; // index = 0;
	public Palette paletteObjects; // index = 1;

	private int selectedPalette = 0;

	private JFileChooser jFileChooser;
	private boolean changed = false;

	public static void main(String[] args) {
		MapEditor mapeditor = new MapEditor();
		mapeditor.start("MapEditor Eden Jump", SCREEN_WIDTH, SCREEN_HEIGHT);	
	}

	@Override
	public void init() {
		GameResources.load();

		camera = new Camera(screenSplit, SCREEN_HEIGHT, -1, -1, -1); //-1 -> no borders
		oldMousePosition = new Vector2D();
		oldCameraPosition = new Vector2D();

		Tileset tileset = GameResources.tileset;

		Map<Integer, BufferedImage> images = tileset.getIdImages();
		PaletteItem[] paletteItems = new PaletteItem[images.size()+2];
		
		int i = 0;
		for(Entry<Integer, BufferedImage> entry: images.entrySet()) {
			
			paletteItems[i] = new PaletteItem("item :"+entry.getKey(), entry.getKey(), entry.getValue());
			i++;
			if(i==8){
				paletteItems[8] = new PaletteItem("Enemy", 8, GameResources.enemy);
				i++;
			}
			if(i==22){
				paletteItems[22] = new PaletteItem("Potion", 22, GameResources.potion);
				i++;
			}
		}
		
		
		paletteTiles = new Palette(screenSplit + 15, 10, paletteItems);
		paletteTiles.setSelectedIndex(0);

		paletteItems = new PaletteItem[1];
		paletteItems[0] = new PaletteItem("Player", 0, null);
		paletteObjects = new Palette(screenSplit + 15, 400, paletteItems);

		map = createNewMap(100, 20, 50);

		jFileChooser = new JFileChooser();
		jFileChooser.setCurrentDirectory(new File("/workspaces/platformer/workspace/maps"));
	}

	public EditorTiledMap createNewMap(int width, int height, int tileSize) {
		EditorTile[][] tiles = new EditorTile[width][height];
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				tiles[x][y] = new EditorTile(x * tileSize, y * tileSize, tileSize, 0, null);
			}
		}
		EditorTiledMap map = new EditorTiledMap(width, height, tileSize, tiles, 0, 0);

		window.setTitle("New Map");
		return map;
	}

	public EditorTiledMap loadMap(File file) throws Exception{
		BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

		int width = Integer.parseInt(bufferedReader.readLine().split("=")[1]);
		int height = Integer.parseInt(bufferedReader.readLine().split("=")[1]);
		int tileSize = Integer.parseInt(bufferedReader.readLine().split("=")[1]);
		EditorTile[][] tiles = new EditorTile[width][height];
		for (int y = 0; y < height; y++) {
			String[] values = bufferedReader.readLine().split(",");
			for (int x = 0; x < width; x++) {
				int value = Integer.parseInt(values[x]);
				BufferedImage img =null;
				for(int i=0; i<paletteTiles.getPaletteItems().length; i++) {
					if(paletteTiles.getPaletteItems()[i].getValue() == value) {
						img = paletteTiles.getPaletteItems()[i].getImage();
					}
				}
				tiles[x][y] = new EditorTile(x * tileSize, y * tileSize, tileSize, value, img);
				//System.out.println(value);
				//System.out.println(paletteTiles.getPaletteItems()[value].getName());
						}
		}
		String[] playerPos = bufferedReader.readLine().split("=")[1].split(",");
		int playerX = Integer.parseInt(playerPos[0]);
		int playerY = Integer.parseInt(playerPos[1]);
		bufferedReader.close();

		EditorTiledMap map = new EditorTiledMap(width, height, tileSize, tiles, playerX, playerY);

		window.setTitle(file.getName());
		return map;
	}

	@Override
	public void update(float tslf) {
		float mouseX = MouseInputManager.getMouseX();
		float mouseY = MouseInputManager.getMouseY();

		//Loading a map
		if(KeyboardInputManager.isKeyDown(KeyEvent.VK_CONTROL) && KeyboardInputManager.isKeyDown(KeyEvent.VK_L)) {
			int returnValue = jFileChooser.showOpenDialog(null);

			if(returnValue == JFileChooser.APPROVE_OPTION) {
				try {
					map = loadMap(jFileChooser.getSelectedFile());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			KeyboardInputManager.setKey(KeyEvent.VK_CONTROL, false);
			KeyboardInputManager.setKey(KeyEvent.VK_L, false);
		} 

		//Scrolling on the map
		if(MouseInputManager.isButtonDown(MouseEvent.BUTTON3)) {
			if(!isPressed) {
				oldCameraPosition.x = camera.getX();
				oldCameraPosition.y = camera.getY();
				oldMousePosition.x = mouseX;
				oldMousePosition.y = mouseY;
				isPressed = true;
			}
			camera.setX(oldCameraPosition.x + (oldMousePosition.x - mouseX));
			camera.setY(oldCameraPosition.y + (oldMousePosition.y - mouseY));
		}else {
			isPressed = false;
		}

		//Updating the map
		map.update(tslf);

		//Saving the map
		if(KeyboardInputManager.isKeyDown(KeyEvent.VK_CONTROL) && KeyboardInputManager.isKeyDown(KeyEvent.VK_S)) {
			int returnValue = jFileChooser.showSaveDialog(null);

			if(returnValue == JFileChooser.APPROVE_OPTION) {
				try {
					File file = jFileChooser.getSelectedFile();
					MapSaver.wirteMap(file, map);

					changed = false;
					window.setTitle(file.getName());
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
			KeyboardInputManager.setKey(KeyEvent.VK_CONTROL, false);
			KeyboardInputManager.setKey(KeyEvent.VK_S, false);
		}

		//Updating the palette
		paletteTiles.update(tslf);
		paletteObjects.update(tslf);

		if(MouseInputManager.isButtonDown(MouseEvent.BUTTON1)) {
			if(paletteTiles.getX() < mouseX && mouseX < paletteTiles.getX() + paletteTiles.getWidth() && paletteTiles.getY() < mouseY && mouseY < paletteTiles.getY() + paletteTiles.getHeight()) {
				selectedPalette = 0;
				paletteObjects.setSelectedIndex(-1);
			}
			if(paletteObjects.getX() < mouseX && mouseX < paletteObjects.getX() + paletteObjects.getWidth() && paletteObjects.getY() < mouseY && mouseY < paletteObjects.getY() + paletteObjects.getHeight()) {
				selectedPalette = 1;
				paletteTiles.setSelectedIndex(-1);
			}
		}

		//Set value when tile is selected
		if(selectedPalette == 0) {
			EditorTile mouseOver = map.getMouseOver();
			if(mouseOver != null) {
				if(camera.isVisibleOnCamera(mouseOver.getX(), mouseOver.getY(), mouseOver.getSize(), mouseOver.getSize())) { 
					if(MouseInputManager.isButtonDown(MouseEvent.BUTTON1)) {
						mouseOver.setValue(paletteTiles.getSelectedPaletteItem().getValue());
						System.out.println(paletteTiles.getSelectedPaletteItem().getValue()+" "+paletteTiles.getSelectedPaletteItem().getName());
						mouseOver.setImage(paletteTiles.getSelectedPaletteItem().getImage());

						if(!changed) {
							window.setTitle(window.getTitle() + "*");
							changed = true;
						}
					}
				}
			}
		} else if(selectedPalette == 1) {
			EditorTile mouseOver = map.getMouseOver();
			if(mouseOver != null) {
				if(camera.isVisibleOnCamera(mouseOver.getX(), mouseOver.getY(), mouseOver.getSize(), mouseOver.getSize())) { 
					if(MouseInputManager.isButtonDown(MouseEvent.BUTTON1)) {
						if(paletteObjects.getSelectedPaletteItem().getName().equals("Player")) {
							map.setPlayerPositon(map.getMouseTileX(), map.getMouseTileY());

							if(!changed) {
								window.setTitle(window.getTitle() + "*");
								changed = true;
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void draw(Graphics g) {
		drawBackground(g);

		g.translate(-(int)camera.getX(), -(int)camera.getY());

		map.draw(g);

		g.translate(+(int)camera.getX(), +(int)camera.getY());

		//Fill background
		g.setColor(Color.WHITE);
		g.fillRect(screenSplit, 0, SCREEN_WIDTH-screenSplit, SCREEN_HEIGHT);

		//Draw Screen-split line
		g.setColor(Color.BLACK);
		g.drawLine(screenSplit, 0, screenSplit, SCREEN_HEIGHT);

		paletteTiles.draw(g);
		paletteObjects.draw(g);
	}

	public void drawBackground(Graphics g) {
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
	}
}
