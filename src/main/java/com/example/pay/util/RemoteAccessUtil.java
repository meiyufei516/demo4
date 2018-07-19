package com.example.pay.util;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.dynamic.DynamicClientFactory;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.util.ArrayUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Scanner;

/**
 * Created by developer_hyaci on 2016/3/16.
 */
public class RemoteAccessUtil {
    //当前远程访问类型，值选项来源于RemoteAccessType;
    private RemoteAccessType remoteType;
    //远程访问的地址;
    private String url;
    //当为WSDL时该值存在
    private Object[] params;
    //当为WSDL时其值需要访问的方法名
    private String methodName;
    //需传递的JSON串
    private String jsonParam;
    //WCF需添加的拦截器
    private AbstractPhaseInterceptor<?> inerceptor;
    //WCF传递参数数组
    private Object[] parmsList;

    private RemoteAccessUtil(){}

    /**
     * 访问以WSDL方式公布的方法时，初始化
     * @param url 访问的WSDL地址
     * @param method wsdl公布的方法名
     * @param params 访问wsdl公布的方法需要的参数
     * */
    public static RemoteAccessUtil wsdlInit(String url,String method,Object ...params){
        RemoteAccessUtil remote=new RemoteAccessUtil();
        remote.remoteType= RemoteAccessType.WSDL;
        remote.url=url;
        remote.methodName=method;
        remote.params=params;
        return remote;
    }
    /**
     * 访问以WSDL方式公布的方法时，初始化
     * @param url 访问的REST地址
     * @param jsonParam 需传递的json参数
     * */
    public static RemoteAccessUtil restInit(String url,String jsonParam){
        RemoteAccessUtil remote=new RemoteAccessUtil();
        remote.remoteType= RemoteAccessType.REST;
        remote.url=url;
        remote.jsonParam=jsonParam;
        return remote;
    }

    /**
     * 访问以WCF方式公布的方法时，初始化
     * @param url 访问的WCF地址
     * @param method wcf公布的方法名
     * @param inerceptor wcf 拦截器
     * @param params 访问WCF公布的方法需要的参数
     * */
    public static RemoteAccessUtil wcfInit(String url,String method,AbstractPhaseInterceptor<?> inerceptor,Object ...params){
        RemoteAccessUtil remote=new RemoteAccessUtil();
        remote.remoteType= RemoteAccessType.WCF;
        remote.url=url;
        remote.methodName = method;
        remote.inerceptor = inerceptor;
        remote.parmsList=params;
        return remote;
    }
    /**
     * 当为空时为null
     * */
    public String recvBackStr() throws Exception{
        String result="";
        switch (remoteType){
            case WSDL:
                result = this.visitWsdl(this.url, this.methodName, this.params);
                break;
            case REST:
//                result = this.visitRestByJdk(this.url,this.jsonParam);
                //该方法在使用spring框架时优先推荐
                result = this.visitRestByRestTemplate(this.url,this.jsonParam);
                break;
            case WCF:
                result = this.visitWcf(this.url,this.methodName,this.inerceptor,this.parmsList);
                break;
            default:
                break;
        }
        if(result.equals("")){
            return null;
        }
        return result;
    }

    /**
     * 实现请求webApi,也可以用于url与参数明确的restFull访问
     * 但对于resetfull的访问通常采用 本类中visitRestByRestTemplate方法
     * 请求方式post
     * 传递参数为json
     * */
    public static String visitWebApi(String fullUrl,String jsonParam){
        String result="";
        try {
//            new URL("http://localhost:8080/CXFRestfulTutorial/rest/changeName")
            URL url = new URL(fullUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

//            String input = "{\"Student\":{\"name\":\"Tom\"}}";

            OutputStream os = conn.getOutputStream();
//            os.write(input.getBytes());
            os.write(jsonParam.getBytes());
            os.flush();

            Scanner scanner;
            if (conn.getResponseCode() != 200) {
                scanner = new Scanner(conn.getErrorStream());
            } else {
                scanner = new Scanner(conn.getInputStream());
            }
            scanner.useDelimiter("\\Z");
            result=scanner.next();
            scanner.close();
            conn.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 构建一个在Spring frameWork中才能存在的restTemplate对象
     * 该对象已经很成熟，其也提供其他各类方法
     * 参见visitRestByRestTemplate方法实现
     * */
    public static RestTemplate buildRestTemp(){
        List<HttpMessageConverter<?>> msgConvers=new RestTemplate().getMessageConverters();
        msgConvers.add(1,new StringHttpMessageConverter(Charset.forName("UTF-8")));
        msgConvers.remove(2);
        return new RestTemplate(msgConvers);
    }

    /**
     * restTemplate对象进行访问URL是必不可少的参数
     * */
    public static HttpEntity<String> buildHttpEntity(MediaType mediaType, String reqStr){

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        return  new HttpEntity<String>(reqStr, headers);
    }

    private String visitRestByRestTemplate(String url,String jsonParam) throws Exception {
        ResponseEntity<String> response=buildRestTemp().postForEntity(url,
                    buildHttpEntity(MediaType.APPLICATION_JSON,jsonParam),
                    String.class);
        if(response!=null && response.getStatusCode() == HttpStatus.OK){
            return response.getBody();
        }
        return "";
    }

    private String visitWsdl(String wsdlUrl,String methodName,Object ... params) throws Exception{
        DynamicClientFactory dcf = DynamicClientFactory.newInstance();
        Client client = dcf.createClient(wsdlUrl);
        Object[] reply = client.invoke(methodName,params);
        if (ArrayUtils.isEmpty(reply))
            return null;
        return reply[0].toString();
    }

    /**
     * 实现请求wcf
     * 请求方式post
     * @param wcfUrl 访问的WCF地址
     * @param method wcf公布的方法名
     * @param inerceptor wcf 拦截器
     * @param params 访问WCF公布的方法需要的参数
     * */
    public String visitWcf(String wcfUrl, String method, AbstractPhaseInterceptor<?> inerceptor, Object ... params) throws Exception {
        JaxWsDynamicClientFactory dcf = JaxWsDynamicClientFactory.newInstance();
        Client client = dcf.createClient(wcfUrl);
        client.getOutInterceptors().add(inerceptor);
        client.getOutInterceptors().add(new LoggingOutInterceptor());
        client.getInInterceptors().add(new LoggingInInterceptor());

        Object[] objects = client.invoke(method, params);
        if (ArrayUtils.isEmpty(objects))
            return null;
        return objects[0].toString();
    }

    public String threeDESUtilRequest() throws Exception {
        String result="";
        switch (remoteType){
            case WCF:
                result = this.visitWcf(this.url,this.methodName,this.inerceptor,this.parmsList);
                break;
            default:
                break;
        }
        return ThreeDESUtil.decode(result);
    }

    
	//限定本系统能够处理的服务
	enum  RemoteAccessType{
		WSDL("WSDL",1),
		REST("REST",2),
		SOCKET("SOCKET",3),
		WCF("WCF",4);

		private String code;
		private int index;

		RemoteAccessType(String code,int index){
			this.code=code;
			this.index=index;
		}

		public String getCode() {return code;}
		public void setCode(String code) {this.code = code;}

		public int getIndex() {return index;}
		public void setIndex(int index) {this.index = index;}
	}
}
