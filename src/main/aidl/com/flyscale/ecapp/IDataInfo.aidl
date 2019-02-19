// IDataInfo.aidl
package com.flyscale.ecapp;


// Declare any non-default types here with import statements

interface IDataInfo{
    void getAudioData(in byte[] data,int len);
}
