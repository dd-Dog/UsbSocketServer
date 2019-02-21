// IDataInfo.aidl
package com.EC.service;


// Declare any non-default types here with import statements

interface IDataInfo{
    void getAudioData(in byte[] data,int len);
}
