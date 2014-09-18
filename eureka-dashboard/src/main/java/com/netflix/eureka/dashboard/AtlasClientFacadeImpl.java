package com.netflix.eureka.dashboard;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.config.ConfigurationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

@Singleton
public class AtlasClientFacadeImpl implements AtlasClientFacade {
    private static Logger log = LoggerFactory.getLogger(AtlasClientFacadeImpl.class);

    @Inject
    public AtlasClientFacadeImpl() {
        final String samplePropValue = ConfigurationManager.getConfigInstance().getString("sample.prop");
        log.info("sample config value " + samplePropValue);
    }

    @Override
    public String getMetrics(String metricsName, String startTimeStr, String endTimeStr) {
        return "";
    }


    private String buildClientRequestFor5xxErrors() {
        return "";
    }

}
