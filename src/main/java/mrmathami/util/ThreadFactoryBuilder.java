package mrmathami.util;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public final class ThreadFactoryBuilder {
	private static final AtomicLong COUNT = new AtomicLong(0);
	private String namePrefix = null;
	private Boolean daemon = null;
	private Integer priority = null;
	private ThreadFactory backingThreadFactory = null;
	private UncaughtExceptionHandler uncaughtExceptionHandler = null;

	private static ThreadFactory build(ThreadFactoryBuilder builder) {
		final ThreadFactory backingThreadFactory =
				builder.backingThreadFactory != null
						? builder.backingThreadFactory
						: Executors.defaultThreadFactory();
		final String namePrefix =
				builder.namePrefix != null
						? builder.namePrefix
						: "ThreadFactoryBuilder-" + COUNT.getAndIncrement();
		final AtomicLong count = new AtomicLong(0);
		final Boolean daemon = builder.daemon;
		final Integer priority = builder.priority;
		final UncaughtExceptionHandler uncaughtExceptionHandler = builder.uncaughtExceptionHandler;

		return runnable -> {
			Thread thread = backingThreadFactory.newThread(runnable);
			thread.setName(namePrefix + "-" + count.getAndIncrement());
			if (daemon != null) thread.setDaemon(daemon);
			if (priority != null) thread.setPriority(priority);
			if (uncaughtExceptionHandler != null) thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
			return thread;
		};
	}

	public ThreadFactoryBuilder setNamePrefix(String namePrefix) {
		if (namePrefix == null) throw new NullPointerException("namePrefix should not be null");
		if (namePrefix.isBlank()) throw new NullPointerException("namePrefix should not be blank");
		this.namePrefix = namePrefix;
		return this;
	}

	public ThreadFactoryBuilder setDaemon(boolean daemon) {
		this.daemon = daemon;
		return this;
	}

	public ThreadFactoryBuilder setPriority(int priority) {
		if (priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY) {
			throw new IllegalArgumentException("Thread priority must be in range of " + Thread.MIN_PRIORITY + " and "
					+ Thread.MAX_PRIORITY + ", receive value " + priority);
		}
		this.priority = priority;
		return this;
	}

	public ThreadFactoryBuilder setUncaughtExceptionHandler(UncaughtExceptionHandler uncaughtExceptionHandler) {
		if (uncaughtExceptionHandler == null) throw new NullPointerException("UncaughtExceptionHandler cannot be null");
		this.uncaughtExceptionHandler = uncaughtExceptionHandler;
		return this;
	}

	public ThreadFactoryBuilder setThreadFactory(ThreadFactory backingThreadFactory) {
		if (uncaughtExceptionHandler == null) throw new NullPointerException("BackingThreadFactory cannot be null");
		this.backingThreadFactory = backingThreadFactory;
		return this;
	}

	public ThreadFactory build() {
		return build(this);
	}
}