package psl.ai2tv.workflow;

import java.util.Iterator;
import java.util.Vector;
import java.util.Set;

import org.cougaar.core.cluster.IncrementalSubscription;
import org.cougaar.core.plugin.SimplePlugIn;
import org.cougaar.domain.planning.ldm.plan.*;
import org.cougaar.domain.planning.ldm.asset.*;
import org.cougaar.util.UnaryPredicate;

import psl.ai2tv.gauge.*;
import psl.ai2tv.workflow.assets.*;

/**
	LDM Plugin for Cougaar. Receives reports from the Gauge and publishes them 
	in the BB.
*/
public class GaugeLDMPlugIn 
	extends SimplePlugIn {
	
	private WFGauge gauge;
	private TimeBucket bucket;
	private ReportAsset report;
	
	private ReportAsset repProto;
	private ClientAsset clientProto;
	
	public void setupSubscriptions() {
		System.out.println("initializing GAUGE");
		// construct asset Protos
		makeAssetProtos();
		//create the gauge
		setupGauge();
		
		//now setup BB subscriptions as needed

	}
	
	public void execute() {
		System.out.println("--- STARTED excution slice of " + getClass().getName());
		/*
		try {
			wait(250);
		} catch (InterruptedException ie) {
			
		}
		*/
		System.out.println("--- ENDED excution slice of " + getClass().getName());
	
	}

	void setReport(TimeBucket tb) {
			bucket = tb;
			publishReport();
	}
		
	private void setupGauge() {
		//report = (ReportAsset)theLDMF.createInstance("ReportProto");
		gauge = new WFGauge();
		gauge.setFrameFileName(System.getProperty("psl.ai2tv.frameindex"));
		gauge.setLDMHandle(this);
		gauge.setup();
	}

	private void publishReport() {
		long sampleTime = bucket.getTime();
		report = (ReportAsset)theLDMF.createInstance("ReportProto");
		report.setItemIdentificationPG(makeIdentificationPG("TimeBucket-" + sampleTime));
		//report.getBucketPG().getGroup().clear();
		Iterator allClients = bucket.getGroupState().keySet().iterator();
		ClientAsset ca;
		
		while(allClients.hasNext()) {
			Object o = allClients.next();
			ClientDesc cd = (ClientDesc) bucket.retrieve(o);	
			// if some entry in the Time Bucket is empty, the bucket is not valid
			// and the report is nto produced
			// we will wait until the next
			if (cd == null)
				return;
				
			System.out.println("--- Time " + sampleTime + " Updating report");
			FrameDesc fd = cd.getFrame();
			System.out.println(cd.getClientID() + " : " + fd.toString());
			ca = (ClientAsset)theLDMF.createInstance("ClientProto");
			ca.setItemIdentificationPG(makeIdentificationPG("AI2TV_Client "+ cd.getClientID()
										+ "@" + sampleTime));
			ca.setClientPG(makeClientPG(cd.getClientID(), "127.0.0.1", cd.getBandwidth(), sampleTime));
			ca.setFramePG(makeFramePG(fd.getLevel(), fd.getNum(), fd.getStart(), fd.getEnd()));
			report.getBucketPG().getGroup().add(ca);
			insertAsset(ca);
			//bucket.update(o, (ClientDesc)null);
		}

		setSampleTime(report, sampleTime);
		//should insert a new ReportAsset in the BB, not simply update an exisiting one
		insertAsset(report);			
		bucket.clearValues();
	}
	
	// methods to prepare and set the data that goes into the BB
	
	private void setSampleTime(ReportAsset rep, long t) {
		NewBucketPG bPG;
		try {
			bPG = (NewBucketPG)rep.providePropertyGroup(Class.forName("psl.ai2tv.workflow.assets.BucketPG"));
		} catch (ClassNotFoundException e) {
			System.err.println ("ALARM - Couldn't modify ReportAsset state");
			e.printStackTrace();
			return;
		}
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
			if (getBlackboardService().getSubscriber().isMyTransaction() == false) {
      			openTransaction();
      			inTrans = true;
      		}
      		getSubscriber().publishAdd(someAsset);
      	} catch (Exception e) {
      		synchronized (System.err) {
      			System.err.println ("Couldn't add Asset " + someAsset.toString());
        		System.err.println("Caught "+e);
        		e.printStackTrace();
        	}
      	}
   		finally {
   			if (inTrans == true) {
		      	closeTransaction();
		      	inTrans = false;
		    }
    	}
	}

	
	/**
		prepare prototypes for Gauge assets
	*/
	private void makeAssetProtos() {
		theLDMF.addPropertyGroupFactory(new psl.ai2tv.workflow.assets.PropertyGroupFactory());	
		
		// set the Prototypes for ReportAssets	
		repProto = (ReportAsset)theLDMF.createPrototype(psl.ai2tv.workflow.assets.ReportAsset.class, "ReportProto");
		repProto.setItemIdentificationPG(makeIdentificationPG("Gauge Report Proto"));
		repProto.setBucketPG(makeBucketPG(0, new Vector()));
		theLDM.cachePrototype("ReportProto", repProto);

		// set the Prototypes for ClientAssets	
		clientProto = (ClientAsset)theLDMF.createPrototype(psl.ai2tv.workflow.assets.ClientAsset.class, "ClientProto");
		clientProto.setItemIdentificationPG(makeIdentificationPG("AI2TV Client Descriptor Proto"));
		clientProto.setClientPG(makeClientPG("dummyID", "127.0.0.1", 0.0, 0));
		clientProto.setFramePG(makeFramePG(-1, -1, -1, -1));
		theLDM.cachePrototype("ClientProto", clientProto);
	}
	
	private ItemIdentificationPG makeIdentificationPG (String id){
		NewItemIdentificationPG new_item_id_pg = (NewItemIdentificationPG)theLDMF.createPropertyGroup("ItemIdentificationPG");
		new_item_id_pg.setItemIdentification(id);	
		return new_item_id_pg;
	}
	
	private BucketPG makeBucketPG (long t, Vector group) {
		NewBucketPG	bPG = (NewBucketPG)theLDMF.createPropertyGroup("BucketPG");
		bPG.setSampleTime(t);
		bPG.setGroup(group);
		
		return bPG;
	}
	
	private ClientPG makeClientPG (String id, String hostname, double bw, long t) {
		NewClientPG cPG = (NewClientPG)theLDMF.createPropertyGroup("ClientPG");
		cPG.setId(id);
		cPG.setHost(hostname);
		cPG.setBandwidth(bw);
		cPG.setSampleTime(t);
		
		return cPG;
	}
		
	private FramePG makeFramePG(int hl, int fid, int s, int e) {
		NewFramePG fPG = (NewFramePG)theLDMF.createPropertyGroup("FramePG");
		fPG.setLevel(hl);
		fPG.setNum(fid);
		fPG.setStart(s);
		fPG.setEnd(e);
		
		return fPG;
		
	}
}