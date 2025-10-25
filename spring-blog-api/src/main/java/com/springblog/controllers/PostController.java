package com.springblog.controllers;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpSession;
import javax.validation.executable.ExecutableType;
import javax.validation.executable.ValidateOnExecution;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import com.springblog.services.functions.ValidationData;
import com.springblog.models.Post;
import com.springblog.services.daos.PostDAO;
import com.springblog.services.functions.Alert;
import com.springblog.services.functions.Picture;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PostController {

    @Autowired
    private PostDAO postDAO;

    @Autowired
    private Alert alert;

    @Autowired
    private Picture picture;

    @Autowired
    private ValidationData validationData;

    @Autowired
    private Post post;
    
    @RequestMapping(value = "/index", method = RequestMethod.POST)
    public @ResponseBody
    String Search(@RequestParam(value = "search") String search, @RequestParam("token") String token, HttpSession session) {
        Gson gson = new Gson();
        Map<String, Object> datas = new HashMap<>();
        try {
            if (session.getAttribute("token").toString().equals(token)) {
                List<Post> posts = postDAO.GetPostByTitle(search);
                if (posts != null) {
                    datas.put("posts", posts);
                } else {
                    datas.put("alert", alert.GetAlert(6, 6));
                    datas.put("page", "/errors/505");
                }
            } else {
                datas.put("alert", alert.GetAlert(6, 6));
                datas.put("page", "/errors/505");
            }
        } catch (Exception e) {
            datas.put("alert", alert.GetAlert(6, 6));
            datas.put("page", "/errors/505");
        }
        return gson.toJson(datas);
    }

    @RequestMapping(value = "/show/{id}", method = RequestMethod.PUT)
    public @ResponseBody
    String GetPostForShow(@PathVariable("id") int id, HttpSession session, HttpServletRequest request) {
        
        Map<String, Object> datas = new HashMap<>();
        Gson gson = new Gson();
        try {
            post = postDAO.GetPostByID(id);
            if (post != null) {
                if (!"".equals(post.getTitle())) {
                    
                    if (post.getPicture() != null && !"".equals(post.getPicture())) {
                        String imageUrl = picture.getPictureUrl() + post.getPicture();
                        System.out.println(imageUrl);
                        datas.put("picture", imageUrl);
                    } else {
                        datas.put("picture", picture.getPictureUrl() + "noimage.jpg");
                    }
                    
                    datas.put("author", post.getAuthor().getName());
                    post.setAuthor(null);
                    post.setPicture(null);
                    datas.put("post", post);
                } else {
                    datas.put("alert", alert.GetAlert(7, 7));
                    datas.put("page", "/errors/404");
                }
            } else {
                datas.put("alert", alert.GetAlert(7, 7));
                datas.put("page", "/errors/404");
            }
        } catch (Exception e) {
            datas.put("alert", alert.GetAlert(8, 8));
            datas.put("page", "/errors/505");
        }
        return gson.toJson(datas);
    }

        @ValidateOnExecution(type = ExecutableType.NONE)
    @RequestMapping(value = "/new", method = RequestMethod.POST)
    public @ResponseBody
    String Save(@RequestParam("title") String title, @RequestParam("briefing") String briefing, @RequestParam("text") String text, @RequestParam("token") String token, @RequestParam(value = "picture", required = false) CommonsMultipartFile file, HttpSession session) throws IOException {
        Gson gson = new Gson();
        Map<String, Object> datas = new HashMap<>();
        if (session.getAttribute("token").toString().equals(token)) {
            try {
                post.setTitle(title);
                post.setBriefing(briefing);
                post.setText(text);
                
                if (file != null && !file.isEmpty()) {
                    String timestamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
                    String fileName = timestamp + ".jpg";

                    String uploadPath = session.getServletContext().getRealPath("/pictures/");
                   
                    File uploadDir = new File(uploadPath);
                    if (!uploadDir.exists()) {
                        uploadDir.mkdirs();
                    }

                    File dest = new File(uploadDir, fileName);

                    file.transferTo(dest);

                    post.setPicture(fileName);

                } else {
                    post.setPicture("noimage.png");
                }

                List<String> errors = validationData.ValidationPost(post);
                if (errors.isEmpty()) {
                    int result = postDAO.InsertPost(post, Integer.parseInt(session.getAttribute("id").toString()));
                    if (result != -1) {
                        datas.put("alert", alert.GetAlert(1, result));
                        datas.put("page", "/index");
                    }
                    if (result == -1) {
                        datas.put("alert", alert.GetAlert(1, result));
                        datas.put("page", "/errors/505");
                    }
                } else {
                    datas.put("errors", errors);
                }
            } catch (NumberFormatException e) {
                datas.put("alert", "Unsuccessfully created.");
                datas.put("page", "/errors/505");
            }
        } else {
            datas.put("alert", "Unsuccessfully created.");
            datas.put("page", "/errors/505");
        }
        return gson.toJson(datas);
    }
    
    @RequestMapping(value = "/show/{id}/checkauthor", method = RequestMethod.PUT)
    public @ResponseBody
    String CheckAuthorPost(@PathVariable("id") int id, HttpSession session) {
        Gson gson = new Gson();
        Map<String, Object> datas = new HashMap<>();
        try {
            post = postDAO.GetPostByID(id);
            if (post != null) {
                if(!(post.getAuthor().getId() == Integer.parseInt(session.getAttribute("id").toString()))){
                    datas.put("check", false);
                    datas.put("alert", alert.GetAlert(2, 2));
                }else{
                    datas.put("check", true);
                }
            } else {
                datas.put("alert", alert.GetAlert(8, 8));
                datas.put("page", "/errors/505");
            }
        } catch (NumberFormatException e) {
            datas.put("alert", alert.GetAlert(8, 8));
            datas.put("page", "/errors/505");
        }
        return gson.toJson(datas);
    }
    
    @RequestMapping(value = "/edit/{id}", method = RequestMethod.PUT)
    public @ResponseBody
    String GetPostForEdit(@PathVariable("id") int id, HttpSession session) {
        Gson gson = new Gson();
        Map<String, Object> datas = new HashMap<>();
        try {
            post = postDAO.GetPostByID(id);
            if (post != null) {
                if (!"".equals(post.getTitle())) {
                    if (post.getAuthor().getId() == Integer.parseInt(session.getAttribute("id").toString())) {
                        datas.put("author", post.getAuthor().getName());
                        
                        if (post.getPicture() != null && !"".equals(post.getPicture())) {
                            String imageUrl =  picture.getPictureUrl() + post.getPicture();
                            System.out.println(imageUrl);
                            datas.put("picture", imageUrl);
                        } else {
                            datas.put("picture",  picture.getPictureUrl() + "noimage.jpg");
                        }
                        
                        post.setAuthor(null);
                        post.setPicture(null);
                        
                        datas.put("post", post);
                    } else {
                        datas.put("alert", alert.GetAlert(2, 2));
                        datas.put("page", "/show/" + id);
                    }
                } else {
                    datas.put("alert", alert.GetAlert(7, 7));
                    datas.put("page", "/errors/404");
                }
            } else {
                datas.put("alert", alert.GetAlert(8, 8));
                datas.put("page", "/errors/505");
            }
        } catch (NumberFormatException e) {
            datas.put("alert", alert.GetAlert(8, 8));
            datas.put("page", "/errors/505");
        }
        return gson.toJson(datas);
    }

    @ValidateOnExecution(type = ExecutableType.NONE)
    @RequestMapping(value = "/edit/update", method = RequestMethod.POST)
    public @ResponseBody
    String Update(@RequestParam("id") Integer id, @RequestParam("title") String title, @RequestParam("briefing") String briefing, @RequestParam("text") String text, @RequestParam("token") String token, @RequestParam(value = "picture", required = false) CommonsMultipartFile file, HttpSession session) throws IOException {
        Gson gson = new Gson();
        Map<String, Object> datas = new HashMap<>();
        if (session.getAttribute("token").toString().equals(token)) {
            try {
                post.setId(id);
                Post post_return = postDAO.GetPostByID(post.getId());
                if (post_return != null) {
                    if (!"".equals(post_return.getTitle())) {
                        
                        post.setPicture(post_return.getPicture());

                        if (file != null && !file.isEmpty()) {
                            String oldFileName = post_return.getPicture();

                            String uploadPath = session.getServletContext().getRealPath("/pictures/");

                            File uploadDir = new File(uploadPath);
                            if (!uploadDir.exists()) {
                                uploadDir.mkdirs();
                            }

                            if (oldFileName != null && !oldFileName.isEmpty()) {
                                File oldFile = new File(uploadPath, oldFileName);
                                if (oldFile.exists()) {
                                    oldFile.delete(); 
                                }
                            }

                            File dest = new File(uploadDir, oldFileName); 
                            file.transferTo(dest); 

                            post.setPicture(oldFileName); 
                        }

                        post.setTitle(title);
                        post.setBriefing(briefing);
                        post.setText(text);
                        post.setAuthor(post_return.getAuthor());
                        post.setDatePost(post_return.getDatePost());
                       
                        List<String> errors = validationData.ValidationPost(post);
                        if (errors.isEmpty()) {
                            int result = postDAO.EditPost(post, Integer.parseInt(session.getAttribute("id").toString()));
                            if (result != -1) {
                                datas.put("alert", alert.GetAlert(2, result));
                                datas.put("page", "/index");
                            }
                            if (result == -1) {
                                datas.put("alert", alert.GetAlert(2, result));
                                datas.put("page", "/errors/505");
                            }
                        } else {
                            datas.put("errors", errors);
                        }
                    } else {
                        datas.put("alert", alert.GetAlert(7, 7));
                        datas.put("page", "/errors/404");
                    }
                } else {
                    datas.put("alert", "Unsuccessfully edited.");
                    datas.put("page", "/errors/505");
                }
            } catch (NumberFormatException e) {
                datas.put("alert", "Unsuccessfully edited.");
                datas.put("page", "/errors/505");
            }
        } else {
            datas.put("alert", "Unsuccessfully edited.");
            datas.put("page", "/errors/505");
        }
        return gson.toJson(datas);
    }

    @RequestMapping(value = "/delete/{id}/{token}", method = RequestMethod.DELETE)
    public @ResponseBody
    String Delete(@PathVariable("id") int id, @PathVariable("token") String token, HttpSession session) {
        Gson gson = new Gson();
        Map<String, Object> datas = new HashMap<>();
        if (session.getAttribute("token").toString().equals(token)) {
            try {
                post = postDAO.GetPostByID(id);
                if (post != null) {
                    if (!"".equals(post.getText())) {
                        int result = postDAO.DeletePost(post, Integer.parseInt(session.getAttribute("id").toString()));
                        if (result != -1) {
                            
                            String picture_to_delete = session.getServletContext().getRealPath("/pictures/") + post.getPicture();
                            File file = new File(picture_to_delete);
                           
                            if (file.exists()){
                                file.delete();
                            }
                            
                            datas.put("alert", alert.GetAlert(3, result));
                            datas.put("page", "/index");
                        }
                        if (result == -1) {
                            datas.put("alert", alert.GetAlert(3, result));
                            datas.put("page", "/errors/505");
                        }
                    } else {
                        datas.put("alert", alert.GetAlert(7, 7));
                        datas.put("page", "/errors/404");
                    }
                } else {
                    datas.put("alert", "Unsuccessfully deleted.");
                    datas.put("page", "/errors/505");
                }
            } catch (NumberFormatException e) {
                datas.put("alert", "Unsuccessfully deleted.");
                datas.put("page", "/errors/505");
            }
        } else {
            datas.put("alert", "Unsuccessfully deleted.");
            datas.put("page", "/errors/505");
        }
        return gson.toJson(datas);
    }
}
