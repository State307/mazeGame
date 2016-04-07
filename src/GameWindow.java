 /**
 * @author Group L
 * Matt Grant, Adam Coggeshall, Jared Frank, Alex Germann, Auston Larson
 * COSC 3011 Program 01
 * GameWindow.java
 */

import javax.swing.*;
import java.util.ArrayList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

public class GameWindow extends JFrame
  implements ActionListener, MouseListener, MouseMotionListener
  {
    /**
     * because it is a serializable object, need this or javac
     * complains a lot
     */
    public static final long serialVersionUID=1;
    
    private ArrayList<FrontEndTileHolder> tileHolders = new ArrayList<FrontEndTileHolder>();
    
    private Messenger messenger;
    
    // We store the most recent mouse event so we can access the mouse's
    // position in our paint method. -AC
    private MouseEvent lastMouseEvent;
    
    // We use this to implement double buffering. Everything is drawn to this
    // buffer, then the buffer is drawn to the screen in one go.
    // This prevents screen flickering. It seems that some components will do
    // this automatically, but JFrame is not one of them. -AC
    private BufferedImage backBuffer;
    
    /**
     * The constructor sets up the UI.
     * We pass it a reference to the backend GameBoard. -AC
     */
    public GameWindow(Messenger messenger)
    {
      super("Group L aMaze");
      
      this.messenger = messenger;
      
      setupUI();
      setupGame();
    }

    /**
     *  Sets up the UI.
     *  Registers event listeners. -AC
     */

    public void setupUI()
    {
      Dimension windowSize = new Dimension(900, 1000);
      
      // Configure the window. -AC
      setSize(windowSize);
      setResizable(false);
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      getContentPane().setBackground(Color.cyan);
      
      // Set up out backbuffer. -AC
      backBuffer = new BufferedImage(
          windowSize.width,
          windowSize.height,
          BufferedImage.TYPE_INT_RGB );
      
      // Add event listeners. -AC
      addMouseListener(this);
      addMouseMotionListener(this);
      
      this.addButtons();
      
      setVisible(true);
    }
    
    /**
     * Sets up the visual representation of tile containers.
     */
    public void setupGame() {
      FrontEndGameBoard board =
          new FrontEndGameBoard(messenger, 290, 300);
      
      FrontEndSideHolder leftSide =
          new FrontEndSideHolder(messenger, BoardSide.LEFT, 50, 80);
      
      FrontEndSideHolder rightSide = 
          new FrontEndSideHolder(messenger, BoardSide.RIGHT, 770, 80);
      
      tileHolders.add( board );
      tileHolders.add( leftSide );
      tileHolders.add( rightSide );
    }
    
    
    /**
     * Used by setupUI() to create and configure the buttons. -AC
     */
    public void addButtons(){
      
      // We decided to create the layout here, and use a FlowLayout.
      // It handles buttons much more naturally than a GridLayout.
      // If we really need to add more elements using more advanced
      // layout, we can add another container later. -AC
      FlowLayout button_layout = new FlowLayout(FlowLayout.LEFT, 5, 5);
      this.setLayout(button_layout);
      
      Button btn_new = new Button("New Game");
      btn_new.setActionCommand("new");
      btn_new.addActionListener(this);
      this.add(btn_new);
      
      Button btn_reset = new Button("Reset");
      btn_reset.setActionCommand("reset");
      btn_reset.addActionListener(this);
      this.add(btn_reset);
      
      Button btn_quit = new Button("Quit");
      btn_quit.setActionCommand("quit");
      btn_quit.addActionListener(this);
      this.add(btn_quit);
    }
    
    // Here we handle events generated by the buttons. -AC
    @Override
    public void actionPerformed(ActionEvent e) {
      switch (e.getActionCommand()) {
      case "new":
        System.out.println("New Game");
        break;
      case "reset":
        messenger.resetGame();
        this.repaint();
        break;
      case "quit":
        System.exit(0);
        break;
      }
    }
    
    
    /**
     * We decided to draw the game board and tiles ourselves, rather than
     * extending UI components. This now calls the draw method of each
     * tile holder. The tile holders are responsible for drawing their
     * tiles. We draw the currently dragged tile ourselves. -AC
     */
    @Override
    public void paint(Graphics windowGraphics) {
      // Use our backbuffer for all the drawing. -AC
      Graphics g = backBuffer.getGraphics();
      
      // Call the super paint method. Seems to clear the window to the
      // background color. -AC
      super.paint(g);
      
      // Draw tileholders and their contained tiles. -AC
      for (FrontEndTileHolder holder : tileHolders) {
        holder.draw(g);
      }
      
      // We draw the tile currently being dragged. -AC
      Image draggedTileImage = messenger.getDraggedTileImage();
      if (draggedTileImage != null) {
        // We position the dragged tile so that its center is on the cursor.
        // I had considered keeping the offset consistent with the offset
        // when the drag starts, but this is a good deal less complicated
        // and the positions that will allow for a valid drop will make a bit
        // more sense. -AC
        int draggedX = lastMouseEvent.getX() - TileDrawer.TILE_SIZE/2;
        int draggedY = lastMouseEvent.getY() - TileDrawer.TILE_SIZE/2;
        TileDrawer.drawTile(g, draggedX, draggedY, draggedTileImage);
      }
      
      windowGraphics.drawImage(backBuffer, 0, 0, null);
    }
    
    // Here we handle mouse input. We end up with some empty methods since
    // we have to implement everything in the MouseListener interface. -AC

    @Override
    public void mousePressed(MouseEvent e) {
      for (FrontEndTileHolder holder : tileHolders) {
        Image tileImage = holder.getTileImageFromClick(e);
        // If there is a tile present, then we can start the drag! -AC
        if (tileImage != null) {
          int slot = holder.getSlotFromClick(e);
          
          messenger.setDragInfo(slot, tileImage);
          lastMouseEvent = e;
          this.repaint();
          
          break;
        }
      }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      // We only have to handle a drop if we are currently dragging. -AC
      if (messenger.getDraggedTileImage() != null) {
        for (FrontEndTileHolder holder : tileHolders) {
          int destinationSlot = holder.getSlotFromClick(e);
          // If we have a destination slot, do a swap. -AC
          if (destinationSlot >= 0) {
            messenger.movetile( messenger.getDragSourceSlot(), destinationSlot);
            break;
          }
        }
        messenger.clearDragInfo();
        this.repaint();
      }
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
      // Store the mouse's position by saving the event, and repaint the window
      // so dragged tiles display correctly. -AC
      lastMouseEvent = e;
      repaint();
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
      // Do nothing. -AC
    }

    @Override
    public void mouseEntered(MouseEvent e) {
      // Do nothing. -AC
    }
    
    @Override
    public void mouseExited(MouseEvent e) {
      // Do nothing for now. I was worried that not handling this event could
      // cause problems with the drag and drop, but it seems to work okay
      // so far. -AC
    }

    @Override
    public void mouseMoved(MouseEvent arg0) {
      // Do nothing. We only listen for the drag event.
    }
  };
