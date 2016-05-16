/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.export;

/**
 * Bean with info from the DB dump header
 * @author K. Benedyczak
 */
public class DumpHeader
{
	/**
	 * Initial version number of Unity 2 schema.
	 */
	public static final int V_INITIAL2 = 3;
	
	
	private int versionMajor;
	private int versionMinor;
	private long timestamp;
	public int getVersionMajor()
	{
		return versionMajor;
	}
	public void setVersionMajor(int versionMajor)
	{
		this.versionMajor = versionMajor;
	}
	public int getVersionMinor()
	{
		return versionMinor;
	}
	public void setVersionMinor(int versionMinor)
	{
		this.versionMinor = versionMinor;
	}
	public long getTimestamp()
	{
		return timestamp;
	}
	public void setTimestamp(long timestamp)
	{
		this.timestamp = timestamp;
	}
}
