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

import brooklyn.location.Location;
import brooklyn.location.LocationRegistry;
import brooklyn.location.LocationResolver;
import brooklyn.location.LocationSpec;
import brooklyn.location.basic.BasicLocationRegistry;
import brooklyn.management.ManagementContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class CloudFoundryPaasLocationResolver implements  LocationResolver {

    public static final Logger log = LoggerFactory
            .getLogger(CloudFoundryPaasLocationResolver.class);

    public static final String CLOUD_FOUNDRY = "cloudfoundry";

    private ManagementContext managementContext;

    @Override
    public void init(ManagementContext managementContext) {
        this.managementContext = checkNotNull(managementContext, "managementContext");
    }

    @Override
    public String getPrefix() {
        return CLOUD_FOUNDRY;
    }

    @Override
    public boolean accepts(String spec, LocationRegistry registry) {
        if (BasicLocationRegistry.isResolverPrefixForSpec(this, spec, true)) {
            return true;
        } else {
            // TODO: check valid CloudFoundry format on spec
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Location newLocationFromString(Map locationFlags,
                                          String spec,
                                          LocationRegistry registry) {
        // TODO: TODO
        return managementContext.getLocationManager().createLocation(
                LocationSpec.create(locationFlags, CloudFoundryPaasLocation.class));
    }


}
