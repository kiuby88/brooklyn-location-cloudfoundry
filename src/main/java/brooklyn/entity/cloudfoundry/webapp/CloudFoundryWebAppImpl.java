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


import brooklyn.entity.Entity;
import brooklyn.entity.cloudfoundry.CloudFoundryEntityImpl;
import brooklyn.util.collections.MutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Map;

public abstract class CloudFoundryWebAppImpl extends CloudFoundryEntityImpl
        implements CloudFoundryWebApp {

    private static final Logger log = LoggerFactory.getLogger(CloudFoundryWebAppImpl.class);

    public CloudFoundryWebAppImpl() {
        super(MutableMap.of(), null);
    }

    public CloudFoundryWebAppImpl(Entity parent) {
        this(MutableMap.of(), parent);
    }

    public CloudFoundryWebAppImpl(Map properties) {
        this(properties, null);
    }

    public CloudFoundryWebAppImpl(Map properties, Entity parent) {
        super(properties, parent);
    }

    @Override
    public abstract Class getDriverInterface();

    @Override
    public PaasWebAppDriver getDriver() {
        return (PaasWebAppDriver) super.getDriver();
    }

    @Override
     public void init() {
        super.init();
        initAtributesValues();
    }

    private void initAtributesValues(){
        setAttribute(BOUND_SERVICES, new LinkedList<String>());
    }

}
