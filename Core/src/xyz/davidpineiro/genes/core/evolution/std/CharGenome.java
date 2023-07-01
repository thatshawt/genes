package xyz.davidpineiro.genes.core.evolution.std;

import xyz.davidpineiro.genes.core.Utils;
import xyz.davidpineiro.genes.core.evolution.Gene;
import xyz.davidpineiro.genes.core.evolution.Genome;
import xyz.davidpineiro.genes.core.evolution.GenomeFactory;

public class CharGenome extends Genome<CharGenome.CharGene> {

    public final String id = Utils.getRandomPrintableString(7);

    @Override
    protected Genome<CharGene> getEmpty() {
        return new CharGenome();
    }

    public static class CharGene implements Gene {

        private char value;
        private boolean active = true;

        public CharGene(char value) {
            this.value = value;
        }

        public char getValue() {
            return value;
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public void mutateGene(float geneMutateChance, float activeMutateChance) {
            if(Utils.chance(geneMutateChance))   value = Utils.getRandomPrintableChar();
            if(Utils.chance(activeMutateChance)) active = !active;
        }

        public static CharGene getRandom(){
            CharGene gene = new CharGene('a');
            gene.mutateGene(1.0f, 0.0f);
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
            try {
                CharGene clone = (CharGene) super.clone();
                clone.value = this.value;
                return clone;
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
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
