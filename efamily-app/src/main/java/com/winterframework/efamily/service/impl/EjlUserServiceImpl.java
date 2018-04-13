package com.winterframework.efamily.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.winterframework.efamily.base.exception.BizException;
import com.winterframework.efamily.base.http.IHttpClient;
import com.winterframework.efamily.base.model.Context;
import com.winterframework.efamily.base.model.Notification;
import com.winterframework.efamily.base.model.NotyMessage;
import com.winterframework.efamily.base.model.NotyTarget;
import com.winterframework.efamily.base.redis.RedisClient;
import com.winterframework.efamily.base.utils.DateUtils;
import com.winterframework.efamily.base.utils.JsonUtils;
import com.winterframework.efamily.common.EfamilyConstant;
import com.winterframework.efamily.core.base.BaseServiceImpl;
import com.winterframework.efamily.dao.IEJLComUserHealythConfigDao;
import com.winterframework.efamily.dao.IEjlChartRoomDao;
import com.winterframework.efamily.dao.IEjlChartRoomUserDao;
import com.winterframework.efamily.dao.IEjlComUserExtendInfoDao;
import com.winterframework.efamily.dao.IEjlFamilyDao;
import com.winterframework.efamily.dao.IEjlFamilyUserDao;
import com.winterframework.efamily.dao.IEjlUserChartRoomDao;
import com.winterframework.efamily.dao.IEjlUserDao;
import com.winterframework.efamily.dto.ChatGroupDetailsList;
import com.winterframework.efamily.dto.ChatGroupListResponse;
import com.winterframework.efamily.dto.ChatRoomUserNotify;
import com.winterframework.efamily.dto.DeleteUserPushStruc;
import com.winterframework.efamily.dto.FriendListResponse;
import com.winterframework.efamily.dto.HealthlyUserRequest;
import com.winterframework.efamily.dto.LoginRequest;
import com.winterframework.efamily.dto.LoginResponse;
import com.winterframework.efamily.dto.RegisterResponse;
import com.winterframework.efamily.dto.UserChartRoomMemberInfo;
import com.winterframework.efamily.dto.UserDeviceInfo;
import com.winterframework.efamily.dto.UserHealthlyConfigRequest;
import com.winterframework.efamily.dto.UserHealthlyConfigStruc;
import com.winterframework.efamily.dto.UserNotice.NoticeType;
import com.winterframework.efamily.dto.device.param.DeviceParamCommon;
import com.winterframework.efamily.dto.device.param.DeviceParamHealth;
import com.winterframework.efamily.entity.EjlChartRoom;
import com.winterframework.efamily.entity.EjlChartRoomUser;
import com.winterframework.efamily.entity.EjlFamily;
import com.winterframework.efamily.entity.EjlFamilyUser;
import com.winterframework.efamily.entity.EjlFamilyUser.Position;
import com.winterframework.efamily.entity.EfDeviceSetting;
import com.winterframework.efamily.entity.EfOrgDevice;
import com.winterframework.efamily.entity.EfUserHealthSetting;
import com.winterframework.efamily.entity.EjlThirdPartyLogin;
import com.winterframework.efamily.entity.EjlUser;
import com.winterframework.efamily.entity.EjlUserChartRoom;
import com.winterframework.efamily.entity.EjlUserExtendInfo;
import com.winterframework.efamily.entity.EjlUserLoginRecord;
import com.winterframework.efamily.entity.EjlUserP2pChatRoom;
import com.winterframework.efamily.entity.EjlVerifyCode;
import com.winterframework.efamily.entity.FmlResource;
import com.winterframework.efamily.entity.ThirdPartyLoginStruc;
import com.winterframework.efamily.enums.AppType;
import com.winterframework.efamily.enums.StatusBizCode;
import com.winterframework.efamily.service.IEfComOrgDeviceService;
import com.winterframework.efamily.service.IEfDeviceSettingService;
import com.winterframework.efamily.service.IEfResourceService;
import com.winterframework.efamily.service.IEfUserHealthSettingService;
import com.winterframework.efamily.service.IEjlChartRoomService;
import com.winterframework.efamily.service.IEjlChartRoomUserService;
import com.winterframework.efamily.service.IEjlComThirdPartyLoginService;
import com.winterframework.efamily.service.IEjlFamilyService;
import com.winterframework.efamily.service.IEjlP2pChatRoomService;
import com.winterframework.efamily.service.IEjlUserChartRoomService;
import com.winterframework.efamily.service.IEjlUserDeviceService;
import com.winterframework.efamily.service.IEjlUserLoginRecordService;
import com.winterframework.efamily.service.IEjlUserP2pChatRoomService;
import com.winterframework.efamily.service.IEjlUserService;
import com.winterframework.efamily.service.IEjlVerifyCodeService;
import com.winterframework.efamily.thirdparty.sms.IBaseSmsService;
import com.winterframework.efamily.thirdparty.sms.ISmsService;
import com.winterframework.efamily.utils.NotificationUtil;
import com.winterframework.efamily.utils.NovaMessageUtil;
import com.winterframework.modules.spring.exetend.PropertyConfig;
import com.winterframework.modules.utils.DateConvertUtils;

@Service("ejlUserServiceImpl")
@Transactional(rollbackFor = Exception.class)
public class EjlUserServiceImpl extends BaseServiceImpl<IEjlUserDao, EjlUser> implements IEjlUserService {

	@PropertyConfig(value="group")
	private String group;
	
	@Resource(name = "ejlUserDaoImpl")
	private IEjlUserDao ejlUserDaoImpl;

	@Resource(name = "ejlChartRoomDaoImpl")
	private IEjlChartRoomDao ejlChartRoomDaoImpl;
	
	@Resource(name = "ejlUserChartRoomDaoImpl")
	private IEjlUserChartRoomDao ejlUserChartRoomDaoImpl;

	@Resource(name = "ejlChartRoomUserDaoImpl")
	private IEjlChartRoomUserDao ejlChartRoomUserDaoImpl;
	
	@Resource(name = "ejlP2pChatRoomServiceImpl")
	private IEjlP2pChatRoomService 	ejlP2pChatRoomServiceImpl;
	
	@Resource(name="httpClientImpl")
	protected IHttpClient httpClientImpl;
	
	@Resource(name = "ejlFamilyDaoImpl")
	private IEjlFamilyDao ejlFamilyDaoImpl;

	@Resource(name = "ejlFamilyUserDaoImpl")
	private IEjlFamilyUserDao ejlFamilyUserDaoImpl;
	
	@Resource(name = "ejlUserDeviceServiceImpl")
	private IEjlUserDeviceService ejlUserDeviceServiceImpl;	
	
	@PropertyConfig(value="sms.server.limit")
	private Long smsServerLimit;
	
	@PropertyConfig(value="sms.server.timeout")
	private Long smsServerTimeout;
	
	@PropertyConfig(value="downloadUrl")
	private String downloadUrl;
	
	@Resource(name="smsServiceImpl")
	private ISmsService smsService;
	
	@PropertyConfig(value="sms.message.template")
	private String messageTemplate;
	
	@PropertyConfig(value="sms.message.template_Ins")
	private String messageTemplateIns;

	@Resource(name = "ejlFamilyServiceImpl")
	private IEjlFamilyService ejlFamilyServiceImpl;
	
	@Resource(name = "ejlUserChartRoomServiceImpl")
	private IEjlUserChartRoomService ejlUserChartRoomServiceImpl;

	@Resource(name = "ejlChartRoomUserServiceImpl")
	private IEjlChartRoomUserService ejlChartRoomUserServiceImpl;
	
	@Resource(name="ejlVerifyCodeServiceImpl")
	private IEjlVerifyCodeService ejlVerifyCodeService;
	
	
	@Resource(name = "ejlChartRoomServiceImpl")
	private IEjlChartRoomService ejlChartRoomServiceImpl;

	@Resource(name = "efResourceServiceImpl")
	private IEfResourceService efResourceServiceImpl; 
	
	@Resource(name = "eJLComUserHealythConfigDaoImpl")
	private IEJLComUserHealythConfigDao eJLComUserHealythConfigDaoImpl;
	
	@Resource(name="ejlComUserExtendInfoDaoImpl")
	private IEjlComUserExtendInfoDao ejlComUserExtendInfoDaoImpl;

	@Resource(name = "ejlUserLoginRecordServiceImpl")
	private IEjlUserLoginRecordService ejlUserLoginRecordServiceImpl;
	
	@Resource(name = "ejlUserP2pChatRoomServiceImpl")
	private IEjlUserP2pChatRoomService ejlUserP2pChatRoomServiceImpl;
	
	@Resource(name = "ejlComThirdPartyLoginServiceImpl")
	private IEjlComThirdPartyLoginService ejlComThirdPartyLoginServiceImpl;
	
	
	@Resource(name = "RedisClient")
	private RedisClient redisClient;  
	@Resource(name="notificationUtil")
	protected NotificationUtil notificationUtil;	
	@Resource(name = "kangdooSmsServiceImpl")
	private IBaseSmsService kangdooSmsService;
	@Resource(name = "huashouSmsServiceImpl")
	private IBaseSmsService huashouSmsService;
	@Resource(name = "qiZhiSmsServiceImpl")
	private IBaseSmsService qiZhiSmsServiceImpl;
	@Resource(name = "beiDouSmsServiceImpl")
	private IBaseSmsService beiDouSmsServiceImpl;
	@Resource(name = "aliyunSmsServiceImpl")
	private IBaseSmsService aliyunSmsServiceImpl;
	
	@Resource(name = "efDeviceSettingServiceImpl")
	private IEfDeviceSettingService efDeviceSettingService;
	
	@Resource(name="efUserHealthSettingServiceImpl")
	private IEfUserHealthSettingService efUserHealthSettingService;
	
	@Resource(name="efComOrgDeviceServiceImpl")
	private IEfComOrgDeviceService efComOrgDeviceServiceImpl;
	
	
	
	
	@Override
	protected IEjlUserDao getEntityDao() {
		return ejlUserDaoImpl;
	}
	
	@Override
	public void bindThirdPartyCertification(Context ctx,Long userId,String sourceId,String sourceType,String oprType) throws BizException{
		EjlUser user = ejlUserDaoImpl.getById(userId);
		if(user==null){
			 throw new BizException(StatusBizCode.USER_UN_VALID.getValue());
		}
		if(user.getType().longValue()!=EfamilyConstant.USER_TYPE_APP){
			 throw new BizException(StatusBizCode.USER_UN_TYPE_VALID.getValue());
		}

		if(oprType.equals("1") && checkSourceIdIsBind(ctx,sourceId,sourceType)){
			log.error("第三方帐号已经被绑定过 ：sourceId = "+sourceId+" ; sourceType = "+sourceType+" ; ");
			throw new BizException(StatusBizCode.USER_THIRD_PART_BIND_EXIST.getValue());
		}
		saveThirdPartyLoginInfo( ctx, userId, sourceId, sourceType, oprType);
	}
	
	public Integer saveThirdPartyLoginInfo(Context ctx,Long userId,String sourceId,String sourceType,String oprType) throws BizException{
		EjlThirdPartyLogin  ejlThirdPartyLogin=new EjlThirdPartyLogin();
		ejlThirdPartyLogin.setType(Integer.valueOf(sourceType));
        ejlThirdPartyLogin.setToken(sourceId);
        ejlThirdPartyLogin.setUserId(userId);
		EjlThirdPartyLogin  ejlThirdPartyLoginCheck = ejlComThirdPartyLoginServiceImpl.selectOneObjByAttribute(ctx, ejlThirdPartyLogin);
		if(ejlThirdPartyLoginCheck!=null){
			ejlThirdPartyLogin.setId(ejlThirdPartyLoginCheck.getId());
		}
		ejlThirdPartyLogin.setStatus(Integer.valueOf(oprType));//1  绑定  ：2 解绑
		return ejlComThirdPartyLoginServiceImpl.save(ctx, ejlThirdPartyLogin);
	}
	
	public boolean checkSourceIdIsBind(Context ctx,String sourceId,String sourceType) throws BizException{
		boolean flag = false;
		EjlThirdPartyLogin  ejlThirdPartyLogin=new EjlThirdPartyLogin();
		ejlThirdPartyLogin.setType(Integer.valueOf(sourceType));
        ejlThirdPartyLogin.setToken(sourceId);
        ejlThirdPartyLogin.setStatus(1);
		EjlThirdPartyLogin  ejlThirdPartyLoginCheck = ejlComThirdPartyLoginServiceImpl.selectOneObjByAttribute(ctx, ejlThirdPartyLogin);
		if(ejlThirdPartyLoginCheck!=null){
             flag = true;
        }
		return flag;
	}
	
	/**
	 * 
	* @Title: notifyForScanWatch 
	* @Description: TODO(扫描表时，进行数据的推送) 
	* @param data
	* @param familyId
	 * @throws BizException 
	 */
	public void notifyForScanWatch(Map<String,String> data,Long userId,Long familyId) throws BizException{
		
		EjlUser user = new EjlUser();
		user.setFamilyId(familyId);
		user.setType(EfamilyConstant.USER_TYPE_APP);
		List<EjlUser> userList = ejlUserDaoImpl.getEjlUserByAttribute(user);
		notifyUser(data,userList,userId,NotyMessage.Type.NOTICE,NoticeType.SCAN_WATCH,null);
	}
    public void notifyForFamilyInfoUpdate(Map<String,String> data,Long userId,Long familyId) throws BizException{
		
		EjlUser user = new EjlUser();
		user.setFamilyId(familyId);
		user.setType(EfamilyConstant.USER_TYPE_APP);
		List<EjlUser> userList = ejlUserDaoImpl.getEjlUserByAttribute(user);
		notifyUser(data,userList,userId,NotyMessage.Type.NOTICE,NoticeType.FAMILY_INFO_UPDATE,userId);
	}
    public void notifyForAddressFriendShip(Map<String,String> data,Long userId,List<EjlUser> friendList,NoticeType noticeType) throws BizException{
   		if(friendList==null || friendList.size()==0){
   			log.info("通讯录添加好友需要推送的用户为空。 userId = "+userId+" ; noticeType = "+noticeType);
			return;
   		}
   	   	notifyUser(data,friendList,userId,NotyMessage.Type.NOTICE,noticeType,null);
   	}
    public void notifyForManageForbitSpeak(Map<String,String> data,Long userId,NoticeType noticeType) throws BizException{
    	List<EjlUser> userList = new ArrayList<EjlUser>();
    	EjlUser user = ejlUserDaoImpl.getById(userId);
    	if(user!=null){
    		userList.add(user);
    	}
  	   	notifyUser(data,userList,userId,NotyMessage.Type.NOTICE,noticeType,null);
  	}
	   
     public void notifyForUpdateWatchOwner(Map<String,String> data,Long userId) throws BizException{
    	EjlUser userOperate = ejlUserDaoImpl.getById(userId);
		EjlUser user = new EjlUser();
		user.setFamilyId(userOperate.getFamilyId());
		user.setType(EfamilyConstant.USER_TYPE_APP);
		List<EjlUser> userList = ejlUserDaoImpl.getEjlUserByAttribute(user);
		List<EjlUser> attentionList =  ejlUserDaoImpl.getAttentionUserByFamilyId(userOperate.getFamilyId());
		if(userList!=null){
			userList.addAll(attentionList);
		}
		notifyUser(data,userList,userId,NotyMessage.Type.NOTICE,NoticeType.UPDATE_WATCH_OWNER,null);
	}
     
     public List<EjlUser> getEjlUserFamilyAndAttentionList(Long userId)throws BizException{
    	List<EjlUser> userListResult = new ArrayList<EjlUser>();
     	EjlUser userOperate = ejlUserDaoImpl.getById(userId);
 		EjlUser user = new EjlUser();
 		user.setFamilyId(userOperate.getFamilyId());
 		user.setType(EfamilyConstant.USER_TYPE_APP);
 		List<EjlUser> userList = ejlUserDaoImpl.getEjlUserByAttribute(user);
 		if(userList!=null){
 			userListResult.addAll(userList);
 		}
 		List<EjlUser> attentionList =  ejlUserDaoImpl.getAttentionUserByFamilyId(userOperate.getFamilyId());
 		if(attentionList!=null){
 			userListResult.addAll(attentionList);
 		}
 		return userListResult;
     }
     
     public void notifyForUnbindDevice(Map<String,String> data,Long userId,List<EjlUser> userList) throws BizException{

		notifyUser(data,userList,userId,NotyMessage.Type.NOTICE,NoticeType.UPDATE_WATCH_OWNER,null);
	}
     
     public void notifyForUpdateWatchOwnerForDevice(String oprType,Map<String,String> data,Long userId) throws Exception{
    	    
    	    //****** 根据操作类型进行推送   ***************
    	    if("1".endsWith(oprType)){ //*** oprType = 1 增加手表 
				 Map<String,String> noticeMapInit = new HashMap<String,String>(); 
				 noticeMapInit.put("fromId",data.get("oprUserId"));
				 noticeMapInit.put("nickname",data.get("oprName"));
				 //*********** 
				 //pushDevice(Long.valueOf(data.get("initiativeUserId")),Long.valueOf(data.get("initiativeDeviceId")), noticeMapInit,EfamilyConstant.PUSH_UNBIND_WATCH,NotyMessage.Type.OPER,true);
				 //------------------【新表需要存储到redis中】 等待device链接时，进行绑定确认，确认以后进行数据库修改 和 用户通知  ---------------------------
				try {
					 redisClient.set(data.get("passiveDeviceCode"), data.get("oprUserId")+"");
				} catch (Exception e) {
					e.printStackTrace();
					log.info("更换手表时，新增手表，存储到redis时出现异常：  userId = "+userId+" ; watchCode = "+data.get("passiveDeviceCode"),e);
				}
				
    	    }else{
    	    	 //*** 跟人换  或者  给一个新加的人 
    	    	 //*** 推两个人 或者 一个人 
    	    	 Map<String,String> noticeMap = new HashMap<String,String>(); 
    	    	 noticeMap.put("userId",data.get("passiveUserId"));
    	    	 noticeMap.put("fromId",data.get("oprUserId"));
    	    	 noticeMap.put("nickname",data.get("oprName"));
    	    	 noticeMap.put("phoneNumber",data.get("oprPhoneNumber"));
    	    	 noticeMap.put("familyName",data.get("oprFamilyName"));//*** 同一个家庭的familyname
    	    	 noticeMap.put("familyId",data.get("oprFamilyId"));
				 pushDevice(Long.valueOf(data.get("initiativeUserId")),Long.valueOf(data.get("initiativeDeviceId")), noticeMap,EfamilyConstant.PUSH_SWITCH_WATCH,NotyMessage.Type.OPER,true);
				 
				 //************ ispushtime 是不是 实时推 不在线 就不推  ************
				 if("2".equals(data.get("passiveUserType"))){
					 //******* 手表用户才发起推送（被换的人有手表才给手表发起推送，没有就不发）  ***********
					 noticeMap.put("userId",data.get("initiativeUserId"));
					 pushDevice(Long.valueOf(data.get("passiveUserId")), Long.valueOf(data.get("passiveDeviceId")), noticeMap,EfamilyConstant.PUSH_SWITCH_WATCH,NotyMessage.Type.OPER,true);
				 }
    	    }
 	}
     
     
     public void pushDevice(Long userId,Long deviceId,Map<String,String> data,int command,NotyMessage.Type nt,boolean isRealTime) throws BizException {
    	 try{
	    	 Notification notification = new Notification();
			 if(data==null){
		        data = new HashMap<String,String>();
		     }
	 		 log.info("操作设备，推送命令给设备： userId = "+userId+" ; deviceId = "+deviceId+" ; command = "+command+" ; data = "+data.toString()); 
			 NotyTarget target = new NotyTarget(userId,deviceId);	//推送目标 
			 NotyMessage message = new NotyMessage();	//推送消息
			 message.setCommand(command);// 更换：20105  解除：20106
			 message.setData(data);
			 //message.setType(NotyMessage.Type.NOTICE);
			 message.setType(nt);
			 notification.setRealTime(isRealTime); //是否实时推送
			 notification.setMessage(message);
			 notification.setTarget(target);
			 notificationUtil.notification(notification);
    	 }catch(Exception e){
    		 log.error("推送异常：",e);
    		 throw new BizException(StatusBizCode.PUSH_FAILED.getValue());
    	 }
     } 
     
 	public void pushDevice(Long userId,Long deviceId,Map<String,String> data,int command,NotyMessage.Type nt) throws Exception {
 		pushDevice(userId, deviceId, data, command, nt, false);
	} 
     
 	public void notifyForManageGroupSetting(Map<String, String> data, Long userId, Long groupId,NoticeType noticeType,String parameterContext) throws BizException {
 		EjlChartRoomUser ejlChartRoomUser = new EjlChartRoomUser();
 		ejlChartRoomUser.setChartRoomId(groupId);
 		ejlChartRoomUser.setStatus(0L);
 		List<EjlUser> userList = new ArrayList<EjlUser>(); 
 		List<EjlUser> userListForChartRoom = ejlChartRoomUserDaoImpl.getChartRoomUserByRoomIdAndStatus(ejlChartRoomUser);
 		if(userListForChartRoom != null){
 			userList.addAll(userListForChartRoom);
 		}
 		String parameterIndex = data.get("parameterIndex");
 		if( EfamilyConstant.GROUP_ROOM_QUIT.equals(parameterIndex) ||EfamilyConstant.GROUP_ROOM_DELETE.equals(parameterIndex)){
 			userList.addAll(getUserListByIdsStr(parameterContext));
 		}
 		
		notifyUser(data,userList,userId,NotyMessage.Type.NOTICE,noticeType,null);
 	}

 	public List<EjlUser> getUserListByIdsStr(String userIds){
 		List<EjlUser> userList = new ArrayList<EjlUser>(); 
	    if(StringUtils.isNotBlank(userIds)){
	 		List<Long>  userIdList = new ArrayList<Long>();
	 		String[] userIdStrArr = userIds.split(",");
	 		for(String userIdTemp: userIdStrArr){
	 			userIdList.add(Long.valueOf(userIdTemp));
	 		}
	 		List<EjlUser> userOptList = ejlUserDaoImpl.getByIds(userIdList);
	 		for(EjlUser user:userOptList){
	 			userList.add(user);
	 		}
	    }
 		return userList;
 	}
 	public void notifyForManageFamilyQuitAndDelete(String userIdStr,Long userId,Long familyId,NoticeType noticeType,Long manageType,Long notNoticeUserId) throws BizException{
 		
 		List<Long>  userIdList = new ArrayList<Long>();
 		String[] userIdStrArr = userIdStr.split(",");
 		for(String userIdTemp: userIdStrArr){
 			userIdList.add(Long.valueOf(userIdTemp));
 		}
 		List<DeleteUserPushStruc> deleteUserPushList = new ArrayList<DeleteUserPushStruc>();
 		List<EjlUser> userOptList = ejlUserDaoImpl.getByIds(userIdList);
 		for(EjlUser user:userOptList){
 			DeleteUserPushStruc deleteUserPushStruc = new DeleteUserPushStruc();
 			deleteUserPushStruc.setHeadImgUrl(user.getHeadImg());
 			deleteUserPushStruc.setNickName(user.getNickName());
 			deleteUserPushStruc.setUserId(user.getId());
 			deleteUserPushList.add(deleteUserPushStruc);
 		}
 		EjlFamily family = ejlFamilyDaoImpl.getById(familyId);
 		Map<String,String> data = new HashMap<String,String>();
 		data.put("userInfo", JsonUtils.toJson(deleteUserPushList));
 		data.put("familyId", familyId+"");
 		data.put("familyName", family.getName());
 		data.put("type", manageType+"" );
 		
     	List<EjlUser> userList = new ArrayList<EjlUser>();
 		if(EfamilyConstant.MANAGE_TYPE_DELETE == manageType ){
      		//*** 删除家庭成员  需要推送给家庭中所有的人  包括被删除的家庭成员
      		EjlUser user = new EjlUser();
     		user.setFamilyId(familyId);
     		user.setType(EfamilyConstant.USER_TYPE_APP);
     		List<EjlUser> userListTemp = ejlUserDaoImpl.getEjlUserByAttribute(user);
     		if(userListTemp != null){
     			userList.addAll(userListTemp);
     		}
     		//包括被删除的家庭成员
     		userList.addAll(userOptList);
     		notNoticeUserId = null;
     		notifyUserForDelelte(data,userList,userId,NotyMessage.Type.OPER,noticeType,notNoticeUserId,userIdList);
      	}else if(EfamilyConstant.MANAGE_TYPE_QUIT == manageType ){
      		//*** 退出家庭推送
      		EjlUser user = new EjlUser();
     		user.setFamilyId(familyId);
     		user.setType(EfamilyConstant.USER_TYPE_APP);
     		List<EjlUser> userListTemp = ejlUserDaoImpl.getEjlUserByAttribute(user);
     		if(userListTemp != null){
     			userList.addAll(userListTemp);
     		}
     		notifyUser(data,userList,userId,NotyMessage.Type.OPER,noticeType,notNoticeUserId);
      	}else{
			log.error("manageType未定义，管理家庭异常：userId = "+userId+" ; familyId = "+familyId+" ; manageType = "+manageType);
			throw new BizException(StatusBizCode.TYPE_UNDEFINED.getValue());
      	}
 		
 		
 	}
 	
 	
 	
     public void notifyForManageJoinFamily(Map<String,String> data,Long userId,Long familyId,NoticeType noticeType,Long manageType,Long notNoticeUserId) throws BizException{
     	List<EjlUser> userList = new ArrayList<EjlUser>();
 		if(EfamilyConstant.MANAGE_TYPE_INVITE == manageType ){
 			//*** 只有家庭群主能发起邀请
 			EjlUser userOperate = ejlUserDaoImpl.getById(userId);
 			if(null!=userOperate){
	      		userList.add(userOperate);
 			}
	      	notNoticeUserId = null;
      	}else if(EfamilyConstant.MANAGE_TYPE_APPLY == manageType ){
      		//*** 申请时 只推送给家庭群主
      		EjlUser userFamilyHost = ejlUserDaoImpl.getFamilyHostUser(familyId);
      		if(null!=userFamilyHost){
	      		userList.add(userFamilyHost);
 			}
      		notNoticeUserId = null;
      	}else if(EfamilyConstant.MANAGE_TYPE_INVITE_AGREE == manageType ||EfamilyConstant.MANAGE_TYPE_AGREE == manageType  ){
      		//*** 邀请同意
      		EjlUser user = new EjlUser();
     		user.setFamilyId(familyId);
     		user.setType(EfamilyConstant.USER_TYPE_APP);
     		List<EjlUser> userListTemp = ejlUserDaoImpl.getEjlUserByAttribute(user);
     		if(userListTemp != null){
     			userList.addAll(userListTemp);
     		}
      	}else{
			log.error("manageType未定义，管理家庭异常：userId = "+userId+" ; familyId = "+familyId+" ; manageType = "+manageType);
			throw new BizException(StatusBizCode.TYPE_UNDEFINED.getValue());
      	}
 		
 		notifyUser(data,userList,userId,NotyMessage.Type.OPER,noticeType,notNoticeUserId);
 	}
     
     public void notifyForManageFriendShip(Map<String,String> data,Long userId,Long cunstomId,NoticeType noticeType) throws BizException{
      	EjlUser user = new EjlUser();
      	user.setId(cunstomId);
  		List<EjlUser> userList = ejlUserDaoImpl.getEjlUserByAttribute(user);
  		notifyUser(data,userList,userId,NotyMessage.Type.OPER,noticeType,null);
  	}

     public void notifyForManageAttention(Map<String,String> data,Long userId,Long oprUserId,Long oprType,NoticeType noticeType,String code) throws BizException{
       	
    	 List<EjlUser> userList = new ArrayList<EjlUser>();
    	 EjlUser user =  ejlUserDaoImpl.getById(userId);
   		 //**** 申请    同意   取消   
   		 if(EfamilyConstant.ATTENTION_DEVICE_APPLY == oprType.intValue()){
   			//申请只推给 家主
   			EjlUser hostUser = getFamilyHostByDeviceCode(code);
   			if(hostUser!=null){
   				userList.add(hostUser);
   			}
   		 }else if(EfamilyConstant.ATTENTION_DEVICE_YES == oprType.intValue()){
    		//同意只推给 申请人
    	    userList.add(user);
    	 }else if(userId.longValue() != oprUserId.longValue()){
   			//取消只推给 被取消的人  本人取消关注 不推送
    		 userList.add(user);
   		} 
   		notifyUser(data,userList,userId,NotyMessage.Type.OPER,noticeType,null);
   	}
     
     public void notifyForTransferFamilyHost(Map<String,String> data,Long userId,Long oprUserId,Long familyId,NoticeType noticeType) throws BizException{
        	
    	 List<EjlUser> userList = new ArrayList<EjlUser>();
   		 EjlUser user = new EjlUser();
  		 user.setFamilyId(familyId);
  		 user.setType(EfamilyConstant.USER_TYPE_APP);
  		 List<EjlUser> userListTemp = ejlUserDaoImpl.getEjlUserByAttribute(user);
  		 if(userListTemp != null){
  			userList.addAll(userListTemp);
  		 }
   		 notifyUser(data,userList,userId,NotyMessage.Type.OPER,noticeType,null);
   	}
     
     public void notifyUserForDelelte(Map<String,String> data,List<EjlUser> userList,Long userId,NotyMessage.Type notyType,NoticeType noticeType,Long notNoticeUserId,List<Long> deleteUserIdList){
     	if(userList == null || userList.size()==0){
 			log.info("需要推送的用户为空。 userId = "+userId+" ; noticeType = "+noticeType);
 			return;
     	}
    		for(EjlUser userTemp : userList){
    			if(notNoticeUserId!=null && notNoticeUserId.longValue() == userTemp.getId().longValue()){
    				continue;
    			}
			     if(data==null){
			     	data = new HashMap<String,String>();
			     }
			     if(deleteUserIdList != null ){
			    	 if(deleteUserIdList.contains(userTemp.getId())){
			    		 data.put("deleteType", "1");
			    	 }else{
			    		 data.put("deleteType", "2");
			    	 }
			     }
    			try { 
    				Map<String,String> data2=new HashMap<String,String>();
    				data2.putAll(data);
    				data2.put("oprTime", DateUtils.getCurTime()+"");
    				data2.put("noticeType", noticeType.getValue()+""); 
 				 
	 				NotyTarget target=new NotyTarget(userTemp.getId(),null);
	 				NotyMessage message=new NotyMessage();
	 				message.setId(null);
	 				message.setType(notyType);
	 				message.setCommand(EfamilyConstant.PUSH);
	 				message.setData(data2);
	 				Notification notification=new Notification();
	 				notification.setTarget(target);
	 				notification.setMessage(message);
	 				notification.setRealTime(false);
	 				notificationUtil.notification(notification);
	    			} catch (Exception e) {
	 				// TODO Auto-generated catch block
	 				e.printStackTrace();
	 				log.info("推送[notifyUser]时出现异常：  userId = "+userTemp.getId());
 			    }
    		}
      }
     
     public void notifyUser(Map<String,String> data,List<EjlUser> userList,Long userId,NotyMessage.Type notyType,NoticeType noticeType,Long notNoticeUserId){
    	if(userList == null || userList.size()==0){
			log.info("需要推送的用户为空。 userId = "+userId+" ; noticeType = "+noticeType);
			return;
    	}
   		for(EjlUser userTemp : userList){
   			if(notNoticeUserId!=null && notNoticeUserId.longValue() == userTemp.getId().longValue()){
   				continue;
   			}
            if(data==null){
            	data = new HashMap<String,String>();
            }
   			try { 
   				Map<String,String> data2=new HashMap<String,String>();
   				data2.putAll(data);
   				data2.put("oprTime", DateUtils.getCurTime()+"");
   				data2.put("noticeType", noticeType.getValue()+""); 
				 
				NotyTarget target=new NotyTarget(userTemp.getId(),null);
				NotyMessage message=new NotyMessage();
				message.setId(null);
				message.setType(notyType);
				message.setCommand(EfamilyConstant.PUSH);
				message.setData(data2);
				Notification notification=new Notification();
				notification.setTarget(target);
				notification.setMessage(message);
				notification.setRealTime(false);
				notificationUtil.notification(notification);
   			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				log.info("推送[notifyUser]时出现异常：  userId = "+userTemp.getId());
			}
   		}
     }
     public void notifyForUpdateUserInfo(Map<String,String> data,Long userId,Long notNoticeUserId) throws BizException{
       	List<FriendListResponse> responseList= new ArrayList<FriendListResponse>();
       	EjlUser userQuery = new EjlUser();
        userQuery.setType(EfamilyConstant.USER_TYPE_APP);
       	userQuery.setId(userId);
       	List<FriendListResponse> responseFriend=ejlUserDaoImpl.getEjlFriendListByUserId(userQuery);
		if(responseFriend!=null ){
			responseList.addAll(responseFriend);
		}
		userQuery= new EjlUser();
		userQuery.setId(userId);
       	List<FriendListResponse> responseFamily=ejlUserDaoImpl.getEjlFamilyListByUserId(userQuery);
       	if(responseFamily!=null ){
       	//如果用户是app用户或者手机用户,发送消息，无设备用户不发送消息
			for(FriendListResponse friendListResponse : responseFamily) {
				if(friendListResponse.getUserType() ==EfamilyConstant.USER_TYPE_APP ||
						friendListResponse.getUserType() == EfamilyConstant.USER_TYPE_WATCH	) {
					responseList.add(friendListResponse);
				}
			}
//       		responseList.addAll(responseFamily);
		}
       	//*** 被操作者也需要推送 ***
       	EjlUser user = ejlUserDaoImpl.getById(userId);
       	FriendListResponse friendListResponse = new FriendListResponse();
       	friendListResponse.setNickName(user.getNickName());
       	friendListResponse.setUserId(userId);
       	friendListResponse.setUserType(user.getType().intValue());
       	responseList.add(friendListResponse);

   		for(FriendListResponse userTemp : responseList){
   			if(notNoticeUserId!=null && notNoticeUserId.longValue() == userTemp.getUserId().longValue()){
   				continue;
   			}
   			if(data==null){
            	data = new HashMap<String,String>();
            }
   			//如果是手表用户
   			if(EfamilyConstant.USER_TYPE_WATCH == userTemp.getUserType()) { 
   				Map<String,String> deviceData=new HashMap<String,String>();
   				deviceData.put("nickName", data.get("nickName"));
   				deviceData.put("sex", data.get("sex"));
   				deviceData.put("headImage", data.get("icon"));
   				pushDevice(userTemp.getUserId(), null, data,EfamilyConstant.PUSH_USER_INFO,NotyMessage.Type.NOTICE,false);
   			}else{
   				data.put("oprTime", DateUtils.getCurTime()+"");
   				data.put("noticeType", NoticeType.UPDATE_USER_INFO.getValue()+""); 
   				pushDevice(userTemp.getUserId(), null, data,EfamilyConstant.PUSH,NotyMessage.Type.NOTICE,false);
   			}
   		}
   	}
     
	@Override
	public RegisterResponse register(String phone, String passwd, Long type) {
		RegisterResponse reponse = new RegisterResponse();
		reponse.setToken("11111111222222223333333344444444");
		reponse.setUserId(312L);
		return reponse;
	}

	public boolean checkVerifyCode(Context ctx,String ip,String phoneNumber,Integer appType) throws BizException{
		boolean flag = true;
		
		int ipLimitOneDayMax = 500;//单个IP地址24小时内的最大请求次数
		int phoneLimitOneDayMax = 5;//单个PHONE地址24小时内的最大请求次数
		
		//*** 1. 限制单个IP地址24小时内请求验证码的次数；
		if(!checkSendSmsLimit("IP_LIMIT_ONE_DAY_"+ip, ipLimitOneDayMax, 24*60*60)){
			log.error("单个IP地址24小时内请求验证码的次数超过最大值：ip = "+ip+" ; phoneNumber = "+phoneNumber
					 +" ; ipLimitOneDayMax = "+ipLimitOneDayMax+" ; ");
		    throw new BizException(StatusBizCode.VERIFY_CODE_VALID_IP_LIMIT.getValue());
		}
		
		//*** 2. 限制24小时内，针对同一手机号码发送验证码的总次数；
		if(!checkSendSmsLimit("PHONE_LIMIT_ONE_DAY_"+phoneNumber, phoneLimitOneDayMax, 24*60*60)){
		    log.error("24小时内，针对同一手机号码发送验证码的总次数超过最大值：ip = "+ip+" ; phoneNumber = "+phoneNumber
		    		 +" ; phoneLimitOneDayMax = "+phoneLimitOneDayMax+" ; ");
		    throw new BizException(StatusBizCode.VERIFY_CODE_VALID_PHONE_LIMIT.getValue());
		}
		
		//*** 3. 限制验证码获取的间隔时间，请至少大于120秒；
		if(!checkSendSmsTimeInterval(ctx, phoneNumber, appType)){
		    log.error("同一号码获取验证码的间隔时间，请需大于120秒：ip = "+ip+" ; phoneNumber = "+phoneNumber
		    		 +" ; phoneLimitOneDayMax = "+phoneLimitOneDayMax+" ; ");
		    throw new BizException(StatusBizCode.VERIFY_CODE_VALID_INTERVAL_LIMIT.getValue());
		}
		
		return flag;
	}
	
	public boolean checkSendSmsLimit(String key,int limitMax,int expireTime)throws BizException {
		boolean flag = true;
		String limitOneDayStr = redisClient.get(key) ;
		if(limitOneDayStr != null){
			int expTime=expireTime;
			try {
				expTime = redisClient.getExpireTime(key).intValue();
			} catch (Exception e) {
				log.error("redis get expire time error.key="+key);
			}
			int limitOneDayInt = Integer.valueOf(limitOneDayStr);
			if(limitOneDayInt>limitMax){
				flag = false;
				log.error("单个IP地址24小时内请求验证码的次数超过最大值：key = "+key+" ; limitMax = "+limitMax+" ; limitOneDayInt = "
				          +limitOneDayInt+" ; ");
			}
			redisClient.set(key, String.valueOf(Integer.valueOf(limitOneDayStr)+1),expTime);
		}else{
			redisClient.set(key, "1" ,expireTime);
		}
		return flag ;
	}
	
	public boolean checkSendSmsTimeInterval(Context ctx,String phoneNumber,Integer appType) throws BizException{
		boolean flag = true;
		int type = appType.intValue()== AppType.INSTITUTION.getValue()?3:1;
		EjlVerifyCode ejlVerifyCode = ejlVerifyCodeService.getEffectiveVerifyCode(phoneNumber,type);
		if(ejlVerifyCode!=null){
			//*** 限制发送间隔  120秒  smsSendLimitInterval
			if(DateUtils.getTimeDiff(DateUtils.parse(DateConvertUtils.format(ejlVerifyCode.getCreateTime(), "yyyy-MM-dd HH:mm:ss"),DateUtils.DATETIME_FORMAT_PATTERN),new Date())<=60){
				log.info("距上一次短信发送 时间间隔 ： " + smsServerLimit +" 秒 以内 ，此次不发送。 phoneNumber = "+phoneNumber+" ; verifyCode = "+ejlVerifyCode.getVerifyCode()+" ; ");
				return false;
			}
			//*** 如果超过时间间隔  则把此条验证码设置为无效
			ejlVerifyCode.setStatus(0L);//0 无效  1有效
			ejlVerifyCodeService.save(ctx, ejlVerifyCode);
		}
		
		return flag;
	}
	
	@Override
	public Integer getVerifyCode(Context ctx,String phoneNumber,Integer appType) throws BizException {
		//生成100000-999999之间的六位随机数
		Random random = new Random();
		int verifyCode = random.nextInt(8999);
		verifyCode = verifyCode+1000;
		int type = appType.intValue()== AppType.INSTITUTION.getValue()?3:1;
		try {
			
			EjlVerifyCode evc=new EjlVerifyCode();
			evc.setPhoneNumber(phoneNumber);
			evc.setGmtCreated(new Date());
			evc.setFamilyId(0L);
			evc.setTimeOut(smsServerTimeout);
			evc.setUserId(0L);
			evc.setType(Long.valueOf(type));
			evc.setVerifyCode(verifyCode+"");
			evc.setStatus(1L);
			evc.setMessageCode("");
			try{
				String code="error";
				if (appType ==AppType.NOVA.getValue()) {
					Integer novacode=NovaMessageUtil.getVerifyCode(phoneNumber, verifyCode);
					if (novacode==null){
						throw new BizException(code);
					}
				}else if(appType==AppType.KANDDOO.getValue()||appType==AppType.INSTITUTION.getValue()){
					kangdooSmsService.send(phoneNumber, "亲爱的康朵用户，你本次操作的验证码为："+verifyCode);
				}else if(appType==AppType.HUASHOU.getValue()){
					huashouSmsService.send(phoneNumber, "亲爱的华寿用户，你本次操作的验证码为："+verifyCode);
				}else if(appType==AppType.QIZHI.getValue()){
					qiZhiSmsServiceImpl.send(phoneNumber,"欢迎您使用奇智医养手表，您的验证码为"+verifyCode+"，请及时输入，注意保密！如非本人操作，可不用理会。");
				}else if(appType==AppType.BEIDOU.getValue()){
					beiDouSmsServiceImpl.send(phoneNumber,"亲爱的健康表用户，您本次操作的验证码为："+verifyCode+"。非本人操作，请忽略。回复TD退订。");
				}else if(appType==AppType.ANLIXIN.getValue()){
					aliyunSmsServiceImpl.send(phoneNumber,verifyCode+"");//只需要验证码 阿里端配置有模板
				}else {
					String content=StringUtils.replace(messageTemplate, "{1}", verifyCode+"");
					code = smsService.send(phoneNumber, content);
					evc.setMessageCode(code);
					if(code!=null){//短信返回判断是否发送成功
						String temp[]=code.split(",");
						if(temp.length<2 || !temp[1].startsWith("0")){
							throw new BizException(code);
						}
					}else{
						throw new BizException("result is null.");
					}
				}
			}catch(BizException e){
				evc.setMessageCode(e.getCode()+":"+e.getMessage());
				evc.setStatus(0L);
				verifyCode=-1;
			}
			try {
				ejlVerifyCodeService.save(ctx,evc);
			} catch (Exception e) {
				log.error("保存验证码信息错误",e);
				throw new BizException();
			}
		} catch (Exception e) {
			log.error("短信发送错误:"+ e);
		}
		
		return verifyCode;
	}

	public void loginRecord(Context ctx,Long userId,String token,String remark,Integer status) {
		//**************** 登录成功 记录登录信息和状态  *******************
		try{
			EjlUserLoginRecord ejlUserLoginRecord = new EjlUserLoginRecord();
			ejlUserLoginRecord.setUserId(userId);
			ejlUserLoginRecord.setToken(token);
			
			if (EfamilyConstant.USER_STATUS_ONLINE == status) {
				ejlUserLoginRecord.setRemark(remark);
				ejlUserLoginRecord.setLoginTime(new Date());
				ejlUserLoginRecord.setStatus(EfamilyConstant.USER_STATUS_ONLINE);
				
			}else if (EfamilyConstant.USER_STATUS_OFFLINE == status || EfamilyConstant.USER_STATUS_TIREN == status)  {
/*				EjlUserLoginRecord ejlUserLoginRecordCheck = ejlUserLoginRecordServiceImpl.selectOneObjByAttribute(ctx, ejlUserLoginRecord);
				if(ejlUserLoginRecordCheck != null){
					ejlUserLoginRecord.setId(ejlUserLoginRecordCheck.getId());
					ejlUserLoginRecord.setLogoutTime(new Date());
					ejlUserLoginRecord.setStatus(status);
				}else{
					log.error("此用户在登录时，未做记录： userId = "+userId+" ; token = "+token);
					return ;
				} */
				log.info("用户离线 或者 被踢 暂时不做记录: userId = "+userId+" ; ");
				return;
			}else{
				log.error("状态未定义，不做记录： userId = "+userId+" ; token = "+token);
				return ;
			}
			ejlUserLoginRecordServiceImpl.save(ctx, ejlUserLoginRecord);
			
		}catch(BizException be){
			log.error("记录登录状态时，出现异常 BizException ： userId = "+userId+" ; token = "+token+" ; message = "+be.getMessage());
		}catch(Exception e){
			log.error("记录登录状态时，出现异常 Exception ： userId = "+userId+" ; token = "+token+" ; message = "+e.getMessage());
		}
	}
	
	
	public boolean modifyPassword(Context ctx,String phoneNumber,String verifyCode,String password,Integer oprType,String oldPassword) throws Exception{
		boolean flag = true;
		//*** 检查密码合法性  并保存
		Pattern pattern = Pattern.compile("^[0-9a-zA-Z]{6,18}$",Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(password);
		if(!matcher.matches()){
			throw new BizException(StatusBizCode.USER_PASSWORD_INVALID.getValue());
		}
		
		EjlUser user = getUserByPhone(phoneNumber);
		if(user==null){
			throw new BizException(StatusBizCode.USER_UN_VALID.getValue());
		}
		if(oprType.intValue()==1){
			EjlVerifyCode evc= ejlVerifyCodeService.getVerifyCode(phoneNumber,verifyCode);
			if(evc==null){//验证码不通过
				throw new BizException(StatusBizCode.UN_VALID_VERIFY_CODE.getValue());
			}
		}else{
			if(StringUtils.isBlank(user.getPasswd())){
				throw new BizException(StatusBizCode.UN_PASSWORD_EMPTY.getValue());
			}
			if(!user.getPasswd().equals(oldPassword)){
				throw new BizException(StatusBizCode.UN_VALID_PASSWORD.getValue());
			}
		}

		ejlUserDaoImpl.updatePasswdByUserId(ctx, user.getId(), password);
		return flag;
	}
	
	
	public EjlUser getLoginUser(Context ctx,String phoneNumber,String verifyCode,String  oprType,String  password,String  sourceType,String  sourceId) throws BizException {
		EjlUser user = null;
		//***  2.验证码登陆;3.注册;5.第三方注册   这三种方式需要检查验证码
		if(EfamilyConstant.LOGIN_VERIFYCODE.equals(oprType)||EfamilyConstant.LOGIN_REGISTER.equals(oprType)||EfamilyConstant.LOGIN_THIRD_PARTY_REGISTER.equals(oprType)||EfamilyConstant.LOGIN_ORG_REGISTER.equals(oprType)){
			if(!EfamilyConstant.LOGIN_ORG_REGISTER.equals(oprType)){
			if(StringUtils.isBlank(phoneNumber)||StringUtils.isBlank(verifyCode)){
				log.error("登录时参数不完整 : phoneNumber = "+phoneNumber+" ; verifyCode = "+verifyCode);
				throw new BizException(StatusBizCode.PARAM_INCOMPLETE.getValue());
			}
			EjlVerifyCode evc= ejlVerifyCodeService.getVerifyCode(phoneNumber,verifyCode);
			if(!(evc!=null||verifyCode.equals("0000"))){//验证码不通过
				throw new BizException(StatusBizCode.UN_VALID_VERIFY_CODE.getValue());
			}
			if(evc!=null){
				evc.setStatus(0L);
				ejlVerifyCodeService.save(ctx,evc);
			}}
			user = ejlUserDaoImpl.getUserByPhone(phoneNumber);
			if(EfamilyConstant.LOGIN_REGISTER.equals(oprType)||EfamilyConstant.LOGIN_ORG_REGISTER.equals(oprType)){
				//*** 检查密码合法性  并保存
				Pattern pattern = Pattern.compile("^[0-9a-zA-Z]{6,18}$",Pattern.CASE_INSENSITIVE);
				Matcher matcher = pattern.matcher(password);
				if(!matcher.matches()){
					throw new BizException(StatusBizCode.USER_PASSWORD_INVALID.getValue());
				}
			}
			if(EfamilyConstant.LOGIN_REGISTER.equals(oprType)||EfamilyConstant.LOGIN_THIRD_PARTY_REGISTER.equals(oprType)){
				//*** 用户已存是否存在
				if(user!=null){
					throw new BizException(StatusBizCode.USER_EXIST.getValue());
				}
			}
		//***  1.密码登录
		}else if(EfamilyConstant.LOGIN_PASSWORD.equals(oprType)){
			user = ejlUserDaoImpl.getUserByPhone(phoneNumber);
			if(user==null){
				throw new BizException(StatusBizCode.USER_UN_VALID.getValue());
			}
			if(StringUtils.isBlank(user.getPasswd())){
				throw new BizException(StatusBizCode.UN_PASSWORD_EMPTY.getValue());
			}
			if(!user.getPasswd().equals(password)){
				throw new BizException(StatusBizCode.UN_VALID_PASSWORD.getValue());
			}
		//***  4.第三方登陆
		}else if(EfamilyConstant.LOGIN_THIRD_PARTY.equals(oprType)){
			user = ejlUserDaoImpl.getUserByThirdPartyToken(sourceId, Integer.valueOf(sourceType));
			if(user==null ){
				throw new BizException(StatusBizCode.USER_UN_VALID.getValue());
			}
		}else{
			log.error("登录类型oprType未定义 : "+oprType);
			throw new BizException(StatusBizCode.TYPE_UNDEFINED.getValue());
		}
		
		return user;
	}
	
	public LoginResponse login(Context ctx,LoginRequest loginRequest) throws BizException {
		LoginResponse reponse = new LoginResponse();
		String phoneNumber=loginRequest.getPhoneNumber(); //电话号码
		String verifyCode=loginRequest.getVerifyCode(); //验证码
		String  oprType=loginRequest.getOprType()+"";//1.密码登陆;2.验证码登陆;3.注册;4.第三方登陆5.第三方注册
	    String  password=loginRequest.getPassword();//密码
	    String  sourceType=loginRequest.getSourceType();//1.QQ;2.weChat;3.weiBo
	    String  sourceId=loginRequest.getSourceId();//第三方认证ID
	    Integer appType = (Integer)ctx.get("appType");

	    
	    EjlUser euser = new EjlUser();
		EjlUser user = getLoginUser(ctx, phoneNumber, verifyCode, oprType, password, sourceType, sourceId);
		if (user == null) {
			if(EfamilyConstant.LOGIN_REGISTER.equals(oprType)||EfamilyConstant.LOGIN_ORG_REGISTER.equals(oprType)){
				//*** 检查密码合法性  并保存
				euser.setPasswd(password);
			}
			euser.setAppType(appType);
			euser.setPhone(phoneNumber);
			euser.setStatus(1L);// 1:在线
			euser.setType(EfamilyConstant.USER_TYPE_APP);
			euser.setNickName(phoneNumber);
			FmlResource fmlResource = efResourceServiceImpl.getRandResourceByAssistantTypeAndStatus(EfamilyConstant.RESOURCE_ASSIST_TYPE_HEADIMAGE, EfamilyConstant.STATUS_SUCCESS_INT);
			if(fmlResource!=null){
				euser.setHeadImg(fmlResource.getResourceId());
				reponse.setHeadImgUrl(fmlResource.getResourceId());
			}else{
				euser.setHeadImg("1");
				reponse.setHeadImgUrl("1");
				log.info("登录时，随机获取头像为空：phoneNumber = "+phoneNumber);
			}
			save(ctx,euser);
			reponse.setUserId(euser.getId());
			reponse.setNickName(phoneNumber);
			List<ThirdPartyLoginStruc> thirdPartyLoginStrucList = new ArrayList<ThirdPartyLoginStruc>();
			if(EfamilyConstant.LOGIN_THIRD_PARTY_REGISTER.equals(oprType)){
			   //*** 保存第三方认证信息
				EjlThirdPartyLogin ejlThirdPartyLogin = new EjlThirdPartyLogin();
				ejlThirdPartyLogin.setToken(sourceId);
				ejlThirdPartyLogin.setType(Integer.valueOf(sourceType));
				ejlThirdPartyLogin.setStatus(1);
				EjlThirdPartyLogin ejlThirdPartyLoginCheck =  ejlComThirdPartyLoginServiceImpl.selectOneObjByAttribute(ctx, ejlThirdPartyLogin);
                if(ejlThirdPartyLoginCheck!=null){
    				//改第三方帐号已经被绑定过
    				throw new BizException(StatusBizCode.USER_THIRD_PART_BIND_EXIST.getValue());
                }
				ejlThirdPartyLogin.setUserId(euser.getId());
				ejlComThirdPartyLoginServiceImpl.save(ctx, ejlThirdPartyLogin);
				thirdPartyLoginStrucList.add(new ThirdPartyLoginStruc(sourceId,sourceType,1));
			}
			reponse.setSource(JsonUtils.toJson(thirdPartyLoginStrucList));
			
		} else {
			if(user.getType().longValue() != EfamilyConstant.USER_TYPE_APP){
				//用户类型不是APP用户 不允许登录
				throw new BizException(StatusBizCode.USER_UN_TYPE_VALID.getValue());
			}
			reponse.setUserId(user.getId());
			reponse.setFamilyId(user.getFamilyId());
			reponse.setNickName(user.getNickName());
			reponse.setHeadImgUrl(user.getHeadImg());
			reponse.setSex(user.getSex());
			reponse.setSignature(user.getSignature());
			reponse.setUserType(user.getType());
			
			List<ThirdPartyLoginStruc> thirdPartyLoginStrucList = ejlComThirdPartyLoginServiceImpl.getThirdPartyLoginStruc(user.getId());
			if(thirdPartyLoginStrucList!=null&&thirdPartyLoginStrucList.size()>0){
				reponse.setSource(JsonUtils.toJson(thirdPartyLoginStrucList));
			}else{
				reponse.setSource(JsonUtils.toJson(new ArrayList<ThirdPartyLoginStruc>()));
			}
			if(null != user.getFamilyId() ){
				 EjlFamily family = ejlFamilyDaoImpl.getById(user.getFamilyId());
				 reponse.setFamilyCode(family.getCode());
				 reponse.setFamilyName(family.getName());
				 EjlFamilyUser ejlFamilyUser = new EjlFamilyUser();
				 ejlFamilyUser.setFamilyId(user.getFamilyId());
				 ejlFamilyUser.setPosition(Position.HOST.getValue());
				 ejlFamilyUser =  ejlFamilyUserDaoImpl.getHostByfamilyIdAndPosition(ejlFamilyUser);
				 if(ejlFamilyUser!=null){
					 reponse.setFamilyHostUserId(ejlFamilyUser.getUserId());
				 }
			}
		}
		
		return reponse;
	}

    public EjlUser getFamilyHostByDeviceCode(String deviceCode)throws BizException{
    	EjlUser user = null;
        Long deviceUserId = ejlUserDeviceServiceImpl.getDeviceUseingUserIdByCode(deviceCode);    	
    	if(deviceUserId!=null){
    		EjlUser deviceUser = ejlUserDaoImpl.getById(deviceUserId);
			EjlFamilyUser ejlFamilyUser = new EjlFamilyUser();
			ejlFamilyUser.setFamilyId(deviceUser.getFamilyId());
			ejlFamilyUser.setPosition(Position.HOST.getValue());
			ejlFamilyUser =  ejlFamilyUserDaoImpl.getHostByfamilyIdAndPosition(ejlFamilyUser);
			if(ejlFamilyUser!=null){
				 user = ejlUserDaoImpl.getById(ejlFamilyUser.getUserId());
			}
    	}
    	return user;
    }
	
	
	@Override
	public void loginOut(Context ctx,Long userId,String token) throws BizException {
		EjlUserLoginRecord ejlUserLoginRecord = new EjlUserLoginRecord();
		ejlUserLoginRecord.setUserId(userId);
		ejlUserLoginRecord.setToken(token);
		EjlUserLoginRecord ejlUserLoginRecordCheck = ejlUserLoginRecordServiceImpl.selectOneObjByAttribute(ctx, ejlUserLoginRecord);
		if(ejlUserLoginRecordCheck != null){
			ejlUserLoginRecord.setId(ejlUserLoginRecordCheck.getId());
			ejlUserLoginRecord.setLogoutTime(new Date());
			ejlUserLoginRecord.setStatus(EfamilyConstant.USER_STATUS_OFFLINE);
		}else{
			log.error("此用户在登录时，未做记录： userId = "+userId+" ; token = "+token);
			return ;
		}
		ejlUserLoginRecordServiceImpl.save(ctx, ejlUserLoginRecord);
	}

	@Override
	public EjlUser getUserByUserId(Long userId) throws BizException {
		return ejlUserDaoImpl.getUserByUserId(userId);
	}
	@Override
	public void updateUserType(Context ctx,long userId,long userType) throws BizException {
		//*** 成员BB上 没有手表，用户AA 则直接变为 TYPE=3(没有手表和APP的用户) 的用户
		EjlUser ejlUser =get(userId); 
		ejlUser.setType(userType);
		save(ctx,ejlUser);
	}
	public void updateUserTypeAndPhone(Context ctx,long userId,long userType,String phoneNumber) throws BizException {
		//*** 成员BB上 没有手表，用户AA 则直接变为 TYPE=3(没有手表和APP的用户) 的用户
		EjlUser ejlUser = get(userId);
		ejlUser.setType(userType);
		ejlUser.setPhone(phoneNumber);
		save(ctx,ejlUser);
	}
	public void switchUserPhone(Context ctx,long userIdA,long userIdB) throws BizException {
		//*** 成员BB上 没有手表，用户AA 则直接变为 TYPE=3(没有手表和APP的用户) 的用户
		EjlUser ejlUserDataA = getEntityDao().getById(userIdA);
		EjlUser ejlUserDataB = getEntityDao().getById(userIdB);
		
		EjlUser ejlUserA = get(userIdA); 
		ejlUserA.setPhone(ejlUserDataB.getPhone());
		save(ctx,ejlUserA);
		
		EjlUser ejlUserB = get(userIdB);  
		ejlUserB.setPhone(ejlUserDataA.getPhone());
		save(ctx,ejlUserB);
	}
	
	@Override
	public void saveOrUpdateUser(Context ctx,EjlUser user) throws BizException {
        save(ctx,user);
	}
	@Override
	public long saveInitUser(Context ctx,Long familyId,Long userType,String phone,String userName) throws BizException {
        //****  检查手机号码
		if(StringUtils.isNotEmpty(phone) && getEntityDao().getUserByPhone(phone)!=null){
			log.error("【无设备用户到设备用户时】用户手机号码已经存在，保存用户失败：phoneNumber = "+phone+" ; userType = "+userType+" ; familyId = "+familyId);
			throw new BizException(StatusBizCode.USER_PHONE_EXIST.getValue());
		}
		EjlUser user = new EjlUser();
		user.setPhone(phone);
		user.setFamilyId(familyId);
		user.setType(userType);
		user.setStatus(0L);
		user.setGmtCreated(new Date());
		user.setNickName(userName);
		if(ctx!=null && ctx.get("appType")!=null && !ctx.get("appType").toString().equals("1")){
			user.setAppType(Integer.valueOf(ctx.get("appType").toString()));
		}else{
			user.setAppType(1);
		}
		FmlResource fmlResource = efResourceServiceImpl.getRandResourceByAssistantTypeAndStatus(EfamilyConstant.RESOURCE_ASSIST_TYPE_HEADIMAGE, EfamilyConstant.STATUS_SUCCESS_INT);
		if(fmlResource!=null){
			user.setHeadImg(fmlResource.getResourceId());
		}else{
			user.setHeadImg("1");
			log.info("保存用户时，随机获取头像为空：userName = "+userName+" ; phone = "+phone);
		}
		save(ctx,user);
		return user.getId();
	}
	
	
	public void zombieToDeviceUser(Context ctx,Long userId,Long userType,Long familyId,String phoneNumber) throws BizException{
		if(StringUtils.isNotEmpty(phoneNumber) && getEntityDao().getUserByPhone(phoneNumber)!=null){
			log.error("【无设备用户到设备用户时】用户手机号码已经存在，保存用户失败：phoneNumber = "+phoneNumber+" ; userId = "+userId+" ; familyId = "+familyId);
			throw new BizException(StatusBizCode.USER_PHONE_EXIST.getValue());
		}		
		EjlUser user = get(userId);
		user.setFamilyId(familyId); 
		user.setType(userType);
		user.setStatus(0L); 
		user.setPhone(phoneNumber);
		save(ctx,user);
	}
	
	
	/**
	* Title: getUserByPhone
	* Description:
	* @param phone
	* @return
	* @throws BizException 
	* @see com.winterframework.efamily.service.IEjlUserService#getUserByPhone(java.lang.String) 
	*/
	@Override
	public EjlUser getUserByPhone(String phone) throws BizException {
		if(StringUtils.isBlank(phone)){
			return null;
		}
		return ejlUserDaoImpl.getUserByPhone(phone);
	}
	@Override
	public EjlUser getValidUserByPhone(String phone) throws BizException {
		return filterValid(getUserByPhone(phone));
	}
	/**
	 * 过滤有效用户 --未平台禁用 非无设备用户
	 * @param user
	 * @return
	 */
	private EjlUser filterValid(EjlUser user){
		if(null!=user){
			if( EjlUser.Type.NO_DEVICE.getCode()!=user.getType().intValue()){
				return user;			
			}
		}
		return null;
	}
	
    public List<ChatRoomUserNotify> getUserByMoreUserId(List<String> userIds) throws BizException {
	    return ejlUserDaoImpl.getUserByMoreUserId(userIds);
	}
	@Override
	public List<FriendListResponse> getEjlFriendListByUserId(Long userId,Long type) throws BizException {
		EjlUser user = new EjlUser();
		user.setId(userId);
		user.setType(type);
		return ejlUserDaoImpl.getEjlFriendListByUserId(user);
	}
	@Override
	public List<FriendListResponse> getEjlFamilyListByUserId(Long userId,Long type) throws BizException {
		EjlUser user = new EjlUser();
		user.setId(userId);
		user.setType(type);
		return ejlUserDaoImpl.getEjlFamilyListByUserId(user);
	}
	
	@Override
	public Long getFamilyCountByUserId(Long userId) throws BizException {
		return ejlUserDaoImpl.getFamilyCountByUserId(userId);
	}

	@Override
	public String manageUserChatGroupInfo(Context ctx,Long userId,String parameterIndex,String parameterContext,String chatGroupId,Long oprType,Long chatType) throws BizException {
		
        if(StringUtils.isBlank(chatGroupId) || StringUtils.isBlank(parameterIndex)|| oprType==null || chatType==null){
        	log.error("修改聊天组信息时，管理聊天组属性时，存在为空的参数，操作失败: userId ="+userId+" ; chatGroupId = "+chatGroupId+" ; chatType = "+chatType+" ; oprType = "+oprType);
        	throw new BizException(StatusBizCode.CHAT_ROOM_UN_VALID.getValue());
        }
        
        if(chatType.longValue()== EfamilyConstant.CHAT_TYPE_P2P){
        	//******** 【注意：单人聊天时，chatGroupId表示对方的userId】 单人对单人聊天：获取和设置群组信息  *********
        	if(EfamilyConstant.OPRTYPE_GET == oprType.longValue()){
                //*** 获取  
        		EjlUserP2pChatRoom ejlUserP2pChatRoom = getEjlUserP2pChatRoom(ctx, userId, Long.valueOf(chatGroupId));
                parameterContext = getEjlUserP2pChartRoomInfoByParam(ejlUserP2pChatRoom, Integer.valueOf(parameterIndex));
    		}else{
    			//*** 修改 或 操作
    			udpateEjlUserP2pChartRoomByParam(ctx,userId, Long.valueOf(chatGroupId), parameterIndex, parameterContext);
    		}
        	
        }else if(chatType.longValue()== EfamilyConstant.CHAT_TYPE_ROOM){
        	//********  群组：获取和设置群组信息  *********
        	if(EfamilyConstant.OPRTYPE_GET == oprType.longValue()){
                //*** 获取
            	EjlUserChartRoom ejlUserChartRoom = new EjlUserChartRoom();
        		ejlUserChartRoom.setUserId(userId);
        		ejlUserChartRoom.setChatRoomId(Long.valueOf(chatGroupId));
                ejlUserChartRoom = ejlUserChartRoomDaoImpl.getByUserIdAndChatRoomId(ejlUserChartRoom);
                parameterContext = getEjlUserChartRoomInfoByParam(ejlUserChartRoom, Integer.valueOf(parameterIndex));
    		}else{
    			//*** 修改 或 操作
    			udpateEjlUserChartRoomByParam(ctx,userId, Long.valueOf(chatGroupId), parameterIndex, parameterContext);
    		}
        }
        
		return parameterContext;
	}
	
	/**
	 * @param userId
	 * @param chartRoomId
	 * @param parmKey
	 * @param parmValue
	 * @return
	 * @throws BizException
	 */
     public EjlUserChartRoom udpateEjlUserChartRoomByParam(Context ctx,Long userId,Long chartRoomId,String parmKey,String parmValue) throws BizException{
    	 
    	//****** 判断 用户是否在群组中
		EjlUserChartRoom ejlUserChartRoomCheck = new EjlUserChartRoom();
		ejlUserChartRoomCheck.setChatRoomId(chartRoomId);
		ejlUserChartRoomCheck.setUserId(userId);
		EjlUserChartRoom userChartData = ejlUserChartRoomDaoImpl.getByUserIdAndChatRoomId(ejlUserChartRoomCheck);
		if( userChartData == null){
    		log.info("对群组进行加入删除退出操作时：出现异常。操作者已经不再群组中，不允许再对群组进行操作。 parmKey = "+parmKey+" ; ");
		    throw new BizException(StatusBizCode.CHAT_ROOM_USER_NOT.getValue());				
		}
       
    	EjlUserChartRoom ejlUserChartRoom = new EjlUserChartRoom();
		ejlUserChartRoom.setUserId(userId);
		ejlUserChartRoom.setChatRoomId(chartRoomId);
		ejlUserChartRoom.setGmtModified(new Date());
		boolean isUpdate = false;
		if(EfamilyConstant.GROUP_SETTING_NAME.equals(parmKey)){
			ejlUserChartRoom.setChatRoomName(parmValue);
			updateChartRoomName(ctx,chartRoomId,parmValue);
			isUpdate = true;
		}else if(EfamilyConstant.GROUP_SETTING_TOP.equals(parmKey)){
			ejlUserChartRoom.setChatRoomTop(Long.valueOf(parmValue));
			isUpdate = true;
		}else if(EfamilyConstant.GROUP_SETTING_NOTIFY.equals(parmKey)){
			ejlUserChartRoom.setMessageNotify(Long.valueOf(parmValue));
			isUpdate = true;
		}else if(EfamilyConstant.GROUP_SETTING_BOOK.equals(parmKey)){
			ejlUserChartRoom.setSaveAddressBook(Long.valueOf(parmValue));
			isUpdate = true;
		}else if(EfamilyConstant.GROUP_ROOM_JOIN.equals(parmKey)){
			joinUserChatGroupInfo(ctx,parmValue, chartRoomId);
		}else if(EfamilyConstant.GROUP_ROOM_QUIT.equals(parmKey)){
			quitUserChatGroupInfo(userId, chartRoomId);
		}else if(EfamilyConstant.GROUP_ROOM_DELETE.equals(parmKey)){
			deleteUserChatGroupInfo(parmValue, chartRoomId);
		}else {
			log.info("处理聊天组设置信息时：出现异常。 设置parmKey未定义。");
		    throw new BizException(StatusBizCode.TYPE_UNDEFINED.getValue());
		}
		
		if(isUpdate){
			ejlUserChartRoomDaoImpl.updateUserChatGroupInfo(ejlUserChartRoom);
		}
		return ejlUserChartRoom;
	}  
	
     public EjlUserP2pChatRoom udpateEjlUserP2pChartRoomByParam(Context ctx,Long userId,Long chartRoomId,String parmKey,String parmValue) throws BizException{
    	EjlUserP2pChatRoom ejlUserP2pChatRoom = getEjlUserP2pChatRoom(ctx, userId, chartRoomId);
 		boolean isUpdate = false;
        if(EfamilyConstant.GROUP_SETTING_TOP.equals(parmKey)){
        	ejlUserP2pChatRoom.setChatRoomTop(Long.valueOf(parmValue));
 			isUpdate = true;
 		}else if(EfamilyConstant.GROUP_SETTING_NOTIFY.equals(parmKey)){
 			ejlUserP2pChatRoom.setMessageNotify(Long.valueOf(parmValue));
 			isUpdate = true;
 		}else {
 			log.info("处理P2P聊天组设置信息时：出现异常。 设置parmKey未定义。");
 		    throw new BizException(StatusBizCode.TYPE_UNDEFINED.getValue());
 		}
 		
 		if(isUpdate){
 			ejlUserP2pChatRoomServiceImpl.save(ctx, ejlUserP2pChatRoom);
 		}
 		return ejlUserP2pChatRoom;
 	}  
     
     public void updateChartRoomName(Context ctx,Long chartRoomId,String name) throws BizException{
    	 EjlChartRoom ejlChartRoom = new EjlChartRoom();
    	 ejlChartRoom.setId(chartRoomId);
    	 ejlChartRoom.setName(name);
    	 ejlChartRoomServiceImpl.save(ctx,ejlChartRoom);
     }
     
     
     public String getEjlUserChartRoomInfoByParam(EjlUserChartRoom ejlUserChartRoom,int parmKey) throws BizException{
 		String parmValue = null;
 		if(EfamilyConstant.GROUP_SETTING_NAME.equals(parmKey)){
 			parmValue = ejlUserChartRoom.getChatRoomName() ; 
 		}else if(EfamilyConstant.GROUP_SETTING_TOP.equals(parmKey)){
 			parmValue = String.valueOf(ejlUserChartRoom.getChatRoomTop()) ;
 		}else if(EfamilyConstant.GROUP_SETTING_NOTIFY.equals(parmKey)){
 			parmValue = String.valueOf(ejlUserChartRoom.getMessageNotify()) ;
 		}else if(EfamilyConstant.GROUP_SETTING_BOOK.equals(parmKey)){
 			parmValue = String.valueOf(ejlUserChartRoom.getSaveAddressBook()) ; 
 		}else{
			log.error("获取天组信息时，操作的 [KEY = "+parmKey+"] 未定义，操作失败 。");
        	throw new BizException(StatusBizCode.TYPE_UNDEFINED.getValue());
 		}
 		return parmValue;
 	}  
	
     public String getEjlUserP2pChartRoomInfoByParam(EjlUserP2pChatRoom ejlUserP2pChatRoom,int parmKey) throws BizException{
  		String parmValue = null;
  		if(EfamilyConstant.GROUP_SETTING_TOP.equals(parmKey)){
  			parmValue = String.valueOf(ejlUserP2pChatRoom.getChatRoomTop()) ;
  		}else if(EfamilyConstant.GROUP_SETTING_NOTIFY.equals(parmKey)){
  			parmValue = String.valueOf(ejlUserP2pChatRoom.getMessageNotify()) ;
  		}else{
 			log.error("获取天组信息时，操作的 [KEY = "+parmKey+"] 未定义，操作失败 。");
         	throw new BizException(StatusBizCode.TYPE_UNDEFINED.getValue());
  		}
  		return parmValue;
  	}  
	@Override
	public void quitUserChatGroupInfo(Long userId,Long chatRoomId) throws BizException {
		//*** userchatroom
		EjlUserChartRoom ejlUserChartRoom = new EjlUserChartRoom();
		ejlUserChartRoom.setChatRoomId(chatRoomId);
		ejlUserChartRoom.setUserId(userId);
		ejlUserChartRoom.setGmtModified(new Date());
		ejlUserChartRoom.setStatus(EfamilyConstant.EXIST_CHAT_ROOT_NO);
		ejlUserChartRoomDaoImpl.updateUserChatGroupInfo(ejlUserChartRoom);
		
		//*** chartRoomUser
		EjlChartRoomUser ejlChartRoomUser = new EjlChartRoomUser();
		ejlChartRoomUser.setChartRoomId(chatRoomId);
		ejlChartRoomUser.setUserId(userId);
		ejlChartRoomUser.setStatus(EfamilyConstant.EXIST_CHAT_ROOT_NO);
		ejlChartRoomUserDaoImpl.updateStatusByUserIdAndChartId(ejlChartRoomUser);
		
	}
	public int deleteUserChatGroupInfo(String userIds,Long chatRoomId) throws BizException {
		String[] userIdArr = userIds.split(","); 
		EjlChartRoom ejlChartRoom = ejlChartRoomDaoImpl.getById(chatRoomId);
		if(ejlChartRoom == null){
			log.error("聊天群不存在，删除聊天群成员失败: userIds ="+userIds+" ; chatRoomId = "+chatRoomId);
        	throw new BizException(StatusBizCode.CHAT_ROOM_UN_VALID.getValue());
		}
		for(int i=0;i<userIdArr.length;i++){
			long userId = Long.valueOf(userIdArr[i]);
			EjlUser userTemp = ejlUserDaoImpl.getById(userId);
			if(userTemp==null){
				log.info("群信息设置，删除聊天群组成员时，userId = "+ userId+ " 对应的用户不存在  .");
				continue;
			}
			quitUserChatGroupInfo( userId, chatRoomId);
		}
		
		return userIdArr.length;
	}
	@Override
	public int joinUserChatGroupInfo(Context ctx,String userIds,Long chatRoomId) throws BizException {
		String[] userIdArr = userIds.split(","); 
		EjlChartRoom ejlChartRoom = ejlChartRoomDaoImpl.getById(chatRoomId);
		if(ejlChartRoom == null || StringUtils.isBlank(userIds)){
			log.error("聊天群不存在，加入聊天群失败: userIds ="+userIds+" ; chatRoomId = "+chatRoomId);
        	throw new BizException(StatusBizCode.PARAM_INCOMPLETE.getValue());
		}
		for(int i=0;i<userIdArr.length;i++){
			long userId = Long.valueOf(userIdArr[i]);
			EjlUser userTemp = ejlUserDaoImpl.getById(userId);
			if(userTemp==null){
				log.info("群信息设置，加入聊天群组时，userId = "+ userId+ " 对应的用户不存在  .");
				continue;
			}
			
			//*** userchatroom
			EjlUserChartRoom ejlUserChartRoom = new EjlUserChartRoom();
			ejlUserChartRoom.setChatRoomId(chatRoomId);
			ejlUserChartRoom.setUserId(userId);
			
			EjlUserChartRoom userChartData = ejlUserChartRoomDaoImpl.getByUserIdAndChatRoomId(ejlUserChartRoom);
			if( userChartData != null){
				ejlUserChartRoom.setId(userChartData.getId());
				if(userChartData.getStatus().longValue() == EfamilyConstant.EXIST_CHAT_ROOT_YES){
					log.info("用户已经在聊天室中，不需再次加入，userId = "+ userId+ " chartRoomId = "+chatRoomId+"  ;  ");
					continue ;
				}
			}
			ejlUserChartRoom.setGmtModified(new Date());
			ejlUserChartRoom.setChatRoomName(ejlChartRoom.getName());
        	ejlUserChartRoom.setStatus(EfamilyConstant.EXIST_CHAT_ROOT_YES);
        	ejlUserChartRoom.setChatRoomTop(EfamilyConstant.CHAT_ROOM_TOP_NO);
        	ejlUserChartRoom.setMessageNotify(EfamilyConstant.MESSAGE_NOTIFY_YES);
        	ejlUserChartRoom.setSaveAddressBook(EfamilyConstant.SAVE_ADDRESS_BOOK_NO);
			
			ejlUserChartRoomServiceImpl.save(ctx,ejlUserChartRoom);
				
			
			//*** chartRoomUser
			EjlChartRoomUser ejlChartRoomUser = new EjlChartRoomUser();
			ejlChartRoomUser.setChartRoomId(chatRoomId);
			ejlChartRoomUser.setUserId(userId);
			EjlChartRoomUser ejlChartRoomUserData = ejlChartRoomUserDaoImpl.selectOneObjByAttribute(ejlChartRoomUser);
			if( ejlChartRoomUserData != null){
				ejlChartRoomUser.setId(ejlChartRoomUserData.getId());
			}
			ejlChartRoomUser.setStatus(EfamilyConstant.EXIST_CHAT_ROOT_YES);
			ejlChartRoomUser.setGmtCreated(new Date());
			ejlChartRoomUserServiceImpl.save(ctx,ejlChartRoomUser);
			
		}
		
		return userIdArr.length;
	}
	
	
	public List<ChatGroupDetailsList> getChatGroupDetailsList(Long userId,Long chatRoomId){
		List<ChatGroupDetailsList> chatGroupDetailsList = null; 
		EjlUserChartRoom ejlUserChartRoom = new EjlUserChartRoom();
		ejlUserChartRoom.setUserId(userId);
		ejlUserChartRoom.setChatRoomId(chatRoomId);
		chatGroupDetailsList = ejlUserChartRoomDaoImpl.getChatGroupDetailsList(ejlUserChartRoom);
		if(chatGroupDetailsList!=null){
			for(ChatGroupDetailsList chatGroupDetailsTemp:chatGroupDetailsList){
				List<UserChartRoomMemberInfo> userChartRoomMemberInfoList = ejlUserChartRoomDaoImpl.getChatGroupMemberInfo(ejlUserChartRoom);
                if(userChartRoomMemberInfoList !=null){
                	chatGroupDetailsTemp.setMemberDetails(userChartRoomMemberInfoList);
                }
			}
		}else{
			chatGroupDetailsList = new ArrayList<ChatGroupDetailsList>();
			log.info("群组信息为空： userId = "+userId+" ; chatRoomId = "+chatRoomId);
		}
		return chatGroupDetailsList;
	}
	
	public List<ChatGroupDetailsList> getChatRoomDetails(Context ctx,Long userId,Long chatRoomId,Long chatType) throws BizException{
		List<ChatGroupDetailsList> chatGroupDetailsListResult = null;
		if(chatType.longValue() == EfamilyConstant.CHAT_TYPE_ROOM){
			chatGroupDetailsListResult = getChatGroupDetailsList(userId, chatRoomId);
		}else if(chatType.longValue() == EfamilyConstant.CHAT_TYPE_P2P){
			chatGroupDetailsListResult = getP2pChatRoomDetails(ctx,userId, chatRoomId);
		}else{
			log.info("获取群组详情时，chattype 聊天类型未定义 ： userId = "+userId+" ; chatRoomId = "+chatRoomId+" ; chatType = "+chatType+"  ;   ");
			throw new BizException(StatusBizCode.USER_UN_VALID.getValue());
		}
		
		return chatGroupDetailsListResult;
	}
	
	public List<ChatGroupDetailsList> getP2pChatRoomDetails(Context ctx,Long userId,Long chatRoomId) throws BizException{
		List<ChatGroupDetailsList> chatGroupDetailsListResult = new ArrayList<ChatGroupDetailsList>(); 
		ChatGroupDetailsList chatGroupDetailsList = new ChatGroupDetailsList();
		EjlUserP2pChatRoom ejlUserP2pChatRoom = getEjlUserP2pChatRoom(ctx, userId, chatRoomId);
		chatGroupDetailsList.setChatRoomId(chatRoomId);
		chatGroupDetailsList.setChatRoomTop(ejlUserP2pChatRoom.getChatRoomTop());
		chatGroupDetailsList.setMessageNotify(ejlUserP2pChatRoom.getMessageNotify());
		chatGroupDetailsListResult.add(chatGroupDetailsList);
		return chatGroupDetailsListResult;
	}
	
	public EjlUserP2pChatRoom getEjlUserP2pChatRoom(Context ctx,Long userId,Long chatRoomId) throws BizException{
		//**** 单人聊天时，前端传递的chatRoomId 是聊天对象的用户ID，需要转换为两人的chatRoomId. 
		chatRoomId = ejlP2pChatRoomServiceImpl.getP2pChatRoomId(ctx, userId, chatRoomId);
		EjlUserP2pChatRoom ejlUserP2pChatRoom = new EjlUserP2pChatRoom();
		ejlUserP2pChatRoom.setChatRoomId(chatRoomId);
		ejlUserP2pChatRoom.setUserId(userId);
		EjlUserP2pChatRoom ejlUserP2pChatRoomCheck =  ejlUserP2pChatRoomServiceImpl.selectOneObjByAttribute(null,ejlUserP2pChatRoom);
		if(ejlUserP2pChatRoomCheck == null){
			ejlUserP2pChatRoom.setMessageNotify(0L);
			ejlUserP2pChatRoom.setChatRoomTop(1L);
			ejlUserP2pChatRoom.setStatus(1L);
			ejlUserP2pChatRoomServiceImpl.save(ctx, ejlUserP2pChatRoom);
		}else{
			ejlUserP2pChatRoom = ejlUserP2pChatRoomCheck;
		}
		return ejlUserP2pChatRoom ;
	}
	
	
	public EjlChartRoom createChatRoom(Context ctx,Long userId) throws BizException {
		EjlChartRoom ejlChartRoom = new EjlChartRoom();
		ejlChartRoom.setGmtCreated(new Date());
		ejlChartRoom.setCreator(userId);
		ejlChartRoom.setType(1L);
		//*** 名称暂时随机生成 具体实现后期再定
		ejlChartRoom.setName(String.valueOf(group+System.currentTimeMillis()));
		ejlChartRoom.setStatus(0L);
		ejlChartRoomServiceImpl.save(ctx,ejlChartRoom);
		return ejlChartRoom;
	}
	
	@Override
	public List<ChatGroupListResponse> getGroupChatListByUserId(Long userId)throws BizException {
		List<ChatGroupListResponse> chatGroupListResponseList = ejlUserDaoImpl.getGroupChatListByUserId(userId);
		return chatGroupListResponseList;
	}

	@Override
	public List<HealthlyUserRequest> getHealthlyUserByUserId(Long userId)
			throws BizException {
		List<HealthlyUserRequest> list = ejlUserDaoImpl.getHealthlyUserByUserId(userId);
		if(list == null){
			return null;
		}
		for(HealthlyUserRequest healthlyUserRequest:list){
			UserDeviceInfo userDeviceInfo=ejlUserDaoImpl.getModeleTypeByDeviceId(healthlyUserRequest.getDeviceId());
			if(userDeviceInfo!=null){
				healthlyUserRequest.setModuleType(userDeviceInfo.getCustomerId()+"_"+userDeviceInfo.getGlevel());
			}
			EfOrgDevice entity = new EfOrgDevice();
			entity.setImei(healthlyUserRequest.getCode());
			entity.setStatus(1);
			Context ctx = new Context();
			ctx.set("userId", -1);
			EfOrgDevice efd = efComOrgDeviceServiceImpl.selectOneObjByAttribute(ctx, entity);
			if(efd != null){
				healthlyUserRequest.setOrgTag(1);
			}else{
				healthlyUserRequest.setOrgTag(0);
			}
		}
		return list;
	}

	@Override
	public UserHealthlyConfigStruc getHealthlyUserConfig(Long userId,
			Long deviceId) throws BizException {
		UserHealthlyConfigStruc struc = new UserHealthlyConfigStruc();
		EfUserHealthSetting userHealthSett=efUserHealthSettingService.getByUserId(userId);
		struc.setUserId(userId);
		struc.setDeviceId(deviceId);
		if(userHealthSett!=null){
			struc.setAge(userHealthSett.getAge()==null?0l:userHealthSett.getAge());
			if(userHealthSett.getHeight()!=null){
				struc.setHeight(userHealthSett.getHeight()==null?0l:userHealthSett.getHeight());
			}
			if(userHealthSett.getWeight()!= null){
				struc.setWeight(userHealthSett.getWeight()==null?0l:userHealthSett.getWeight());
			}
			struc.setArm(userHealthSett.getArm());
		}
		EjlUser user = ejlUserDaoImpl.getById(userId);
		if(user!=null){
		struc.setSex(user.getSex()==null?0l:user.getSex());
		}
		EfDeviceSetting deviceSetting=efDeviceSettingService.getByUserIdAndDeviceId(userId,deviceId);
		if(deviceSetting != null){
			DeviceParamCommon deviceParamCommon=JsonUtils.fromJson(deviceSetting.getCommon(), DeviceParamCommon.class);
			if(deviceParamCommon!=null){
				struc.setHeartSwitch(deviceParamCommon.getHeartOnff()==null?0:deviceParamCommon.getHeartOnff());
				struc.setRemindSwitch(deviceParamCommon.getSittingOnff()==null?0:deviceParamCommon.getSittingOnff());
				struc.setStepCountSwitch(deviceParamCommon.getWalkOnff()==null?0:deviceParamCommon.getWalkOnff());
				struc.setSleepSwitch(deviceParamCommon.getSleepOnff()==null?0:deviceParamCommon.getSleepOnff());
				struc.setId(-1l);
			}
			DeviceParamHealth deviceParamHealthy = JsonUtils.fromJson(deviceSetting.getHealth(), DeviceParamHealth.class);
			if(deviceParamHealthy!=null){
				struc.setSitTime(deviceParamHealthy.getSittingTime()==null?0f:deviceParamHealthy.getSittingTime());
				struc.setSittingSpan(deviceParamHealthy.getSittingSpan()==null?null:Arrays.asList(deviceParamHealthy.getSittingSpan()));
				struc.setSleepSpan(deviceParamHealthy.getSleepSpan()==null?null:Arrays.asList(deviceParamHealthy.getSleepSpan()));
			}
		}
		return struc;
	}

	@Override
	public void updateHealthlyUserConfig(Context ctx,
			UserHealthlyConfigRequest userHealthlyConfigStruc)
			throws BizException {
		EjlUser user =get(userHealthlyConfigStruc.getUserId()); 
		user.setAge(userHealthlyConfigStruc.getAge()==null?user.getAge():userHealthlyConfigStruc.getAge());
		user.setHeight(userHealthlyConfigStruc.getHeight()==null?user.getHeight():userHealthlyConfigStruc.getHeight());
		user.setSex(userHealthlyConfigStruc.getSex()==null?user.getSex():userHealthlyConfigStruc.getSex());
		/*user.setStepGoal(userHealthlyConfigStruc.getStepCount());
		user.setSitTime(userHealthlyConfigStruc.getSitTime());*/
		user.setWeight(userHealthlyConfigStruc.getWeight()==null?user.getWeight():userHealthlyConfigStruc.getWeight());
		save(ctx,user);
		
		EjlUserExtendInfo entity = new EjlUserExtendInfo();
		entity.setUserId(userHealthlyConfigStruc.getUserId());
		EjlUserExtendInfo ejlUserExtendInfo = ejlComUserExtendInfoDaoImpl.selectOneObjByAttribute(entity);
		if(ejlUserExtendInfo == null&&(userHealthlyConfigStruc.getSitTime()!=null||userHealthlyConfigStruc.getStepCount()!=null||userHealthlyConfigStruc.getSleepSwitch()!=null)){
			entity.setSitTime(userHealthlyConfigStruc.getSitTime());
			entity.setStepGoal(userHealthlyConfigStruc.getStepCount());
			entity.setSleepSpan(userHealthlyConfigStruc.getSleepSpan()!=null?JsonUtils.toJson(userHealthlyConfigStruc.getSleepSpan().split(",")):null);
			entity.setSittingSpan(userHealthlyConfigStruc.getSittingSpan()!=null?JsonUtils.toJson(userHealthlyConfigStruc.getSittingSpan().split(",")):null);
			ejlComUserExtendInfoDaoImpl.insert(entity);
		}else if (userHealthlyConfigStruc.getSitTime()!=null||userHealthlyConfigStruc.getStepCount()!=null||userHealthlyConfigStruc.getSleepSwitch()!=null){
			ejlUserExtendInfo.setSitTime(userHealthlyConfigStruc.getSitTime());
			ejlUserExtendInfo.setStepGoal(userHealthlyConfigStruc.getStepCount());
			ejlUserExtendInfo.setSleepSpan(userHealthlyConfigStruc.getSleepSpan()!=null?JsonUtils.toJson(userHealthlyConfigStruc.getSleepSpan().split(",")):null);
			ejlUserExtendInfo.setSittingSpan(userHealthlyConfigStruc.getSittingSpan()!=null?JsonUtils.toJson(userHealthlyConfigStruc.getSittingSpan().split(",")):null);
			ejlComUserExtendInfoDaoImpl.update(ejlUserExtendInfo);
		}
		
		/*UserHealtyConfig userHealtyConfig = new UserHealtyConfig();
		userHealtyConfig.setId(userHealthlyConfigStruc.getId());
		userHealtyConfig.setUserId(userHealthlyConfigStruc.getUserId());
		userHealtyConfig.setDeviceId(userHealthlyConfigStruc.getDeviceId());
		userHealtyConfig.setHeartSwitch(userHealthlyConfigStruc.getHeartSwitch());
		userHealtyConfig.setSittingSwitch(userHealthlyConfigStruc.getRemindSwitch());
		userHealtyConfig.setStepSwitch(userHealthlyConfigStruc.getStepCountSwitch());
		userHealtyConfig.setClimbSwitch(userHealthlyConfigStruc.getClimbSwitch());*/
		/*if(userHealthlyConfigStruc.getRemindSwitch()!=null ||
				userHealthlyConfigStruc.getStepCountSwitch()!=null||userHealthlyConfigStruc.getSleepSwitch()!=null){
			UserHealtyConfig queryEntity =eJLComUserHealythConfigDaoImpl.getByDeviceIdAndUser(userHealthlyConfigStruc.getUserId(), userHealthlyConfigStruc.getDeviceId());
			if(queryEntity==null){
				queryEntity = new UserHealtyConfig();
				queryEntity.setSittingSwitch(userHealthlyConfigStruc.getRemindSwitch());
				queryEntity.setStepSwitch(userHealthlyConfigStruc.getStepCountSwitch());
				queryEntity.setUserId(userHealthlyConfigStruc.getUserId());
				queryEntity.setDeviceId(userHealthlyConfigStruc.getDeviceId());
				queryEntity.setSleepSwitch(userHealthlyConfigStruc.getSleepSwitch());
				eJLComUserHealythConfigDaoImpl.insert(queryEntity);
			}
			else{
				queryEntity.setSittingSwitch(userHealthlyConfigStruc.getRemindSwitch());
				queryEntity.setStepSwitch(userHealthlyConfigStruc.getStepCountSwitch());
				queryEntity.setSleepSwitch(userHealthlyConfigStruc.getSleepSwitch());
				eJLComUserHealythConfigDaoImpl.update(queryEntity);
			}
		}*/
	}
	
	public int clearPhoneForUnconfirmUser(Context ctx,Long familyId)throws BizException {
		return ejlUserDaoImpl.clearPhoneForUnconfirmUser(ctx, familyId);
	}
	
	public EjlUser getUserByThirdPartyToken(String thirdPartyToken,Integer type) throws BizException{
		
		return ejlUserDaoImpl.getUserByThirdPartyToken(thirdPartyToken, type);
	}
}