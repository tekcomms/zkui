/**
 * Copyright (c) 2014, Deem Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.deem.zkui.utils;

import java.net.Socket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum CmdUtil {

    INSTANCE;
    private final static Logger logger = LoggerFactory.getLogger(CmdUtil.class);

    public String executeCmd(String cmd, String zkServer, String zkPort) throws IOException {
        Socket s = null;
        PrintWriter out = null;
        BufferedReader reader = null;
        StringBuilder sb;

        try {
            s = new Socket(zkServer, Integer.parseInt(zkPort));
            out = new PrintWriter(s.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(s.getInputStream()));

            out.write(cmd);
            out.flush();
            sb = new StringBuilder();

            String line = reader.readLine();
            while (line != null) {
                sb.append(line);
                sb.append("<br/>");
                line = reader.readLine();
            }
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception ex) {
                    logger.error(ex.getMessage());
                }
            }

            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ex) {
                    logger.error(ex.getMessage());
                }
            }

            if (s != null) {
                try {
                    s.close();
                } catch (Exception ex) {
                    logger.error(ex.getMessage());
                }
            }
        }

        return sb.toString();
    }
}
