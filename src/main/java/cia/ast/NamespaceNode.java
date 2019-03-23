package cia.ast;

import javax.annotation.Nonnull;
import java.util.List;

public final class NamespaceNode extends Node implements INamespace {
	public NamespaceNode(@Nonnull String name) {
		super(name);
	}

	@Nonnull
	@Override
	public final List<IClass> getComposites() {
		return getChildrenList(IClass.class);
	}

	@Nonnull
	@Override
	public final List<IFunction> getFunctions() {
		return getChildrenList(IFunction.class);
	}

	@Nonnull
	@Override
	public final List<IVariable> getVariables() {
		return getChildrenList(IVariable.class);
	}
}
