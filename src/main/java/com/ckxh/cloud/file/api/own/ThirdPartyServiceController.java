package com.ckxh.cloud.file.api.own;

import java.io.File;
import java.util.Date;
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
import com.ckxh.cloud.base.util.DateUtil;
import com.ckxh.cloud.base.util.JsonUtil;

@Scope("singleton")
@Controller
@RequestMapping("/own/3rdService")
@AuthPathOnBind("get:/platformApi/own/thirdparty/normal/#")
public class ThirdPartyServiceController extends FileLoader {

	@ResponseBody
	@RequestMapping(method=RequestMethod.GET)
	public void download(@RequestParam String fileId, HttpServletRequest request, HttpServletResponse response){
		this.downloadFile(fileId, request, response, false);
	}
	
	@ResponseBody
	@RequestMapping(method=RequestMethod.POST, consumes={"multipart/form-data"})
	public String upload(@RequestPart MultipartFile file, HttpServletRequest request, HttpServletResponse response){
		if (file != null && !file.isEmpty() && file.getSize() > 0) { 
			if(file.getSize() / 1024 > 200){
				return JsonUtil.createSuccessJson(false, null, "invalid", "max file size 200KB");
			}
			
			File newFile = null;
            try {
            	String uid = this.getFileLoaderUid(request);
            	
            	String filePfx = "upload/thirdparty/" + DateUtil.convertDateToString(new Date(), DateUtil.Date_YMD_NoSep) + "/" + uid + "/";
            	
                newFile = new File(request.getServletContext().getRealPath("/") + filePfx + this.getUniqueFileName(file.getOriginalFilename()));
                
                if(newFile.getParent()!=null && !new File(newFile.getParent()).exists()){
        		    new File(newFile.getParent()).mkdirs();
        		}
                
                file.transferTo(newFile);
                
                Long fileId = this.saveFile(uid, filePfx, newFile.getName());
                
                return JsonUtil.createSuccessJson(true, this.getFileIdRetJson(fileId), "success", null);
            }catch (Exception e) {
                if(newFile != null){
                	newFile.delete();
                }
                
                return JsonUtil.createSuccessJson(false, null, "error", e.getMessage());
            }
        }else{
        	return JsonUtil.createSuccessJson(false, null, "invalid", "no file was found");
        }
	}
}
