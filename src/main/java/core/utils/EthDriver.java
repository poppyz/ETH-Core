package core.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import org.bson.Document;
import org.json.JSONObject;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.ipc.UnixIpcService;
import org.web3j.protocol.ipc.WindowsIpcService;
import org.web3j.protocol.websocket.WebSocketService;
import org.web3j.utils.Async;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;


public class EthDriver {
    public static final int DEFAULT_BLOCK_TIME = 15 * 1000;
    private Web3j web3j;
    private Logger log;
    private HashMap<String, Subscription> subscriptionHashMap;
    private List<Transaction> transactionsPendings;
    private ArrayList<String> address;
    private Credentials rich;

    public EthDriver(JSONObject ethConfig) throws IOException, CipherException {
        web3j = null;
        String path = ethConfig.getString("path");
        switch (ethConfig.getInt("linktype")) {
            case 0: {
                //通过 HTTP 连接 GETH
                log.info("link type HTTP path: " + path);
                web3j = Web3j.build(new HttpService(path), DEFAULT_BLOCK_TIME, Async.defaultExecutorService());
                break;
            }
            case 1: {
                //通过 webSocket 连接 GETH
                log.info("link type webSocket path: " + path);
                WebSocketService ws = new WebSocketService(path, false);
                ws.connect();
                web3j = Web3j.build(ws, DEFAULT_BLOCK_TIME, Async.defaultExecutorService());
                log.info("Connected to Ethereum client version: "
                        + web3j.web3ClientVersion().send().getWeb3ClientVersion());
                break;
            }

            case 2: {
                //通过 IPC 连接 GETH
                String osName = System.getProperty("os.name");
                log.info("link type IPC on : " + osName + "; path: " + ethConfig.getString("path"));
                System.out.println(osName);
                if (osName.startsWith("Windows")) {
                    web3j = Web3j.build(new WindowsIpcService(path), DEFAULT_BLOCK_TIME, Async.defaultExecutorService());
                } else {
                    web3j = Web3j.build(new UnixIpcService(path), DEFAULT_BLOCK_TIME, Async.defaultExecutorService());
                }
                break;
            }
            default: {
                log.error("Unknow link type");
                return;
            }
        }

        ObjectMapper objectMapper = new ObjectMapper();
        WalletFile walletFile = objectMapper.readValue(ethConfig.getJSONObject("Rich").getJSONObject("wallet").toString(), WalletFile.class);
        rich = Credentials.create(Wallet.decrypt(ethConfig.getJSONObject("Rich").getString("pwd"), walletFile));
    }

    // 从Mongodb中获取预设的2,000,000个地址
    public void getAddress(MongoCollection<Document> collection) {
        MongoCursor<Document> mongoCursor = collection.find().projection(Projections.excludeId()).batchSize(500).iterator();
        while (mongoCursor.hasNext()) {
            Document doc = mongoCursor.next();
            String addr = doc.getString("address");
            address.add(addr);
        }
        log.info(String.format("Get %d walletfile from Mongodb", address.size()));
    }

    //从block中获取交易数组
    public List<Transaction> getTransactionFromBlock(EthBlock.Block block) {
        List<Transaction> result = block.getTransactions().stream().map(tx -> ((EthBlock.TransactionObject) tx.get()).get()).collect(Collectors.toList());
        return result;
    }

    //从节点按照区块高度获取区块
    public EthBlock.Block getBlockByNumber(long number) throws IOException {
        return web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(BigInteger.valueOf(number)), true).send().getBlock();
    }

    //从节点中按照地址获取该地址的余额
    public BigInteger get_balance(String address) throws IOException {
        EthGetBalance ethGetBalance = web3j.ethGetBalance(address, DefaultBlockParameter.valueOf("latest")).send();
        return ethGetBalance.getBalance();
    }

    //从节点获取该地址的Nonce  仅为已打包的nonce + 1 并不包括未确认交易
    private BigInteger getNonce(String address) throws Exception {
        EthGetTransactionCount ethGetTransactionCount =
                web3j.ethGetTransactionCount(address, DefaultBlockParameter.valueOf("latest"))
                        .sendAsync()
                        .get();
        return ethGetTransactionCount.getTransactionCount();
    }

    //对RAW交易进行签名
    private byte[] signedMessage(Credentials from, RawTransaction rawTransaction) {
        return TransactionEncoder.signMessage(rawTransaction, from);
    }

    //创建未签名的RAW交易
    private RawTransaction
    createEtherTransaction(String fromAddress, String toAddress, BigInteger value, BigInteger nonce) throws Exception {
        RawTransaction rawTransaction = RawTransaction.createEtherTransaction(nonce, BigInteger.valueOf(10_000_000_000L), BigInteger.valueOf(21_000), toAddress, value);
        return rawTransaction;
    }

    //发送已签名的交易
    private String send(byte[] signedMessage) throws ExecutionException, InterruptedException, IOException {
        try {
            String hash = Numeric.toHexString(Hash.sha3(signedMessage));
            CompletableFuture<EthSendTransaction> ethSendTransaction =
                    web3j.ethSendRawTransaction(Numeric.toHexString(signedMessage))
                            .sendAsync();
            return hash;
        } catch (Exception e) {
            log.error(e.toString());
            throw e;
        }
    }

    //创建 并 发送交易
    public String sendTransaction(Credentials from, String toAddress, long amount) throws Exception {
        BigInteger nonce = this.getNonce(from.getAddress());
        RawTransaction rawTransaction = createEtherTransaction(from.getAddress(), toAddress, BigInteger.valueOf(amount), nonce);
        String hash = send(signedMessage(from, rawTransaction));
        return hash;
    }

    //测试数据库完整性的函数，用来检查数据库中的Block 和 交易一一对应的.防止上传交易时遗漏
    public boolean checkAll(MongoCollection<Document> blockInfo, MongoCollection<Document> txDoc) {
        MongoCursor<Document> mongoCursor = blockInfo.find().iterator();
        while (mongoCursor.hasNext()) {
            Document doc = mongoCursor.next();
            {
                {
                    if (Tools.check(doc, txDoc)) {
                        log.info(String.format("%d check success have %d tx", doc.getLong("number"), ((List<String>) doc.get("transactions")).size()));
                    } else {
                        log.error(String.format("%d check fail", doc.getLong("number")));
                    }
                }
            }
        }
        return true;
    }

    //批量创建Web3j连接,用来多线程时并发
    protected ArrayList<Web3j> createmuchclient(int count, String path) throws ConnectException {

        ArrayList<Web3j> web3js = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            WebSocketService ws_ = new WebSocketService(path, false);
            ws_.connect();
            web3js.add(Web3j.build(ws_));
        }
        return web3js;
    }

    //随机自动按照一定速率发送交易 用来模拟真实ETH网络
    public void randomSendTx(MongoCollection<Document> walletFileDoc) {
        {
            //每秒自动随机发送 10 笔交易
            Random rand = new Random();
            String randomWalletFrom = null;
            String randomWalletTo = null;
            long lastSendPoint = 0;
            ObjectMapper objectMapper = new ObjectMapper();
            while (true) {
                lastSendPoint = System.currentTimeMillis();
                randomWalletFrom = address.get(rand.nextInt(address.size()));
                randomWalletTo = address.get(rand.nextInt(address.size()));

                if (randomWalletFrom.equals(randomWalletTo)) {
                    continue;
                }
                try {
                    if (get_balance(randomWalletFrom) == BigInteger.valueOf(0)) {
                        sendTransaction(rich, randomWalletFrom, (long) (1 * Math.pow(10, Convert.Unit.ETHER.ordinal())));
                        continue;
                    }
                    WalletFile walletFile = objectMapper.readValue(walletFileDoc.find(Filters.eq("address", randomWalletFrom)).projection(Projections.excludeId()).first().toJson(), WalletFile.class);
                    Credentials From = Credentials.create(Wallet.decrypt("123456", walletFile));
                    int amountPaid = rand.nextInt(address.size());

                    sendTransaction(From, randomWalletTo, (long) (amountPaid * Math.pow(100, Convert.Unit.WEI.ordinal())));
                    long expirationTime = lastSendPoint + 20;
                    if ((expirationTime - System.currentTimeMillis()) > 0) {
                        Thread.sleep((expirationTime - System.currentTimeMillis()));
                        continue;
                    }
                } catch (Exception e) {
                    log.error(e.toString());
                }
            }
        }
    }

    //从节点订阅区块并获取交易信息 上传至Mongodb
    public void TxintoDb(int start, MongoCollection<Document> transactionsCollection, MongoCollection<Document> blockCollection) {
        Subscription subscription = (Subscription) web3j.replayPastAndFutureBlocksFlowable(new DefaultBlockParameterNumber(start), true).subscribe(block -> {

            Document blockDoc = Tools.block2Doc(block.getBlock());
            blockCollection.insertOne(blockDoc);
            ArrayList<Document> txs = new ArrayList<>();
            block.getBlock().getTransactions().forEach(tx ->
                    {
                        txs.add(Tools.transaction2Doc(((EthBlock.TransactionObject) tx.get())));
                    }
            );
            if (!txs.isEmpty()) {
                transactionsCollection.insertMany(txs);
            }
        });
        this.subscriptionHashMap.put("TxintoDb",subscription);
    }

    //从节点订阅未确认的交易
    public void getPendingTx() {
        Subscription subscription = (Subscription) web3j.pendingTransactionFlowable().subscribe(tx -> {
            log.info(tx.getHash());
        });
        this.subscriptionHashMap.put("getPendingTx",subscription);
    }

    //获取订阅
    public Subscription getSubscription(String name)
    {
        return this.subscriptionHashMap.get(name);
    }

    //移除订阅
    public void delSubscription(String name)
    {
        if (this.subscriptionHashMap.get(name) != null)
        {
            this.subscriptionHashMap.get(name).cancel();
            this.subscriptionHashMap.remove(name);
        }
    }


}
