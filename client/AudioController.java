/*
 * @(#)AudioController.java
 *
 * Copyright (c) 2003: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 *
 * Copyright (c) 2003: @author Dan Phung
 * Last modified by: Dan Phung (dp2041@cs.columbia.edu)
 *
 * CVS version control block - do not edit manually
 *  $RCSfile$
 *  $Revision$
 *  $Date$
 *  $Source$
 */
package psl.ai2tv.client;

import javax.sound.sampled.*;
import java.io.*;
/**
 * Controls the playing of one audio file.  Supports play, stop,
 * pause, goto and change gain (volume) methods.
 *
 * @version	$Revision$
 * @author	Dan Phung (dp2041@cs.columbia.edu)
 */

public class AudioController implements Runnable{
  // public static int threadCount = 0;
  private final static int WAIT_INTERVAL = 333;

  // for input 
  private AudioFormat audioFormat;
  private AudioFileFormat audioFileFormat;
  private AudioInputStream audioStream;
  private RandomAccessFile audioFile;
  private int bufferSize;
  private long audioFileLength;

  // for output
  Thread playThread;
  private SourceDataLine audioLine;
  private DataLine.Info audioInfo;

  // for playing
  private boolean stopped;
  private boolean paused;
  private boolean isActive;
  private boolean isRunning;

  public AudioController(){
    System.out.println("creating audio controller");
    audioFormat = null;
    audioFileFormat = null;
    audioStream = null;
    audioFile = null;
    bufferSize = 0;
    audioFileLength = 0;

    playThread = null;    
    audioLine = null;
    audioInfo = null;

    isActive = false;
    isRunning = false;
    stopped = false;
    paused = false;
  }

  public boolean initializeAudioLine(){
    if (audioFormat == null){
      System.err.println("Error, audio format must be initialized first");
    }

    audioInfo = new DataLine.Info(SourceDataLine.class,
				  audioFormat); // audioFormat is an AudioFormat object

    if (!AudioSystem.isLineSupported(audioInfo)) {
      System.err.println("AudioSystem line is not supported");
      return false;
    }

    // Obtain and open the line.
    try {
      audioLine = (SourceDataLine) AudioSystem.getLine(audioInfo);
      // audioLine.open(audioFormat);
    } catch (LineUnavailableException ex) {
      System.err.println("Error opening audio line with info: " + audioInfo);
      return false;
    }

    playThread = new Thread(this);
    return true;
  }

  public boolean initializeAudioFile(String filename, int bufferSize){

    File file = new File(filename);

    if (!file.exists() || !file.canRead() || !file.isFile()){
      System.err.println("Error, audio file does not exists or is unreadable");
      return false;
    }
    try {
      audioFile = new RandomAccessFile(filename,"r"); // open in ReadOnly mode
    } catch (FileNotFoundException e){
      System.err.println("Error, audio file not found: " + e);
      return false;
    }

    try {
      audioFileFormat = AudioSystem.getAudioFileFormat(file);
      audioFormat = audioFileFormat.getFormat();
      audioFileLength = audioFile.length();
    } catch (UnsupportedAudioFileException e) {
      System.err.println("Audio file audioFormat for " + filename + " not supported.");
      return false;
    } catch (IOException e) {
      System.err.println("Caught IOException initializing audio file: " + e);
      e.printStackTrace();
      return false;
    }

    this.bufferSize = bufferSize;
    return true;
  }

  public void setVolume(float volume) {
    // System.out.println("setVolume: " + volume);
    if(audioLine == null){
      System.err.println("Error, audioLine is not initialized.");
      return;
    }

    /*
    if (volume > 1 || volume < 0){
      System.err.println("Error, volume must be a value between 0 and 1.");
      return;
    }
    */

    FloatControl gainControl = 
      (FloatControl) audioLine.getControl(FloatControl.Type.MASTER_GAIN);
    // float dB = (float) 
    // (Math.log(volume==0.0?0.0001:volume)/Math.log(10.0)*20.0);
    // gainControl.setValue(dB);
    // System.out.println("current level: " + gainControl.getValue());
    gainControl.setValue(volume);
  }


  /** 
   * this may not be the best approach as it may use up all the
   * available threads.  Should keep an eye out on this method.
   */
  public void play() {
    System.out.println("<AudioController> play");
    System.out.println("isActive: " + isActive);
    System.out.println("playThread.isAlive(): " + playThread.isAlive());
    if (!isActive && playThread != null){
      isActive = true;
      System.out.println("calling playThread.start();");
      playThread.start();
    } 

    if (paused){
      paused = false;
    } 
    if (stopped){
      stopped = false;
    }
  }

  public void pause(){
    System.out.println("<AudioController> pause");
    // toggle pause
    if (paused){
      paused = false;
      
    } else{
      paused = true;
      audioLine.flush();
      // 999
      // audioLine.stop();
    }
  }

  public void stop(){
    System.out.println("<AudioController> stop");
    if (isActive){
      stopped = true;
      // 999
      // audioLine.stop();
      audioLine.flush(); // flush out anything that was in there
      try {
	audioFile.seek((int) 0);
      } catch (IOException e){
	System.err.println("Error in stopPlaying: " + e);
      }
    }
  }

  public void gotoTimeSeconds(long time){
    System.out.println("<AudioController> gotoTimeSeconds: " + time);
    if (audioFile == null){
      System.err.println("Error, audioFile is null");
      return;
    }

    long timeIndicator = secondsToBytes(time);
    // System.out.println("" + timeIndicator + "vs\n" + audioFileLength);
    if (timeIndicator > audioFileLength){
      System.err.println("Error, cannot go past the end of the file");
      return;
    }
    
    try {
      audioLine.flush(); // flush out whatever might have been there
      audioFile.seek(timeIndicator);
    } catch (IOException e){
      System.err.println("IOException in gotoTimeSeconds: " + e);
      return;
    }
  }

  public void shutdown() {
    isActive = false;
    audioInfo = null;

    if (audioStream != null){
      try{
	audioStream.close();
      } catch(IOException e){
	System.err.println("Error closing audioStream: " + e);
      }
    }

    if (audioFile != null){
      try{
	audioFile.close();
      } catch(IOException e){
	System.err.println("Error closing audioFile: " + e);
      }
    }

    audioLine.close();

    // 999 we shouldn't have to do this, there's a resource that isn't
    // properly shutdown...
    System.exit(0); 
  }

  // timeIndicator = time to skip to(sec) * frame rate * 4 bytes/frame
  // ie. 60 seconds = 10584000
  public long secondsToBytes(long sec){
    long timeIndicator = -1;
    if (audioFormat == null){
      System.err.println("Error, audio format unknown");
      return timeIndicator;
    }

    timeIndicator = sec *  (long) audioFormat.getFrameRate() * audioFormat.getFrameSize();
    return timeIndicator;    
  }

  public void printAudioFileInfo(){
    if (audioFile == null || audioFormat == null){
      System.err.println("Error, initialize audioFile and audioFormat first.");
      return;
    }
    try {
      System.out.println("audio file length: " + audioFile.length());
    } catch (IOException e){
      System.err.println("Error, cannot get audio file length");
    }
    System.out.println("audio format frame size: " + audioFormat.getFrameSize());
    System.out.println("audio file format frame length: " + audioFileFormat.getFrameLength());
    System.out.println("audio file format frame rate: " + audioFormat.getFrameRate());
    System.out.println("got the audioFormat: " + audioFormat);
  }

  /**
   * associated w/ play()
   */
  public void run(){
    // System.out.print("thread #" + ++threadCount);
    System.out.print("<AudioController> playThread.run()");
    if (audioLine == null){
      System.err.println("Error, audio line is false, initializeAudioLine first.");
      return;
    }

    try {
      audioLine.open(audioFormat);
      audioLine.start();
    } catch (javax.sound.sampled.LineUnavailableException e){
      System.err.println("Error opening or starting audioLine: " + e);
      return;
    }

    // isActive = audioLine.isActive(); // this is only active once the line receives data
    // isRunning = audioLine.isRunning(); // active once start is invoked

    isActive = true;
    stopped = false;
    paused = false;
    int offset = 0;
    int totalToRead;
    int numBytesRead = 0;
    byte[] audioBuffer = new byte[bufferSize];
    // long total = 0;
    // while(total < audioFileLength && !stopped && !paused) {
    while (isActive){
      while (paused || stopped){
	try {
	  Thread.sleep(WAIT_INTERVAL);
	} catch (InterruptedException e){
	  System.err.println("Warning, interrupted exception: " + e);
	}
      }

      try {
	numBytesRead = audioFile.read(audioBuffer, offset, bufferSize);
      } catch (IOException e){
	System.err.println("Error, reading audio: " + e);
	return;	
      }

      // System.out.println("num bytes read: " + numBytesRead);
      if (numBytesRead == -1) break;
      // total += numBytesRead;
      audioLine.write(audioBuffer, 0, (numBytesRead - offset));
    }

    return;
  }

  public static void main(String[] args){
    if (args.length < 2){
      System.out.println("usage: java AudioController <audioFile> <bufferSize>");
      System.exit(0);
    }

    String filename = args[0];
    int bufferSize = Integer.parseInt(args[1]);
    
    AudioController audioController = new AudioController();
    if (audioController.initializeAudioFile(filename, bufferSize) && 
	audioController.initializeAudioLine()){
      // System.out.println("filelength: " + audioController.secondsToBytes(1068));
      // audioController.gotoTimeSeconds(63);
      audioController.play();
    }
  }
}
