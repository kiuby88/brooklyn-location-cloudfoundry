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
package eu.seaclouds.location.cloudfoundry;

import brooklyn.entity.Application;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.cloudfoundry.services.CloudFoundryService;
import brooklyn.entity.cloudfoundry.webapp.CloudFoundryWebApp;
import brooklyn.entity.cloudfoundry.webapp.PaasHardwareResources;
import brooklyn.entity.cloudfoundry.webapp.java.JavaCloudFoundryPaasWebApp;
import brooklyn.entity.trait.Startable;
import brooklyn.event.AttributeSensor;
import brooklyn.launcher.camp.SimpleYamlLauncher;

import brooklyn.test.Asserts;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class CloudFoundryYamlTest extends AbstractCloudFoundryPaasLocationLiveTest {


    private final String SERVICE_NAME = APPLICATION_SERVICE_NAME+"-mysql";
    private final String SERVICE_TYPE_ID = "cleardb";
    private final String SERVICE_PLAN = "spark";

    @Test( groups={"Live"} )
    public void deployWebappWithServicesFromYaml(){
        SimpleYamlLauncher launcher = new SimpleYamlLauncher();
        launcher.setShutdownAppsOnExit(false);
        Application app = launcher.launchAppYaml("cf-webapp-db.yaml");


        final CloudFoundryService service = (CloudFoundryService)
                findEntityChildByDisplayName(app, "DB HelloWorld Visitors");

        final CloudFoundryWebApp server = (CloudFoundryWebApp)
                findEntityChildByDisplayName(app, "AppServer HelloWorld");

        Asserts.succeedsEventually(new Runnable() {
            public void run() {

                assertNotNull(server);
                assertNotNull(service);

                assertEquals(server.getAttribute(CloudFoundryWebApp.BOUND_SERVICES).size(), 1);

                assertEquals(service.getAttribute(CloudFoundryService.SERVICE_TYPE_ID),
                        SERVICE_TYPE_ID);
                assertEquals(service.getConfig(CloudFoundryService.PLAN), SERVICE_PLAN);
                assertEquals(service.getConfig(CloudFoundryService.SERVICE_INSTANCE_NAME),
                        SERVICE_NAME);

                assertTrue(server.getAttribute(Startable.SERVICE_UP));
                assertTrue(server.getAttribute(JavaCloudFoundryPaasWebApp
                        .SERVICE_PROCESS_IS_RUNNING));

                assertNotNull(server.getAttribute(Attributes.MAIN_URI));
                assertNotNull(server.getAttribute(JavaCloudFoundryPaasWebApp.ROOT_URL));

                assertEquals(server.getAttribute(JavaCloudFoundryPaasWebApp.DISK),
                        PaasHardwareResources.REQUIRED_DISK.getDefaultValue());
                assertEquals(server.getAttribute(JavaCloudFoundryPaasWebApp.INSTANCES_NUM),
                        PaasHardwareResources.REQUIRED_INSTANCES.getDefaultValue());
                assertEquals(server.getAttribute(JavaCloudFoundryPaasWebApp.MEMORY),
                        PaasHardwareResources.REQUIRED_MEMORY.getDefaultValue());

                assertNotNull(server.getAttribute(CloudFoundryWebApp.VCAP_SERVICES));
                assertNotEquals(server.getAttribute(CloudFoundryWebApp.VCAP_SERVICES), "");


                //TODO
                //check server.getAttribute(CloudFoundryWebApp.ENV)
                //check servicegetAttribute(Dinamic Sensors)
            }
        });



    }


    private Entity findEntityChildByDisplayName(Application app, String displayName){
        for(Object entity: app.getChildren().toArray())
            if(((Entity)entity).getDisplayName().equals(displayName)){
                return (Entity)entity;
            }
        return null;
    }


}
