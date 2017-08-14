package com.testutils.testinfo;

import com.testutils.testinfo.internal.ApplicationInfo;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class ApplicationInfoProvider {

    private static final File FILE_PATH = new File(ApplicationInfoProvider.class.getClassLoader().getResource(".").getFile(), "applicationsInfo.json");
    private static AtomicReference<List<ApplicationInfo>> applicationsInfo = new AtomicReference<>();

    public static void addApplication(List<String> aa) throws Exception {
    	//ToDo
    }

    public static List<ApplicationInfo> getApplicationInfoList() throws IOException {
    	//ToDo
    	return Collections.emptyList();
    }

}
