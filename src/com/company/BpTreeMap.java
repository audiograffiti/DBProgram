package com.company;


/************************************************************************************
 * @file BpTreeMap.java
 *
 * @author  John Miller
 */

import java.io.*;
import java.lang.reflect.Array;
import static java.lang.System.out;
import java.util.*;

/************************************************************************************
 * This class provides B+Tree maps.  B+Trees are used as multi-level index structures
 * that provide efficient access for both point queries and range queries.
 */
public class BpTreeMap <K extends Comparable <K>, V>
        extends AbstractMap <K, V>
        implements Serializable, Cloneable, SortedMap <K, V>
{
    /** The maximum fanout for a B+Tree node.
     */
    private static final int ORDER = 5;

    /** Key
     */
    private final Class <K> classK;

    /** Value
     */
    private final Class <V> classV;

    /********************************************************************************
     * This inner class defines nodes that are stored in the B+tree map.
     */
    private class Node
    {
        boolean   isLeaf;
        int       nKeys;
        K []      key;
        Object [] ref;
        @SuppressWarnings("unchecked")
        Node (boolean _isLeaf)
        {
            isLeaf = _isLeaf;
            nKeys  = 0;
            key    = (K []) Array.newInstance (classK, ORDER - 1);
            if (isLeaf) {
                //ref = (V []) Array.newInstance (classV, ORDER);
                ref = new Object [ORDER];
            } else {
                ref = (Node []) Array.newInstance (Node.class, ORDER);
            } // if
        } // constructor
    } // Node inner class

    /** The root of the B+Tree
     */
    private final Node root;

    private int nodes;
    /** The counter for the number nodes accessed (for performance testing).
     */
    private int count = 0;

    /********************************************************************************
     * Construct an empty B+Tree map.
     * @param _classK  the class for keys (K)
     * @param _classV  the class for values (V)
     */
    public BpTreeMap (Class <K> _classK, Class <V> _classV)
    {
        classK = _classK;
        classV = _classV;
        root   = new Node (true);
        nodes = 0;
    } // constructor

    /********************************************************************************
     * Return null to use the natural order based on the key type.  This requires the
     * key type to implement Comparable.
     */
    public Comparator <? super K> comparator ()
    {
        return null;
    } // comparator

    /********************************************************************************
     * Return a set containing all the entries as pairs of keys and values.
     * @return  the set view of the map
     */
    public Set <Map.Entry <K, V>> entrySet ()
    {
        Set <Map.Entry <K, V>> enSet = new HashSet <> ();
        allKeyValues(enSet, root);
        //  T O   B E   I M P L E M E N T E D

        return enSet;
    } // entrySet

    private Set<Map.Entry<K, V>> allKeyValues(Set<Map.Entry<K, V>> set, Node n){
        if(n == null){
            return set;
        }
        if(n.isLeaf){
            for(int i = 0; i < n.nKeys; i++){
                final int index = i;
                Entry<K, V> entry = new Entry<K, V>() {
                    @Override
                    public K getKey() {
                        return n.key[index];
                    }

                    @Override
                    public V getValue() {
                        return (V) n.ref[index];
                    }

                    @Override
                    public V setValue(V value) {
                        n.ref[index] = value;
                        return (V) n.ref[index];
                    }
                };
                set.add(entry);
            }
        }
        try{
            set = allKeyValues(set, (Node) n.ref[0]);
            set = allKeyValues(set, (Node) n.ref[1]);
            set = allKeyValues(set, (Node) n.ref[2]);
            set = allKeyValues(set, (Node) n.ref[3]);
            set = allKeyValues(set, (Node) n.ref[4]);
        }catch (ClassCastException e){
            // n is a leaf, no need to do anything
        }
        return set;
    }

    /********************************************************************************
     * Given the key, look up the value in the B+Tree map.
     * @param key  the key used for look up
     * @return  the value associated with the key
     */
    @SuppressWarnings("unchecked")
    public V get (Object key)
    {
        return find ((K) key, root);
    } // get

    /********************************************************************************
     * Put the key-value pair in the B+Tree map.
     * @param key    the key to insert
     * @param value  the value to insert
     * @return  null (not the previous value)
     */
    public V put (K key, V value)
    {
        insert (key, value, root, null);
        return null;
    } // put

    /********************************************************************************
     * Return the first (smallest) key in the B+Tree map.
     * @return  the first key in the B+Tree map.
     */
    public K firstKey ()
    {
        Node n = findSmallestKey(root);
        //  T O   B E   I M P L E M E N T E D

        return n.key[0];
    } // firstKey

    private Node findSmallestKey(Node n){
        if(!n.isLeaf){
            try{
                n = findSmallestKey((Node) n.ref[0]);
            }catch(ClassCastException e){
                // program counter won't reach here because left child of n is a node
            }
        }
        return n;
    }

    /********************************************************************************
     * Return the last (largest) key in the B+Tree map.
     * @return  the last key in the B+Tree map.
     */
    public K lastKey ()
    {
        //  T O   B E   I M P L E M E N T E D
        Node n = findLargestKey(root);
        return n.key[n.nKeys - 1];
    } // lastKey

    private Node findLargestKey(Node n){
        if(!n.isLeaf){
            try{
                n = findLargestKey((Node) n.ref[n.nKeys]);
            }catch(ClassCastException e){
                // program counter won't reach here because right child of n is a node
            }
        }
        return n;
    }

    /********************************************************************************
     * Return the portion of the B+Tree map where key < toKey.
     * @return  the submap with keys in the range [firstKey, toKey)
     */
    public SortedMap <K,V> headMap (K toKey)
    {
        SortedMap<K, V> map = new TreeMap<>();
        map = findHeadMap(map, toKey, root);
        //  T O   B E   I M P L E M E N T E D

        return map;
    } // headMap

    /********************************************************************************
     * Return the portion of the B+Tree map where fromKey <= key.
     * @return  the submap with keys in the range [fromKey, lastKey]
     */
    public SortedMap <K,V> tailMap (K fromKey)
    {
        //  T O   B E   I M P L E M E N T E D
        SortedMap<K, V> map = new TreeMap<>();
        map = findTailMap(map, fromKey, root);
        return map;
    } // tailMap

    /********************************************************************************
     * Return the portion of the B+Tree map whose keys are between fromKey and toKey,
     * i.e., fromKey <= key < toKey.
     * @return  the submap with keys in the range [fromKey, toKey)
     */
    public SortedMap <K,V> subMap (K fromKey, K toKey)
    {
        //  T O   B E   I M P L E M E N T E D
        SortedMap <K,V> map = new TreeMap<>();
        map = findSubMap(map, fromKey, toKey, root);
        return map;
    } // subMap

    private SortedMap<K, V> findSubMap(SortedMap<K, V> map, K fromKey, K toKey, Node n){
        if(n.isLeaf){
            for(int i = 0; i < n.nKeys; i++){
                if(fromKey.compareTo(n.key[i]) <= 0 && n.key[i].compareTo(toKey) < 0){
                    map.put(n.key[i], (V) n.ref[i]);
                }
            }
        }
        else{
            for(int i = 0; i < n.ref.length; i++){
                if(n.ref[i] == null){
                    continue;
                }
                findSubMap(map, fromKey, toKey, (Node) n.ref[i]);
            }
        }
        return map;
    }

    private SortedMap<K, V> findTailMap(SortedMap<K, V> map, K key, Node n){
        if(n.isLeaf){
            for(int i = 0; i < n.nKeys; i++){
                if(key.compareTo(n.key[i]) <= 0){
                    map.put(n.key[i], (V) n.ref[i]);
                }
            }
        }
        else{
            for(int i = 0; i < n.ref.length; i++){
                if(n.ref[i] == null){
                    continue;
                }
                findTailMap(map, key, (Node) n.ref[i]);
            }
        }
        return map;
    }

    private SortedMap<K, V> findHeadMap(SortedMap<K, V> map, K key, Node n){
        if(n.isLeaf){
            for(int i = 0; i < n.nKeys; i++){
                if(n.key[i].compareTo(key) < 0){
                    map.put(n.key[i], (V) n.ref[i]);
                }
            }
        }
        else{
            for(int i = 0; i < n.ref.length; i++) {
                if (n.ref[i] == null) {
                    continue;
                }
                findHeadMap(map, key, (Node) n.ref[i]);
            }
        }
        return map;
    }

    /********************************************************************************
     * Return the size (number of keys) in the B+Tree.
     * @return  the size of the B+Tree
     */
    public int size ()
    {
        return countNodes(root, 0);
        //  T O   B E   I M P L E M E N T E D

    } // size

    private int countNodes(Node n, int sum){
        if(n.isLeaf){
            return n.nKeys;
        }
        else{
            for(int i = 0; i < n.ref.length; i++){
                if(n.ref[i] == null){
                    continue;
                }
                sum += countNodes((Node) n.ref[i], 0);
            }
        }
        return sum;
    }

    /********************************************************************************
     * Print the B+Tree using a pre-order traveral and indenting each level.
     * @param n      the current node to print
     * @param level  the current level of the B+Tree
     */
    @SuppressWarnings("unchecked")
    private void print (Node n, int level)
    {
        if(n == null){
            return;
        }
        out.println ("BpTreeMap");
        out.println ("-------------------------------------------");

        for (int j = 0; j < level; j++) out.print ("\t");
        out.print ("[ . ");
        for (int i = 0; i < n.nKeys; i++) out.print (n.key [i] + " . ");
        out.println ("]");
        if ( ! n.isLeaf) {
            for (int i = 0; i <= n.nKeys; i++) print ((Node) n.ref [i], level + 1);
        } // if

        out.println ("-------------------------------------------");
    } // print

    /********************************************************************************
     * Recursive helper function for finding a key in B+trees.
     * @param key  the key to find
     * @param n the current node
     */
    @SuppressWarnings("unchecked")
    private V find (K key, Node n)
    {
        count++;
        for (int i = 0; i < n.nKeys; i++) {
            K k_i = n.key [i];
            if (key.compareTo (k_i) <= 0) {
                if (n.isLeaf) {
                    return (key.equals (k_i)) ? (V) n.ref [i] : null;
                } else {
                    return find (key, (Node) n.ref [i]);
                } // if
            } // if
        } // for
        return (n.isLeaf) ? null : find (key, (Node) n.ref [n.nKeys]);
    } // find

    /********************************************************************************
     * Recursive helper function for inserting a key in B+trees.
     * @param key  the key to insert
     * @param ref  the value/node to insert
     * @param n    the current node
     * @param p    the parent node
     */
    private void insert (K key, V ref, Node n, Node p)
    {
        if (n.nKeys < ORDER - 1) {
            for (int i = 0; i < n.nKeys; i++) {
                K k_i = n.key [i];
                if (key.compareTo (k_i) < 0) {
                    wedge (key, ref, n, i);
                } else if (key.equals (k_i)) {
                    out.println ("BpTreeMap:insert: attempt to insert duplicate key = " + key);
                } // if
            } // for
            wedge (key, ref, n, n.nKeys);
        }
        else {
            Node newRoot = split (key, ref, n);
            root.isLeaf = false;
            root.key = newRoot.key;
            root.ref = newRoot.ref;
            root.nKeys = newRoot.nKeys;
            //  T O   B E   I M P L E M E N T E D

        } // if
        nodes++;
    } // insert

    /********************************************************************************
     * Wedge the key-ref pair into node n.
     * @param key  the key to insert
     * @param ref  the value/node to insert
     * @param n    the current node
     * @param i    the insertion position within node n
     */
    private void wedge (K key, V ref, Node n, int i)
    {
        if(!n.isLeaf){
            for(int index = 0; index < n.nKeys; index++){
                if(key.compareTo(n.key[index]) <= 0){ // less than or equal to n.key
                    Node node = (Node) n.ref[index];
                    node.nKeys++;
                    node.key[node.nKeys] = key;
                    node.ref[node.nKeys] = ref;
                    //n.ref[i] = node;
                    break;
                }
                else{
                    if(index != n.nKeys - 1){ // check other keys first
                        continue;
                    }
                    Node node = (Node) n.ref[++index];
                    Node nodeBeingChanged = node;
                    while(!node.isLeaf){
                        node = (Node) node.ref[index];
                    }
                    if(node.nKeys == ORDER - 1){
                        Node newRoot = split(key, ref, node);
                        if(nodeBeingChanged != node){
                            nodeBeingChanged = replaceNode(nodeBeingChanged, newRoot, node); // newRoot has been inserted
                            n.ref[index] = nodeBeingChanged;
                            break;
                        }
                        root.isLeaf = false;
                        root.nKeys++;
                        root.key[root.nKeys - 1] = newRoot.key[newRoot.nKeys - 1];
                        root.ref[root.nKeys - 1] = newRoot.ref[0];
                        root.ref[root.nKeys] = newRoot.ref[1];
                        break;
                    }
                    node.key[node.nKeys] = key;
                    node.ref[node.nKeys] = ref;
                    node.nKeys++;
                    break;
                }
            }
        }
        else{
            for (int j = n.nKeys; j > i; j--) {
                n.key [j] = n.key [j - 1];
                n.ref [j] = n.ref [j - 1];
            } // for
            n.key [i] = key;
            n.ref [i] = ref;
            n.nKeys++;
        }
    } // wedge

    /********************************************************************************
     * Split node n and return the newly created node.
     * @param key  the key to insert
     * @param ref  the value/node to insert
     * @param n    the current node
     */
    private Node split (K key, V ref, Node n)
    {
        Node node = new Node(false);

        node.key[0] = n.key[2];
        node.nKeys++;

        Node leftNode = new Node(true);
        Node rightNode = new Node(true);

        try{
            Node leaf = (Node) n.ref[0];
            if(n.nKeys == 4 && leaf.isLeaf){
                leftNode = new Node(false);
                rightNode = new Node(false);
            }
        }catch(ClassCastException e){

        }

        int leftIndex = 0;
        int rightIndex = 0;
        for(int i = 0; i < n.nKeys; i++){        // insert back old values
            if(n.key[i].compareTo(node.key[0]) <= 0){ // left
                leftNode.key[leftIndex] = n.key[i];
                leftNode.ref[leftIndex] = n.ref[i];
                node.ref[0] = leftNode;
                leftNode.nKeys++;
                leftIndex++;
            }
            else{                                     // right
                rightNode.key[rightIndex] = n.key[i];
                rightNode.ref[rightIndex] = n.ref[i];
                node.ref[1] = rightNode;
                rightNode.nKeys++;
                rightIndex++;
            }

            if(n.nKeys == 4 && i == 3 && n.ref[i + 1] != null){ // process last node on right side
                //rightNode.key[rightIndex] = n.key[i];
//                if(rightIndex == 3){
//                    rightNode.ref[rightIndex + 1] = n.ref[i];
//                }
                rightNode.ref[rightIndex] = n.ref[i + 1];
                rightNode.isLeaf = false;
                node.ref[1] = rightNode;
                rightIndex++;
                findInsertionNode(node, key, ref);
                return node;
            }
        }

        if(key.compareTo(node.key[0]) < 0){           // insert new key/value
            insert(key, ref,( Node) node.ref[0], null);
        }
        else{
            insert(key, ref,( Node) node.ref[1], null);
        }
        return node;
        //  T O   B E   I M P L E M E N T E D
    } // split

    private void findInsertionNode(Node node, K key, V ref){
        if(key.compareTo(node.key[0]) > 0){
            try {
                findInsertionNode((Node) node.ref[1], key, ref);
            }catch (ClassCastException e){
                insert(key, ref, node, null);
            }
        }
    }

    private Node replaceNode(Node rootNode, Node newNode, Node oldNode){
        if(rootNode == oldNode){
            rootNode = newNode;
            return rootNode;
        }
        else{
            try{
                rootNode.ref[1] = replaceNode((Node) rootNode.ref[1], newNode, oldNode);
            }catch(ClassCastException e){

            }
        }
        return rootNode;
    }

    /********************************************************************************
     * The main method used for testing.
     * @param args the command-line arguments (args [0] gives number of keys to insert)
     */
    public static void main (String [] args)
    {
        BpTreeMap <Integer, Integer> bpt = new BpTreeMap <> (Integer.class, Integer.class);
        int nodes = 20;
        for(int i = 1; i <= nodes; ++i){
            bpt.put(i, i * 10);
        }
        bpt.entrySet();
        bpt.firstKey();
        bpt.lastKey();
        Set <Map.Entry <Integer, Integer>> entry = bpt.entrySet();
        bpt.headMap(80);
        bpt.tailMap(80);
        bpt.subMap(5, 15);
        out.println(bpt.size());
//        int totKeys = 10;
//        if (args.length == 1) totKeys = Integer.valueOf (args [0]);
//        for (int i = 1; i < totKeys; i += 2) bpt.put (i, i * i);
//        bpt.print (bpt.root, 1);
//        for (int i = 1; i <= nodes; i++) {
//            out.println ("key = " + i + " value = " + bpt.get (i));
//        }
//        out.println ("-------------------------------------------");
//        out.println ("Average number of nodes accessed = " + bpt.count / (double) totKeys);
    } // main

} // BpTreeMap class
