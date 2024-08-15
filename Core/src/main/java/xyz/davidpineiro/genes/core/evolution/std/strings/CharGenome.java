package xyz.davidpineiro.genes.core.evolution.std.strings;

import xyz.davidpineiro.genes.core.Utils;
import xyz.davidpineiro.genes.core.evolution.AbstractGene;
import xyz.davidpineiro.genes.core.evolution.IGene;
import xyz.davidpineiro.genes.core.evolution.Genome;
import xyz.davidpineiro.genes.core.evolution.GenomeFactory;

public class CharGenome extends Genome<CharGenome.CharGene> {

    public final String id = Utils.getRandomPrintableString(7);

    @Override
    protected Genome<CharGene> getEmpty() {
        return new CharGenome();
    }

    public static class CharGene extends AbstractGene {

        private char value;

        public CharGene(char value) {
            this.value = value;
        }

        public char getValue() {
            return value;
        }

        @Override
        public void mutate() {
            value = Utils.getRandomPrintableChar();
        }

        public static CharGene getRandom(){
            CharGene gene = new CharGene('a');
            gene.mutate();
            return gene;
        }

        @Override
        public String toString() {
            return "CharGene{" +
                    "value=" + value +
                    ", active=" + active +
                    '}';
        }

        @Override
        public CharGene clone() {
            return new CharGene(this.value);
        }
    }

    public static Genome<CharGene> getRandomGenome() {
        return GenomeFactory.getRandomGenome(new CharGenome(), CharGene::getRandom, 10);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("{id:'%s'}[", this.id));
        for(CharGene gene : this){
            builder.append(String.format("%s%s, ", gene.isActive() ? "" : "!", gene.value));
        }
        builder.append("]");
        return builder.toString();
    }

    public static CharGenome fromString(String s){
        CharGenome init = new CharGenome();
        for(char c : s.toCharArray()){
            init.add(new CharGene(c));
        }
        return init;
    }
}
