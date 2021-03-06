/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.exceptions;

/**
 * Thrown when attribtue type is invalid.
 * @author K. Benedyczak
 */
public class IllegalAttributeTypeException extends EngineException
{
	private static final long serialVersionUID = 1L;

	public IllegalAttributeTypeException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public IllegalAttributeTypeException(String msg)
	{
		super(msg);
	}
}
