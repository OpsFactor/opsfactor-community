package com.opsfactor.community.platform.list;

import java.util.List;

public abstract class UtilList {

    public static boolean isNull(List list) {
        return (list == null || list.isEmpty());
    }
}
