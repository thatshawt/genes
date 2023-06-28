package xyz.davidpineiro.genes.core;

import java.util.Random;

public final class Utils {

    private static final Random random = new Random();
    private static final String ASCII_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";

    public static boolean chance(float percent){
        return  percent >= random.nextFloat();
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

}
