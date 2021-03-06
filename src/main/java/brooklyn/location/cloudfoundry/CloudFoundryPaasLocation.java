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
package brooklyn.location.cloudfoundry;

import brooklyn.config.ConfigKey;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.location.basic.AbstractLocation;
import brooklyn.location.paas.PaasLocation;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class CloudFoundryPaasLocation extends AbstractLocation
        implements PaasLocation, PaasHardwareResources {

    public static final Logger LOG = LoggerFactory.getLogger(CloudFoundryPaasLocation.class);

    public static ConfigKey<String> CF_USER = ConfigKeys.newStringConfigKey("user");
    public static ConfigKey<String> CF_PASSWORD = ConfigKeys.newStringConfigKey("password");
    public static ConfigKey<String> CF_ORG = ConfigKeys.newStringConfigKey("org");
    public static ConfigKey<String> CF_ENDPOINT = ConfigKeys.newStringConfigKey("endpoint");
    public static ConfigKey<String> CF_SPACE = ConfigKeys.newStringConfigKey("space");

    CloudFoundryClient client;

    public CloudFoundryPaasLocation() {
        super();
    }

    @Override
    public void init() {
        super.init();
    }

    public void setUpClient() {
        if (client == null) {
            CloudCredentials credentials =
                    new CloudCredentials(getConfig(CF_USER), getConfig(CF_PASSWORD));
            client = new CloudFoundryClient(credentials,
                    getTargetURL(getConfig(CF_ENDPOINT)),
                    getConfig(CF_ORG), getConfig(CF_SPACE), true);
            client.login();
        }
    }

    @Override
    public String getPaasProviderName() {
        return "CloudFoundry";
    }

    private static URL getTargetURL(String target) {
        try {
            return URI.create(target).toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException("The target URL is not valid: " + e.getMessage());
        }
    }

    public CloudFoundryClient getCloudFoundryClient() {
        return client;
    }


}
