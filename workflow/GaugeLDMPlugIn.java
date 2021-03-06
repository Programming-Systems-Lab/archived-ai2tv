package psl.ai2tv.workflow;

import psl.ai2tv.gauge.ClientDesc;
import psl.ai2tv.gauge.FrameDesc;
import psl.ai2tv.gauge.TimeBucket;
import psl.ai2tv.workflow.assets.*;

import org.cougaar.core.domain.RootFactory;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.PrototypeRegistryService;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.ItemIdentificationPG;
import org.cougaar.planning.ldm.asset.NewItemIdentificationPG;

import org.apache.log4j.Logger;

import java.util.Iterator;
import java.util.Vector;

/**
   LDM Plugin for Cougaar. Receives reports from the Gauge and publishes them in the BB.
*/
public class GaugeLDMPlugIn 
  extends ComponentPlugin {

  private static final Logger logger = Logger.getLogger(GaugeLDMPlugIn.class);

  private WFGauge gauge;
  private TimeBucket bucket;

  private ReportAsset repProto;
  private ClientAsset clientProto;
  private RootFactory factory;
  private PrototypeRegistryService prototypeRegistry;

  public void setupSubscriptions() {
    logger.debug("initializing GAUGE");
    // construct asset Protos
    makeAssetProtos();
    //create the gauge
    setupGauge();
		
    //now setup BB subscriptions as needed

  }

  /**
   * Used by the binding utility through reflection to set my DomainService
   */
  public void setDomainService(DomainService domainService) {

    factory = domainService.getFactory();
  }

  public void setPrototypeRegistryService(PrototypeRegistryService prototypeRegistryService) {
    this.prototypeRegistry = prototypeRegistryService;
  }

  public void execute() {
    //logger.debug("--- STARTED excution slice of " + getClass().getRequirement());
    /*
      try {
      wait(250);
      } catch (InterruptedException ie) {
			
      }
    */
    //logger.debug("--- ENDED excution slice of " + getClass().getRequirement());
	
  }

  void setReport(TimeBucket tb) {
    bucket = tb;
    publishReport();
  }
		
  private void setupGauge() {
    //report = (ReportAsset)factory.createInstance("ReportProto");
    gauge = new WFGauge();
    gauge.setFrameFileName(System.getProperty("psl.ai2tv.frameindex"));
    gauge.setLDMHandle(this);
    gauge.setup();
    logger.info("WF process is ready");
  }

  private void publishReport() {
    long sampleTime = bucket.getTime();
    ReportAsset report = (ReportAsset)factory.createInstance("ReportProto");
    report.setItemIdentificationPG(makeIdentificationPG("TimeBucket-" + sampleTime));

    NewBucketPG bucketPG = (NewBucketPG) factory.createPropertyGroup("BucketPG");
    bucketPG.setGroup(new Vector());
    report.setBucketPG(bucketPG);

    //report.getBucketPG().getGroup().clear();
    Iterator allClients = bucket.getGroupState().keySet().iterator();
    ClientAsset ca;

    // DEBUG
    StringBuffer buf = new StringBuffer();

    while(allClients.hasNext()) {
      Object o = allClients.next();
      ClientDesc cd = (ClientDesc) bucket.retrieve(o);
      // if some entry in the Time Bucket is empty, the bucket is not
      // valid and the report is not produced we will wait until the
      // next iteration
      if (cd == null) {
	// logger.warn("clientDesc is null, not publishing");
	return;
      }

      FrameDesc fd = cd.getFrame();
      // logger.debug(cd.getClientID() + " : " + fd.toString());
      ca = (ClientAsset)factory.createInstance("ClientProto");
      ca.setItemIdentificationPG(makeIdentificationPG("AI2TV_Client "+ cd.getClientID()
						      + "@" + sampleTime));
      ca.setClientPG(makeClientPG(cd.getClientID(), "127.0.0.1", cd.getLevel(), cd.getCacheLevel(),
				  cd.getFrameRate(), cd.getBandwidth(), sampleTime, cd.getPrefetchedFrames(), 
				  cd.getReserveFrames(), cd.getAvgDistWF2Client(), cd.getPenalties()
				  ));
      ca.setFramePG(makeFramePG(fd.getLevel(), fd.getNum(), fd.getStart(), fd.getEnd(), 
				fd.getTimeShown(), fd.getTimeOffset(), fd.getTimeDownloaded()
				));
      report.getBucketPG().getGroup().add(ca);
      // [commented out by matias - publishing report only, for AI2TVPlugin] insertAsset(ca);
      bucket.update(o, (ClientDesc)null);

      buf.append("{id=" + cd.getClientID() + ", frame=" + fd.getNum() + "}");
    }

    setSampleTime(report, sampleTime);
    //should insert a new ReportAsset in the BB, not simply update an exisiting one
    logger.debug("inserting report, time=" + sampleTime + ",group=" + buf.toString());
    insertAsset(report);
    bucket.clearValues();
  }
	
  // methods to prepare and set the data that goes into the BB
	
  private void setSampleTime(ReportAsset rep, long t) {
    /*NewBucketPG bPG;
      try {
      bPG = (NewBucketPG)rep.providePropertyGroup(Class.forName("psl.ai2tv.workflow.assets.BucketPG"));
      } catch (ClassNotFoundException e) {
      logger.warn ("ALARM - Couldn't modify ReportAsset state");
      e.printStackTrace();
      return;
      }*/
    NewBucketPG bPG = (NewBucketPG) rep.getBucketPG();
    bPG.setSampleTime(t);
    rep.setBucketPG(bPG);	
  }
	
  /**
   * Utility method to insert a Cougaar <code>Asset</code> onto the Cougaar blackboard.
   * @param someAsset the <code>Asset</code> to be put on the blackboard.
   */
  private void insertAsset (Asset someAsset) {
    boolean inTrans = false;
    try {
      if (blackboard.getSubscriber().isMyTransaction() == false) {
	blackboard.openTransaction();
	inTrans = true;
      }
      blackboard.publishAdd(someAsset);
    } catch (Exception e) {
      synchronized (System.err) {
	logger.warn ("Couldn't add Asset " + someAsset.toString());
	logger.warn("Caught "+e);
	e.printStackTrace();
      }
    }
    finally {
      if (inTrans == true) {
	blackboard.closeTransaction();
	inTrans = false;
      }
    }
  }

	
  /**
     prepare prototypes for Gauge assets
  */
  private void makeAssetProtos() {
    factory.addPropertyGroupFactory(new PropertyGroupFactory());

    // set the Prototypes for ReportAssets
    repProto = (ReportAsset)factory.createPrototype(ReportAsset.class, "ReportProto");
    repProto.setItemIdentificationPG(makeIdentificationPG("Gauge Report Proto"));
    repProto.setBucketPG(makeBucketPG(0, new Vector()));
    prototypeRegistry.cachePrototype("ReportProto", repProto);

    // set the Prototypes for ClientAssets
    clientProto = (ClientAsset)factory.createPrototype(ClientAsset.class, "ClientProto");
    clientProto.setItemIdentificationPG(makeIdentificationPG("AI2TV Client Descriptor Proto"));
    clientProto.setClientPG(makeClientPG("dummyID", "127.0.0.1", 0, 0, 0, 0.0, 0, 0, 0, 0, 0));
    clientProto.setFramePG(makeFramePG(-1, -1, -1, -1, -1, -1, -1));
    prototypeRegistry.cachePrototype("ClientProto", clientProto);
  }
	
  private ItemIdentificationPG makeIdentificationPG (String id){
    NewItemIdentificationPG new_item_id_pg = (NewItemIdentificationPG)factory.createPropertyGroup("ItemIdentificationPG");
    new_item_id_pg.setItemIdentification(id);
    return new_item_id_pg;
  }
	
  private BucketPG makeBucketPG (long t, Vector group) {
    NewBucketPG	bPG = (NewBucketPG)factory.createPropertyGroup("BucketPG");
    bPG.setSampleTime(t);
    bPG.setGroup(group);
		
    return bPG;
  }
	
  private ClientPG makeClientPG (String id, String hostname, int level, 
				 int cacheLevel, int frameRate,
				 double bw, long t, int prefetchedFrames, 
				 int reserveFrames, double avgDist, int penalties) {
    NewClientPG cPG = (NewClientPG)factory.createPropertyGroup("ClientPG");
    cPG.setId(id);
    cPG.setHost(hostname);
    cPG.setLevel(level);
    cPG.setCacheLevel(cacheLevel);
    cPG.setFrameRate(frameRate);
    cPG.setBandwidth(bw);
    cPG.setSampleTime(t);
    cPG.setPrefetchedFrames(prefetchedFrames);
    cPG.setReserveFrames(reserveFrames);
    cPG.setAvgDistWF2Client(avgDist);
    cPG.setPenalties(penalties);
    cPG.setAdapt(false);
    cPG.setNewFrameRate(WFHelperFunctions.DEFAULT_FRAME_RATE);
		
    return cPG;
  }
		
  private FramePG makeFramePG(int hl, int fid, int s, int e, 
			      long timeShown, int timeOffset, long timeDownloaded) {
    NewFramePG fPG = (NewFramePG)factory.createPropertyGroup("FramePG");
    fPG.setLevel(hl);
    fPG.setNum(fid);
    fPG.setStart(s);
    fPG.setEnd(e);
    fPG.setTimeShown(timeShown);
    fPG.setTimeOffset(timeOffset);
    fPG.setTimeDownloaded(timeDownloaded);
    return fPG;
  }
}
