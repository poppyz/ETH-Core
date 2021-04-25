package core.utils.crypto;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class crypto {
    public static byte[] Keccak256(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest keccak256 = MessageDigest.getInstance("SHA3-256");
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        return keccak256.digest(data);
    }

    public static byte[] aes(byte[] data) throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        kgen.init(128, new SecureRandom(password.getBytes()));
        SecretKey secretKey = kgen.generateKey();
        byte[] enCodeFormat = secretKey.getEncoded();
        SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");
        Cipher cipher = Cipher.getInstance("AES");// 创建密码器
        cipher.init(Cipher.ENCRYPT_MODE, key);// 初始化
        byte[] result = cipher.doFinal(data);
        return result; // 加密
    }
//    public static byte[] aesCbcEncrypt(byte[] data,byte[] key) throws NoSuchPaddingException, NoSuchAlgorithmException {
//        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
//        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
//        IvParameterSpec ips = new IvParameterSpec(IV.getBytes());
//
//    }

}


//
//
//    }
//
//        public static String Encrypt(String content) throws Exception {
//        byte[] raw = KEY.getBytes("utf-8");
//        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
//        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");//"算法/模式/补码方式"
//        //使用CBC模式，需要一个向量iv，可增加加密算法的强度
//        IvParameterSpec ips = new IvParameterSpec(IV.getBytes());
//        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ips);
//        byte[] encrypted = cipher.doFinal(content.getBytes());
//        return new BASE64Encoder().encode(encrypted);
//    }
//
//        //解密
//        public static String Decrypt(String content) throws Exception {
//        try {
//            byte[] raw = KEY.getBytes("utf-8");
//            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
//            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
//            IvParameterSpec ips = new IvParameterSpec(IV.getBytes());
//            cipher.init(Cipher.DECRYPT_MODE, skeySpec, ips);
//            byte[] encrypted1 = new BASE64Decoder().decodeBuffer(content);
//            try {
//                byte[] original = cipher.doFinal(encrypted1);
//                String originalString = new String(original);
//                return originalString;
//            } catch (Exception e) {
//                System.out.println(e.toString());
//                return null;
//            }
//        } catch (Exception ex) {
//            System.out.println(ex.toString());
//            return null;
//        }
//
//————————————————
//        版权声明：本文为CSDN博主「王绍桦」的原创文章，遵循CC 4.0 BY-SA版权协议，转载请附上原文出处链接及本声明。
//        原文链接：https://blog.csdn.net/rexueqingchun/article/details/86606376
//    }
//}
