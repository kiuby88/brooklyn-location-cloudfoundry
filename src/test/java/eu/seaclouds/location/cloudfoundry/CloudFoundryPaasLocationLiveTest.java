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

import brooklyn.location.cloudfoundry.CloudFoundryPaasLocation;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;


public class CloudFoundryPaasLocationLiveTest extends AbstractCloudFoundryPaasLocationLiveTest {


    @Test
    public void testClientSetUp() {
        cloudFoundryPaasLocation.setUpClient();
        assertNotNull(cloudFoundryPaasLocation.getCloudFoundryClient());
    }

    @Test
    public void testClientSetUpPerLocationInstanve() {
        cloudFoundryPaasLocation.setUpClient();
        CloudFoundryClient client1 = cloudFoundryPaasLocation.getCloudFoundryClient();
        cloudFoundryPaasLocation.setUpClient();
        CloudFoundryClient client2 = cloudFoundryPaasLocation.getCloudFoundryClient();
        assertEquals(client1, client2);
    }


}
