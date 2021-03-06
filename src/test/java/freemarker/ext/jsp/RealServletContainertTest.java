package freemarker.ext.jsp;

import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import freemarker.ext.servlet.FreemarkerServlet;
import freemarker.test.servlet.DefaultModel2TesterAction;
import freemarker.test.servlet.WebAppTestCase;

/**
 * Tests {@link FreemarkerServlet} on a real (embedded) Servlet container.
 */
@SuppressWarnings("boxing")
public class RealServletContainertTest extends WebAppTestCase {

    private static final String WEBAPP_BASIC = "basic";
    private static final String WEBAPP_EL_FUNCTIONS = "elFunctions";
    private static final String WEBAPP_TLD_DISCOVERY = "tldDiscovery";

    @Test
    public void basicTrivial() throws Exception {
        assertJSPAndFTLOutputEquals(WEBAPP_BASIC, "tester?view=trivial");
    }

    @Test
    @Ignore  // c:forEach fails because of EL context issues
    public void basicTrivialJSTL() throws Exception {
        assertOutputsEqual(WEBAPP_BASIC, "tester?view=trivial.jsp", "tester?view=trivial-jstl-@Ignore.ftl");        
    }

    @Test
    public void basicCustomTags1() throws Exception {
        assertExpectedEqualsOutput(WEBAPP_BASIC, "customTags1.txt", "tester?view=customTags1.ftl", false);
    }

    @Test
    public void basicCustomAttributes() throws Exception {
        restartWebAppIfStarted(WEBAPP_BASIC);  // To clear the application scope attributes
        assertExpectedEqualsOutput(WEBAPP_BASIC, "attributes.txt", "tester"
                + "?action=" + AllKindOfContainersModel2Action.class.getName()
                + "&view=attributes.ftl");

        restartWebAppIfStarted(WEBAPP_BASIC);  // To clear the application scope attributes
        assertExpectedEqualsOutput(WEBAPP_BASIC, "attributes-2.3.22-future.txt", "tester"
                + "?action=" + AllKindOfContainersModel2Action.class.getName()
                + "&view=attributes.ftl&viewServlet=freemarker-2.3.22-future");
        
        restartWebAppIfStarted(WEBAPP_BASIC);  // To clear the application scope attributes
        assertExpectedEqualsOutput(WEBAPP_BASIC, "attributes-2.3.0.txt", "tester"
                + "?action=" + AllKindOfContainersModel2Action.class.getName()
                + "&view=attributes.ftl&viewServlet=freemarker-2.3.0",
                true,
                ImmutableList.<Pattern>of(
                        Pattern.compile("(?<=^Date-time: ).*", Pattern.MULTILINE), // Uses Date.toString, so plat. dep.
                        Pattern.compile("(?<=^MyMap: ).*", Pattern.MULTILINE)  // Uses HashMap, so order unknown
                        ));
    }

    @Test
    public void elFunctions() throws Exception {
        //System.out.println(getResponseContent(WEBAPP_EL_FUNCTIONS, "tester?view=1.jsp"));
        //System.out.println(getResponseContent(WEBAPP_EL_FUNCTIONS, "tester?view=1.ftl"));
        assertJSPAndFTLOutputEquals(WEBAPP_EL_FUNCTIONS, "tester?view=1");
    }
    
    @Test
    public void tldDiscoveryBasic() throws Exception {
        try {
            restartWebAppIfStarted(WEBAPP_TLD_DISCOVERY);
            assertExpectedEqualsOutput(WEBAPP_TLD_DISCOVERY, "test1.txt", "tester?view=test1.ftl");
        } finally {
            JspTestFreemarkerServlet.resetToDefaults();
        }
    }

    @Test
    public void tldDiscoveryEmulatedProblems1() throws Exception {
        try {
            JspTestFreemarkerServlet.emulateNoJarURLConnections = true;
            restartWebAppIfStarted(WEBAPP_TLD_DISCOVERY);
            assertExpectedEqualsOutput(WEBAPP_TLD_DISCOVERY, "test1.txt", "tester?view=test1.ftl");
        } finally {
            JspTestFreemarkerServlet.resetToDefaults();
        }       
    }

    @Test
    public void tldDiscoveryEmulatedProblems2() throws Exception {
        try {
            JspTestFreemarkerServlet.emulateNoJarURLConnections = true;
            JspTestFreemarkerServlet.emulateNoUrlToFileConversions = true;
            // Because of emulateNoUrlToFileConversions = true it won't be able to list the directories, so:
            System.setProperty(
                    FreemarkerServlet.SYSTEM_PROPERTY_CLASSPATH_TLDS,
                    "META-INF/tldDiscovery MetaInfTldSources-1.tld");
            restartWebAppIfStarted(WEBAPP_TLD_DISCOVERY);
            assertExpectedEqualsOutput(WEBAPP_TLD_DISCOVERY, "test1.txt", "tester?view=test1.ftl");
        } finally {
            JspTestFreemarkerServlet.resetToDefaults();
            System.clearProperty(FreemarkerServlet.SYSTEM_PROPERTY_CLASSPATH_TLDS);
        }
    }

    @Test
    public void tldDiscoveryClasspathOnly() throws Exception {
        try {
            System.setProperty(FreemarkerServlet.SYSTEM_PROPERTY_META_INF_TLD_SOURCES, "clear, classpath");
            restartWebAppIfStarted(WEBAPP_TLD_DISCOVERY);
            assertExpectedEqualsOutput(WEBAPP_TLD_DISCOVERY, "test1.txt", "tester?view=test1.ftl");
        } finally {
            JspTestFreemarkerServlet.resetToDefaults();
            System.clearProperty(FreemarkerServlet.SYSTEM_PROPERTY_META_INF_TLD_SOURCES);
        }
    }

    /**
     * Tests that (1) webInfPerLibJars still loads from WEB-INF/lib/*.jar, and (2) that
     * {@link FreemarkerServlet#SYSTEM_PROPERTY_META_INF_TLD_SOURCES} indeed overrides the init-param, and that the
     * Jetty container's JSTL jar-s will still be discovered.
     */
    @Test
    public void tldDiscoveryNoClasspath() throws Exception {
        try {
            System.setProperty(FreemarkerServlet.SYSTEM_PROPERTY_META_INF_TLD_SOURCES, "clear, webInfPerLibJars");
            restartWebAppIfStarted(WEBAPP_TLD_DISCOVERY);
            assertExpectedEqualsOutput(WEBAPP_TLD_DISCOVERY,
                    "test-noClasspath.txt", "tester?view=test-noClasspath.ftl");
        } finally {
            JspTestFreemarkerServlet.resetToDefaults();
            System.clearProperty(FreemarkerServlet.SYSTEM_PROPERTY_META_INF_TLD_SOURCES);
        }
    }
    
    @Test
    public void tldDiscoveryRelative() throws Exception {
        assertExpectedEqualsOutput(WEBAPP_TLD_DISCOVERY, "subdir/test-rel.txt", "tester?view=subdir/test-rel.ftl");
    }
    
    public static class AllKindOfContainersModel2Action extends DefaultModel2TesterAction {

        public String execute(HttpServletRequest req, HttpServletResponse resp) throws Exception {
            req.setAttribute("linkedList", initTestCollection(new LinkedList<Integer>()));
            req.setAttribute("arrayList", initTestCollection(new ArrayList<Integer>()));
            req.setAttribute("myList", new MyList());
            
            req.setAttribute("linkedHashMap", initTestMap(new LinkedHashMap()));
            req.setAttribute("treeMap", initTestMap(new TreeMap()));
            req.setAttribute("myMap", new MyMap());
            
            req.setAttribute("treeSet", initTestCollection(new TreeSet()));
            
            return super.execute(req, resp);
        }
        
        private Collection<Integer> initTestCollection(Collection<Integer> list) {
            for (int i = 0; i < 3; i++) list.add(i + 1);
            return list;
        }

        private Map<String, Integer> initTestMap(Map<String, Integer> map) {
            for (int i = 0; i < 3; i++) map.put(String.valueOf((char) ('a' + i)), i + 1);
            return map;
        }
        
    }
    
    public static class MyMap extends AbstractMap<String, Integer> {

        @Override
        public Set<Map.Entry<String, Integer>> entrySet() {
            return ImmutableSet.<Map.Entry<String, Integer>>of(
                    new MyEntry("a", 1), new MyEntry("b", 2), new MyEntry("c", 3));
        }
        
        private static class MyEntry implements Map.Entry<String, Integer> {
            
            private final String key;
            private final Integer value;

            public MyEntry(String key, Integer value) {
                this.key = key;
                this.value = value;
            }

            public String getKey() {
                return key;
            }

            public Integer getValue() {
                return value;
            }

            public Integer setValue(Integer value) {
                throw new UnsupportedOperationException();
            }
            
        }
        
    }
    
    public static class MyList extends AbstractList<Integer> {

        @SuppressWarnings("boxing")
        @Override
        public Integer get(int index) {
            return index + 1;
        }

        @Override
        public int size() {
            return 3;
        }
        
    }
    
}
