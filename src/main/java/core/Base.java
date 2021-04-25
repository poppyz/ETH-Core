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


    public static void main(String[] args) throws Exception {
    }
}

