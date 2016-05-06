/**
 *
 * Copyright (c) 2014, Deem Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package com.deem.zkui.controller;

import freemarker.template.TemplateException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.deem.zkui.utils.CmdUtil;
import com.deem.zkui.utils.ServletUtil;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = {"/monitor"})
public class Monitor extends HttpServlet {

    private final static Logger logger = LoggerFactory.getLogger(Monitor.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.debug("Monitor Action!");
        try {
            Properties globalProps = (Properties) this.getServletContext().getAttribute("globalProps");
            String zkServer = globalProps.getProperty("zkServer");

            Map<String, Object> templateParam = new HashMap<>();
            StringBuffer stats = new StringBuffer();
            for (String zkObj : zkServer.split(",")) {
                stats.append("<hr/><h4>").append("Server: ").append(zkObj).append("</h4><hr/>");
                String[] monitorZKServer = zkObj.split(":");

                try {
                    stats.append("<strong>Server Summary:</strong><br/>");
                    stats.append(CmdUtil.INSTANCE.executeCmd("srvr", monitorZKServer[0], monitorZKServer[1]));
                    stats.append("<br/><strong>Connection Summary:</strong><br/>");
                    stats.append(CmdUtil.INSTANCE.executeCmd("cons", monitorZKServer[0], monitorZKServer[1]));
                    stats.append("<strong>Watch Summary:</strong><br/>");
                    stats.append(CmdUtil.INSTANCE.executeCmd("wchs", monitorZKServer[0], monitorZKServer[1]));
                } catch (IOException ex) {
                    stats.append("<p style=\"color:red;\">");
                    stats.append("Server Response Error: ").append(ex.getMessage());
                    stats.append("</p><br/>");
                }
            }
            templateParam.put("stats", stats);
            ServletUtil.INSTANCE.renderHtml(request, response, templateParam, "monitor.ftl.html");

        } catch (IOException | TemplateException ex) {
            logger.error(Arrays.toString(ex.getStackTrace()));
            ServletUtil.INSTANCE.renderError(request, response, ex.getMessage());
        }
    }
}
