package com.gykj.mvvmlibrary.utils;

/**
 * author ：wiggins on 2017/7/19 17:25
 * e-mail ：traywangjun@gmail.com
 * desc :关于加解密JAVA一般使用的JCE，关于C++可以实现AES加解密的开源项目就多的数不胜数的。
 * 理论上上算法一样，对称密钥一样就能够互相识别了。
 * 相信很多人开始想法都同我一样，起初我JAVA用JCE，C++使用openssl。
 * 结果发现加密出的密文完全不相同。
 * 出现问题就要解决
 * 了解了一下JCE：
 * JCE中AES支持五中模式：CBC，CFB，ECB，OFB，PCBC；支持三种填充：NoPadding，PKCS5Padding，ISO10126Padding。
 * 不支持SSL3Padding。不支持“NONE”模式。
 * 好原来有模式和填充一说。
 * 在OPENSSL中直接有一个CBC加解密函数。填充没找到，试过后发现C++加密出的内容比JAVA的要长出一截，前面的内容是完全一样的。
 * 这应该就出现在填充上。
 * 本来以为找到问题关键就应该很容易解决的。结果发现OPENSSL的填充是固定实现的，而我所需要解密的java端代码不能改动。
 * 一条路走不通咱就换条路。最后发现有一个开源项目Botan什么都有而且同JCE十分相似并且满足我要求垮平台，好就是它了。
 * 附：
 * 算法/模式/填充 16字节加密后数据长度 不满16字节加密后长度
 * AES/CBC/NoPadding 16 不支持
 * AES/CBC/PKCS5Padding 32 16
 * AES/CBC/ISO10126Padding 32 16
 * AES/CFB/NoPadding 16 原始数据长度
 * AES/CFB/PKCS5Padding 32 16
 * AES/CFB/ISO10126Padding 32 16
 * AES/ECB/NoPadding 16 不支持
 * AES/ECB/PKCS5Padding 32 16
 * AES/ECB/ISO10126Padding 32 16
 * AES/OFB/NoPadding 16 原始数据长度
 * AES/OFB/PKCS5Padding 32 16
 * AES/OFB/ISO10126Padding 32 16
 * AES/PCBC/NoPadding 16 不支持
 * AES/PCBC/PKCS5Padding 32 16
 * AES/PCBC/ISO10126Padding 32 16
 * 可以看到，在原始数据长度为16的整数倍时，
 * 假如原始数据长度等于16*n，
 * 则使用NoPadding时加密后数据长度等于16*n
 * ，其它情况下加密数据长度等于16*(n+1)。
 * 在不足16的整数倍的情况下，假如原始数据长度等于16*n+m[其中m小于16]
 * ，除了NoPadding填充之外的任何方式，加密数据长度都等于16*(n+1)
 * ；NoPadding填充情况下，CBC、ECB和PCBC三种模式是不支持的，CFB、OFB两种模式下则加密数据长度等于原始数据长度。
 * version :1.0
 */

import android.util.Base64;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESHelper {
    private static final String CipherMode = "AES/CBC/PKCS5Padding";
    private static final String password = "pigxpigxpigxpigx";
    public static String algorithm ="AES";
    /**
     * 创建密钥
     *
     * @param password 例如："0123456701234567" 128位 16*8 所有密钥长度不能超过16字符中文占两个。192 24；
     *                 256 32
     * @return SecretKeySpec 实例
     */
    private static SecretKeySpec createKey(String password) {
        byte[] data = null;
        if (password == null) {
            password = "";
        }
        StringBuffer sb = new StringBuffer(16);
        sb.append(password);
        while (sb.length() < 16) {
            sb.append("0");
        }
        if (sb.length() > 16) {
            sb.setLength(16);
        }
        try {
            data = sb.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return new SecretKeySpec(data, "AES");
    }
    /**
     * 加密字节数据
     *
     * @param content  需要加密的字节数组
     * @param password 密钥 128 <16个字节 192 <24,256 <32个字节
     * @return 加密完后的字节数组
     */
    public static byte[] encrypt(byte[] content, String password) {
        try {
            SecretKeySpec key = createKey(password);
            Cipher cipher = Cipher.getInstance(CipherMode);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] result = cipher.doFinal(content);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * 加密(结果为16进制字符串)
     *
     * @param content  要加密的字符串
     * @param password 密钥
     * @return 加密后的16进制字符串
     */
    public static String encrypt(String content, String password) {
        byte[] data = null;
        try {
            data = content.getBytes("UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        data = encrypt(data, password);
        String result = Base64.encodeToString(data, Base64.NO_WRAP);
        return result;
    }

    /**
     * 加密(结果为16进制字符串)
     *
     * @param content 要加密的字符串
     * @return 加密后的16进制字符串
     */
    public static String enctrypt(String content,String password){
        SecretKey key = getKey(algorithm,password);
        byte [] enCodeFormat = key.getEncoded();
        SecretKeySpec ss = new SecretKeySpec(password.getBytes(), algorithm);
        try {
            IvParameterSpec iv = new IvParameterSpec(password.getBytes("UTF-8"));
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, ss,iv);
//            cipher.init(Cipher.ENCRYPT_MODE, ss);
            byte b [] = cipher.doFinal(content.getBytes());
            String result = Base64.encodeToString(b, Base64.NO_PADDING);
            return  result;
            //return byteToHex(b);
        } catch (Exception e) {
            Log.e("AES 加密失败",e.getMessage());
        }

        return null;
    }
    /**
     * 使用keygenerator 依赖平台，导致Android和java加密出来的密文不一致
     *
     * @param algorithm
     * @param password
     * @return
     */
    public static SecretKey getKey(String algorithm,String password){
        try {
            KeyGenerator gen = KeyGenerator.getInstance(algorithm);
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            secureRandom.setSeed(password.getBytes());

            gen.init(128, secureRandom);
//            gen.init(128);
            return gen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            Log.e("getKey 获取加密密钥失败 ",e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public static String decrypt1(byte [] content,String password){
//        SecretKey key = getKey(algorithm,password);
//        byte [] enCodeFormat = key.getEncoded();
        SecretKeySpec ss = new SecretKeySpec(password.getBytes(), algorithm);
        try {
            IvParameterSpec iv = new IvParameterSpec(password.getBytes("UTF-8"));
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, ss,iv);
//            cipher.init(Cipher.DECRYPT_MODE, ss);
            byte b [] = cipher.doFinal(content);
            return new String(b);
        } catch (Exception e) {
            Log.e("AES 解密失败",e.getMessage());
        }

        return null;
    }

    /**
     * 解密字节数组
     **/
    public static byte[] decrypt(byte[] content, String password) {
        try {
            SecretKeySpec key = createKey(password);
            Cipher cipher = Cipher.getInstance(CipherMode);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] result = cipher.doFinal(content);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 解密16进制的字符串为字符串
     **/
    public static String decrypt(String content, String password) {
        byte[] data = null;
        try {
            data = Base64.decode(content, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String result = decrypt1(data, password);
        if (data == null)
        {
            return null;
        }
        if(result == null)
        {
            return null;
        }

        return result;
    }

    /**
     * 解密16进制的字符串为字符串
     **/
    public static String decrypt(String content) {
        byte[] data = null;
        try {
            data = Base64.decode(content, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
        }
        data = decrypt(data, password);
        if (data == null)
            return null;
        String result = null;
        try {
            result = new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 字节数组转成16进制字符串
     * @param b
     * @return 16进制字符串
     */
    public static String byte2hex(byte[] b) { // 一个字节的数，
        StringBuffer sb = new StringBuffer(b.length * 2);
        String tmp = "";
        for (int n = 0; n < b.length; n++) {
            // 整数转成十六进制表示
            tmp = (Integer.toHexString(b[n] & 0XFF));
            if (tmp.length() == 1) {
                sb.append("0");
            }
            sb.append(tmp);
        }
        return sb.toString().toUpperCase(); // 转成大写
    }

    /**
     * 将hex字符串转换成字节数组 *
     *
     * @return 字节数组
     */
    public static byte[] hexStrToByte(String hexStr) {
        if (hexStr == null || hexStr.equals("")) {
            return null;
        }
        hexStr = hexStr.toUpperCase();
        int length = hexStr.length() / 2;
        char[] hexChars = hexStr.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }
    private static byte charToByte(char c) {
        int index = "0123456789ABCDEF".indexOf(c);
        if(index==-1){
            index = "0123456789abcdef".indexOf(c);
        }
        return (byte)index;
    }

    public static byte[] byteToHex(String hexStr){
        if (hexStr == null || hexStr.equals("")) {
            return null;
        }

        int length = hexStr.length();
        byte[] hexChars = hexStr.getBytes();
        byte[] d = new byte[16];
        for (int i = 0; i < length; i++) {
                d[i]=hexChars[i];
        }
        return d;
    }


}
