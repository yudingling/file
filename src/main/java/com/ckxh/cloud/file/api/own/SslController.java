package com.ckxh.cloud.file.api.own;

import java.io.File;
import java.util.Date;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.ckxh.cloud.base.annotation.AuthPathOnBind;
import com.ckxh.cloud.base.util.ConstString;
import com.ckxh.cloud.base.util.DateUtil;
import com.ckxh.cloud.base.util.JsonUtil;
import com.ckxh.cloud.base.util.LogUtil;
import com.ckxh.cloud.base.util.MsgPackUtil;

@Scope("singleton")
@Controller
@RequestMapping("/own/ssl")
public class SslController extends FileLoader {
	
	@ResponseBody
	@RequestMapping(method=RequestMethod.GET)
	@AuthPathOnBind("get:/platformApi/own/user/normal/#")
	public void download(@RequestParam String fileId, HttpServletRequest request, HttpServletResponse response){
		this.downloadFile(fileId, request, response, true);
	}
	
	@ResponseBody
	@RequestMapping(method=RequestMethod.POST, consumes={"multipart/form-data"})
	public String upload(@RequestPart MultipartFile keyFile, @RequestPart MultipartFile crtFile, HttpServletRequest request, HttpServletResponse response){
		if(keyFile == null || keyFile.isEmpty() || crtFile == null || crtFile.isEmpty()){
			return JsonUtil.createSuccessJson(false, null, "invalid", "file error, need keyFile and crtFile");
		}else{
			File newKeyFile = null, newCrtFile = null;
			try {
				String uid = this.getFileLoaderUid(request);
				
				String filePfx = "upload/ssl/" + DateUtil.convertDateToString(new Date(), DateUtil.Date_YMD_NoSep) + "/" + uid + "/";
				
            	String path = request.getServletContext().getRealPath("/") + filePfx;
            	
            	newKeyFile = new File(path + "client.key");
            	newCrtFile = new File(path + "client.crt");
            	
            	if(newKeyFile.getParent()!=null && !new File(newKeyFile.getParent()).exists()){
        		    new File(newKeyFile.getParent()).mkdirs();
        		}
            	
            	keyFile.transferTo(newKeyFile);
            	crtFile.transferTo(newCrtFile);
            	
        		Long keyId = this.saveFile(uid, filePfx, newKeyFile.getName());
        		Long crtId = this.saveFile(uid, filePfx, newCrtFile.getName());
            		
        		HashMap<String, Long> map = new HashMap<String, Long>();
        		map.put(ConstString.SSLFileLoader_returnFileId_key, keyId);
        		map.put(ConstString.SSLFileLoader_returnFileId_crt, crtId);
        		
            	return JsonUtil.createSuccessJson(true, MsgPackUtil.serialize2Str(map) , "success", null);
            }catch (Exception e) {
            	//ssl api was called by platform from the background, we need to log the error
            	LogUtil.error(e);
                
                if(newKeyFile != null){
                	newKeyFile.delete();
                }
                if(newCrtFile != null){
                	newCrtFile.delete();
                }
                
                return JsonUtil.createSuccessJson(false, null, "error", e.getMessage());
            }
		}
	}
}
