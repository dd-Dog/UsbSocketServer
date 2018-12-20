package com.flyscale.ecserver.util;

/**
 * Created by bian on 2018/12/20.
 */

public interface QueryCompeleteCallback {
    void  onQuerySuccess(String result);
    void  onQueryFailure(Exception e);
}
