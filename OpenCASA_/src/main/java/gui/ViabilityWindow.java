package gui;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import data.Spermatozoon;
import functions.ComputerVision;
import functions.Paint;
import functions.VideoRecognition;
import ij.IJ;
import ij.ImagePlus;

public class ViabilityWindow extends ImageAnalysisWindow implements ChangeListener, MouseListener{

  private ImagePlus aliveImpOutline;
  private ImagePlus deadImpOutline;
  protected List<Spermatozoon> aliveSpermatozoa     = new ArrayList<Spermatozoon>();
  protected List<Spermatozoon> deadSpermatozoa      = new ArrayList<Spermatozoon>();
  private enum Channel{RED,GREEN,BLUE}
  private boolean         isThresholding  = false;
  public ViabilityWindow() {
    super();
    setChangeListener(this);
    setMouseListener(this);
  }

  /**
   * This method refreshes the showed image after changing the threshold with
   * the sliderbar
   */
  private void doSliderRefresh() {
    if (!isThresholding) {
      isThresholding = true;
      Thread t1 = new Thread(new Runnable() {
        public void run() {
          processImage(true);
          isThresholding = false;
        }
      });
      t1.start();
    }
  }
  
  private List<Spermatozoon> getSpermatozoa (Channel rgbChannel){    
    ComputerVision cv = new ComputerVision();
    if(rgbChannel == Channel.RED)
      impTh = cv.getRedChannel(impOrig.duplicate());
    else if (rgbChannel == Channel.GREEN)
      impTh = cv.getGreenChannel(impOrig.duplicate());
    else if (rgbChannel == Channel.BLUE)
      impTh = cv.getBlueChannel(impOrig.duplicate());
    cv.convertToGrayscale(impTh);
    thresholdImagePlus(impTh);
    //this will be useful for painting outlines later
    if(rgbChannel == Channel.RED)
      deadImpOutline = impTh;
    else if (rgbChannel == Channel.GREEN)
      aliveImpOutline = impTh;
    else if (rgbChannel == Channel.BLUE)
      aliveImpOutline = impTh;
    VideoRecognition vr = new VideoRecognition();
    List<Spermatozoon>[] sperm = vr.detectSpermatozoa(impTh);
    return sperm[0];
  }

  protected void processImage(boolean eventType){
    //If eventType == true, the threshold has changed or it needs to be calculated
    // In that class, eventType is always true
    aliveSpermatozoa = getSpermatozoa(Channel.GREEN);
    deadSpermatozoa = getSpermatozoa(Channel.RED);
    if (aliveSpermatozoa != null && deadSpermatozoa!=null){
      spermatozoa = new ArrayList<Spermatozoon>(aliveSpermatozoa);
      spermatozoa.addAll(deadSpermatozoa);
      selectAll();//set as selected all spermatozoa to allow boundary painting
      idenfitySperm();
    }
    // Calculate outlines
    ComputerVision cv = new ComputerVision();
    cv.outlineThresholdImage(aliveImpOutline);
    cv.outlineThresholdImage(deadImpOutline);
    drawImage();
  }
  
  private void drawImage(){
    //Draw cells on image
    impDraw = impOrig.duplicate();
    Paint paint = new Paint();
    paint.drawOutline(impDraw, aliveImpOutline);
    paint.drawOutline(impDraw, deadImpOutline);
    impDraw.setColor(Color.red);
    paint.drawBoundaries(impDraw, deadSpermatozoa);
    impDraw.setColor(Color.green);
    paint.drawBoundaries(impDraw, aliveSpermatozoa);
    setImage();
  }
  @Override
  public void stateChanged(ChangeEvent e) {
    Object auxWho = e.getSource();
    if ((auxWho == sldThreshold)) {
      // Updating threshold value from slider
      threshold = sldThreshold.getValue();
      doSliderRefresh();
    }
  }

  @Override
  public void mouseClicked(MouseEvent e) {}

  @Override
  public void mousePressed(MouseEvent e) {setRawImage();}

  @Override
  public void mouseReleased(MouseEvent e) {drawImage();}

  @Override
  public void mouseEntered(MouseEvent e) {}

  @Override
  public void mouseExited(MouseEvent e) {}
  
}
