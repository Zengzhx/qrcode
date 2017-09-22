package com.uusafe.pub.action;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import org.apache.commons.codec.net.QCodec;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.google.zxing.WriterException;
import com.uusafe.framework.annotation.ActionClass;
import com.uusafe.framework.cache.CacheUtil;
import com.uusafe.framework.common.ThreadParameter;
import com.uusafe.framework.common.UUConstant;
import com.uusafe.framework.common.UUDbConstant;
import com.uusafe.framework.core.session.BusinessRequest;
import com.uusafe.framework.core.session.BusinessRespone;
import com.uusafe.framework.proxy.ServiceAnnoProxy;
import com.uusafe.framework.util.ActionUtils;
import com.uusafe.framework.util.Page;
import com.uusafe.framework.util.qr.QRUtils;
import com.uusafe.pub.init.ErailScanRecordLoader;
import com.uusafe.pub.service.ErailService;

/**
 * 电子围栏Action
 * @ClassName ElectronicFenceAction
 * @Description TODO
 * @author wangzf
 * @date 2016年12月21日 下午5:59:06
 */
@ActionClass(value="electronicFence")
public class ElectronicFenceAction {
	
	private static Logger log = Logger.getLogger(ElectronicFenceAction.class);

	
	public ErailService getErailService(boolean hasTrans){
		String serviceValue = "erailServiceImpl";
		return  (ErailService) (hasTrans ? ServiceAnnoProxy.getTransService(serviceValue) : ServiceAnnoProxy.getService(serviceValue));
	}
	
	/**
	 * 创建电子围栏二维码
	 * @Title getFenceQRCode
	 * @Author wangzf
	 * @Description TODO
	 * @param request
	 * @param response
	 * @return void
	 */
	public void getFenceQRCode(BusinessRequest request ,BusinessRespone response){
//		String enterUrl = "/data/QRCode/developer/enter.jpg";
//		String leaveUrl = "/data/QRCode/developer/leave.jpg";
//		response.set("enterUrl", enterUrl);
//		response.set("leaveUrl", leaveUrl);
//		String enterContent = "{\"content\":{\"erail\":1},\"rand\":\""+id+"\"}";
		String rand = (String)CacheUtil.getFromCache("FenceQRCodeRand", String.class,true);
//		log.info("getFenceQRCode------------------->>>>"+rand);
		response.set("erailQRCodeRand", rand);
		String sessionId = request.getSession().getSessionId();
		response.set("sessionId", sessionId);
		String companyCode = ActionUtils.getCompanyCode(request);
		
		response.set("enterErailList", ErailScanRecordLoader.enterErailList.get(companyCode));
		response.set("leaveErailList", ErailScanRecordLoader.leaveErailList.get(companyCode));
		
		if(ErailScanRecordLoader.currentErailList.size()>0) {
			log.info("currentErailList----size------------>>>>"+ErailScanRecordLoader.currentErailList.size());

			try {
				String erailRecordJson = ErailScanRecordLoader.currentErailList.get(0);
				log.info("currentErailList----erailRecordJson------------>>>>"+erailRecordJson);
				if(StringUtils.isNotBlank(erailRecordJson)) {
					JSONObject obj = JSONObject.fromObject(erailRecordJson);	
					String erailSessionId = obj.getString("sessionId");
					log.info("erailSessionId---------------->>>>"+erailSessionId);
					log.info("sessionId     ---------------->>>>"+sessionId);

					if(sessionId.equalsIgnoreCase(erailSessionId)) {
						String userName = obj.getString("userName");
						int erailStatus = obj.getInt("erailStatus");
						response.set("userName", userName);
						response.set("erailStatus", erailStatus);
						
						ErailScanRecordLoader.currentErailList.remove(0);
					}
				}else {
					ErailScanRecordLoader.currentErailList.remove(0);
				}
			} catch (Exception e) {
			}
		}
		
		response.set(UUConstant.RESULT_STATUS, UUConstant.RESULT_STATUS_OK);
		
	} 
	

	/**
	 * 创建二维码
	 * @Title createQRCode
	 * @Author wangzf
	 * @Description TODO
	 * @param content 二维码内容
	 * @param path 创建图片路径
	 * @throws Exception
	 * @return void
	 */
	public void createQRCode(String content,String path) throws Exception {
//		String baseUrl = "https://192.168.1.158:3000/emm/qrscan?";
//		String companyCode = "developer";
//		String loginName = "f3";
//		long qrId = 540;
//		int isPwValidate = 1;
//		String key = "zhizhangyi";
//		String companyCode1 = DesUtil.encrypt(companyCode, key);
//		String qrId1 = DesUtil.encrypt(qrId + "", key);
//		String loginName1 = DesUtil.encrypt(loginName, key);
//		String isPwValidate1 = DesUtil.encrypt(isPwValidate + "", key);
//		String content = baseUrl +"companyCode=" + companyCode1 + "&&loginName=" + loginName1 + "&&qrId=" + qrId1 + "&&isPwValidate=" + isPwValidate1;
		
		try {

//			String path = "/data/QRCode/developer/cwwH.jpg";
			File file = new File(path);
			if (!file.isDirectory()) {
				file.mkdirs();
			}

			QRUtils.genBarcode(content, file);
		} catch (WriterException e) {
			log.error("@|"+ThreadParameter.get(UUDbConstant.DSNAME_THREAD)+"|@  创建电子围栏二维码失败:"+e);
			e.printStackTrace();
		} catch (IOException e) {
			log.error("@|"+ThreadParameter.get(UUDbConstant.DSNAME_THREAD)+"|@  创建电子围栏二维码失败:"+e);
			e.printStackTrace();
		}

	}
	
	/**
	 * 获取电子围栏内人员名单
	 * */
	public void queryErailDeviceList(BusinessRequest request ,BusinessRespone response){
		ErailService service = getErailService(false);
		Map params = request.getParams();
		int pageNumber = request.getInt("pageNumber", Page.DEF_CURRENT_PAGE);
		int pageSize = request.getInt("pageSize", Page.DEF_PAGE_SIZE);
		Page page = service.queryErailDeviceList(params, pageNumber, pageSize);
		
		if(page != null) {
			response.set("rows", page.getResults());
			response.set("pageNumber", page.getCurrentPage());
			response.set("pageSize", page.getPageSize());
			response.set("total", page.getRecordCount());
			response.set("pageCount", page.getPageCount());
			response.set(UUConstant.RESULT_STATUS, UUConstant.RESULT_STATUS_OK);
		}else {
			response.set(UUConstant.RESULT_STATUS, UUConstant.RESULT_STATUS_FAIL);
		}
	}
	
	public void insertErailDevice(BusinessRequest request ,BusinessRespone response) {
		ErailService service = getErailService(true);
		String deviceId = request.getStr("deviceId");
		int erailStatus = request.getInt("erailStatus");
		if(StringUtils.isNotBlank(deviceId)) {
			service.insertErailDevice(deviceId,erailStatus);
		}
		
		List<Map> erailList = service.queryErailDeviceLastList(erailStatus);
		if(erailList!=null && erailList.size()>0) {
			response.set("erailList", erailList);
			response.set(UUConstant.RESULT_STATUS, UUConstant.RESULT_STATUS_OK);
		}else {
			response.set(UUConstant.RESULT_STATUS, UUConstant.RESULT_STATUS_FAIL);
		}
	}
	
}
