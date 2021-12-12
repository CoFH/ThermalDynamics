package cofh.thermal.dynamics.handler;

import com.google.common.graph.Graph;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author covers1624
 */
@SuppressWarnings ("UnstableApiUsage")
public class GraphHelper {

    /**
     * Checks if <code>a</code> and <code>b</code> have indirect connectivity through the {@link Graph}.
     * <p>
     * This function will use object identity in order to identify <code>b</code> node.
     *
     * @param graph The graph to check.
     * @param a     The first node.
     * @param b     The second node.
     * @return If <code>a</code> and <code>b</code> are connected indirectly.
     */
    public static <T> boolean hasConnectivity(Graph<T> graph, T a, T b) {

        HashSet<T> seen = new HashSet<>();
        LinkedList<T> queue = new LinkedList<>();
        for (T adj : graph.adjacentNodes(a)) {
            // Ignore direct edge between a - b.
            if (adj == b || !seen.add(adj)) continue;
            queue.add(adj);
        }
        while (!queue.isEmpty()) {
            T t = queue.poll();
            // We found the edge!
            if (t == b) return true;
            for (T adj : graph.adjacentNodes(t)) {
                if (seen.add(adj)) {
                    queue.add(adj);
                }
            }
        }
        return false;
    }

    /**
     * Extracts a list of sub graphs from the given {@link Graph}.
     * <p>
     * A sub-graph is identified by a set of nodes which don't have any edges
     * connecting them to another set of nodes.
     *
     * @param graph The {@link Graph}
     * @return A list of isolated sub graphs.
     */
    public static <T> List<Set<T>> separateGraphs(Graph<T> graph) {

        Set<T> seen = new HashSet<>();
        LinkedList<T> stack = new LinkedList<>();
        List<Set<T>> separated = new LinkedList<>();
        while (true) {
            T first = null;
            // Find next node in graph we haven't seen.
            for (T node : graph.nodes()) {
                if (!seen.contains(node)) {
                    first = node;
                    break;
                }
            }
            // We have discovered all nodes, exit.
            if (first == null) break;

            // Start recursively building out all nodes in this sub-graph
            Set<T> subGraph = new HashSet<>();
            assert stack.isEmpty();
            stack.push(first);
            while (!stack.isEmpty()) {
                T entry = stack.pop();
                if (seen.contains(entry)) continue;
                stack.addAll(graph.adjacentNodes(entry));
                seen.add(entry);
                subGraph.add(entry);
            }
            separated.add(subGraph);
        }

        return separated;
    }

}
