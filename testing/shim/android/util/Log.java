package android.util;
/** TEST SHIM (JVM harness only): prints error lines so they appear in the evidence. */
public class Log {
    public static int e(String t, String m, Throwable th){ System.out.println("  [Log.e] "+t+": "+m+" -> "+th.getClass().getSimpleName()+": "+th.getMessage()); return 0; }
    public static int w(String t, String m, Throwable th){ return 0; }
    public static int w(String t, String m){ return 0; }
}
