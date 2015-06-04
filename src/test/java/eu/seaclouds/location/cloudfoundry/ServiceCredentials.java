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

import brooklyn.entity.basic.Entities;
import brooklyn.entity.cloudfoundry.generic.GenericService;
import brooklyn.entity.cloudfoundry.services.CloudFoundryService;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.test.Asserts;
import com.google.common.collect.ImmutableList;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class ServiceCredentials extends AbstractCloudFoundryPaasLocationLiveTest  {

    public final String SERVICE_TYPE_FIELD = "ID" ;
    static final String SERVICE_PLAN_FIELD = "PLAN";
    static final String SERVICE_NAME_FIELD =  "NAME";

    @DataProvider(name = "serviceDescriptionProvider")
    public Object[][] createServiceDescriptions() {
        return new Object[][]{
                {addServiceDescription("blazemeter", "free-tier"),},
                {addServiceDescription("rediscloud", "30mb")},
                {addServiceDescription("cleardb", "spark")},
                {addServiceDescription("cloudamqp", "lemur")},
                {addServiceDescription("elephantsql", "turtle")},
                {addServiceDescription("sendgrid", "free")},
                {addServiceDescription("mongolab", "sandbox")},
                {addServiceDescription("newrelic", "standard")},
                {addServiceDescription("loadimpact", "lifree")},
                {addServiceDescription("memcachier", "dev")},
                {addServiceDescription("memcachedcloud", "30mb")},
                {addServiceDescription("searchly", "starter")},
                {addServiceDescription("cloudforge", "free")},
                {addServiceDescription("ironworker", "free")},
                {addServiceDescription("ironmq", "free")},
                {addServiceDescription("pubnub", "free")},
                {addServiceDescription("cine-io", "starter")},
                {addServiceDescription("cedexisradar", "free-community-edition")},
                {addServiceDescription("statica", "starter")}

                //{addServiceDescription("searchify", "")}, not free plan
                //{addServiceDescription("cedexisopenmix", "")}, not free plan
                //{addServiceDescription("temporize", "")}, not free plan
        };
    }

    private Map<String, String> addServiceDescription(String type, String plan){
        String name = "test-brooklyn-"+type;
        return addServiceDescription(type, name, plan);
    }

    private Map<String, String> addServiceDescription(String type, String name, String plan){
        Map<String, String> service= new HashMap<String, String>();
        service.put(SERVICE_TYPE_FIELD, type);
        service.put(SERVICE_PLAN_FIELD, plan);
        service.put(SERVICE_NAME_FIELD, name);
        return service;
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception {
        if (app != null) Entities.destroyAllCatching(app.getManagementContext());
    }

    @Test(groups = {"Live"}, dataProvider = "serviceDescriptionProvider")
    protected void instanceServiceTest(Map<String, String > serviceDescription) {

        final CloudFoundryService service = app.createAndManageChild(EntitySpec.create(GenericService.class)
                .configure("serviceInstanceName", serviceDescription.get(SERVICE_NAME_FIELD))
                .configure("plan", serviceDescription.get(SERVICE_PLAN_FIELD))
                .configure("serviceType", serviceDescription.get(SERVICE_TYPE_FIELD))
                .location(cloudFoundryPaasLocation));

        app.start(ImmutableList.of(cloudFoundryPaasLocation));

        final CloudServiceInstance serviceInstance = cloudFoundryPaasLocation
                .getCloudFoundryClient()
                .getServiceInstance(serviceDescription.get(SERVICE_NAME_FIELD));

        Asserts.succeedsEventually(new Runnable() {
            @Override
            public void run() {
                assertNotNull(serviceInstance);
                assertTrue(serviceInstance.getCredentials().isEmpty());

            }
        });
    }















        /*

        static {
            HashMap<String, Map<String, String>> aMap = new HashMap<String, Map<String, String>>();
            serviceDescriptions = Collections.unmodifiableMap(aMap);

            //not free
            //addServiceDescription("searchify", "test-Brooklyn-searchify", "");

            //free
            addServiceDescription("blazemeter","test-brooklyn-blazemeter", "free-tier");

            //not
            addServiceDescription("rediscloud","test-brooklyn-", "30mb");

            //free
            addServiceDescription("cleardb","test-brooklyn-cleardb", "spark");

            //
            addServiceDescription("cloudamqp","test-brooklyn-cloudamqp", "lemur");

            //
            //addServiceDescription("","test-brooklyn-", "");


            //
            //addServiceDescription("","test-brooklyn-", "");


        }
    */




//    public static void addServiceDescription(String type, String name, String plan){
//        Map<String, String> service= new HashMap<String, String>();
//        service.put(SERVICE_TYPE_FIELD, type);
//        service.put(SERVICE_PLAN_FIELD, plan);
//        service.put(SERVICE_NAME_FIELD, name);
//        serviceDescriptions.put(type, service);

}
