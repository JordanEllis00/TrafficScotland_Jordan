package org.me.gcu.trafficscotland_jordan;

import java.lang.reflect.Method;
import java.util.*;

/** Minimal JUnit-style runner for the command-line harness (Android Studio uses real JUnit 4). */
public class Runner {
    public static void main(String[] a) throws Exception {
        Class<?>[] classes = { DatexXmlParserTest.class, TrafficSituationTest.class, SituationFilterTest.class };
        int pass = 0, fail = 0;
        for (Class<?> c : classes) {
            System.out.println("Running " + c.getName());
            List<Method> ms = new ArrayList<>(Arrays.asList(c.getDeclaredMethods()));
            ms.sort(Comparator.comparing(Method::getName));
            for (Method m : ms) {
                if (m.getAnnotation(org.junit.Test.class) == null) continue;
                try { m.invoke(c.getDeclaredConstructor().newInstance()); System.out.printf("  PASS  %s%n", m.getName()); pass++; }
                catch (java.lang.reflect.InvocationTargetException e) { System.out.printf("  FAIL  %s  -> %s%n", m.getName(), e.getCause()); fail++; }
            }
        }
        System.out.printf("%nTests run: %d, Passed: %d, Failed: %d%n", pass + fail, pass, fail);
        System.exit(fail == 0 ? 0 : 1);
    }
}
