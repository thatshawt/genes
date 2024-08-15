package xyz.davidpineiro.genes.core.problems;

public interface CurriculumSolver<P, S>{
    void solve(Curriculum<P,S> curriculum);
}

// CurriculumSolver<Problem, Solution, Error>
