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
package brooklyn.location.paas.cloudfoundry;

import brooklyn.config.ConfigKey;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.location.MachineDetails;
import brooklyn.location.OsDetails;
import brooklyn.location.basic.AbstractLocation;
import brooklyn.util.flags.SetFromFlag;
import com.google.common.collect.ImmutableSet;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Set;

public class CloudFoundryPaasLocation extends AbstractLocation implements PaasLocation {
    public static final Logger LOG = LoggerFactory.getLogger(CloudFoundryPaasLocation.class);

    public static ConfigKey<String> CF_USER = ConfigKeys.newStringConfigKey("user");
    public static ConfigKey<String> CF_PASSWORD = ConfigKeys.newStringConfigKey("password");
    public static ConfigKey<String> CF_ORG = ConfigKeys.newStringConfigKey("org");
    public static ConfigKey<String> CF_ENDPOINT = ConfigKeys.newStringConfigKey("endpoint");
    public static ConfigKey<String> CF_SPACE = ConfigKeys.newStringConfigKey("space");

    CloudFoundryClient client;

    @SetFromFlag(nullable = false)
    protected InetAddress address;


    public CloudFoundryPaasLocation() {
        super();
    }

    @Override
    public void init() {
        super.init();
        //init client
        initClient();
    }

    private void initClient() {
        CloudCredentials credentials = new CloudCredentials(getConfig(CF_USER), getConfig(CF_PASSWORD));
        client = new CloudFoundryClient(credentials, getTargetURL(getConfig(CF_ENDPOINT)), getConfig(CF_ORG), getConfig(CF_SPACE), true);
        client.login();
    }

    private static URL getTargetURL(String target) {
        try {
            return URI.create(target).toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException("The target URL is not valid: " + e.getMessage());
        }
    }

    @Override
    public InetAddress getAddress() {
        return address;
    }

    @Override
    public OsDetails getOsDetails() {
        return null;
    }

    @Override
    public MachineDetails getMachineDetails() {
        return null;
    }

    public CloudFoundryClient getCloudFoundryClient() {
        return client;
    }

    @Nullable
    @Override
    public String getHostname() {
        return null;
    }

    @Override
    public Set<String> getPublicAddresses() {
        return null;
    }

    @Override
    public Set<String> getPrivateAddresses() {
        return null;
    }
}
