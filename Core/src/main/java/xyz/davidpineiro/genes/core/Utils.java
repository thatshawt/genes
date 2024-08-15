package xyz.davidpineiro.genes.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.RandomAccess;

public final class Utils {

    private static final Random random = new Random();
    private static final String ASCII_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";

    public static boolean chance(float percent){
        return  percent > random.nextFloat();
    }

    public static char getRandomPrintableChar(){
        return ASCII_CHARS.charAt(random.nextInt(ASCII_CHARS.length()));
    }

    public static String getRandomPrintableString(int length){
        StringBuilder builder = new StringBuilder();
        for(int i=0;i<length;i++){
            builder.append(getRandomPrintableChar());
        }
        return builder.toString();
    }

    public static <E> E pickRandom(ArrayList<E> stuff){
        int yea = random.nextInt(stuff.size());
        return stuff.get(yea);
    }

    public static boolean betweenInc(int a, int b,int c){
        return b <= a  && a <= c;
    }

    public static boolean betweenInc(float a, float b, float c){
        return b <= a  && a <= c;
    }

}
