package psl.ai2tv.workflow;

import org.cougaar.domain.planning.ldm.asset.Asset;

public class ClientAssetAdapter extends Asset {
	private static String version = "0.1";
	
  /**
   * Create a new ClientAssetAdapter
   */
  public ClientAssetAdapter() {
    super();
  }

  /**
   * Create a new ClientAssetAdapter from a Prototype
   * @param prototype the Asset's prototype
   */
  public ClientAssetAdapter(Asset prototype) {
    super(prototype);
  }

  /**
   * returns the version for this <code>ExecAgentAsset</code>
   * @return version ID
   */
  public String getVersion() {
    return version;
  }

}