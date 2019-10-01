package mrmathami.cia.cpp.ast;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Hold a type
 */
public interface ITypeContainer<E extends Node> {
	@Nullable
	Node getType();

	@Nonnull
	E setType(@Nullable Node type);
}