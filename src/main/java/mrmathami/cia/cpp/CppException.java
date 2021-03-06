package mrmathami.cia.cpp;

import mrmathami.annotations.Nonnull;

public final class CppException extends Exception {
	private static final long serialVersionUID = -1L;

	public CppException(@Nonnull String message) {
		super(message);
	}

	public CppException(@Nonnull String message, @Nonnull Throwable cause) {
		super(message, cause);
	}
}
