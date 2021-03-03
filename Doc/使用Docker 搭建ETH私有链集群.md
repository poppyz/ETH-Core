ETH 集群 设计 [Link](https://cloud.tencent.com/developer/article/1592228)
接下来按照此文档进行




首先获取geth镜像 推荐stable版本
```
docker pull ethereum/client-go:stable
```

创建账户
```
geth -datadir ./data account new
```
配置创世区块genesis.json
```
{
  "config": {
    "chainId": 1995,
    "homesteadBlock": 0,
    "eip150Block": 0,
    "eip155Block": 0,
    "eip158Block": 0,
    "byzantiumBlock": 0,
    "constantinopleBlock": 0,
    "petersburgBlock": 0,
    "istanbulBlock": 0
  },
    "alloc"      : {
    "0xcbaa3e70c0c4c332d66b3db88d034fc95de7bf15": {"balance": "100000000000000000000"},
    "0xc796ef1774683249a19752a2b18d6a78b8783521": {"balance": "1000000000000000000"},
    "0x9b74ee168f39159ade04f6415a28e6bb2f045a31": {"balance": "1000000000000000000"},
    "0x8bf575a924279269f831a17fd8f7d8b141fcab03": {"balance": "1000000000000000000"}
  },
  "coinbase": "0x0000000000000000000000000000000000000000",
  "difficulty" : "0x400",
  "extraData"  : "",
  "gasLimit"   : "0x2fefd8",
  "nonce"      : "0x0000000000000000",
  "mixhash": "0x0000000000000000000000000000000000000000000000000000000000000000",
  "parentHash": "0x0000000000000000000000000000000000000000000000000000000000000000",
  "timestamp": "0x00"
}
