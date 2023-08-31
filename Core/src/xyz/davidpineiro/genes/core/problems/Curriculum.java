package xyz.davidpineiro.genes.core.problems;

import java.util.List;

public interface Curriculum<P,S> extends List<Curriculum.ProblemNode<P, S>> {
    class ProblemNode<P,S>{
        public final P problem;
        public S solution = null;

        public ProblemNode(P problem) {
            this.problem = problem;
        }
    }

}
