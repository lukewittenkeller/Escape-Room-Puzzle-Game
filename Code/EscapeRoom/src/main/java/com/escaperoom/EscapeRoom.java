package com.escaperoom;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.escaperoom.engine.BackgroundMusic;
import com.escaperoom.engine.Camera;
import com.escaperoom.engine.Screen;
import com.escaperoom.game.GameInfo;
import com.escaperoom.game.actors.Player;
import com.escaperoom.game.levels.Level;
import com.escaperoom.game.levels.LevelOne;
import com.escaperoom.game.levels.Tutorial;
import com.escaperoom.ui.component.Inventory;
import com.escaperoom.ui.menu.PauseMenu;
import com.escaperoom.ui.menu.StartMenu;

public class EscapeRoom extends JFrame implements Runnable, ActionListener, KeyListener {

	private static final long serialVersionUID = 1L;

	// Game Engine
	private Camera camera;
	private Screen screen;
	private Dimension screenSize;
	
	// Game components
	private Level currentLevel;
	private GameInfo gameInfo = new GameInfo(new Player());
	private StartMenu startMenu;
	private PauseMenu pauseMenu;
	//private static JFrame inventory; // Bottom container JFrame
	private Inventory inventory;

	// Audio Engine
	BackgroundMusic bgm = new BackgroundMusic();
	
	// Map information
	private int mapWidth = 15;
	private int mapHeight = 15;

	// Cosmetic Info
	private BufferedImage image;
	private int[] pixels;

	//Running game
	private Thread thread;
	private boolean running;


	
	public EscapeRoom() {
		screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setSize(screenSize.width - 400, screenSize.height - 400);
		
		//Main JFrame details
		super.setUndecorated(true);
		super.addKeyListener(camera);
		super.setResizable(false);
		super.setTitle("3D Engine");
		super.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		super.setLayout(new BorderLayout());
		super.setLocationRelativeTo(null);
		super.setVisible(true);
		super.addKeyListener(this);
		
		//Instantiate menus
		startMenu = new StartMenu();
		startMenu.addActionListners(this);
		pauseMenu = new PauseMenu();
		pauseMenu.addActionListners(this);

		super.add(startMenu);

	}

	private synchronized void start() {
		running = true;
		thread.start();
	}

	public synchronized void stop() {
		running = false;
		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void render() {
		BufferStrategy bs = getBufferStrategy();
		if (bs == null) {
			createBufferStrategy(3);
			return;
		}
		Graphics g = bs.getDrawGraphics();
		g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);

		bs.show();

	}

	public void run() {
		bgm.playMusic(new File("src\\main\\resources\\doom.wav"));
		long lastTime = System.nanoTime();
		final double ns = 1000000000.0 / 30.0;// 60 times per second
		double delta = 0;
		requestFocus();

		while (running) {
			long now = System.nanoTime();
			delta = delta + ((now - lastTime) / ns);
			lastTime = now;
			while (delta >= 1)// Make sure update is only happening 60 times a second
			{

				// handles all of the logic restricted time
				screen.update(camera, pixels);
				
				if (gameInfo.change_pos) 
				{
					//System.out.println("Changed Position");
					gameInfo.change_pos = false;
					camera.xPos = gameInfo.getCameraPositionX();
					camera.yPos = gameInfo.getCameraPositionY();
				}
				else 
				{
					gameInfo.setCameraPositionX(camera.xPos);
					gameInfo.setCameraPositionY(camera.yPos);
				}

				
				gameInfo.setCameraPositionX(camera.xPos);
				gameInfo.setCameraPositionY(camera.yPos);
				gameInfo.setLastKeyPressed(camera.getLastKey());
				currentLevel.levelLogic(gameInfo);
				//inventory.update(gameInfo.getPlayer());
				camera.update(currentLevel.getMap());
				delta--;

				// If the player activated a puzzle (no longer showing the game screen)
				if (gameInfo.getActivePuzzle() != null) {
					camera.setLastKeyPressed(null); //Without this, the last key pressed would still be 'E' and the puzzle would reactivate if the user quit it
					JPanel puzzle = gameInfo.getActivePuzzle();

					// Show Puzzle
					showPuzzleScreen(puzzle);

					// Run the level logic to check for puzzle completion
					currentLevel.levelLogic(gameInfo);

					// Remove Puzzle
					gameInfo.setActivePuzzle(null);
					hidePuzzleScreen(puzzle);

					screen.update(camera, pixels);
					render();
					continue;

				}
			}
			render();// displays to the screen unrestricted time
		}
	}
	
	private void hidePuzzleScreen(JPanel activePuzzle) {
		SwingUtilities.invokeLater(() -> {
			requestFocus();
			super.remove(activePuzzle);
			super.revalidate();
			super.repaint();
		});		
	}

	private void showPuzzleScreen(JPanel activePuzzle) {
		SwingUtilities.invokeLater(() -> {
			requestFocus();
			super.add(activePuzzle);
			super.revalidate();
			super.repaint();
		});
		
	}
	
	
	//Load a specific level and display it on the screen
	public void loadLevel(Level currentLevel,int x, int y) {
		this.currentLevel = currentLevel;
		int startPositionX =x;
		int startPositionY = y;
		
		SwingUtilities.invokeLater(() -> {
			thread = new Thread(this);
			image = new BufferedImage(screenSize.width - 400, screenSize.height - 400, BufferedImage.TYPE_INT_RGB);
			pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
		
		
			//Instantiate GameEngine Objects
			screen = new Screen(currentLevel.getMap(), mapWidth, mapHeight, currentLevel.getTextures(),
					currentLevel.getSprites(), 
					screenSize.width - 400,
					screenSize.height - 400);

			camera = new Camera(startPositionX,startPositionY, 1,0, 0, -.66, screen.sprites);
			
			
			requestFocus();
			super.remove(startMenu);
			super.addKeyListener(camera);
			super.addKeyListener(this);
			start();
			
			// This section is all for the bottom of the screen inventory
			final Rectangle mainScreen = this.getBounds();
			inventory = new Inventory(mainScreen,screenSize,currentLevel);
			gameInfo.getPlayer().setDisplay(inventory);
			super.repaint();
			super.revalidate();
		});
	}
	
	

	@Override
	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyReleased(KeyEvent event) {

		// If the user wants to pause the game
		if (event.getKeyCode() == KeyEvent.VK_ESCAPE) {
			stop();
			bgm.pause();

			/*
			 * This ensures that we update the GUI in the Application Thread (Basically
			 * Platform.runLater() in JavaFX)
			 */
			SwingUtilities.invokeLater(() -> {
				requestFocus();
				super.add(pauseMenu);
				super.revalidate();
				super.repaint();
			});

		}

	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();

		if (src.equals(pauseMenu.getQuitButton())) {
			running = false;
			SwingUtilities.invokeLater(() -> {
				requestFocus();
			
				super.add(startMenu);
				super.remove(pauseMenu);
				gameInfo.getPlayer().setDisplay(null);
				super.revalidate();
				super.repaint();
			});
			

			// If the user wants to resume the game
		} else if (src.equals(pauseMenu.getResumeButton())) {
			SwingUtilities.invokeLater(() -> {
				camera.setRotationSpeed(pauseMenu.getOptionsMenu().getRotationSpeed());
				bgm.setGain(pauseMenu.getOptionsMenu().getVolume());
				requestFocus();
				super.remove(pauseMenu);
				super.revalidate();
				super.repaint();

				running = true;
				new Thread(this).start();

			});

		} else if (src.equals(pauseMenu.getOptionsButton())) {
			SwingUtilities.invokeLater(() -> {
				requestFocus();
				super.remove(pauseMenu);
				super.add(pauseMenu.getOptionsMenu());
				super.revalidate();
				super.repaint();
			});

		} else if (src.equals(pauseMenu.getOptionsMenu().getBackButton())) {
			SwingUtilities.invokeLater(() -> {
				requestFocus();
				super.remove(pauseMenu.getOptionsMenu());
				super.add(pauseMenu);
				super.revalidate();
				super.repaint();
			});
		} else if(src.equals(startMenu.getTutorialButton())) {
			loadLevel(new Tutorial(),4,4);
			
		} else if(src.equals(startMenu.getLevelOneButton())) {
			loadLevel(new LevelOne(),7,7);

		}

	}

	public static void main(String[] args) {
		EscapeRoom game = new EscapeRoom();
		game.setVisible(true);

		// This JOptionPane is to only show at the start of the game
		JOptionPane.showMessageDialog(game, "USE WASD TO MOVE CHARACTER" + "\n" + "Move Foward: W"
				+ "\n" + "Move Backward: S" + "\n" + "Move Camera Left/Right: A and D");
	}
}