package it.unimi.dsi.webgraph.labelling;

/*		 
 * Copyright (C) 2007-2016 Paolo Boldi and Sebastiano Vigna 
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses/>.
 *
 */

/*
 * Adapted from AbstractIntLabel by Alex Thomo, Jan 2018.
 */

/** An abstract (single-attribute) long label.
 *
 * <p>This class provides basic methods for a label holding an long.
 * Concrete implementations may impose further requirements on the long.
 * 
 * <p>Implementing subclasses must provide constructors, {@link Label#copy()},
 * {@link Label#fromBitStream(it.unimi.dsi.io.InputBitStream, long)}, {@link Label#toBitStream(it.unimi.dsi.io.OutputBitStream, long)}
 * and possibly override {@link #toString()}.
 */

public abstract class AbstractLongLabel extends AbstractLabel implements Label {
	/** The key of the attribute represented by this label. */
	protected final String key;
	/** The value of the attribute represented by this label. */
	public long value;

	/** Creates an long label with given key and value.
	 * 
	 * @param key the (only) key of this label.
	 * @param value the value of this label.
	 */
	public AbstractLongLabel( String key, long value ) {
		this.key = key;
		this.value = value;
	}

	public String wellKnownAttributeKey() {
		return key;
	}

	public String[] attributeKeys() {
		return new String[] { key };
	}

	public Class<?>[] attributeTypes() {
		return new Class[] { long.class };
	}

	public Object get( String key ) {
		return Long.valueOf( getLong( key ) ); 
	}

	public int getInt( String key ) {
		return (int)getLong( key );
	}

	public long getLong( String key ) {
		if ( this.key.equals( key ) ) return value;
		throw new IllegalArgumentException( "Unknown key " + key );
	}

	public float getFloat( String key ) {
		return getLong( key );
	}

	public double getDouble( String key ) {
		return getLong( key );
	}

	public Object get() {
		return Long.valueOf( getLong() ); 
	}

	public int getInt() {
		return (int)value;
	}

	public long getLong() {
		return value;
	}

	public float getFloat() {
		return value;
	}

	public double getDouble() {
		return value;
	}

	public String toString() {
		return key + ":" + value;
	}
	
	@Override
	public boolean equals( Object x ) {
		if ( x instanceof AbstractLongLabel ) 
			return ( value == ( (AbstractLongLabel)x ).value );
		else return false;
	}
	
	@Override
	public int hashCode() {
		return (int)value;
	}
}
