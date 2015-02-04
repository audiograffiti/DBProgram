package com.company;


/************************************************************************************
 * @file ExtHashMap.java
 *
 * @author  John Miller
 */

import java.io.*;
import java.lang.reflect.Array;
import static java.lang.System.out;
import java.util.*;

/************************************************************************************
 * This class provides hash maps that use the Extendable Hashing algorithm.  Buckets
 * are allocated and stored in a hash table and are referenced using directory dir.
 */
public class ExtHashMap <K, V>
        extends AbstractMap <K, V>
        implements Serializable, Cloneable, Map <K, V>
{
    /** The number of slots (for key-value pairs) per bucket.
     */
    private static final int SLOTS = 4;

    /** The class for type K.
     */
    private final Class <K> classK;

    /** The class for type V.
     */
    private final Class <V> classV;

    /********************************************************************************
     * This inner class defines buckets that are stored in the hash table.
     */
    private class Bucket
    {
        int  nKeys;
        K [] key;
        V [] value;
        @SuppressWarnings("unchecked")
        Bucket ()
        {
            nKeys = 0;
            key   = (K []) Array.newInstance (classK, SLOTS);
            value = (V []) Array.newInstance (classV, SLOTS);
        } // constructor
    } // Bucket inner class

    /** The hash table storing the buckets (buckets in physical order)
     */
    private final List <Bucket> hTable;

    /** The directory providing access paths to the buckets (buckets in logical oder)
     */
    private final List <Bucket> dir;

    /** The modulus for hashing (= 2^D) where D is the global depth
     */
    private int mod;

    /** The number of buckets
     */
    private int nBuckets;

    /** Counter for the number buckets accessed (for performance testing).
     */
    private int count = 0;

    /********************************************************************************
     * Construct a hash table that uses Extendable Hashing.
     * @param _classK    the class for keys (K)
     * @param _classV    the class for keys (V)
     * @param initSize  the initial number of buckets (a power of 2, e.g., 4)
     */
    public ExtHashMap (Class <K> _classK, Class <V> _classV, int initSize)
    {
        classK = _classK;
        classV = _classV;
        hTable = new ArrayList <> ();   // for bucket storage
        dir    = new ArrayList <> ();   // for bucket access
        mod    = nBuckets = initSize;
    } // constructor

    /********************************************************************************
     * Return a set containing all the entries as pairs of keys and values.
     * @return  the set view of the map
     */
    public Set <Map.Entry <K, V>> entrySet ()
    {
        Set <Map.Entry <K, V>> enSet = new HashSet <> ();
    	for(int i = 0; i < this.nBuckets; i++){
    		Bucket b = dir.get(i);
    		for(int j = 0; j < ExtHashMap.SLOTS; j++){
    			if(b.key[j] != null){
    				K tKey = b.key[j];
    				V tVal = b.value[j];
    				enSet.add (new AbstractMap.SimpleEntry <K, V> (tKey, tVal));

        return enSet;
    } // entrySet

    /********************************************************************************
     * Given the key, look up the value in the hash table.
     * @param key  the key used for look up
     * @return  the value associated with the key
     */
    public V get (Object key)
    {
        int    i = h (key);
        Bucket b = dir.get (i);
        this.count++;
        V value;
        //check bucket for key, if found, return the value at the same index in value list as index of key
        for(int j = 0; j < b.key.length; j++){
        	if(b.key[j] != null && b.key[j].equals(key)){
        		value = b.value[j];
        		return value;
        	}
        }
        //if key is not found, use previous hashing function to check the next possible bucket for the key
        for(this.mod = nBuckets; this.mod >= 2; this.mod = this.mod/2){
        	int k = h(key);
        	Bucket c = dir.get(k);
        	//increment the count variable since a new bucket is being checked
        	this.count++;
        	//compare all key values in new bucket to search key, if they match, return value at this index
        	for(int l = 0; l < c.key.length; l++){
        		if(c.key[l] != null && c.key[l].equals(key)){
        			value = c.value[l];
        			this.mod = this.nBuckets;
        			return value;
        		}
        	}
        }
        //if all possible hash functions are checked and key is not found, alert user and return null
        this.mod = this.nBuckets;
        out.println(key + " not found.");
        return null;
    } // get

    /********************************************************************************
     * Put the key-value pair in the hash table.
     * @param key    the key to insert
     * @param value  the value to insert
     * @return  null (not the previous value)
     */
    public V put (K key, V value)
    {
        boolean exists = false;
    	//check to see if the key already exists in hTable, if it does, do not re-insert
    	for(int p = 0; p < hTable.size(); p++){
    		Bucket check = hTable.get(p);
    		for(int m = 0; m < ExtHashMap.SLOTS; m++){
    			if(check.key[m] != null && check.key[m].equals(key)){
    				exists = true;
    			}
    		}
    	}
    	//if the key was not found in hTable, insert into the next available slot
    	if(exists != true){
    		//add new bucket if hTable is empty (size = 1)
        	if(hTable.isEmpty()){
        		Bucket hBuc = new Bucket();
        		hTable.add(hBuc);
        	}
        	boolean inserted = false;
        	//set index to insert to last bucket in the table
        	int index = hTable.size() - 1;
        	Bucket h = hTable.get(index);
        	//check if there is room available in last bucket, if so insert key, value pair here
        	if(h.nKeys < ExtHashMap.SLOTS){
        		for(int j = 0; j < ExtHashMap.SLOTS && inserted == false; j++){
        			if(h.key[j] == null){
        				Array.set(h.key, j, key);
        				Array.set(h.value, j, value);
        				h.nKeys++;
        				inserted = true;
        			}
        		}
        	}
        	//if last bucket was full, add new bucket and insert key value pair into first slot in this bucket
        	else {
        		Bucket hBuc = new  Bucket();
        		hTable.add(hBuc);
        		int ind = hTable.size() -1;
        		hBuc = hTable.get(ind);
        		Array.set(hBuc.key, 0, key);
        		Array.set(hBuc.value, 0, value);
        		hBuc.nKeys++;
        	}
        }
    	//find the index to store the value based on the hash code of the key
    	int    i = h (key);
        Bucket b = new Bucket();
        //if the size of the dir list is less than the index to insert, increase the dir list to proper size
        if(i >= dir.size()){
        	for(int a = dir.size(); a <= i; a++){
        		Bucket tBuc = new Bucket();
        		dir.add(tBuc);
        	}
        }
        b = dir.get(i);
        //check if there is an empty spot in the key list, if so, insert the key and value
        if(b.nKeys < ExtHashMap.SLOTS){
    		for(int j = 0; j < ExtHashMap.SLOTS; j++){
    			if(b.key[j] == null){
    				Array.set(b.key, j, key);
    				Array.set(b.value, j, value);
    				b.nKeys++;
    				return null;
    			}
   			}
   		}
   
        //if the index to insert to is already full, increase mod value and reorganize values accordingly
    	else{
    		int capacity = 0;
    		//check to see if the list is full by counting number of keys in the list
    		for(int a = 0; a < nBuckets; a++){
    			Bucket t = dir.get(a);
    			capacity += t.nKeys;
    		}
    		//if list is full, increase the map depth by one and double the mod for the hashing function
    		if(capacity >= this.size()){
    			nBuckets = nBuckets*2;
    			mod = mod *2;
    			//index is now based on new hashing function
    			i = h(key);
    		}
    		for(int a = dir.size(); a < nBuckets; a++){
    			Bucket tBuc = new Bucket();
    			dir.add(tBuc);
    		}
    		boolean inserted = false;
    		int index;
    		for(index = 0; index < ExtHashMap.SLOTS; index++){
    			K tKey = b.key[index];
    			V tValue = b.value[index];
    			//check key list against new hashing function, if hash value does not equal index, but value in correct bucket
    			if(h(tKey) != i){
    				if(inserted == false){
    					//set this index to the key and value to insert
    					Array.set(b.key, index, key);
    					Array.set(b.value, index, value);
    					//put the previous value of this index in the correct bucket based on the new hash function
    					this.put(tKey, tValue);
    					inserted = true;
    				}
    				//if initial value is already inserted and another key no longer belongs in the bucket, set value to null
    				else{
    					Array.set(b.key, index, null);
    					Array.set(b.value, index, null);
    					this.put(tKey, tValue);
    					b.nKeys--;
    				}
    			}
    		}
    	}
        return null;
    } // put

    /********************************************************************************
     * Return the size (SLOTS * number of buckets) of the hash table.
     * @return  the size of the hash table
     */
    public int size ()
    {
        return SLOTS * nBuckets;
    } // size

    /********************************************************************************
     * Print the hash table.
     */
    private void print ()
    {
        out.println ("Hash Table (Extendable Hashing)");
        out.println ("-------------------------------------------");
        out.println("Key ----> Value:");
        for(int i = 0; i < this.hTable.size(); i++){
        	Bucket b = hTable.get(i);
        	for(int j = 0; j < ExtHashMap.SLOTS; j++){
        		if(b.key[j] != null){
        			out.println(b.key[j] + " ----> " + b.value[j]);
        		}
        	}
        }
        out.println ("-------------------------------------------");
    } // print

    /********************************************************************************
     * Hash the key using the hash function.
     * @param key  the key to hash
     * @return  the location of the directory entry referencing the bucket
     */
    private int h (Object key)
    {
        return key.hashCode () % mod;
    } // h

    /********************************************************************************
     * The main method used for testing.
     * @param  args the command-line arguments (args [0] gives number of keys to insert)
     */
    /*public static void main (String [] args)
    {
        ExtHashMap <Integer, Integer> ht = new ExtHashMap <> (Integer.class, Integer.class, 4);
        int nKeys = 30;
        if (args.length == 1) nKeys = Integer.valueOf (args [0]);
        for (int i = 1; i < nKeys; i += 1) ht.put (i, i * i);
        ht.print ();
        for (int i = 0; i < nKeys; i++) {
            out.println ("key = " + i + " value = " + ht.get (i));
        } // for
        out.println ("-------------------------------------------");
        out.println ("Average number of buckets accessed = " + ht.count / (double) nKeys);
    } // main
*/
} // ExtHashMap class
