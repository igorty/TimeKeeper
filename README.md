# Overview #

**TimeKeeper** is intended to monitor a bunch of things which are dependent on time; is some kind of flexible time organizer.

![timekeeper_sample](https://cloud.githubusercontent.com/assets/19972519/18230301/483f1e38-729f-11e6-86e3-dcfdbce738cb.png)

It provides so-called *time counters* which can work in four different modes. Two of them are rather spread: these are *Stopwatch* and *Timer* modes. *Stopwatch* may be initialized not only with 0 time unit values: its initial value may be any positive time value. Of course *Timer* may have any initial value too. When *timer* reaches zero, it continues counting showing negative time value.

Time value may be edited after *Stopwatch* or *Timer* *time counter* is created, no matter it is running or paused. Time value editing has form of adding/subtracting specified time amount. *Timer* time value may be even turned to elapsed (negative) after this operation, while *Stopwatch* may not: minimal value for it is just zero. Restart possibility is supported for mentioned time counters too.

Two other time counter's modes are so-called *Elapsed from* and *Remains till*. These time counters calculate time amount elapsed from or remaining till specified time instant. Time zone rules are taken into account when calculating the time difference, so there will be no time "jumping" when clocks are moved from "winter" to "summer" time or vice versa.

The feature of both last mentioned modes initialization, that they may be initialized with their negative values. Here is the explanation. Let's say today is *the 1st of January, 2016*. We want time counter to count time elapsed from *the 1st of January, 2010*. In this case it simply shows **positive** elapsed time. But we can set (when it is applicable) target date time that **has not** come yet. In case of *Elapsed from* mode time counter initially will show **negative** difference (target date time will be in the future, so **there is no positive "elapsed from" difference!**), and after reaching the target time point value is becoming **positive**. *Remains till* mode has fully **opposite** behavior.

## How time difference is represented to the user ##

The feature of time representing is that it will be displayed as *3 months 15 days 5 hours* instead of *105 days 5 hours* or *2,525 hours* for example. There is user-convenient conversion performed.

*Stopwatch*/*Timer* and *Elapsed from*/*Remains till* have some differences.

*Stopwatch* and *Timer* may provide counting in **two** ways. The key difference between these ways is only in days-in-month quantity. In one case each month is evaluated with 30 days (so year is evaluated with 360 days), while in other case each month's days quantity forms 365 days in year. Each of *Stopwatches* and *Timers* time counters is set with one of this ways at creation time, and this time counting way cannot be changed later.

*Elapsed from* and *Remains till* time counters count time difference in the following manner. Let's say today is *the 1st of May, 2016* and the target date time is *the 14th of July the same year*. The time difference will be represented as *2 months 13 days*.

## Time counters layout customization ##
Each time counter may be shown in its own customized layout, which can be changed whenever user wants.

Time unit names may be shown in four ways:

* signs (such as " for the second time unit);

* short name;

* full name;

* digits only.

Time value itself may be shown in four ways too:

* *"If reaches"* mode: time units showing starts from biggest non zero value (e.g. current value of time counter is 10 minutes and 0 seconds, only these values are being shown);

* *"Show all"* mode: all time units from years to seconds are being shown;

* *"Custom strict"*: only time units from specified by user range are being shown. Other time units won't be shown even in case of bigger than chosen range time value;

* *"Increase able"*: time units from specified by user range are always shown. Bigger time units are only shown if time value reaches them, less time units are not shown at all.

Supported time units are: years, months, days, hours, minutes and seconds.

User can provide convenient explanation text to each time counter. There is a possibility to customize time counters order by dragging them. Time values can be copied to clipboard or time counter may be dragged itself on any text field to set current time counter's value on it. There is also visual and sound notification provided when time elapses (sound notification is not yet implemented).

## Documentation ##

Project is not fully completed and there are several tasks to do (basically with GUI). Its documentation is rich, but a lot of Javadocs and comments are in Russian. I made it locally and didn't expect it to grow up, but will translate all remaining marks to English.

Program logic and GUI are separated. [app.tar.time_obj](TimeKeeper/src/main/java/app/tar/time_obj) package represents program logic and
[app.tar.graphical_shell](TimeKeeper/src/main/java/app/tar/graphical_shell) represents GUI.

There are three languages are provided: English, Russian and Ukrainian. Project is made with an opportunity to easily implement additional languages.

Time counting is based on [java.time](https://docs.oracle.com/javase/8/docs/api/java/time/package-summary.html). GUI is based on [JavaFX](https://docs.oracle.com/javase/8/javafx/api/toc.htm). Additionally [ControlsFX](http://fxexperience.com/controlsfx/) are used ([Bitbucket link](https://bitbucket.org/controlsfx/controlsfx/)).

Initial class is [app.tar.graphical_shell.Main_class](TimeKeeper/src/main/java/app/tar/graphical_shell/Main_class.java).

## Additional resources ##

[ControlsFX](http://fxexperience.com/controlsfx/) ([Bitbucket link](https://bitbucket.org/controlsfx/controlsfx/)).

Images are taken from:

* [iconmonstr](http://iconmonstr.com/);

* [freepik](http://www.freepik.com/);

* [flaticon](www.flaticon.com);

* [Hopstarter](http://hopstarter.deviantart.com/) ([additonal link](http://www.iconarchive.com/artist/hopstarter.html)) ([Attribution-NonCommercial-NoDerivatives 4.0 International license](https://creativecommons.org/licenses/by-nc-nd/4.0/legalcode)).

## Contacts ##

igor.tar@yahoo.com
