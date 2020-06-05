package com.ryan.tmall.util;

import org.springframework.web.multipart.MultipartFile;

/**
 * 上传图片，其中有一个 MultipartFile 类型的属性，用于接受上传文件的注入
 */
public class UploadedImageFile {
    MultipartFile image;

    public MultipartFile getImage() {
        return image;
    }

    public void setImage(MultipartFile image) {
        this.image = image;
    }

}
