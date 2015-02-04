package com.company;


/************************************************************************************
 * @file LinHashMap.java
 *
 * @author  John Miller
 */

import java.io.*;
import java.lang.reflect.Array;
import static java.lang.System.out;
import java.util.*;

/************************************************************************************
 * This class provides hash maps that use the Linear Hashing algorithm.
 * A hash table is created that is an array of buckets.
 */
public class LinHashMap <K, V>
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
        int    nKeys;
        K []   key;
        V []   value;
        Bucket next;
        @SuppressWarnings("unchecked")
        Bucket (Bucket n)
        {
            nKeys = 0;
            key   = (K []) Array.newInstance (classK, SLOTS);
            value = (V []) Array.newInstance (classV, SLOTS);
            next  = n;
        } // constructor
    } // Bucket inner class

    /** The list of buckets making up the hash table.
     */
    private final List <Bucket> hTable;

    /** The modulus for low resolution hashing
     */
    private int mod1;

    /** The modulus for high resolution hashing
     */
    private int mod2;

    /** Counter for the number buckets accessed (for performance testing).
     */
    private int count = 0;

    /** The index of the next bucket to split.
     */
    private int split = 0;

    /********************************************************************************
     * Construct a hash table that uses Linear Hashing.
     * @param _classK    the class for keys (K)
     * @param _classV    the class for keys (V)
     * @param initSize  the initial number of home buckets (a power of 2, e.g., 4)
     */
    public LinHashMap (Class <K> _classK, Class <V> _classV, int initSize)
    {
        classK = _classK;
        classV = _classV;
        hTable = new ArrayList <> ();
        mod1   = initSize;
        mod2   = 2 * mod1;
    } // constructor

    /********************************************************************************
     * Return a set containing all the entries as pairs of keys and values.
     * @return  the set view of the map
     */
    public Set <Map.Entry <K, V>> entrySet ()
    {
        Set <Map.Entry <K, V>> enSet = new HashSet <> ();

        for (int i = 0; i < size(); i++) {            
    		for (Bucket b = hTable.get(i); b != null; b = b.next) {
                
    			enSet.add (new AbstractMap.SimpleEntry <K, V> (b.key[i], b.value[i]));
            
    		} // end inner for
        } // end outer for

        return enSet;
    } // entrySet

    /********************************************************************************
     * Given the key, look up the value in the hash table.
     * @param key  the key used for look up
     * @return  the value associated with the key
     */
    public V get (Object key)
    {
        int i = h (key);

        Bucket b = hTable.get(i);    	
             	
        for(int j = 0; j < b.nKeys; j++){
            	
        	if (b.key[j] == key){                	
        		
        		count++;
                return b.value[j];
                	
        	} // end if
        		
        } // end inner for

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
        int i = h (key);

        // If there are less buckets than the index number, create new buckets.
        if(hTable.size() <= i){       	
        	
        	// add buckets
        	for(int j = hTable.size(); j <= i; j++){
        		
        		Bucket b0 = new Bucket(null);
        		hTable.add(b0);
        		
        	} // end for        	
        } // end if
    	    	
        // Insert key and value pair:
        
    	Bucket b = hTable.get(i);
    	
    	// If bucket is full, insert at overflow.
    	if(b.nKeys >= SLOTS){
    		
    		for (int j = SLOTS; b.key[j-1] != null; j++){
  
    			if(b.key[j] == null){
        			
        			b.key[j] = key;
        			b.value[j] = value;        		
        			hTable.set(i, b);
        			b.nKeys++;
        			break;
            	
        		} // end if        		    			
    		} // end for   
    		
    	} else { // Insert at the next available spot.
    	
    		for(int j = 0; j < SLOTS; j++){
    		    
        		if(b.key[j] == null){
        			
        			b.key[j] = key;
        			b.value[j] = value;        		
        			hTable.set(i, b);
        			b.nKeys++;
        			break;
        			            	
        		} // end if
        	} // end for    		
    	} // end if-else
    	 	
    	// If there are less buckets than where the split indicates, fill table with buckets.
        if(hTable.size() <= split){       	
        	
        	// add buckets
        	for(int k = hTable.size(); k <= split; k++){
        		
        		Bucket b0 = new Bucket(null);
        		hTable.add(b0);
        		
        	} // end for        	
        } // end if
        
        // Check if needs to split buckets:
    	
    	Bucket bSplit = hTable.get(split); // the bucket to split
    	Bucket b2; // the new bucket
    	int i2; // new index for the second hash function
    	
    	// If bucket is full, split the bucket.
    	if(bSplit.nKeys >= SLOTS){
    		
    		// rehash each key to determine which ones will stay in the same bucket or move to the new bucket
    		for(int j = 0; j < bSplit.nKeys; j++){
    			
    			i2 = h2 (bSplit.key[j]);
    			
    			// if the new index is not the same as the old bucket, move that key and value to the new bucket.
    			if(i2 != i){
    				
    				// If there are less buckets than the index number, create new buckets.
    		        if(hTable.size() <= i2){       	
    		        	
    		        	// add buckets
    		        	for(int k = hTable.size(); k <= i2; k++){
    		        		
    		        		Bucket b0 = new Bucket(null);
    		        		hTable.add(b0);
    		        		
    		        	} // end for        	
    		        } // end if
    		        
    		        // Insert the key and value into the new bucket
    		        b2 = hTable.get(i2);
    		        
    		        for(int m = 0; b2.key[m-1] != null; m++){
    	    		    
    	        		if(b2.key[m] == null){
    	        			
    	        			b2.key[m] = bSplit.key[j];
    	        			b2.value[m] = bSplit.value[j];        		
    	        			hTable.set(i, b2);
    	        			b2.nKeys++;
    	        			bSplit.nKeys--;
    	        			            	
    	        		} // end if
    	        	} // end for 
    			} // end if
    		} // end for
    	} // end if
    	    
    	split++; // update split to the next bucket

        return null;
    } // put

    /********************************************************************************
     * Return the size (SLOTS * number of home buckets) of the hash table.
     * @return  the size of the hash table
     */
    public int size ()
    {
        return SLOTS * (mod1 + split);
    } // size

    /********************************************************************************
     * Print the hash table.
     */
    private void print ()
    {
        out.println ("Hash Table (Linear Hashing)");
        out.println ("-------------------------------------------");

        out.println("Key --> Value:");
                
        for (int i = 0; i < hTable.size(); i++) {
            
            Bucket b = hTable.get(i);
            
            for (int j = 0; j < SLOTS; j++) {             
                
            	if(b.key[j] != null){
            		
            		out.println ();
            		out.println(b.key[j] + " --> " + b.value[j]);
            		
            	} // end if            	
            } // end inner for            
        } // end outer for

        out.println ("-------------------------------------------");
    } // print

    /********************************************************************************
     * Hash the key using the low resolution hash function.
     * @param key  the key to hash
     * @return  the location of the bucket chain containing the key-value pair
     */
    private int h (Object key)
    {
        return key.hashCode () % mod1;
    } // h

    /********************************************************************************
     * Hash the key using the high resolution hash function.
     * @param key  the key to hash
     * @return  the location of the bucket chain containing the key-value pair
     */
    private int h2 (Object key)
    {
        return key.hashCode () % mod2;
    } // h2

    /********************************************************************************
     * The main method used for testing.
     * @param  args the command-line arguments (args [0] gives number of keys to insert)
     */
    public static void main (String [] args)
    {
        LinHashMap <Integer, Integer> ht = new LinHashMap <> (Integer.class, Integer.class, 11);
        int nKeys = 30;
        if (args.length == 1) nKeys = Integer.valueOf (args [0]);
        for (int i = 1; i < nKeys; i += 2) ht.put (i, i * i);
        ht.print ();
        for (int i = 0; i < nKeys; i++) {
            out.println ("key = " + i + " value = " + ht.get (i));
        } // for
        out.println ("-------------------------------------------");
        out.println ("Average number of buckets accessed = " + ht.count / (double) nKeys);
    } // main

} // LinHashMap class
