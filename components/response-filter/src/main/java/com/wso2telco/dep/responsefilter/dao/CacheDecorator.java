package com.wso2telco.dep.responsefilter.dao;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.wso2telco.dep.responsefilter.exception.ResponseFilterException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CacheDecorator implements FilterSchemaDao {

    protected Log log = LogFactory.getLog(this.getClass());
    private final FilterSchemaDao filterSchemaDao;

    public CacheDecorator(FilterSchemaDao filterSchemaDao) {
        this.filterSchemaDao = filterSchemaDao;
    }

    @Override
    public String find(String sp, String application, String api, String httpVerb, String resource) throws ResponseFilterException {
        final int cacheId = generateCacheId(sp, application, api, httpVerb, resource);
        String responseFilter = getFromCache(cacheId);
        if (responseFilter.isEmpty()) {
            log.debug("response filters are not found in the cache");
            responseFilter = this.filterSchemaDao.find(sp, application, api, httpVerb, resource);
            this.addToCache(cacheId, responseFilter==null?"{}":responseFilter);
        }
        return "{}".equals(responseFilter)?null:responseFilter;
    }

    private int generateCacheId(String sp, String application, String api, String httpVerb, String resource) {
        return (sp + application + api + httpVerb + resource).hashCode();
    }

    private String getFromCache(final int key) {
        log.debug("retrieving response filters from cache: " + key);
        Request request = new Request.Builder()
                .url("http://localhost:8080/response-filter/" + key)
                .get()
                .build();
        return new OkHttpClient().newCall(request).request().body().toString();
//        HttpURLConnection connection = null;
//        URL requestUrl;
//        try {
//            requestUrl = new URL("http://localhost:8080/response-filter/" + key);
//            connection = (HttpURLConnection) requestUrl.openConnection();
//            connection.setDoOutput(true);
//            connection.setInstanceFollowRedirects(false);
//            connection.setRequestMethod("GET");
//            connection.setRequestProperty("Accept", "application/json");
//            connection.setUseCaches(false);
//            if (connection.getResponseCode() == 200) {
//                return readInputStream(connection.getInputStream());
//            }
//        } catch (Exception e) {
//            log.error("Error occurred while retrieving response filters from cache: " + key);
//        } finally {
//            if (connection != null) {
//                connection.disconnect();
//            }
//        }
//        return "";
    }

    private String readInputStream(InputStream input) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(input));
        StringBuilder sb = new StringBuilder();
        String output;
        while ((output = br.readLine()) != null) {
            sb.append(output);
        }
        br.close();
        return sb.toString();
    }

    private void addToCache(final int key, final String value) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                RequestBody requestBody = RequestBody.create(MediaType.parse("text/plain"), value);
                Request request = new Request.Builder()
                        .url("http://localhost:8080/response-filter/" + key)
                        .put(requestBody)
                        .build();
                try {
                    new OkHttpClient().newCall(request).execute();
                } catch (IOException e) {
                    log.error("Error occurred while adding response filters to the cache", e);
                }
                log.debug("Response filters are adding to the cache: " + key);
//                HttpURLConnection connection = null;
//                URL requestUrl;
//                try {
//                    requestUrl = new URL("http://localhost:8080/response-filter/" + key);
//                    connection = (HttpURLConnection) requestUrl.openConnection();
//                    connection.setDoOutput(true);
//                    connection.setInstanceFollowRedirects(false);
//                    connection.setRequestMethod("PUT");
//                    connection.setRequestProperty("Content-Type", "application/json");
//                    connection.setUseCaches(false);
//                    connection.getOutputStream().write(value.getBytes("UTF-8"));
//                    log.debug("Response filters are added to the cache: " + key);
//                } catch (Exception e) {
//                    log.error("Error occurred while adding response filters to the cache", e);
//                } finally {
//                    if (connection != null) {
//                        connection.disconnect();
//                    }
//                }
            }
        }).start();
    }

}
