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
package brooklyn.entity.cloudfoundry.webapp.java;


import brooklyn.entity.Entity;
import brooklyn.entity.cloudfoundry.webapp.CloudFoundryWebApp;
import brooklyn.entity.cloudfoundry.webapp.CloudFoundryWebAppImpl;
import brooklyn.event.feed.http.HttpFeed;
import brooklyn.event.feed.http.HttpPollConfig;
import brooklyn.event.feed.http.HttpValueFunctions;
import brooklyn.util.collections.MutableMap;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


public class JavaCloudFoundryPaasWebAppImpl extends CloudFoundryWebAppImpl implements JavaCloudFoundryPaasWebApp {
    
    private static final Logger log = LoggerFactory.getLogger(JavaCloudFoundryPaasWebAppImpl.class);

    private volatile HttpFeed httpFeed;

    public JavaCloudFoundryPaasWebAppImpl() {
        super(MutableMap.of(), null);
    }

    public JavaCloudFoundryPaasWebAppImpl(Entity parent) {
        this(MutableMap.of(), parent);
    }

    public JavaCloudFoundryPaasWebAppImpl(Map properties) {
        this(properties, null);
    }

    public JavaCloudFoundryPaasWebAppImpl(Map properties, Entity parent) {
        super(properties, parent);
    }

    @Override
    public Class getDriverInterface() {
        return JavaPaasWebAppDriver.class;
    }

    @Override
    public JavaPaasWebAppDriver getDriver() {
        return (JavaPaasWebAppDriver) super.getDriver();
    }

    @Override
    public String getBuildpack() {
        return getConfig(BUILDPACK);
    }

    @Override
    public Integer resize(Integer integer) {
        getDriver().changeInstancesNumber(integer);
        return getCurrentSize();
    }

    @Override
    public Integer getCurrentSize() {
        return getDriver().getInstancesNumber();
    }



    @Override
    protected void connectSensors() {
        super.connectSensors();


        String managementUri = String.format("%s/monitoring?format=json",
                getAttribute(CloudFoundryWebApp.ROOT_URL));

        setAttribute(JavaCloudFoundryPaasWebApp.MONITOR_URL, managementUri);

        Map<String, String> includeRuntimeUriVars = ImmutableMap.of("include-runtime", "true");

        httpFeed = HttpFeed.builder()
                .entity(this)
                .period(200)
                .baseUri(managementUri)
                .poll(new HttpPollConfig<Long>(USED_MEMORY)
                        .checkSuccess(HttpValueFunctions.responseCodeEquals(200))
                        .onSuccess(HttpValueFunctions.<Long>jsonContentsFromPath("$.list[12].memoryInformations.usedMemory")))
                .build();

       /* Map<String, String> map = new HashMap<String, String >();

        map.put("uri", managementUri);
        map.put("jsonPath", "$.list[12].memoryInformations.usedMemory");
        map.put("name", "jm.memory");
        map.put("period", "2000ms");
        map.put("targetType", "long");

//        map.put(HttpRequestSensor.SENSOR_URI, managementUri);
//        map.put(HttpRequestSensor.JSON_PATH, "$.list[12].memoryInformations.usedMemory");
//        map.put(HttpRequestSensor.SENSOR_NAME, "jm.memory");
//        map.put(HttpRequestSensor.SENSOR_PERIOD, "2000ms");
//        map.put(HttpRequestSensor.SENSOR_TYPE, "long");


        ConfigBag params = ConfigBag.newInstance(map);
        HttpRequestSensor a = new HttpRequestSensor(params);
        a.apply(this);*/

    }



}
