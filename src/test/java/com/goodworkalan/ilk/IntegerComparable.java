package com.goodworkalan.ilk;

/**
 * A comparable implementation to test interfaces with explicit types.
 *  
 * @author Alan Gutierrez
 */
public class IntegerComparable implements Comparable<Integer> {
    /**
     * Do nothing comparison always returns true.
     * 
     * @param o
     *            The integer to compare.
     * @return Always zero.
     */
    public int compareTo(Integer o) {
        return 0;
    }   
}