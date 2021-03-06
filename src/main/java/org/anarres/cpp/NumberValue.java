package org.anarres.cpp;

import mrmathami.annotations.Nonnull;
import mrmathami.annotations.Nullable;

import java.util.Objects;

final class NumberValue implements Comparable<NumberValue> {

	static final NumberValue INTEGER_ZERO = new NumberValue(0);
	static final NumberValue INTEGER_POSITIVE_ONE = new NumberValue(1);
	static final NumberValue INTEGER_NEGATIVE_ONE = new NumberValue(-1);

	static final NumberValue DECIMAL_NAN = new NumberValue(Double.NaN);
	static final NumberValue DECIMAL_ZERO = new NumberValue(0.0);
	static final NumberValue DECIMAL_POSITIVE_ONE = new NumberValue(1.0);
	static final NumberValue DECIMAL_POSITIVE_INFINITY = new NumberValue(Double.POSITIVE_INFINITY);
	static final NumberValue DECIMAL_NEGATIVE_ONE = new NumberValue(-1.0);
	static final NumberValue DECIMAL_NEGATIVE_INFINITY = new NumberValue(Double.NEGATIVE_INFINITY);

	@Nonnull
	static NumberValue of(double decimal) {
		if (Double.isNaN(decimal)) return DECIMAL_NAN;
		if (decimal == 0.0) return DECIMAL_ZERO;
		if (decimal > 0.0) {
			if (decimal == 1.0) return DECIMAL_POSITIVE_ONE;
			if (Double.isInfinite(decimal)) return DECIMAL_POSITIVE_INFINITY;
		} else {
			if (decimal == -1.0) return DECIMAL_NEGATIVE_ONE;
			if (Double.isInfinite(decimal)) return DECIMAL_NEGATIVE_INFINITY;
		}
		return new NumberValue(decimal);
	}

	@Nonnull
	static NumberValue of(long integer) {
		if (integer == 0) return INTEGER_ZERO;
		if (integer == 1) return INTEGER_POSITIVE_ONE;
		if (integer == -1) return INTEGER_NEGATIVE_ONE;
		return new NumberValue(integer);
	}

	private final double decimal;
	private final long integer;
	private final boolean mode;

	private NumberValue(double decimal) {
		this.decimal = decimal;
		this.integer = 0;
		this.mode = true;
	}

	private NumberValue(long integer) {
		this.decimal = 0;
		this.integer = integer;
		this.mode = false;
	}

	@Nonnull
	private NumberValue newValue(double decimal) {
		if (mode && Double.compare(decimal, this.decimal) == 0) return this;
		return of(decimal);
	}

	@Nonnull
	private NumberValue newValue(long integer) {
		if (!mode && integer == this.integer) return this;
		return of(integer);
	}

	@Nonnull
	public NumberValue negate() {
		return mode ? newValue(-decimal) : newValue(-integer);
	}

	@Nonnull
	public NumberValue add(@Nonnull NumberValue value) {
		return mode == value.mode
				? mode
				? newValue(decimal + value.decimal)
				: newValue(integer + value.integer)
				: newValue(mode ? decimal + value.integer : integer + value.decimal);
	}

	@Nonnull
	public NumberValue subtract(@Nonnull NumberValue value) {
		return mode == value.mode
				? mode
				? newValue(decimal - value.decimal)
				: newValue(integer - value.integer)
				: newValue(mode ? decimal - value.integer : integer - value.decimal);
	}

	@Nonnull
	public NumberValue multiply(@Nonnull NumberValue value) {
		return mode == value.mode
				? mode
				? newValue(decimal * value.decimal)
				: newValue(integer * value.integer)
				: newValue(mode ? decimal * value.integer : integer * value.decimal);
	}

	@Nonnull
	public NumberValue divide(@Nonnull NumberValue value) {
		return mode == value.mode
				? mode
				? newValue(decimal / value.decimal)
				: newValue(integer / value.integer)
				: newValue(mode ? decimal / value.integer : integer / value.decimal);
	}

	@Nonnull
	public NumberValue remainder(@Nonnull NumberValue value) {
		// C++ doesn't support modulus on double
		if (mode || value.mode) throw new UnsupportedOperationException();
		return newValue(integer % value.integer);
	}

	@Nonnull
	public NumberValue shiftLeft(int n) {
		if (mode) throw new UnsupportedOperationException();
		return newValue(integer << n);
	}

	@Nonnull
	public NumberValue shiftRight(int n) {
		if (mode) throw new UnsupportedOperationException();
		return newValue(integer >> n);
	}

	private static int shiftValue(@Nonnull NumberValue value) {
		if (value.mode || value.integer > Integer.MAX_VALUE || value.integer < Integer.MIN_VALUE) {
			throw new UnsupportedOperationException();
		}
		return (int) value.integer;
	}

	@Nonnull
	public NumberValue shiftLeft(@Nonnull NumberValue value) {
		if (mode) throw new UnsupportedOperationException();
		return newValue(integer << shiftValue(value));
	}

	@Nonnull
	public NumberValue shiftRight(@Nonnull NumberValue value) {
		if (mode) throw new UnsupportedOperationException();
		return newValue(integer >> shiftValue(value));
	}

	@Nonnull
	public NumberValue and(@Nonnull NumberValue value) {
		if (mode || value.mode) throw new UnsupportedOperationException();
		return newValue(integer & value.integer);
	}

	@Nonnull
	public NumberValue or(@Nonnull NumberValue value) {
		if (mode || value.mode) throw new UnsupportedOperationException();
		return newValue(integer | value.integer);
	}

	@Nonnull
	public NumberValue xor(@Nonnull NumberValue value) {
		if (mode || value.mode) throw new UnsupportedOperationException();
		return newValue(integer ^ value.integer);
	}

	@Nonnull
	public NumberValue not() {
		if (mode) throw new UnsupportedOperationException();
		return newValue(~integer);
	}

	@Override
	public int hashCode() {
		return Objects.hash(decimal, integer, mode);
	}

	@Override
	public String toString() {
		return mode ? Double.toString(decimal) : Long.toString(integer);
	}

	public boolean equals(long integer) {
		return mode ? Double.compare(decimal, integer) == 0 : integer == this.integer;
	}

	public boolean equals(double decimal) {
		return Double.compare(decimal, mode ? this.decimal : this.integer) == 0;
	}

	@Override
	public boolean equals(@Nullable Object object) {
		if (this == object) return true;
		if (!(object instanceof NumberValue)) return false;
		final NumberValue value = (NumberValue) object;
		return value.mode ? equals(value.decimal) : equals(value.integer);
	}

	public int compareTo(long integer) {
		return mode ? Double.compare(decimal, integer) : Long.compare(this.integer, integer);
	}

	public int compareTo(double decimal) {
		return Double.compare(mode ? this.decimal : this.integer, decimal);
	}

	@Override
	public int compareTo(@Nonnull NumberValue value) {
		return value.mode ? compareTo(value.decimal) : compareTo(value.integer);
	}
}
