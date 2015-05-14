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
package brooklyn.entity.cloudfoundry.services;

import brooklyn.entity.Entity;
import brooklyn.entity.cloudfoundry.CloudFoundryEntityImpl;
import brooklyn.util.collections.MutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Map;

//TODO refactor this class and CloudFoundryServiceImpl and delete the duplicate code
public abstract class CloudFoundryServiceImpl extends CloudFoundryEntityImpl
        implements CloudFoundryService {

    private static final Logger log = LoggerFactory.getLogger(CloudFoundryServiceImpl.class);

    public CloudFoundryServiceImpl() {
        super(MutableMap.of(), null);
    }

    public CloudFoundryServiceImpl(Entity parent) {
        this(MutableMap.of(), parent);
    }

    public CloudFoundryServiceImpl(Map properties) {
        this(properties, null);
    }

    public CloudFoundryServiceImpl(Map properties, Entity parent) {
        super(properties, parent);
    }

    @Override
    public Class getDriverInterface() {
        return PaasServiceDriver.class;
    }

    @Nullable
    @Override
    public PaasServiceDriver getDriver() {
        return (PaasServiceDriver) super.getDriver();
    }

    @Override
    public void init() {
        super.init();
        setAttribute(SERVICE_TYPE_ID, getServiceTypeId());
    }

    @Override
    public void destroy() {
        super.destroy();
        getDriver().deleteService();
    }


}
