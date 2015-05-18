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
package brooklyn.entity.cloudfoundry.services.sql.cleardb;


import brooklyn.entity.cloudfoundry.services.CloudFoundryServiceImpl;
import brooklyn.entity.cloudfoundry.services.PaasServiceCloudFoundryDriver;
import brooklyn.entity.cloudfoundry.webapp.CloudFoundryWebApp;
import brooklyn.entity.cloudfoundry.webapp.CloudFoundryWebAppImpl;
import brooklyn.location.cloudfoundry.CloudFoundryPaasLocation;
import com.google.gson.JsonElement;
import org.cloudfoundry.client.lib.domain.Staging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class ClearDbServiceCloudFoundryDriver extends PaasServiceCloudFoundryDriver
        implements ClearDbServiceDriver{

    public static final Logger log = LoggerFactory
            .getLogger(ClearDbServiceCloudFoundryDriver.class);

    public ClearDbServiceCloudFoundryDriver(CloudFoundryServiceImpl entity,
                                            CloudFoundryPaasLocation location) {
        super(entity, location);
    }

    @Override
    public ClearDbServiceImpl getEntity() {
        return (ClearDbServiceImpl) super.getEntity();
    }

    @Override
    public void operation(CloudFoundryWebAppImpl app) {
        String DRIVER = "com.mysql.jdbc.Driver";
        Connection con;
        Statement stmt;
        try {
            Class.forName(DRIVER).newInstance();

            con = DriverManager.getConnection(createJDBC(app));
            stmt = con.createStatement();
            String text = new String(Files.readAllBytes(
                    Paths.get(getEntity().getConfig(ClearDbService.CREATION_SCRIPT_URL))),
                    StandardCharsets.UTF_8);
            stmt.execute(text);
            stmt.close();
            con.close();
        } catch (Exception e) {
            throw new RuntimeException("Error during database creation in driver" + this +
                    " deploying service "+getEntity().getId());
        }
    }

    private String createJDBC(CloudFoundryWebAppImpl app){
        Map<String, Object> clearDbServiceDescription= getClearDbServiceDescription(app);
        return generateJDBC(getCredentials(clearDbServiceDescription));
    }

    private Map<String, Object> getClearDbServiceDescription(CloudFoundryWebAppImpl app){
        List<Map<String, Object>> clearDbServiceList = getClearDbServiceList(app);
        return  findServiceDescription(clearDbServiceList);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getClearDbServiceList(CloudFoundryWebAppImpl app){
        String SYSTEM_ENV= "system_env_json";
        String VCAP_SERVICES= "VCAP_SERVICES";

        Map<String, Object> systemEnvMap = (Map<String, Object>) app.getApplicationEnvAsMap()
                .get(SYSTEM_ENV);
        Map<String, Object> vcapServices = (Map<String, Object>) systemEnvMap.get(VCAP_SERVICES);
        List<Map<String, Object>> cleardbList = (List<Map<String, Object>>) vcapServices
                .get(getEntity().getServiceTypeId());
        return cleardbList;
    }

    private Map<String, Object> findServiceDescription(List<Map<String, Object>> serviceDescriptions) {
        return findServiceDescription(serviceDescriptions, getEntity()
                .getConfig(ClearDbService.SERVICE_INSTANCE_NAME));
    }

    private Map<String, Object> findServiceDescription(List<Map<String, Object>> serviceDescriptions,
                                                       String serviceId){
        String NAME="name";
        Map<String, Object> result=null;
        for(Map<String, Object> vcapService: serviceDescriptions){
            if(vcapService.get(NAME).equals(serviceId)){
                result= vcapService;
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getCredentials(Map<String, Object> serviceDescription){
        String CREDENTIALS = "credentials";
        return (Map<String, String>) serviceDescription.get(CREDENTIALS);
    }

    private String generateJDBC(Map<String, String> credentials){

        String hostName = credentials.get("hostname");
        String username = credentials.get("username");
        String port= credentials.get("port");
        String password = credentials.get("password");
        String name = credentials.get("name");

        String url = String.format("jdbc:mysql://%s:%s/%s?user=%s&password=%s",
                hostName,
                port,
                name,
                username,
                password);
        return url;
    }


}
