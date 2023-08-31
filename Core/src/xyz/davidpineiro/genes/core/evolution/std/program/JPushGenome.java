package xyz.davidpineiro.genes.core.evolution.std.program;

import xyz.davidpineiro.genes.core.evolution.AbstractGene;
import xyz.davidpineiro.genes.core.evolution.IGene;
import xyz.davidpineiro.genes.core.evolution.Genome;

public class JPushGenome extends Genome<JPushGenome.JPushGene> {
    @Override
    protected Genome<JPushGene> getEmpty() {
        return new JPushGenome();
    }

    public static class JPushGene extends AbstractGene {
        @Override
        public void mutate() {

        }

        @Override
        public IGene clone() {
            return null;
        }
    }
}
