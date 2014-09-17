package com.netflix.eureka.dashboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HealthCheckServlet extends HttpServlet {
    private static Logger log = LoggerFactory.getLogger(HealthCheckServlet.class);

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        new AtlasClientFacadeImpl().getMetrics("mock","now","e-3h");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("Handling healthcheck");
        resp.getWriter().write("OK");
    }
}
