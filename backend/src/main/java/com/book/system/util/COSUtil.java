package com.book.system.util;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.region.Region;
import com.qcloud.cos.model.PutObjectRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class COSUtil {
    
    // 从环境变量读取配置
    private static final String SECRET_ID = System.getenv("COS_SECRET_ID");
    private static final String SECRET_KEY = System.getenv("COS_SECRET_KEY");
    private static final String BUCKET_NAME = System.getenv("COS_BUCKET_NAME");
    private static final String REGION = System.getenv("COS_REGION");
    
    private static COSClient cosClient;
    
    static {
        System.out.println("=== COS 环境变量调试 ===");
    System.out.println("COS_SECRET_ID: " + (System.getenv("COS_SECRET_ID") != null ? "已设置 (长度: " + System.getenv("COS_SECRET_ID").length() + ")" : "未设置"));
    System.out.println("COS_SECRET_KEY: " + (System.getenv("COS_SECRET_KEY") != null ? "已设置 (长度: " + System.getenv("COS_SECRET_KEY").length() + ")" : "未设置"));
    System.out.println("COS_BUCKET_NAME: " + (System.getenv("COS_BUCKET_NAME") != null ? System.getenv("COS_BUCKET_NAME") : "未设置"));
    System.out.println("COS_REGION: " + (System.getenv("COS_REGION") != null ? System.getenv("COS_REGION") : "未设置"));
        if (SECRET_ID != null && SECRET_KEY != null && BUCKET_NAME != null && REGION != null) {
            try {
                BasicCOSCredentials cred = new BasicCOSCredentials(SECRET_ID, SECRET_KEY);
                Region region = new Region(REGION);
                ClientConfig clientConfig = new ClientConfig(region);
                cosClient = new COSClient(cred, clientConfig);
                System.out.println("COS客户端初始化成功");
            } catch (Exception e) {
                System.err.println("COS客户端初始化失败: " + e.getMessage());
            }
        } else {
            System.err.println("COS环境变量未配置");
        }
    }
    
    public static String uploadFile(MultipartFile file, String fileName) {
        if (cosClient == null) {
            System.err.println("COS客户端未初始化");
            return null;
        }
        
        try {
            // 创建临时文件
            File tempFile = File.createTempFile("avatar", fileName);
            try (InputStream is = file.getInputStream();
                 FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
            }
            
            // 上传到 COS
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                BUCKET_NAME, 
                "avatars/" + fileName, 
                tempFile
            );
            cosClient.putObject(putObjectRequest);
            
            // 删除临时文件
            tempFile.delete();
            
            // 返回访问 URL
            return "https://" + BUCKET_NAME + ".cos." + REGION + ".myqcloud.com/avatars/" + fileName;
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}