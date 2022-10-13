package cofh.thermal.dynamics.handler;

import com.google.common.graph.Graph;
import com.google.common.graph.ValueGraph;

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
