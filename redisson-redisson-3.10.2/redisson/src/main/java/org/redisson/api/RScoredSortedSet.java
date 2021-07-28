/**
 * Copyright (c) 2013-2019 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.redisson.api;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.redisson.api.mapreduce.RCollectionMapReduce;
import org.redisson.client.protocol.ScoredEntry;

/**
 * Set containing elements sorted by score.
 * 
 * @author Nikita Koksharov
 *
 * @param <V> object type
 */
public interface RScoredSortedSet<V> extends RScoredSortedSetAsync<V>, Iterable<V>, RExpirable, RSortable<Set<V>> {

    public enum Aggregate {
        
        SUM, MAX, MIN
        
    }
    
    /**
     * Returns <code>RMapReduce</code> object associated with this object
     * 
     * @param <KOut> output key
     * @param <VOut> output value
     * @return MapReduce instance
     */
    <KOut, VOut> RCollectionMapReduce<V, KOut, VOut> mapReduce();
    
    /**
     * Removes and returns first available tail element of <b>any</b> sorted set,
     * waiting up to the specified wait time if necessary for an element to become available
     * in any of defined sorted sets <b>including</b> this one.
     * <p>
     * Requires <b>Redis 5.0.0 and higher.</b>
     * 
     * @param queueNames - names of queue
     * @param timeout how long to wait before giving up, in units of
     *        {@code unit}
     * @param unit a {@code TimeUnit} determining how to interpret the
     *        {@code timeout} parameter
     * @return the tail element, or {@code null} if all sorted sets are empty 
     */
    V pollLastFromAny(long timeout, TimeUnit unit, String ... queueNames);
    
    /**
     * Removes and returns first available head element of <b>any</b> sorted set,
     * waiting up to the specified wait time if necessary for an element to become available
     * in any of defined sorted sets <b>including</b> this one.
     * <p>
     * Requires <b>Redis 5.0.0 and higher.</b>
     * 
     * @param queueNames - names of queue
     * @param timeout how long to wait before giving up, in units of
     *        {@code unit}
     * @param unit a {@code TimeUnit} determining how to interpret the
     *        {@code timeout} parameter
     * @return the head element, or {@code null} if all sorted sets are empty 
     */
    V pollFirstFromAny(long timeout, TimeUnit unit, String ... queueNames);

    /**
     * Removes and returns the head element waiting if necessary for an element to become available.
     *
     * @return the head element
     */
    V takeFirst();

    /**
     * Removes and returns the tail element waiting if necessary for an element to become available.
     *
     * @return the tail element
     */
    V takeLast();
    
    /**
     * Removes and returns the head element or {@code null} if this sorted set is empty.
     *
     * @param timeout how long to wait before giving up, in units of
     *        {@code unit}
     * @param unit a {@code TimeUnit} determining how to interpret the
     *        {@code timeout} parameter
     * @return the head element, 
     *         or {@code null} if this sorted set is empty
     */
    V pollFirst(long timeout, TimeUnit unit);

    /**
     * Removes and returns the tail element or {@code null} if this sorted set is empty.
     *
     * @param timeout how long to wait before giving up, in units of
     *        {@code unit}
     * @param unit a {@code TimeUnit} determining how to interpret the
     *        {@code timeout} parameter
     * @return the tail element or {@code null} if this sorted set is empty
     */
    V pollLast(long timeout, TimeUnit unit);
    
    /**
     * Removes and returns the head elements or {@code null} if this sorted set is empty.
     *
     * @param count - elements amount
     * @return the head element, 
     *         or {@code null} if this sorted set is empty
     */
    Collection<V> pollFirst(int count);

    /**
     * Removes and returns the tail elements or {@code null} if this sorted set is empty.
     * 
     * @param count - elements amount
     * @return the tail element or {@code null} if this sorted set is empty
     */
    Collection<V> pollLast(int count);
    
    /**
     * Removes and returns the head element or {@code null} if this sorted set is empty.
     *
     * @return the head element, 
     *         or {@code null} if this sorted set is empty
     */
    V pollFirst();

    /**
     * Removes and returns the tail element or {@code null} if this sorted set is empty.
     *
     * @return the tail element or {@code null} if this sorted set is empty
     */
    V pollLast();

    /**
     * Returns the head element or {@code null} if this sorted set is empty.
     *
     * @return the head element or {@code null} if this sorted set is empty
     */
    V first();

    /**
     * Returns the tail element or {@code null} if this sorted set is empty.
     *
     * @return the tail element or {@code null} if this sorted set is empty
     */
    V last();

    /**
     * Returns score of the tail element or returns {@code null} if this sorted set is empty.
     *
     * @return the tail element or {@code null} if this sorted set is empty
     */
    Double firstScore();

    /**
     * Returns score of the head element or returns {@code null} if this sorted set is empty.
     *
     * @return the tail element or {@code null} if this sorted set is empty
     */
    Double lastScore();

    /**
     * Adds all elements contained in the specified map to this sorted set.
     * Map contains of score mapped by object. 
     * 
     * @param objects - map of elements to add
     * @return amount of added elements, not including already existing in this sorted set
     */
    int addAll(Map<V, Double> objects);

    /**
     * Removes values by score range.
     * 
     * @param startScore - start score. 
     *                     Use <code>Double.POSITIVE_INFINITY</code> or <code>Double.NEGATIVE_INFINITY</code> 
     *                     to define infinity numbers
     * @param startScoreInclusive - start score inclusive
     * @param endScore - end score
     *                     Use <code>Double.POSITIVE_INFINITY</code> or <code>Double.NEGATIVE_INFINITY</code> 
     *                     to define infinity numbers
     * 
     * @param endScoreInclusive - end score inclusive
     * @return number of elements removed
     */
    int removeRangeByScore(double startScore, boolean startScoreInclusive, double endScore, boolean endScoreInclusive);

    /**
     * Removes values by rank range. Indexes are zero based. 
     * <code>-1</code> means the highest score, <code>-2</code> means the second highest score.
     * 
     * @param startIndex - start index 
     * @param endIndex - end index
     * @return number of elements removed
     */
    int removeRangeByRank(int startIndex, int endIndex);

    /**
     * Returns rank of value, with the scores ordered from low to high.
     * 
     * @param o - object
     * @return rank or <code>null</code> if value does not exist
     */
    Integer rank(V o);
    
    /**
     * Returns rank of value, with the scores ordered from high to low.
     * 
     * @param o - object
     * @return rank or <code>null</code> if value does not exist
     */
    Integer revRank(V o);

    /**
     * Returns score of element or <code>null</code> if it doesn't exist.
     * 
     * @param o - element
     * @return score
     */
    Double getScore(V o);

    /**
     * Adds element to this set, overrides previous score if it has been already added.
     *
     * @param score - object score
     * @param object - object itself
     * @return <code>true</code> if element has added and <code>false</code> if not.
     */
    boolean add(double score, V object);

    /**
     * Adds element to this set, overrides previous score if it has been already added.
     * Finally return the rank of the item
     * @param score - object score
     * @param object - object itself
     * @return rank
     */
    Integer addAndGetRank(double score, V object);

    /**
     * Adds element to this set, overrides previous score if it has been already added.
     * Finally return the reverse rank of the item
     * @param score - object score
     * @param object - object itself
     * @return reverse rank
     */
    Integer addAndGetRevRank(double score, V object);

    /**
     * Adds element to this set only if has not been added before.
     * <p>
     * Works only with <b>Redis 3.0.2 and higher.</b>
     *
     * @param score - object score
     * @param object - object itself
     * @return <code>true</code> if element has added and <code>false</code> if not.
     */
    boolean tryAdd(double score, V object);

    /**
     * Returns size of this set.
     * 
     * @return size
     */
    int size();

    /**
     * Returns <code>true</code> if this set is empty
     * 
     * @return <code>true</code> if empty
     */
    boolean isEmpty();
    
    /**
     * Returns stream of elements in this set.
     * Elements are loaded in batch. Batch size is 10. 
     * 
     * @return stream of elements
     */
    Stream<V> stream();
    
    /**
     * Returns stream of elements in this set.
     * If <code>pattern</code> is not null then only elements match this pattern are loaded.
     * 
     * @param pattern - search pattern
     * @return stream of elements
     */
    Stream<V> stream(String pattern);
    
    /**
     * Returns stream of elements in this set.
     * Elements are loaded in batch. Batch size is defined by <code>count</code> param. 
     * 
     * @param count - size of elements batch
     * @return stream of elements
     */
    Stream<V> stream(int count);
    
    /**
     * Returns stream of elements in this set.
     * Elements are loaded in batch. Batch size is defined by <code>count</code> param.
     * If pattern is not null then only elements match this pattern are loaded.
     * 
     * @param pattern - search pattern
     * @param count - size of elements batch
     * @return stream of elements
     */
    Stream<V> stream(String pattern, int count);
    
    /**
     * Returns an iterator over elements in this set.
     * If <code>pattern</code> is not null then only elements match this pattern are loaded.
     * 
     * @param pattern - search pattern
     * @return iterator
     */
    Iterator<V> iterator(String pattern);
    
    /**
     * Returns an iterator over elements in this set.
     * Elements are loaded in batch. Batch size is defined by <code>count</code> param. 
     * 
     * @param count - size of elements batch
     * @return iterator
     */
    Iterator<V> iterator(int count);
    
    /**
     * Returns an iterator over elements in this set.
     * Elements are loaded in batch. Batch size is defined by <code>count</code> param.
     * If pattern is not null then only elements match this pattern are loaded.
     * 
     * @param pattern - search pattern
     * @param count - size of elements batch
     * @return iterator
     */
    Iterator<V> iterator(String pattern, int count);

    /**
     * Returns <code>true</code> if this sorted set contains encoded state of the specified element.
     *
     * @param o element whose presence in this collection is to be tested
     * @return <code>true</code> if this sorted set contains the specified
     *         element and <code>false</code> otherwise
     */
    boolean contains(Object o);

    /**
     * Returns this sorted set in array of Object type.
     * 
     * @return array of values
     */
    Object[] toArray();

    /**
     * Returns this sorted set in array of defined type.
     * 
     * @param a - instance of array
     * @return array of values
     */
    <T> T[] toArray(T[] a);

    /**
     * Removes a single instance of the specified element from this
     * sorted set, if it is present.
     *
     * @param o element to be removed from this sorted set, if present
     * @return <code>true</code> if an element was removed as a result of this call
     */
    boolean remove(Object o);

    /**
     * Returns <code>true</code> if this sorted set contains all of the elements
     * in encoded state in the specified collection.
     *
     * @param  c collection to be checked for containment in this sorted set
     * @return <code>true</code> if this sorted set contains all of the elements
     *         in the specified collection
     */
    boolean containsAll(Collection<?> c);

    /**
     * Removes all of this sorted set's elements that are also contained in the
     * specified collection.
     *
     * @param c collection containing elements to be removed from this collection
     * @return <code>true</code> if this sorted set changed as a result of the
     *         call
     */
    boolean removeAll(Collection<?> c);

    /**
     * Retains only the elements in this sorted set that are contained in the
     * specified collection.
     *
     * @param c collection containing elements to be retained in this collection
     * @return <code>true</code> if this sorted set changed as a result of the call
     */
    boolean retainAll(Collection<?> c);

    /**
     * Removes all elements of this sorted set.
     */
    void clear();

    /**
     * Increases score of specified element by value.
     * 
     * @param element - element whose score needs to be increased
     * @param value - value
     * @return updated score of element
     */
    Double addScore(V element, Number value);

    /**
     * Adds score to element and returns its rank
     * 
     * @param object - object itself
     * @param value - object score
     * @return rank
     */
    Integer addScoreAndGetRank(V object, Number value);

    /**
     * Adds score to element and returns its reverse rank
     * 
     * @param object - object itself
     * @param value - object score
     * @return reverse rank
     */
    Integer addScoreAndGetRevRank(V object, Number value);
    
    /**
     * Returns values by rank range. Indexes are zero based. 
     * <code>-1</code> means the highest score, <code>-2</code> means the second highest score.
     * 
     * @param startIndex - start index 
     * @param endIndex - end index
     * @return elements
     */
    Collection<V> valueRange(int startIndex, int endIndex);

    /**
     * Returns values by rank range in reverse order. Indexes are zero based. 
     * <code>-1</code> means the highest score, <code>-2</code> means the second highest score.
     * 
     * @param startIndex - start index 
     * @param endIndex - end index
     * @return elements
     */
    Collection<V> valueRangeReversed(int startIndex, int endIndex);

    /**
     * Returns entries (value and its score) by rank range. Indexes are zero based. 
     * <code>-1</code> means the highest score, <code>-2</code> means the second highest score.
     * 
     * @param startIndex - start index 
     * @param endIndex - end index
     * @return entries
     */
    Collection<ScoredEntry<V>> entryRange(int startIndex, int endIndex);
    
    /**
     * Returns entries (value and its score) by rank range in reverse order. Indexes are zero based. 
     * <code>-1</code> means the highest score, <code>-2</code> means the second highest score.
     * 
     * @param startIndex - start index 
     * @param endIndex - end index
     * @return entries
     */
    Collection<ScoredEntry<V>> entryRangeReversed(int startIndex, int endIndex);

    /**
     * Returns all values between <code>startScore</code> and <code>endScore</code>.
     * 
     * @param startScore - start score. 
     *                     Use <code>Double.POSITIVE_INFINITY</code> or <code>Double.NEGATIVE_INFINITY</code> 
     *                     to define infinity numbers
     * @param startScoreInclusive - start score inclusive
     * @param endScore - end score
     *                     Use <code>Double.POSITIVE_INFINITY</code> or <code>Double.NEGATIVE_INFINITY</code> 
     *                     to define infinity numbers
     * 
     * @param endScoreInclusive - end score inclusive
     * @return values
     */
    Collection<V> valueRange(double startScore, boolean startScoreInclusive, double endScore, boolean endScoreInclusive);

    /**
     * Returns all values between <code>startScore</code> and <code>endScore</code> in reversed order.
     * 
     * @param startScore - start score. 
     *                     Use <code>Double.POSITIVE_INFINITY</code> or <code>Double.NEGATIVE_INFINITY</code> 
     *                     to define infinity numbers
     * @param startScoreInclusive - start score inclusive
     * @param endScore - end score
     *                     Use <code>Double.POSITIVE_INFINITY</code> or <code>Double.NEGATIVE_INFINITY</code> 
     *                     to define infinity numbers
     * 
     * @param endScoreInclusive - end score inclusive
     * @return values
     */
    Collection<V> valueRangeReversed(double startScore, boolean startScoreInclusive, double endScore, boolean endScoreInclusive);

    /**
     * Returns all entries (value and its score) between <code>startScore</code> and <code>endScore</code>.
     * 
     * @param startScore - start score. 
     *                     Use <code>Double.POSITIVE_INFINITY</code> or <code>Double.NEGATIVE_INFINITY</code> 
     *                     to define infinity numbers
     * @param startScoreInclusive - start score inclusive
     * @param endScore - end score
     *                     Use <code>Double.POSITIVE_INFINITY</code> or <code>Double.NEGATIVE_INFINITY</code> 
     *                     to define infinity numbers
     * 
     * @param endScoreInclusive - end score inclusive
     * @return entries
     */
    Collection<ScoredEntry<V>> entryRange(double startScore, boolean startScoreInclusive, double endScore, boolean endScoreInclusive);

    /**
     * Returns all values between <code>startScore</code> and <code>endScore</code>.
     * 
     * @param startScore - start score. 
     *                     Use <code>Double.POSITIVE_INFINITY</code> or <code>Double.NEGATIVE_INFINITY</code> 
     *                     to define infinity numbers
     * @param startScoreInclusive - start score inclusive
     * @param endScore - end score
     *                     Use <code>Double.POSITIVE_INFINITY</code> or <code>Double.NEGATIVE_INFINITY</code> 
     *                     to define infinity numbers
     * 
     * @param endScoreInclusive - end score inclusive
     * @param offset - offset of sorted data
     * @param count - amount of sorted data
     * @return values
     */
    Collection<V> valueRange(double startScore, boolean startScoreInclusive, double endScore, boolean endScoreInclusive, int offset, int count);

    /**
     * Returns all values between <code>startScore</code> and <code>endScore</code> in reversed order.
     * 
     * @param startScore - start score. 
     *                     Use <code>Double.POSITIVE_INFINITY</code> or <code>Double.NEGATIVE_INFINITY</code> 
     *                     to define infinity numbers
     * @param startScoreInclusive - start score inclusive
     * @param endScore - end score
     *                     Use <code>Double.POSITIVE_INFINITY</code> or <code>Double.NEGATIVE_INFINITY</code> 
     *                     to define infinity numbers
     * 
     * @param endScoreInclusive - end score inclusive
     * @param offset - offset of sorted data
     * @param count - amount of sorted data
     * @return values
     */
    Collection<V> valueRangeReversed(double startScore, boolean startScoreInclusive, double endScore, boolean endScoreInclusive, int offset, int count);

    /**
     * Returns all entries (value and its score) between <code>startScore</code> and <code>endScore</code>.
     * 
     * @param startScore - start score. 
     *                     Use <code>Double.POSITIVE_INFINITY</code> or <code>Double.NEGATIVE_INFINITY</code> 
     *                     to define infinity numbers
     * @param startScoreInclusive - start score inclusive
     * @param endScore - end score
     *                     Use <code>Double.POSITIVE_INFINITY</code> or <code>Double.NEGATIVE_INFINITY</code> 
     *                     to define infinity numbers
     * 
     * @param endScoreInclusive - end score inclusive
     * @param offset - offset of sorted data
     * @param count - amount of sorted data
     * @return entries
     */
    Collection<ScoredEntry<V>> entryRange(double startScore, boolean startScoreInclusive, double endScore, boolean endScoreInclusive, int offset, int count);

    /**
     * Returns all entries (value and its score) between <code>startScore</code> and <code>endScore</code> in reversed order.
     * 
     * @param startScore - start score. 
     *                     Use <code>Double.POSITIVE_INFINITY</code> or <code>Double.NEGATIVE_INFINITY</code> 
     *                     to define infinity numbers
     * @param startScoreInclusive - start score inclusive
     * @param endScore - end score
     *                     Use <code>Double.POSITIVE_INFINITY</code> or <code>Double.NEGATIVE_INFINITY</code> 
     *                     to define infinity numbers
     * 
     * @param endScoreInclusive - end score inclusive
     * @return entries
     */
    Collection<ScoredEntry<V>> entryRangeReversed(double startScore, boolean startScoreInclusive, double endScore, boolean endScoreInclusive);

    /**
     * Returns all entries (value and its score) between <code>startScore</code> and <code>endScore</code> in reversed order.
     * 
     * @param startScore - start score. 
     *                     Use <code>Double.POSITIVE_INFINITY</code> or <code>Double.NEGATIVE_INFINITY</code> 
     *                     to define infinity numbers
     * @param startScoreInclusive - start score inclusive
     * @param endScore - end score
     *                     Use <code>Double.POSITIVE_INFINITY</code> or <code>Double.NEGATIVE_INFINITY</code> 
     *                     to define infinity numbers
     * 
     * @param endScoreInclusive - end score inclusive
     * @param offset - offset of sorted data
     * @param count - amount of sorted data
     * @return entries
     */
    Collection<ScoredEntry<V>> entryRangeReversed(double startScore, boolean startScoreInclusive, double endScore, boolean endScoreInclusive, int offset, int count);

    /**
     * Returns the number of elements with a score between <code>startScore</code> and <code>endScore</code>.
     * 
     * @param startScore - start score
     * @param startScoreInclusive - start score inclusive
     * @param endScore - end score
     * @param endScoreInclusive - end score inclusive
     * @return count of elements
     */
    int count(double startScore, boolean startScoreInclusive, double endScore, boolean endScoreInclusive);
    
    /**
     * Read all values at once.
     * 
     * @return values
     */
    Collection<V> readAll();

    /**
     * Intersect provided ScoredSortedSets 
     * and store result to current ScoredSortedSet
     * 
     * @param names - names of ScoredSortedSet
     * @return length of intersection
     */
    int intersection(String... names);

    /**
     * Intersect provided ScoredSortedSets with defined aggregation method 
     * and store result to current ScoredSortedSet
     * 
     * @param aggregate - score aggregation mode
     * @param names - names of ScoredSortedSet
     * @return length of intersection
     */
    int intersection(Aggregate aggregate, String... names);

    /**
     * Intersect provided ScoredSortedSets mapped to weight multiplier 
     * and store result to current ScoredSortedSet
     * 
     * @param nameWithWeight - name of ScoredSortedSet mapped to weight multiplier
     * @return length of intersection
     */
    int intersection(Map<String, Double> nameWithWeight);

    /**
     * Intersect provided ScoredSortedSets mapped to weight multiplier 
     * with defined aggregation method 
     * and store result to current ScoredSortedSet
     * 
     * @param aggregate - score aggregation mode
     * @param nameWithWeight - name of ScoredSortedSet mapped to weight multiplier
     * @return length of intersection
     */
    int intersection(Aggregate aggregate, Map<String, Double> nameWithWeight);

    /**
     * Union provided ScoredSortedSets 
     * and store result to current ScoredSortedSet
     * 
     * @param names - names of ScoredSortedSet
     * @return length of union
     */
    int union(String... names);

    /**
     * Union provided ScoredSortedSets with defined aggregation method 
     * and store result to current ScoredSortedSet
     * 
     * @param aggregate - score aggregation mode
     * @param names - names of ScoredSortedSet
     * @return length of union
     */
    int union(Aggregate aggregate, String... names);

    /**
     * Union provided ScoredSortedSets mapped to weight multiplier 
     * and store result to current ScoredSortedSet
     * 
     * @param nameWithWeight - name of ScoredSortedSet mapped to weight multiplier
     * @return length of union
     */
    int union(Map<String, Double> nameWithWeight);

    /**
     * Union provided ScoredSortedSets mapped to weight multiplier 
     * with defined aggregation method 
     * and store result to current ScoredSortedSet
     * 
     * @param aggregate - score aggregation mode
     * @param nameWithWeight - name of ScoredSortedSet mapped to weight multiplier
     * @return length of union
     */
    int union(Aggregate aggregate, Map<String, Double> nameWithWeight);
    
}
