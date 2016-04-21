/***
 * This is a variation of org.objectweb.asm.tree.analysis.BasicValue
 * that tracks reference types
 * 
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package ca.nevdull.jbcc3;

import java.util.HashMap;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.Value;

public class TypedValue implements Value {
	
    public static final TypedValue UNINITIALIZED_VALUE = new TypedValue(null);

    public static final TypedValue INT_VALUE = new TypedValue(Type.INT_TYPE);

    public static final TypedValue FLOAT_VALUE = new TypedValue(Type.FLOAT_TYPE);

    public static final TypedValue LONG_VALUE = new TypedValue(Type.LONG_TYPE);

    public static final TypedValue DOUBLE_VALUE = new TypedValue(
            Type.DOUBLE_TYPE);

    public static final TypedValue RETURNADDRESS_VALUE = new TypedValue(
            Type.VOID_TYPE);

    private final Type type;

	private static HashMap<Type,TypedValue> referenceCache = new HashMap<Type,TypedValue>();

    public TypedValue(final Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }
    
    public static TypedValue getReference(Type type) {
    	assert (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY);
    	TypedValue t = referenceCache.get(type);
    	if (t == null) {
    		t = new TypedValue(type);
    		referenceCache.put(type,t);
    	}
    	return t;
    }

    public int getSize() {
        return type == Type.LONG_TYPE || type == Type.DOUBLE_TYPE ? 2 : 1;
    }

    public boolean isReference() {
        return type != null
                && (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY);
    }

    @Override
    public boolean equals(final Object value) {
        if (value == this) {
            return true;
        } else if (value instanceof TypedValue) {
            if (type == null) {
                return ((TypedValue) value).type == null;
            } else {
                return type.equals(((TypedValue) value).type);
            }
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return type == null ? 0 : type.hashCode();
    }
	
    @Override
    public String toString() {
        if (this == UNINITIALIZED_VALUE) {
            return ".";
        } else if (this == RETURNADDRESS_VALUE) {
            return "A";
        } else if (isReference()) {
            return "R";
        } else {
            return type.getDescriptor();
        }
    }

}
