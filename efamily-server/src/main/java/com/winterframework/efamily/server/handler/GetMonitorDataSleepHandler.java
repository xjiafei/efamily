package com.winterframework.efamily.server.handler;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.winterframework.efamily.base.enums.StatusCode;
import com.winterframework.efamily.base.model.Context;
import com.winterframework.efamily.base.model.Response;
import com.winterframework.efamily.dto.GetMonitorDataRequest;
import com.winterframework.efamily.dto.GetMonitorDataResponse;
import com.winterframework.efamily.server.core.AbstractHandler;
import com.winterframework.efamily.server.exception.ServerException;
import com.winterframework.efamily.server.protocol.FmlRequest;
import com.winterframework.efamily.server.protocol.FmlResponse;
import com.winterframework.efamily.server.utils.StrUtils;
import com.winterframework.modules.spring.exetend.PropertyConfig;

/**
 * 获取家庭用户健康列表handler
 * @author floy
 *
 */
@Service("getMonitorDataSleepHandler")
public class GetMonitorDataSleepHandler extends AbstractHandler {
	@PropertyConfig("server.url.app")
	private String serverUrl;
	@PropertyConfig("app.server.get_monitor_sleep_data")
	private String urlPath;
	
	@Override
	protected FmlResponse doHandle(Context ctx, FmlRequest request) throws ServerException {
		GetMonitorDataRequest  bizReqList =  new GetMonitorDataRequest();
		bizReqList.setDateType(StrUtils.stringToInteger(request.getData().get("dateType"), null));
		bizReqList.setStartDateTime(StrUtils.stringToLong(request.getData().get("startDateTime"),null)) ;
		bizReqList.setEndDateTime(StrUtils.stringToLong(request.getData().get("endDateTime"),null)) ;
		bizReqList.setUserId(Long.valueOf(request.getData().get("userId"))) ;
		bizReqList.setDeviceId(StrUtils.stringToLong(request.getData().get("deviceId"),null)) ;
		
		Response<GetMonitorDataResponse> bizRes=http(serverUrl,urlPath,ctx,bizReqList,GetMonitorDataResponse.class);
		FmlResponse res=new FmlResponse(request);
        if(null!=bizRes){			
			res.setStatus(bizRes.getStatus().getCode()); 
			Map<String,String> responseMap = new HashMap<String,String>();
			if(bizRes.getData()!=null){
			   responseMap.put("sleep", bizRes.getData().getHealthyMonitorData());
			}   
			res.setData(responseMap);
		}else{
			res.setStatus(StatusCode.UNKNOW.getValue());
		}
		return res;
	}
}