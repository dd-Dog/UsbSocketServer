package com.xr.service;
/**
 *
 * @author : HQX 
 *
 * @version : 1.0.0 
 *
 * 2019年1月9日  上午10:50:26
 *
 */
import com.xr.service.IDataInfo;

interface IListenService {

	void setCallBack(IDataInfo info);
	void setVoiceData(String data);
}
