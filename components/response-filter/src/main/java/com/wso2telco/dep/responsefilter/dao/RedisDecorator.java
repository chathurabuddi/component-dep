package com.wso2telco.dep.responsefilter.dao;

import com.wso2telco.dep.responsefilter.exception.ResponseFilterException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RedisDecorator implements FilterSchemaDao {

    protected Log log = LogFactory.getLog(this.getClass());
    private final FilterSchemaDao filterSchemaDao;

    public RedisDecorator(FilterSchemaDao filterSchemaDao) {
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
        return "";
    }

    private void addToCache(final int key, final String value) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                log.debug("Response filters are adding to the cache: " + key);
            }
        }).start();
    }
}
