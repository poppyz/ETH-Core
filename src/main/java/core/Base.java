package core;
import core.utils.Mongodb;
import core.utils.EthDriver;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.web3j.crypto.ECKeyPair;
import org.web3j.utils.Numeric;
import org.web3j.utils.Strings;

public class Base {
    private final Logger log;
    private final Mongodb mongodb;
    private final EthDriver ethDriver;
    private final ExecutorService threadPools;
    private final HashMap<String, AtomicInteger> eventStauts;       //每次塞入线程池的函数 的状态。 正整数代表 主程序对线程池内部函数的请求。负整数 代表线程池内部返回的信息


    public Base() throws Exception {
        log = LoggerFactory.getLogger(Base.class);
        String result = new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("config")))
                .lines().collect(Collectors.joining(System.lineSeparator()));
        JSONObject config = new JSONObject(result);
        System.out.println(result);
        JSONObject ethConfig = config.getJSONObject("ETH");
        ethDriver = new EthDriver(ethConfig);

        mongodb = new Mongodb(config.getJSONObject("mongodb"));


        threadPools = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 8);
        eventStauts = new HashMap<>();
    }

    static KeyPair createSecp256k1KeyPair(SecureRandom random) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "BC");
        ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec("secp256k1");
        if (random != null) {
            keyPairGenerator.initialize(ecGenParameterSpec, random);
        } else {
            keyPairGenerator.initialize(ecGenParameterSpec);
        }

        return keyPairGenerator.generateKeyPair();
    }
    public static void main(String[] args) throws Exception {
        System.out.println(createSecp256k1KeyPair(new SecureRandom()).getPrivate().getEncoded());
//        Base test = new Base();
//        test.ethDriver.getAddress(test.mongodb.getCollection("wallet"));
//        test.threadPools.submit(()->
//        {
//            AtomicInteger flag = new AtomicInteger();
//            test.eventStauts.put("randomSendTx",flag);
//            test.ethDriver.randomSendTx(test.mongodb.getCollection("wallet"));
//        });
////        MessageDigest sha3_256 = MessageDigest.getInstance("SHA3-256");
////        byte[] md5str = sha3_256.digest("sdf".getBytes(StandardCharsets.UTF_8));
//        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDSA");
//        ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec("secp256k1");
////        //e6dfe2ee25e9578a5b2e9b42e90f88d3491485236fd972bc356605e2daabe8e8
////        Hex.decode("e6dfe2ee25e9578a5b2e9b42e90f88d3491485236fd972bc356605e2daabe8e8");
////        byte[] decoded = Hex.decodeHex("00A0BF");
//
//        keyPairGenerator.initialize(ecGenParameterSpec, new SecureRandom());
//        KeyPair keyPair = keyPairGenerator.generateKeyPair();
//        System.out.println(keyPair.getPrivate().toString());

        BCECPrivateKey privateKey = (BCECPrivateKey)keyPair.getPrivate();
        BCECPublicKey publicKey = (BCECPublicKey)keyPair.getPublic();
        BigInteger privateKeyValue = privateKey.getD();
        byte[] publicKeyBytes = publicKey.getQ().getEncoded(false);
        BigInteger publicKeyValue = new BigInteger(1, Arrays.copyOfRange(publicKeyBytes, 1, publicKeyBytes.length));
        return new ECKeyPair(privateKeyValue, publicKeyValue);



    }
}

