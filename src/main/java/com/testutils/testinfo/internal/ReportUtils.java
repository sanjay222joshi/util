package com.testutils.testinfo.internal;

import com.markit.n6platform.util.basic.ExceptionUtils;
import com.markit.n6platform.util.basic.StringUtils;

public class ReportUtils {

    public static String readableName(String name) {
        return ExceptionUtils.swallowException(() -> readableNameInternal(name), name);
    }

    private static String readableNameInternal(String name) {
        if (StringUtils.isBlank(name)) {
            return name;
        }
        String existing = name.trim();
        existing = existing.replaceAll("test", "");
        existing = existing.replaceAll("Test", "");

        boolean cons = false;

        StringBuilder newName = new StringBuilder();
        for (int i = 0; i < existing.length(); i++) {
            char c = existing.charAt(i);
            if (c == '_') {
                if(cons){
                    newName.append("-");
                }else {
                    newName.append(" ");
                }
            } else if (i == 0) {
                newName.append(Character.toUpperCase(existing.charAt(0)));
            } else if (Character.isUpperCase(c)) {
                newName.append(' ');

                StringBuilder b = new StringBuilder();
                boolean first = true;
                char lc = c;

                boolean appendFirst = true;
                for (int j = i + 1; j < existing.length()
                        && (Character.isUpperCase(c = existing.charAt(j))); j++, i++) {

                    appendFirst = false;
                    cons = true;
                    if (first) {
                        b.append(lc);
                        first = false;
                    }
                    b.append(c);
                }

                newName.append(b);
                if (appendFirst) {
                    newName.append(Character.toLowerCase(lc));
                }
            } else {
                if (cons) {
                    newName.replace(newName.length() - 1, newName.length(), " " + newName.charAt(newName.length() - 1));
                    cons = false;
                }
                newName.append(c);
            }
        }
        return newName.toString().replace("  ", " ").trim();
    }
}
