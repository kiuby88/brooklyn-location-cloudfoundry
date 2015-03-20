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
package brooklyn.location.entity.cloudfoundry.java;

import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.EntityLocal;
import brooklyn.location.Location;
import brooklyn.location.cloudfoundry.CloudFoundryPaasLocation;
import brooklyn.util.ResourceUtils;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.Staging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;


public class JavaPaasWebAppCloudFoundryDriver implements JavaPaasWebAppDriver {


    public static final Logger log = LoggerFactory.getLogger(JavaPaasWebAppCloudFoundryDriver.class);
    //Both of them could be a config KEY!!!!!!!!!!
    private static final int DEFAULT_MEMORY = 512; // MB
    private final String BUILDPACK_URL = "https://github.com/cloudfoundry/java-buildpack.git";

    private final CloudFoundryPaasLocation location;
    private final ResourceUtils resource;
    JavaCloudFoundryPaasWebAppImpl entity;
    private String applicationPath;
    private String applicationName;
    CloudFoundryClient client;

    public JavaPaasWebAppCloudFoundryDriver(JavaCloudFoundryPaasWebAppImpl entity, CloudFoundryPaasLocation location) {
        this.entity = checkNotNull(entity, "entity");
        this.location = checkNotNull(location, "location");
        this.resource = ResourceUtils.create(entity);
        init();
    }

    private void init() {
        initApplicationParameters();
    }

    @SuppressWarnings("unchecked")
    private void initApplicationParameters() {
        applicationName = getEntity().getConfig(JavaCloudFoundryPaasWebApp.APPLICATION_NAME);
        applicationPath = getEntity().getConfig(JavaCloudFoundryPaasWebApp.APPLICATION_PATH);
    }

    @Override
    public EntityLocal getEntity() {
        return entity;
    }

    @Override
    public Location getLocation() {
        return location;
    }

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

    private void deploy() {
        List<String> serviceNames = null;
        List<String> uris = new ArrayList<String>();
        Staging staging;
        File war;
        try {
            staging = new Staging(null, BUILDPACK_URL);
            uris.add(inferApplicationDomainUri(applicationName));
            //fixme a URI in necessary
            war = new File(applicationPath);

            client.createApplication(applicationName, staging, DEFAULT_MEMORY, uris, serviceNames);
            client.uploadApplication(applicationName, war.getCanonicalPath());

        } catch (IOException e) {
            log.error("Error deploying application {} managed by driver {}",
                    new Object[]{getEntity(), this});
        }
    }

    private String inferApplicationDomainUri(String name) {
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
        entity.setAttribute(JavaCloudFoundryPaasWebApp.ROOT_URL, domainUri);

        entity.setAttribute(JavaCloudFoundryPaasWebApp.INSTANCES_NUM,
                application.getInstances());
        application.getResources();
        entity.setAttribute(JavaCloudFoundryPaasWebApp.MEMORY,
                application.getMemory());

        entity.setAttribute(JavaCloudFoundryPaasWebApp.DISK,
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


