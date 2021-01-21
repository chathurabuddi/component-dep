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
package com.wso2telco.dep.responsefilter.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import com.wso2telco.core.dbutils.DbUtils;
import com.wso2telco.core.dbutils.util.DataSourceNames;
import com.wso2telco.dep.responsefilter.exception.ResponseFilterException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.jersey.uri.UriTemplate;

public class DbFilterSchemaDao implements FilterSchemaDao {

    protected Log log = LogFactory.getLog(this.getClass());

    @Override
    public String find(String sp, String application, String api, String httpVerb, String resource) throws ResponseFilterException {
        log.debug("retrieving response filters from database: " + sp + "|" + application + "|" + api + "|" + httpVerb + "|" + resource);
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        String filterSchema = null;

        try {
            connection = DbUtils.getDbConnection(DataSourceNames.WSO2TELCO_DEP_DB);
            if (connection == null) {
                throw new SQLException("database connection error");
            }

            StringBuilder query = new StringBuilder("SELECT operation, fields FROM response_filter");
            query.append(" WHERE sp=? AND application=? AND api=?");
            statement = connection.prepareStatement(query.toString());
            statement.setString(1, sp);
            statement.setString(2, application);
            statement.setString(3, api);
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String[] uriTemplateComponents = resultSet.getString(1).split(" ");
                if (httpVerb != null &&
                        httpVerb.equals(uriTemplateComponents[0]) &&
                        new UriTemplate(uriTemplateComponents[1]).match(resource, new ArrayList<String>())) {
                    filterSchema = resultSet.getString(2);
                }
            }
        } catch (Exception e) {
            throw new ResponseFilterException(e);
        } finally {
            DbUtils.closeAllConnections(statement, connection, resultSet);
        }
        return filterSchema;
    }
}
