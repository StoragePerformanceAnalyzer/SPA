package edu.kit.sdq.storagebenchmarkharness;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

public class FullFactorialExploration implements ExplorationStrategy
{
	private final static Logger LOGGER = Logger.getLogger(FullFactorialExploration.class);

	/**
	 * This function generates a full factorial exploration using the cartesian
	 * product of a list of lists. In contrast to the mathematic definition,
	 * which is defined using sets, this implementation uses lists and maintains
	 * a order on the elements.
	 * 
	 * Example (with [] as List-Syntax):
	 * 
	 * <pre>
	 *   explore([ [1,2,3], ["a", "b"], [1.2, 2.4] ]) returns
	 *    [
	 *      [1, "a", 1.2], [1, "a", 2.4], [1, "b", 1.2], [1, "b", 2.4], 
	 *      [2, "a", 1.2], [2, "a", 2.4], [2, "b", 1.2], [2, "b", 2.4], 
	 *      [3, "a", 1.2], [3, "a", 2.4], [3, "b", 1.2], [3, "b", 2.4]
	 *    ]
	 * </pre>
	 * 
	 * The last list in the input is varied first while the first list in the
	 * input is varied last. This leads to a stable order where subsequent calls
	 * lead to the same result.
	 * 
	 * Special cases:
	 * <ul>
	 * <li>If {@code input} is null, null is returned</li>
	 * <li>If {@code input} only contains empty lists, a empty list is returned</li>
	 * <li>If {@code input} is a empty list, a empty list is returned
	 * <li>If one of the lists in {@code input} is empty, a single null is used
	 * at this place in the result</li>
	 * </ul>
	 * 
	 * @param input
	 *            a {@code List} of {@code List} for the input
	 * @return the full exploration as specified above
	 */
	public <T> List<List<T>> explore(final List<? extends List<? extends T>> input)
	{
		if (input == null)
		{
			return null;
		}

		if (input.size() == 0)
		{
			return new ArrayList<List<T>>();
		}

		// Store lengths
		int lengths[] = new int[input.size()];
		int elementCount = 1;
		boolean everyListWasEmpty = true;
		for (int i = 0; i < input.size(); i++)
		{
			lengths[i] = input.get(i).size();
			if (lengths[i] > 0)
			{
				everyListWasEmpty = false;
				elementCount *= lengths[i];
			} else
			{
				lengths[i] = 1;
			}
		}

		LOGGER.trace("ElementCount is %d, everyListWasEmpty=%b", elementCount, everyListWasEmpty);
		if (everyListWasEmpty)
		{
			// All lists were len=0
			return new ArrayList<List<T>>();
		}

		// Construct Result
		// Can't be Array because of generics
		List<List<T>> result = Lists.newArrayListWithCapacity(elementCount);
		for (int i = 0; i < elementCount; i++)
		{
			result.add(new ArrayList<T>(input.size()));
		}

		for (int l = 0; l < input.size(); l++)
		{
			LOGGER.trace("For Input %d", l);

			List<? extends T> line = input.get(l);

			// Calculate the multiplication of the lengths of all lines after
			// this
			int lenAfter = 1;
			for (int la = l + 1; la < input.size(); la++)
			{
				lenAfter *= lengths[la];
			}
			LOGGER.trace("Lenafter is %d", lenAfter);

			for (int i = 0; i < elementCount; i++)
			{
				List<T> addList = result.get(i);
				if (line.size() == 0)
				{
					addList.add(null);
				} else
				{
					addList.add(line.get((i / lenAfter) % line.size()));
				}
			}
		}

		return result;
	}
}
