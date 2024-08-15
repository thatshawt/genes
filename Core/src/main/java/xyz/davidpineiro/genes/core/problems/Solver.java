package xyz.davidpineiro.genes.core.problems;

public interface Solver<P,S> {
    S solve(P problem);
}
