package com.netflix.eureka.dashboard;

public interface AtlasClientFacade {
    String getMetrics(String metricsName, String startTimeStr, String endTimeStr);
}
