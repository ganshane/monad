// Copyright 2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.support.services.algorithm;

import java.util.Random;

/**
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-02-02
 */
public class AES {
    String key;//密钥
    int round;//加密轮数

    public AES() {
        super();
        this.key = key();
        this.round = new Random().nextInt(100);
    }

    public AES(String key, int round) {
        super();
        this.key = key;
        this.round = round;
    }

    //S盒
    static final String[][] Sbox = {
            {"63", "7c", "77", "7b", "f2", "6b", "6f", "c5", "30", "01", "67", "2b", "fe", "d7", "ab", "76"},
            {"ca", "82", "c9", "7d", "fa", "59", "47", "f0", "ad", "d4", "a2", "af", "9c", "a4", "72", "c0"},
            {"b7", "fd", "93", "26", "36", "3f", "f7", "cc", "34", "a5", "e5", "f1", "71", "d8", "31", "15"},
            {"04", "c7", "23", "c3", "18", "96", "05", "9a", "07", "12", "80", "e2", "eb", "27", "b2", "75"},
            {"09", "83", "2c", "1a", "1b", "6e", "5a", "a0", "52", "3b", "d6", "b3", "29", "e3", "2f", "84"},
            {"53", "d1", "00", "ed", "20", "fc", "b1", "5b", "6a", "cb", "be", "39", "4a", "4c", "58", "cf"},
            {"d0", "ef", "aa", "fb", "43", "4d", "33", "85", "45", "f9", "02", "7f", "50", "3c", "9f", "a8"},
            {"51", "a3", "40", "8f", "92", "9d", "38", "f5", "bc", "b6", "da", "21", "10", "ff", "f3", "d2"},
            {"cd", "0c", "13", "ec", "5f", "97", "44", "17", "c4", "a7", "7e", "3d", "64", "5d", "19", "73"},
            {"60", "81", "4f", "dc", "22", "2a", "90", "88", "46", "ee", "b8", "14", "de", "5e", "0b", "db"},
            {"e0", "32", "3a", "0a", "49", "06", "24", "5c", "c2", "d3", "ac", "62", "91", "95", "e4", "79"},
            {"e7", "c8", "37", "6d", "8d", "d5", "4e", "a9", "6c", "56", "f4", "ea", "65", "7a", "ae", "08"},
            {"ba", "78", "25", "2e", "1c", "a6", "b4", "c6", "e8", "dd", "74", "1f", "4b", "bd", "8b", "8a"},
            {"70", "3e", "b5", "66", "48", "03", "f6", "0e", "61", "35", "57", "b9", "86", "c1", "1d", "9e"},
            {"e1", "f8", "98", "11", "69", "d9", "8e", "94", "9b", "1e", "87", "e9", "ce", "55", "28", "df"},
            {"8c", "a1", "89", "0d", "bf", "e6", "42", "68", "41", "99", "2d", "0f", "b0", "54", "bb", "16"}
    };
    //逆S盒
    static final String[][] InvSbox = {
            {"52", "09", "6a", "d5", "30", "36", "a5", "38", "bf", "40", "a3", "9e", "81", "f3", "d7", "fb"},
            {"7c", "e3", "39", "82", "9b", "2f", "ff", "87", "34", "8e", "43", "44", "c4", "de", "e9", "cb"},
            {"54", "7b", "94", "32", "a6", "c2", "23", "3d", "ee", "4c", "95", "0b", "42", "fa", "c3", "4e"},
            {"08", "2e", "a1", "66", "28", "d9", "24", "b2", "76", "5b", "a2", "49", "6d", "8b", "d1", "25"},
            {"72", "f8", "f6", "64", "86", "68", "98", "16", "d4", "a4", "5c", "cc", "5d", "65", "b6", "92"},
            {"6c", "70", "48", "50", "fd", "ed", "b9", "da", "5e", "15", "46", "57", "a7", "8d", "9d", "84"},
            {"90", "d8", "ab", "00", "8c", "bc", "d3", "0a", "f7", "e4", "58", "05", "b8", "b3", "45", "06"},
            {"d0", "2c", "1e", "8f", "ca", "3f", "0f", "02", "c1", "af", "bd", "03", "01", "13", "8a", "6b"},
            {"3a", "91", "11", "41", "4f", "67", "dc", "ea", "97", "f2", "cf", "ce", "f0", "b4", "e6", "73"},
            {"96", "ac", "74", "22", "e7", "ad", "35", "85", "e2", "f9", "37", "e8", "1c", "75", "df", "6e"},
            {"47", "f1", "1a", "71", "1d", "29", "c5", "89", "6f", "b7", "62", "0e", "aa", "18", "be", "1b"},
            {"fc", "56", "3e", "4b", "c6", "d2", "79", "20", "9a", "db", "c0", "fe", "78", "cd", "5a", "f4"},
            {"1f", "dd", "a8", "33", "88", "07", "c7", "31", "b1", "12", "10", "59", "27", "80", "ec", "5f"},
            {"60", "51", "7f", "a9", "19", "b5", "4a", "0d", "2d", "e5", "7a", "9f", "93", "c9", "9c", "ef"},
            {"a0", "e0", "3b", "4d", "ae", "2a", "f5", "b0", "c8", "eb", "bb", "3c", "83", "53", "99", "61"},
            {"17", "2b", "04", "7e", "ba", "77", "d6", "26", "e1", "69", "14", "63", "55", "21", "0c", "7d"}

    };

    //字节代替
    public char[] subBytes(char[] state) {
        char[] result = new char[state.length];
        for (int i = 0; i < state.length; i++) {
            String s = Integer.toHexString(state[i]);
            if (s.length() < 2) {
                s = "0" + s;
            }
            String rs = Sbox[s.charAt(0) < 97 ? s.charAt(0) - 48 : s.charAt(0) - 87][s.charAt(1) < 97 ? s.charAt(1) - 48 : s.charAt(1) - 87];
            result[i] = (char) Integer.parseInt(rs, 16);
        }
        return result;
    }

    //逆字节代替
    public char[] invSubBytes(char[] state) {
        char[] result = new char[16];
        for (int i = 0; i < state.length; i++) {
            String s = Integer.toHexString(state[i]);
            if (s.length() < 2) {
                s = "0" + s;
            }
            String rs = InvSbox[s.charAt(0) < 97 ? s.charAt(0) - 48 : s.charAt(0) - 87][s.charAt(1) < 97 ? s.charAt(1) - 48 : s.charAt(1) - 87];
            result[i] = (char) Integer.parseInt(rs, 16);
        }
        return result;
    }

    //列混淆
    public char[] mixColumns(char[] state) {
        char[] lisa = {2, 3, 1, 1};
        char[] result = new char[16];
        for (int col = 0; col < 4; col++) {
            char[] lisb = new char[4];
            int flagc = col;
            for (int m = 0; m < 4; m++) {
                lisb[m] = state[flagc];
                flagc += 4;
            }
            for (int row = 0; row < 4; row++) {
                int k = ffmul(lisb[0], lisa[(4 - row) % 4]) ^ ffmul(lisb[1], lisa[(5 - row) % 4]) ^ ffmul(lisb[2], lisa[(6 - row) % 4]) ^ ffmul(lisb[3], lisa[(7 - row) % 4]);
                result[row * 4 + col] = (char) k;
            }
        }
        return result;
    }

    //逆列混淆
    public char[] invMixColumns(char[] state) {
        char[] lisa = {14, 11, 13, 9};
        char[] result = new char[16];
        for (int col = 0; col < 4; col++) {
            char[] lisb = new char[4];
            int flagc = col;
            for (int m = 0; m < 4; m++) {
                lisb[m] = state[flagc];
                flagc += 4;
            }
            for (int row = 0; row < 4; row++) {
                int k = ffmul(lisb[0], lisa[(4 - row) % 4]) ^ ffmul(lisb[1], lisa[(5 - row) % 4]) ^ ffmul(lisb[2], lisa[(6 - row) % 4]) ^ ffmul(lisb[3], lisa[(7 - row) % 4]);
                result[row * 4 + col] = (char) k;
            }
        }
        return result;
    }

    //字节乘法
    public int ffmul(int a, int b) {
        String ba = Integer.toBinaryString(a);
        int[] stor = new int[8];
        while (ba.length() < 8) {
            ba = "0" + ba;
        }
        stor[0] = b;
        for (int i = 1; i < 8; i++) {
            String bb = Integer.toBinaryString(stor[i - 1]);
            if (bb.length() < 8) {
                stor[i] = leftshift(stor[i - 1], 1);
            } else
                stor[i] = leftshift(stor[i - 1], 1) ^ 27;
        }
        int result = 0;
        for (int i = 7; i >= 0; i--) {
            if (ba.charAt(i) == '1') {
                if (result == 0) {
                    result = stor[7 - i];
                } else
                    result = result ^ stor[7 - i];
            }
        }
        return result;
    }

    //二进制数左移
    public int leftshift(int num, int step) {
        String ba = Integer.toBinaryString(num);
        while (ba.length() < 8) {
            ba = "0" + ba;
        }
        for (int i = 0; i < step; i++) {
            ba += "0";
        }
        return Integer.parseInt(ba.substring(step), 2);
    }

    //行移位
    public char[] shiftRows(char[] state) {
        char[][] in = new char[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                in[i][j] = state[i * 4 + j];
            }
        }
        char[] result = new char[16];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                result[i * 4 + j] = in[i][(j + i) % 4];
            }
        }
        return result;
    }

    //逆行移位
    public char[] invShiftRows(char[] state) {
        char[][] in = new char[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                in[i][j] = state[i * 4 + j];
            }
        }
        char[] result = new char[16];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                int col = (j - i) > 0 ? (j - i) : (j - i) + 4;
                result[i * 4 + j] = in[i][col % 4];
            }
        }
        return result;
    }

    //轮密钥加
    public char[] addRoundKey(char[] state, char[] key) {
        char[] result = new char[16];
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                result[row * 4 + col] = (char) (state[row * 4 + col] ^ key[row * 4 + col]);
            }
        }
        return result;
    }

    //密钥扩展
    public char[][] keyExpansion(char[] key, int round) {
        char[] RC = new char[round];
        for (int i = 0; i < round; i++) {
            if (i == 0) {
                RC[i] = 1;
            } else {
                RC[i] = (char) ffmul(2, RC[i - 1]);
            }
        }

        char[][] newkey = new char[round][16];
        char[] start = {key[12], key[13], key[14], key[15]};
        for (int r = 0; r < round; r++) {
            for (int i = 3; i < 7; i++) {
                if (i == 3) {
                    char ot = start[0];
                    start[0] = start[1];
                    start[1] = start[2];
                    start[2] = start[3];
                    start[3] = ot;//RotWord()
                    start = subBytes(start);
                    start = XOR(start, new char[]{RC[r], 0, 0, 0});
                }
                char[] last = {key[(i - 3) * 4], key[(i - 3) * 4 + 1], key[(i - 3) * 4 + 2], key[(i - 3) * 4 + 3]};
                start = XOR(start, last);
                for (int k = 0; k < 4; k++) {
                    key[(i - 3) * 4 + k] = start[k];
                }
                for (int j = 0; j < 4; j++) {
                    newkey[r][(i + 1) % 4 + j * 4] = start[j];
                }
            }
        }
        return newkey;
    }

    //异或
    public char[] XOR(char[] a, char[] b) {
        char[] result = new char[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = (char) (((int) a[i]) ^ ((int) b[i]));
        }
        return result;
    }

    //字节加密
    public char[] cipher(char[] in, char[] key, int round) {
        char[] out = new char[16];
        char[] newword = new char[16];
        for (int i = 0; i < 16; i++) {
            newword[i] = key[i];
        }
        char[][] keys = keyExpansion(newword, round);
        in = exchange(in);
        key = exchange(key);
        in = addRoundKey(in, key);

        for (int i = 0; i < round - 1; i++) {
            out = subBytes(in);
            out = shiftRows(out);
            out = mixColumns(out);
            in = addRoundKey(out, keys[i]);
        }
        out = subBytes(in);
        out = shiftRows(out);
        out = addRoundKey(out, keys[round - 1]);
        return exchange(out);
    }

    //字符串加密
    public String cipher(String in, String key, int round) {
        StringBuffer sb = new StringBuffer();
        char[] keys = Tools.hexStr2Cs(key);
        String hexStr = Tools.Str2hexStr(in);
        while (hexStr.length() >= 32) {
            String sin = hexStr.substring(0, 32);
            hexStr = hexStr.substring(32);
            sb.append(Tools.Cs2hexStr(cipher(Tools.hexStr2Cs(sin), keys, round)));
        }
        if (hexStr.length() > 0) {
            while (hexStr.length() < 32) {
                hexStr += "0";
            }
            sb.append(Tools.Cs2hexStr(cipher(Tools.hexStr2Cs(hexStr), keys, round)));
        }
        return sb.toString();
    }

    public String cipher(String in) {
        return cipher(in, this.key, this.round);
    }

    //字节解密
    public char[] inCipher(char[] in, char[] key, int round) {
        char[] out = new char[16];
        char[] newword = new char[16];
        for (int i = 0; i < 16; i++) {
            newword[i] = key[i];
        }
        char[][] keys = keyExpansion(newword, round);
        in = exchange(in);
        key = exchange(key);
        in = addRoundKey(in, keys[round - 1]);

        for (int i = 0; i < round - 1; i++) {
            out = invShiftRows(in);
            out = invSubBytes(out);
            out = addRoundKey(out, keys[round - 2 - i]);
            in = invMixColumns(out);

        }
        out = invShiftRows(in);
        out = invSubBytes(out);
        out = addRoundKey(out, key);
        return exchange(out);
    }

    //字符串解密
    public String inCipher(String in, String key, int round) {
        StringBuffer sb = new StringBuffer();
        char[] keys = Tools.hexStr2Cs(key);
        String hexStr = in;//Str2hexStr(in);
        while (hexStr.length() >= 32) {
            String sin = hexStr.substring(0, 32);
            hexStr = hexStr.substring(32);
            sb.append(Tools.hexStr2Str(Tools.Cs2hexStr(inCipher(Tools.hexStr2Cs(sin), keys, round))));
        }
        if (hexStr.length() > 0) {
            while (hexStr.length() < 32) {
                hexStr += "0";
            }
            sb.append(Tools.hexStr2Str(Tools.Cs2hexStr(inCipher(Tools.hexStr2Cs(hexStr), keys, round))));
        }
        while (sb.charAt(sb.length() - 1) == 0) {
            sb = sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    public String inCipher(String in) {
        return inCipher(in, this.key, this.round);
    }

    //行列变换
    public char[] exchange(char[] chars) {
        char[] nchars = new char[chars.length];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                nchars[i * 4 + j] = chars[j * 4 + i];
            }
        }
        return nchars;
    }

    public static void print(String s, char[] chars) {
        System.out.println(s + " ");
        String[] sts = Tools.Cs2Ss(chars);
        for (int i = 0; i < sts.length; i++) {
            System.out.print(sts[i] + " ");
            if (i % 4 == 3) {
                System.out.println();
            }
        }
        System.out.println();
    }

    //密钥生成器
    public String key() {
        StringBuffer sb = new StringBuffer();
        Random r = new Random();
        for (int i = 0; i < 8; i++) {
            sb.append((char) r.nextInt(65535));
        }
        return Tools.Str2hexStr(sb.toString());
    }

    public static void main(String[] args) {
        AES aes = new AES();
        String cstring = "大家,， 爱上对方";
        //String cword = "2b7e151628aed2a6abf7158809cf4f3c";
        String cword = aes.key();
        String mw = aes.cipher(cstring);
        System.out.println("密文:    " + mw);
        String result1 = aes.inCipher(mw);
        System.out.println(result1);
        System.out.println("密钥:    " + cword);
    }
}

