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
package eu.seaclouds.location.openshift;

import brooklyn.entity.Application;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.openshift.webapp.OpenShiftWebApp;
import brooklyn.entity.trait.Startable;
import brooklyn.launcher.camp.SimpleYamlLauncher;
import brooklyn.test.Asserts;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Created by Jose on 03/07/15.
 */
public class OpenShiftYamlTest {


    @Test( groups={"Live"} )
    public void deployWebappWithServicesFromYaml(){
        SimpleYamlLauncher launcher = new SimpleYamlLauncher();
        launcher.setShutdownAppsOnExit(false);
        Application app = launcher.launchAppYaml("openShift-webapp-db.yaml").getApplication();

        final OpenShiftWebApp server = (OpenShiftWebApp)
                findEntityChildByDisplayName(app, "Web AppServer HelloWorld");


        Asserts.succeedsEventually(new Runnable() {
            public void run() {

                assertNotNull(server);

                //assertTrue(server.getAttribute(Startable.SERVICE_UP));
                //assertTrue(server.getAttribute(OpenShiftWebApp
                //        .SERVICE_PROCESS_IS_RUNNING));

                //assertNotNull(server.getAttribute(Attributes.MAIN_URI));
                //assertNotNull(server.getAttribute(OpenShiftWebApp.ROOT_URL));

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
