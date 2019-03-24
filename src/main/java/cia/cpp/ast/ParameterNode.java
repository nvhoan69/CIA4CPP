package cia.cpp.ast;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ParameterNode extends Node implements IParameter {
	@Nullable
	private final IType type;

	private ParameterNode(@Nonnull String name, @Nullable IType type) {
		super(name);
		this.type = type;
	}

	public static ParameterNodeBuilder builder() {
		return new ParameterNodeBuilder();
	}

	@Nullable
	@Override
	public final IType getType() {
		return type;
	}

	public static final class ParameterNodeBuilder extends NodeBuilder<ParameterNode, ParameterNodeBuilder> {
		@Nullable
		private IType type;

		@Nonnull
		@Override
		public final ParameterNode build() {
			if (name == null) {
				throw new NullPointerException("Builder element(s) is null.");
			}
			return new ParameterNode(name, type);
		}

		@Nullable
		public final IType getType() {
			return type;
		}

		@Nonnull
		public final ParameterNodeBuilder setType(@Nullable IType type) {
			this.type = type;
			return this;
		}
	}
}
