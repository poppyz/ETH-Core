package core;
import core.utils.Mongodb;
import core.utils.EthDriver;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.security.MessageDigest;

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
        Base test = new Base();
        test.ethDriver.getAddress(test.mongodb.getCollection("wallet"));
        test.threadPools.submit(()->
        {
            AtomicInteger flag = new AtomicInteger();
            test.eventStauts.put("randomSendTx",flag);
            test.ethDriver.randomSendTx(test.mongodb.getCollection("wallet"));
        });
//        MessageDigest sha3_256 = MessageDigest.getInstance("SHA3-256");
//        byte[] md5str = sha3_256.digest("sdf".getBytes(StandardCharsets.UTF_8));


    }
}

