/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.base.registries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maintains a simple registry of some implementations.
 * 
 * @author K. Benedyczak
 */
public abstract class TypesRegistryBase<T>
{
	private Map<String, T> elements;
	
	public TypesRegistryBase(List<? extends T> typeElements)
	{
		this.elements = new HashMap<String, T>(10);
		if (typeElements == null)
			typeElements = new ArrayList<T>();
		for (T idDef: typeElements)
		{
			String id = getId(idDef);
			if (this.elements.containsKey(id))
				throw new IllegalStateException("Key " + id + " is used twice");
			this.elements.put(id, idDef);
		}
	}
	
	public T getByName(String name) throws IllegalArgumentException
	{
		T ret = elements.get(name);
		if (ret == null)
			throw new IllegalArgumentException("Type " + name + " is not supported");
		return ret;
	}
	
	public Collection<T> getAll()
	{
		return elements.values();
	}
	
	protected abstract String getId(T from);
}
