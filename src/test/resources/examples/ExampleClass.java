package com.example;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * This class demonstrates various examples of Javadoc documentation.
 * It contains methods with different levels of documentation quality
 * to showcase how the documentation analyzer evaluates them.
 * 
 * @author Example Author
 * @version 1.0
 */
public class ExampleClass {
    
    private List<String> items;
    private Map<String, Integer> counts;
    
    /**
     * Constructs a new ExampleClass with empty collections.
     */
    public ExampleClass() {
        this.items = new ArrayList<>();
        this.counts = new HashMap<>();
    }
    
    /**
     * Adds an item to the list.
     * 
     * @param item the item to add
     */
    public void addItem(String item) {
        items.add(item);
        
        // Update count
        int count = counts.getOrDefault(item, 0);
        counts.put(item, count + 1);
    }
    
    /**
     * Gets the count of a specific item.
     * 
     * @param item the item to get the count for
     * @return the count of the item, or 0 if the item doesn't exist
     */
    public int getCount(String item) {
        return counts.getOrDefault(item, 0);
    }
    
    /**
     * Calculates the average length of all items.
     * 
     * @return the average length of all items, or 0 if there are no items
     * @throws ArithmeticException if an arithmetic error occurs
     */
    public double calculateAverageLength() {
        if (items.isEmpty()) {
            return 0;
        }
        
        int totalLength = 0;
        for (String item : items) {
            totalLength += item.length();
        }
        
        return (double) totalLength / items.size();
    }
    
    // Poor documentation - missing parameter and return documentation
    /**
     * Finds items that contain the specified substring.
     */
    public List<String> findItems(String substring) {
        List<String> result = new ArrayList<>();
        
        for (String item : items) {
            if (item.contains(substring)) {
                result.add(item);
            }
        }
        
        return result;
    }
    
    // No documentation
    public void clearItems() {
        items.clear();
        counts.clear();
    }
    
    /**
     * Processes an item and returns a result.
     * 
     * @param item the item to process
     * @param options processing options
     * @return the processed result
     * @throws IllegalArgumentException if the item is invalid
     * @throws NullPointerException if the item is null
     */
    public String processItem(String item, Map<String, Object> options) {
        if (item == null) {
            throw new NullPointerException("Item cannot be null");
        }
        
        if (item.isEmpty()) {
            throw new IllegalArgumentException("Item cannot be empty");
        }
        
        // Process the item based on options
        StringBuilder result = new StringBuilder(item);
        
        if (options.containsKey("reverse") && (boolean) options.get("reverse")) {
            result.reverse();
        }
        
        if (options.containsKey("uppercase") && (boolean) options.get("uppercase")) {
            return result.toString().toUpperCase();
        }
        
        return result.toString();
    }
    
    /**
     * Merges two lists of items.
     * 
     * @param list1 the first list to merge
     * @param list2 the second list to merge
     * @return a new list containing all items from both lists
     */
    public static List<String> mergeItems(List<String> list1, List<String> list2) {
        List<String> result = new ArrayList<>(list1);
        result.addAll(list2);
        return result;
    }
    
    /**
     * Gets all items in the list.
     * 
     * @return an unmodifiable view of the items list
     */
    public List<String> getItems() {
        return List.copyOf(items);
    }
}
