// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 

package monad.core.internal;


final class ByteEncoder {

    private static char a_char_array1d_static_fld[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=".toCharArray();
    private static byte a_byte_array1d_static_fld[];

    static {
        a_byte_array1d_static_fld = new byte[256];
        for (int i = 0; i < 256; i++)
            a_byte_array1d_static_fld[i] = -1;

        for (int j = 65; j <= 90; j++)
            a_byte_array1d_static_fld[j] = (byte) (j - 65);

        for (int k = 97; k <= 122; k++)
            a_byte_array1d_static_fld[k] = (byte) ((k + 26) - 97);

        for (int l = 48; l <= 57; l++)
            a_byte_array1d_static_fld[l] = (byte) ((l + 52) - 48);

        a_byte_array1d_static_fld[43] = 62;
        a_byte_array1d_static_fld[47] = 63;
    }

    //加密
    public static char[] encode(byte abyte0[]) {
        char ac[] = new char[(abyte0.length + 2) / 3 << 2];
        int i = 0;
        for (int j = 0; i < abyte0.length; j += 4) {
            boolean flag = false;
            boolean flag1 = false;
            int k;
            k = (k = 0xff & abyte0[i]) << 8;
            if (i + 1 < abyte0.length) {
                k |= 0xff & abyte0[i + 1];
                flag1 = true;
            }
            k <<= 8;
            if (i + 2 < abyte0.length) {
                k |= 0xff & abyte0[i + 2];
                flag = true;
            }
            ac[j + 3] = a_char_array1d_static_fld[flag ? k & 0x3f : 64];
            k >>= 6;
            ac[j + 2] = a_char_array1d_static_fld[flag1 ? k & 0x3f : 64];
            k >>= 6;
            ac[j + 1] = a_char_array1d_static_fld[k & 0x3f];
            k >>= 6;
            ac[j] = a_char_array1d_static_fld[k & 0x3f];
            i += 3;
        }

        return ac;
    }

    //解密
    public static byte[] decode(char ac[]) {
        int i = ac.length;
        for (int j = 0; j < ac.length; j++)
            if (ac[j] > '\377' || a_byte_array1d_static_fld[ac[j]] < 0)
                i--;

        int k = (i / 4) * 3;
        if (i % 4 == 3)
            k += 2;
        if (i % 4 == 2)
            k++;
        byte abyte0[] = new byte[k];
        k = 0;
        int l = 0;
        int i1 = 0;
        for (int j1 = 0; j1 < ac.length; j1++) {
            byte byte0;
            if ((byte0 = ac[j1] <= '\377' ? a_byte_array1d_static_fld[ac[j1]] : -1) >= 0) {
                l <<= 6;
                k += 6;
                l |= byte0;
                if (k >= 8) {
                    k -= 8;
                    abyte0[i1++] = (byte) (l >> k);
                }
            }
        }

        if (i1 != abyte0.length)
            throw new Error("\u6570\u636E\u957F\u5EA6\u4E0D\u4E00\u81F4(\u5B9E\u9645\u5199\u5165\u4E86 " + i1 + "\u5B57\u8282\uFF0C\u4F46\u662F\u7CFB\u7EDF\u6307\u793A\u6709" + abyte0.length + "\u5B57\u8282)");
        else
            return abyte0;
    }
}
