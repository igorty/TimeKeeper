package time_obj;


/**
 * Time counter work mode.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public enum Mode
{
	/** Stopwatch mode. */
	M_stopwatch,
	/** Timer mode. */
	M_countdown,
	/** Counting time elapsed from specified instant. */
	M_elapsed_from,
	/** Counting time remaining to specified instant. */
	M_remains_till
}
