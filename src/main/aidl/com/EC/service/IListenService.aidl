// IListenService.aidl
package com.EC.service;

import com.EC.service.IDataInfo;

// Declare any non-default types here with import statements

interface IListenService {
    void setCallBack(in IDataInfo info);
    void setVoiceData(String data);
}