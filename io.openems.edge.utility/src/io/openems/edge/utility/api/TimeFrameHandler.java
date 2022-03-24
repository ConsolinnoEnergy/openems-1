package io.openems.edge.utility.api;

import java.time.LocalDateTime;

/**
 * This Interface handles a TimeFrame.
 * Means: Either check if your current Time (by {@link TimeFrameType}) is in between a Time Frame.
 * E.g. if you want something to work between 0-15 min each hour -> select TimeFrameType Minute
 * and set your start at 0 and stop at 15.
 * When calling this Interface, either call the {@link #isWithinTimeFrame(TimeFrameType, int, int)} method.
 * to check if your current time is in between a given time frame (depending on the TimeFrameType).
 * Or call the {@link #isWithinTimeFrameWithGivenCurrent(int, int, int)} method. to check if your measured and stored
 * time is in between this time frame.
 */
public interface TimeFrameHandler {

    int UNEXPECTED_ERROR = -1;

    /**
     * Checks if your current time is within the given TimeFrame.
     *
     * @param type  the TimeFrameType e.g. MONTHS
     * @param start the start deltaTime
     * @param stop  the stop deltaTime
     * @return true when the current time is within the timeFrame
     */
    static boolean isWithinTimeFrame(TimeFrameType type, int start, int stop) {
        int current = getCurrentTimeByTimeFrameType(type);
        if (current == UNEXPECTED_ERROR) {
            return false;
        }
        return isWithinTimeFrameWithGivenCurrent(start, stop, current);

    }

    /**
     * With a given "currentTime" as integer, check if your current time is within the timeFrame.
     *
     * @param start   the starting point.
     * @param stop    the stopping point.
     * @param current your current time represented by int. e.g. if it is 1:30 am but type is Minutes current == 30.
     * @return a boolean
     */
    static boolean isWithinTimeFrameWithGivenCurrent(int start, int stop, int current) {

        //if start = 45 and stop 15
        //logic switches start from 00-15 or from 45-60
        if (start > stop) {
            return current >= start || current <= stop;
        } else {
            return current >= start && current <= stop;
        }
    }

    /**
     * Get the Current time as int depending on timerType.
     * e.g. if it is 1:30am and you want to measure minutes -> return 30.
     *
     * @param type the {@link TimeFrameType}
     * @return current Time as int depending on TimeFrameType.
     */
    static int getCurrentTimeByTimeFrameType(TimeFrameType type) {
        switch (type) {

            case SECONDS:
                return LocalDateTime.now().getSecond();

            case MINUTE:
                return LocalDateTime.now().getMinute();

            case HOUR:
                return LocalDateTime.now().getHour();

            case DAY:
                return LocalDateTime.now().getDayOfMonth();

            case MONTH:
                return LocalDateTime.now().getMonthValue();

            case YEAR:
                return LocalDateTime.now().getYear();

            case DAY_OF_YEAR:
                return LocalDateTime.now().getDayOfYear();

            default:
                return UNEXPECTED_ERROR;
        }
    }

}
