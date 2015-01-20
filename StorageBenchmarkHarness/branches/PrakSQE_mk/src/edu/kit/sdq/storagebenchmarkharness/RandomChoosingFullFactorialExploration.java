package edu.kit.sdq.storagebenchmarkharness;

import java.util.Collections;
import java.util.List;

/**
 * This class accepts the same input as the {@code FullFactorialExploration}. In
 * fact is uses the same procedure. Except that it does not work on all
 * experiments one after one but insteads chooses n experiments randomly out of
 * the full exploration. Each experiment will be selected at most once.
 * 
 * ATTENTION: The stragey is not suited for multi hosts setups where multiple
 * hosts should be benchmaked. Additionally the benchmarks are not repetable for
 * obvious reasons.
 * 
 * @author dominik
 * 
 */
public class RandomChoosingFullFactorialExploration extends FullFactorialExploration
{
	private final static Logger LOGGER = Logger.getLogger(RandomChoosingFullFactorialExploration.class);

	private final int count;

	public RandomChoosingFullFactorialExploration(int count)
	{
		this.count = count;
	}

	public <T> List<List<T>> explore(final List<? extends List<? extends T>> input)
	{
		LOGGER.error("-------------------------------------");
		LOGGER.error("# ATTENTION: Random Choosing in effect!");
		LOGGER.error("# Results will be unexpected");
		LOGGER.error("# Never use with more than one system");
		LOGGER.error("-------------------------------------");

		List<List<T>> re = super.explore(input);

		if (re == null)
		{
			return null;
		}

		Collections.shuffle(re);

		return re.subList(0, count);
	}
}
