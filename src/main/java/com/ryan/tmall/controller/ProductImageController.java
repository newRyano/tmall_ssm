package com.ryan.tmall.controller;
 
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
 
import javax.imageio.ImageIO;
import javax.servlet.http.HttpSession;

import com.ryan.tmall.pojo.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.ryan.tmall.pojo.ProductImage;
import com.ryan.tmall.service.ProductImageService;
import com.ryan.tmall.service.ProductService;
import com.ryan.tmall.util.ImageUtil;
import com.ryan.tmall.util.UploadedImageFile;
 
@Controller
@RequestMapping("")
public class ProductImageController {
    @Autowired
    ProductService productService;
 
    @Autowired
    ProductImageService productImageService;

    /**
     * 1. 通过pi对象接受type和pid的注入
       2. 借助productImageService，向数据库中插入数据。
       3. 根据session().getServletContext().getRealPath( "img/productSingle")，定位到存放单个产品图片的目录
          除了productSingle，还有productSingle_middle和productSingle_small。 因为每上传一张图片，都会有对应的正常，中等和小的三种大小图片，并且放在3个不同的目录下
       4. 文件命名以保存到数据库的产品图片对象的id+".jpg"的格式命名
       5. 通过uploadedImageFile保存文件
       6. 借助ImageUtil.change2jpg()方法把格式真正转化为jpg，而不仅仅是后缀名为.jpg
       7. 再借助ImageUtil.resizeImage把正常大小的图片，改变大小之后，分别复制到productSingle_middle和productSingle_small目录下。
       8. 处理完毕之后，客户端条跳转到admin_productImage_list?pid=，并带上pid。
     */
    @RequestMapping("admin_productImage_add")
    public String add(ProductImage  pi, HttpSession session, UploadedImageFile uploadedImageFile) {
        productImageService.add(pi);
        String fileName = pi.getId()+ ".jpg";
        String imageFolder;
        String imageFolder_small=null;
        String imageFolder_middle=null;
        if(ProductImageService.type_single.equals(pi.getType())){
            imageFolder= session.getServletContext().getRealPath("img/productSingle");
            imageFolder_small= session.getServletContext().getRealPath("img/productSingle_small");
            imageFolder_middle= session.getServletContext().getRealPath("img/productSingle_middle");
        }
        else{
            imageFolder= session.getServletContext().getRealPath("img/productDetail");
        }
 
        File f = new File(imageFolder, fileName);
        f.getParentFile().mkdirs();
        try {
            uploadedImageFile.getImage().transferTo(f);
            BufferedImage img = ImageUtil.change2jpg(f);
            ImageIO.write(img, "jpg", f);
 
            if(ProductImageService.type_single.equals(pi.getType())) {
                File f_small = new File(imageFolder_small, fileName);
                File f_middle = new File(imageFolder_middle, fileName);
 
                ImageUtil.resizeImage(f, 56, 56, f_small);
                ImageUtil.resizeImage(f, 217, 190, f_middle);
            }
 
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:admin_productImage_list?pid="+pi.getPid();
    }

    /**
     * 点击删除超链，进入ProductImageController的delete方法
      1. 获取id
      2. 根据id获取ProductImage 对象pi
      3. 借助productImageService，删除数据
      4. 如果是单个图片，那么删除3张正常，中等，小号图片
      5. 如果是详情图片，那么删除一张图片
      6. 客户端跳转到admin_productImage_list地址
     */
    @RequestMapping("admin_productImage_delete")
    public String delete(int id,HttpSession session) {
        ProductImage pi = productImageService.get(id);
 
        String fileName = pi.getId()+ ".jpg";
        String imageFolder;
        String imageFolder_small=null;
        String imageFolder_middle=null;
 
        if(ProductImageService.type_single.equals(pi.getType())){
            imageFolder= session.getServletContext().getRealPath("img/productSingle");
            imageFolder_small= session.getServletContext().getRealPath("img/productSingle_small");
            imageFolder_middle= session.getServletContext().getRealPath("img/productSingle_middle");
            File imageFile = new File(imageFolder,fileName);
            File f_small = new File(imageFolder_small,fileName);
            File f_middle = new File(imageFolder_middle,fileName);
            imageFile.delete();
            f_small.delete();
            f_middle.delete();
 
        }
        else{
            imageFolder= session.getServletContext().getRealPath("img/productDetail");
            File imageFile = new File(imageFolder,fileName);
            imageFile.delete();
        }
 
        productImageService.delete(id);
 
        return "redirect:admin_productImage_list?pid="+pi.getPid();
    }

    /**
     * 通过产品页面的图片管理访问ProductImageController的list()方法
       1. 获取参数pid
       2. 根据pid获取Product对象
       3. 根据pid对象获取单个图片的集合pisSingle
       4. 根据pid对象获取详情图片的集合pisDetail
       5. 把product 对象，pisSingle ，pisDetail放在model上
       6. 服务端跳转到admin/listProductImage.jsp页面
       7. 在listProductImage.jsp，使用c:forEach 遍历pisSingle
       8. 在listProductImage.jsp，使用c:forEach 遍历pisDetail
     */
    @RequestMapping("admin_productImage_list")
    public String list(int pid, Model model) {
        Product p =productService.get(pid);
        List<ProductImage> pisSingle = productImageService.list(pid, ProductImageService.type_single);
        List<ProductImage> pisDetail = productImageService.list(pid, ProductImageService.type_detail);
 
        model.addAttribute("p", p);
        model.addAttribute("pisSingle", pisSingle);
        model.addAttribute("pisDetail", pisDetail);
 
        return "admin/listProductImage";
    }
}