package psl.ai2tv;

/**
 * Shared constants for siena events
 *
 */
public class SienaConstants {

  // these represent the typical client probe frame updates
  public static final String AI2TV_FRAME = "AI2TVFrame";
  public static final String AI2TV_FRAME_MISSED = "AI2TVFrameMissed";
  public static final String CLIENT_ID = "ClientID";
  public static final String BANDWIDTH = "bandwidth";
  public static final String PROBE_TIME = "probeTime";
  public static final String ABS_TIME_SENT = "absoluteTimeSentStamp";
  public static final String PREV_PROP_DELAY = "previousPropagationDelay";
  public static final String CLIENT_CURRENT_TIME = "clientCurrentTime";
  public static final String LEFTBOUND = "leftbound";
  public static final String RIGHTBOUND = "rightbound";
  public static final String MOMENT = "moment";
  public static final String LEVEL = "level";
  public static final String FRAME_RATE = "frameRate";
  public static final String CACHE_LEVEL = "cacheLevel";
  public static final String SIZE = "size";
  public static final String CLIENT_RESERVE_FRAMES = "clientReserveFrames";  
  public static final String PREFETCHED_FRAMES = "prefetchedFrames";  
  public static final String TIME_SHOWN = "timeShown";
  public static final String TIME_OFFSET = "timeOffset";
  public static final String TIME_DOWNLOADED = "timeDownloaded";

  // these represent the actions of the clients, and are typically sent
  // between peers
  public static final String AI2TV_CLIENT_SHUTDOWN = "clientShutdown";
  public static final String AI2TV_VIDEO_ACTION = "AI2TVVideoAction";
  public static final String PLAY = "play";
  public static final String STOP = "stop";
  public static final String PAUSE = "pause";
  public static final String GOTO = "goto";
  public static final String NEWTIME = "newtime";

  // these are from the WF to adjust certain clients
  public static final String AI2TV_CLIENT_ADJUST = "AI2TVClientAdjust";
  public static final String CHANGE_CLIENT_LEVEL = "changeClientLevel";
  public static final String CHANGE_CACHE_LEVEL = "changeCacheLevel";
  public static final String CHANGE_FRAME_RATE = "changeFrameRate";
  public static final String JUMP_TO = "jumpTo";

  // these are for the WF to gather certain timing stats
  public static final String AI2TV_WF_REG = "AI2TVWFReg";
  public static final String AI2TV_WF_UPDATE_REQUEST = "AI2TVWFUpdateRequest";
  public static final String AI2TV_WF_UPDATE_REPLY = "AI2TVWFUpdateReply";
  public static final String AI2TV_UPDATE_TIME_SENT = "timeSent";
  public static final String AI2TV_UPDATE_TIME_RCVD = "timeReceived";

  public static final String AI2TV_VIDEO_PREFETCH = "";

}
