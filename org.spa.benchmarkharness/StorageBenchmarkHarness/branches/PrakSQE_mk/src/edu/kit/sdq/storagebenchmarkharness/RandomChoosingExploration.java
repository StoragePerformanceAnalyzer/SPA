package edu.kit.sdq.storagebenchmarkharness;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;

public class RandomChoosingExploration implements ExplorationStrategy
{
	private final static Logger LOGGER = Logger.getLogger(RandomChoosingExploration.class);

	private final int count;

	public RandomChoosingExploration(int count)
	{
		this.count = count;
	}

	@Override
	public <T> List<List<T>> explore(List<? extends List<? extends T>> input)
	{
		LOGGER.error("-------------------------------------");
		LOGGER.error("# ATTENTION: Random Choosing in effect!");
		LOGGER.error("# Results will be unexpected");
		LOGGER.error("-------------------------------------");

		if (input == null)
		{
			return null;
		}

		if (input.size() == 0)
		{
			return new ArrayList<List<T>>();
		}

		Random rand = new Random();

		List<List<T>> result = Lists.newArrayListWithCapacity(count);
		for (int i = 0; i < count; i++)
		{
			List<T> item = new ArrayList<T>(input.size());

			for (int li = 0; li < input.size(); li++)
			{
				List<? extends T> l = input.get(li);
				if (l.size() == 0)
				{
					LOGGER.trace("Input %d: fixed null", i);
					item.add(null);
				} else if (l.size() == 1)
				{
					LOGGER.trace("Input %d: fixed value", i, l.get(0));
					item.add(l.get(0));
				} else if (l.size() == 2 && l.get(0).getClass() == Integer.class)
				{
					int min = (Integer) l.get(0);
					int max = (Integer) l.get(1);
					LOGGER.trace("Input %d: random range between %d and %d", i, min, max);

					Integer v = rand.nextInt(max - min + 1) + min;

					item.add((T) v);
				} else
				{
					LOGGER.trace("Input %d: random out of %d samples", i, l.size());
					item.add(l.get(rand.nextInt(l.size())));
				}
			}

			result.add(item);
		}

		return result;
	}

}
