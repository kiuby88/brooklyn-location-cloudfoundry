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
package brooklyn.entity.cloudfoundry;

import brooklyn.util.os.Os;
import brooklyn.util.text.Strings;
import com.google.common.base.Throwables;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalResourcesDownloader {
    private static final String BROOKLYN_DIR="brooklyn";

    public static File downloadResourceInLocalDir(String url) {
        File localResource = createLocalFilePathNameForUrl(url,
                findArchiveNameFromUrl(url));
        LocalResourcesDownloader.downloadResource(url, localResource);
        return localResource;
    }

    /**
     * Generates a tmp directory based on:</br>
     * TMP_OS_DIRECTORY/brooklyn/ENTITY_ID/RANDOM_STRING(8)
     * @return
     */
    public static String findATmpDir(){
        String osTmpDir = new Os.TmpDirFinder().get().get();
        return osTmpDir + File.separator +
                BROOKLYN_DIR + File.separator +
                Strings.makeRandomId(8);
    }

    public static File createLocalFilePathNameForUrl(String url, String fileName){
        return new File(createLocalPathNameForUrl(url, fileName));
    }

    public static String createLocalPathNameForUrl(String url, String fileName){
        String targetDirName = LocalResourcesDownloader.findATmpDir();
        String filePathName = targetDirName + File.separator + fileName;

        File targetDir = new File(targetDirName);
        targetDir.mkdirs();

        return filePathName;
    }

    public static String findArchiveNameFromUrl(String url) {
        String name = url.substring(url.lastIndexOf('/') + 1);
        if (name.indexOf("?") > 0) {
            Pattern p = Pattern.compile("[A-Za-z0-9_\\-]+\\..(ar|AR)($|(?=[^A-Za-z0-9_\\-]))");
            Matcher wars = p.matcher(name);
            if (wars.find()) {
                name = wars.group();
            }
        }
        return name;
    }

    public static void downloadResource(String url, File target){
            try {
                downloadResource(new URL(url), target);
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
    }

    public static void downloadResource(URL url, File target){
        try {
            FileUtils.copyURLToFile(url, target);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

}
