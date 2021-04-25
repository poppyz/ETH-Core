## ETH 地址的生成
### Java

>从TypeScript代码很容易看出来
首先32byte的值通过secp256k1派生出Pubkey （需要时未压缩公钥 也就是 包含X Y坐标） 然后通过sha3-256 对公钥进行hash
然后取24之后的字符转为Hex 格式 得到地址
需要注意
- ```公钥 组成是 0x04 + X + Y```  X Y 为 G点的横纵坐标 所以 进行sha3-256 时候需要删除最前面的0x04
- sha3-256 的结果是32字节 我们只需要20字节 所以我们只保留hash结果的后20字节就可以

```
import { Keccak } from 'sha3'
import * as secp256k1 from "secp256k1"
import * as crypto from "crypto"
class Account {
    fullPubkey:Buffer;
    comparssPublic:Buffer;
    prikey:Buffer; 
    address:string;
    constructor(key:Buffer) {
        if (!secp256k1.privateKeyVerify(key)){
            throw new Error('Function not implemented.')
        }
        this.prikey = key
        this.fullPubkey =  Buffer.from(secp256k1.publicKeyCreate(this.prikey,false))
        this.comparssPublic = Buffer.from(secp256k1.publicKeyCreate(this.prikey,true))
        var hash = new Keccak(256);
        hash.update(this.fullPubkey.slice(1));
        this.address = hash.digest('hex').slice(24)
        console.log(this.prikey.toString("hex"))
        console.log(this.fullPubkey.toString("hex"))
        console.log(this.comparssPublic.toString("hex"))
        console.log(this.address)
    }
 
    keyVerify():Buffer{
        var key
        do{
            key = crypto.randomBytes(32);
        }
        while(!secp256k1.privateKeyVerify(key))
        return key
    }
    getAddress():string{
        return this.address
    }
    getPubkey():string{
        return this.fullPubkey.toString('hex')
    }
    getPrikey():string{
        return this.prikey.toString('hex')
    }
}
```
JAVA
```
KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "BC");
ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec("secp256k1");
keyPairGenerator.initialize(ecGenParameterSpec, new SecureRandom());
KeyPair keyPair = keyPairGenerator.generateKeyPair();
```
Java Web3j
```
public static ECKeyPair createEcKeyPair(SecureRandom random) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        KeyPair keyPair = createSecp256k1KeyPair(random);
        return ECKeyPair.create(keyPair);
        }
```

