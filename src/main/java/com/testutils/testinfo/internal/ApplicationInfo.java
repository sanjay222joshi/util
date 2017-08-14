package com.testutils.testinfo.internal;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ApplicationInfo implements JsonMapConvertible {

    private final String host, systemPort, environment, logicalName, build, teamCityBuildId, teamCityProjectName;

    public ApplicationInfo(String host, String systemPort, String environment, String logicalName,
                           String build, String teamCityBuildId, String teamCityProjectName) {
        this.host = host;
        this.systemPort = systemPort;
        this.environment = environment;
        this.logicalName = logicalName;
        this.build = build;
        this.teamCityBuildId = teamCityBuildId;
        this.teamCityProjectName = teamCityProjectName;
    }


    public String getHost() {
        return host;
    }

    public String getSystemPort() {
        return systemPort;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getLogicalName() {
        return logicalName;
    }

    public String getBuild() {
        return build;
    }

    public String getTeamCityBuildId() {
        return teamCityBuildId;
    }

    public String getTeamCityProjectName() {
        return teamCityProjectName;
    }

    @Override
    public Map<String, Object> jsonObjectAsMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("host", host);
        map.put("systemPort", systemPort);
        map.put("environment", environment);
        map.put("logicalName", logicalName);
        map.put("build", build);
        map.put("teamCityBuildId", teamCityBuildId);
        map.put("teamCityProjectName", teamCityProjectName);
        return map;
    }

    public static ApplicationInfo fromMap(Map<String, Object> map) {
        String host = (String) map.get("host");
        String systemPort = (String) map.get("systemPort");
        String environment = (String) map.get("environment");
        String logicalName = (String) map.get("logicalName");
        String build = (String) map.get("build");
        String teamCityBuildId = (String) map.get("teamCityBuildId");
        String teamCityProjectName = (String) map.get("teamCityProjectName");
        return new ApplicationInfo(host, systemPort, environment, logicalName, build, teamCityBuildId, teamCityProjectName);
    }


    public static ApplicationInfo fromJSONObject(JSONObject status) {
        String host = status.getJSONObject("instance").getString("host");
        String systemPort = status.getJSONObject("ports").getString("systemPort");
        String environment = status.getJSONObject("instance").getString("environment");
        String logicalName = status.getJSONObject("instance").getString("logicalName");
        String build = status.getJSONObject("version").getString("projectBuild");
        String teamCityBuildId = status.getJSONObject("reference").getJSONObject("teamcity").getString("tcBuildTypeId");
        String teamCityProjectName = status.getJSONObject("reference").getJSONObject("teamcity").getString("tcProjectName");
        return new ApplicationInfo(host, systemPort, environment, logicalName, build, teamCityBuildId, teamCityProjectName);
    }
}
