/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package brooklyn.entity.cloudfoundry.webapp;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.cloudfoundry.PaasEntityCloudFoundryDriver;
import brooklyn.entity.cloudfoundry.services.CloudFoundryService;
import brooklyn.location.cloudfoundry.CloudFoundryPaasLocation;
import brooklyn.util.text.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;


public abstract class PaasWebAppCloudFoundryDriver extends PaasEntityCloudFoundryDriver
        implements PaasWebAppDriver {

    public static final Logger log = LoggerFactory.getLogger(PaasWebAppCloudFoundryDriver.class);

    private String applicationPath;
    private String applicationName;

    public PaasWebAppCloudFoundryDriver(CloudFoundryWebAppImpl entity,
                                        CloudFoundryPaasLocation location) {
        super(entity, location);
    }

    @Override
    protected void init() {
        super.init();
        initApplicationParameters();
    }

    @SuppressWarnings("unchecked")
    private void initApplicationParameters() {
        applicationName = getEntity().getConfig(CloudFoundryWebApp.APPLICATION_NAME);
        applicationPath = getEntity().getConfig(CloudFoundryWebApp.APPLICATION_PATH);
    }

    @Override
    public CloudFoundryWebAppImpl getEntity() {
        return (CloudFoundryWebAppImpl) super.getEntity();
    }

    protected String getApplicationPath(){
        return applicationPath;
    }

    protected String getApplicationName(){
        return applicationName;
    }

    public abstract String getBuildpack();

    @Override
    public boolean isRunning() {
        CloudApplication app = getClient().getApplication(applicationName);
        return (app != null)
                && app.getState().equals(CloudApplication.AppState.STARTED);
    }

    @Override
    public void start() {
        super.start();

        preDeploy();
        deploy();
        preLaunch();
        launch();
        postLaunch();
    }

    public void preDeploy() {}

    public abstract void deploy();

    public void preLaunch() {
        bindServices();
        
        configureEnv();
    }

    protected void configureEnv() {
        setEnv(getEntity().getConfig(CloudFoundryWebApp.ENV));
    }

    private void bindServices() {
        List<Entity> config = getEntity().getConfig(CloudFoundryWebApp.NAMED_SERVICES);
        if (config != null) {
            for (Entity serviceEntityId : config) {
                bindService(serviceEntityId);
            }
        }
    }

    /*TODO RENAME Method. It could be represent that the service is bound and the service
     operation is called*/
    private void bindService(Entity rawEntity){

        CloudFoundryService cloudFoundryService;
        if (rawEntity instanceof CloudFoundryService){
            cloudFoundryService = (CloudFoundryService) rawEntity;
        
            String serviceName = cloudFoundryService
                    .getConfig(CloudFoundryService.SERVICE_INSTANCE_NAME);

            if (Strings.isEmpty(serviceName)){
                log.error("Trying to get service instance name from {}, but getting null",
                        cloudFoundryService);
            }
            bindingServiceToEntity(serviceName);

            //TODO move to configureBoundService
            setCredentialsOnService(cloudFoundryService);

            configureBoundService(serviceName);

            //TODO move to configureBoundService
            cloudFoundryService.operation(getEntity());

            
        } else {
            log.error("The service entity {} is not available from the application {}",
                    new Object[]{rawEntity, getEntity()});

            throw new NoSuchElementException("No entity matching id " + rawEntity.getId() +
                    " in Management Context "+getEntity().getManagementContext()+
                    " during entity service binding "+getEntity().getId());
        }
    }

    private void bindingServiceToEntity(String serviceId) {
        getClient().bindService(applicationName, serviceId);
        log.info("The service {} was bound correctly to the application {}",
                new Object[]{serviceId, applicationName});

        updateVariableEnvironmentSensors();
    }

    private void updateVariableEnvironmentSensors(){
        Map<String, Object> env = getClient().getApplicationEnvironment(applicationName);
        JsonObject envTree = new Gson().toJsonTree(env).getAsJsonObject();
        getEntity().setAttribute(CloudFoundryWebApp.VCAP_SERVICES,
                envTree.getAsJsonObject("system_env_json")
                        .getAsJsonObject("VCAP_SERVICES").toString());
    }

    protected void setCredentialsOnService(CloudFoundryService service) {
        service.setBindingCredentialsFromApp(getEntity());
    }
    
    protected void configureBoundService(String serviceId){
        if(serviceIsBoundToCloudApplication(serviceId)){
            updateBoundServiceToEntity(serviceId);
            //TODO configuration of the service, for example create the tables of a database
        }
    }

    //TODO it ma be renamed to updateBoundServicesSensor
    protected void updateBoundServiceToEntity(String serviceId){
        if(serviceIsBoundToCloudApplication(serviceId)){
            List<String> currentBoundServices = getEntity()
                    .getAttribute(CloudFoundryWebApp.BOUND_SERVICES);
            currentBoundServices.add(serviceId);
            getEntity().setAttribute(CloudFoundryWebApp.BOUND_SERVICES, currentBoundServices);
        }
    }

    /**
     * Return if a service is bound to the cloud application. Checks the running application in
     * the cloud.
     * @param serviceId
     * @return
     */
    public boolean serviceIsBoundToCloudApplication(String serviceId){
        return getClient().getApplication(applicationName)
                .getServices().contains(serviceId);
    }

    public void launch() {
        getClient().startApplication(applicationName);
    }

    public void postLaunch() {
        CloudApplication application = getClient().getApplication(applicationName);
        String domainUri = application.getUris().get(0);
        getEntity().setAttribute(Attributes.MAIN_URI, URI.create(domainUri));
        getEntity().setAttribute(CloudFoundryWebApp.ROOT_URL, domainUri);

        getEntity().setAttribute(CloudFoundryWebApp.INSTANCES_NUM,
                application.getInstances());
        application.getResources();
        getEntity().setAttribute(CloudFoundryWebApp.MEMORY,
                application.getMemory());

        getEntity().setAttribute(CloudFoundryWebApp.DISK,
                application.getDiskQuota());
    }

    @Override
    public void restart() {
        // TODO: complete
    }

    @Override
    public void stop() {
        getClient().stopApplication(applicationName);
        deleteApplication();
    }

    @Override
    public void deleteApplication() {
        getClient().deleteApplication(applicationName);
    }

    protected String inferApplicationDomainUri(String name) {
        String defaultDomainName = getClient().getDefaultDomain().getName();
        return name + "-domain." + defaultDomainName;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public void setEnv(String key, String value) {
        CloudApplication app = getClient().getApplication(applicationName);

        // app.setEnv() replaces the entire set of variables, so we need to add it externally.
        Map envAsMap = app.getEnvAsMap(); 
        envAsMap.put(key, value);
        app.setEnv(envAsMap);
        getClient().updateApplicationEnv(applicationName, envAsMap);
    }
    
    @SuppressWarnings("unchecked")
    void setEnv(Map<String, String> envs) {
        CloudApplication app = getClient().getApplication(applicationName);

        // app.setEnv() replaces the entire set of variables, so we need to add it externally.
        Map oldEnv = app.getEnvAsMap();
        oldEnv.putAll(envs);
        getClient().updateApplicationEnv(applicationName, oldEnv);
    }
}
