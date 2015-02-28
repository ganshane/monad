// Copyright 2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.support.services.algorithm;

import java.math.BigInteger;
import java.util.Random;

/**
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-02-02
 */


public class RSA {
    KEY ku;//公钥
    KEY kr;//私钥

    public RSA() {
        Random r = new Random();
        BigInteger p, q, n, nn;

        BigInteger e = BigInteger.ONE;// = new BigInteger(3+"");
        BigInteger d = BigInteger.ONE;
        //素数p,q,e,d
        while (true) {
            p = BigInteger.probablePrime(17, r);//new BigInteger(7+"");
            q = BigInteger.probablePrime(19, r);//new BigInteger(5+"");

            n = p.multiply(q);

            nn = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
            if (nn.longValue() > 65535) {

                for (int i = 3; i < nn.intValue(); i++) {
                    if (MyMath.gcd(i, nn.intValue()) == 1) {
                        e = BigInteger.valueOf(i);

                        if (MyMath.exgcd(e, nn).longValue() == -1)
                            continue;
                        else
                            break;
                    }
                }
                d = MyMath.exgcd(e, nn).mod(nn);
                BigInteger big = d.multiply(d);
                big = big.multiply(big);
                if (big.compareTo(n) > 0) {
                    break;
                } else
                    continue;
            }

        }

        this.ku = new KEY(e, n);
        this.kr = new KEY(d, n);
    }

    public RSA(KEY ku, KEY kr) {
        super();
        this.ku = ku;
        this.kr = kr;
    }

    class KEY {
        BigInteger x;
        BigInteger n;

        public KEY(BigInteger x, BigInteger n) {
            super();
            this.x = x;
            this.n = n;
        }

    }

    //加密
    public String Encryption(String s, KEY key) {
        StringBuffer sb = new StringBuffer();
        char[] cs = s.toCharArray();
        for (int i = 0; i < cs.length; i++) {
            int k = cs[i];
            if (k < key.n.longValue()) {
                BigInteger p = new BigInteger(k + "");
                int kk = Integer.parseInt(MyMath.reaminder(key.n, p, Long.parseLong(key.x.toString())).toString());
                char c = (char) kk;
                sb.append(c);
            } else {
                sb.append(cs[i]);
            }
        }
        return sb.toString();
    }

    //加密成16进制字符串
    public String Encryption(String s) {
        StringBuffer sb = new StringBuffer();
        char[] cs = s.toCharArray();
        for (int i = 0; i < cs.length; i++) {
            int k = cs[i];
            if (k < this.ku.n.longValue()) {
                BigInteger p = new BigInteger(k + "");
                long kk = Long.parseLong(MyMath.reaminder(this.ku.n, p, Long.parseLong(this.ku.x.toString())).toString());
                sb.append(Long.toHexString(kk));
                sb.append(" ");
            } else {
                sb.append(Integer.toHexString(k));
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    //解密
    public String Decryption(String s, KEY key) {
        return Encryption(s, key);
    }

    public String Decryption(String s) {
        StringBuffer sb = new StringBuffer();
        String[] ss = s.split(" ");
        for (int i = 0; i < ss.length; i++) {
            long k = Long.parseLong(ss[i], 16);
            BigInteger p = new BigInteger(k + "");
            int kk = Integer.parseInt(MyMath.reaminder(this.kr.n, p, Long.parseLong(this.kr.x.toString())).toString());
            sb.append(Tools.obox(Integer.toHexString(kk), 4));
        }
        return Tools.hexStr2Str(sb.toString());
    }

    public static void main(String[] args) {
        RSA rsa = new RSA();
        String s = "大家好abi 是低估 斯蒂芬和欧冠发 蛋糕房女生佛 ,；";
        String sa = rsa.Encryption(s);
        System.out.println("密文:    " + sa);
        System.out.println("明文:    " + rsa.Decryption(sa));
        System.out.println("e:" + rsa.kr.x + "  d:" + rsa.ku.x + "   n:" + rsa.kr.n);
    }
}

