ETH 集群 设计 [Link](https://cloud.tencent.com/developer/article/1592228)

首先获取geth镜像 推荐stable版本
```
docker pull ethereum/client-go:stable
```

创建Docker网络 
```
docker network create -d bridge --subnet=130.100.0.0/16 ethnet
```

创建账户
```
geth -datadir ./data account new
```
配置创世区块genesis.json
```
{
  "config": {
    "chainId": 19950000,
    "homesteadBlock": 0,
    "eip150Block": 0,
    "eip155Block": 0,
    "eip158Block": 0
  },
    "alloc": {
    "0xcbaa3e70c0c4c332d66b3db88d034fc95de7bf15": {"balance": "4000000000000000000000000"},
    "0xc796ef1774683249a19752a2b18d6a78b8783521": {"balance": "10000000000000000000"},
    "0x9b74ee168f39159ade04f6415a28e6bb2f045a31": {"balance": "10000000000000000000"},
    "0x8bf575a924279269f831a17fd8f7d8b141fcab03": {"balance": "10000000000000000000"}
  },
  "coinbase": "0x0000000000000000000000000000000000000000",
  "difficulty": "1",
  "gasLimit": "8000000",
  "nonce": "0x0000000000000000",
  "mixhash": "0x0000000000000000000000000000000000000000000000000000000000000000",
  "parentHash": "0x0000000000000000000000000000000000000000000000000000000000000000",
  "timestamp": "0x00"
}
```
ETH 启动脚本
node0 && node1 && node2
```
#!/bin/sh
if [ ! -d "/workspace/data/geth" ];then
      echo "geth Yes" 
  	geth -datadir /workspace/data/ init /workspace/genesis.json         #初始化节点
  else
	geth -datadir /workspace/data/ --syncmode full --networkid 19950000 --nousb --nodiscover --rpc.allow-unprotected-txs --allow-insecure-unlock --unlock "0xc796ef1774683249a19752a2b18d6a78b8783521" --password /workspace/pwd --miner.etherbase "0xc796ef1774683249a19752a2b18d6a78b8783521" console 2>>/workspace/data/geth.log     #启动节点
fi
```
miner0
```
#!/bin/sh
if [ ! -d "/workspace/data/geth" ];then
      echo "geth Yes" 
  	geth -datadir /workspace/data/ init /workspace/genesis.json
  else
	geth -datadir /workspace/data/ --syncmode full --networkid 19950000 --nousb --nodiscover --rpc.allow-unprotected-txs console 2>>/workspace/data/geth.log
fi
```

node0
```
docker run -it --name=node0 --network ethnet --ip 130.100.0.10 --hostname node0 -v /home/ubuntu/workspace/data/node0:/workspace --entrypoint /workspace/init.sh ethereum/client-go:stable /workspace/mine.sh
```

node1
```
docker run -it --name=node1 --network ethnet --ip 130.100.0.11 --hostname node1 -v /home/ubuntu/workspace/data/node1:/workspace --entrypoint /workspace/init.sh ethereum/client-go:stable /workspace/mine.sh
```

miner0
```
docker run -it --name=miner0 --network ethnet --ip 130.100.100.1 --hostname miner0 -v /home/ubuntu/workspace/data/miner0:/workspace --entrypoint /workspace/init.sh ethereum/client-go:stable /workspace/node.sh
```

static-nodes.json
```
[
  "enode://bea35032bef24443ea9c9b3bcc307960c940aee0764e6fac72ec816d35f52d5db38000d75d6aa7ed34bbf07fa95c7efebcb0509fca8e2cc653960486b64e62cf@172.100.0.10:30303",
  "enode://c84894420a01c06084663f4572c883239a2fdc3571058b0243278b38a2704e16d0dbb4a66166995057bf0580c51f8d7893ac6a4c9b41a4b7bbf4e0ae59237d19@172.100.0.11:30303",
  "enode://94c4eb8f8481cef48b30060319e9c5f2fc3dc073529c7d8dedfffbc7eae32924b9e174f79fa3da6994dd55ccb0b90cdb87d5a8afc69e8b253894c0d47f571c6b@172.100.100.1:30303",
  "enode://656e6fff7c1d0355503b2d857b1e6f7a7a419ad4b8f3bee2b54917bab43fd9a7c83a84bb8626eff591b6e4206762bebf7c338320c9b27a6c8d05c87af503f0a6@35.72.63.205:30303"
]

```

trusted-nodes.json
```
[
  "enode://bea35032bef24443ea9c9b3bcc307960c940aee0764e6fac72ec816d35f52d5db38000d75d6aa7ed34bbf07fa95c7efebcb0509fca8e2cc653960486b64e62cf@172.100.0.10:30303",
  "enode://c84894420a01c06084663f4572c883239a2fdc3571058b0243278b38a2704e16d0dbb4a66166995057bf0580c51f8d7893ac6a4c9b41a4b7bbf4e0ae59237d19@172.100.0.11:30303",
  "enode://94c4eb8f8481cef48b30060319e9c5f2fc3dc073529c7d8dedfffbc7eae32924b9e174f79fa3da6994dd55ccb0b90cdb87d5a8afc69e8b253894c0d47f571c6b@172.100.100.1:30303",
  "enode://656e6fff7c1d0355503b2d857b1e6f7a7a419ad4b8f3bee2b54917bab43fd9a7c83a84bb8626eff591b6e4206762bebf7c338320c9b27a6c8d05c87af503f0a6@35.72.63.205:30303"
]

```

### 存在问题
* 当手动 docker stop nodeX 之后再次start 或者 直接docker restart nodeX时候 节点信息会出现 高度为0且无法同步的现象 暂未解决 我认为是我启动脚本写的有问题导致了多次init 了。但我改了脚本还是这个问题。


### 总结
ETH 搭建太难了，耽误了我两三周的时间，有时候我电脑重启三个节点都挂了，我不得不每次都要重来一次。最后我在亚马逊上开了云主机 建立了Node2 作为备份才能继续往下开发
如果没有使用docker 我可能要放弃了。

