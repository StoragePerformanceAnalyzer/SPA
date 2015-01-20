package edu.kit.sdq.storagebenchmarkharness;

import org.slf4j.LoggerFactory;

/**
 * This class wraps around the {@code slf4j} package to provide some features
 * which {@code slf4j} does not support. This class can be used as a drop-in
 * replacement for {@code slf4fj}. It supports the following new features:
 * 
 * <ul>
 * <li>Log-Messages can be formated using Format-Strings and var-args. This
 * enables passing of multiple arguments to a log-message, for example:
 * 
 * <pre>
 * Logger.debug(&quot;Array (size=%d) contains %s at element %d&quot;, array.length, array[i], i);
 * </pre>
 * 
 * This makes the old Logger syntax obsolete which used '{}' as substitution
 * parameter and only supported two arguments.</li>
 * </ul>
 * 
 * The new features generate some overhead because the String formating is done
 * prior to the check whether the messages will be logged at all. This is why
 * this logging should not be used in performance cirtical environments.
 * 
 * @author Dominik Bruhn
 * 
 */
public final class Logger
{
	private final org.slf4j.Logger log;

	private Logger(org.slf4j.Logger logger)
	{
		log = logger;
	}

	public static Logger getLogger(Class<?> clazz)
	{
		return new Logger(LoggerFactory.getLogger(clazz));
	}

	public void trace(String msg)
	{
		log.trace(msg);
	}

	public void trace(String format, Object... args)
	{
		log.trace(String.format(format, args));
	}

	public void trace(String msg, Throwable t)
	{
		log.trace(msg, t);
	}

	public void debug(String format, Object... args)
	{
		log.debug(String.format(format, args));
	}

	public void debug(String msg, Throwable t)
	{
		log.debug(msg, t);
	}

	public void info(String format, Object... args)
	{
		log.info(String.format(format, args));
	}

	public void info(String msg, Throwable t)
	{
		log.info(msg, t);
	}

	public void warn(String format, Object... args)
	{
		log.warn(String.format(format, args));
	}

	public void warn(String msg, Throwable t)
	{
		log.warn(msg, t);
	}

	public void error(String format, Object... args)
	{
		log.error(String.format(format, args));
	}

	public void error(String msg, Throwable t)
	{
		log.error(msg, t);
	}
}
