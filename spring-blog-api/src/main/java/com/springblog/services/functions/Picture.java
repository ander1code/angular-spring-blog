package com.springblog.services.functions;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
//import org.springframework.context.ApplicationContext;
//import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class Picture {

    public String ByteToBase64(byte[] bytes) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("data:image/png;base64,");
            sb.append(StringUtils.newStringUtf8(Base64.encodeBase64(bytes, false)));
            return sb.toString();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
    
    public String getPictureUrl() {
        // ApplicationContext context = new ClassPathXmlApplicationContext("/WEB-INF/applicationContext.xml");
        // String pictureUrl = (String) context.getBean("host") + "/pictures/";
        String host = "http://localhost:8080";
        String pictureUrl = host + "/pictures/";
        return pictureUrl;
    }
}
