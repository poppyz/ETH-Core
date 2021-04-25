package core.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bson.Document;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletFile;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.Transaction;

import java.io.BufferedWriter;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import static com.mongodb.client.model.Filters.eq;

public class Tools {

    public static Document block2Doc(EthBlock.Block block) {
        Document doc = new Document();
        doc.append("number", block.getNumber().longValue());
        doc.append("hash", block.getHash());
        doc.append("nonce", block.getNonce().longValue());
        doc.append("sha3Uncles", block.getSha3Uncles());
        doc.append("logsBloom", block.getLogsBloom());
        doc.append("transactionsRoot", block.getTransactionsRoot());
        doc.append("stateRoot", block.getStateRoot());
        doc.append("receiptsRoot", block.getReceiptsRoot());
        doc.append("author", block.getAuthor());
        doc.append("miner", block.getMiner());
        doc.append("mixHash", block.getMixHash());
        doc.append("difficulty", block.getDifficulty().longValue());
        doc.append("totalDifficulty", block.getTotalDifficulty().longValue());
        doc.append("extraData", block.getExtraData());
        doc.append("size", block.getSize().longValue());
        doc.append("gasLimit", block.getGasLimit().longValue());
        doc.append("gasUsed", block.getGasUsed().longValue());
        doc.append("timestamp", block.getTimestamp().longValue());
        ArrayList<String> txs = new ArrayList<>();
        block.getTransactions().forEach(
                tx ->
                {
                    txs.add(((EthBlock.TransactionObject) tx.get()).get().getHash());
                }
        );
        doc.append("txcount", txs.size());
        doc.append("transactions", txs);
        doc.append("uncles", block.getUncles());
        doc.append("sealFields", block.getSealFields());
        return doc;
    }


    public static Document transaction2Doc(Transaction tx) {

        Document doc = new Document();
        doc.append("hash", tx.getHash());
        doc.append("nonce", tx.getNonce().longValue());
        doc.append("blockHash", tx.getBlockHash());
        doc.append("blockNumber", tx.getBlockNumber().longValue());
        doc.append("transactionIndex", tx.getTransactionIndex().longValue());
        doc.append("from", tx.getFrom());
        doc.append("to", tx.getTo());
        doc.append("value", tx.getValue().longValue());
        doc.append("gasPrice", tx.getGasPrice().longValue());
        doc.append("gas", tx.getGas().longValue());
        doc.append("input", tx.getInput());
        doc.append("creates", tx.getCreates());
        doc.append("publicKey", tx.getPublicKey());
        doc.append("raw", tx.getRaw());
        doc.append("r", tx.getR());
        doc.append("s", tx.getS());
        doc.append("v", tx.getV());
        doc.append("chainId", tx.getChainId());
        return doc;
    }

    public static Transaction transaction2Doc(Document doc) {
        Transaction tx = new Transaction(doc.getString("hash"), Long.toHexString(doc.getLong("nonce")), doc.getString("blockHash"),
                Long.toHexString(doc.getLong("blockNumber")), Long.toHexString(doc.getLong("transactionIndex")),
                doc.getString("from"), doc.getString("to"), Long.toHexString(doc.getLong("value")), Long.toHexString(doc.getLong("gasPrice")),
                Long.toHexString(doc.getLong("gas")), doc.getString("input"), doc.getString("creates"), doc.getString("publicKey"),
                doc.getString("raw"), doc.getString("r"), doc.getString("s"), doc.getLong("v"));
        return tx;
    }


    public static WalletFile createWallet(String pwd, boolean useFullScrypt) throws Exception {
        ECKeyPair ecKeyPair = Keys.createEcKeyPair();
        WalletFile walletFile = null;
        if (useFullScrypt) {
            walletFile = Wallet.createStandard(pwd, ecKeyPair);
        } else {
            walletFile = Wallet.createLight(pwd, ecKeyPair);
        }
        return walletFile;
    }


    public static boolean createWallets(long count, String pwd, boolean useFullScrypt, BufferedWriter walletFilesLocal, AtomicLong success, AtomicLong status) throws Exception {
        createWallet(pwd, useFullScrypt);
        ArrayList<WalletFile> walletFilesTemp = new ArrayList<>();
        for (long i = 0; i < count; i++) {
            walletFilesTemp.add(createWallet(pwd, useFullScrypt));
            if (walletFilesTemp.size() == 50) {
                synchronized (walletFilesLocal) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    for (WalletFile walletFileTemp : walletFilesTemp) {
                        String json = null;
                        json = objectMapper.writeValueAsString(walletFileTemp);
                        walletFilesLocal.write(json + '\n');
                        success.addAndGet(1);
                    }
                    walletFilesTemp.clear();
                    walletFilesLocal.flush();
                }
            }
        }
        synchronized (walletFilesLocal) {
            ObjectMapper objectMapper = new ObjectMapper();
            for (WalletFile walletFileTemp : walletFilesTemp) {
                String json = null;
                json = objectMapper.writeValueAsString(walletFileTemp);
                walletFilesLocal.write(json + '\n');
                success.addAndGet(1);
            }
            walletFilesTemp.clear();
            walletFilesLocal.flush();
        }
        return true;
    }


    public static void createWalletByThread(ExecutorService threadPools, String pwd, boolean useFullScrypt, long number, BufferedWriter walletFilesLocal) throws Exception {
        int threads = (int) (Runtime.getRuntime().availableProcessors() * 1.2);
        AtomicLong success = new AtomicLong();
        ArrayList<AtomicLong> status = new ArrayList();
        long perCount = number / threads;
        for (int i = 0; i < threads + 1; i++) {
            AtomicLong state = new AtomicLong();
            status.add(state);
            int index = i;
            threadPools.submit(() ->
            {
                try {
                    createWallets(perCount, pwd, useFullScrypt, walletFilesLocal, success, state);
                    state.addAndGet(2);
                } catch (Exception e) {
                    e.printStackTrace();
                    state.addAndGet(1);
                }
            });
        }
        if (number - perCount * threads > 0) {
            AtomicLong state = new AtomicLong();
            status.add(state);
            threads++;
            int finalThreads = threads;
            threadPools.submit(() ->
            {
                try {
                    createWallets(number - perCount * (finalThreads - 1), pwd, useFullScrypt, walletFilesLocal, success, state);
                    state.addAndGet(2);
                } catch (Exception e) {
                    e.printStackTrace();
                    state.addAndGet(1);
                }
            });
        }

        while (success.get() != number) {
            long end = 0;
            long fail = 0;
            for (AtomicLong state : status) {
                switch ((int) state.get()) {
                    case 0: {
                        Thread.sleep(10);
                        continue;
                    }
                    case 1: {
                        fail++;
                        end++;
                        break;
                    }
                    default: {
                        end++;
                        break;
                    }
                }
            }
            if (end == threads) {
                System.out.printf(String.format("Plan to generate %d, actually produce %d, error %d", number, success.get(), fail));
                break;
            }
        }
    }

    public static boolean check(Document lastblock, MongoCollection<Document> transactionsDoc) {
        Set<String> txs = new HashSet<String>((List<String>) lastblock.get("transactions"));
        int raw_len = txs.size();
        MongoCursor<Document> mongoCursor = transactionsDoc.find(eq("number", lastblock.getLong("number"))).iterator();
        while (mongoCursor.hasNext()) {
            Document doc = mongoCursor.next();
            if (txs.add(doc.getString("hash"))) {
                return false;
            }
        }
        return true;
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
}
