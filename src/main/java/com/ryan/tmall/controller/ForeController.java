package com.ryan.tmall.controller;

import com.github.pagehelper.PageHelper;
import com.ryan.tmall.comparator.*;
import com.ryan.tmall.pojo.*;
import com.ryan.tmall.service.*;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("")
public class ForeController {
    @Autowired
    CategoryService categoryService;
    @Autowired
    ProductService productService;
    @Autowired
    UserService userService;
    @Autowired
    ProductImageService productImageService;
    @Autowired
    PropertyValueService propertyValueService;
    @Autowired
    OrderService orderService;
    @Autowired
    OrderItemService orderItemService;
    @Autowired
    ReviewService reviewService;

    @RequestMapping("forehome")
    public String home(Model model) {
        List<Category> cs = categoryService.list();
        productService.fill(cs);
        productService.fillByRow(cs);
        model.addAttribute("cs", cs);
        return "fore/home";
    }

    /**
     * 1. 通过参数User获取浏览器提交的账号密码
     * 2. 通过HtmlUtils.htmlEscape(name);把账号里的特殊符号进行转义
     * 3. 判断用户名是否存在
         3.1 如果已经存在，就服务端跳转到reigster.jsp，并且带上错误提示信息
         3.2 如果不存在，则加入到数据库中，并服务端跳转到registerSuccess.jsp页面
     */
    @RequestMapping("foreregister")
    public String register(Model model, User user) {
        String name = user.getName();
        name = HtmlUtils.htmlEscape(name);
        user.setName(name);
        boolean exist = userService.isExist(name);

        if (exist) {
            String m = "用户名已经被使用,不能使用";
            model.addAttribute("msg", m);
            model.addAttribute("user", null);
            return "fore/register";
        }
        userService.add(user);

        return "redirect:registerSuccessPage";
    }

    /**
     * 1. 获取账号密码
     * 2. 把账号通过HtmlUtils.htmlEscape进行转义
     * 3. 根据账号和密码获取User对象
         3.1 如果对象为空，则服务端跳转回login.jsp，也带上错误信息，并且使用 loginPage.jsp 中的办法显示错误信息
         3.2 如果对象存在，则把对象保存在session中，并客户端跳转到首页"forehome"
     */
    @RequestMapping("forelogin")
    public String login(@RequestParam("name") String name, @RequestParam("password") String password, Model model, HttpSession session) {
        name = HtmlUtils.htmlEscape(name);
        User user = userService.get(name, password);

        if (null == user) {
            model.addAttribute("msg", "账号密码错误");
            return "fore/login";
        }
        session.setAttribute("user", user);
        return "redirect:forehome";
    }

    /**
     * 登出
     * @param session
     * @return
     */
    @RequestMapping("forelogout")
    public String logout(HttpSession session) {
        session.removeAttribute("user");
        return "redirect:forehome";
    }

    /**
     *  1. 获取参数pid
        2. 根据pid获取Product 对象p
        3. 根据对象p，获取这个产品对应的单个图片集合
        4. 根据对象p，获取这个产品对应的详情图片集合
        5. 获取产品的所有属性值
        6. 获取产品对应的所有的评价
        7. 设置产品的销量和评价数量
        8. 把上述取值放在request属性上
        9. 服务端跳转到 "product.jsp" 页面
     */
    @RequestMapping("foreproduct")
    public String product( int pid, Model model) {
        Product p = productService.get(pid);

        List<ProductImage> productSingleImages = productImageService.list(p.getId(), ProductImageService.type_single);
        List<ProductImage> productDetailImages = productImageService.list(p.getId(), ProductImageService.type_detail);
        p.setProductSingleImages(productSingleImages);
        p.setProductDetailImages(productDetailImages);

        List<PropertyValue> pvs = propertyValueService.list(p.getId());
        List<Review> reviews = reviewService.list(p.getId());
        productService.setSaleAndReviewNumber(p);
        model.addAttribute("reviews", reviews);
        model.addAttribute("p", p);
        model.addAttribute("pvs", pvs);
        return "fore/product";
    }

    /**
     * 获取session中的"user"对象
        如果不为空，即表示已经登录，返回字符串"success"
        如果为空，即表示未登录，返回字符串"fail"
     */
    @RequestMapping("forecheckLogin")
    @ResponseBody
    public String checkLogin( HttpSession session) {
        User user =(User)  session.getAttribute("user");
        if(null!=user)
            return "success";
        return "fail";
    }

    /**
     * 在 modal.jsp 中，点击了登录按钮之后，访问路径/foreloginAjax,导致ForeController.loginAjax()方法被调用
     1. 获取账号密码
     2. 通过账号密码获取User对象
        2.1 如果User对象为空，那么就返回"fail"字符串。
        2.2 如果User对象不为空，那么就把User对象放在session中，并返回"success" 字符串
     */
    @RequestMapping("foreloginAjax")
    @ResponseBody
    public String loginAjax(@RequestParam("name") String name, @RequestParam("password") String password,HttpSession session) {
        name = HtmlUtils.htmlEscape(name);
        User user = userService.get(name,password);

        if(null==user){
            return "fail";
        }
        session.setAttribute("user", user);
        return "success";
    }

    /**
     * 1. 获取参数cid
       2. 根据cid获取分类Category对象 c
       3. 为c填充产品
       4. 为产品填充销量和评价数据
       5. 获取参数sort
         5.1 如果sort==null，即不排序
         5.2 如果sort!=null，则根据sort的值，从5个Comparator比较器中选择一个对应的排序器进行排序
       6. 把c放在model中
       7. 服务端跳转到 category.jsp
     */
    @RequestMapping("forecategory")
    public String category(int cid,String sort, Model model) {
        Category c = categoryService.get(cid);
        productService.fill(c);
        productService.setSaleAndReviewNumber(c.getProducts());

        if(null!=sort){
            switch(sort){
                case "review":
                    Collections.sort(c.getProducts(),new ProductReviewComparator());
                    break;
                case "date" :
                    Collections.sort(c.getProducts(),new ProductDateComparator());
                    break;

                case "saleCount" :
                    Collections.sort(c.getProducts(),new ProductSaleCountComparator());
                    break;

                case "price":
                    Collections.sort(c.getProducts(),new ProductPriceComparator());
                    break;

                case "all":
                    Collections.sort(c.getProducts(),new ProductAllComparator());
                    break;
            }
        }

        model.addAttribute("c", c);
        return "fore/category";
    }

    /**
     * 通过search.jsp或者simpleSearch.jsp提交数据到路径 /foresearch， 导致ForeController.search()方法被调用
     1. 获取参数keyword
     2. 根据keyword进行模糊查询，获取满足条件的前20个产品
     3. 为这些产品设置销量和评价数量
     4. 把产品结合设置在model的"ps"属性上
     5. 服务端跳转到 searchResult.jsp 页面
     */
    @RequestMapping("foresearch")
    public String search( String keyword,Model model){

        PageHelper.offsetPage(0,20);
        List<Product> ps= productService.search(keyword);
        productService.setSaleAndReviewNumber(ps);
        model.addAttribute("ps",ps);
        return "fore/searchResult";
    }

    /**
     * 通过访问的地址 /forebuyone 导致ForeController.buyone()方法被调用
     1. 获取参数pid
     2. 获取参数num
     3. 根据pid获取产品对象p
     4. 从session中获取用户对象user

     接下来就是新增订单项OrderItem， 新增订单项要考虑两个情况
     a. 如果已经存在这个产品对应的OrderItem，并且还没有生成订单，即还在购物车中。 那么就应该在对应的OrderItem基础上，调整数量
        a.1 基于用户对象user，查询没有生成订单的订单项集合
        a.2 遍历这个集合
        a.3 如果产品是一样的话，就进行数量追加
        a.4 获取这个订单项的 id

     b. 如果不存在对应的OrderItem,那么就新增一个订单项OrderItem
        b.1 生成新的订单项
        b.2 设置数量，用户和产品
        b.3 插入到数据库
        b.4 获取这个订单项的 id

     最后， 基于这个订单项id客户端跳转到结算页面/forebuy
     */
    @RequestMapping("forebuyone")
    public String buyone(int pid, int num, HttpSession session) {
        Product p = productService.get(pid);
        int oiid = 0;

        User user =(User)  session.getAttribute("user");
        boolean found = false;
        List<OrderItem> ois = orderItemService.listByUser(user.getId());
        for (OrderItem oi : ois) {
            if(oi.getProduct().getId().intValue()==p.getId().intValue()){
                oi.setNumber(oi.getNumber()+num);
                orderItemService.update(oi);
                found = true;
                oiid = oi.getId();
                break;
            }
        }

        if(!found){
            OrderItem oi = new OrderItem();
            oi.setUid(user.getId());
            oi.setNumber(num);
            oi.setPid(pid);
            orderItemService.add(oi);
            oiid = oi.getId();
        }
        return "redirect:forebuy?oiid="+oiid;
    }

    /**
     * 1. 通过字符串数组获取参数oiid
         为什么这里要用字符串数组试图获取多个oiid，而不是int类型仅仅获取一个oiid? 因为根据购物流程环节与表关系，结算页面还需要显示在购物车中选中的多条OrderItem数据，所以为了兼容从购物车页面跳转过来的需求，要用字符串数组获取多个oiid
       2. 准备一个泛型是OrderItem的集合ois
       3. 根据前面步骤获取的oiids，从数据库中取出OrderItem对象，并放入ois集合中
       4. 累计这些ois的价格总数，赋值在total上
       5. 把订单项集合放在session的属性 "ois" 上
       6. 把总价格放在 model的属性 "total" 上
       7. 服务端跳转到buy.jsp
     */
    @RequestMapping("forebuy")
    public String buy( Model model,String[] oiid,HttpSession session){
        List<OrderItem> ois = new ArrayList<>();
        float total = 0;

        for (String strid : oiid) {
            int id = Integer.parseInt(strid);
            OrderItem oi= orderItemService.get(id);
            total +=oi.getProduct().getPromotePrice()*oi.getNumber();
            ois.add(oi);
        }

        session.setAttribute("ois", ois);
        model.addAttribute("total", total);
        return "fore/buy";
    }


    /**
     * 访问地址 /foreaddCart 导致 ForeController.addCart()方法被调用
     addCart()方法和立即购买中的 ForeController.buyone()步骤做的事情是一样的，区别在于返回不一样
     1. 获取参数pid
     2. 获取参数num
     3. 根据pid获取产品对象p
     4. 从session中获取用户对象user

     接下来就是新增订单项OrderItem， 新增订单项要考虑两个情况
     a. 如果已经存在这个产品对应的OrderItem，并且还没有生成订单，即还在购物车中。 那么就应该在对应的OrderItem基础上，调整数量
        a.1 基于用户对象user，查询没有生成订单的订单项集合
        a.2 遍历这个集合
        a.3 如果产品是一样的话，就进行数量追加
        a.4 获取这个订单项的 id

     b. 如果不存在对应的OrderItem,那么就新增一个订单项OrderItem
        b.1 生成新的订单项
        b.2 设置数量，用户和产品
        b.3 插入到数据库
        b.4 获取这个订单项的 id

      与ForeController.buyone() 客户端跳转到结算页面不同的是， 最后返回字符串"success"，表示添加成功
     */
    @RequestMapping("foreaddCart")
    @ResponseBody
    public String addCart(int pid, int num, Model model,HttpSession session) {
        Product p = productService.get(pid);
        User user =(User)  session.getAttribute("user");
        boolean found = false;

        List<OrderItem> ois = orderItemService.listByUser(user.getId());
        for (OrderItem oi : ois) {
            if(oi.getProduct().getId().intValue()==p.getId().intValue()){
                oi.setNumber(oi.getNumber()+num);
                orderItemService.update(oi);
                found = true;
                break;
            }
        }

        if(!found){
            OrderItem oi = new OrderItem();
            oi.setUid(user.getId());
            oi.setNumber(num);
            oi.setPid(pid);
            orderItemService.add(oi);
        }
        return "success";
    }

    /**
     *访问地址/forecart导致ForeController.cart()方法被调用
     1. 通过session获取当前用户
         所以一定要登录才访问，否则拿不到用户对象,会报错
     2. 获取为这个用户关联的订单项集合 ois
     3. 把ois放在model中
     4. 服务端跳转到cart.jsp
     */
    @RequestMapping("forecart")
    public String cart( Model model,HttpSession session) {
        User user =(User)  session.getAttribute("user");
        List<OrderItem> ois = orderItemService.listByUser(user.getId());
        model.addAttribute("ois", ois);
        return "fore/cart";
    }

    /**
     * 调整订单数量
     *
     * 点击增加或者减少按钮后，根据 cartPage.jsp 中的js代码，会通过Ajax访问/forechangeOrderItem路径，导致ForeController.changeOrderItem()方法被调用
     1. 判断用户是否登录
     2. 获取pid和number
     3. 遍历出用户当前所有的未生成订单的OrderItem
     4. 根据pid找到匹配的OrderItem，并修改数量后更新到数据库
     5. 返回字符串"success"
     */
    @RequestMapping("forechangeOrderItem")
    @ResponseBody
    public String changeOrderItem( Model model,HttpSession session, int pid, int number) {
        User user =(User)  session.getAttribute("user");
        if(null==user)
            return "fail";

        List<OrderItem> ois = orderItemService.listByUser(user.getId());
        for (OrderItem oi : ois) {
            if(oi.getProduct().getId().intValue()==pid){
                oi.setNumber(number);
                orderItemService.update(oi);
                break;
            }

        }
        return "success";
    }

    /**
     * 删除订单项
     *
     *     点击删除按钮后，根据 cartPage.jsp 中的js代码，会通过Ajax访问/foredeleteOrderItem路径，导致ForeController.deleteOrderItem方法被调用
     1. 判断用户是否登录
     2. 获取oiid
     3. 删除oiid对应的OrderItem数据
     4. 返回字符串"success"
     */
    @RequestMapping("foredeleteOrderItem")
    @ResponseBody
    public String deleteOrderItem( Model model,HttpSession session,int oiid){
        User user =(User)  session.getAttribute("user");
        if(null==user)
            return "fail";
        orderItemService.delete(oiid);
        return "success";
    }

    /**
     * 提交订单访问路径 /forecreateOrder, 导致ForeController.createOrder 方法被调用
     1. 从session中获取user对象
     2. 通过参数Order接受地址，邮编，收货人，用户留言等信息
     3. 根据当前时间加上一个4位随机数生成订单号
     4. 根据上述参数，创建订单对象
     5. 把订单状态设置为等待支付
     6. 从session中获取订单项集合 ( 在结算功能的ForeController.buy() 13行，订单项集合被放到了session中 )
     7. 把订单加入到数据库，并且遍历订单项集合，设置每个订单项的order，更新到数据库
     8. 统计本次订单的总金额
     9. 客户端跳转到确认支付页forealipay，并带上订单id和总金额
     */
    @RequestMapping("forecreateOrder")
    public String createOrder(Model model, Order order, HttpSession session){
        User user =(User)  session.getAttribute("user");
        String orderCode = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()) + RandomUtils.nextInt(10000);
        order.setOrderCode(orderCode);
        order.setCreateDate(new Date());
        order.setUid(user.getId());
        order.setStatus(OrderService.waitPay);
        List<OrderItem> ois= (List<OrderItem>)  session.getAttribute("ois");

        float total =orderService.add(order,ois);
        return "redirect:forealipay?oid="+order.getId() +"&total="+total;
    }

    /**
     * 1. 获取参数oid
       2. 根据oid获取到订单对象order
       3. 修改订单对象的状态和支付时间
       4. 更新这个订单对象到数据库
       5. 把这个订单对象放在model的属性"o"上
       6. 服务端跳转到payed.jsp
     */
    @RequestMapping("forepayed")
    public String payed(int oid, float total, Model model) {
        Order order = orderService.get(oid);
        order.setStatus(OrderService.waitDelivery);
        order.setPayDate(new Date());
        orderService.update(order);
        model.addAttribute("o", order);
        return "fore/payed";
    }

    /**
     *1. 通过session获取用户user
      2. 查询user所有的状态不是"delete" 的订单集合os
      3. 为这些订单填充订单项
      4. 把os放在model的属性"os"上
      5. 服务端跳转到bought.jsp
     */
    @RequestMapping("forebought")
    public String bought( Model model,HttpSession session) {
        User user =(User)  session.getAttribute("user");
        List<Order> os= orderService.list(user.getId(),OrderService.delete);

        orderItemService.fill(os);

        model.addAttribute("os", os);

        return "fore/bought";
    }

    /**
     *1. 获取参数oid
      2. 通过oid获取订单对象o
      3. 为订单对象填充订单项
      4. 把订单对象放在request的属性"o"上
      5. 服务端跳转到 confirmPay.jsp
     */
    @RequestMapping("foreconfirmPay")
    public String confirmPay( Model model,int oid) {
        Order o = orderService.get(oid);
        orderItemService.fill(o);
        model.addAttribute("o", o);
        return "fore/confirmPay";
    }

    /**
     * 确认收货成功
     1. 获取参数oid
     2. 根据参数oid获取Order对象o
     3. 修改对象o的状态为等待评价，修改其确认支付时间
     4. 更新到数据库
     5. 服务端跳转到orderConfirmed.jsp页面
     */
    @RequestMapping("foreorderConfirmed")
    public String orderConfirmed( Model model,int oid) {
        Order o = orderService.get(oid);
        o.setStatus(OrderService.waitReview);
        o.setConfirmDate(new Date());
        orderService.update(o);
        return "fore/orderConfirmed";
    }

    /**
     * 1. 获取参数oid
       2. 根据oid获取订单对象o
       3. 修改状态
       4. 更新到数据库
       5. 返回字符串"success"
     */
    @RequestMapping("foredeleteOrder")
    @ResponseBody
    public String deleteOrder( Model model,int oid){
        Order o = orderService.get(oid);
        o.setStatus(OrderService.delete);
        orderService.update(o);
        return "success";
    }

    /**
     * 1. 获取参数oid
       2. 根据oid获取订单对象o
       3. 为订单对象填充订单项
       4. 获取第一个订单项对应的产品,因为在评价页面需要显示一个产品图片，那么就使用这第一个产品的图片了
       5. 获取这个产品的评价集合
       6. 为产品设置评价数量和销量
       7. 把产品，订单和评价集合放在request上
       8. 服务端跳转到 review.jsp
     */
    @RequestMapping("forereview")
    public String review( Model model,int oid) {
        Order o = orderService.get(oid);
        orderItemService.fill(o);
        Product p = o.getOrderItems().get(0).getProduct();
        List<Review> reviews = reviewService.list(p.getId());
        productService.setSaleAndReviewNumber(p);
        model.addAttribute("p", p);
        model.addAttribute("o", o);
        model.addAttribute("reviews", reviews);
        return "fore/review";
    }

    /**
     * 在评价产品页面点击提交评价，就把数据提交到了/foredoreview路径，导致ForeController.doreview方法被调用
     1. 获取参数oid
     2. 根据oid获取订单对象o
     3. 修改订单对象状态
     4. 更新订单对象到数据库
     5. 获取参数pid
     6. 根据pid获取产品对象
     7. 获取参数content (评价信息)
     8. 对评价信息进行转义，道理同注册ForeController.register()
     9. 从session中获取当前用户
     10. 创建评价对象review
     11. 为评价对象review设置 评价信息，产品，时间，用户
     12. 增加到数据库
     13. 客户端跳转到/forereview： 评价产品页面，并带上参数showonly=true
     */
    @RequestMapping("foredoreview")
    public String doreview( Model model,HttpSession session,@RequestParam("oid") int oid,@RequestParam("pid") int pid,String content) {
        Order o = orderService.get(oid);
        o.setStatus(OrderService.finish);
        orderService.update(o);

        Product p = productService.get(pid);
        content = HtmlUtils.htmlEscape(content);

        User user =(User)  session.getAttribute("user");
        Review review = new Review();
        review.setContent(content);
        review.setPid(pid);
        review.setCreateDate(new Date());
        review.setUid(user.getId());
        reviewService.add(review);

        return "redirect:forereview?oid="+oid+"&showonly=true";
    }
}