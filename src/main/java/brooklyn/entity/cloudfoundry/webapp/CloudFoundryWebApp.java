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

import brooklyn.config.ConfigKey;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.cloudfoundry.CloudFoundryEntity;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.BasicAttributeSensor;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.event.basic.Sensors;
import brooklyn.util.flags.SetFromFlag;
import brooklyn.util.text.Identifiers;
import com.google.common.reflect.TypeToken;

import java.util.List;

/**
 * Generic web application to be deployed on a CloudFoundry location.
 */
public interface CloudFoundryWebApp extends CloudFoundryEntity{

    @SetFromFlag("application-name")
    ConfigKey<String> APPLICATION_NAME = ConfigKeys.newStringConfigKey(
            "cloudFoundryWebApp.application.name", "Name of the application"
    , "cf-app-" + Identifiers.makeRandomId(8));

    @SetFromFlag("application-path")
    ConfigKey<String> APPLICATION_PATH = ConfigKeys.newStringConfigKey(
            "cloudFoundryWebApp.application.path", "URI of the application");

    @SuppressWarnings("unchecked")
    @SetFromFlag("bind")
    ConfigKey<List<Entity>> NAMED_SERVICES = new BasicConfigKey(List.class,
            "cloudFoundry.webapp.boundServices",
            "List of names of the services that should be bound to this application, " +
                    "providing credentials for its usage");
    
    @SuppressWarnings("unchecked")
    @SetFromFlag("bound_services")
    public static final AttributeSensor<List<String>> BOUND_SERVICES =
            new BasicAttributeSensor<List<String>>(new TypeToken<List<String>>() {},
            "cloudFoundry.webapp.boundServices",
            "List of names of the services that were bound to this application, " +
                    "providing credentials for its usage");

    AttributeSensor<String> ROOT_URL =
            Sensors.newStringSensor("webapp.url", "URL of the application");

    public static final AttributeSensor<String> VCAP_SERVICES = 
            Sensors.newStringSensor( "vcap.services", 
                    "JSON information related to services bound to the application, " +
                    "such as credentials, endpoint information, selected plan, etc.");
    
    /**
     * @return URL of the CloudFoundry Buildpack needed for building the application
     */
    public String getBuildpack();


}
