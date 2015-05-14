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

import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.cloudfoundry.webapp.CloudFoundryWebApp;
import brooklyn.entity.cloudfoundry.webapp.PaasHardwareResources;
import brooklyn.entity.cloudfoundry.webapp.java.JavaCloudFoundryPaasWebApp;
import brooklyn.entity.cloudfoundry.services.CloudFoundryService;
import brooklyn.entity.cloudfoundry.services.sql.cleardb.ClearDbService;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.trait.Startable;
import brooklyn.test.Asserts;
import com.google.common.collect.ImmutableList;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.LinkedList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.testng.Assert.*;


public class JavaWebAndServiceCloudFoundryLiveTest extends AbstractCloudFoundryPaasLocationLiveTest {

    private final String APPLICATION_PATH = checkNotNull(getClass().getClassLoader()
            .getResource("brooklyn-example-hello-world-webapp.war")).getFile();

    private final String SERVICE_NAME = APPLICATION_SERVICE_NAME+"-mysql";
    private final String SERVICE_TYPE_ID = "cleardb";
    private final String SERVICE_PLAN = "spark";

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception {
        if (app != null) Entities.destroyAllCatching(app.getManagementContext());
    }

    @Test(groups = {"Live"})
    protected void deployAppWithServicesTest() throws Exception {
        List<String> servicesToBind=new LinkedList<String>();
        final CloudFoundryService service = app
                .createAndManageChild(EntitySpec.create(ClearDbService.class)
                        .configure("serviceInstanceName", SERVICE_NAME)
                        .configure("plan", SERVICE_PLAN)
                        .location(cloudFoundryPaasLocation));

        servicesToBind.add(service.getId());
        final JavaCloudFoundryPaasWebApp server = app
                .createAndManageChild(EntitySpec.create(JavaCloudFoundryPaasWebApp.class)
                        .configure("application-name", APPLICATION_NAME + "-withServices")
                        .configure("application-path", APPLICATION_PATH)
                        .configure("bind", servicesToBind)
                        .location(cloudFoundryPaasLocation));

        app.start(ImmutableList.of(cloudFoundryPaasLocation));

        Asserts.succeedsEventually(new Runnable() {
            public void run() {

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
            }
        });
    }

    @Test(expectedExceptions = brooklyn.util.exceptions.PropagatedRuntimeException.class,
    groups={"Live"})
    protected void deployAppWithNotAvailableServicesEntityTest() throws Exception {
        List<String> servicesToBind=new LinkedList<String>();

            servicesToBind.add("NotExistingService");
            final JavaCloudFoundryPaasWebApp server = app
                    .createAndManageChild(EntitySpec.create(JavaCloudFoundryPaasWebApp.class)
                            .configure("application-name",
                                    APPLICATION_NAME + "-withNotAvailableService")
                            .configure("application-path", APPLICATION_PATH)
                            .configure("bind", servicesToBind)
                            .location(cloudFoundryPaasLocation));
            app.start(ImmutableList.of(cloudFoundryPaasLocation));
    }


}
