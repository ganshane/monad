package monad.node.internal;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import org.junit.Test;

/**
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 */
public class JNATest {

    /*
    public static native double cos(double x);
    public static native double sin(double x);

    static {
        Native.register(Platform.C_LIBRARY_NAME);
    }
    */
    @Test
    public void test_performance() {
        output();
        output();
    }

    private void output() {
        int r = 0;
        for (int i = 0; i < 50000000; i++) {
            r += 1;
        }
        System.out.println(r);
    }

    @Test
    public void test_jna() {
        long pointerValue = Native.malloc(10);
        Pointer pointer = new Pointer(pointerValue);
        pointer.setInt(2, 123);
        int value = pointer.getInt(2);
        System.out.println(value);

        Native.free(pointerValue);

        /*
        System.out.println("cos(0)=" + cos(0));
        System.out.println("sin(0)=" + sin(0));
        */
    }
}
