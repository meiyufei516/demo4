package com.example.pay.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import java.security.Key;
import java.util.Base64;

public class ThreeDESUtil {
    private static String sKey = "qJzGEh6hESZDVJeCnFPGuxuaiB7NLQM5"; //密匙
    private static String sIv = "Kl7ZgtM1dvQ=";                      //向量

    private static byte[] key = Base64.getDecoder().decode(sKey);
    private static byte[] iv  = Base64.getDecoder().decode(sIv);
    //加密
    public static String encode(String value){
        try {
            return Base64.getEncoder().encodeToString(T3encode(key, value.getBytes("UTF-8")));
        } catch (Exception e) {
            return value;//加密失败，返回原文
        }
    }

    //解密
    public static String decode(String value) {
        try{
            return new String(T3decode(key, Base64.getDecoder().decode(value)));
        }catch (Exception e) {
            return "";//解密失败，返回空
        }
    }
    /**
     * ECB加密,不要IV
     * @param key 密钥
     * @param data 明文
     * @return Base64编码的密文
     * @throws Exception
     */
    public static byte[] T3encode(byte[] key, byte[] data)
            throws Exception {
        Key deskey = null;
        DESedeKeySpec spec = new DESedeKeySpec(key);
        SecretKeyFactory keyfactory = SecretKeyFactory.getInstance("desede");
        deskey = keyfactory.generateSecret(spec);
        Cipher cipher = Cipher.getInstance("desede" + "/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, deskey);
        byte[] bOut = cipher.doFinal(data);
        return bOut;
    }
    /**
     * ECB解密,不要IV
     * @param key 密钥
     * @param data Base64编码的密文
     * @return 明文
     * @throws Exception
     */
    public static byte[] T3decode(byte[] key, byte[] data)
            throws Exception {
        Key deskey = null;
        DESedeKeySpec spec = new DESedeKeySpec(key);
        SecretKeyFactory keyfactory = SecretKeyFactory.getInstance("desede");
        deskey = keyfactory.generateSecret(spec);
        Cipher cipher = Cipher.getInstance("desede" + "/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, deskey);
        byte[] bOut = cipher.doFinal(data);
        return bOut;
    }
}

