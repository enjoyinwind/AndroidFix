/*
 * Copyright (C) 2016 Baidu, Inc. All Rights Reserved.
 */
package com.dodola.rocoofix.utils

/**
 * Created by jixin.jia on 15/11/10.
 */
class NuwaSetUtils {
    public static boolean isExcluded(String path, Set<String> excludeClass) {
        def isExcluded = false;
        excludeClass.each { exclude ->
            if (path.matches(exclude.substring(0, exclude.lastIndexOf(".")) + "[a-zA-Z0-9_\$]*\\.class")) {
                isExcluded = true
            }
        }
        return isExcluded
    }

    public static boolean isExcludedInPackage(String path, Set<String> excludePackage) {
        def isExcluded = false;
        excludePackage.each { exclude ->
            if (path.startsWith(exclude)) {
                isExcluded = true
            }
        }
        return isExcluded
    }

    public static boolean isIncluded(String path, Set<String> includePackage) {
        if (includePackage.size() == 0) {
            return true
        }

        def isIncluded = false;
        includePackage.each { include ->
            if (path.contains(include)) {
                isIncluded = true
            }
        }
        return isIncluded
    }
}
