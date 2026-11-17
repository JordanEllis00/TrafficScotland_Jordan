package org.junit;
/** TEST SHIM: the few JUnit 4 assertions the tests use (Android Studio uses the real org.junit.Assert). */
public class Assert {
    public static void assertEquals(Object e, Object a){ if(!java.util.Objects.equals(e,a)) throw new AssertionError("expected:<"+e+"> but was:<"+a+">"); }
    public static void assertEquals(long e, long a){ if(e!=a) throw new AssertionError("expected:<"+e+"> but was:<"+a+">"); }
    public static void assertEquals(String m,Object e, Object a){ if(!java.util.Objects.equals(e,a)) throw new AssertionError(m+" expected:<"+e+"> but was:<"+a+">"); }
    public static void assertEquals(double e,double a,double d){ if(Math.abs(e-a)>d) throw new AssertionError("expected:<"+e+"> but was:<"+a+"> (delta "+d+")"); }
    public static void assertEquals(String m,double e,double a,double d){ if(Math.abs(e-a)>d) throw new AssertionError(m+" expected:<"+e+"> but was:<"+a+"> (delta "+d+")"); }
    public static void assertTrue(String m, boolean c){ if(!c) throw new AssertionError(m); }
    public static void assertTrue(boolean c){ if(!c) throw new AssertionError(); }
    public static void assertFalse(boolean c){ if(c) throw new AssertionError(); }
    public static void assertFalse(String m, boolean c){ if(c) throw new AssertionError(m); }
}
