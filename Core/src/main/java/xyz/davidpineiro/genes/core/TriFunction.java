package xyz.davidpineiro.genes.core;

import java.util.function.Function;

@FunctionalInterface
public interface TriFunction<A,B,C,D> {

    D apply(A a, B b, C d);

}
