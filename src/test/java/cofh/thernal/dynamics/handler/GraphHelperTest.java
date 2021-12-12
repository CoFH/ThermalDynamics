package cofh.thernal.dynamics.handler;

import cofh.thermal.dynamics.handler.GraphHelper;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static cofh.thermal.dynamics.handler.GraphHelper.hasConnectivity;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author covers1624
 */
@SuppressWarnings ("UnstableApiUsage")
public class GraphHelperTest {

    @Test
    public void testHasConnectivity() {
        Object a = new Object();
        Object b = new Object();
        Object edge = new Object();
        MutableGraph<Object> graph = GraphBuilder.undirected().build();
        graph.addNode(a);
        graph.addNode(b);
        graph.addNode(edge);

        graph.putEdge(a, edge);
        graph.putEdge(b, edge);

        assertTrue(hasConnectivity(graph, a, b));
        graph.removeEdge(a, edge);
        assertFalse(hasConnectivity(graph, a, b));

        // Ignores direct edge.
        graph.putEdge(a, b);
        assertFalse(hasConnectivity(graph, a, b));
    }

    @Test
    public void testSeparateGraphs() {
        Object a = new Object();
        Object b = new Object();
        Object edge = new Object();
        MutableGraph<Object> graph = GraphBuilder.undirected().build();
        graph.addNode(a);
        graph.addNode(b);
        graph.addNode(edge);

        graph.putEdge(a, edge);
        graph.putEdge(b, edge);

        {
            List<Set<Object>> separated = GraphHelper.separateGraphs(graph);
            assertEquals(1, separated.size());
            Set<Object> set = separated.get(0);
            assertTrue(set.contains(a));
            assertTrue(set.contains(b));
            assertTrue(set.contains(edge));
        }

        {
            graph.removeEdge(a, edge);
            List<Set<Object>> separated = GraphHelper.separateGraphs(graph);
            assertEquals(2, separated.size());
            Set<Object> set1 = separated.get(0);
            Set<Object> set2 = separated.get(1);
            if (set1.contains(a)) {
                assertTrue(set2.contains(b));
                assertTrue(set2.contains(edge));
            } else {
                assertTrue(set1.contains(b));
                assertTrue(set1.contains(edge));
            }
        }
    }
}
