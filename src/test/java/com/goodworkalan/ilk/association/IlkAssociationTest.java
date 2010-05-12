package com.goodworkalan.ilk.association;

import static org.testng.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.goodworkalan.ilk.Ilk;

/**
 * Unit tests for the {@link IlkAssociation} class.
 *
 * @author Alan Gutierrez
 */
public class IlkAssociationTest {
    @Test
    public void multiLookup() {
        IlkAssociation<String> assoc = new IlkAssociation<String>(true);
        assoc.assignable(new Ilk<List<String>>() {}.key, "assignableListString");
        assoc.exact(new Ilk<ArrayList<Integer>>() {}.key, "exactArrayListInteger");
        assoc.exact(new Ilk<ArrayList<String>>() {}.key, "exactArrayListString1");
        assoc.exact(new Ilk<ArrayList<String>>() {}.key, "exactArrayListString2");
        assoc.annotated(Three.class, "annotatedThree1");
        assoc.annotated(Three.class, "annotatedThree2");
        List<String> got = assoc.getAll(new Ilk<ArrayList<String>>() {}.key);
        assertEquals(got.size(), 3);
        assertEquals(got.get(0), "exactArrayListString1");
        assertEquals(got.get(1), "exactArrayListString2");
        assertEquals(got.get(2), "assignableListString");
        assertEquals(assoc.get(new Ilk<ArrayList<String>>() {}.key), "exactArrayListString1");
        assertNull(assoc.get(new Ilk<ArrayList<Long>>() {}.key));
        assertNull(assoc.get(new Ilk<Long>() {}.key));
        assertEquals(assoc.getAll(new Ilk<Two>() { }.key).get(1), "annotatedThree2");
        assertNull(assoc.get(new Ilk<One>() { }.key));
        IlkAssociation<String> copy = new IlkAssociation<String>(assoc);
        got = copy.getAll(new Ilk<ArrayList<String>>() {}.key);
        assertEquals(got.size(), 3);
        assertEquals(got.get(0), "exactArrayListString1");
        assertEquals(got.get(1), "exactArrayListString2");
        assertEquals(got.get(2), "assignableListString");
    }
    
    @Test
    public void lookup() {
        IlkAssociation<String> assoc = new IlkAssociation<String>(false);
        assoc.assignable(new Ilk<List<String>>() {}.key, "assignableListString");
        assoc.exact(new Ilk<ArrayList<Integer>>() {}.key, "exactArrayListInteger");
        assoc.exact(new Ilk<ArrayList<String>>() {}.key, "exactArrayListString1");
        assoc.exact(new Ilk<ArrayList<String>>() {}.key, "exactArrayListString2");
        assoc.annotated(Three.class, "annotatedThree1");
        assoc.annotated(Three.class, "annotatedThree2");
        List<String> got = assoc.getAll(new Ilk<ArrayList<String>>() {}.key);
        assertEquals(got.size(), 2);
        assertEquals(got.get(0), "exactArrayListString2");
        assertEquals(got.get(1), "assignableListString");
        assertEquals(assoc.get(new Ilk<ArrayList<String>>() {}.key), "exactArrayListString2");
        assertNull(assoc.get(new Ilk<ArrayList<Long>>() {}.key));
        assertNull(assoc.get(new Ilk<Long>() {}.key));
        assertEquals(assoc.getAll(new Ilk<Two>() { }.key).get(0), "annotatedThree2");
        assertNull(assoc.get(new Ilk<One>() { }.key));
    }

}
