package io.github.zebalu.graph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

public class TestGraph {
    public static void main(String[] args) throws Exception {
        article();
        example();
        puzzle();
    }

    private static void article() {
        Graph<Integer> graph = new Graph<>();
        graph.addEdge(1, 2, 2);
        graph.addEdge(1, 5, 3);
        graph.addEdge(5, 6, 3);
        graph.addEdge(5, 2, 2);
        graph.addEdge(2, 6, 2);
        graph.addEdge(2, 3, 3);
        graph.addEdge(6, 7, 1);
        graph.addEdge(3, 7, 2);
        graph.addEdge(3, 4, 4);
        graph.addEdge(7, 4, 2);
        graph.addEdge(7, 8, 3);
        graph.addEdge(4, 8, 2);
        cut(graph);
    }

    private static void example() {
        String example = """
                jqt: rhn xhk nvd
                rsh: frs pzl lsr
                xhk: hfx
                cmg: qnr nvd lhk bvb
                rhn: xhk bvb hfx
                bvb: xhk hfx
                pzl: lsr hfx nvd
                qnr: nvd
                ntq: jqt hfx bvb xhk
                nvd: lhk
                lsr: lhk
                rzs: qnr cmg lsr rsh
                frs: qnr lhk lsr""";
        cutStrGraph(example);
    }

    private static void puzzle() throws IOException {
        try {
            cutStrGraph(Files.readString(Path.of("input.txt").toAbsolutePath()));
        } catch (NoSuchFileException e) {
            System.err.println("Can not find: "+e.getMessage());
            System.err.println("Do you have this input file? It should contain: https://adventofcode.com/2023/day/25/input");
        }
    }

    private static void cutStrGraph(String input) {
        Graph<String> graph = new Graph<>();
        input.lines().forEach(line -> {
            String[] split = line.split(": ");
            String from = split[0];
            for (String to : split[1].split(" ")) {
                graph.addEdge(from, to, 1.0);
            }
        });
        cut(graph);
    }

    private static <T> void cut(Graph<T> graph) {
        Instant start = Instant.now();
        Graph.StoerWagner<T> stoerWagner = new Graph.StoerWagner<>(graph);
        Instant end = Instant.now();
        System.out.println("partition group 1: "+stoerWagner.getPartition1().getVertices());
        System.out.println("partition group 2: "+stoerWagner.getPartition2().getVertices());
        System.out.println("edges cut: "+stoerWagner.getCutEdges());
        System.out.println("cost of cut: "+stoerWagner.getBestWeight());
        System.out.println("aoc answer: "+stoerWagner.getPartition1().vertexCount() * stoerWagner.getPartition2().vertexCount());
        System.out.println("time: "+ Duration.between(start, end).toMillis()+" ms");
    }
}
