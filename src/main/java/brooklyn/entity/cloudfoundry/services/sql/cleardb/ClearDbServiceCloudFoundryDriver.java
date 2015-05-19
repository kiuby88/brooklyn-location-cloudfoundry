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


import brooklyn.entity.cloudfoundry.services.CloudFoundryService;
import brooklyn.entity.cloudfoundry.services.CloudFoundryServiceImpl;
import brooklyn.entity.cloudfoundry.services.PaasServiceCloudFoundryDriver;
import brooklyn.entity.cloudfoundry.webapp.CloudFoundryWebAppImpl;
import brooklyn.location.cloudfoundry.CloudFoundryPaasLocation;
import com.google.gson.Gson;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Map;

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

            con = DriverManager.getConnection(createJDBCStringConnection(app));
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

    private String createJDBCStringConnection(CloudFoundryWebAppImpl app){
        return generateJDBCFromCredentials(getServiceCredentials(app));
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getServiceCredentials(CloudFoundryWebAppImpl app){
        JSONArray pathResult= JsonPath.read(new Gson().toJson(app.getApplicationEnvAsMap()),
                "$.system_env_json.VCAP_SERVICES."
                        + getEntity().getServiceTypeId()+
                        "[?(@.name =~/.*" +
                        getEntity().getConfig(CloudFoundryService.SERVICE_INSTANCE_NAME) +
                       "/i)].credentials");
        if((pathResult!=null)&&(pathResult.size()==1)){
            return ((Map<String, String>) pathResult.get(0));
        } else {
            throw new RuntimeException("Error finding a service credentials in driver" + this +
                    " deploying service "+getEntity().getId());
        }
    }

    private String generateJDBCFromCredentials(Map<String, String> credentials){
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
