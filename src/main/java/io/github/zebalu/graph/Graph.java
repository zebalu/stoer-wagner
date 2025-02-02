package io.github.zebalu.graph;

import java.util.*;

public class Graph<T> {
    public static class Edge<T> {
        private final T from;
        private final T to;
        private final double weight;

        public Edge(T from, T to, double weight) {
            if (from == null || to == null || from.equals(to)) {
                throw new IllegalArgumentException(from + " and " + to + " cannot be null or equals");
            }
            this.from = from;
            this.to = to;
            this.weight = weight;
        }

        public T getFrom() {
            return from;
        }

        public T getTo() {
            return to;
        }

        public double getWeight() {
            return weight;
        }

        public T getOther(T v) {
            if (from.equals(v)) {
                return to;
            } else if (to.equals(v)) {
                return from;
            } else {
                throw new IllegalArgumentException(v + " is not on the edge: " + from + " and " + to);
            }
        }

        @Override
        public String toString() {
            return "Edge[" + from + " --> " + to + ", " + weight + "]";
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof Edge<?> e) {
                return (this.from.equals(e.from) && this.to.equals(e.to)) || (this.to.equals(e.from) && this.from.equals(e.to));
            }
            return false;
        }

        @Override
        public int hashCode() {
            return from.hashCode() + to.hashCode();
        }
    }

    private final Map<T, Set<T>> vertexMap = new HashMap<>();
    private final Map<T, Set<Edge<T>>> edgeMap = new HashMap<>();
    private final Set<Edge<T>> edges = new HashSet<>();

    public void addVertex(T vertex) {
        vertexMap.computeIfAbsent(vertex, __ -> new HashSet<>());
        edgeMap.computeIfAbsent(vertex, __ -> new HashSet<>());
    }

    public void addEdge(T from, T to, double weight) {
        addEdge(new Edge<>(from, to, weight));
    }

    public void addEdge(Edge<T> edge) {
        addVertex(edge.getFrom());
        addVertex(edge.getTo());
        vertexMap.get(edge.getFrom()).add(edge.getTo());
        vertexMap.get(edge.getTo()).add(edge.getFrom());
        edgeMap.get(edge.getFrom()).add(edge);
        edgeMap.get(edge.getTo()).add(edge);
        edges.add(edge);
    }

    public void removeEdge(Edge<T> edge) {
        edges.remove(edge);
        vertexMap.get(edge.from).remove(edge.to);
        vertexMap.get(edge.to).remove(edge.from);
        edgeMap.get(edge.from).remove(edge);
        edgeMap.get(edge.to).remove(edge);
    }

    public void removeVertex(T vertex) {
        List<Edge<T>> toProcess = new ArrayList<>(edgeMap.get(vertex));
        for (var edge : toProcess) {
            T other = edge.getOther(vertex);
            vertexMap.get(other).remove(vertex);
            edgeMap.get(other).remove(edge);
            edges.remove(edge);
        }
        vertexMap.remove(vertex);
    }

    public Set<Edge<T>> getEdges() {
        return Collections.unmodifiableSet(edges);
    }

    public Set<Edge<T>> getEdgesOf(T vertex) {
        return Collections.unmodifiableSet(edgeMap.get(vertex));
    }

    public Set<T> getVertices() {
        return Collections.unmodifiableSet(vertexMap.keySet());
    }

    public int vertexCount() {
        return vertexMap.size();
    }

    public int edgeCount() {
        return edgeMap.size();
    }

    public static class StoerWagner<T> {
        private final Graph<Integer> workingGraph = new Graph<>();
        private final Graph<T> originalGraph;
        private final Map<Integer, Set<T>> intToSet = new HashMap<>();
        private final Map<Set<T>, Integer> setToInt = new HashMap<>();
        private Set<T> bestCut;
        private double bestWeight = Double.MAX_VALUE;
        private final Graph<T> partition1 = new Graph<>();
        private final Graph<T> partition2 = new Graph<>();
        private final Set<Edge<T>> cutEdges = new HashSet<>();

        public StoerWagner(Graph<T> graph) {
            this.originalGraph = graph;
            Map<T, Set<T>> helper = new HashMap<>();
            graph.getVertices().forEach(vertex -> {
                Set<T> set = new HashSet<>(Set.of(vertex));
                int id = registerSet(set);
                helper.put(vertex, set);
                workingGraph.addVertex(id);
            });
            graph.getEdges().forEach(edge -> {
                Set<T> from = helper.get(edge.getFrom());
                Set<T> to = helper.get(edge.getTo());
                workingGraph.addEdge(setToInt.get(from), setToInt.get(to), edge.getWeight());
            });
            findBestCut();
            buildPartitions();
        }

        public Graph<T> getPartition1() {
            return partition1;
        }

        public Graph<T> getPartition2() {
            return partition2;
        }

        public Set<Edge<T>> getCutEdges() {
            return Collections.unmodifiableSet(cutEdges);
        }

        public double getBestWeight() {
            return bestWeight;
        }

        private void findBestCut() {
            while (workingGraph.vertexCount() > 1) {
                minCutPhase();
            }
        }

        private int registerSet(Set<T> set) {
            int id = setToInt.computeIfAbsent(set, s -> setToInt.size());
            intToSet.put(id, set);
            return id;
        }

        private void minCutPhase() {
            record VertexCost(int vertex, double cost) {
            }
            Map<Integer, VertexCost> costMap = new HashMap<>();
            int stratVertex = workingGraph.getVertices().iterator().next();
            VertexCost startCost = new VertexCost(stratVertex, 0.0);
            costMap.put(stratVertex, startCost);
            SequencedSet<VertexCost> queue = new TreeSet<>(Comparator.<VertexCost>comparingDouble(VertexCost::cost).reversed());
            queue.add(startCost);
            int last = -1;
            int beforeLast = -1;
            Set<Integer> processed = new HashSet<>();
            double lastSumWeight = Double.MIN_VALUE;
            while (!queue.isEmpty() && !costMap.isEmpty()) {
                VertexCost vc = queue.removeFirst();
                processed.add(vc.vertex());
                beforeLast = last;
                last = vc.vertex();
                lastSumWeight = vc.cost();
                costMap.remove(vc.vertex());
                for (var edge : workingGraph.getEdgesOf(vc.vertex())) {
                    var other = edge.getOther(vc.vertex());
                    if (!processed.contains(other)) {
                        var currWeight = costMap.getOrDefault(other, new VertexCost(other, 0.0));
                        VertexCost nextWeight = new VertexCost(other, currWeight.cost() + edge.getWeight());
                        queue.remove(currWeight);
                        queue.add(nextWeight);
                        costMap.put(other, nextWeight);
                    }
                }
            }
            if (lastSumWeight < bestWeight) {
                bestWeight = lastSumWeight;
                bestCut = intToSet.get(last);
            }
            merge(beforeLast, last);
        }

        private void merge(int beforeLast, int last) {
            Set<T> lastSet = intToSet.get(last);
            Set<T> beforeSet = intToSet.get(beforeLast);
            Set<T> merged = HashSet.newHashSet(lastSet.size() + beforeSet.size());
            merged.addAll(lastSet);
            merged.addAll(beforeSet);
            int newId = registerSet(merged);
            Map<Integer, Double> sumMap = new HashMap<>();
            sumWeights(beforeLast, last, sumMap);
            sumWeights(last, beforeLast, sumMap);
            workingGraph.removeVertex(beforeLast);
            workingGraph.removeVertex(last);
            workingGraph.addVertex(newId);
            sumMap.forEach((key, value) -> workingGraph.addEdge(newId, key, value));
        }

        private void sumWeights(int from, int not, Map<Integer, Double> sumMap) {
            for (var edge : workingGraph.getEdgesOf(from)) {
                int other = edge.getOther(from);
                if (other != not) {
                    sumMap.compute(other, (k, v) -> edge.getWeight() + (v != null ? v : 0.0));
                }
            }
        }

        private void buildPartitions() {
            bestCut.forEach(partition1::addVertex);
            originalGraph.getVertices().stream().filter(v -> !bestCut.contains(v)).forEach(partition2::addVertex);
            originalGraph.getEdges().forEach(edge -> {
                if (bestCut.contains(edge.getFrom()) && bestCut.contains(edge.getTo())) {
                    partition1.addEdge(edge);
                } else if (!bestCut.contains(edge.getFrom()) && !bestCut.contains(edge.getTo())) {
                    partition2.addEdge(edge);
                } else {
                    cutEdges.add(edge);
                }
            });
        }
    }

}
