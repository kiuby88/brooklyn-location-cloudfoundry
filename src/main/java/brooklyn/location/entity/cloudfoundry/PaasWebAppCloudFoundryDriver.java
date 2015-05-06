package brooklyn.location.entity.cloudfoundry;

import brooklyn.entity.basic.Attributes;
import brooklyn.location.cloudfoundry.CloudFoundryPaasLocation;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

import static com.google.common.base.Preconditions.checkNotNull;


public abstract class PaasWebAppCloudFoundryDriver implements PaasWebAppDriver {

    public static final Logger log = LoggerFactory.getLogger(PaasWebAppCloudFoundryDriver.class);

    private final CloudFoundryPaasLocation location;
    CloudFoundryWebAppImpl entity;
    private String applicationPath;
    private String applicationName;
    CloudFoundryClient client;

    public PaasWebAppCloudFoundryDriver(CloudFoundryWebAppImpl entity, CloudFoundryPaasLocation location) {
        this.entity = checkNotNull(entity, "entity");
        this.location = checkNotNull(location, "location");
        init();
    }

    private void init() {
        initApplicationParameters();
    }

    @SuppressWarnings("unchecked")
    private void initApplicationParameters() {
        applicationName = getEntity().getConfig(CloudFoundryWebApp.APPLICATION_NAME);
        applicationPath = getEntity().getConfig(CloudFoundryWebApp.APPLICATION_PATH);
    }

    @Override
    public CloudFoundryWebAppImpl getEntity() {
        return entity;
    }

    @Override
    public CloudFoundryPaasLocation getLocation() {
        return location;
    }

    protected String getApplicationPath(){
        return applicationPath;
    }

    protected String getApplicationName(){
        return applicationName;
    }

    protected CloudFoundryClient getClient(){
        return client;
    }

    public abstract String getBuildPackUrl();

    @Override
    public boolean isRunning() {
        CloudApplication app = client.getApplication(applicationName);
        return (app != null)
                && app.getState().equals(CloudApplication.AppState.STARTED);
    }

    @Override
    public void rebind() {
    }

    @Override
    public void start() {
        preDeploy();
        deploy();
        launch();
        postLaunch();
    }

    public void preDeploy() {
        if (client == null) {
            location.setUpClient();
            client = location.getCloudFoundryClient();
            checkNotNull(client);
        }
    }

    public abstract void deploy();

    protected String inferApplicationDomainUri(String name) {
        String defaultDomainName = client.getDefaultDomain().getName();
        return name + "-domain." + defaultDomainName;
    }

    private void launch() {
        client.startApplication(applicationName);
    }

    private void postLaunch() {
        CloudApplication application = client.getApplication(applicationName);
        String domainUri = application.getUris().get(0);
        entity.setAttribute(Attributes.MAIN_URI, URI.create(domainUri));
        entity.setAttribute(CloudFoundryWebApp.ROOT_URL, domainUri);

        entity.setAttribute(CloudFoundryWebApp.INSTANCES_NUM,
                application.getInstances());
        application.getResources();
        entity.setAttribute(CloudFoundryWebApp.MEMORY,
                application.getMemory());

        entity.setAttribute(CloudFoundryWebApp.DISK,
                application.getDiskQuota());
    }

    @Override
    public void restart() {

    }

    @Override
    public void stop() {
        client.stopApplication(applicationName);
        deleteApplication();
    }

    @Override
    public void deleteApplication() {
        client.deleteApplication(applicationName);
    }

}
