package edu.kit.sdq.storagebenchmarkharness;

import java.util.List;

/**
 * This interface provides a single methode which is used for 'exploration'.
 * This means the transformation of a list of possible values for each parameter
 * into a list of configurations.
 * 
 * The following contract applies:
 * <ul>
 * <li>The input is a List of Lists, where the outer list has the length n.
 * <li>The function returns a new list of lists where the inner lists all have
 * length n
 * <li>The type of the i_th element of each inner output list is the same.
 * <li>The type of the i_th element of each inner output lists is the same as
 * the type of the i_th outer input list.
 * <li>The order of subsequent calls with the same input stays the same
 * <li>The method produces a deterministic result
 * <li>It may only return null if the input was null
 * </ul>
 * 
 * See {@code FullFactorialExploration} for a implementation. Other
 * implementations might use other strategies like only taking the first value
 * or using a Plackett–Burman.
 * 
 * @author Dominik Bruhn
 */
public interface ExplorationStrategy
{
	public <T> List<List<T>> explore(final List<? extends List<? extends T>> input);
}