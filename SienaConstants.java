package psl.ai2tv;

/**
 * Shared constants for siena events
 *
 */
public class SienaConstants {

  // these represent the typical client probe frame updates
  public static final String AI2TV_FRAME = "AI2TV_FRAME";
  public static final String CLIENT_ID = "CLIENT_ID";
  public static final String BANDWIDTH = "bandwidth";
  public static final String PROBE_TIME = "probeTime";
  public static final String ABS_TIME_SENT = "absoluteTimeSentStamp";
  public static final String PREV_PROP_DELAY = "previousPropagationDelay";
  public static final String CLIENT_CURRENT_TIME = "clientCurrentTime";
  public static final String LEFTBOUND = "leftbound";
  public static final String RIGHTBOUND = "rightbound";
  public static final String MOMENT = "moment";
  public static final String LEVEL = "level";
  public static final String SIZE = "size";
  public static final String TIME_SHOWN = "timeShown";
  
  public static final String AI2TV_WF_REG = "AI2TV_WF_REG";
  // client ID
  // 

  // these represent the actions of the clients, and are typically sent
  // between peers
  public static final String AI2TV_VIDEO_ACTION = "AI2TV_VIDEO_ACTION";
  public static final String PLAY = "PLAY";
  public static final String STOP = "STOP";
  public static final String PAUSE = "PAUSE";
  public static final String GOTO = "GOTO";
  public static final String NEWTIME = "NEWTIME";

  // these are from the WF to adjust certain clients
  public static final String AI2TV_CLIENT_ADJUST = "AI2TV_CLIENT_ADJUST";
  public static final String CHANGE_LEVEL = "CHANGE_LEVEL";
  public static final String CHANGE_LEVEL_UP = "UP";
  public static final String CHANGE_LEVEL_DOWN = "DOWN";
  public static final String PLAN_FOR = "PLAN_FOR";

  // these are for the WF to gather certain timing stats
  public static final String AI2TV_WF_UPDATE_REQUEST = "AI2TV_WF_UPDATE_REQUEST";
  public static final String AI2TV_WF_UPDATE_REPLY = "AI2TV_WF_UPDATE_REPLY";
  public static final String AI2TV_UPDATE_TIME_SENT = "TIME_SENT";
  public static final String AI2TV_UPDATE_TIME_RCVD = "TIME_RECEIVED";

  public static final String AI2TV_VIDEO_PREFETCH = "";

}
