package com.netflix.eureka.dashboard;


import com.google.inject.AbstractModule;
import com.netflix.governator.annotations.Modules;
import com.netflix.karyon.KaryonBootstrap;
import com.netflix.karyon.KaryonServer;
import com.netflix.karyon.archaius.ArchaiusBootstrap;

@ArchaiusBootstrap
@KaryonBootstrap(name = "eureka-dashboard")
@Modules(include = {
 SystemDashboard.SystemDashboardModule.class
})
public class SystemDashboard {

    public static class SystemDashboardModule extends AbstractModule {

        @Override
        protected void configure() {

        }
    }

    public static void main(String[] args) {
        KaryonServer.main(new String[] {SystemDashboard.class.getName()});
    }
}
