package com.xr.service;
/**
 *
 * @author : HQX 
 *
 * @version : 1.0.0 
 *
 * 2019年1月9日  上午10:51:27
 *
 */
interface IDataInfo {

	void getAudioData(in  byte[] data,int len);
	void getPctoAppData(String data);
}
