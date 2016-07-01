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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.zookeeper.KeeperException;
import com.deem.zkui.dao.Dao;
import com.deem.zkui.utils.ServletUtil;
import com.deem.zkui.utils.ZooKeeperUtil;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = {"/import"})
@MultipartConfig(fileSizeThreshold = 1024 * 1024 * 10, // 10 MB
        maxFileSize = 1024 * 1024 * 50, // 50 MB
        maxRequestSize = 1024 * 1024 * 100)      // 100 MB
public class Import extends HttpServlet {

    private final static Logger logger = LoggerFactory.getLogger(Import.class);

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.debug("Importing Action!");
        BufferedReader br = null;

        try {
            Properties globalProps = (Properties) this.getServletContext().getAttribute("globalProps");
            Dao dao = new Dao(globalProps);
            String zkServer = globalProps.getProperty("zkServer");

            StringBuilder sbFile = new StringBuilder();
            String forceImport = "false";
            String uploadFileName = "";

            DiskFileItemFactory factory = new DiskFileItemFactory();
            factory.setSizeThreshold(1034);
            ServletFileUpload upload = new ServletFileUpload(factory);
            List items = upload.parseRequest(request);

            Iterator iter = items.iterator();
            while (iter.hasNext()) {
                FileItem item = (FileItem) iter.next();
                if (item.isFormField()) {
                    if (item.getFieldName().equals("forceImport")) {
                        forceImport = item.getString();
                    }
                } else {
                    uploadFileName = item.getName();
                    sbFile.append(item.getString());
                }
            }

            InputStream inpStream;

            logger.debug("Upload file Processing " + uploadFileName);
            dao.insertHistory((String) request.getSession().getAttribute("authName"), request.getRemoteAddr(), "Import File: " + uploadFileName + "<br/>" + "Overwrite: " + forceImport);
            inpStream = new ByteArrayInputStream(sbFile.toString().getBytes());

            // open the stream and put it into BufferedReader
            br = new BufferedReader(new InputStreamReader(inpStream));
            String inputLine;
            List<String> importFile = new ArrayList<>();
            Integer lineCnt = 0;
            while ((inputLine = br.readLine()) != null) {
                lineCnt++;
                // Empty or comment?
                if (inputLine.trim().equals("") || inputLine.trim().startsWith("#")) {
                    continue;
                }
                if (inputLine.startsWith("-")) {
                    //DO nothing.
                } else if (!inputLine.matches("/.+=.+=.*")) {
                    throw new IOException("Invalid format at line " + lineCnt + ": " + inputLine);
                }

                importFile.add(inputLine);
            }

            ZooKeeperUtil.INSTANCE.importData(importFile, Boolean.valueOf(forceImport), ServletUtil.INSTANCE.getZookeeper(request, response, zkServer, globalProps));
            String summaryPrefix = "Import File: " + uploadFileName + ", ";
            StringBuilder summary = new StringBuilder(summaryPrefix);

            for (String line : importFile) {
                String[] tokens = line.split("=");
                String nodePath = tokens[0];
                String propName = tokens[1];
                String propValue = (tokens.length == 3) ? tokens[2] : null;

                summary.setLength(summaryPrefix.length());
                summary.append((line.startsWith("-")) ? "Delete" : "Add").append(" Entry: ");
                summary.append(nodePath).append(",");
                summary.append(propName).append("=");
                if (propValue != null) {
                    summary.append((!ZooKeeperUtil.INSTANCE.checkIfPwdField(propName)) ? propValue : ZooKeeperUtil.SOPA_PIPA);
                }

                dao.insertHistory((String) request.getSession().getAttribute("authName"), request.getRemoteAddr(), summary.toString());
            }
            request.getSession().setAttribute("flashMsg", "Import Completed!");
            response.sendRedirect("/home");
        } catch (FileUploadException | IOException | InterruptedException | KeeperException ex) {
            logger.error(Arrays.toString(ex.getStackTrace()));
            ServletUtil.INSTANCE.renderError(request, response, ex.getMessage());
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception ex) {
                    logger.error(ex.getMessage());
                }
            }
        }
    }
}
