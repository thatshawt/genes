package xyz.davidpineiro.genes.core.evolution.exceptions;

import xyz.davidpineiro.genes.core.evolution.IGene;
import xyz.davidpineiro.genes.core.evolution.Genome;

public class EvolutionTickLimitException extends Exception{

    public final Genome<?> bestGenome;

    public <E extends IGene> EvolutionTickLimitException(Genome<E> bestGenome) {
            this.bestGenome = bestGenome;
    }

    @Override
    public String toString() {
        return "EvolutionTickLimitException{" +
                "bestGenome=" + bestGenome +
                '}';
    }
}
