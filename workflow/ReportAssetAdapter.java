package psl.ai2tv.workflow;

import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.domain.planning.ldm.asset.PropertyGroup;

public class ReportAssetAdapter extends Asset {
	private static String version = "0.1";
	
	/** holds on to the set of sampled clients*/
	protected ClientAssetAdapter[] clients;
	
  /**
   * Create a new ReportAssetAdapter
   */
  public ReportAssetAdapter() {
    super();
    clients = new ClientAssetAdapter[]{};
  }

  /**
   * Create a new ReportAssetAdapter from a Prototype
   * @param prototype the Asset's prototype
   */
  public ReportAssetAdapter(Asset prototype) {
    super(prototype);
    clients = new ClientAssetAdapter[]{};
  }

  /**
   * returns the version for this <code>ExecAgentAsset</code>
   * @return version ID
   */
  public String getVersion() {
    return version;
  }
  
   /**
   * Creates and returns a clean copy of the PropertyGroup specified
   * @param c the Class of a certain PropertyGroup
   * @return an instance of the PropertyGroup class
   *
   * Note: verification that c describes a PropertyGroup class is delegated to the generateDefaultPG() 
   * method of class AssetSkeletonBase
   */
  public PropertyGroup providePropertyGroup (Class c) {
  		PropertyGroup npg = generateDefaultPG(c);
  		if (npg == null)
  			System.err.println ("Couldn't create a PropertyGroup of class " + c.getName());
  		return npg;
  		
  }

}