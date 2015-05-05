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
package brooklyn.location.entity.cloudfoundry;

import brooklyn.config.ConfigKey;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.BrooklynConfigKeys;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.Lifecycle;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.Sensors;
import brooklyn.location.cloudfoundry.CloudFoundryPaasLocation;
import brooklyn.util.flags.SetFromFlag;
import brooklyn.util.time.Duration;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Generic web application to be deployed on a CloudFoundry location.
 */
public interface CloudFoundryWebApp {

    @SetFromFlag("args")
    ConfigKey<List> ARGS = ConfigKeys.newConfigKey(List.class,
            "javaCloudFoundryWebApp.args", "Necessary arguments for java web app",
            Lists.newArrayList());

    @SetFromFlag("application-name")
    ConfigKey<String> APPLICATION_NAME = ConfigKeys.newStringConfigKey(
            "javaCloudFoundryWebApp.application.name", "Name of the application");

    @SetFromFlag("application-path")
    ConfigKey<String> APPLICATION_PATH = ConfigKeys.newStringConfigKey(
            "javaCloudFoundryWebApp.application.path", "URI of the application");

    @SetFromFlag("startTimeout")
    ConfigKey<Duration> START_TIMEOUT = BrooklynConfigKeys.START_TIMEOUT;

    @SetFromFlag("maxRebindSensorsDelay")
    ConfigKey<Duration> MAXIMUM_REBIND_SENSOR_CONNECT_DELAY = ConfigKeys.newConfigKey(Duration.class,
            "javaCloudFoundryWebApp.maxSensorRebindDelay",
            "The maximum delay to apply when reconnecting sensors when rebinding to this entity. " +
                    "Brooklyn will wait a random amount of time, up to the value of this config key, to " +
                    "avoid a thundering herd problem when the entity shares its machine with " +
                    "several others. Set to null or to 0 to disable any delay.",
            Duration.TEN_SECONDS);

    AttributeSensor<CloudFoundryPaasLocation> PAAS_LOCATION = Sensors.newSensor(
            CloudFoundryPaasLocation.class, "javaCloudFoundryWebApp.paasLocation",
            "Location used to deploy the application");

    AttributeSensor<Boolean> SERVICE_PROCESS_IS_RUNNING = Sensors.newBooleanSensor(
            "service.process.isRunning",
            "Whether the process for the service is confirmed as running");

    AttributeSensor<Lifecycle> SERVICE_STATE_ACTUAL = Attributes.SERVICE_STATE_ACTUAL;

    AttributeSensor<String> ROOT_URL =
            Sensors.newStringSensor("webapp.url", "URL of the application");

    public String getApplicationName();

    public void kill();


}
