package xyz.davidpineiro.genes.core.evolution;

public interface GenomeFactory<E extends IGene>{
    Genome<E> randomGenome();

    static <E extends IGene> Genome<E> getRandomGenome(
            Genome<E> firstEmptyGenome,
            GeneFactory<E> geneFactory,
            int n
    ){
        for(int i=0;i<n;i++){
            firstEmptyGenome.add(geneFactory.randomGene());
        }
        return firstEmptyGenome;
    }
}
