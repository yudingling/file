package com.ckxh.cloud.file.api.own;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import com.ckxh.cloud.base.util.Common;
import com.ckxh.cloud.base.util.ConstString;
import com.ckxh.cloud.base.util.DateUtil;
import com.ckxh.cloud.base.util.LogUtil;
import com.ckxh.cloud.base.util.MsgPackUtil;
import com.ckxh.cloud.persistence.common.SysTool;
import com.ckxh.cloud.persistence.common.auth.AuthUtil;
import com.ckxh.cloud.persistence.db.model.MAIN_FILE;
import com.ckxh.cloud.persistence.db.sys.service.FileService;
import com.fasterxml.jackson.core.JsonProcessingException;

public class FileLoader {
	@Autowired
	protected FileService fileService;
	
	private boolean checkFile(String filePath, HttpServletResponse response){
		if((new File(filePath)).exists()){
			return true;
			
		}else{
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return false;
		}
	}
	
	/**
	 * download file
	 */
	private void responseFile(String fileName, String filePath, HttpServletResponse response){
		
		if(!this.checkFile(filePath, response)){
			return;
		}
		
		OutputStream os = null;
		try {
			os = response.getOutputStream();
			
			response.reset();
			response.setHeader("Content-Disposition", "attachment; fileName=" + fileName);
			response.setContentType("multipart/form-data");
			
			Common.writeBytesFromFile(filePath, os);
			
		} catch (IOException e) {
			LogUtil.error(e);
		} finally {
            if (os != null) {
                try {
					os.close();
				} catch (IOException e) {
					LogUtil.error(e);
				}
            }
        }
	}
	
	/**
	 * download file(stream)
	 */
	private void responseByteStream(String fileName, String filePath, HttpServletResponse response){
		
		if(!this.checkFile(filePath, response)){
			return;
		}
		
		OutputStream os = null;
		try {
			os = response.getOutputStream();
			
			response.reset();
			response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        	response.setContentType("application/octet-stream; charset=utf-8");
			
			Common.writeBytesFromFile(filePath, os);
			
		} catch (IOException e) {
			LogUtil.error(e);
		} finally {
            if (os != null) {
                try {
					os.close();
				} catch (IOException e) {
					LogUtil.error(e);
				}
            }
        }
	}
	
	protected void downloadStream(String fileId, HttpServletRequest request, HttpServletResponse response, boolean privateData){
		
		MAIN_FILE file = this.getAvailFileForUser(request, fileId, privateData);
		
		if(file != null){
			String filePath = request.getServletContext().getRealPath("/") + file.getFILE_PFX() + file.getFILE_NM();
			this.responseByteStream(file.getFILE_NM(), filePath, response);
			
		}else{
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		}
	}
	
	protected void downloadFile(String fileId, HttpServletRequest request, HttpServletResponse response, boolean privateData){
		
		MAIN_FILE file = this.getAvailFileForUser(request, fileId, privateData);
		
		if(file != null){
			String filePath = request.getServletContext().getRealPath("/") + file.getFILE_PFX() + file.getFILE_NM();
			this.responseFile(file.getFILE_NM(), filePath, response);
			
		}else{
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		}
	}
	
	
	/**
	 * whether the user is authorized to the file.
	 */
	private MAIN_FILE getAvailFileForUser(HttpServletRequest request, String fileId, boolean privateData){
		if(fileId == null || fileId.length() <= 0){
			return null;
		}else{
			String uid = this.getFileLoaderUid(request);
			
			if(uid != null){
				MAIN_FILE file = this.fileService.getFile(fileId);
				
				return ConstString.Sys_fileLoader.equals(uid) || (!privateData) || uid.equals(file.getU_ID()) ? file : null;
			}else
				return null;
		}
	}
	
	/**
	 * get the user id for the current file request, search with the priority below:
	 *     session > FileLoader_uid > AuthParam_uid
	 */
	protected final String getFileLoaderUid(HttpServletRequest request){
		String uid = AuthUtil.getIdFromSession(request.getSession());
		
		if(uid == null){
			uid = request.getParameter(ConstString.FileLoader_uid);
			
			if(uid == null){
				uid = request.getParameter(ConstString.AuthParam_uid);
			}
		}
		
		return uid;
	}
	
	protected final String getFileIdRetJson(Long fileId) throws JsonProcessingException{
		Map<String, Long> map = new HashMap<String, Long>();
		map.put(ConstString.FileLoader_returnFileId, fileId);
		
		return MsgPackUtil.serialize2Str(map);
	}
	
	protected final String getUniqueFileName(String orgName){
		int index = orgName.lastIndexOf(".");
		String endFix = index >= 0 ? orgName.substring(index) : "";
		
		return Common.uuid32() + endFix;
	}
	
	protected Long saveFile(String uid, String filePfx, String fileNm) throws Exception{
		Timestamp ts = DateUtil.getCurrentTS();
		
		MAIN_FILE file = new MAIN_FILE(
				SysTool.longUuidInLocal(), //'file' project is deployed as a singleton, we can use local uuid
				uid, 
				filePfx, 
				fileNm, 
				ts, ts);
		
		if(this.fileService.saveFile(file)){
			return file.getFILE_ID();
		}else{
			throw new Exception("save db failed");
		}
	}
}
