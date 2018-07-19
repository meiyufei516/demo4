package com.example.pay.web;

import com.example.pay.config.Config;
import com.example.pay.domain.RequestParam;
import com.example.pay.domain.ResponseRaram;
import com.example.pay.util.RemoteAccessUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.XmlFriendlyNameCoder;
import com.thoughtworks.xstream.io.xml.XppDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

@Controller
public class test {

    @Autowired
    private Config config;

    @RequestMapping("/")
    public String test(){
        return "/index";
    }

    @RequestMapping("/payment")
    public String pay(@Param("money") String money, HttpServletRequest request, Model model){

    //封装对象
        RequestParam requestParam=new RequestParam();
        requestParam.setAppid(config.getAppid());//公众号ID
        requestParam.setMch_id(config.getMch_id());//商户号
        requestParam.setDevice_info("web");        //设备号
        requestParam.setNonce_str(UUID.randomUUID().toString());//随机字符串
        requestParam.setSign("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");//签名
        requestParam.setBody(config.getBody());//商品描述
        requestParam.setOut_trade_no("bbbbbbbbbbbbbbbbbbbbbbbbbbbb");//商户订单号
        requestParam.setTotal_fee(money);//标价金额
        requestParam.setSpbill_create_ip(getIPAddress(request));//终端IP
        requestParam.setTrade_type(config.getTrade_type());//交易类型
        requestParam.setProduct_id("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");//商品ID

    //转化为xml
        XStream xStream = new XStream(new XppDriver(new XmlFriendlyNameCoder("_-", "_"))); //
        xStream.alias("xml", RequestParam.class);//根元素名需要是xml
    //调用方法访问接口
        String Code_url=httpOrder(xStream.toXML(requestParam),config.getPayUrl());
        System.out.println("返回的二维码地址是："+Code_url);
        model.addAttribute("Out_trade_no","bbbbbbbbbbbbbbbbbbbbbbbbbbbb");
        model.addAttribute("Trade_type",config.getTrade_type());
        model.addAttribute("Total_fee",money);
        model.addAttribute("Code_url","https://www.baidu.com/");
         return "/payPage";
    }

    /**
     * 调统一下单API
     * @param orderInfo
     * @return
     */
    private static  String httpOrder(String orderInfo,String url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        //加入数据
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            BufferedOutputStream buffOutStr = new BufferedOutputStream(conn.getOutputStream());
            buffOutStr.write(orderInfo.getBytes());
            buffOutStr.flush();
            buffOutStr.close();
        //获取输入流
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = null;
            StringBuffer sb = new StringBuffer();
            while((line = reader.readLine())!= null){
                sb.append(line);
            }
            XStream xStream = new XStream(new XppDriver(new XmlFriendlyNameCoder("_-", "_")));

        //将请求返回的内容通过xStream转换为responseRaram对象
            xStream.alias("xml", ResponseRaram.class);
            System.out.println("返回的xml是："+xStream.fromXML(sb.toString()));
            ResponseRaram responseRaram = (ResponseRaram) xStream.fromXML(sb.toString());
            //根据微信文档return_code 和result_code都为SUCCESS的时候才会返回code_url
            if(null!=responseRaram&& "SUCCESS".equals(responseRaram.getReturn_code())&& "SUCCESS".equals(responseRaram.getResult_code())){
                return responseRaram.getCode_url();
            }else{
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取ip地址
     * @param request
     * @return
     */
    public static String getIPAddress(HttpServletRequest request) {
        String ip = null;

        //X-Forwarded-For：Squid 服务代理
        String ipAddresses = request.getHeader("X-Forwarded-For");
        if (ipAddresses == null || ipAddresses.length() == 0 || "unknown".equalsIgnoreCase(ipAddresses)) {
            //Proxy-Client-IP：apache 服务代理
            ipAddresses = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddresses == null || ipAddresses.length() == 0 || "unknown".equalsIgnoreCase(ipAddresses)) {
            //WL-Proxy-Client-IP：weblogic 服务代理
            ipAddresses = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddresses == null || ipAddresses.length() == 0 || "unknown".equalsIgnoreCase(ipAddresses)) {
            //HTTP_CLIENT_IP：有些代理服务器
            ipAddresses = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ipAddresses == null || ipAddresses.length() == 0 || "unknown".equalsIgnoreCase(ipAddresses)) {
            //X-Real-IP：nginx服务代理
            ipAddresses = request.getHeader("X-Real-IP");
        }

        //有些网络通过多层代理，那么获取到的ip就会有多个，一般都是通过逗号（,）分割开来，并且第一个ip为客户端的真实IP
        if (ipAddresses != null && ipAddresses.length() != 0) {
            ip = ipAddresses.split(",")[0];
        }

        //还是不能获取到，最后再通过request.getRemoteAddr();获取
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ipAddresses)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
