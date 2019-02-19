// IListenService.aidl
package com.flyscale.ecserver;

import com.flyscale.ecapp.IDataInfo;

// Declare any non-default types here with import statements

interface IListenService {
    void setCallBack(in IDataInfo info);
    void setVoiceData(String data);
}