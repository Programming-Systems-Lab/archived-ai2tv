package psl.ai2tv.workflow;

import psl.ai2tv.workflow.assets.*;
import psl.workflakes.littlejil.*;
import psl.workflakes.littlejil.assets.ExecClassAgentAsset;
import psl.workflakes.littlejil.assets.NewExecutorPG;
import psl.workflakes.littlejil.assets.NewClassPG;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.domain.RootFactory;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.PrototypeRegistryService;
import org.cougaar.util.UnaryPredicate;

import laser.littlejil.Diagram;
import laser.littlejil.Program;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * This plugin subscribes to ReportAssets published by the GaugeLDMPlugin, and for each report it
 * "registers" the resources appropriately in the resources table and publishes a new Ai2TV LittleJIL diagram.
 * @author matias
 */
public class AI2TVPlugin extends ComponentPlugin {

    private static final Logger logger = Logger.getLogger(TaskExpanderPlugin.class);
    private IncrementalSubscription resourceTableSubscription;
    private IncrementalSubscription reportAssetSubscription;

    private RootFactory factory;

    byte[] diagramByteArray;    // cache of the serialized LittleJIL ai2tv diagram
    private boolean publishedExecAgentAsset;

    private static class ResourceTablePredicate implements UnaryPredicate {
        public boolean execute(Object o) {
            return (o instanceof LittleJILResourceTable);
        }
    }

    private static class ReportAssetPredicate implements UnaryPredicate {
        public boolean execute(Object o) {
            return (o instanceof ReportAsset);
        }
    }

    public AI2TVPlugin() throws IOException {

        // read in the serialized ai2tv diagram... we will instantiate a new one each time we run the workflow
        // we keep the serialized byte array in memory so as not to have to read from the file every time
        String filename = System.getProperty("ai2tv.diagram");
        File file = new File(filename);
        if (!file.exists()) {
            throw new FileNotFoundException(filename);
        }

        diagramByteArray = new byte[(int) file.length()];

        BufferedInputStream bufIn = new BufferedInputStream(new FileInputStream(file));
        bufIn.read(diagramByteArray);
        bufIn.close();

        logger.info("Loaded ai2tv diagram");

        // now register TaskExpanderPlugin listener
        TaskExpanderPlugin.addListener(new TaskExpanderListener());

    }

    /**
     * Used by the binding utility through reflection to set my DomainService
     */
    public void setDomainService(DomainService domainService) {
        factory = domainService.getFactory();
    }

    public void setPrototypeRegistryService(PrototypeRegistryService prototypeRegistryService) {

        factory.addPropertyGroupFactory(new PropertyGroupFactory());

        // set the Prototypes for ClientAssets
        ClientAsset clientProto = (ClientAsset) factory.createPrototype(ClientAsset.class, "ClientProto");
        prototypeRegistryService.cachePrototype("ClientProto", clientProto);

        // set the Prototypes for ExecClassAgentAssets
        factory.addPropertyGroupFactory(new psl.workflakes.littlejil.assets.PropertyGroupFactory());
        {
            ExecClassAgentAsset prototype = (ExecClassAgentAsset)
                    factory.createPrototype(ExecClassAgentAsset.class, "ExecClassAgentProto");

            prototypeRegistryService.cachePrototype("ExecClassAgent", prototype);
        }

    }

    public void setupSubscriptions() {

        resourceTableSubscription = (IncrementalSubscription) blackboard.subscribe(new ResourceTablePredicate());
        reportAssetSubscription = (IncrementalSubscription) blackboard.subscribe(new ReportAssetPredicate());


    }

    public void execute() {

        LittleJILResourceTable resourceTable;
        if (resourceTableSubscription.size() == 0) {
            // create a new resource table
            resourceTable = new LittleJILResourceTable();
            blackboard.publishAdd(resourceTable);
        } else {
            resourceTable = (LittleJILResourceTable) resourceTableSubscription.first();
        }

        for (Enumeration reports = reportAssetSubscription.getAddedList(); reports.hasMoreElements();) {

            ReportAsset reportAsset = (ReportAsset) reports.nextElement();

            logger.info("got new ReportAsset");

            /*BucketPG bucketPG = reportAsset.getBucketPG();
            logger.debug("group=" + bucketPG.getGroup());*/

            PluginUtil.Timing.addTimestamp("got report");

            // if we haven't published the ExecAgent asset yet, do so now
            if (!publishedExecAgentAsset) {
                ExecClassAgentAsset asset = (ExecClassAgentAsset) factory.createInstance("ExecClassAgent");
                NewExecutorPG executorPG = (NewExecutorPG) factory.createPropertyGroup("ExecutorPG");
                executorPG.setCapabilities("any");

                NewClassPG classPG = (NewClassPG) factory.createPropertyGroup("ClassPG");
                //classPG.setClassName("psl.workflakes.littlejil.TaskExecutorInternalPlugin$DummyExecutableTask");
                classPG.setClassName("psl.ai2tv.workflow.WFHelperFunctions");
                asset.setExecutorPG(executorPG);
                asset.setClassPG(classPG);

                blackboard.publishAdd(asset);

                publishedExecAgentAsset = true;

            }

            // instantiate a new ai2tv LittleJIL diagram
            ObjectInputStream objIn = null;
            try {
                objIn = new ObjectInputStream(new ByteArrayInputStream(diagramByteArray));

                Program program = (Program) objIn.readObject();
                Diagram diagram = program.getRootDiagram();

                // add resources for this diagram

                Vector group = reportAsset.getBucketPG().getGroup();
                if (group == null) {
                    logger.warn("group in reportAsset.getBucketPG() is null!");
                    continue;
                }

                resourceTable.addResource(diagram, "clients", group);

                // for base client we just instantiate a new ClientAsset
                ClientAsset baseClient = (ClientAsset) factory.createInstance("ClientProto");
                baseClient.setClientPG(factory.createPropertyGroup("ClientPG"));
                baseClient.setFramePG(factory.createPropertyGroup("FramePG"));

                resourceTable.addResource(diagram, "baseClient", baseClient);

                logger.debug("publishing diagram");
                blackboard.publishAdd(diagram);

            } catch (Exception e) {
                logger.warn("Could not instantiate new diagram: " + e);
            }

        }

    }

    class TaskExpanderListener extends TaskExpanderPlugin.Listener {

        public void taskPublished(String taskName) {
            logger.debug(taskName + " published, adding timestamp");
            PluginUtil.Timing.addTimestamp("publish" + taskName);
        }

        public void leafTaskPublished(String taskName) {
            logger.debug(taskName + " published, adding timestamp");
            PluginUtil.Timing.addTimestamp("publish" + taskName);
        }

        public void taskFinished(String taskName) {
            logger.debug(taskName + " finished, adding timestamp");
            PluginUtil.Timing.addTimestamp("end" + taskName);

            if (taskName.equals("ROOT")) {
                PluginUtil.Timing.newRow();
            }
        }
    }

    public static class DummyExecutableTask implements ExecutableTask {

        public void execute(String method, Hashtable inParams, Hashtable outParams) throws Exception {

            logger.info("executing method " + method);

            if (method.equals("FindBase")) {

                Vector clients = (Vector) inParams.get("clients");
                ClientAsset baseClient = (ClientAsset) inParams.get("baseClient");

                logger.debug("FindBase got clients " + clients + ", base=" + baseClient);

            } else if (method.equals("EvaluateClient")) {

                ClientAsset baseClient = (ClientAsset) inParams.get("baseClient");
                ClientAsset client = (ClientAsset) inParams.get("clients");

                logger.debug("EvaluateClient got client=" + client + ", base=" + baseClient);

            } else if (method.equals("AdaptClient")) {

                ClientAsset client = (ClientAsset) inParams.get("clients");
                logger.debug("AdaptClients got client: " + client);

            } else {

                logger.debug("unknown method " + method);
            }

        }

    }
}




