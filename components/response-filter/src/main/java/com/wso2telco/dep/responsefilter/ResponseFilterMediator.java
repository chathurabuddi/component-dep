/*
 * Copyright (c) 2016, WSO2.Telco Inc. (http://www.wso2telco.com) All Rights Reserved.
 * <p>
 * WSO2.Telco Inc. licences this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wso2telco.dep.responsefilter;

import java.io.IOException;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wso2telco.dep.responsefilter.dao.CacheDecorator;
import com.wso2telco.dep.responsefilter.dao.DbFilterSchemaDao;
import com.wso2telco.dep.responsefilter.dao.FilterSchemaDao;
import lk.chathurabuddi.json.schema.constants.FreeFormAction;
import lk.chathurabuddi.json.schema.filter.JsonSchemaFilter;
import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;

public class ResponseFilterMediator extends AbstractMediator {

    private FilterSchemaDao filterSchemaDao = new DbFilterSchemaDao();
    private String cacheEnabled;

    public boolean mediate(MessageContext messageContext) {
        long beginTimestamp = System.currentTimeMillis();
        String api = messageContext.getProperty("api.ut.api").toString() + ":" + messageContext.getProperty("api.ut.version").toString();
        String httpVerb = messageContext.getProperty("api.ut.HTTP_METHOD").toString();
        String resource = messageContext.getProperty("api.ut.resource").toString();
        String userId = messageContext.getProperty("api.ut.userId").toString().split("@")[0];
        String appName = messageContext.getProperty("api.ut.application.name").toString();

        log.debug("API : " + api + " | USER : " + userId + " | APP_NAME : " + appName + " | HTTP_VERB : " + httpVerb + " | RESOURCE : " + resource);

        org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) messageContext)
                .getAxis2MessageContext();

        try {
            String jsonString = (String) messageContext.getProperty("jsonPayload");
            if (jsonString != null && !"".equals(jsonString.trim())){
                String filterSchema = this.filterSchemaDao.find(userId, appName, api, httpVerb, resource);
                if (filterSchema != null) {
                    String filteredJson = new JsonSchemaFilter(filterSchema, jsonString, FreeFormAction.DETACH).filter();
                    if (isNonEmptyJson(filteredJson) || isMatchingPayload(jsonString, filterSchema)) {
                        JsonUtil.getNewJsonPayload(axis2MessageContext, filteredJson, true, true);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error while filtering the response.", e);
            return false;
        }
        long endTimestamp = System.currentTimeMillis();
        log.debug("response filter mediator took " + (endTimestamp-beginTimestamp) + "ms");
        return true;
    }

    // compare the root elements of the payload according to the filter schema
    private boolean isMatchingPayload(String jsonString, String filterSchema) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(jsonString);
        JsonNode schema = mapper.readTree(filterSchema);
        String rootElementType = schema.findPath("type").asText();
        return !((json.isArray() && !"array".equals(rootElementType)) ||
                ("object".equals(rootElementType) && !rootElementsMatched(json, schema)));
    }

    private boolean rootElementsMatched(JsonNode json, JsonNode schema) {
        Iterator<String> properties = schema.findPath("properties").fieldNames();
        while (properties.hasNext()) {
            String property = properties.next();
            if (json.has(property)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNonEmptyJson(String filteredJson) {
        return filteredJson != null && !filteredJson.isEmpty() && !"{}".equals(filteredJson);
    }

    public String isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(String cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
        if ("true".equals(cacheEnabled)) {
            log.debug("response filter caching enabled");
            this.filterSchemaDao = new CacheDecorator(this.filterSchemaDao);
        }
    }

}
