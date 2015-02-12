package monad.core.internal;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Cracker {
    private static String createSerialNo(String s1, String s2) {
        String s3 = "0f9cfb7a9acced8a4167ea8006ccd098";
        int i1 = 0;
        String s4 = "";
        for (int j1 = 0; j1 < s1.length(); j1++) {
            i1 = i1 != s3.length() ? i1 : 0;
            s4 = s4 + s3.charAt(i1) + (char) (s1.charAt(j1) ^ s3.charAt(i1));
            i1++;
        }

        return b(c(s4, s2));
    }

    private static String b(String s1) {
        return new String(ByteEncoder.encode(s1.getBytes()));
    }

    private static String c(String s1, String s2) {
        s2 = d(s2);
        int i = 0;
        String s3 = "";
        for (int i1 = 0; i1 < s1.length(); i1++) {
            i = i != s2.length() ? i : 0;
            int j1 = s1.charAt(i1) ^ s2.charAt(i);
            s3 = s3 + (char) j1;
            i++;
        }

        return s3;
    }

    private static String d(String s) {
        char ac1[] = {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
        };
        byte[] s1 = s.getBytes();
        MessageDigest messagedigest;
        try {
            (messagedigest = MessageDigest.getInstance("MD5")).update(s1);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        int i1;
        char ac2[] = new char[(i1 = (s1 = messagedigest.digest()).length) << 1];
        int j1 = 0;
        for (int k1 = 0; k1 < i1; k1++) {
            byte byte0 = s1[k1];
            ac2[j1++] = ac1[byte0 >>> 4 & 0xf];
            ac2[j1++] = ac1[byte0 & 0xf];
        }

        return new String(ac2);
    }

    private static String c(String s1) {
        if (s1 == null)
            return null;
        try {
            //return s1;
            return new String(ByteEncoder.decode(s1.toCharArray()));
        } catch (Exception _ex) {
            return null;
        }
    }

    public static void main(String[] args) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        //config expired time
        String expiredTime = "20140501";

        String code = "xcQv?QVz3:QrzLfbh[_=R3<d@ul0013nolimitviewer" + expiredTime + "/S[]EtpBk9PV>lbkW@<1GpG8u]8B/svMxv\\@X=W3?MLb2kR1gqTlQ09`X3ZVl75/S1TO;lcklHIb3p2O8:dID\\k?0PSA7f:C@yjO4Oe_3QzOyybC1qoVzc8?G8cTQ<FZu29r1F4[WbRs]z>qF>^;0o?ZB8q^QFIUYbN<N=zTNqfKC7DDmp\\NlKzi8VaTIi6gku/89@]Ia/yTGKCSeYrja@WkK7kmTnEWXm_mP6Ml:oahU4vyoK^XM9ld0wJ8F=sQ42Qn2JM4/ofQ0M[e9f5BF]\\?r;KlS0udrz9rIsuUuQEnFtKa:nPL;1D:s3HNIcN`u[2LAr];NJz_Q>Hq0R=qa4n3TP[^lgjK:a3vJUf5K;7oM0<QwQ?Jok9ToZmyZDnaOT_QM8u>bEb4ncuhaARRflml1GR_ghms`Q_J<[c@/7JcCSloQV3pP\\XIf_GD`yx4f?R/z>uNIx2y3E\\Bez/Q8;i3Ed4hr?hw88b/L/Wv::9RmCBhd8x?\\rc;6;eSc1?[^kpBw0W7]zFPRAJ6]oM9]Gj;F=td5xgh[am/dq[rQy^G:5/fasNga<I<Y\\LFoWee4rl5rlBjRPK3mdm3w1;1ZgHrFHdpWJQiCcNznEawg9S_Ay<Mr_D22<RH_ijB6;IzWnIQ=d@<r\\?NlPRj8xpH8akjW4Fgw7UHay9UScLt8jZVWA;qbvg>>y<0YSw;RcB81asp]Y7B\\Jx8I93EU/2bmxlkHTH[8eNxmTGXt=ypoH6MaCrqD4Z_lz]El_<uKzQt:bz=ew8DR0Mqo>yiyBya/sN9gguSz4YyQrSFDmKp>5bshwC:6bEFE]JZj`j3n@ONJ0@L3kQgJfZd3vWRGeOFd\\8l]^P@Gn]YK6T7odcq4FkBk^]V_:^=9A]Lf]^JB<vv8`FFuaktkt8;ptgSCIEXjI:Kx`F2jkCR2wF]USFi1RcI]D7bsvTt73tz^=hz5LbA`[QGBCkNAE8\\8OGhLB[dALkIW7jWGqfsdULN5[2";
        String seed = "VIEWERKEY@1223nolimitviewer";
        String seedWithMd5 = d(seed);//s5为md5的值
        String serialNo = "0013nolimitviewer" + createSerialNo(code, seedWithMd5);
        //output serialno
        System.out.println(serialNo);
        //check serialno
        System.out.println(new Cracker().decodeSerialNo(serialNo));
    }

    private String decodeSerialNo(String s1) throws NoSuchAlgorithmException {
        int i1 = Integer.parseInt(s1.substring(0, 4));
        String s3 = s1.substring(4, i1 + 4);//nolimitviewer
        String s4 = "VIEWERKEY@1223" + s3;
        String s5 = d(s4);//s5为md5的值
        s1 = s1.substring(i1 + 4);//密文
        String m = b(s1, s5);//解密
        return m.substring(s4.length(), s4.length() + 4 + s3.length() + 8);
    }

    private String b(String s1, String s2_1) {
        s1 = c(s1);//还原原始字符串密文
        s1 = c(s1, s2_1);//还原原始密文
        String ret = "";
        for (int s2 = 0; s2 < s1.length(); s2++) {
            int i1 = s1.charAt(s2) ^ s1.charAt(s2 + 1);
            ret = ret + (char) i1;
            s2++;
        }

        return ret;

    }
}
